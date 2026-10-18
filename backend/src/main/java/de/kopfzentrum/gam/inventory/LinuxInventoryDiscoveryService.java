package de.kopfzentrum.gam.inventory;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Schritt 40k33a: erste modulare Linux-Inventarisierung auf echter Hardware.
 * Verwendet ausschließlich lokale Standardwerkzeuge und /proc-/sys-Dateien.
 * Fehlende Werkzeuge oder Rechte führen zu Teilergebnissen, nicht zum Abbruch.
 */
@Service
public class LinuxInventoryDiscoveryService {
  private static final long FAST_TIMEOUT_SECONDS = 8;
  private static final long SOFTWARE_TIMEOUT_SECONDS = 30;
  // 40k36t7: Strukturierte Software wird wie beim SSH-Pfad separat vom Geräteprotokoll übergeben.
  private final ConcurrentHashMap<String,String> latestSoftwareDetails = new ConcurrentHashMap<>();


  /** 40k36t7: Erkennt, ob eine registrierte Identität der Maschine entspricht,
   * auf der das GAM-Backend selbst läuft. Damit entscheidet der zentrale Dispatcher
   * zwischen Backend-lokal und SSH, ohne SSH für den lokalen Host vorauszusetzen. */
  public boolean isLocalTarget(String address, String hardwareAddress, String name) {
    if (!isLinux()) return false;
    String localIp=run("ip -o -4 addr show scope global 2>/dev/null | awk 'NR==1{split($4,a,\"/\");print a[1]}'", FAST_TIMEOUT_SECONDS);
    String localMac=normalizeMac(run("ip -o link show 2>/dev/null | awk '$2!=\"lo:\" && /link\\/ether/{print $17;exit}'", FAST_TIMEOUT_SECONDS));
    String localName=firstNonBlank(run("hostnamectl --static 2>/dev/null", FAST_TIMEOUT_SECONDS), localHostName());
    if(address!=null && localIp!=null && address.trim().equalsIgnoreCase(localIp)) return true;
    if(hardwareAddress!=null && localMac!=null && normalizeMac(hardwareAddress)!=null && normalizeMac(hardwareAddress).equalsIgnoreCase(localMac)) return true;
    return name!=null && localName!=null && name.trim().equalsIgnoreCase(localName);
  }

  /** Übergibt die zuletzt lokal ermittelte Softwareliste genau einmal. */
  public String consumeLatestSoftwareDetails(String identityHint) {
    if(identityHint==null) return null;
    return latestSoftwareDetails.remove(identityHint);
  }

