package de.kopfzentrum.gam.inventory;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;

/**
 * 40k33b5: Geräteidentität, Quellenübersicht und Identitätsverwaltung.
 *
 * Baut bewusst NICHT auf einer zweiten Identitätsverwaltung auf: sämtliche
 * Daten stammen aus der bereits vorhandenen gam_discovery_registered_devices-
 * Tabelle (DiscoveryRegistrationRepository), der Alias-Tabelle (ebenfalls dort),
 * dem Merge-Audit-Log aus 40k33b4 (DeviceMergeRepository) und der bereits seit
 * 40k33b1 bestehenden Kategorie-Änderungshistorie. Konfidenzbewertung und
 * abgeleitete Felder (Plattform/Unterkategorie/Quellenliste) werden bei jedem
 * Aufruf frisch berechnet, nicht zusätzlich gespeichert.
 *
 * Seit 40k33b7 zusätzlich: Integritätsprüfung bereits zusammengeführter
 * Identitäten und geführte, rein manuelle Wiederauftrennung. Beides erweitert
 * ausschließlich diese bestehende Identitätsverwaltung - keine zweite
 * Mergeverwaltung, keine neue Discovery, keine automatische Entscheidung.
 */
@Service
public class DeviceIdentityService {
  private static final Logger log = LoggerFactory.getLogger(DeviceIdentityService.class);
  private final DiscoveryRegistrationRepository registrations;
  private final DeviceMergeRepository merges;
  private final DeviceMergeService mergeService;
  private final LinuxNetworkDiscoveryService linux;
  private final AndroidAdbService adb;
  private final AppInventoryRepository appInventory;
  private final PlatformInventoryRepository platformInventory;
  private final WindowsInventoryDiscoveryService windowsLocal;
  private final WindowsRemoteInventoryService windowsRemote;
  private final MacOsInventoryService macOs;
  private final ObjectMapper objectMapper;

  public DeviceIdentityService(DiscoveryRegistrationRepository registrations, DeviceMergeRepository merges,
                               DeviceMergeService mergeService, LinuxNetworkDiscoveryService linux,
                               AndroidAdbService adb, AppInventoryRepository appInventory,
                               PlatformInventoryRepository platformInventory,
                               WindowsInventoryDiscoveryService windowsLocal, WindowsRemoteInventoryService windowsRemote,
                               MacOsInventoryService macOs, ObjectMapper objectMapper) {
    this.registrations = registrations;
    this.merges = merges;
    this.mergeService = mergeService;
    this.linux = linux;
    this.adb = adb;
    this.appInventory = appInventory;
    this.platformInventory = platformInventory;
    this.windowsLocal = windowsLocal;
    this.windowsRemote = windowsRemote;
    this.macOs = macOs;
    this.objectMapper = objectMapper;
  }

  public record ConfidenceAssessment(String label, List<String> reasons) {}

  public record IdentityRow(
    String identityKey, String name, boolean manualName, String deviceType, boolean manualDeviceType,
    String category, String subcategory, String platform, String address, String hardwareAddress,
    String serialNumber, String manufacturer, String status, Object firstSeenAt, Object lastSeenAt,
    long detectionCount, int lastScanHits, List<String> sources, int sourceCount,
    List<String> aliases, int aliasCount, int mergeCount, String confidenceLabel,
    List<String> confidenceReasons, boolean hasOpenCandidate, String protocol,
    String adbHost, Integer adbPort, Object adbLastConnectedAt) {}

  public record HistoryEntry(Object timestamp, String kind, String description) {}

  public record ReassessResult(IdentityRow identity, List<DeviceMergeService.Candidate> openCandidates) {}

  /** 40k33b7: Ein Feld der tabellarischen Gegenüberstellung bei einer Integritätswarnung. */
  public record IntegrityResult(String status, List<String> reasons,
                                List<DiscoveryRegistrationRepository.FieldComparisonRow> comparisonTable,
                                String comparedIdentityKey, String checkedAt, int conflictCount,
                                String criticalityLevel, String criticalMessage, boolean manualConfirmationRequired) {}

  /** 40k33b7: Ein möglicher Bestandteil einer früheren Zusammenführung, der wieder abgespalten werden kann. */
  public record SplitCandidate(String ref, String label, String name, String deviceType, String address,
                               String hardwareAddress, String serialNumber, String manufacturer, boolean fullSnapshot) {}

  public List<IdentityRow> overview() {
    List<Map<String,Object>> rows = registrations.findAll();
    Set<String> withOpenCandidate = candidateKeys();
    List<IdentityRow> result = new ArrayList<>();
    for (Map<String,Object> row : rows) result.add(toIdentityRow(row, withOpenCandidate));
    return result;
  }

  public IdentityRow detail(String identityKey) {
    Map<String,Object> row = registrations.find(identityKey);
    return toIdentityRow(row, candidateKeys());
  }

  public List<HistoryEntry> history(String identityKey) {
    List<HistoryEntry> entries = new ArrayList<>();
    for (Map<String,Object> h : registrations.typeHistoryFor(identityKey)) {
      String from = str(h.get("oldDeviceType")), to = str(h.get("newDeviceType"));
      entries.add(new HistoryEntry(h.get("changedAt"), "KATEGORIE",
        "Kategorie geändert" + (from != null ? " von „" + from + "“" : "") + " zu „" + to + "“"
          + (h.get("changedBy") != null ? " durch " + h.get("changedBy") : "")));
    }
    for (Map<String,Object> a : registrations.aliasesWithTimestamps(identityKey)) {
      entries.add(new HistoryEntry(a.get("createdAt"), "ALIAS", "Alias „" + a.get("aliasName") + "“ hinzugekommen"));
    }
    for (Map<String,Object> m : merges.logEntriesTouching(identityKey)) {
      boolean wasTarget = identityKey.equals(str(m.get("targetIdentityKey")));
      String description = wasTarget
        ? "Zusammenführung: " + m.get("mergedIdentityKeys") + " in dieses Gerät übernommen"
        : "Dieses Gerät wurde in " + m.get("targetIdentityKey") + " zusammengeführt";
      if (m.get("summary") != null) description += " · " + m.get("summary");
      entries.add(new HistoryEntry(m.get("performedAt"), "MERGE", description));
    }
    entries.sort(Comparator.comparing((HistoryEntry e) -> String.valueOf(e.timestamp())).reversed());
    return entries;
  }

  public ReassessResult reassess(String identityKey) {
    IdentityRow row = detail(identityKey);
    List<DeviceMergeService.Candidate> open = mergeService.findCandidates().stream()
      .filter(c -> identityKey.equals(c.keyA()) || identityKey.equals(c.keyB()))
      .toList();
    return new ReassessResult(row, open);
  }

  public record LinuxActionResult(boolean success, String message, IdentityRow identity) {}

  /**
   * 40k33b8: Führt die Linux-Inventarisierung gezielt für EIN Gerät erneut aus,
   * ohne einen vollständigen Suchlauf zu starten. Reine Wiederverwendung von
   * LinuxNetworkDiscoveryService.inspectSingle() und der bereits vorhandenen
   * recordDiscoveryHit()-Schreiblogik - dieselbe Identität wird nur ergänzt,
   * niemals ein neues Gerät erzeugt.
   */
  public LinuxActionResult runLinuxInventory(String identityKey) {
    Map<String,Object> row = registrations.find(identityKey);
    String address = str(row.get("address"));
    if (address == null) throw new IllegalArgumentException("Für dieses Gerät ist keine IP-Adresse bekannt - eine Linux-Inventarisierung ist nicht möglich.");
    boolean manualCategory = truthy(row.get("manualDeviceType"));
    String fingerprintHint = (nullToEmpty(str(row.get("deviceType"))) + " " + nullToEmpty(str(row.get("protocol")))).trim();
    var result = linux.inspectSingle(address, str(row.get("name")), fingerprintHint, manualCategory);
    if (result.isEmpty()) {
      return new LinuxActionResult(false,
        "Für dieses Gerät wurden aktuell keine Linux-Hinweise (Hostname/Fingerabdruck) gefunden - die Inventarisierung wurde nicht ausgelöst.",
        detail(identityKey));
    }
    var device = result.get();
    registrations.recordDiscoveryHit(device.address(), device.hardwareAddress(), device.serialNumber(),
      device.name(), device.type(), device.manufacturer(), device.protocol(), device.status());
    return new LinuxActionResult(true, "Linux-Inventarisierung wurde durchgeführt.", detail(identityKey));
  }

