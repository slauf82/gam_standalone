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
    String ramModules = run("(command -v dmidecode >/dev/null && dmidecode -t memory 2>/dev/null | awk '/^[[:space:]]*Size:/{if($2!=\"No\"&&$2!=\"0\")print $2\" \"$3}' | paste -sd '; ' -) || true", FAST_TIMEOUT_SECONDS);
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
    String adapters = run("ip -o link show 2>/dev/null | awk -F': ' '$2!=\"lo\"{split($2,a,\"@\");printf a[1]\"; \"}'", FAST_TIMEOUT_SECONDS);
    success(successful, "NETWORK", ipv4, mac);

    Software software = collectSoftware();
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
    String runningServices = run("systemctl list-units --type=service --state=running --no-legend --plain 2>/dev/null | wc -l", FAST_TIMEOUT_SECONDS);
    String updates = detectUpdates();
    success(successful, "MAINTENANCE", runningServices, updates);

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
        + detail("IPv6", ipv6)
        + detail("Standardgateway", gateway)
        + detail("DNS-Server", dns)
        + detail("Netzwerkadapter", adapters)
        + detail("Installierte Software", software.total() + " eindeutige Pakete" + (software.names().isBlank() ? "" : " – " + software.names()))
        + detail("Softwarequellen", software.sources())
        + detail("Firewall", firewall)
        + detail("AppArmor", appArmor)
        + detail("SELinux", selinux)
        + detail("Secure Boot", secureBoot)
        + detail("Datenträgerverschlüsselung", encryption)
        + detail("Laufende Dienste", runningServices == null ? null : runningServices + " Dienste")
        + detail("Fehlgeschlagene Dienste", failedServices)
        + detail("Verfügbare Updates", updates)
        + detail("Erfolgreiche Module", String.join(", ", successful))
        + detail("Nicht verfügbare Module", unavailable.isEmpty() ? null : String.join(", ", unavailable));

    out.accept(new DiscoveredDevice("linux-local:" + stableId(hostname, serial, mac), hostname, "Linux-PC", ipv4, mac,
        protocol, "ONLINE", displayManufacturer, serial, now, false));

    diagnostic(diagnostics, sessionId, "LINUX_INVENTORY", unavailable.isEmpty() ? "COMPLETED" : "PARTIAL",
        firstNonBlank(prettyName, distro) + " · " + successful.size() + " Module erfolgreich" + (unavailable.isEmpty() ? "" : " · " + unavailable.size() + " nicht verfügbar"), currentCount + 1);
  }

  private Software collectSoftware() {
    Set<String> names = new LinkedHashSet<>();
    List<String> sourceCounts = new ArrayList<>();
    addPackages(names, sourceCounts, "dpkg", "dpkg-query -W -f='${binary:Package}\\n' 2>/dev/null");
    addPackages(names, sourceCounts, "rpm", "rpm -qa --qf '%{NAME}\\n' 2>/dev/null");
    addPackages(names, sourceCounts, "pacman", "pacman -Qq 2>/dev/null");
    addPackages(names, sourceCounts, "apk", "apk info 2>/dev/null");
    addPackages(names, sourceCounts, "Flatpak", "flatpak list --app --columns=application 2>/dev/null");
    addPackages(names, sourceCounts, "Snap", "snap list 2>/dev/null | awk 'NR>1{print $1}'");
    return new Software(names.size(), String.join("; ", sourceCounts), String.join("; ", names));
  }

  private void addPackages(Set<String> names, List<String> sourceCounts, String label, String command) {
    List<String> rows = runLines(command, SOFTWARE_TIMEOUT_SECONDS);
    if (rows.isEmpty()) return;
    int before = names.size();
    rows.stream().map(String::trim).filter(s -> !s.isBlank()).forEach(names::add);
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

  private static String osRelease(String key) {
    String value = run("awk -F= '$1==\"" + key + "\"{sub(/^[^=]*=/,\"\");gsub(/^\"|\"$/,\"\");print;exit}' /etc/os-release 2>/dev/null", FAST_TIMEOUT_SECONDS);
    return clean(value);
  }
  private static String detectInit() { return firstNonBlank(run("ps -p 1 -o comm= 2>/dev/null", FAST_TIMEOUT_SECONDS), "unbekannt"); }
  private static String readFile(String path) { return clean(run("cat '" + path.replace("'", "") + "' 2>/dev/null", FAST_TIMEOUT_SECONDS)); }
  private static String env(String name) { return clean(System.getenv(name)); }
  private static void success(List<String> successful, String module, String... values) { for (String v : values) if (v != null && !v.isBlank()) { successful.add(module); return; } }
  private static String run(String command, long timeoutSeconds) { List<String> lines = runLines(command, timeoutSeconds); return clean(String.join(" ", lines)); }
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
  private static boolean commandExists(String command) { return !run("command -v " + command + " 2>/dev/null", 2).isBlank(); }
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
  private record Software(int total, String sources, String names) {}
}