  public void scan(boolean enabled, String now, Consumer<DiscoveredDevice> out,
                   Consumer<DeviceDiscoveryService.DiscoveryDiagnosticEvent> diagnostics,
                   String sessionId, int currentCount) {
    if (!enabled) {
      diagnostic(diagnostics, sessionId, "LINUX_INVENTORY", "SKIPPED", "Linux-Inventarisierung ist deaktiviert", currentCount);
      return;
    }
    if (!isLinux()) {
      diagnostic(diagnostics, sessionId, "LINUX_INVENTORY", "SKIPPED", "GAM wird nicht unter Linux ausgeführt", currentCount);
      return;
    }

    diagnostic(diagnostics, sessionId, "LINUX_INVENTORY", "RUNNING", "Modulare Linux-Inventarisierung wird gestartet", currentCount);
    List<String> successful = new ArrayList<>();
    List<String> unavailable = new ArrayList<>();

    String hostname = firstNonBlank(run("hostnamectl --static 2>/dev/null", FAST_TIMEOUT_SECONDS), localHostName());
    String prettyName = osRelease("PRETTY_NAME");
    String distro = firstNonBlank(osRelease("NAME"), "Linux");
    String distroVersion = firstNonBlank(osRelease("VERSION_ID"), run("uname -r", FAST_TIMEOUT_SECONDS));
    String kernel = run("uname -sr", FAST_TIMEOUT_SECONDS);
    String architecture = run("uname -m", FAST_TIMEOUT_SECONDS);
    String desktop = firstNonBlank(env("XDG_CURRENT_DESKTOP"), env("DESKTOP_SESSION"));
    String init = commandExists("systemctl") ? firstNonBlank(run("systemctl --version 2>/dev/null | head -n1", FAST_TIMEOUT_SECONDS), "systemd") : detectInit();
    String uptime = firstNonBlank(run("uptime -p 2>/dev/null", FAST_TIMEOUT_SECONDS), run("awk '{printf \"%.0f Minuten\", $1/60}' /proc/uptime 2>/dev/null", FAST_TIMEOUT_SECONDS));
    success(successful, "SYSTEM", prettyName, kernel, architecture);

    String manufacturer = readFile("/sys/class/dmi/id/sys_vendor");
    String model = readFile("/sys/class/dmi/id/product_name");
    String version = readFile("/sys/class/dmi/id/product_version");
    String serial = firstNonBlank(readFile("/sys/class/dmi/id/product_serial"), readFile("/sys/class/dmi/id/board_serial"));
    String uuid = readFile("/sys/class/dmi/id/product_uuid");
    String board = join(" ", readFile("/sys/class/dmi/id/board_vendor"), readFile("/sys/class/dmi/id/board_name"), readFile("/sys/class/dmi/id/board_version"));
    String bios = join(" / ", readFile("/sys/class/dmi/id/bios_vendor"), readFile("/sys/class/dmi/id/bios_version"), readFile("/sys/class/dmi/id/bios_date"));
    success(successful, "DMI", manufacturer, model, serial, board, bios);

    String cpuModel = firstNonBlank(run("lscpu 2>/dev/null | sed -n 's/^Model name:[[:space:]]*//p' | head -n1", FAST_TIMEOUT_SECONDS),
        run("awk -F: '/model name/{gsub(/^[ \\t]+/,\"\",$2);print $2;exit}' /proc/cpuinfo 2>/dev/null", FAST_TIMEOUT_SECONDS));
    String sockets = run("lscpu -p=SOCKET 2>/dev/null | grep -v '^#' | sort -u | wc -l", FAST_TIMEOUT_SECONDS);
    String cores = firstNonBlank(run("lscpu 2>/dev/null | sed -n 's/^Core(s) per socket:[[:space:]]*//p' | head -n1", FAST_TIMEOUT_SECONDS), run("nproc --all 2>/dev/null", FAST_TIMEOUT_SECONDS));
    String threads = run("nproc --all 2>/dev/null", FAST_TIMEOUT_SECONDS);
    String virtualization = run("lscpu 2>/dev/null | sed -n 's/^Virtualization:[[:space:]]*//p' | head -n1", FAST_TIMEOUT_SECONDS);
    String hypervisor = run("systemd-detect-virt 2>/dev/null", FAST_TIMEOUT_SECONDS);
    success(successful, "CPU", cpuModel, threads);

    String ramTotal = run("free -h 2>/dev/null | awk '/^Mem:/{print $2}'", FAST_TIMEOUT_SECONDS);
    String ramAvailable = run("free -h 2>/dev/null | awk '/^Mem:/{print $7}'", FAST_TIMEOUT_SECONDS);
    String swap = run("free -h 2>/dev/null | awk '/^Swap:/{print $2 \" gesamt, \" $3 \" belegt\"}'", FAST_TIMEOUT_SECONDS);
    String ramModules = run("(command -v dmidecode >/dev/null && dmidecode -t memory 2>/dev/null | awk '/^[[:space:]]*Size:/{if($2!=\"No\"&&$2!=\"0\"){if(n++)printf \"; \"; printf \"%s %s\",$2,$3}} END{print \"\"}') || true", FAST_TIMEOUT_SECONDS);
    success(successful, "MEMORY", ramTotal);

    String blockDevices = run("lsblk -dn -o NAME,SIZE,TYPE,ROTA,MODEL,TRAN 2>/dev/null | awk '$3==\"disk\"{printf \"%s: %s · %s · %s · %s; \",$1,$2,($4==0?\"SSD/NVMe\":\"HDD\"),$5,$6}'", FAST_TIMEOUT_SECONDS);
    String filesystems = run("lsblk -ln -o NAME,FSTYPE,SIZE,MOUNTPOINTS 2>/dev/null | awk '$2!=\"\"{printf \"%s: %s · %s · %s; \",$1,$2,$3,$4}'", FAST_TIMEOUT_SECONDS);
    String mounts = run("df -hT -x tmpfs -x devtmpfs 2>/dev/null | awk 'NR>1{printf \"%s: %s · %s/%s · %s; \",$7,$2,$4,$3,$6}'", FAST_TIMEOUT_SECONDS);
    success(successful, "STORAGE", blockDevices, filesystems);

    String ipv4 = run("ip -o -4 addr show scope global 2>/dev/null | awk 'NR==1{split($4,a,\"/\");print a[1]}'", FAST_TIMEOUT_SECONDS);
    String ipv6 = run("ip -o -6 addr show scope global 2>/dev/null | awk 'NR==1{split($4,a,\"/\");print a[1]}'", FAST_TIMEOUT_SECONDS);
    String mac = normalizeMac(run("ip -o link show 2>/dev/null | awk '$2!=\"lo:\" && /link\\/ether/{print $17;exit}'", FAST_TIMEOUT_SECONDS));
    String gateway = run("ip route show default 2>/dev/null | awk 'NR==1{print $3}'", FAST_TIMEOUT_SECONDS);
    String dns = run("(resolvectl dns 2>/dev/null || cat /etc/resolv.conf 2>/dev/null) | awk '/DNS Servers:/{for(i=3;i<=NF;i++)printf $i\" \"} /^nameserver/{printf $2\" \"}'", FAST_TIMEOUT_SECONDS);
    String adapters = run("ip -o -4 addr show scope global 2>/dev/null | awk '{printf \"%s %s; \",$2,$4}'", FAST_TIMEOUT_SECONDS);
    success(successful, "NETWORK", ipv4, mac);

    Software software = collectSoftware();
    String softwareCacheKey=firstNonBlank(ipv4, hostname);
    if(softwareCacheKey!=null && software.details()!=null && !software.details().isBlank()) latestSoftwareDetails.put(softwareCacheKey,software.details());
    if (software.total() > 0) successful.add("SOFTWARE"); else unavailable.add("SOFTWARE");

    String firewall = firstNonBlank(
        run("ufw status 2>/dev/null | head -n1", FAST_TIMEOUT_SECONDS),
        run("firewall-cmd --state 2>/dev/null", FAST_TIMEOUT_SECONDS),
        run("nft list ruleset 2>/dev/null | grep -qE 'hook input|hook forward' && echo 'nftables-Regeln vorhanden'", FAST_TIMEOUT_SECONDS));
    String appArmor = run("aa-status --enabled >/dev/null 2>&1 && echo aktiv || true", FAST_TIMEOUT_SECONDS);
    String selinux = firstNonBlank(run("getenforce 2>/dev/null", FAST_TIMEOUT_SECONDS), readFile("/sys/fs/selinux/enforce"));
    String secureBoot = run("if [ -d /sys/firmware/efi ]; then if command -v mokutil >/dev/null; then mokutil --sb-state 2>/dev/null; else echo 'UEFI aktiv, Secure-Boot-Status ohne mokutil nicht verfügbar'; fi; else echo 'Legacy-BIOS / kein UEFI'; fi", FAST_TIMEOUT_SECONDS);
    String encryption = run("lsblk -ln -o TYPE 2>/dev/null | grep -q '^crypt$' && echo 'Verschlüsseltes Blockgerät erkannt' || true", FAST_TIMEOUT_SECONDS);
    success(successful, "SECURITY", firewall, appArmor, selinux, secureBoot);

    String failedServices = run("systemctl --failed --no-legend --plain 2>/dev/null | awk '{printf $1\"; \"}'", FAST_TIMEOUT_SECONDS);
    String runningServices = run("systemctl list-units --type=service --state=running --no-legend --plain 2>/dev/null | awk '{printf $1\"; \"}'", FAST_TIMEOUT_SECONDS);
    String updates = detectUpdates();
    String upgradableNames = detectUpgradableNames();
    success(successful, "MAINTENANCE", runningServices, updates);

    // 40k36t12: Backend-lokal liefert dieselbe Datentraeger-/Verschluesselungstiefe wie SSH.
    String distroId = osRelease("ID");
    String packageManager = detectPackageManager();
    String rootSource = run("findmnt -n -o SOURCE / 2>/dev/null || awk '$2==\"/\"{print $1;exit}' /proc/mounts", FAST_TIMEOUT_SECONDS);
    String luksVolumes = run("lsblk -rno NAME,KNAME,TYPE,SIZE,FSTYPE,MOUNTPOINT,PKNAME 2>/dev/null | awk '$5==\"crypto_LUKS\"{printf \"%s|%s|%s|%s|%s|%s;\",$1,$2,$4,$5,$6,$7}'", FAST_TIMEOUT_SECONDS);
    String dmCrypt = run("dmsetup ls --target crypt 2>/dev/null | awk '{printf $1\";\"}'", FAST_TIMEOUT_SECONDS);
    String rootEncrypted = detectRootEncrypted(rootSource);
    String crypttab = run("awk 'NF && $1 !~ /^#/{printf \"%s|%s|%s|%s;\",$1,$2,$3,$4}' /etc/crypttab 2>/dev/null", FAST_TIMEOUT_SECONDS);
    String swapEncrypted = run("swapon --noheadings --raw --show=NAME 2>/dev/null | awk 'BEGIN{e=\"nein\"} /\\/dev\\/mapper\\//{e=\"ja\"} END{print e}'", FAST_TIMEOUT_SECONDS);
    String lvm = run("if command -v pvs >/dev/null 2>&1; then pvs --noheadings -o pv_name,vg_name 2>/dev/null | sed 's/^[[:space:]]*//'; else echo 'nicht installiert'; fi", FAST_TIMEOUT_SECONDS);
    String raid = run("if command -v mdadm >/dev/null 2>&1; then cat /proc/mdstat 2>/dev/null | tr '\\n' ' '; else echo 'kein mdadm-RAID erkannt'; fi", FAST_TIMEOUT_SECONDS);
    String btrfs = run("if command -v btrfs >/dev/null 2>&1; then btrfs filesystem show 2>/dev/null | tr '\\n' ' '; else echo 'nicht installiert'; fi", FAST_TIMEOUT_SECONDS);
    String zfs = run("if command -v zpool >/dev/null 2>&1; then zpool list -H 2>/dev/null | tr '\\n' ' '; else echo 'nicht installiert'; fi", FAST_TIMEOUT_SECONDS);
    String tpmCrypt = run("if [ -e /dev/tpm0 ] || [ -e /dev/tpmrm0 ]; then printf 'TPM vorhanden'; else printf 'TPM nicht erkannt'; fi; printf ', systemd-cryptenroll '; command -v systemd-cryptenroll >/dev/null 2>&1 && echo 'vorhanden' || echo 'nicht vorhanden'", FAST_TIMEOUT_SECONDS);
    String storageTools = run("for c in lsblk findmnt blkid cryptsetup dmsetup pvs vgs lvs mdadm btrfs zpool systemd-cryptenroll; do command -v $c >/dev/null 2>&1 && v=ja || v=nein; printf \"%s:%s;\" $c $v; done", FAST_TIMEOUT_SECONDS);
    String encryptionSummary = "LUKS:" + countEntries(luksVolumes) + ";geöffnet:" + countEntries(dmCrypt) + ";Root verschlüsselt:" + firstNonBlank(rootEncrypted, "unbekannt");

    String permissionUser = firstNonBlank(env("USER"), run("id -un 2>/dev/null", FAST_TIMEOUT_SECONDS));
    String permissionUid = run("id -u 2>/dev/null", FAST_TIMEOUT_SECONDS);
    String permissionGroups = run("id -Gn 2>/dev/null", FAST_TIMEOUT_SECONDS);
    String adminGroup = run("id -nG 2>/dev/null | grep -qwE 'sudo|wheel' && echo ja || echo nein", FAST_TIMEOUT_SECONDS);
    String sudoAvailable = commandExists("sudo") ? "ja" : "nein";
    String sudoNoPass = run("sudo -n true >/dev/null 2>&1 && echo ja || echo nein", FAST_TIMEOUT_SECONDS);
    String rootAccess = "0".equals(permissionUid) ? "ja" : "nein";

    String permissionMode = "ja".equals(rootAccess) ? "Root" :
        ("ja".equals(sudoNoPass) ? "Administrator" :
        ("ja".equals(adminGroup) ? "Erweitert" : "Standard"));
    String inventoryLevel = "ja".equals(rootAccess) ? "Vollzugriff" :
        ("ja".equals(sudoNoPass) ? "Administrator" :
        ("ja".equals(adminGroup) ? "Erweitert" : "Basis"));
    List<String> permissionLimitations = new ArrayList<>();
    if (!"ja".equals(rootAccess) && !"ja".equals(adminGroup)) {
      permissionLimitations.add("Benutzer ist keiner Administratorgruppe (sudo/wheel) zugeordnet");
    }
    if (!"ja".equals(rootAccess) && "ja".equals(sudoAvailable) && !"ja".equals(sudoNoPass)) {
      permissionLimitations.add("sudo erfordert interaktive Bestätigung bzw. ein Kennwort");
    }
    if (!"ja".equals(rootAccess) && !"ja".equals(sudoAvailable)) {
      permissionLimitations.add("sudo ist nicht verfügbar");
    }

    if (successful.isEmpty()) {
      diagnostic(diagnostics, sessionId, "LINUX_INVENTORY", "FAILED", "Linux-System lieferte keine verwertbaren Inventardaten", currentCount);
      return;
    }

    String displayManufacturer = join(" ", manufacturer, model, version);
    String protocol = "Linux-Inventarisierung / lokale Systemwerkzeuge und /proc-/sys-Dateien"
        + detail("Distribution", prettyName)
        + detail("Version", distroVersion)
        + detail("Kernel", kernel)
        + detail("Architektur", architecture)
        + detail("Desktop-Umgebung", desktop)
        + detail("Init-System", init)
        + detail("Laufzeit", uptime)
        + detail("Benutzer", env("USER"))
        + detail("System-UUID", uuid)
        + detail("BIOS/UEFI", bios)
        + detail("Mainboard", board)
        + detail("Prozessor", cpuModel)
        + detail("CPU", join(" · ", countLabel(sockets,"Sockel"), countLabel(cores,"Kerne je Sockel"), countLabel(threads,"Threads")))
        + detail("Virtualisierung", join(" / ", virtualization, hypervisor))
        + detail("Arbeitsspeicher", ramTotal)
        + detail("Verfügbarer RAM", ramAvailable)
        + detail("Swap", swap)
        + detail("RAM-Module", ramModules)
        + detail("Datenträger", blockDevices)
        + detail("Dateisysteme", filesystems)
        + detail("Mountpoints", mounts)
        + detail("Root-Dateisystem", rootSource)
        + detail("Verschlüsselungsübersicht", encryptionSummary)
        + detail("LUKS-Volumes", luksVolumes)
        + detail("DM-Crypt", dmCrypt)
        + detail("Root-Verschlüsselung", rootEncrypted)
        + detail("Crypttab", crypttab)
        + detail("Verschlüsselter Swap", swapEncrypted)
        + detail("LVM", lvm)
        + detail("Software-RAID", raid)
        + detail("Btrfs", btrfs)
        + detail("ZFS", zfs)
        + detail("TPM / systemd-cryptenroll", tpmCrypt)
        + detail("Storage-Werkzeuge", storageTools)
        + detail("IPv6", ipv6)
        + detail("Standardgateway", gateway)
        + detail("DNS-Server", dns)
        + detail("Netzwerkadapter", adapters)
        + detail("Distributions-ID", distroId)
        + detail("Distributions-Version", distroVersion)
        + detail("Paketmanager", packageManager)
        + detail("Installierte Software", software.total() + " eindeutige Pakete" + (software.names().isBlank() ? "" : " – " + software.names()))
        + detail("Softwarequellen", software.sources())
        + detail("Softwareinventar", software.total() + " Softwareeinträge insgesamt strukturiert gespeichert")
        + detail("Firewall", firewall)
        + detail("AppArmor", appArmor)
        + detail("SELinux", selinux)
        + detail("Secure Boot", secureBoot)
        + detail("Datenträgerverschlüsselung", encryption)
        + detail("Berechtigungs-Benutzer", permissionUser)
        + detail("Benutzer-UID", permissionUid)
        + detail("Benutzergruppen", permissionGroups)
        + detail("Administratorgruppe", adminGroup)
        + detail("sudo verfügbar", sudoAvailable)
        + detail("sudo ohne Kennwort", sudoNoPass)
        + detail("Root aktiv", rootAccess)
        + detail("Berechtigungsmodus", permissionMode)
        + detail("Inventarisierungstiefe", inventoryLevel)
        + detail("Berechtigungseinschränkungen", permissionLimitations.isEmpty() ? "keine" : String.join("; ", permissionLimitations))
        + detail("Laufende Dienste", runningServices)
        + detail("Fehlgeschlagene Dienste", failedServices)
        + detail("Verfügbare Updates", updates)
        + detail("Aktualisierbare Pakete", upgradableNames)
        + detail("Erfolgreiche Module", String.join(", ", successful))
        + detail("Nicht verfügbare Module", unavailable.isEmpty() ? "keine" : String.join(", ", unavailable));

    out.accept(new DiscoveredDevice("linux-local:" + stableId(hostname, serial, mac), hostname, "Linux-PC", ipv4, mac,
        protocol, "ONLINE", displayManufacturer, serial, now, false));

    diagnostic(diagnostics, sessionId, "LINUX_INVENTORY", unavailable.isEmpty() ? "COMPLETED" : "PARTIAL",
        firstNonBlank(prettyName, distro) + " · " + successful.size() + " Module erfolgreich" + (unavailable.isEmpty() ? "" : " · " + unavailable.size() + " nicht verfügbar"), currentCount + 1);
  }