  /** 40k36: macOS gezielt per gemeinsamer SSH-Konfiguration inventarisieren. */
  public LinuxActionResult runMacOsInventory(String identityKey) {
    Map<String,Object> row=registrations.find(identityKey); String address=str(row.get("address"));
    if(address==null) throw new IllegalArgumentException("Für dieses Gerät ist keine IP-Adresse bekannt.");
    var result=macOs.inventory(address,str(row.get("name")));
    if(result.isEmpty()) return new LinuxActionResult(false,"macOS-Inventarisierung nicht möglich. Bitte auf dem Mac 'Entfernte Anmeldung' aktivieren und SSH-Konfiguration prüfen.",detail(identityKey));
    var d=result.get(); registrations.recordDiscoveryHit(d.address(),d.hardwareAddress(),d.serialNumber(),d.name(),d.type(),d.manufacturer(),d.protocol(),d.status());
    return new LinuxActionResult(true,"macOS-Inventarisierung wurde erfolgreich durchgeführt.",detail(identityKey));
  }
  public MacOsInventoryService.TestResult testMacOsSsh(String identityKey){Map<String,Object> row=registrations.find(identityKey);String address=str(row.get("address"));if(address==null)throw new IllegalArgumentException("Für dieses Gerät ist keine IP-Adresse bekannt.");return macOs.test(address);}

  /** 40k33b8: Reiner SSH-Verbindungstest, ohne vollständige Inventarisierung. */
  public LinuxNetworkDiscoveryService.SshTestResult testLinuxSsh(String identityKey) {
    Map<String,Object> row = registrations.find(identityKey);
    String address = str(row.get("address"));
    if (address == null) throw new IllegalArgumentException("Für dieses Gerät ist keine IP-Adresse bekannt.");
    return linux.testSshConnection(address);
  }

  /** 40k33b8: Verwirft den On-Demand-Zwischenspeicher (40k33b6b) für dieses Gerät. */
  public void refreshLinuxCache(String identityKey) {
    Map<String,Object> row = registrations.find(identityKey);
    String address = str(row.get("address"));
    if (address != null) linux.invalidateCache(address);
  }

  /** 40k34b: globaler ADB-Status (Pfad, Version, verbundene/autorisierte/nicht autorisierte/offline Geräte). */
  public AndroidAdbService.AdbStatus androidAdbStatus() {
    return adb.status();
  }

  /** 40k34b: modernes Wireless-Debugging-Pairing. Der Code verlässt diese Methode nicht in Logs/Fehlern. */
  public AndroidAdbService.ActionResult pairAndroidDevice(String host, int pairingPort, String pairingCode) {
    return adb.pair(host, pairingPort, pairingCode);
  }

  /**
   * 40k34b: Stellt eine ADB-Verbindung her (klassisches TCP/IP oder nach erfolgreichem
   * Pairing). Wird optional ein Identitätsschlüssel übergeben, wird die Verbindungsadresse
   * für die spätere automatische Wiederverbindung an genau dieser Identität gespeichert -
   * es entsteht dabei nie ein neues Gerät.
   */
  public AndroidAdbService.ActionResult connectAndroidDevice(String identityKey, String host, int port) {
    var result = adb.connect(host, port);
    String serial = host + ":" + port;
    if (!result.success()) {
      return new AndroidAdbService.ActionResult(false, androidConnectionHint(serial, result.message()));
    }
    var status = adb.status();
    var device = status.devices().stream().filter(d -> serial.equals(d.serial())).findFirst().orElse(null);
    if (device == null) {
      return new AndroidAdbService.ActionResult(false, androidConnectionHint(serial,
        "ADB meldete zwar einen Verbindungsversuch, das Gerät erscheint danach aber nicht in der Geräteliste."));
    }
    if (!"device".equals(device.state())) {
      String message = "unauthorized".equals(device.state())
        ? "Das Gerät ist erreichbar, wartet aber auf die Bestätigung der ADB-Freigabe am Android-Gerät."
        : "Das Gerät wurde mit dem Status '" + device.state() + "' erkannt und ist noch nicht einsatzbereit.";
      return new AndroidAdbService.ActionResult(false, message);
    }
    if (identityKey != null && !identityKey.isBlank()) {
      registrations.find(identityKey); // wirft, falls Identität nicht existiert
      registrations.recordAdbConnection(identityKey, host, port);
    }
    return new AndroidAdbService.ActionResult(true, "Android-Gerät " + serial + " ist verbunden und autorisiert.");
  }

  private String androidConnectionHint(String serial, String technicalMessage) {
    String detail = technicalMessage == null ? "" : technicalMessage.trim();
    return "Verbindung zu " + serial + " konnte nicht hergestellt werden. "
      + "Bitte am Android-Gerät unter Entwickleroptionen > Drahtloses Debugging prüfen, ob WLAN-Debugging eingeschaltet ist, "
      + "und den dort aktuell angezeigten Verbindungsport in GAM eintragen. Eine neue Kopplung ist erst nötig, wenn die Verbindung mit dem aktuellen Port weiterhin scheitert."
      + (detail.isBlank() ? "" : " Technische Rückmeldung: " + detail);
  }

  public AndroidAdbService.ActionResult disconnectAndroidDevice(String host, int port) {
    return adb.disconnect(host, port);
  }

  /**
   * 40k34b: Führt die ADB-Inventarisierung für ein bereits bekanntes Android-Gerät durch
   * und übernimmt das Ergebnis über die bereits bestehende recordDiscoveryHit()-Schreiblogik
   * in dieselbe Geräteidentität - keine zweite Android-Inventarisierung, kein neues Gerät.
   */
  public LinuxActionResult runAndroidInventory(String identityKey) {
    Map<String,Object> row = registrations.find(identityKey);
    String adbHost = str(row.get("adbHost"));
    Object adbPortObj = row.get("adbPort");
    String address = str(row.get("address"));
    if (adbHost == null || adbPortObj == null) {
      return new LinuxActionResult(false, "Für dieses Gerät ist noch keine ADB-Verbindung bekannt. Bitte zuerst koppeln/verbinden.", detail(identityKey));
    }
    int adbPort = ((Number) adbPortObj).intValue();
    String serial = adbHost + ":" + adbPort;
    var status = adb.status();
    boolean connectedAuthorized = status.devices().stream().anyMatch(d -> serial.equals(d.serial()) && "device".equals(d.state()));
    if (!connectedAuthorized) {
      var reconnect = adb.connect(adbHost, adbPort);
      if (!reconnect.success()) {
        return new LinuxActionResult(false, androidConnectionHint(serial, reconnect.message()), detail(identityKey));
      }
      status = adb.status();
      connectedAuthorized = status.devices().stream().anyMatch(d -> serial.equals(d.serial()) && "device".equals(d.state()));
      if (!connectedAuthorized) {
        return new LinuxActionResult(false, "Gerät " + serial + " wurde erreicht, ist aber noch nicht autorisiert. Bitte die Verbindungsfreigabe auf dem Android-Gerät bestätigen.", detail(identityKey));
      }
      registrations.recordAdbConnection(identityKey, adbHost, adbPort);
    }
    var inventory = adb.inventorize(serial);
    if (!inventory.success()) {
      return new LinuxActionResult(false, "ADB-Inventarisierung lieferte keine Daten (" + inventory.status() + ").", detail(identityKey));
    }
    Map<String,String> fields = inventory.fields();
    StringBuilder protocol = new StringBuilder("ADB (WLAN)");
    for (var e : fields.entrySet()) protocol.append(" · ").append(e.getKey()).append(": ").append(e.getValue());
    protocol.append(" · Inventarisierungsstatus: erfolgreich · Inventarisierungsdauer: ").append(inventory.durationMs() / 1000.0).append(" s");
    String name = firstNonBlank(fields.get("Gerätename"), fields.get("Modell"), str(row.get("name")));
    String manufacturer = firstNonBlank(fields.get("Hersteller"), str(row.get("manufacturer")));
    String serialNumber = fields.get("Seriennummer"); // nur wenn vom Gerät freigegeben - siehe AndroidInventoryParser
    registrations.recordDiscoveryHit(address, str(row.get("hardwareAddress")), serialNumber, name,
      str(row.get("deviceType")), manufacturer, protocol.toString(), "ONLINE");
    return new LinuxActionResult(true, "ADB-Inventarisierung wurde durchgeführt (" + fields.size() + " Merkmale erfasst).", detail(identityKey));
  }

