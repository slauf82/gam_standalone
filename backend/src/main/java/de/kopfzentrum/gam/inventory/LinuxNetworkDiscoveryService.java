package de.kopfzentrum.gam.inventory;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Schritt 40k33b: Erkennung und Inventarisierung von Linux-Systemen im Netzwerk.
 *
 * Ohne Zugangsdaten werden SSH-Banner sowie typische Linux-/Server-Dienste erkannt.
 * Optional kann GAM mit einem gemeinsamen SSH-Benutzer und SSH-Schlüssel eine
 * vertiefte, ausschließlich lesende Inventarisierung durchführen. Konfiguration:
 * gam.discovery.linux.ssh-user / GAM_LINUX_SSH_USER
 * gam.discovery.linux.ssh-key  / GAM_LINUX_SSH_KEY
 * gam.discovery.linux.ssh-port / GAM_LINUX_SSH_PORT (Standard 22)
 *
 * Schritt 40k33b3: Ergänzt eine gezielte Linux-Nachprüfung, die zusätzlich zur
 * eigenen Netzwerkprüfung auch bereits von anderen Quellen bekannte Hinweise
 * (Hostname, Fingerabdrücke) als Auslöser berücksichtigt. WICHTIG: Ein reiner
 * "Linux"-Hinweis (z.B. Hostname beginnt mit "linux-") löst nur die Nachprüfung
 * aus - er begründet für sich genommen noch keine Kategorie. Eine automatische
 * Einstufung als Computer/Notebook erfolgt erst bei einem zusätzlichen,
 * eindeutigen Client-Indiz und nur, wenn keine eindeutige Serverrolle
 * (Proxmox, Hypervisor, Container-Host, Server-Chassis, expliziter
 * "Server"-Hinweis) vorliegt. Samba/NFS allein zählen dabei ausdrücklich
 * NICHT als eindeutiger Serverbeleg. Ohne eindeutigen Beleg in beide
 * Richtungen bleibt der bisherige konservative Standard ("Server / Linux-System")
 * unverändert bestehen - bestehende automatische Kategorien werden also nur bei
 * klarer neuer technischer Erkenntnis korrigiert.
 */
@Service
public class LinuxNetworkDiscoveryService {
  private static final Logger log = LoggerFactory.getLogger(LinuxNetworkDiscoveryService.class);
  private static final int CONNECT_TIMEOUT_MS = 450;
  private static final int MAX_TARGETS = 1024;
  // HP-Geschäftsnotebooks (EliteBook/ProBook) folgen dem Namensschema NNNN[b|p|s|w],
  // z.B. 6460b, 8470p, 2570p. Bewusst eng gefasst als EIN zusätzliches Client-Indiz
  // unter mehreren - keine vollständige Modell-Datenbank, nur ein bekanntes Muster.
  private static final Pattern HP_BUSINESS_LAPTOP_MODEL = Pattern.compile("\\d{3,4}[bpsw]");
  private final Environment env;
  private final LinuxSshSettingsRepository sshSettings;

  public LinuxNetworkDiscoveryService(Environment env, LinuxSshSettingsRepository sshSettings) { this.env = env; this.sshSettings = sshSettings; }

  /**
   * Ein zu prüfendes Ziel samt der bereits von anderen Quellen bekannten Hinweise.
   * hostnameHint: bereits bekannter Gerätename (für "beginnt mit"/"enthält"-Prüfung).
   * fingerprintHint: weitere bereits bekannte Texte (Typ, Protokoll anderer Quellen),
   * die auf Linux-typische Fingerabdrücke hin durchsucht werden.
   * manualCategory: true, wenn für diese Identität bereits eine manuell gesetzte
   * Kategorie existiert (nur für die Protokollierung relevant; die eigentliche
   * Schreibsperre besteht bereits unverändert in DiscoveryRegistrationRepository).
   */
  public record LinuxScanTarget(String ip, String hostnameHint, String fingerprintHint, boolean manualCategory) {}

  private record ClassificationResult(String category, String reason) {}

  public void scan(boolean enabled, Collection<LinuxScanTarget> rawTargets, String now,
                   Consumer<DiscoveredDevice> out,
                   Consumer<DeviceDiscoveryService.DiscoveryDiagnosticEvent> diagnostics,
                   String sessionId, int currentCount) {
    if (!enabled) {
      diagnostic(diagnostics, sessionId, "LINUX_NETWORK", "SKIPPED", "Linux-Netzwerkerkennung ist deaktiviert", currentCount);
      return;
    }
    LinkedHashMap<String,LinuxScanTarget> byIp = new LinkedHashMap<>();
    for (LinuxScanTarget t : rawTargets) {
      if (t == null || t.ip() == null) continue;
      String ip = t.ip().trim();
      if (!isIpv4(ip)) continue;
      byIp.putIfAbsent(ip, new LinuxScanTarget(ip, nullToEmpty(t.hostnameHint()), nullToEmpty(t.fingerprintHint()), t.manualCategory()));
    }
    List<LinuxScanTarget> targets = byIp.values().stream().limit(MAX_TARGETS).toList();
    if (targets.isEmpty()) {
      diagnostic(diagnostics, sessionId, "LINUX_NETWORK", "SKIPPED", "Keine IPv4-Ziele für die Linux-Netzwerkerkennung vorhanden", currentCount);
      return;
    }

    LinuxSshSettingsRepository.Settings ssh = sshSettings.load();
    String sshUser = ssh.username(); String sshKey = ssh.keyPath(); int sshPort = ssh.port();
    boolean deepInventory = ssh.enabled() && !sshUser.isBlank() && commandExists("ssh") && (!"PASSWORD".equals(ssh.authMode()) || commandExists("sshpass"));
    diagnostic(diagnostics, sessionId, "LINUX_NETWORK", "RUNNING",
      "Linux-Netzwerkerkennung prüft " + targets.size() + " IPv4-Ziele" + (deepInventory ? " einschließlich SSH-Inventarisierung" : " per Dienst-Fingerprinting"), currentCount);

    ExecutorService pool = Executors.newFixedThreadPool(Math.min(16, Math.max(4, Runtime.getRuntime().availableProcessors())));
    List<Future<Optional<DiscoveredDevice>>> futures = new ArrayList<>();
    for (LinuxScanTarget target : targets) futures.add(pool.submit(() -> inspect(target, sshPort, sshUser, sshKey, deepInventory, now)));
    pool.shutdown();

    int detected = 0, deep = 0, clientCorrected = 0, manualUnchanged = 0;
    for (Future<Optional<DiscoveredDevice>> future : futures) {
      try {
        Optional<DiscoveredDevice> result = future.get(14, TimeUnit.SECONDS);
        if (result.isPresent()) {
          DiscoveredDevice device = result.get();
          out.accept(device); detected++;
          if (device.protocol() != null && device.protocol().contains("SSH-Inventar")) deep++;
          if (device.type() != null && device.type().startsWith("Computer")) clientCorrected++;
          if (device.protocol() != null && device.protocol().contains("Manuelle Kategorie")) manualUnchanged++;
        }
      } catch (Exception ignored) { future.cancel(true); }
    }
    pool.shutdownNow();
    String status = detected == 0 ? "COMPLETED" : (deepInventory && deep < detected ? "PARTIAL" : "COMPLETED");
    diagnostic(diagnostics, sessionId, "LINUX_NETWORK", status,
      detected + " Linux-/Unix-Systeme erkannt, davon " + deep + " vertieft per SSH inventarisiert, "
        + clientCorrected + " als Computer/Notebook eingestuft, " + manualUnchanged + " mit unangetasteter manueller Kategorie",
      currentCount + detected);
  }