  private Software collectSoftware() {
    Set<String> names = new LinkedHashSet<>();
    List<String> sourceCounts = new ArrayList<>();
    List<String> details = new ArrayList<>();
    addPackages(names, sourceCounts, details, "APT / dpkg",
      "dpkg-query -W -f='${binary:Package}|${Version}|${binary:Package}|\n' 2>/dev/null");
    addPackages(names, sourceCounts, details, "RPM",
      "rpm -qa --qf '%{NAME}|%{VERSION}-%{RELEASE}|%{NAME}|\n' 2>/dev/null");
    addPackages(names, sourceCounts, details, "Pacman",
      "pacman -Q 2>/dev/null | awk '{print $1\"|\"$2\"|\"$1\"|\"}'");
    addPackages(names, sourceCounts, details, "APK",
      "apk info -v 2>/dev/null | awk '{print $1\"|||\"}'");
    addPackages(names, sourceCounts, details, "Flatpak",
      "flatpak list --app --columns=application,version,ref 2>/dev/null | awk -F'\\t' '{print $1\"|\"$2\"|\"$3\"|\"}'");
    addPackages(names, sourceCounts, details, "Snap",
      "snap list 2>/dev/null | awk 'NR>1{print $1\"|\"$2\"|\"$1\"|\"}'");
    return new Software(names.size(), String.join("; ", sourceCounts), String.join("; ", names), String.join("\u001e", details));
  }

