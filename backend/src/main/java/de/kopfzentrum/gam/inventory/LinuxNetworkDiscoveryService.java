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
  private static final Logger discoveryDebug = LoggerFactory.getLogger("GAM_DISCOVERY_DIAGNOSTICS");
  private static final int CONNECT_TIMEOUT_MS = 450;
  private static final int MAX_TARGETS = 1024;
  // HP-Geschäftsnotebooks (EliteBook/ProBook) folgen dem Namensschema NNNN[b|p|s|w],
  // z.B. 6460b, 8470p, 2570p. Bewusst eng gefasst als EIN zusätzliches Client-Indiz
  // unter mehreren - keine vollständige Modell-Datenbank, nur ein bekanntes Muster.
  private static final Pattern HP_BUSINESS_LAPTOP_MODEL = Pattern.compile("\\d{3,4}[bpsw]");
  private final Environment env;
  private final LinuxSshSettingsRepository sshSettings;
  private final EmbeddedSshClientService embeddedSsh;
  private final OsAccessProfileRepository accessProfiles;
  // 40k36t3: Große Softwarelisten werden separat vom Discovery-Protokoll weitergereicht.
  private final ConcurrentHashMap<String,String> latestSoftwareDetails = new ConcurrentHashMap<>();

  public LinuxNetworkDiscoveryService(Environment env, LinuxSshSettingsRepository sshSettings, EmbeddedSshClientService embeddedSsh, OsAccessProfileRepository accessProfiles) {
    this.env = env; this.sshSettings = sshSettings; this.embeddedSsh = embeddedSsh; this.accessProfiles=accessProfiles;
  }

  private LinuxSshSettingsRepository.Settings sshFor(String host){
    var p=accessProfiles.resolve("SSH",host);
    if(p!=null) return new LinuxSshSettingsRepository.Settings(p.enabled(),p.username(),p.authMode(),p.password(),p.keyPath(),p.port());
    return sshSettings.load();
  }

  /** 40k36t8: Zentrale Abfrage für den Plattformdispatcher, ob für das konkrete
   * Ziel wirklich ein aktivierter SSH-Zugang vorhanden ist. */
  public boolean hasConfiguredSshAccess(String host){
    if(host==null||host.isBlank()) return false;
    var s=sshFor(host);
    return s.enabled() && s.username()!=null && !s.username().isBlank();
  }

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
    boolean deepInventory = ssh.enabled() && !sshUser.isBlank();
    diagnostic(diagnostics, sessionId, "LINUX_NETWORK", "RUNNING",
      "Linux-Netzwerkerkennung prüft " + targets.size() + " IPv4-Ziele" + (deepInventory ? " einschließlich SSH-Inventarisierung" : " per Dienst-Fingerprinting"), currentCount);

    ExecutorService pool = Executors.newFixedThreadPool(Math.min(16, Math.max(4, Runtime.getRuntime().availableProcessors())));
    List<Future<Optional<DiscoveredDevice>>> futures = new ArrayList<>();
    for (LinuxScanTarget target : targets) futures.add(pool.submit(() -> inspect(target, sshPort, sshUser, sshKey, deepInventory, now, null)));
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
    LinuxSshSettingsRepository.Settings ssh = sshFor(ip);
    String sshUser = ssh.username(); String sshKey = ssh.keyPath(); int sshPort = ssh.port();
    boolean deepInventory = ssh.enabled() && !sshUser.isBlank();
    log.debug("Manuelle Linux-Inventarisierung für {} angestoßen (SSH konfiguriert: {})", ip, deepInventory);
    LinuxScanTarget target = new LinuxScanTarget(ip.trim(), nullToEmpty(hostnameHint), nullToEmpty(fingerprintHint), manualCategory);
    return inspect(target, sshPort, sshUser, sshKey, deepInventory, OffsetDateTime.now().toString(), null);
  }


  /** 40k36n: Einmaliger, nicht gespeicherter Linux-Lauf mit sudo-Kennwort. */
  public Optional<DiscoveredDevice> inspectSingleWithSudo(String ip, String hostnameHint, String fingerprintHint, boolean manualCategory, String sudoPassword) {
    if (!isIpv4(ip)) return Optional.empty();
    if (sudoPassword == null || sudoPassword.isBlank()) return Optional.empty();
    LinuxSshSettingsRepository.Settings ssh = sshFor(ip);
    String sshUser = ssh.username(); String sshKey = ssh.keyPath(); int sshPort = ssh.port();
    boolean deepInventory = ssh.enabled() && !sshUser.isBlank();
    if (!deepInventory) return Optional.empty();
    LinuxScanTarget target = new LinuxScanTarget(ip.trim(), nullToEmpty(hostnameHint), nullToEmpty(fingerprintHint), manualCategory);
    return inspect(target, sshPort, sshUser, sshKey, true, OffsetDateTime.now().toString(), sudoPassword);
  }

  /** 40k36q2: Gestuftes Ergebnis eines reinen SSH-Verbindungstests. */
  public record SshTestResult(boolean sshConfigured, boolean reachable, boolean tcpReachable, boolean handshakeOk, boolean authOk, String message, String checkedAt) {}

  public SshTestResult testSshConnection(String ip) {
    LinuxSshSettingsRepository.Settings ssh=sshFor(ip); String now=OffsetDateTime.now().toString();
    if(!ssh.enabled()||ssh.username().isBlank()) return new SshTestResult(false,false,false,false,false,"SSH-Zugang ist noch nicht konfiguriert.",now);

    // 40k36q2: Der separate Java-Socket-Test ist nur noch Diagnose und blockiert SSH nicht mehr.
    // Apache MINA führt den maßgeblichen SSH-Verbindungsaufbau selbst durch. Damit kann ein
    // abweichendes Verhalten des einfachen Socket-Vortests keinen funktionierenden SSH-Zugang
    // mehr fälschlich als geschlossen klassifizieren.
    EmbeddedSshClientService.TestResult result=embeddedSsh.testDetailed(ip,ssh.port(),ssh.username(),ssh.authMode(),ssh.password(),ssh.keyPath());

    // Ein erfolgreicher SSH-Handshake beweist zugleich, dass TCP zum Zielport funktioniert.
    // Nur wenn bereits der Handshake scheitert, führen wir zusätzlich einen großzügigen
    // 10-Sekunden-TCP-Diagnosetest aus. Sein Ergebnis ist rein informativ.
    boolean tcp = result.handshakeOk() || tcpPortOpen(ip, ssh.port(), 10000);
    boolean ok=result.commandOk();
    String tcpInfo = tcp
        ? "TCP-Port "+ssh.port()+" erreichbar. "
        : "Separater TCP-Diagnosetest konnte Port "+ssh.port()+" innerhalb von 10 Sekunden nicht bestätigen; der SSH-Client wurde trotzdem direkt ausgeführt. ";
    String message = tcpInfo + result.message();
    return new SshTestResult(true,ok,tcp,result.handshakeOk(),result.authOk(),message,now);
  }

  /** 40k33b8: Verwirft den On-Demand-Zwischenspeicher (40k33b6b) für ein einzelnes Gerät ("Cache aktualisieren"). */
  public void invalidateCache(String ip) {
    onDemandCache.keySet().removeIf(key -> key.startsWith(ip + "|"));
    log.debug("On-Demand-Zwischenspeicher für {} wurde geleert", ip);
  }

  private Optional<DiscoveredDevice> inspect(LinuxScanTarget target, int sshPort, String sshUser, String sshKey, boolean deepInventory, String now, String sudoPassword) {
    long fullScanStartedAt=System.currentTimeMillis();
    String ip = target.ip();
    // 40k36q3: Ein passender Mehrfachzugang fuer genau diesen Host hat Vorrang vor dem globalen Fallback.
    LinuxSshSettingsRepository.Settings hostSsh=sshFor(ip);
    if(hostSsh.enabled()&&!hostSsh.username().isBlank()){sshPort=hostSsh.port();sshUser=hostSsh.username();sshKey=hostSsh.keyPath();deepInventory=true;}
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
    // 40k36t2: Ein erfolgreich konfigurierter SSH-Zugang darf selbst der Bootstrap
    // fuer die Linux-Erkennung sein. Vorher entstand ein Henne-Ei-Problem: Ohne
    // Linux-Hinweis wurde inspect() beendet, obwohl GAM Handshake, Authentifizierung
    // und Testbefehl bereits erfolgreich ausfuehren konnte.
    if (deepInventory && triggers.isEmpty())
      triggers.add("SSH-Zugang konfiguriert – Betriebssystem wird direkt über SSH bestimmt");
    if (triggers.isEmpty()) return Optional.empty();
    discoveryDebug.debug("Linux-Nachprüfung für {} ausgelöst durch: {}", ip, String.join(" · ", triggers));

    long precheckMs=System.currentTimeMillis()-fullScanStartedAt;
    discoveryDebug.info("[LINUX-TIMING] {} Vorprüfung/Netzwerkdienste: {} ms",ip,precheckMs);
    long inventoryStartedAt = System.currentTimeMillis();
    Map<String,String> inventory = Map.of();
    String inventoryStatus;
    if (!deepInventory) {
      inventoryStatus = "SSH-Zugang nicht konfiguriert (gam.discovery.linux.ssh-user/-key)";
      discoveryDebug.debug("Linux-Inventarisierung für {} übersprungen: kein SSH-Zugang konfiguriert (normaler Zustand, SSH ist optional)", ip);
    } else {
      // 40k36t2: Der kurze rohe Banner-Vortest ist nur noch Diagnose. Er darf einen
      // funktionierenden Apache-MINA-SSH-Zugang nicht blockieren. Gerade langsame
      // sshd-Banner koennen innerhalb des 700-ms-Vortests fehlen, obwohl Handshake
      // und Authentifizierung danach problemlos funktionieren.
      discoveryDebug.debug("SSH-Verbindung zu {} wird direkt für die Inventarisierung aufgebaut (Port {}, Banner-Vortest: {})",
        ip, sshPort, sshBanner == null ? "ohne Antwort" : "Antwort erhalten");
      inventory = sshInventory(ip, sshPort, sshUser, sshKey, sudoPassword);
      if (inventory.isEmpty()) {
        // 40k37e56a7fix5: Ein fehlgeschlagener/leerer SSH-Lauf ist KEIN Linux-Beleg.
        // Vorher lief die Methode weiter und erzeugte aus Fallbacks trotzdem
        // "Linux bestätigt: ja" / "Linux / Unix". Dadurch konnten fremde Geräte
        // (z. B. ein iPad) Linux-Metadaten erhalten und der Plattformlauf wurde
        // fälschlich als erfolgreich gewertet. Ohne echte SSH-Inventardaten wird
        // daher nichts als Linux persistiert.
        inventoryStatus = "SSH-Verbindung konfiguriert, aber Inventarbefehl lieferte keine Daten";
        discoveryDebug.debug("Linux-Inventarisierung für {} lieferte trotz direktem SSH-Versuch keine Daten; kein Linux-Beleg, keine Linux-Persistierung", ip);
        return Optional.empty();
      } else if (!isConfirmedLinuxInventory(inventory)) {
        inventoryStatus = "SSH erreichbar, Ziel antwortete jedoch nicht als Linux-System";
        discoveryDebug.debug("SSH-Inventar von {} ist nicht als Linux bestätigt: OS={}, Kernel={}, Distro={}",
          ip, inventory.get("OS"), inventory.get("KERNEL"), inventory.get("DISTRO"));
        return Optional.empty();
      } else {
        inventoryStatus = "erfolgreich";
        discoveryDebug.debug("Linux-Inventarisierung für {} erfolgreich: {} Merkmale erfasst ({})", ip, inventory.size(),
          String.join(", ", inventory.keySet()));
      }
    }
    long inventoryDurationMs = System.currentTimeMillis() - inventoryStartedAt;
    discoveryDebug.info("[LINUX-TIMING] {} SSH-Inventarbefehl inkl. Übertragung/Parsing: {} ms",ip,inventoryDurationMs);
    // 40k33b9: echte (nicht geschätzte) SNMP-Angaben als Fallback nutzen, wenn SSH keine
    // eigenen Werte geliefert hat - ausschließlich, wenn das Muster "Linux <Host> <Kernel>"
    // tatsächlich in der von der Zielmaschine selbst gemeldeten sysDescr steckt.
    String[] snmpHostKernel = snmpSysDescr != null ? parseSnmpLinuxHostKernel(snmpSysDescr) : null;
    String hostLabel = firstNonBlank(inventory.get("HOSTNAME"), snmpHostKernel != null ? snmpHostKernel[0] : null, hostname, reverseName(ip), "Linux-System " + ip);
    String os = firstNonBlank(inventory.get("OS"), inferOs(services, sshBanner), "Linux / Unix");
    String kernelFallback = inventory.get("KERNEL") != null ? null : (snmpHostKernel != null ? "Linux " + snmpHostKernel[1] + " (laut SNMP-Systembeschreibung)" : null);
    String serial = firstNonBlank(inventory.get("UUID"), inventory.get("SERIAL"));
    String manufacturer = firstNonBlank(inventory.get("VENDOR"), inferManufacturer(services));

    String softwareDetails = inventory.get("SOFTWARE_DETAILS");
    if (softwareDetails != null && !softwareDetails.isBlank()) latestSoftwareDetails.put(ip, softwareDetails);
    else latestSoftwareDetails.remove(ip);
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
      inventory.get("BLOCK_DEVICES") != null,
      inventory.get("ENCRYPTION_SUMMARY") != null,
      inventory.get("NETWORK") != null,
      roles != null,
      !services.isEmpty(),
      inventory.get("VIRTUALIZATION") != null,
      inventory.get("SSHHOSTKEY") != null
    );
    long completenessHits = completenessChecklist.stream().filter(Boolean::booleanValue).count();
    int completenessPercent = (int) Math.round(100.0 * completenessHits / completenessChecklist.size());
    discoveryDebug.debug("Linux-Inventarisierungsgrad für {}: {}% ({} von {} Merkmalen), Quellen: {}, Rollen: {}",
      ip, completenessPercent, completenessHits, completenessChecklist.size(), usedSources, roles);

    StringBuilder protocol = new StringBuilder("Linux-Netzwerkerkennung");
    if (!inventory.isEmpty()) protocol.append(" · SSH · Linux");
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
    detail(protocol, "Hardwarehersteller", inventory.get("VENDOR"));
    detail(protocol, "System-UUID", inventory.get("UUID"));
    detail(protocol, "Hardware-Seriennummer", inventory.get("SERIAL"));
    detail(protocol, "Chassis-Typ", inventory.get("CHASSIS"));
    detail(protocol, "Datenträger", inventory.get("DISKS"));
    detail(protocol, "Dateisysteme", inventory.get("FILESYSTEMS"));
    detail(protocol, "Blockgeräte", inventory.get("BLOCK_DEVICES"));
    detail(protocol, "Verschlüsselungsübersicht", inventory.get("ENCRYPTION_SUMMARY"));
    detail(protocol, "LUKS-Container", inventory.get("LUKS_VOLUMES"));
    detail(protocol, "Aktive dm-crypt-Mapper", inventory.get("DM_CRYPT"));
    detail(protocol, "Root-Dateisystem", inventory.get("ROOT_SOURCE"));
    detail(protocol, "Root-Verschlüsselung", inventory.get("ROOT_ENCRYPTED"));
    detail(protocol, "crypttab", inventory.get("CRYPTTAB"));
    detail(protocol, "Verschlüsselter Swap", inventory.get("SWAP_ENCRYPTED"));
    detail(protocol, "LVM", inventory.get("LVM"));
    detail(protocol, "Software-RAID", inventory.get("RAID"));
    detail(protocol, "Btrfs", inventory.get("BTRFS"));
    detail(protocol, "ZFS", inventory.get("ZFS"));
    detail(protocol, "TPM / systemd-cryptenroll", inventory.get("TPM_CRYPT"));
    detail(protocol, "Storage-Werkzeuge", inventory.get("STORAGE_TOOLS"));
    detail(protocol, "Berechtigungsbenutzer", inventory.get("PERMISSION_USER"));
    detail(protocol, "Benutzer-ID", inventory.get("PERMISSION_UID"));
    detail(protocol, "Gruppen", inventory.get("PERMISSION_GROUPS"));
    detail(protocol, "Administratorgruppe", inventory.get("ADMIN_GROUP"));
    detail(protocol, "sudo verfügbar", inventory.get("SUDO_AVAILABLE"));
    detail(protocol, "sudo ohne Kennwort", inventory.get("SUDO_NOPASS"));
    detail(protocol, "Root-Rechte", inventory.get("ROOT_ACCESS"));
    detail(protocol, "Erhöhter Lauf", inventory.get("PRIVILEGED_RUN"));
    detail(protocol, "Inventarisierungstiefe", inventory.get("INVENTORY_LEVEL"));
    detail(protocol, "Berechtigungseinschränkungen", inventory.get("PERMISSION_LIMITATIONS"));
    detail(protocol, "Netzwerkadapter", inventory.get("NETWORK"));
    detail(protocol, "Standardgateway", inventory.get("GATEWAY"));
    detail(protocol, "DNS-Server", inventory.get("DNS"));
    detail(protocol, "Virtualisierung", inventory.get("VIRTUALIZATION"));
    detail(protocol, "Container", inventory.get("CONTAINERS"));
    detail(protocol, "Laufende Dienste", inventory.get("SERVICES"));
    detail(protocol, "Paketmanager", inventory.get("PKG_MANAGER"));
    detail(protocol, "Installierte Pakete", inventory.get("PACKAGES"));
    detail(protocol, "Softwareinventar", inventory.get("PACKAGES") == null ? null : inventory.get("PACKAGES") + " Pakete strukturiert gespeichert");
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
    discoveryDebug.debug("Linux-Ergebnis für {} wird an die Geräteidentität übergeben: Kategorie={}, Protokolllänge={} Zeichen",
      ip, type, protocol.length());
    if (!inventory.isEmpty()) protocol.append(" · SSH-Inventar (schreibgeschützt)");

    long totalMs=System.currentTimeMillis()-fullScanStartedAt;
    discoveryDebug.info("[LINUX-TIMING] {} Gesamter Linux-Erfassungspfad: {} ms (Vorprüfung {} ms, SSH-Inventar {} ms, Nachverarbeitung {} ms)",
      ip,totalMs,precheckMs,inventoryDurationMs,Math.max(0,totalMs-precheckMs-inventoryDurationMs));
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

  private Map<String,String> sshInventory(String ip, int port, String user, String key, String sudoPassword) {
    LinuxSshSettingsRepository.Settings cfg=sshFor(ip);
    String output = (sudoPassword != null && !sudoPassword.isBlank())
      ? runRemoteScriptWithSudo(ip, port, user, key, cfg.password(), cfg.authMode(), sudoPassword, remoteScript(), 20)
      : runRemoteCommand(ip, port, user, key, cfg.password(), cfg.authMode(), remoteScript(), 10);
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
  private String runRemoteCommand(String ip, int port, String user, String key, String password, String authMode, String remoteCommand, int timeoutSeconds) {
    EmbeddedSshClientService.Result result = embeddedSsh.execute(ip, port, user, authMode, password, key,
      "sh -c '" + remoteCommand.replace("'", "'\\''") + "'", null, timeoutSeconds);
    if (!result.success()) {
      log.debug("SSH-Befehl an {} fehlgeschlagen: {}", ip, result.message());
      return null;
    }
    String output = result.stdout();
    return output == null || output.isBlank() ? null : output;
  }


  /** Führt das Inventarskript über sudo -S aus; das Kennwort wird nur über stdin übertragen. */
  private String runRemoteScriptWithSudo(String ip, int port, String user, String key, String sshPassword, String authMode, String sudoPassword, String script, int timeoutSeconds) {
    String input = (sudoPassword == null ? "" : sudoPassword) + "\n" + script + "\n";
    EmbeddedSshClientService.Result result = embeddedSsh.execute(ip, port, user, authMode, sshPassword, key,
      "sudo -S -p '' sh -s", input, timeoutSeconds);
    if (!result.success()) {
      log.debug("Erhöhter SSH-Befehl an {} fehlgeschlagen: {}", ip, result.message());
      return null;
    }
    String output = result.stdout();
    return output == null || output.isBlank() ? null : output;
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
    LinuxSshSettingsRepository.Settings cfg=sshFor(ip);
    String sshUser=cfg.username(), sshKey=cfg.keyPath(); int sshPort=cfg.port();
    if (!cfg.enabled() || sshUser.isBlank()) {
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
  /** 40k36t3: Übergibt die zuletzt per SSH ermittelte Softwareliste genau einmal
   * an das strukturierte Softwareinventar. Sie bleibt bewusst außerhalb des Discovery-Protokolls. */
  public String consumeLatestSoftwareDetails(String ip) {
    if (ip == null) return null;
    return latestSoftwareDetails.remove(ip);
  }

  public String currentHostKeyFingerprint(String ip) {
    LinuxSshSettingsRepository.Settings cfg=sshFor(ip);
    String sshUser=cfg.username(), sshKey=cfg.keyPath(); int sshPort=cfg.port();
    if (!cfg.enabled() || sshUser.isBlank()) return null;
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
      "printf 'VENDOR='; cat /sys/class/dmi/id/sys_vendor 2>/dev/null; printf '\\n'; " +
      "printf 'UUID='; cat /sys/class/dmi/id/product_uuid 2>/dev/null; printf '\\n'; " +
      "printf 'SERIAL='; (cat /sys/class/dmi/id/product_serial 2>/dev/null || cat /sys/class/dmi/id/board_serial 2>/dev/null); printf '\\n'; " +
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
      // 40k36t: Strukturierte Softwarequellen fuer dieselbe plattformneutrale Tabelle wie macOS.
      // Zeilenformat: Quelle|Name|Version|Paket-ID/Pfad, getrennt durch " ; ".
      "printf 'SOFTWARE_DETAILS='; { " +
      "if command -v dpkg-query >/dev/null 2>&1; then dpkg-query -W -f='APT / dpkg|${binary:Package}|${Version}|${binary:Package}||\\n' 2>/dev/null; " +
      "elif command -v pacman >/dev/null 2>&1; then pacman -Q 2>/dev/null | awk '{printf \\\"pacman|%s|%s|%s||\\\\n\\\",$1,$2,$1}'; " +
      "elif command -v rpm >/dev/null 2>&1; then rpm -qa --qf 'RPM / DNF / YUM|%{NAME}|%{VERSION}-%{RELEASE}|%{NAME}||\\n' 2>/dev/null; " +
      "elif command -v apk >/dev/null 2>&1; then apk info -v 2>/dev/null | awk '{printf \\\"APK|%s||%s||\\\\n\\\",$0,$0}'; fi; " +
      "command -v snap >/dev/null 2>&1 && snap list 2>/dev/null | awk 'NR>1{printf \\\"Snap|%s|%s|%s||\\\\n\\\",$1,$2,$1}'; " +
      "command -v flatpak >/dev/null 2>&1 && flatpak list --app --columns=application,name,version 2>/dev/null | awk -F '\\\\t' '{printf \\\"Flatpak|%s|%s|%s||\\\\n\\\",$2,$3,$1}'; " +
      "if command -v brew >/dev/null 2>&1; then brew list --formula --versions 2>/dev/null | awk '{n=$1;$1=\\\"\\\";sub(/^ /,\\\"\\\");printf \\\"Homebrew Formulae|%s|%s|%s||\\\\n\\\",n,$0,n}'; fi; " +
      // 40k36t1: Bereitliegende Installationsartefakte bewusst getrennt vom installierten Paketbestand erfassen.
      // Suche bleibt auf typische Ablageorte und geringe Tiefe begrenzt, damit SSH-Inventarisierungen nicht das gesamte Dateisystem durchlaufen.
      "for f in $(find /home /root /tmp /var/tmp -maxdepth 4 -type f -name '*.deb' -print 2>/dev/null | head -n 200); do " +
      "n=$(dpkg-deb -f \\\"$f\\\" Package 2>/dev/null); v=$(dpkg-deb -f \\\"$f\\\" Version 2>/dev/null); a=$(dpkg-deb -f \\\"$f\\\" Architecture 2>/dev/null); [ -z \\\"$n\\\" ] && n=$(basename \\\"$f\\\"); printf 'DEB-Pakete|%s|%s|%s (%s)|%s\\n' \\\"$n\\\" \\\"$v\\\" \\\"$n\\\" \\\"$a\\\" \\\"$f\\\"; done; " +
      "for f in $(find /home /root /tmp /var/tmp -maxdepth 4 -type f -name '*.rpm' -print 2>/dev/null | head -n 200); do " +
      "n=$(rpm -qp --qf '%{NAME}' \\\"$f\\\" 2>/dev/null); v=$(rpm -qp --qf '%{VERSION}-%{RELEASE}' \\\"$f\\\" 2>/dev/null); a=$(rpm -qp --qf '%{ARCH}' \\\"$f\\\" 2>/dev/null); [ -z \\\"$n\\\" ] && n=$(basename \\\"$f\\\"); printf 'RPM-Pakete|%s|%s|%s (%s)|%s\\n' \\\"$n\\\" \\\"$v\\\" \\\"$n\\\" \\\"$a\\\" \\\"$f\\\"; done; " +
      "} | tr '\\n' '\\036'; printf '\\n'; " +
      "printf 'UPDATES='; (apt list --upgradable 2>/dev/null | sed '1d' | wc -l || dnf check-update -q 2>/dev/null | wc -l || true); " +
      // 40k33b6a: zusätzliche Felder für die vollständigere Systeminventarisierung.
      "printf 'DISTRO='; awk -F= '/^ID=/{gsub(/\"/,\"\",$2);print $2;exit}' /etc/os-release 2>/dev/null; printf '\\n'; " +
      "printf 'DISTRO_VERSION='; awk -F= '/^VERSION_ID=/{gsub(/\"/,\"\",$2);print $2;exit}' /etc/os-release 2>/dev/null; printf '\\n'; " +
      "printf 'UPTIME='; (uptime -p 2>/dev/null || awk '{d=int($1/86400);h=int($1%86400/3600);printf \"%dd %dh\",d,h}' /proc/uptime 2>/dev/null); printf '\\n'; " +
      "printf 'GATEWAY='; (ip route show default 2>/dev/null | awk '/default/{print $3;exit}'); printf '\\n'; " +
      "printf 'DNS='; (awk '/^nameserver/{printf \"%s \",$2}' /etc/resolv.conf 2>/dev/null); printf '\\n'; " +
      "printf 'PKG_MANAGER='; (command -v apt >/dev/null 2>&1 && echo apt) || (command -v dnf >/dev/null 2>&1 && echo dnf) || (command -v yum >/dev/null 2>&1 && echo yum) || (command -v pacman >/dev/null 2>&1 && echo pacman) || (command -v zypper >/dev/null 2>&1 && echo zypper) || echo unbekannt; " +
      "printf 'UPGRADABLE_NAMES='; (apt list --upgradable 2>/dev/null | sed '1d' | cut -d/ -f1 | head -n 6 | paste -sd, - || dnf check-update -q 2>/dev/null | awk '{print $1}' | head -n 6 | paste -sd, - || true); printf '\\n'; " +
      // 40k36h: Vertiefte Linux-Datentraeger- und Verschluesselungsinventarisierung.
      // Alle Abfragen sind schreibgeschuetzt; fehlende optionale Werkzeuge brechen den Lauf nicht ab.
      "printf 'BLOCK_DEVICES='; lsblk -P -o NAME,KNAME,TYPE,SIZE,FSTYPE,MOUNTPOINT,UUID,PARTUUID,LABEL,PKNAME 2>/dev/null | tr '\\n' ';'; printf '\\n'; " +
      "printf 'LUKS_VOLUMES='; lsblk -rno NAME,KNAME,TYPE,SIZE,FSTYPE,MOUNTPOINT,PKNAME 2>/dev/null | awk '$5==\"crypto_LUKS\"{printf \"%s|%s|%s|%s|%s|%s;\",$1,$2,$4,$5,$6,$7}'; printf '\\n'; " +
      "printf 'DM_CRYPT='; (dmsetup ls --target crypt 2>/dev/null | awk '{printf \"%s;\",$1}' || true); printf '\\n'; " +
      "printf 'ROOT_SOURCE='; (findmnt -n -o SOURCE / 2>/dev/null || awk '$2==\"/\"{print $1;exit}' /proc/mounts); printf '\\n'; " +
      "printf 'ROOT_ENCRYPTED='; root=$(findmnt -n -o SOURCE / 2>/dev/null); case \"$root\" in /dev/mapper/*|/dev/dm-*) echo ja;; *) cur=$root; enc=nein; i=0; while [ -n \"$cur\" ] && [ $i -lt 8 ]; do fs=$(lsblk -nro FSTYPE \"$cur\" 2>/dev/null | head -n1); typ=$(lsblk -nro TYPE \"$cur\" 2>/dev/null | head -n1); if [ \"$fs\" = crypto_LUKS ] || [ \"$typ\" = crypt ]; then enc=ja; break; fi; parent=$(lsblk -nro PKNAME \"$cur\" 2>/dev/null | head -n1); [ -z \"$parent\" ] && break; cur=/dev/$parent; i=$((i+1)); done; echo $enc;; esac; " +
      "printf 'CRYPTTAB='; if [ -r /etc/crypttab ]; then awk '!/^[[:space:]]*#/ && NF{printf \"%s->%s;\",$1,$2}' /etc/crypttab; else printf 'nicht vorhanden oder nicht lesbar'; fi; printf '\\n'; " +
      "printf 'SWAP_ENCRYPTED='; swap=$(awk 'NR>1{print $1}' /proc/swaps 2>/dev/null | head -n1); if [ -z \"$swap\" ]; then echo 'kein Swap'; else case \"$swap\" in /dev/mapper/*|/dev/dm-*) echo ja;; *) t=$(lsblk -nro TYPE \"$swap\" 2>/dev/null | head -n1); [ \"$t\" = crypt ] && echo ja || echo nein;; esac; fi; " +
      "printf 'LVM='; if command -v lvs >/dev/null 2>&1; then lvs --noheadings --separator '|' -o vg_name,lv_name,lv_size,lv_attr,pool_lv 2>/dev/null | awk '{$1=$1;printf \"%s;\",$0}'; else printf 'nicht installiert'; fi; printf '\\n'; " +
      "printf 'RAID='; if grep -q '^md' /proc/mdstat 2>/dev/null; then awk '/^md/{printf \"%s \",$0;getline;printf \"%s;\",$0}' /proc/mdstat; else printf 'kein mdadm-RAID erkannt'; fi; printf '\\n'; " +
      "printf 'BTRFS='; if command -v btrfs >/dev/null 2>&1; then btrfs filesystem show 2>/dev/null | tr '\\n' ' ' | sed 's/[[:space:]][[:space:]]*/ /g'; else printf 'nicht installiert'; fi; printf '\\n'; " +
      "printf 'ZFS='; if command -v zpool >/dev/null 2>&1; then zpool list -H -o name,size,alloc,free,health 2>/dev/null | awk '{printf \"%s|%s|%s|%s|%s;\",$1,$2,$3,$4,$5}'; else printf 'nicht installiert'; fi; printf '\\n'; " +
      "printf 'TPM_CRYPT='; if { [ -c /dev/tpmrm0 ] || [ -c /dev/tpm0 ]; }; then printf 'TPM vorhanden'; else printf 'kein TPM-Gerät erkannt'; fi; command -v systemd-cryptenroll >/dev/null 2>&1 && printf ', systemd-cryptenroll vorhanden'; printf '\\n'; " +
      "printf 'STORAGE_TOOLS='; for c in lsblk findmnt blkid cryptsetup dmsetup pvs vgs lvs mdadm btrfs zpool systemd-cryptenroll; do command -v $c >/dev/null 2>&1 && printf '%s:ja;' $c || printf '%s:nein;' $c; done; printf '\\n'; " +
      "printf 'ENCRYPTION_SUMMARY='; luks=$(lsblk -rno FSTYPE 2>/dev/null | awk '$1==\"crypto_LUKS\"{n++}END{print n+0}'); open=$(dmsetup ls --target crypt 2>/dev/null | wc -l); root=$(findmnt -n -o SOURCE / 2>/dev/null); case \"$root\" in /dev/mapper/*|/dev/dm-*) re=ja;; *) re=nein;; esac; printf 'LUKS:%s;geöffnet:%s;Root verschlüsselt:%s' \"$luks\" \"$open\" \"$re\"; printf '\\n'; " +
      // 40k36n: Linux-Berechtigungen und Inventarisierungstiefe.
      "printf 'PERMISSION_USER='; id -un 2>/dev/null; printf '\\n'; " +
      "printf 'PERMISSION_UID='; id -u 2>/dev/null; printf '\\n'; " +
      "printf 'PERMISSION_GROUPS='; id -Gn 2>/dev/null | tr ' ' ','; printf '\\n'; " +
      "printf 'ADMIN_GROUP='; id -Gn 2>/dev/null | grep -Eq '(^| )(sudo|wheel|admin)( |$)' && echo ja || echo nein; " +
      "printf 'SUDO_AVAILABLE='; command -v sudo >/dev/null 2>&1 && echo ja || echo nein; " +
      "printf 'SUDO_NOPASS='; sudo -n true >/dev/null 2>&1 && echo ja || echo nein; " +
      "printf 'ROOT_ACCESS='; uid=$(id -u 2>/dev/null); [ \"$uid\" = 0 ] && echo ja || echo nein; " +
      "printf 'PRIVILEGED_RUN='; uid=$(id -u 2>/dev/null); [ \"$uid\" = 0 ] && echo ja || echo nein; " +
      "printf 'PERMISSION_LIMITATIONS='; uid=$(id -u 2>/dev/null); lim=''; [ \"$uid\" != 0 ] && lim='kein Root-Kontext'; command -v sudo >/dev/null 2>&1 || lim=\"${lim}${lim:+; }sudo nicht installiert\"; sudo -n true >/dev/null 2>&1 || lim=\"${lim}${lim:+; }sudo benötigt Kennwort oder ist nicht erlaubt\"; [ -r /etc/shadow ] || lim=\"${lim}${lim:+; }geschützte Kontodaten nicht lesbar\"; [ -r /root ] || lim=\"${lim}${lim:+; }Root-Bereich nicht lesbar\"; printf '%s\\n' \"${lim:-keine}\"; " +
      "printf 'INVENTORY_LEVEL='; uid=$(id -u 2>/dev/null); if [ \"$uid\" = 0 ]; then echo Vollzugriff; elif sudo -n true >/dev/null 2>&1; then echo Administrator; elif id -Gn 2>/dev/null | grep -Eq '(^| )(sudo|wheel|admin)( |$)'; then echo Erweitert; else echo Standard; fi; " +
      // 40k33b7: SSH-Hostkey-Fingerabdruck fuer die Integritaetspruefung - aendert sich
      // ein spaeter erneut abgerufener Fingerabdruck, deutet das stark auf ein anderes
      // physisches Geraet an derselben Identitaet hin (siehe DeviceIdentityService).
      "printf 'SSHHOSTKEY='; (for f in /etc/ssh/ssh_host_ed25519_key.pub /etc/ssh/ssh_host_ecdsa_key.pub /etc/ssh/ssh_host_rsa_key.pub; do " +
      "[ -r \"$f\" ] && ssh-keygen -lf \"$f\" 2>/dev/null | awk '{print $2}' && break; done)";
  }

  /** 40k36t2: Linux wird erst nach echten SSH-Inventardaten bestaetigt. */
  private static boolean isConfirmedLinuxInventory(Map<String,String> inventory) {
    if (inventory == null || inventory.isEmpty()) return false;
    String kernel = nullToEmpty(inventory.get("KERNEL")).toLowerCase(Locale.ROOT);
    String distro = nullToEmpty(inventory.get("DISTRO")).toLowerCase(Locale.ROOT);
    String os = nullToEmpty(inventory.get("OS")).toLowerCase(Locale.ROOT);
    return kernel.startsWith("linux") || !distro.isBlank() ||
      containsAny(os, "linux", "ubuntu", "debian", "fedora", "centos", "rhel", "rocky", "alma", "opensuse", "suse", "arch", "cachyos", "manjaro", "alpine");
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
  private static boolean tcpPortOpen(String ip, int port, int timeoutMs) { try (Socket s = new Socket()) { s.connect(new InetSocketAddress(ip, port), timeoutMs); return true; } catch (Exception e) { return false; } }
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