  public record AppInventoryRunResult(boolean success, String status, String message, int appCount,
                                      int userAppCount, int systemAppCount, String changesSummary) {}

  /**
   * 40k34c: Führt einen App-/Paketinventarlauf für ein bereits über ADB verbundenes
   * Android-Gerät durch. Nutzt ausschließlich die bereits bestehenden
   * AndroidAdbService-Methoden (keine zweite ADB-Architektur) und schreibt in die
   * NEUEN, aber generisch gehaltenen App-Tabellen - niemals als eigenständiges Gerät,
   * niemals als Merge-Merkmal.
   */
  public AppInventoryRunResult runAndroidAppInventory(String identityKey) {
    Map<String,Object> row = registrations.find(identityKey);
    String adbHost = str(row.get("adbHost"));
    Object adbPortObj = row.get("adbPort");
    if (adbHost == null || adbPortObj == null) {
      return new AppInventoryRunResult(false, "NICHT_ERREICHBAR", "Für dieses Gerät ist noch keine ADB-Verbindung bekannt.", 0, 0, 0, null);
    }
    String serial = adbHost + ":" + ((Number) adbPortObj).intValue();
    var status = adb.status();
    if (!status.available()) return new AppInventoryRunResult(false, "NICHT_ERREICHBAR", "ADB ist nicht verfügbar.", 0, 0, 0, null);
    boolean authorized = status.devices().stream().anyMatch(d -> serial.equals(d.serial()) && "device".equals(d.state()));
    boolean unauthorizedFound = status.devices().stream().anyMatch(d -> serial.equals(d.serial()) && "unauthorized".equals(d.state()));
    if (!authorized && !unauthorizedFound) {
      var reconnect = adb.connect(adbHost, ((Number) adbPortObj).intValue());
      if (reconnect.success()) {
        registrations.recordAdbConnection(identityKey, adbHost, ((Number) adbPortObj).intValue());
        status = adb.status();
        authorized = status.devices().stream().anyMatch(d -> serial.equals(d.serial()) && "device".equals(d.state()));
        unauthorizedFound = status.devices().stream().anyMatch(d -> serial.equals(d.serial()) && "unauthorized".equals(d.state()));
      }
    }
    if (unauthorizedFound) return new AppInventoryRunResult(false, "NICHT_AUTORISIERT", "Bitte bestätigen Sie die ADB-Autorisierung auf dem Android-Gerät.", 0, 0, 0, null);
    if (!authorized) return new AppInventoryRunResult(false, "NICHT_ERREICHBAR", androidConnectionHint(serial, null), 0, 0, 0, null);

    long runId = appInventory.startRun(identityKey);
    long started = System.currentTimeMillis();
    log.info("[APPS] App-Inventarisierung gestartet für Gerät {}", identityKey);
    var lists = adb.listInstalledPackages(serial);
    record AppOccurrence(String packageName, int userId, String userName, boolean system) {}
    List<AppOccurrence> occurrences = new ArrayList<>();
    for (var userList : lists.userPackageLists()) {
      for (String pkg : userList.packages()) occurrences.add(new AppOccurrence(pkg, userList.userId(), userList.userName(), false));
    }
    for (String pkg : lists.systemPackages()) occurrences.add(new AppOccurrence(pkg, -1, "System", true));
    if (occurrences.isEmpty()) {
      appInventory.finishRun(runId, "FEHLGESCHLAGEN", 0, 0, 0, null, "Paketliste konnte nicht gelesen werden.");
      log.info("[APPS] App-Inventarisierung für {} fehlgeschlagen: keine Paketliste erhalten", identityKey);
      return new AppInventoryRunResult(false, "FEHLGESCHLAGEN", "Paketliste konnte nicht gelesen werden.", 0, 0, 0, null);
    }
    var dumpResult = adb.packageDumpDetailed(serial);
    Map<String,AndroidAppInventoryParser.AppRecord> details = AndroidAppInventoryParser.parse(dumpResult.output());
    String launcher = adb.resolveDefaultLauncher(serial);
    String browser = adb.resolveDefaultBrowser(serial);

    List<String> previousActive = appInventory.activePackageNames(identityKey);
    Set<String> previousSet = new HashSet<>(previousActive);
    List<String> currentKeys = new ArrayList<>();
    int added = 0, updated = 0, userCount = 0, systemCount = 0, incompleteDetails = 0;
    for (AppOccurrence occurrence : occurrences) {
      String pkg = occurrence.packageName();
      String occurrenceKey = occurrence.userId() + ":" + pkg;
      currentKeys.add(occurrenceKey);
      boolean isSystem = occurrence.system();
      boolean isDisabled = lists.disabledPackages().contains(pkg);
      if (isSystem) systemCount++; else userCount++;
      var detail = details.get(pkg);
      if (detail == null) incompleteDetails++;
      List<String> roles = new ArrayList<>();
      if (pkg.equals(launcher)) roles.add("Launcher");
      if (pkg.equals(browser)) roles.add("Standardbrowser");
      var app = new AppInventoryRepository.InstalledApp(
        pkg, occurrence.userId(), occurrence.userName(), null,
        detail != null ? detail.versionName() : null,
        detail != null ? detail.versionCode() : null,
        detail != null ? toTimestamp(detail.firstInstallTime()) : null,
        detail != null ? toTimestamp(detail.lastUpdateTime()) : null,
        detail != null ? detail.installerPackage() : null,
        isSystem, detail != null && detail.updatedSystem(), !isDisabled,
        detail != null && detail.debuggable(), detail != null && detail.testOnly(),
        detail != null ? detail.minSdk() : null, detail != null ? detail.targetSdk() : null,
        roles.isEmpty() ? null : String.join(", ", roles));
      appInventory.upsertApp(identityKey, app);
      if (!previousSet.contains(occurrenceKey)) added++; else if (detail != null) updated++;
    }
    List<String> removedPackages = previousActive.stream().filter(p -> !currentKeys.contains(p)).toList();

    int detailCoverage = occurrences.isEmpty() ? 0 : (int) Math.round((occurrences.size() - incompleteDetails) * 100.0 / occurrences.size());
    boolean complete = incompleteDetails == 0 && !dumpResult.timedOut() && !dumpResult.truncated();
    boolean usableWithHints = detailCoverage >= 75;
    String runStatus = complete ? "VOLLSTAENDIG" : usableWithHints ? "ERFOLGREICH_MIT_HINWEISEN" : "TEILWEISE";
    // Die Paketliste ist die maßgebliche Quelle für vorhandene/entfernte Apps.
    // Fehlende optionale Detaildaten dürfen die Entfernungserkennung nicht blockieren.
    appInventory.markRemoved(identityKey, currentKeys);

    String changesSummary = added + " neu, " + removedPackages.size() + " entfernt, " + updated + " aktualisiert";
    long duration = System.currentTimeMillis() - started;
    List<String> appWarnings = new ArrayList<>();
    if (incompleteDetails > 0) appWarnings.add(incompleteDetails + " von " + occurrences.size() + " Paketen ohne optionale Detaildaten (" + detailCoverage + " % Abdeckung)");
    if (dumpResult.warning() != null && !dumpResult.warning().isBlank()) appWarnings.add(dumpResult.warning());
    appInventory.finishRun(runId, runStatus, occurrences.size(), userCount, systemCount, changesSummary,
      appWarnings.isEmpty() ? null : String.join("; ", appWarnings));
    log.info("[APPS] App-Inventarisierung für {} abgeschlossen: {} ({} Apps, {} Benutzer-Apps, {} System-Apps, {}) in {} ms",
      identityKey, runStatus, occurrences.size(), userCount, systemCount, changesSummary, duration);

    // 40k34c: Inventarisierungsgrad-Merkmalsgruppe "Apps und Pakete" - dieselbe
    // "· Label: Value"-Konvention wie bei Linux/Android-Systeminventar, keine
    // separate Bewertung pro App.
    String protocol = str(row.get("protocol"));
    StringBuilder appendix = new StringBuilder(protocol == null ? "" : protocol);
    appendix.append(" · Apps und Pakete: ").append(occurrences.size()).append(" Pakete (")
      .append(userCount).append(" Benutzer-Apps, ").append(systemCount).append(" System-Apps)")
      .append(" · App-Inventarisierungsstatus: ").append(complete ? "erfolgreich" : usableWithHints ? "erfolgreich mit Hinweisen" : "teilweise")
      .append(" · Letzter App-Inventarlauf: ").append(OffsetDateTime.now());
    registrations.recordDiscoveryHit(str(row.get("address")), str(row.get("hardwareAddress")), str(row.get("serialNumber")),
      str(row.get("name")), str(row.get("deviceType")), str(row.get("manufacturer")), appendix.toString(), "ONLINE");

    String resultMessage = complete ? "App-Inventarisierung erfolgreich."
      : usableWithHints ? "App-Inventarisierung erfolgreich mit Hinweisen: " + String.join("; ", appWarnings)
      : "Teilinventarisierung: " + String.join("; ", appWarnings);
    return new AppInventoryRunResult(true, runStatus, resultMessage,
      occurrences.size(), userCount, systemCount, changesSummary);
  }