  private void addPackages(Set<String> names, List<String> sourceCounts, List<String> details, String label, String command) {
    List<String> rows = runLines(command, SOFTWARE_TIMEOUT_SECONDS);
    if (rows.isEmpty()) return;
    int before = names.size();
    for(String raw:rows){
      String line=raw==null?"":raw.trim();
      if(line.isBlank()) continue;
      String[] c=line.split("\\|",-1);
      String name=c.length>0?c[0].trim():"";
      if(name.isBlank()) continue;
      names.add(name);
      String version=c.length>1?c[1].trim():"";
      String pkg=c.length>2?c[2].trim():name;
      String path=c.length>3?c[3].trim():"";
      details.add(label+"|"+name+"|"+version+"|"+pkg+"|"+path);
    }
    sourceCounts.add(label + ": " + (names.size() - before));
  }

  private String detectUpdates() {
    if (commandExists("apt")) return run("apt list --upgradable 2>/dev/null | awk 'NR>1{n++} END{print n+0 \" Pakete\"}'", SOFTWARE_TIMEOUT_SECONDS);
    if (commandExists("dnf")) return run("dnf -q check-update 2>/dev/null | awk 'NF>=3 && $1 !~ /^Last/{n++} END{print n+0 \" Pakete\"}'", SOFTWARE_TIMEOUT_SECONDS);
    if (commandExists("pacman")) return run("checkupdates 2>/dev/null | wc -l | awk '{print $1 \" Pakete\"}'", SOFTWARE_TIMEOUT_SECONDS);
    if (commandExists("zypper")) return run("zypper -q lu 2>/dev/null | awk '/^v |^  v/{n++} END{print n+0 \" Pakete\"}'", SOFTWARE_TIMEOUT_SECONDS);
    if (commandExists("apk")) return run("apk version -l '<' 2>/dev/null | wc -l | awk '{print $1 \" Pakete\"}'", SOFTWARE_TIMEOUT_SECONDS);
    return null;
  }