  /**
   * 40k33b8: Führt dieselbe Nachprüfung/Inventarisierung wie scan() aus, aber gezielt
   * für EIN einzelnes, bereits bekanntes Gerät - ohne einen kompletten Suchlauf
   * anzustoßen. Wird von den neuen manuellen Aktionen im Dialog "Geräteidentität"
   * verwendet ("Linux-Inventarisierung starten"/"Inventarisierung erneut
   * durchführen"). Reine Wiederverwendung von inspect() - keine zweite Logik.
   */
  public Optional<DiscoveredDevice> inspectSingle(String ip, String hostnameHint, String fingerprintHint, boolean manualCategory) {
    if (!isIpv4(ip)) return Optional.empty();
    LinuxSshSettingsRepository.Settings ssh = sshSettings.load();
    String sshUser = ssh.username(); String sshKey = ssh.keyPath(); int sshPort = ssh.port();
    boolean deepInventory = ssh.enabled() && !sshUser.isBlank() && commandExists("ssh") && (!"PASSWORD".equals(ssh.authMode()) || commandExists("sshpass"));
    log.debug("Manuelle Linux-Inventarisierung für {} angestoßen (SSH konfiguriert: {})", ip, deepInventory);
    LinuxScanTarget target = new LinuxScanTarget(ip.trim(), nullToEmpty(hostnameHint), nullToEmpty(fingerprintHint), manualCategory);
    return inspect(target, sshPort, sshUser, sshKey, deepInventory, OffsetDateTime.now().toString());
  }

  /** 40k33b8: Ergebnis eines reinen SSH-Verbindungstests, ohne vollständige Inventarisierung. */
  public record SshTestResult(boolean sshConfigured, boolean reachable, String message, String checkedAt) {}

  public SshTestResult testSshConnection(String ip) {
    LinuxSshSettingsRepository.Settings ssh=sshSettings.load(); String now=OffsetDateTime.now().toString();
    if(!ssh.enabled()||ssh.username().isBlank()) return new SshTestResult(false,false,"SSH-Zugang ist noch nicht konfiguriert.",now);
    if(!commandExists("ssh")) return new SshTestResult(true,false,"Auf dem GAM-Server wurde kein SSH-Client gefunden.",now);
    String banner=readBanner(ip,ssh.port());
    if(banner==null) return new SshTestResult(true,false,"Host erreichbar, aber SSH-Port "+ssh.port()+" ist geschlossen oder der SSH-Dienst ist deaktiviert.",now);
    if("PASSWORD".equals(ssh.authMode())&&!commandExists("sshpass")) return new SshTestResult(true,false,"SSH-Port ist erreichbar, aber Passwortanmeldung benötigt auf dem GAM-Server das Werkzeug sshpass. Bitte Schlüsselanmeldung verwenden oder sshpass installieren.",now);
    String output=runRemoteCommand(ip,ssh.port(),ssh.username(),ssh.keyPath(),ssh.password(),ssh.authMode(),"printf GAM_SSH_OK",8);
    boolean ok=output!=null&&output.contains("GAM_SSH_OK");
    return new SshTestResult(true,ok,ok?"SSH-Verbindung und Authentifizierung erfolgreich.":"SSH-Port erreichbar, aber Authentifizierung fehlgeschlagen. Bitte Benutzername und Schlüssel/Passwort prüfen.",now);
  }

  /** 40k33b8: Verwirft den On-Demand-Zwischenspeicher (40k33b6b) für ein einzelnes Gerät ("Cache aktualisieren"). */
  public void invalidateCache(String ip) {
    onDemandCache.keySet().removeIf(key -> key.startsWith(ip + "|"));
    log.debug("On-Demand-Zwischenspeicher für {} wurde geleert", ip);
  }