  public record PlatformInventoryResult(boolean success, String status, String message, String platform) {}

  /** 40k34m: die fünf im Auftrag genannten Plattformen - Grundlage für Status/"Alle Plattformen". */
  private static final List<String> SUPPORTED_PLATFORMS = List.of("Windows", "Linux", "Android", "macOS", "iOS");

  /**
   * 40k34m: Zentraler Einstiegspunkt für eine gezielte Erst- oder Nachinventarisierung
   * EINER Plattform eines bereits bekannten Geräts - unabhängig davon, ob und wie
   * Discovery dieses Gerät bereits erkannt hat (Trennung Discovery/Inventarisierung).
   * Ruft ausschließlich bereits bestehende, plattformspezifische Methoden auf - keine
   * zweite Inventarisierungslogik, keine Android-Sonderbehandlung im Vergleich zu den
   * übrigen Plattformen. Für Windows/macOS/iOS existiert aktuell kein Mechanismus, ein
   * FREMDES Gerät fernzuinventarisieren - das wird hier ehrlich als "nicht unterstützt"
   * zurückgemeldet statt vorgetäuscht (siehe Dokumentation).
   */
  public PlatformInventoryResult runPlatformInventory(String identityKey, String platform, String actor) {
    Map<String,Object> row = registrations.find(identityKey); // wirft, falls Identität nicht existiert
    // 40k34r: verhindert doppelte, gleichzeitig laufende Inventarisierungen derselben
    // Plattform für dasselbe Gerät - gilt sowohl für die manuelle als auch die
    // automatische Auslösung (keine zweite, separate Prüfung nötig).
    if (platformInventory.isRunning(identityKey, platform)) {
      return new PlatformInventoryResult(false, "LAEUFT", "Für diese Plattform läuft bereits eine Inventarisierung - bitte warten, bis sie abgeschlossen ist.", platform);
    }
    long runId = platformInventory.startRun(identityKey, platform, actor);
    try {
      PlatformInventoryResult result = switch (platform) {
        case "Android" -> dispatchAndroidPlatformInventory(identityKey);
        case "Linux" -> dispatchLinuxPlatformInventory(row);
        case "Windows" -> dispatchWindowsPlatformInventory(identityKey, row);
        case "macOS" -> dispatchMacOsPlatformInventory(identityKey);
        case "iOS" -> new PlatformInventoryResult(false, "NICHT_UNTERSTUETZT", "iOS-Inventarisierung ist in dieser Version noch nicht implementiert.", platform);
        default -> new PlatformInventoryResult(false, "FEHLGESCHLAGEN", "Unbekannte Plattform: " + platform, platform);
      };
      platformInventory.finishRun(runId, result.status(), result.message());
      log.info("[PLATFORM-INVENTORY] {} für {} durch {}: {} - {}", platform, identityKey, actor, result.status(), result.message());
      return result;
    } catch (Exception e) {
      platformInventory.finishRun(runId, "FEHLGESCHLAGEN", e.getMessage());
      log.warn("[PLATFORM-INVENTORY] {} für {} fehlgeschlagen: {}", platform, identityKey, e.getMessage());
      return new PlatformInventoryResult(false, "FEHLGESCHLAGEN", e.getMessage(), platform);
    }
  }


  private PlatformInventoryResult dispatchMacOsPlatformInventory(String identityKey) {
    LinuxActionResult r=runMacOsInventory(identityKey);
    return new PlatformInventoryResult(r.success(),r.success()?"ERFOLGREICH":"FEHLGESCHLAGEN",r.message(),"macOS");
  }

  private PlatformInventoryResult dispatchAndroidPlatformInventory(String identityKey) {
    log.debug("[AUTO-INVENTORY] Android-Dispatcher aufgerufen für {}: Inventarisierer = AndroidAdbService/AppInventoryRepository (runAndroidInventory/runAndroidAppInventory)", identityKey);
    var systemResult = runAndroidInventory(identityKey);
    log.info("[AUTO-INVENTORY] Android-Systeminventar für {}: erfolgreich={}, Meldung='{}'", identityKey, systemResult.success(), systemResult.message());
    var appResult = runAndroidAppInventory(identityKey);
    log.info("[AUTO-INVENTORY] Android-App-Inventar für {}: erfolgreich={}, Status={}, Meldung='{}'", identityKey, appResult.success(), appResult.status(), appResult.message());
    boolean success = systemResult.success() || appResult.success();
    String status = !success ? "FEHLGESCHLAGEN"
      : (appResult.success() && ("VOLLSTAENDIG".equals(appResult.status()) || "ERFOLGREICH_MIT_HINWEISEN".equals(appResult.status()))) ? "ERFOLGREICH"
      : "TEILWEISE";
    String message = "System: " + systemResult.message() + " · Apps: " + appResult.message();
    // 40k34s: Diagnose "Anzahl gelieferter Detailfelder" und "Merge erfolgreich" -
    // recordDiscoveryHit() (Merge/Konsolidierung) läuft bereits innerhalb von
    // runAndroidInventory()/runAndroidAppInventory() selbst; hier wird zur
    // Nachvollziehbarkeit nur noch das Ergebnis (Feldanzahl im gespeicherten
    // Protokolltext) ausgelesen und protokolliert - keine zweite Merge-Logik.
    try {
      Map<String,Object> after = registrations.find(identityKey);
      String protocol = str(after.get("protocol"));
      int fieldCount = protocol == null ? 0 : (int) protocol.chars().filter(ch -> ch == '·').count();
      log.info("[AUTO-INVENTORY] Android-Merge für {} abgeschlossen: {} Detailfelder im Geräteprotokoll vorhanden", identityKey, fieldCount);
    } catch (Exception e) {
      log.debug("[AUTO-INVENTORY] Detailfeld-Zählung für {} übersprungen: {}", identityKey, e.getMessage());
    }
    return new PlatformInventoryResult(success, status, message, "Android");
  }