  // 40k36t12: Wartungs-/Storage-Hilfswerte parallel zum SSH-Pfad.
  private String detectUpgradableNames() {
    if (commandExists("apt")) return run("apt list --upgradable 2>/dev/null | sed '1d' | cut -d/ -f1 | head -n 20 | paste -sd, -", SOFTWARE_TIMEOUT_SECONDS);
    if (commandExists("dnf")) return run("dnf -q check-update 2>/dev/null | awk 'NF>=3 && $1 !~ /^Last/{print $1}' | head -n 20 | paste -sd, -", SOFTWARE_TIMEOUT_SECONDS);
    if (commandExists("pacman")) return run("checkupdates 2>/dev/null | awk '{print $1}' | head -n 20 | paste -sd, -", SOFTWARE_TIMEOUT_SECONDS);
    return null;
  }

  private String detectPackageManager() {
    for (String c : List.of("apt","dnf","yum","pacman","zypper","apk")) if (commandExists(c)) return c;
    return "unbekannt";
  }

  private String detectRootEncrypted(String rootSource) {
    String root = clean(rootSource);
    if (root == null) return "unbekannt";
    if (root.startsWith("/dev/mapper/") || root.startsWith("/dev/dm-")) return "ja";
    String probe = run("src=" + shellQuote(root) + "; cur=$src; i=0; while [ -n \"$cur\" ] && [ $i -lt 8 ]; do fs=$(lsblk -nro FSTYPE \"$cur\" 2>/dev/null | head -n1); [ \"$fs\" = crypto_LUKS ] && { echo ja; exit; }; pk=$(lsblk -nro PKNAME \"$cur\" 2>/dev/null | head -n1); [ -z \"$pk\" ] && break; cur=/dev/$pk; i=$((i+1)); done; echo nein", FAST_TIMEOUT_SECONDS);
    return firstNonBlank(probe, "nein");
  }