  private Optional<DiscoveredDevice> inspect(LinuxScanTarget target, int sshPort, String sshUser, String sshKey, boolean deepInventory, String now) {
    String ip = target.ip();
    String sshBanner = readBanner(ip, sshPort);
    LinkedHashSet<String> services = new LinkedHashSet<>();
    if (sshBanner != null) services.add("SSH " + clean(sshBanner));
    probe(ip, 111, "rpcbind/NFS", services);
    probe(ip, 2049, "NFS", services);
    probe(ip, 445, "Samba/SMB", services);
    probe(ip, 9090, "Cockpit", services);
    probe(ip, 8006, "Proxmox VE", services);
    probe(ip, 8123, "Home Assistant", services);
    probe(ip, 3000, "Grafana", services);
    probe(ip, 9000, "Portainer", services);
    probe(ip, 53, "DNS/Pi-hole/AdGuard", services);
    // 40k33b6a: zusätzliche, credential-freie Rollen-Indizien (Webserver/Datenbank).
    probe(ip, 80, "HTTP/Webserver", services);
    probe(ip, 443, "HTTPS/Webserver", services);
    probe(ip, 3306, "MySQL/MariaDB", services);
    probe(ip, 5432, "PostgreSQL", services);
    // 40k33b9: weitere credential-freie Rollen-Indizien für die Linux-Inventarisierung ohne SSH.
    probe(ip, 631, "IPP/CUPS-Druckserver", services);
    probe(ip, 2375, "Docker-API (ungesichert)", services);
    probe(ip, 10000, "Webmin", services);

    // 40k33b9: Bereits offene Web-Ports werden zusätzlich per einfachem HTTP-Abruf geprüft, um
    // bekannte Linux-Weboberflächen zu erkennen (Cockpit/CasaOS/OpenMediaVault/Portainer/
    // Webmin/Nextcloud/Grafana/Prometheus/Proxmox/Home Assistant) - reine Auswertung von Text,
    // der ohnehin schon abgerufen wird, keine neue Discovery-Quelle, kein neuer Netzwerkzugriff
    // über das hinaus, was ein normaler Webbrowser beim Aufruf der Seite auch täte.
    for (int webPort : new int[]{80, 443, 9090, 8006, 8123, 3000, 9000, 10000}) {
      boolean open = services.stream().anyMatch(s -> s.contains("(TCP " + webPort + ")"));
      if (open) httpProbe(ip, webPort, webPort == 443, services);
    }

    String hostname = clean(target.hostnameHint());
    String hostnameLower = hostname == null ? "" : hostname.toLowerCase(Locale.ROOT);
    String fingerprintLower = nullToEmpty(clean(target.fingerprintHint())).toLowerCase(Locale.ROOT);
    // 40k33b9: bereits von SNMP gemeldete Systembeschreibung (sysDescr) - eine ECHTE, von der
    // Zielmaschine selbst gemeldete Zeichenkette, keine Schätzung. Wird sowohl als zusätzlicher
    // Auslöser als auch als Fallback-Quelle für Kernel/Hostname ohne SSH genutzt.
    String snmpSysDescr = extractSnmpSysDescr(target.fingerprintHint());

    // 40k33b3: Auslöser für die gezielte Linux-Nachprüfung sammeln. Ein Hostname-Hinweis
    // löst hier NUR die Nachprüfung aus, er entscheidet noch nicht über die Kategorie.
    List<String> triggers = new ArrayList<>();
    if (hostnameLower.startsWith("linux-")) triggers.add("Hostname beginnt mit \"linux-\"");
    else if (hostnameLower.contains("linux")) triggers.add("Hostname enthält \"linux\"");
    if (containsAny(fingerprintLower, "linux", "ubuntu", "debian", "raspbian", "fedora", "centos", "alpine", "opensuse", "openssh", "systemd"))
      triggers.add("Von anderer Quelle bereits bekannter Linux-Fingerabdruck");
    if (sshBanner != null) triggers.add("SSH-Port 22 erreichbar");
    if (services.stream().anyMatch(s -> s.contains("Cockpit"))) triggers.add("Cockpit-Port 9090 erreichbar");
    if (fingerprintLower.contains("mdns") && containsAny(fingerprintLower, "linux", "_workstation._tcp", "_ssh._tcp", "avahi"))
      triggers.add("Linux-typischer mDNS-/Service-Hinweis");
    if (services.stream().anyMatch(s -> s.contains("NFS") || s.contains("Proxmox") || s.contains("Portainer") || s.contains("Home Assistant")))
      triggers.add("Linux-typischer Netzwerkdienst erkannt");
    if (snmpSysDescr != null && parseSnmpLinuxHostKernel(snmpSysDescr) != null)
      triggers.add("Von SNMP bereits gemeldete Linux-Systembeschreibung");
    if (triggers.isEmpty()) return Optional.empty();
    log.debug("Linux-Nachprüfung für {} ausgelöst durch: {}", ip, String.join(" · ", triggers));

    long inventoryStartedAt = System.currentTimeMillis();
    Map<String,String> inventory = Map.of();
    String inventoryStatus;
    if (!deepInventory) {
      inventoryStatus = "SSH-Zugang nicht konfiguriert (gam.discovery.linux.ssh-user/-key)";
      log.debug("Linux-Inventarisierung für {} übersprungen: kein SSH-Zugang konfiguriert (normaler Zustand, SSH ist optional)", ip);
    } else if (sshBanner == null) {
      inventoryStatus = "SSH-Port nicht erreichbar";
      log.debug("Linux-Inventarisierung für {} übersprungen: SSH-Port {} nicht erreichbar", ip, sshPort);
    } else {
      log.debug("SSH-Verbindung zu {} wird für die Inventarisierung aufgebaut (Port {})", ip, sshPort);
      inventory = sshInventory(ip, sshPort, sshUser, sshKey);
      if (inventory.isEmpty()) {
        inventoryStatus = "SSH erreichbar, aber Anmeldung fehlgeschlagen oder keine Daten empfangen";
        log.debug("Linux-Inventarisierung für {} lieferte keine Daten (SSH-Anmeldung fehlgeschlagen oder leere Antwort)", ip);
      } else {
        inventoryStatus = "erfolgreich";
        log.debug("Linux-Inventarisierung für {} erfolgreich: {} Merkmale erfasst ({})", ip, inventory.size(),
          String.join(", ", inventory.keySet()));
      }
    }
    long inventoryDurationMs = System.currentTimeMillis() - inventoryStartedAt;
    // 40k33b9: echte (nicht geschätzte) SNMP-Angaben als Fallback nutzen, wenn SSH keine
    // eigenen Werte geliefert hat - ausschließlich, wenn das Muster "Linux <Host> <Kernel>"
    // tatsächlich in der von der Zielmaschine selbst gemeldeten sysDescr steckt.
    String[] snmpHostKernel = snmpSysDescr != null ? parseSnmpLinuxHostKernel(snmpSysDescr) : null;
    String hostLabel = firstNonBlank(inventory.get("HOSTNAME"), snmpHostKernel != null ? snmpHostKernel[0] : null, hostname, reverseName(ip), "Linux-System " + ip);
    String os = firstNonBlank(inventory.get("OS"), inferOs(services, sshBanner), "Linux / Unix");
    String kernelFallback = inventory.get("KERNEL") != null ? null : (snmpHostKernel != null ? "Linux " + snmpHostKernel[1] + " (laut SNMP-Systembeschreibung)" : null);
    String serial = firstNonBlank(inventory.get("UUID"), inventory.get("SERIAL"));
    String manufacturer = firstNonBlank(inventory.get("VENDOR"), inferManufacturer(services));

    String chassisRole = chassisRole(inventory.get("CHASSIS"));
    String clientEvidence = detectClientEvidence(hostnameLower, fingerprintLower, chassisRole);
    ClassificationResult classification = classify(services, inventory, hostnameLower, fingerprintLower, clientEvidence, chassisRole);
    String type = classification.category();
    String roles = detectRoles(services, inventory, !inventory.isEmpty());

    // 40k33b9: Quellenübersicht - welche bereits vorhandenen Discoveryquellen haben zu dieser
    // Einstufung beigetragen? Rein informativ, reine Auswertung bereits vorhandener Daten.
    List<String> usedSources = new ArrayList<>();
    if (services.stream().anyMatch(s -> s.contains("(TCP "))) usedSources.add("Netzwerkscanner (offene Ports)");
    if (services.stream().anyMatch(s -> s.contains("(HTTP"))) usedSources.add("HTTP/HTTPS-Weboberflächen");
    if (snmpSysDescr != null) usedSources.add("SNMP");
    if (fingerprintLower.contains("mdns") || fingerprintLower.contains("avahi")) usedSources.add("mDNS/Avahi");
    if (containsAny(fingerprintLower, "home assistant")) usedSources.add("Home Assistant");
    if (!inventory.isEmpty()) usedSources.add("SSH");
    List<String> sshOnlyFields = List.of("Distribution/Kernel (exakt statt geschätzt)", "Arbeitsspeicher/CPU im Detail",
      "Installierte Pakete", "Laufende Dienste (vollständig)", "Dateisysteme/Mountpoints", "SSH-Hostkey");

    // 40k33b9: Inventarisierungsgrad - wie viele der bekannten Informationskategorien sind
    // (mit oder ohne SSH) tatsächlich befüllt? Rein informativ, keine Bewertung "gut/schlecht".
    List<Boolean> completenessChecklist = List.of(
      hostname != null || inventory.get("HOSTNAME") != null,
      os != null,
      inventory.get("KERNEL") != null || kernelFallback != null,
      inventory.get("ARCH") != null,
      inventory.get("CPU") != null,
      inventory.get("MEMORY") != null,
      inventory.get("DISKS") != null,
      inventory.get("NETWORK") != null,
      roles != null,
      !services.isEmpty(),
      inventory.get("VIRTUALIZATION") != null,
      inventory.get("SSHHOSTKEY") != null
    );
    long completenessHits = completenessChecklist.stream().filter(Boolean::booleanValue).count();
    int completenessPercent = (int) Math.round(100.0 * completenessHits / completenessChecklist.size());
    log.debug("Linux-Inventarisierungsgrad für {}: {}% ({} von {} Merkmalen), Quellen: {}, Rollen: {}",
      ip, completenessPercent, completenessHits, completenessChecklist.size(), usedSources, roles);

    StringBuilder protocol = new StringBuilder("Linux-Netzwerkerkennung");
    detail(protocol, "Nachprüfung ausgelöst durch", String.join(" · ", triggers));
    detail(protocol, "Linux bestätigt", "ja");
    detail(protocol, "Kategorieentscheidung", classification.reason());
    if (target.manualCategory()) detail(protocol, "Manuelle Kategorie", "bleibt aufgrund von Priorität unverändert");
    detail(protocol, "Erkennungsmerkmale", String.join(" · ", services));
    detail(protocol, "Betriebssystem", os);
    detail(protocol, "Distributions-ID", inventory.get("DISTRO"));
    detail(protocol, "Distributions-Version", inventory.get("DISTRO_VERSION"));
    detail(protocol, "Kernel", firstNonBlank(inventory.get("KERNEL"), kernelFallback));
    detail(protocol, "Architektur", inventory.get("ARCH"));
    detail(protocol, "Laufzeit seit Start", inventory.get("UPTIME"));
    detail(protocol, "CPU", inventory.get("CPU"));
    detail(protocol, "CPU-Threads", inventory.get("THREADS"));
    detail(protocol, "Arbeitsspeicher", inventory.get("MEMORY"));
    detail(protocol, "Datenträger", inventory.get("DISKS"));
    detail(protocol, "Dateisysteme", inventory.get("FILESYSTEMS"));
    detail(protocol, "Netzwerkadapter", inventory.get("NETWORK"));
    detail(protocol, "Standardgateway", inventory.get("GATEWAY"));
    detail(protocol, "DNS-Server", inventory.get("DNS"));
    detail(protocol, "Virtualisierung", inventory.get("VIRTUALIZATION"));
    detail(protocol, "Container", inventory.get("CONTAINERS"));
    detail(protocol, "Laufende Dienste", inventory.get("SERVICES"));
    detail(protocol, "Paketmanager", inventory.get("PKG_MANAGER"));
    detail(protocol, "Installierte Pakete", inventory.get("PACKAGES"));
    detail(protocol, "Ausstehende Updates", inventory.get("UPDATES"));
    detail(protocol, "Aktualisierbare Pakete", inventory.get("UPGRADABLE_NAMES"));
    detail(protocol, "Erkannte Rollen", roles);
    detail(protocol, "SSH-Hostkey", inventory.get("SSHHOSTKEY"));
    detail(protocol, "Inventarisierungsgrad", completenessPercent + " %");
    detail(protocol, "Verwendete Quellen", String.join(" · ", usedSources));
    if (inventory.isEmpty()) {
      // 40k33b9: SSH ist ausdrücklich optional - "nicht eingerichtet" ist ein normaler
      // Zustand und wird bewusst NICHT als Fehler formuliert.
      detail(protocol, "Zusätzlich über SSH verfügbar", String.join(" · ", sshOnlyFields));
    }
    detail(protocol, "Inventarisierungsstatus", inventoryStatus);
    detail(protocol, "Letzter Inventarisierungsversuch", now);
    if (!inventory.isEmpty()) {
      detail(protocol, "Inventarisierungsdauer", (inventoryDurationMs / 1000.0) + " s");
      detail(protocol, "Erfasste Merkmale", String.valueOf(inventory.size()));
    }
    log.debug("Linux-Ergebnis für {} wird an die Geräteidentität übergeben: Kategorie={}, Protokolllänge={} Zeichen",
      ip, type, protocol.length());
    if (!inventory.isEmpty()) protocol.append(" · SSH-Inventar (schreibgeschützt)");

    return Optional.of(new DiscoveredDevice("linux-network:" + ip, hostLabel, type, ip, null,
      protocol.toString(), "ONLINE", manufacturer, serial, now, false));
  }