  private PlatformInventoryResult dispatchLinuxPlatformInventory(Map<String,Object> row) {
    String address = str(row.get("address"));
    if (address == null) return new PlatformInventoryResult(false, "NICHT_ERREICHBAR", "Für dieses Gerät ist keine IP-Adresse bekannt.", "Linux");
    linux.invalidateCache(address);
    var discovered = linux.inspectSingle(address, str(row.get("name")), null, false);
    if (discovered.isEmpty()) return new PlatformInventoryResult(false, "NICHT_ERREICHBAR", "Gerät war über SSH nicht erreichbar oder antwortete nicht als Linux-System.", "Linux");
    var d = discovered.get();
    registrations.recordDiscoveryHit(d.address(), d.hardwareAddress(), d.serialNumber(), d.name(), d.type(), d.manufacturer(), d.protocol(), d.status());
    return new PlatformInventoryResult(true, "ERFOLGREICH", "Linux-Inventarisierung erfolgreich.", "Linux");
  }

  /**
   * 40k34q: Windows-Dispatch - unterscheidet anhand des bereits bestehenden
   * Identitätsschlüssel-Präfixes "windows-local:" (siehe
   * WindowsInventoryDiscoveryService), ob es sich um den GAM-Host selbst
   * (lokale WMI-/PowerShell-Inventarisierung, unverändert seit 40k34p) oder
   * ein entferntes Windows-Gerät (neue WinRM-Ferninventarisierung) handelt -
   * dieselbe Übernahmelogik (recordDiscoveryHit()) für beide Fälle, keine
   * Windows-Sonderbehandlung im übrigen Dispatcher.
   */
  private PlatformInventoryResult dispatchWindowsPlatformInventory(String identityKey, Map<String,Object> row) {
    if (identityKey.startsWith("windows-local:")) {
      boolean[] found = {false};
      windowsLocal.scan(true, OffsetDateTime.now().toString(),
        d -> { found[0] = true; registrations.recordDiscoveryHit(d.address(), d.hardwareAddress(), d.serialNumber(), d.name(), d.type(), d.manufacturer(), d.protocol(), d.status()); },
        event -> {}, "manual-" + identityKey, 0);
      return found[0]
        ? new PlatformInventoryResult(true, "ERFOLGREICH", "Lokale Windows-Inventarisierung erfolgreich.", "Windows")
        : new PlatformInventoryResult(false, "FEHLGESCHLAGEN", "Lokale Windows-Inventarisierung lieferte kein Ergebnis (läuft GAM auf einem Windows-Host?).", "Windows");
    }
    String address = str(row.get("address"));
    if (address == null) return new PlatformInventoryResult(false, "NICHT_ERREICHBAR", "Für dieses Gerät ist keine IP-Adresse bekannt.", "Windows");
    var discovered = windowsRemote.inspectSingle(address, str(row.get("name")));
    if (discovered.isEmpty()) return new PlatformInventoryResult(false, "NICHT_ERREICHBAR",
      "Windows-Gerät war über WinRM nicht erreichbar oder die Anmeldung schlug fehl (gemeinsamer WinRM-Zugang konfiguriert?).", "Windows");
    var d = discovered.get();
    registrations.recordDiscoveryHit(d.address(), d.hardwareAddress(), d.serialNumber(), d.name(), d.type(), d.manufacturer(), d.protocol(), d.status());
    return new PlatformInventoryResult(true, "ERFOLGREICH", "Windows-Ferninventarisierung (WinRM) erfolgreich.", "Windows");
  }

  /**
   * 40k34m: "Alle bekannten Plattformen erneut inventarisieren" - ermittelt anhand
   * bereits gespeicherter Hinweise (Plattformfeld, ADB-Verbindung, Protokolltext),
   * welche Plattformen grundsätzlich zu diesem Gerät passen, und inventarisiert
   * nacheinander nur diese - nicht passende Plattformen werden übersprungen.
   */
  /**
   * 40k34r: EIN gemeinsamer Ort für die Frage "welche Plattformen passen zu diesem
   * Gerät, basierend auf bereits gespeicherten Hinweisen?" - vorher nur inline in
   * runAllKnownPlatforms() vorhanden, jetzt zusätzlich vom automatischen Trigger
   * genutzt. Keine zweite Heuristik.
   */
  private List<String> applicablePlatforms(Map<String,Object> row) {
    // 40k34s: KORREKTUR - row.get("platform") existierte nie (DiscoveryRegistrationRepository.
    // find() liefert keine solche Spalte; "Plattform" wird ausschließlich über platformOf()
    // berechnet, nie gespeichert). Dadurch war platformField hier IMMER null und androidLikely/
    // linuxLikely/windowsLikely konnten nur über adbHost bzw. wörtliche Protokoll-Treffer wahr
    // werden - für evidenzbasiert klassifizierte Android-Geräte (device_type="Smartphones &
    // Tablets", kein ADB verbunden, kein "android" im Protokolltext) war das NIE der Fall.
    String deviceType = str(row.get("deviceType"));
    String protocol = str(row.get("protocol"));
    String platform = platformOf(deviceType, protocol);
    String protocolLower = protocol == null ? "" : protocol.toLowerCase(Locale.ROOT);
    boolean androidLikely = row.get("adbHost") != null || "Android".equals(platform);
    boolean linuxLikely = "Linux".equals(platform) || protocolLower.contains("linux bestätigt: ja") || protocolLower.contains("distributions-id:");
    boolean windowsLikely = "Windows".equals(platform);
    log.debug("[AUTO-INVENTORY] Plattformermittlung für deviceType='{}': platformOf()='{}' -> Android={}, Linux={}, Windows={}",
      deviceType, platform, androidLikely, linuxLikely, windowsLikely);
    List<String> platforms = new ArrayList<>();
    if (androidLikely) platforms.add("Android");
    if (linuxLikely) platforms.add("Linux");
    if (windowsLikely) platforms.add("Windows");
    return platforms;
  }

  public List<PlatformInventoryResult> runAllKnownPlatforms(String identityKey, String actor) {
    Map<String,Object> row = registrations.find(identityKey);
    List<PlatformInventoryResult> results = new ArrayList<>();
    for (String platform : applicablePlatforms(row)) results.add(runPlatformInventory(identityKey, platform, actor));
    return results;
  }

  private static final int AUTO_INVENTORY_COOLDOWN_MINUTES = 30;
  private final java.util.concurrent.ExecutorService autoInventoryExecutor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();

  /**
   * 40k34r: Automatischer Anstoß der Plattforminventarisierung unmittelbar nach
   * erfolgreicher Discovery. Wird von DeviceDiscoveryService für jedes in einem
   * Suchlauf gesehene, bereits registrierte Gerät aufgerufen. Läuft vollständig
   * asynchron (eigener Executor) - blockiert Discovery in keinem Fall. Startet
   * NUR, wenn: die Plattform anhand bereits gespeicherter Hinweise passt,
   * aktuell keine Inventarisierung derselben Plattform läuft, und der Cooldown
   * (Standard 30 Minuten) verstrichen ist. Gilt ausdrücklich NICHT für die
   * manuelle "Neu inventarisieren"-Aktion (runPlatformInventory() direkt) -
   * diese bleibt jederzeit ohne Cooldown möglich.
   */
  public void autoTriggerPlatformInventoryIfEligible(String identityKey) {
    Map<String,Object> row;
    try { row = registrations.find(identityKey); } catch (Exception e) {
      log.debug("[AUTO-INVENTORY] {} übersprungen: Identität nicht (mehr) auffindbar", identityKey);
      return;
    }
    List<String> platforms = applicablePlatforms(row);
    if (platforms.isEmpty()) {
      log.debug("[AUTO-INVENTORY] {} (deviceType='{}'): Gerät geeignet? NEIN - keine unterstützte Plattform anhand vorhandener Hinweise erkennbar", identityKey, row.get("deviceType"));
      return;
    }
    for (String platform : platforms) {
      log.debug("[AUTO-INVENTORY] {} (deviceType='{}'): Gerät geeignet? JA - Plattform {} erkannt", identityKey, row.get("deviceType"), platform);
      if (platformInventory.isRunning(identityKey, platform)) {
        log.info("[AUTO-INVENTORY] {} für {} übersprungen: läuft bereits eine Inventarisierung dieser Plattform", platform, identityKey);
        continue;
      }
      if (platformInventory.recentlyRun(identityKey, platform, AUTO_INVENTORY_COOLDOWN_MINUTES)) {
        log.debug("[AUTO-INVENTORY] {} für {} übersprungen: Cooldown ({} Minuten) noch aktiv", platform, identityKey, AUTO_INVENTORY_COOLDOWN_MINUTES);
        continue;
      }
      String platformToRun = platform;
      log.info("[AUTO-INVENTORY] {} für {}: Dispatcher wird aufgerufen (Grund: unmittelbar nach Discovery als passend erkannt, weder aktiv noch im Cooldown)", platformToRun, identityKey);
      autoInventoryExecutor.submit(() -> {
        try {
          long started = System.currentTimeMillis();
          PlatformInventoryResult result = runPlatformInventory(identityKey, platformToRun, "automatisch (nach Discovery)");
          log.info("[AUTO-INVENTORY] {} für {} abgeschlossen: erfolgreich={}, Status={}, Meldung='{}' ({} ms)",
            platformToRun, identityKey, result.success(), result.status(), result.message(), System.currentTimeMillis() - started);
        } catch (Exception e) {
          log.warn("[AUTO-INVENTORY] {} für {} fehlgeschlagen (unerwartete Ausnahme): {}", platformToRun, identityKey, e.getMessage());
        }
      });
    }
  }