  private static int countEntries(String value) {
    String v = clean(value);
    if (v == null) return 0;
    int n=0; for (String s : v.split(";")) if (!s.isBlank()) n++;
    return n;
  }

  private static String shellQuote(String value) {
    if (value == null) return "''";
    return "'" + value.replace("'", "'\\''") + "'";
  }

  private static String osRelease(String key) {
    String value = run("awk -F= '$1==\"" + key + "\"{sub(/^[^=]*=/,\"\");gsub(/^\"|\"$/,\"\");print;exit}' /etc/os-release 2>/dev/null", FAST_TIMEOUT_SECONDS);
    return clean(value);
  }
  private static String detectInit() { return firstNonBlank(run("ps -p 1 -o comm= 2>/dev/null", FAST_TIMEOUT_SECONDS), "unbekannt"); }
  private static String readFile(String path) { return clean(run("cat '" + path.replace("'", "") + "' 2>/dev/null", FAST_TIMEOUT_SECONDS)); }
  private static String env(String name) { return clean(System.getenv(name)); }
  private static void success(List<String> successful, String module, String... values) { for (String v : values) if (v != null && !v.isBlank()) { successful.add(module); return; } }
  private static String run(String command, long timeoutSeconds) {
    List<String> lines = runLines(command, timeoutSeconds);
    String value = clean(String.join(" ", lines));
    return value == null ? "" : value;
  }
  private static List<String> runLines(String command, long timeoutSeconds) {
    List<String> result = new ArrayList<>();
    try {
      Process process = new ProcessBuilder("sh", "-c", command).redirectErrorStream(true).start();
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
        String line; while ((line = reader.readLine()) != null) result.add(line);
      }
      if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) { process.destroyForcibly(); return List.of(); }
      return result;
    } catch (Exception ignored) { return List.of(); }
  }
  private static boolean commandExists(String command) {
    String result = run("command -v " + command + " 2>/dev/null", 2);
    return result != null && !result.isBlank();
  }
  private static boolean isLinux() { return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("linux"); }
  private static String localHostName() { try { return InetAddress.getLocalHost().getHostName(); } catch (Exception ignored) { return "Linux-PC"; } }
  private static String clean(String value) { if (value == null) return null; String v = value.trim().replaceAll("\\s+", " "); return v.isBlank() ? null : v; }
  private static String firstNonBlank(String... values) { for (String value : values) { String v = clean(value); if (v != null) return v; } return null; }
  private static String normalizeMac(String value) { String v = clean(value); return v == null ? null : v.replace('-', ':').toUpperCase(Locale.ROOT); }
  private static String join(String separator, String... values) { List<String> result = new ArrayList<>(); for (String value : values) { String v=clean(value); if(v!=null) result.add(v); } return result.isEmpty()?null:String.join(separator,result); }
  private static String countLabel(String value, String unit) { String v=clean(value); return v==null?null:v+" "+unit; }
  private static String detail(String label, String value) { String v=clean(value); return v==null?"":" · "+label+": "+v; }
  private static String stableId(String... values) {
    try { MessageDigest md=MessageDigest.getInstance("SHA-256"); for(String value:values) if(value!=null) md.update(value.getBytes(StandardCharsets.UTF_8)); byte[] d=md.digest(); StringBuilder b=new StringBuilder(); for(int i=0;i<8;i++) b.append(String.format("%02x",d[i])); return b.toString(); }
    catch(Exception e){ return Integer.toHexString(String.join("|",values).hashCode()); }
  }
  private static void diagnostic(Consumer<DeviceDiscoveryService.DiscoveryDiagnosticEvent> diagnostics, String sessionId, String phase, String status, String message, int count) {
    diagnostics.accept(new DeviceDiscoveryService.DiscoveryDiagnosticEvent(sessionId, java.time.OffsetDateTime.now().toString(), phase, status, message, count));
  }
  private record Software(int total, String sources, String names, String details) {}
}