  /**
   * 40k33b6a: Leitet aus bereits vorhandenen Signalen (offene Ports, laufende
   * systemd-Dienste, erkannte Container) eine Liste erkannter Server-/Dienstrollen
   * ab. Rein additive Auswertung bereits gesammelter Daten - keine zusätzlichen
   * Netzwerkzugriffe, keine Änderung der Kategorieentscheidung (classify()).
   */
  private static String detectRoles(Set<String> services, Map<String,String> inventory, boolean sshUsed) {
    String svcText = String.join(" ", services).toLowerCase(Locale.ROOT);
    String unitText = nullToEmpty(inventory.get("SERVICES")).toLowerCase(Locale.ROOT);
    String containers = nullToEmpty(inventory.get("CONTAINERS"));
    boolean hasHttp = svcText.contains("http/webserver") || svcText.contains("https/webserver") || svcText.contains("weboberfläche");
    boolean hasDocker = !containers.isBlank() || containsAny(unitText, "docker", "containerd") || svcText.contains("portainer") || svcText.contains("docker-api");
    boolean hasSamba = svcText.contains("samba");
    boolean hasPrint = svcText.contains("ipp/cups");
    List<String> roles = new ArrayList<>();
    if (hasHttp || containsAny(unitText, "nginx", "apache2", "httpd", "caddy")) roles.add("Webserver");
    if (svcText.contains("mysql") || svcText.contains("postgresql") || containsAny(unitText, "mysqld", "mariadb", "postgresql")) roles.add("Datenbankserver");
    // 40k33b9: Rollenerkennung nutzt jetzt bewusst Kombinationen mehrerer bereits vorhandener Quellen.
    if (hasDocker) roles.add(sshUsed && hasHttp ? "Docker Host (SSH + HTTP + Container-Hinweis)" : "Containerhost");
    if (hasHttp && hasSamba) roles.add("Dateiserver (HTTP + Samba)");
    else if (hasSamba || svcText.contains("nfs")) roles.add("Fileserver (Samba/NFS)");
    if (hasPrint) roles.add("Druckserver (IPP/CUPS)");
    if (svcText.contains("ssh")) roles.add("SSH-Fernzugriff aktiv");
    if (svcText.contains("cockpit")) roles.add("Cockpit-Verwaltung aktiv");
    if (svcText.contains("proxmox")) roles.add("Virtualisierungshost (Proxmox)");
    if (hasHttp && svcText.contains("home assistant")) roles.add("Home Assistant Server (HTTP + Home Assistant)");
    else if (svcText.contains("home assistant")) roles.add("Smart-Home-Zentrale (Home Assistant)");
    if (svcText.contains("grafana")) roles.add("Monitoring (Grafana)");
    if (svcText.contains("casaos") || svcText.contains("openmediavault")) roles.add("NAS-Weboberfläche erkannt");
    if (svcText.contains("webmin")) roles.add("Webmin-Verwaltung aktiv");
    if (roles.isEmpty()) return null;
    return String.join(" · ", roles);
  }