  /** 40k34m: Statusübersicht je Plattform - "NOCH_NIE", falls für diese Plattform noch kein Lauf existiert. */
  public List<Map<String,Object>> platformInventoryStatus(String identityKey) {
    registrations.find(identityKey); // wirft, falls Identität nicht existiert
    List<Map<String,Object>> existing = platformInventory.latestRunsByPlatform(identityKey);
    Map<String,Map<String,Object>> byPlatform = new HashMap<>();
    for (var r : existing) byPlatform.put(String.valueOf(r.get("platform")), r);
    List<Map<String,Object>> result = new ArrayList<>();
    for (String platform : SUPPORTED_PLATFORMS) {
      Map<String,Object> row = byPlatform.get(platform);
      if (row != null) { result.add(row); continue; }
      Map<String,Object> none = new LinkedHashMap<>();
      none.put("platform", platform); none.put("status", "NOCH_NIE"); none.put("startedAt", null);
      none.put("finishedAt", null); none.put("message", null); none.put("durationSeconds", null);
      result.add(none);
    }
    return result;
  }

  public List<Map<String,Object>> androidInstalledApps(String identityKey) {
    return appInventory.currentApps(identityKey);
  }

  public List<Map<String,Object>> androidAppRunHistory(String identityKey, int limit) {
    return appInventory.recentRuns(identityKey, limit);
  }

  /** 40k34f: Aggregierte App-Zusammenfassung über alle Android-Geräte, für den Report. */
  public Map<String,Object> androidAppsGlobalSummary() {
    return appInventory.globalAppSummary();
  }

  private static Object toTimestamp(Long epochMs) {
    return epochMs == null ? null : java.sql.Timestamp.from(java.time.Instant.ofEpochMilli(epochMs));
  }

  /**
   * 40k34b: Begrenzte, nachvollziehbare automatische Wiederverbindung zu bereits bekannten
   * ADB-Netzwerkzielen - höchstens wenige Versuche, kein Dauerpolling, kein Endlos-Loop.
   */
  public List<String> reconnectKnownAndroidDevices() {
    List<String> results = new ArrayList<>();
    int attempts = 0;
    for (Map<String,Object> target : registrations.knownAdbTargets()) {
      if (attempts >= 5) break;
      String host = str(target.get("adbHost"));
      Object portObj = target.get("adbPort");
      if (host == null || portObj == null) continue;
      attempts++;
      var result = adb.connect(host, ((Number) portObj).intValue());
      results.add(target.get("identityKey") + ": " + (result.success() ? "erfolgreich" : result.message()));
    }
    return results;
  }

  private static String firstNonBlank(String... values) {
    for (String v : values) if (v != null && !v.isBlank()) return v;
    return null;
  }

  /**
   * 40k33b7: Integritätsprüfung einer bereits zusammengeführten Identität. Prüft
   * ausschließlich - trifft aber KEINE automatische Entscheidung:
   * 1. Ob diese Identität aktuell einem anderen, bereits registrierten Gerät als
   *    harter Konflikt ("RED") gegenübersteht - Wiederverwendung derselben
   *    Kandidaten-/Konflikterkennung wie bei der Zusammenführung selbst
   *    (DeviceMergeService.findCandidates()), keine zweite Bewertungslogik.
   * 2. Bei Linux-Geräten mit konfiguriertem SSH-Zugang zusätzlich: hat sich der
   *    SSH-Hostkey seit der letzten Erkennung geändert? Ein geänderter Hostkey ist
   *    ein sehr starkes Indiz für ein anderes physisches Gerät an derselben
   *    Identität.
   * Wird ausschließlich auf Anforderung ausgeführt (Öffnen des Bereichs
   * "Integritätsprüfung" im Dialog "Geräteidentität"), nicht automatisch geplant.
   */
  public IntegrityResult checkIntegrity(String identityKey) {
    Map<String,Object> row = registrations.find(identityKey);
    List<String> reasons = new ArrayList<>();
    List<String> rawConflicts = new ArrayList<>();
    List<DiscoveryRegistrationRepository.FieldComparisonRow> table = new ArrayList<>();
    String comparedWith = null;
    int conflictCount = 0;

    for (var c : mergeService.findCandidates()) {
      boolean involves = identityKey.equals(c.keyA()) || identityKey.equals(c.keyB());
      if (!involves || !"RED".equals(c.warningLevel())) continue;
      String other = identityKey.equals(c.keyA()) ? c.keyB() : c.keyA();
      Map<String,Object> otherRow = safeFind(other);
      if (otherRow == null) continue;
      comparedWith = other;
      List<String> conflicts = c.conflicts();
      conflictCount += Math.max(1, conflicts.size());
      rawConflicts.addAll(conflicts);
      for (String conflict : conflicts) reasons.add(conflict + " (im Vergleich zu " + displayName(otherRow) + ")");
      table.addAll(DiscoveryRegistrationRepository.compareIdentityFields(row, otherRow));
    }

    String platform = platformOf(str(row.get("deviceType")), str(row.get("protocol")));
    String storedHostKey = DiscoveryRegistrationRepository.detailValues(str(row.get("protocol"))).get("SSH-Hostkey");
    String address = str(row.get("address"));
    if ("Linux".equals(platform) && storedHostKey != null && address != null) {
      String current = linux.currentHostKeyFingerprint(address);
      if (current != null && !current.equalsIgnoreCase(storedHostKey)) {
        conflictCount++;
        String hostkeyConflict = "abweichender SSH-Hostkey";
        rawConflicts.add(hostkeyConflict);
        reasons.add("SSH-Hostkey hat sich geändert (zuletzt: " + storedHostKey + ", jetzt: " + current
          + ") - das deutet stark auf ein anderes physisches Gerät an derselben Identität hin.");
        table.add(new DiscoveryRegistrationRepository.FieldComparisonRow("SSH-Hostkey", storedHostKey, current, true));
      }
    }

    // 40k34e: Android-Ergänzungen derselben Integritätsprüfung - ausschließlich anhand
    // bereits gespeicherter Daten (keine ADB-Abfrage je Regel, siehe Performance-Vorgabe).
    String adbHost = str(row.get("adbHost"));
    Object adbPortObj = row.get("adbPort");
    if (adbHost != null && platform != null && !"Android".equals(platform)) {
      conflictCount++;
      String conflict = "ADB-Gerät wurde einem Datensatz mit nicht-Android-Plattform (" + platform + ") zugeordnet";
      rawConflicts.add(conflict);
      reasons.add(conflict + " - das ist technisch unplausibel und sollte geprüft werden.");
    }
    if (adbHost != null && adbPortObj != null) {
      List<String> boundElsewhere = registrations.otherIdentitiesWithSameAdbConnection(identityKey, adbHost, ((Number) adbPortObj).intValue());
      if (!boundElsewhere.isEmpty()) {
        conflictCount++;
        String conflict = "Zwei unterschiedliche Datensätze sind derzeit mit derselben ADB-Verbindung verknüpft (ADB)";
        rawConflicts.add(conflict);
        reasons.add(conflict + ": " + adbHost + ":" + adbPortObj + " ist außerdem an " + String.join(", ", boundElsewhere) + " gebunden.");
      }
    }
    if ("Android".equals(platform)) {
      List<Map<String,Object>> runs = appInventory.recentRuns(identityKey, 5);
      List<Map<String,Object>> completeRuns = runs.stream().filter(r -> "VOLLSTAENDIG".equals(r.get("status"))).toList();
      if (completeRuns.size() >= 2) {
        Map<String,Object> latestComplete = completeRuns.get(0);
        int latestCount = ((Number) latestComplete.getOrDefault("appCount", 0)).intValue();
        boolean earlierHadApps = completeRuns.stream().skip(1).anyMatch(r -> ((Number) r.getOrDefault("appCount", 0)).intValue() > 0);
        if (latestCount == 0 && earlierHadApps) {
          conflictCount++;
          String conflict = "Vollständiger App-Inventarlauf ohne Pakete trotz vorheriger Bestände";
          rawConflicts.add(conflict);
          reasons.add(conflict + " - möglicher Hinweis auf einen fehlerhaften Lauf statt einer tatsächlichen Änderung.");
        }
      }
      if (!completeRuns.isEmpty()) {
        boolean hasBasicData = DiscoveryRegistrationRepository.detailValues(str(row.get("protocol"))).get("Android-Version") != null;
        if (!hasBasicData) {
          conflictCount++;
          String conflict = "App-Inventarlauf meldet Erfolg, obwohl grundlegende Systemdaten fehlen";
          rawConflicts.add(conflict);
          reasons.add(conflict + " - bitte Systeminventarisierung erneut ausführen.");
        }
      }
    }

    String status = conflictCount == 0 ? "Integrität hoch" : conflictCount == 1 ? "Bitte überprüfen" : "Integritätswarnung";

    if (reasons.isEmpty()) reasons.add("Keine Auffälligkeiten anhand der zuletzt bekannten Discoverydaten.");
    // 40k33b10: dieselbe Schutzstufen-Einordnung wie im Merge-Dialog - derselbe Konflikt
    // erscheint hier und dort konsistent als kritische Schutzregel (keine zweite Klassifikation).
    String criticalityLevel = DiscoveryRegistrationRepository.highestConflictSeverity(rawConflicts);
    String criticalMessage = DiscoveryRegistrationRepository.requiresManualConfirmation(criticalityLevel) ? DiscoveryRegistrationRepository.criticalConflictMessage(rawConflicts) : null;
    return new IntegrityResult(status, reasons, table, comparedWith, OffsetDateTime.now().toString(), conflictCount,
      criticalityLevel, criticalMessage, DiscoveryRegistrationRepository.requiresManualConfirmation(criticalityLevel));
  }

  /**
   * 40k33b7: Ermittelt, was aus dieser Identität wieder abgespalten werden könnte -
   * aus den bekannten Aliasen sowie aus den Quell-Schnappschüssen früherer
   * Zusammenführungen (ab 40k33b7 gesichert, siehe DeviceMergeRepository).
   * Vor 40k33b7 durchgeführte Zusammenführungen haben keinen Schnappschuss und
   * liefern daher nur den Alias als Ansatzpunkt.
   */
  public List<SplitCandidate> splitCandidates(String identityKey) {
    List<SplitCandidate> result = new ArrayList<>();
    for (String alias : registrations.aliasesFor(identityKey)) {
      result.add(new SplitCandidate("ALIAS:" + alias, "Alias „" + alias + "“ als eigenständiges Gerät abspalten",
        alias, null, null, null, null, null, false));
    }
    List<String> snapshots = merges.sourceSnapshotsForTarget(identityKey);
    for (int listIndex = 0; listIndex < snapshots.size(); listIndex++) {
      List<Map<String,Object>> entries = parseSnapshot(snapshots.get(listIndex));
      for (int entryIndex = 0; entryIndex < entries.size(); entryIndex++) {
        Map<String,Object> e = entries.get(entryIndex);
        result.add(new SplitCandidate("SNAPSHOT:" + listIndex + ":" + entryIndex,
          "Frühere Zusammenführung: „" + str(e.get("name")) + "“ (mit vollständigen Merkmalen)",
          str(e.get("name")), str(e.get("deviceType")), str(e.get("address")), str(e.get("hardwareAddress")),
          str(e.get("serialNumber")), str(e.get("manufacturer")), true));
      }
    }
    return result;
  }

  /**
   * 40k33b7: Führt eine vom Benutzer bestätigte Wiederauftrennung durch. Legt ein
   * NEUES, eigenständiges Gerät über die bereits vorhandene register()-Methode an
   * (dieselbe Identitätsauflösung wie bei jedem regulären Discovery-Treffer) und
   * entfernt - nur bei einer alias-basierten Auswahl - den betreffenden Alias vom
   * Ursprungsgerät. Das Ursprungsgerät selbst bleibt unverändert erhalten, damit
   * keine Informationen verloren gehen; die Zusammenführung wird NICHT rückgängig
   * gemacht, sondern es entsteht zusätzlich ein neues, separates Gerät.
   */
  public Map<String,Object> split(String identityKey, String ref, String actor) {
    registrations.find(identityKey); // wirft, falls Identität nicht existiert
    String name, deviceType = null, address = null, mac = null, serial = null, manufacturer = null;
    if (ref == null) throw new IllegalArgumentException("Auswahl fehlt.");
    if (ref.startsWith("ALIAS:")) {
      name = ref.substring("ALIAS:".length());
    } else if (ref.startsWith("SNAPSHOT:")) {
      String[] parts = ref.split(":", 3);
      if (parts.length != 3) throw new IllegalArgumentException("Ungültige Auswahl.");
      int listIndex = Integer.parseInt(parts[1]), entryIndex = Integer.parseInt(parts[2]);
      List<String> snapshots = merges.sourceSnapshotsForTarget(identityKey);
      if (listIndex < 0 || listIndex >= snapshots.size()) throw new IllegalArgumentException("Dieser Schnappschuss ist nicht mehr verfügbar.");
      List<Map<String,Object>> entries = parseSnapshot(snapshots.get(listIndex));
      if (entryIndex < 0 || entryIndex >= entries.size()) throw new IllegalArgumentException("Dieser Eintrag ist nicht mehr verfügbar.");
      Map<String,Object> e = entries.get(entryIndex);
      name = str(e.get("name")); deviceType = str(e.get("deviceType")); address = str(e.get("address"));
      mac = str(e.get("hardwareAddress")); serial = str(e.get("serialNumber")); manufacturer = str(e.get("manufacturer"));
    } else {
      throw new IllegalArgumentException("Unbekannte Auswahl.");
    }
    if (name == null || name.isBlank()) throw new IllegalArgumentException("Für die Wiederauftrennung wird mindestens ein Name benötigt.");

    registrations.register(address, mac, serial, name, deviceType, manufacturer, "Wiederauftrennung (manuell)", "UNBEKANNT");
    String newKey = DiscoveryRegistrationRepository.resolveKey(address, mac, serial, name);
    if (ref.startsWith("ALIAS:")) registrations.removeAlias(identityKey, name);
    merges.logSplit(identityKey, newKey, "Wiederauftrennung: „" + name + "“ als eigenständiges Gerät abgespalten.", actor);
    return registrations.find(newKey);
  }