  /**
   * 40k33b3: Getrennte Client-Indiz-Erkennung (Notebook/Desktop/Workstation/PC).
   * Ein reiner "linux"-Hostname-Treffer zählt hier ausdrücklich NICHT - nur
   * eindeutige Geräte-/Modell- bzw. Chassis-Hinweise begründen ein Client-Indiz.
   */
  private static String detectClientEvidence(String hostnameLower, String fingerprintLower, String chassisRole) {
    String combined = hostnameLower + " " + fingerprintLower;
    if (containsAny(combined, "notebook", "laptop", "workstation", "desktop", "thinkpad", "elitebook", "probook",
        "latitude", "inspiron", "vostro", "pavilion", "ideapad", "zenbook", "vivobook", "macbook", "chromebook",
        "aspire", "satellite", "tecra")) {
      return "Client-typisches Schlüsselwort im bekannten Gerätenamen/Modell";
    }
    for (String token : hostnameLower.split("[^a-z0-9]+")) {
      if (token.length() >= 4 && HP_BUSINESS_LAPTOP_MODEL.matcher(token).matches()) {
        return "Bekanntes Notebook-Modellmuster im Hostnamen (\"" + token + "\", z.B. HP EliteBook/ProBook-Baureihe)";
      }
    }
    if ("client".equals(chassisRole)) {
      return "SMBIOS-Chassis-Typ laut SSH-Inventar deutet auf Notebook/Desktop hin";
    }
    return null;
  }

  /**
   * Ordnet den SMBIOS-Chassis-Typ-Code (/sys/class/dmi/id/chassis_type) grob einer
   * Rolle zu. Nur eindeutige Codes werden bewertet; unbekannte/mehrdeutige Codes
   * liefern null und wirken sich damit bewusst NICHT auf die Kategorie aus.
   */
  private static String chassisRole(String rawChassisType) {
    String v = clean(rawChassisType);
    if (v == null) return null;
    try {
      int code = Integer.parseInt(v.trim());
      return switch (code) {
        case 8, 9, 10, 14, 30, 31, 32 -> "client"; // Portable/Laptop/Notebook/Sub-Notebook/Tablet/Convertible/Detachable
        case 3, 4, 6, 7, 13, 15 -> "client";       // Desktop/Low-Profile-Desktop/Mini-Tower/Tower/All-in-One/Space-saving
        case 17, 23, 28 -> "server";               // Server-Gehäuse/Rack-Chassis/Blade-Enclosure
        default -> null;
      };
    } catch (NumberFormatException e) { return null; }
  }

  /**
   * 40k33b3: Kategorieentscheidung. Eindeutige Serverrollen haben immer Vorrang vor
   * einem Client-Indiz. Samba/NFS allein zählen ausdrücklich NICHT als eindeutiger
   * Serverbeleg. Ohne eindeutigen Beleg in beide Richtungen bleibt der bisherige
   * konservative Standard ("Server / Linux-System") unverändert bestehen.
   */
  private static ClassificationResult classify(Set<String> services, Map<String,String> inv, String hostnameLower,
                                                String fingerprintLower, String clientEvidence, String chassisRole) {
    String all = (String.join(" ", services) + " " + nullToEmpty(inv.get("OS")) + " " + nullToEmpty(inv.get("VIRTUALIZATION"))
      + " " + nullToEmpty(inv.get("CONTAINERS")) + " " + hostnameLower + " " + fingerprintLower).toLowerCase(Locale.ROOT);

    if (all.contains("proxmox"))
      return new ClassificationResult("Virtualisierungsserver / Proxmox", "Proxmox-Signatur erkannt (eindeutige Serverrolle, hat Vorrang)");
    if (all.contains("home assistant"))
      return new ClassificationResult("Smart-Home-Zentrale / Linux", "Home-Assistant-Signatur erkannt (eindeutige Serverrolle, hat Vorrang)");
    if ("server".equals(chassisRole))
      return new ClassificationResult("Server / Linux-System", "Server-/Rack-Gehäuse laut SSH-Inventar (eindeutige Serverrolle, hat Vorrang)");
    if (containsAny(all, "portainer", "kubernetes", "k8s", "containerd") || !nullToEmpty(inv.get("CONTAINERS")).isBlank())
      return new ClassificationResult("Container-Host / Linux", "Container-/Orchestrierungs-Dienst erkannt (eindeutige Serverrolle, hat Vorrang)");
    if (containsAny(all, "kvm", "hyperv", "hyper-v", "vmware", "esxi", "qemu", "xen"))
      return new ClassificationResult("Virtualisierungsserver / Linux", "Hypervisor-Signatur erkannt (eindeutige Serverrolle, hat Vorrang)");
    if (containsAny(all, "-srv", "srv-", " server", "server.", "server-"))
      return new ClassificationResult("Server / Linux-System", "Hostname/Fingerabdruck nennt ausdrücklich \"Server\" (eindeutige Serverrolle, hat Vorrang)");

    if (clientEvidence != null)
      return new ClassificationResult("Computer / Linux", "Client-Indiz ohne gegenläufige Serverrolle: " + clientEvidence);

    if (containsAny(all, "raspberry", "raspbian"))
      return new ClassificationResult("Einplatinencomputer / Linux", "Raspberry-Pi-Signatur erkannt (unveränderte Einstufung wie bisher)");

    return new ClassificationResult("Server / Linux-System",
      "Kein eindeutiger Client- oder Serverbeleg; bisheriger konservativer Standard beibehalten (Samba/NFS allein zählt nicht als Serverbeleg)");
  }

  private Map<String,String> sshInventory(String ip, int port, String user, String key) {
    LinuxSshSettingsRepository.Settings cfg=sshSettings.load();
    String output = runRemoteCommand(ip, port, user, key, cfg.password(), cfg.authMode(), remoteScript(), 10);
    if (output == null) return Map.of();
    LinkedHashMap<String,String> result = new LinkedHashMap<>();
    for (String line : output.split("\n")) {
      int eq = line.indexOf('=');
      if (eq > 0) result.put(line.substring(0, eq), line.substring(eq + 1).trim());
    }
    return result;
  }