  private List<Map<String,Object>> parseSnapshot(String json) {
    try {
      return objectMapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<List<Map<String,Object>>>() {});
    } catch (Exception e) { return List.of(); }
  }

  private Map<String,Object> safeFind(String key) {
    try { return registrations.find(key); } catch (Exception e) { return null; }
  }

  private static String displayName(Map<String,Object> row) {
    String name = str(row.get("name"));
    return name != null ? name : str(row.get("identityKey"));
  }

  public Map<String,Object> addAlias(String identityKey, String alias) {
    registrations.find(identityKey);
    registrations.addAlias(identityKey, alias);
    return Map.of("identityKey", identityKey, "aliases", registrations.aliasesFor(identityKey));
  }

  public Map<String,Object> removeAlias(String identityKey, String alias) {
    registrations.removeAlias(identityKey, alias);
    return Map.of("identityKey", identityKey, "aliases", registrations.aliasesFor(identityKey));
  }

  public Map<String,Object> renameAlias(String identityKey, String oldAlias, String newAlias) {
    registrations.renameAlias(identityKey, oldAlias, newAlias);
    return Map.of("identityKey", identityKey, "aliases", registrations.aliasesFor(identityKey));
  }

  /**
   * "Hauptnamen ändern" bzw. "Alias zum Hauptnamen machen" - beides ist derselbe
   * bereits vorhandene, geschützte Operation (updateDeviceName legt den bisherigen
   * Namen automatisch als Alias ab, siehe DiscoveryRegistrationRepository).
   */
  public Map<String,Object> setMainName(String identityKey, String newName) {
    return registrations.updateDeviceName(identityKey, newName);
  }

  private Set<String> candidateKeys() {
    Set<String> keys = new HashSet<>();
    for (var c : mergeService.findCandidates()) { keys.add(c.keyA()); keys.add(c.keyB()); }
    return keys;
  }

  private IdentityRow toIdentityRow(Map<String,Object> row, Set<String> withOpenCandidate) {
    String identityKey = str(row.get("identityKey"));
    String deviceType = str(row.get("deviceType"));
    String protocol = str(row.get("protocol"));
    String[] categoryParts = splitCategory(deviceType);
    List<String> sources = DiscoveryRegistrationRepository.distinctSourceLabels(protocol);
    List<String> aliases = registrations.aliasesFor(identityKey);
    int mergeCount = merges.mergeCountForTarget(identityKey);
    boolean hasMac = str(row.get("hardwareAddress")) != null;
    boolean hasSerial = str(row.get("serialNumber")) != null;
    boolean hasManufacturer = str(row.get("manufacturer")) != null;
    ConfidenceAssessment confidence = assessConfidence(hasMac, hasSerial, hasManufacturer, sources.size(), aliases.size());

    return new IdentityRow(identityKey, str(row.get("name")), truthy(row.get("manualName")), deviceType,
      truthy(row.get("manualDeviceType")), categoryParts[0], categoryParts[1], platformOf(deviceType, protocol),
      str(row.get("address")), str(row.get("hardwareAddress")), str(row.get("serialNumber")),
      str(row.get("manufacturer")), str(row.get("status")), row.get("firstSeenAt"), row.get("lastSeenAt"),
      number(row.get("detectionCount")), (int) number(row.get("lastScanHits")), sources, sources.size(),
      aliases, aliases.size(), mergeCount, confidence.label(), confidence.reasons(),
      withOpenCandidate.contains(identityKey), protocol,
      str(row.get("adbHost")), row.get("adbPort") instanceof Number n ? n.intValue() : null, row.get("adbLastConnectedAt"));
  }

  /**
   * 40k33b5: Unterkategorie wird aus der bereits im ganzen Projekt verwendeten
   * Kategorie-Namenskonvention "Familie / Spezifisch" abgeleitet (z.B. "Energie /
   * Wechselrichter" -> Kategorie "Energie", Unterkategorie "Wechselrichter").
   * Keine neue Datenstruktur/Migration nötig.
   */
  private static String[] splitCategory(String type) {
    if (type == null || type.isBlank()) return new String[]{null, null};
    String[] parts = type.split("/", 2);
    if (parts.length == 2) return new String[]{parts[0].trim(), parts[1].trim()};
    return new String[]{type.trim(), null};
  }

  /** 40k34t: öffentlich gemacht (unverändert in der Logik), damit InventoryRepository
   * dieselbe, bereits korrigierte Plattformerkennung wiederverwenden kann, statt sie
   * ein zweites Mal zu implementieren. */
  public static String platformOf(String deviceType, String protocol) {
    String all = (nullToEmpty(deviceType) + " " + nullToEmpty(protocol)).toLowerCase(Locale.ROOT);
    if (all.contains("adb")) return "Android";
    if (all.contains("linux")) return "Linux";
    if (all.contains("windows") || all.contains("wmi") || all.contains("winrm") || all.contains("powershell")) return "Windows";
    if (all.contains("home assistant")) return "Home Assistant";
    if (all.contains("proxmox")) return "Proxmox";
    if (all.contains("macos") || all.contains("mac os")) return "macOS";
    // 40k34a: Android-Geräte nutzen dieselbe Plattform-Erkennung wie alle anderen Plattformen -
    // keine Sonderlogik, nur eine weitere Zeile nach demselben Muster.
    // 40k34s: KORREKTUR (Diagnose, siehe docs/40k34s-...): diese Zeile prüfte bisher
    // ausschließlich auf das wörtliche Vorkommen von "android"/"chromecast"/"fire tv"/
    // "shield" im Gerätetyp/Protokoll. Die evidenzbasierte Nachklassifizierung
    // (40k34h–n) schreibt für Android-Smartphones/-Tablets aber NICHT das Wort
    // "android", sondern ausschließlich die generische Zielkategorie
    // "Smartphones & Tablets" in device_type - unabhängig von Hersteller, Modell
    // oder Gerätename. Dadurch lieferte platformOf() für ALLE so klassifizierten
    // Android-Geräte bisher `null` ("Plattform: —" in der Oberfläche, keine
    // automatische Inventarisierung). Da im Projekt keine iOS-Inventarisierung
    // existiert, wird "Smartphones & Tablets" hier generisch als Android behandelt -
    // außer der Text nennt eindeutig iPhone/iPad (dann bewusst kein Android).
    if (all.contains("iphone") || all.contains("ipad")) return null;
    if (all.contains("android") || all.contains("chromecast") || all.contains("fire tv") || all.contains("shield")
      || all.contains("smartphones & tablets") || all.contains("smartphones und tablets")) return "Android";
    return null;
  }

  /**
   * 40k33b5: Vertrauensbewertung EINER bereits stabilisierten Identität (nach
   * einem erfolgreichen Merge gibt es keinen zweiten Datensatz mehr, mit dem
   * paarweise verglichen werden könnte - anders als bei der Kandidatenbewertung
   * in DeviceIdentityConfidenceEngine/DeviceMergeService). Bewertet daher, WIE GUT
   * diese eine Identität durch eindeutige Merkmale und mehrere Quellen abgesichert
   * ist, nicht ob sie mit einem anderen Datensatz übereinstimmt.
   */
  private static ConfidenceAssessment assessConfidence(boolean hasMac, boolean hasSerial, boolean hasManufacturer,
                                                        int sourceCount, int aliasCount) {
    List<String> reasons = new ArrayList<>();
    if (hasMac) reasons.add("MAC-Adresse bekannt");
    if (hasSerial) reasons.add("Seriennummer bekannt");
    if (hasManufacturer) reasons.add("Hersteller bekannt");
    if (sourceCount >= 2) reasons.add(sourceCount + " unabhängige Discovery-Quellen bestätigen dieses Gerät");
    if (aliasCount > 0) reasons.add(aliasCount + " bekannte(r) Alias(e)");
    boolean strongIdentifier = hasMac || hasSerial;
    String label;
    if (strongIdentifier && sourceCount >= 2) label = "Sehr sicher";
    else if (strongIdentifier || sourceCount >= 3) label = "Sicher";
    else if (sourceCount >= 2 || aliasCount > 0) label = "Teilweise bestätigt";
    else label = "Unsicher";
    if (reasons.isEmpty()) reasons.add("Nur eine einzelne Discovery-Quelle ohne eindeutige Hardwarekennung");
    return new ConfidenceAssessment(label, reasons);
  }

  private static long number(Object value) { return value instanceof Number n ? n.longValue() : 0L; }
  private static boolean truthy(Object value) { return Boolean.TRUE.equals(value) || number(value) == 1; }
  private static String str(Object value) { return value == null ? null : (value instanceof String s ? (s.isBlank() ? null : s) : String.valueOf(value)); }
  private static String nullToEmpty(String value) { return value == null ? "" : value; }
}