  /**
   * 40k33b6b: Gemeinsame, wiederverwendete SSH-Ausführung. Wird sowohl von der
   * regulären Linux-Inventarisierung (sshInventory) als auch vom neuen
   * On-Demand-Abruf (fetchSection) genutzt - keine zweite Verbindungslogik.
   * Liefert den rohen stdout-Text oder null bei Fehler/Timeout.
   */
  private static String runRemoteCommand(String ip, int port, String user, String key, String password, String authMode, String remoteCommand, int timeoutSeconds) {
    List<String> cmd = new ArrayList<>();
    if ("PASSWORD".equals(authMode)) { cmd.add("sshpass"); cmd.add("-p"); cmd.add(password == null ? "" : password); }
    cmd.addAll(List.of("ssh", "-o", "BatchMode=" + ("PASSWORD".equals(authMode) ? "no" : "yes"), "-o", "ConnectTimeout=4",
      "-o", "ConnectionAttempts=1", "-o", "StrictHostKeyChecking=accept-new", "-p", Integer.toString(port)));
    if (key != null && !key.isBlank()) { cmd.add("-i"); cmd.add(key); }
    cmd.add(user + "@" + ip);
    cmd.add("sh -c '" + remoteCommand.replace("'", "'\\''") + "'");
    try {
      Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
      if (!p.waitFor(timeoutSeconds, TimeUnit.SECONDS)) { p.destroyForcibly(); log.debug("SSH-Befehl an {} lief in ein Timeout", ip); return null; }
      String output;
      try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
        StringBuilder sb = new StringBuilder();
        for (String line; (line = r.readLine()) != null;) sb.append(line).append('\n');
        output = sb.toString();
      }
      // 40k33b8: Ein von Null verschiedener Exit-Code spiegelt bei einer verketteten
      // Befehlsfolge oft nur den LETZTEN Teilbefehl wider (z.B. eine abschließende
      // for-Schleife, deren letzter Test fehlschlägt), obwohl zuvor bereits
      // erfolgreich Daten ausgegeben wurden. Ein einzelner fehlgeschlagener
      // Teilbefehl darf die gesamte Inventarisierung nicht verwerfen - nur ein
      // gleichzeitig LEERES Ergebnis gilt als echter Verbindungs-/Auth-Fehler.
      if (p.exitValue() != 0 && output.isBlank()) { log.debug("SSH-Befehl an {} lieferte weder Ausgabe noch Erfolgscode", ip); return null; }
      if (p.exitValue() != 0) log.debug("SSH-Befehl an {} endete mit Exit-Code {}, lieferte aber Ausgabe - wird übernommen", ip, p.exitValue());
      return output;
    } catch (Exception e) { log.debug("SSH-Befehl an {} fehlgeschlagen: {}", ip, e.toString()); return null; }
  }

  /** 40k33b6b: Ergebnis eines On-Demand-Abrufs für einen einzelnen, einklappbaren Bereich. */
  public record OnDemandSection(String section, String label, String content, boolean available,
                                String fetchedAt, boolean fromCache) {}

  private record CacheEntry(String content, boolean available, OffsetDateTime fetchedAt) {}

  private final ConcurrentHashMap<String,CacheEntry> onDemandCache = new ConcurrentHashMap<>();
  private static final long ON_DEMAND_CACHE_MINUTES = 10;

  /**
   * 40k33b6b: Erweiterte Linux-Analyse - wird ausschließlich abgerufen, wenn der
   * Benutzer den jeweiligen einklappbaren Bereich öffnet (Lazy Loading), nicht
   * automatisch während des regulären Suchlaufs. Ergebnisse werden für
   * {@value #ON_DEMAND_CACHE_MINUTES} Minuten je Gerät und Bereich
   * zwischengespeichert, um unnötige wiederholte SSH-Zugriffe zu vermeiden.
   * Nutzt dieselben SSH-Zugangsdaten/Einstellungen wie die reguläre
   * Inventarisierung - keine zweite Konfiguration.
   */
  public OnDemandSection fetchSection(String ip, String section) {
    String label = sectionLabel(section);
    String cacheKey = ip + "|" + section;
    CacheEntry cached = onDemandCache.get(cacheKey);
    if (cached != null && java.time.Duration.between(cached.fetchedAt(), OffsetDateTime.now()).toMinutes() < ON_DEMAND_CACHE_MINUTES) {
      return new OnDemandSection(section, label, cached.content(), cached.available(), cached.fetchedAt().toString(), true);
    }
    LinuxSshSettingsRepository.Settings cfg=sshSettings.load();
    String sshUser=cfg.username(), sshKey=cfg.keyPath(); int sshPort=cfg.port();
    if (!cfg.enabled() || sshUser.isBlank() || !commandExists("ssh")) {
      OffsetDateTime now = OffsetDateTime.now();
      String message = "Für die erweiterte Linux-Analyse ist kein gemeinsamer SSH-Zugang konfiguriert.";
      onDemandCache.put(cacheKey, new CacheEntry(message, false, now));
      return new OnDemandSection(section, label, message, false, now.toString(), false);
    }
    String command = commandFor(section);
    String output = command == null ? null : runRemoteCommand(ip, sshPort, sshUser, sshKey, cfg.password(), cfg.authMode(), command, 15);
    OffsetDateTime now = OffsetDateTime.now();
    String content = (output == null || output.isBlank()) ? "Keine Daten verfügbar (Werkzeug nicht installiert oder keine Einträge)." : output.trim();
    boolean available = output != null && !output.isBlank();
    onDemandCache.put(cacheKey, new CacheEntry(content, available, now));
    return new OnDemandSection(section, label, content, available, now.toString(), false);
  }

  /**
   * 40k33b7: Fragt den aktuellen SSH-Hostkey-Fingerabdruck erneut ab (kein Cache -
   * die Integritätsprüfung soll bewusst den aktuellen Stand sehen). Liefert null,
   * wenn kein SSH-Zugang konfiguriert ist oder das Gerät nicht erreichbar ist.
   */
  public String currentHostKeyFingerprint(String ip) {
    LinuxSshSettingsRepository.Settings cfg=sshSettings.load();
    String sshUser=cfg.username(), sshKey=cfg.keyPath(); int sshPort=cfg.port();
    if (!cfg.enabled() || sshUser.isBlank() || !commandExists("ssh")) return null;
    String command = "for f in /etc/ssh/ssh_host_ed25519_key.pub /etc/ssh/ssh_host_ecdsa_key.pub /etc/ssh/ssh_host_rsa_key.pub; do "
      + "[ -r \"$f\" ] && ssh-keygen -lf \"$f\" 2>/dev/null | awk '{print $2}' && break; done";
    String output = runRemoteCommand(ip, sshPort, sshUser, sshKey, cfg.password(), cfg.authMode(), command, 8);
    return output == null || output.isBlank() ? null : output.trim();
  }

  private static String sectionLabel(String section) {
    return switch (section) {
      case "packages" -> "Installierte Software";
      case "docker" -> "Docker";
      case "podman" -> "Podman";
      case "snap" -> "Snap";
      case "flatpak" -> "Flatpak";
      case "virtualization" -> "Virtualisierung";
      case "containers" -> "Container";
      case "devenv" -> "Entwicklungsumgebung";
      case "monitoring" -> "Monitoring";
      default -> section;
    };
  }

  private static String commandFor(String section) {
    return switch (section) {
      case "packages" -> "(dpkg -l 2>/dev/null | awk 'NR>5{printf \"%s %s\\n\",$2,$3}' || rpm -qa --qf '%{NAME} %{VERSION}-%{RELEASE}\\n' 2>/dev/null || pacman -Q 2>/dev/null || apk info -v 2>/dev/null) | sort";
      case "docker" -> "command -v docker >/dev/null 2>&1 && { echo '--- Container ---'; docker ps -a --format '{{.Names}} | {{.Image}} | {{.Status}}' 2>/dev/null; echo '--- Images ---'; docker images --format '{{.Repository}}:{{.Tag}} | {{.Size}}' 2>/dev/null; }";
      case "podman" -> "command -v podman >/dev/null 2>&1 && { echo '--- Container ---'; podman ps -a --format '{{.Names}} | {{.Image}} | {{.Status}}' 2>/dev/null; echo '--- Images ---'; podman images --format '{{.Repository}}:{{.Tag}} | {{.Size}}' 2>/dev/null; }";
      case "snap" -> "command -v snap >/dev/null 2>&1 && snap list 2>/dev/null";
      case "flatpak" -> "command -v flatpak >/dev/null 2>&1 && flatpak list --columns=application,version,branch 2>/dev/null";
      case "virtualization" -> "{ printf 'Erkannte Virtualisierung: '; (systemd-detect-virt 2>/dev/null || echo unbekannt); echo; (command -v virsh >/dev/null 2>&1 && virsh list --all 2>/dev/null); (command -v kvm-ok >/dev/null 2>&1 && kvm-ok 2>/dev/null); }";
      case "containers" -> "{ (command -v docker >/dev/null 2>&1 && docker ps -a --format 'Docker: {{.Names}} | {{.Image}} | {{.Status}} | {{.Ports}}' 2>/dev/null); (command -v podman >/dev/null 2>&1 && podman ps -a --format 'Podman: {{.Names}} | {{.Image}} | {{.Status}}' 2>/dev/null); }";
      case "devenv" -> "for c in git docker podman node npm python3 java gcc make go rustc; do v=$(\"$c\" --version 2>/dev/null | head -n1); [ -n \"$v\" ] && printf '%s: %s\\n' \"$c\" \"$v\"; done";
      case "monitoring" -> "for s in node_exporter prometheus-node-exporter zabbix-agent zabbix-agent2 netdata glances telegraf collectd; do systemctl is-active \"$s\" >/dev/null 2>&1 && printf '%s: aktiv\\n' \"$s\"; done";
      default -> null;
    };
  }

  private static String remoteScript() {
    return "kv(){ printf '%s=' \"$1\"; shift; \"$@\" 2>/dev/null | tr '\\n' ' ' | sed 's/[[:space:]][[:space:]]*/ /g;s/ $//'; printf '\\n'; }; " +
      "kv HOSTNAME hostname; " +
      "printf 'OS='; (awk -F= '/^PRETTY_NAME=/{gsub(/^\"|\"$/,\"\",$2);print $2;exit}' /etc/os-release 2>/dev/null || uname -s); " +
      "kv KERNEL uname -sr; kv ARCH uname -m; " +
      "printf 'VENDOR='; cat /sys/class/dmi/id/sys_vendor 2>/dev/null; " +
      "printf 'UUID='; cat /sys/class/dmi/id/product_uuid 2>/dev/null; " +
      "printf 'SERIAL='; (cat /sys/class/dmi/id/product_serial 2>/dev/null || cat /sys/class/dmi/id/board_serial 2>/dev/null); " +
      "printf 'CHASSIS='; cat /sys/class/dmi/id/chassis_type 2>/dev/null; printf '\\n'; " +
      "printf 'CPU='; (lscpu 2>/dev/null | sed -n 's/^Model name:[[:space:]]*//p' | head -n1 || awk -F: '/model name/{gsub(/^[ \\t]+/,\"\",$2);print $2;exit}' /proc/cpuinfo); " +
      "printf 'THREADS='; (nproc --all 2>/dev/null || getconf _NPROCESSORS_ONLN 2>/dev/null); " +
      "printf 'MEMORY='; (free -h 2>/dev/null | awk '/^Mem:/{print $2}' || awk '/MemTotal/{printf \"%.1f GiB\\n\",$2/1048576}' /proc/meminfo); " +
      "printf 'DISKS='; lsblk -dn -o NAME,SIZE,TYPE,MODEL 2>/dev/null | awk '$3==\"disk\"{printf \"%s %s %s; \",$1,$2,$4}'; printf '\\n'; " +
      "printf 'FILESYSTEMS='; df -hT -x tmpfs -x devtmpfs 2>/dev/null | awk 'NR>1{printf \"%s %s %s/%s; \",$7,$2,$4,$3}'; printf '\\n'; " +
      "printf 'NETWORK='; ip -o -4 addr show scope global 2>/dev/null | awk '{printf \"%s %s; \",$2,$4}'; printf '\\n'; " +
      "printf 'VIRTUALIZATION='; (systemd-detect-virt 2>/dev/null || true); " +
      "printf 'CONTAINERS='; (docker ps --format '{{.Names}}' 2>/dev/null | paste -sd, - || podman ps --format '{{.Names}}' 2>/dev/null | paste -sd, - || true); printf '\\n'; " +
      "printf 'SERVICES='; systemctl list-units --type=service --state=running --no-legend --plain 2>/dev/null | awk 'NR<=30{printf \"%s; \",$1}'; printf '\\n'; " +
      "printf 'PACKAGES='; (dpkg-query -W -f='${binary:Package}\\n' 2>/dev/null || rpm -qa 2>/dev/null || pacman -Qq 2>/dev/null) | wc -l; " +
      "printf 'UPDATES='; (apt list --upgradable 2>/dev/null | sed '1d' | wc -l || dnf check-update -q 2>/dev/null | wc -l || true); " +
      // 40k33b6a: zusätzliche Felder für die vollständigere Systeminventarisierung.
      "printf 'DISTRO='; awk -F= '/^ID=/{gsub(/\"/,\"\",$2);print $2;exit}' /etc/os-release 2>/dev/null; printf '\\n'; " +
      "printf 'DISTRO_VERSION='; awk -F= '/^VERSION_ID=/{gsub(/\"/,\"\",$2);print $2;exit}' /etc/os-release 2>/dev/null; printf '\\n'; " +
      "printf 'UPTIME='; (uptime -p 2>/dev/null || awk '{d=int($1/86400);h=int($1%86400/3600);printf \"%dd %dh\",d,h}' /proc/uptime 2>/dev/null); printf '\\n'; " +
      "printf 'GATEWAY='; (ip route show default 2>/dev/null | awk '/default/{print $3;exit}'); printf '\\n'; " +
      "printf 'DNS='; (awk '/^nameserver/{printf \"%s \",$2}' /etc/resolv.conf 2>/dev/null); printf '\\n'; " +
      "printf 'PKG_MANAGER='; (command -v apt >/dev/null 2>&1 && echo apt) || (command -v dnf >/dev/null 2>&1 && echo dnf) || (command -v yum >/dev/null 2>&1 && echo yum) || (command -v pacman >/dev/null 2>&1 && echo pacman) || (command -v zypper >/dev/null 2>&1 && echo zypper) || echo unbekannt; " +
      "printf 'UPGRADABLE_NAMES='; (apt list --upgradable 2>/dev/null | sed '1d' | cut -d/ -f1 | head -n 6 | paste -sd, - || dnf check-update -q 2>/dev/null | awk '{print $1}' | head -n 6 | paste -sd, - || true); printf '\\n'; " +
      // 40k33b7: SSH-Hostkey-Fingerabdruck fuer die Integritaetspruefung - aendert sich
      // ein spaeter erneut abgerufener Fingerabdruck, deutet das stark auf ein anderes
      // physisches Geraet an derselben Identitaet hin (siehe DeviceIdentityService).
      "printf 'SSHHOSTKEY='; (for f in /etc/ssh/ssh_host_ed25519_key.pub /etc/ssh/ssh_host_ecdsa_key.pub /etc/ssh/ssh_host_rsa_key.pub; do " +
      "[ -r \"$f\" ] && ssh-keygen -lf \"$f\" 2>/dev/null | awk '{print $2}' && break; done)";
  }

  private static void probe(String ip, int port, String label, Set<String> services) { if (open(ip, port)) services.add(label + " (TCP " + port + ")"); }

  /**
   * 40k33b9: Sehr einfacher, kurz getimeouteter HTTP-Abruf für bereits offene Web-Ports, um
   * bekannte Linux-Weboberflächen zu erkennen. Rein lesend, kein neuer Discovery-Mechanismus -
   * dieselbe Art Zugriff, die auch ein Browser beim Öffnen der Seite machen würde.
   */
  private static void httpProbe(String ip, int port, boolean https, Set<String> services) {
    try {
      var client = java.net.http.HttpClient.newBuilder()
        .connectTimeout(java.time.Duration.ofMillis(700)).followRedirects(java.net.http.HttpClient.Redirect.NORMAL).build();
      var request = java.net.http.HttpRequest.newBuilder(java.net.URI.create((https ? "https://" : "http://") + ip + ":" + port + "/"))
        .timeout(java.time.Duration.ofMillis(900)).GET().build();
      var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      String label = webUiLabel(nullToEmpty(response.body()).toLowerCase(Locale.ROOT));
      if (label != null) services.add(label + " (HTTP" + (https ? "S" : "") + " " + port + ")");
    } catch (Exception ignored) { /* Web-UI-Erkennung ist rein optional - kein Abbruch, keine Fehlerausgabe. */ }
  }

  private static String webUiLabel(String bodyLower) {
    if (bodyLower.contains("cockpit")) return "Cockpit-Weboberfläche";
    if (bodyLower.contains("casaos")) return "CasaOS-Weboberfläche";
    if (bodyLower.contains("openmediavault")) return "OpenMediaVault-Weboberfläche";
    if (bodyLower.contains("portainer")) return "Portainer-Weboberfläche";
    if (bodyLower.contains("webmin")) return "Webmin-Weboberfläche";
    if (bodyLower.contains("nextcloud")) return "Nextcloud-Weboberfläche";
    if (bodyLower.contains("grafana")) return "Grafana-Weboberfläche";
    if (bodyLower.contains("prometheus time series") || bodyLower.contains(">prometheus<")) return "Prometheus-Weboberfläche";
    if (bodyLower.contains("proxmox virtual environment")) return "Proxmox-Weboberfläche";
    if (bodyLower.contains("home assistant")) return "Home-Assistant-Weboberfläche";
    return null;
  }

  /**
   * 40k33b9: Extrahiert eine bereits von SNMP gemeldete sysDescr-Zeichenkette aus dem
   * Fingerabdruck-Hinweis (siehe DeviceDiscoveryService, SNMP läuft seit 40k33b9 vor der
   * Linux-Nachprüfung und speichert die Systembeschreibung jetzt mit). Liefert nur echten,
   * bereits vom Zielgerät gemeldeten Text zurück - keine Schätzung.
   */
  private static String extractSnmpSysDescr(String fingerprintHint) {
    String text = nullToEmpty(fingerprintHint);
    int idx = text.indexOf("SNMP-Systembeschreibung:");
    if (idx < 0) return null;
    String rest = text.substring(idx + "SNMP-Systembeschreibung:".length()).trim();
    int cut = rest.indexOf(" · ");
    return clean(cut > 0 ? rest.substring(0, cut) : rest);
  }

  /**
   * 40k33b9: Erkennt ausschließlich das bekannte net-snmp-Standardformat für Linux
   * ("Linux &lt;Hostname&gt; &lt;Kernel-Version&gt; ..."). Liefert null, wenn die
   * sysDescr diesem Muster nicht entspricht - es wird nichts geraten.
   */
  private static String[] parseSnmpLinuxHostKernel(String sysDescr) {
    if (sysDescr == null) return null;
    var m = Pattern.compile("^Linux\\s+(\\S+)\\s+([0-9][\\w.\\-]+)").matcher(sysDescr.trim());
    if (!m.find()) return null;
    return new String[]{m.group(1), m.group(2)};
  }
  private static boolean open(String ip, int port) { try (Socket s = new Socket()) { s.connect(new InetSocketAddress(ip, port), CONNECT_TIMEOUT_MS); return true; } catch (Exception e) { return false; } }
  private static String readBanner(String ip, int port) {
    try (Socket s = new Socket()) {
      s.connect(new InetSocketAddress(ip, port), CONNECT_TIMEOUT_MS); s.setSoTimeout(700);
      byte[] b = s.getInputStream().readNBytes(240); if (b.length == 0) return "SSH erreichbar";
      String text = new String(b, StandardCharsets.US_ASCII).replaceAll("[\\r\\n]+", " ").trim();
      return text.startsWith("SSH-") ? text : "SSH erreichbar";
    } catch (Exception e) { return null; }
  }
  private static String reverseName(String ip) { try { String h = java.net.InetAddress.getByName(ip).getCanonicalHostName(); return h.equals(ip) ? null : h; } catch (Exception e) { return null; } }
  private static String inferOs(Set<String> services, String banner) { String all = (String.join(" ", services) + " " + Objects.toString(banner, "")).toLowerCase(Locale.ROOT); if (all.contains("ubuntu")) return "Ubuntu Linux"; if (all.contains("debian")) return "Debian Linux"; return null; }
  private static String inferManufacturer(Set<String> services) { String all = String.join(" ", services).toLowerCase(Locale.ROOT); if (all.contains("proxmox")) return "Proxmox Server Solutions"; if (all.contains("home assistant")) return "Home Assistant"; return null; }
  private static void detail(StringBuilder b, String label, String value) { if (value != null && !value.isBlank()) b.append(" · ").append(label).append(": ").append(clean(value)); }
  private static String clean(String s) { return s == null ? null : s.replaceAll("[\\r\\n]+", " ").replaceAll("\\s+", " ").trim(); }
  private static String nullToEmpty(String value) { return value == null ? "" : value; }
  private static boolean containsAny(String value, String... needles) { if (value == null) return false; for (String needle : needles) if (value.contains(needle)) return true; return false; }
  private String setting(String property, String variable, String fallback) { String v = env.getProperty(property); if (v == null || v.isBlank()) v = System.getenv(variable); return v == null || v.isBlank() ? fallback : v.trim(); }
  private int intSetting(String property, String variable, int fallback, int min, int max) { try { int n = Integer.parseInt(setting(property, variable, Integer.toString(fallback))); return Math.max(min, Math.min(max, n)); } catch (Exception e) { return fallback; } }
  private static boolean commandExists(String command) { try { Process p = new ProcessBuilder("sh", "-c", "command -v " + command + " >/dev/null 2>&1").start(); return p.waitFor(2, TimeUnit.SECONDS) && p.exitValue() == 0; } catch (Exception e) { return false; } }
  private static boolean isIpv4(String value) { String[] p = value.split("\\."); if (p.length != 4) return false; try { for (String x : p) { int n = Integer.parseInt(x); if (n < 0 || n > 255) return false; } return true; } catch (Exception e) { return false; } }
  private static String firstNonBlank(String... values) { for (String v : values) if (v != null && !v.isBlank()) return v.trim(); return null; }
  private static void diagnostic(Consumer<DeviceDiscoveryService.DiscoveryDiagnosticEvent> d, String s, String p, String status, String message, int count) { d.accept(new DeviceDiscoveryService.DiscoveryDiagnosticEvent(s, OffsetDateTime.now().toString(), p, status, message, count)); }
}
