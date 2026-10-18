package de.kopfzentrum.gam.inventory;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
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
  private final LinuxInventoryDiscoveryService linuxLocal;
  private final BuiltinDiscoverySettingsRepository builtinSettings;
  private final AndroidAdbService adb;
  private final AppInventoryRepository appInventory;
  private final SoftwareInventoryRepository softwareInventory;
  private final PlatformInventoryRepository platformInventory;
  private final WindowsInventoryDiscoveryService windowsLocal;
  private final WindowsRemoteInventoryService windowsRemote;
  private final MacOsInventoryService macOs;
  private final AppleMobileDiscoveryService appleMobile;
  private final ObjectMapper objectMapper;
  // 40k36t76: Standard=1, Administrator=2, SYSTEM=3.
  private record WindowsRun(long generation,int priority,String mode) {}
  private final java.util.concurrent.ConcurrentHashMap<String,WindowsRun> activeWindowsRuns=new java.util.concurrent.ConcurrentHashMap<>();
  private final java.util.concurrent.atomic.AtomicLong windowsRunGeneration=new java.util.concurrent.atomic.AtomicLong();
  private WindowsRun beginWindowsRun(String key,int priority,String mode){
    final WindowsRun[] chosen={null};
    activeWindowsRuns.compute(key,(k,current)->{
      if(current!=null&&current.priority()>priority){chosen[0]=current;return current;}
      WindowsRun next=new WindowsRun(windowsRunGeneration.incrementAndGet(),priority,mode);chosen[0]=next;
      if(current!=null)log.info("[WINDOWS-RUN] {}: {} wird durch {} ersetzt.",key,current.mode(),mode);
      return next;
    }); return chosen[0];
  }
  private boolean ownsWindowsRun(String key,WindowsRun run){return run!=null&&activeWindowsRuns.get(key)==run;}
  private void finishWindowsRun(String key,WindowsRun run){if(run!=null)activeWindowsRuns.remove(key,run);}

  public DeviceIdentityService(DiscoveryRegistrationRepository registrations, DeviceMergeRepository merges,
                               DeviceMergeService mergeService, LinuxNetworkDiscoveryService linux,
                               LinuxInventoryDiscoveryService linuxLocal,
                               BuiltinDiscoverySettingsRepository builtinSettings,
                               AndroidAdbService adb, AppInventoryRepository appInventory,
                               SoftwareInventoryRepository softwareInventory,
                               PlatformInventoryRepository platformInventory,
                               WindowsInventoryDiscoveryService windowsLocal, WindowsRemoteInventoryService windowsRemote,
                               MacOsInventoryService macOs, AppleMobileDiscoveryService appleMobile, ObjectMapper objectMapper) {
    this.registrations = registrations;
    this.merges = merges;
    this.mergeService = mergeService;
    this.linux = linux;
    this.linuxLocal = linuxLocal;
    this.builtinSettings = builtinSettings;
    this.adb = adb;
    this.appInventory = appInventory;
    this.softwareInventory = softwareInventory;
    this.platformInventory = platformInventory;
    this.windowsLocal = windowsLocal;
    this.windowsRemote = windowsRemote;
    this.macOs = macOs;
    this.appleMobile = appleMobile;
    this.objectMapper = objectMapper;
  }

  /**
   * 40k36p6t: Einmalige, idempotente Rückgewinnung bereits früher zusammengeführter
   * Netzwerkidentitäten aus den vorhandenen Merge-Schnappschüssen. Damit werden auch
   * Geräte sichtbar, die VOR diesem Build zusammengeführt wurden.
   */
  @PostConstruct
  void restoreMergedNetworkIdentities(){
    try {
      for (Map<String,Object> device : registrations.findAll()) {
        String targetKey=str(device.get("identityKey"));
        if(targetKey==null) continue;
        registrations.recordNetworkIdentity(targetKey,str(device.get("address")),str(device.get("hardwareAddress")),targetKey);
        for(String snapshotJson:merges.sourceSnapshotsForTarget(targetKey)){
          for(Map<String,Object> source:parseSnapshot(snapshotJson)){
            registrations.recordNetworkIdentity(targetKey,str(source.get("address")),str(source.get("hardwareAddress")),str(source.get("identityKey")));
          }
        }
      }
    } catch (Exception e) {
      log.warn("40k36p6t: Netzwerkidentitäten aus Merge-Schnappschüssen konnten nicht vollständig wiederhergestellt werden: {}", e.getMessage());
    }
  }

  public record ConfidenceAssessment(String label, List<String> reasons) {}

  public record IdentityRow(
    String identityKey, String name, boolean manualName, String deviceType, boolean manualDeviceType,
    String category, String subcategory, String platform, String address, String hardwareAddress,
    String serialNumber, String manufacturer, String status, Object firstSeenAt, Object lastSeenAt,
    long detectionCount, int lastScanHits, List<String> sources, int sourceCount,
    List<String> aliases, int aliasCount, int mergeCount, List<String> networkAddresses,
    List<String> networkHardwareAddresses, String confidenceLabel,
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
      var ssh = linux.testSshConnection(address);
      return new LinuxActionResult(false, ssh.reachable()
        ? "SSH ist erreichbar und die Anmeldung funktioniert, aber das Ziel konnte noch nicht als Linux-System bestätigt werden. Linux-Erkennungsdaten prüfen."
        : "Linux-Inventarisierung nicht möglich: SSH-Verbindung oder Anmeldung ist nicht erfolgreich. " + ssh.message(),
        detail(identityKey));
    }
    var device = result.get();
    registrations.recordVerifiedInventoryHit(device.address(), device.hardwareAddress(), device.serialNumber(),
      device.name(), device.type(), device.manufacturer(), device.protocol(), device.status());
    int softwareCount=softwareInventory.replaceLinuxFromDetails(identityKey,linux.consumeLatestSoftwareDetails(address));
    return new LinuxActionResult(true, "Linux-Inventarisierung wurde durchgeführt; "+softwareCount+" strukturierte Softwareeinträge wurden aktualisiert.", detail(identityKey));
  }

  /** 40k36n: Linux einmalig mit sudo-Kennwort tiefer inventarisieren. */
  public LinuxActionResult runLinuxElevatedInventory(String identityKey, String sudoPassword) {
    if (sudoPassword == null || sudoPassword.isBlank()) throw new IllegalArgumentException("Für den erhöhten Linux-Lauf ist das sudo-Kennwort erforderlich.");
    Map<String,Object> row = registrations.find(identityKey);
    String address = str(row.get("address"));
    if (address == null) throw new IllegalArgumentException("Für dieses Gerät ist keine IP-Adresse bekannt.");
    boolean manualCategory = truthy(row.get("manualDeviceType"));
    String fingerprintHint = (nullToEmpty(str(row.get("deviceType"))) + " " + nullToEmpty(str(row.get("protocol")))).trim();
    var result = linux.inspectSingleWithSudo(address, str(row.get("name")), fingerprintHint, manualCategory, sudoPassword);
    if (result.isEmpty()) return new LinuxActionResult(false, "Erhöhte Linux-Inventarisierung fehlgeschlagen. sudo-Berechtigung, Kennwort und SSH-Verbindung prüfen.", detail(identityKey));
    var d = result.get();
    registrations.recordVerifiedInventoryHit(d.address(), d.hardwareAddress(), d.serialNumber(), d.name(), d.type(), d.manufacturer(), d.protocol(), d.status());
    int softwareCount=softwareInventory.replaceLinuxFromDetails(identityKey,linux.consumeLatestSoftwareDetails(address));
    return new LinuxActionResult(true, "Linux wurde einmalig mit erhöhten sudo-Rechten inventarisiert; "+softwareCount+" strukturierte Softwareeinträge wurden aktualisiert. Das Kennwort wurde nicht gespeichert.", detail(identityKey));
  }

  /** 40k36r: macOS zuerst backend-lokal prüfen; nur fremde Macs benötigen SSH. */
  public LinuxActionResult runMacOsInventory(String identityKey) {
    Map<String,Object> row=registrations.find(identityKey); String address=str(row.get("address"));
    boolean backendLocal=macOs.isBackendLocalTarget(address,str(row.get("serialNumber")),str(row.get("name")));
    boolean localEnabled=builtinSettings.enabled("MACOS_LOCAL");
    boolean sshEnabled=builtinSettings.enabled("MACOS_SSH");
    if(backendLocal && !localEnabled){
      if(!sshEnabled) return new LinuxActionResult(false,"macOS lokal und macOS SSH sind deaktiviert; es wird keine Inventarisierung gestartet.",detail(identityKey));
      log.info("[PLATFORM-INVENTORY] macOS {} ist Backend-lokal, aber MACOS_LOCAL ist deaktiviert; aktiver Remote-Modus MACOS_SSH darf übernehmen.",identityKey);
      backendLocal=false;
    }
    if(!backendLocal && !sshEnabled) return new LinuxActionResult(false,"macOS SSH ist deaktiviert; entfernte macOS-Inventarisierung wird nicht gestartet.",detail(identityKey));
    if(address==null&&!backendLocal) throw new IllegalArgumentException("Für ein entferntes macOS-Gerät ist keine IP-Adresse bekannt.");
    if(backendLocal) log.info("[PLATFORM-INVENTORY] macOS {} wird Backend-lokal inventarisiert; MACOS_LOCAL hat Vorrang vor MACOS_SSH.",identityKey);
    else log.info("[PLATFORM-INVENTORY] macOS {} wird über SSH inventarisiert; MACOS_LOCAL ist nicht anwendbar/aktiv.",identityKey);
    var result=backendLocal
      ? macOs.inventoryDetailedLocal(address,str(row.get("name")),null)
      : macOs.inventoryDetailed(address,str(row.get("name")),null);
    if(result.isEmpty()) return new LinuxActionResult(false,backendLocal
      ?"Backend-lokale macOS-Inventarisierung lieferte keine Daten. Lokale Berechtigungen und macOS-Werkzeuge prüfen."
      :"macOS-Inventarisierung nicht möglich. Bitte auf dem entfernten Mac 'Entfernte Anmeldung' aktivieren und SSH-Konfiguration prüfen.",detail(identityKey));
    var snapshot=result.get(); var d=snapshot.device();
    registrations.recordVerifiedInventoryHit(d.address(),d.hardwareAddress(),d.serialNumber(),d.name(),d.type(),d.manufacturer(),d.protocol(),d.status());
    softwareInventory.replaceMacOs(identityKey,snapshot.software());
    return new LinuxActionResult(true,(backendLocal?"macOS backend-lokal":"macOS über SSH")+" erfolgreich inventarisiert; "+snapshot.software().size()+" Softwareeinträge strukturiert gespeichert; Benutzersoftware: "+snapshot.userSoftwareScanStatus()+".",detail(identityKey));
  }
  public LinuxActionResult runMacOsElevatedInventory(String identityKey,String sudoPassword) {
    if(sudoPassword==null||sudoPassword.isBlank()) throw new IllegalArgumentException("Für den erhöhten Lauf ist das macOS-Administratorkennwort erforderlich.");
    Map<String,Object> row=registrations.find(identityKey); String address=str(row.get("address"));
    boolean backendLocal=macOs.isBackendLocalTarget(address,str(row.get("serialNumber")),str(row.get("name")));
    boolean localEnabled=builtinSettings.enabled("MACOS_LOCAL");
    boolean sshEnabled=builtinSettings.enabled("MACOS_SSH");
    if(backendLocal && !localEnabled){ if(!sshEnabled) throw new IllegalStateException("macOS lokal und macOS SSH sind deaktiviert."); backendLocal=false; }
    if(!backendLocal && !sshEnabled) throw new IllegalStateException("macOS SSH ist deaktiviert.");
    if(address==null&&!backendLocal) throw new IllegalArgumentException("Für ein entferntes macOS-Gerät ist keine IP-Adresse bekannt.");
    var result=backendLocal
      ? macOs.inventoryDetailedLocal(address,str(row.get("name")),sudoPassword)
      : macOs.inventoryDetailed(address,str(row.get("name")),sudoPassword);
    if(result.isEmpty()) return new LinuxActionResult(false,backendLocal
      ?"Erhöhte backend-lokale macOS-Inventarisierung fehlgeschlagen. Kennwort und lokale sudo-Berechtigung prüfen."
      :"Erhöhte macOS-Inventarisierung fehlgeschlagen. Kennwort, sudo-Berechtigung und SSH-Verbindung prüfen.",detail(identityKey));
    var snapshot=result.get(); var d=snapshot.device();
    registrations.recordVerifiedInventoryHit(d.address(),d.hardwareAddress(),d.serialNumber(),d.name(),d.type(),d.manufacturer(),d.protocol(),d.status());
    softwareInventory.replaceMacOs(identityKey,snapshot.software());
    return new LinuxActionResult(true,"macOS wurde mit erhöhten Rechten vollständig inventarisiert; "+snapshot.software().size()+" Softwareeinträge strukturiert gespeichert; Benutzersoftware: "+snapshot.userSoftwareScanStatus()+". Das Kennwort wurde nicht gespeichert.",detail(identityKey));
  }
  public java.util.List<java.util.Map<String,Object>> softwareInventory(String identityKey){
    Map<String,Object> row=registrations.find(identityKey);
    var current=softwareInventory.current(identityKey);
    if(current.isEmpty()){
      String platform=platformOf(str(row.get("deviceType")),str(row.get("protocol")));
      if("macOS".equals(platform)) softwareInventory.replaceMacOsFromProtocol(identityKey,str(row.get("protocol")));
      else if("Linux".equals(platform)) softwareInventory.replaceLinuxFromProtocol(identityKey,str(row.get("protocol")));
      current=softwareInventory.current(identityKey);
    }
    return current;
  }

  public MacOsInventoryService.TestResult testMacOsSsh(String identityKey){Map<String,Object> row=registrations.find(identityKey);String address=str(row.get("address"));if(address==null)throw new IllegalArgumentException("Für dieses Gerät ist keine IP-Adresse bekannt.");return macOs.test(address);}
  public boolean isWindowsBackendLocal(String identityKey){
    try{
      Map<String,Object> row=registrations.find(identityKey);
      return System.getProperty("os.name","").toLowerCase(Locale.ROOT).contains("win") && isCurrentWindowsHost(identityKey,row);
    }catch(Exception e){
      return false;
    }
  }

  public LinuxActionResult runWindowsElevatedInventory(String identityKey,String username,String password) {
    Map<String,Object> row=registrations.find(identityKey); String address=str(row.get("address"));
    boolean backendLocal=System.getProperty("os.name","").toLowerCase(Locale.ROOT).contains("win") && isCurrentWindowsHost(identityKey,row);
    if(backendLocal){
      WindowsRun run=beginWindowsRun(identityKey,2,"Administrator");
      if(run.priority()>2)return new LinuxActionResult(false,"Ein höherwertiger SYSTEM-Inventarlauf ist bereits aktiv; der Administratorlauf wurde nicht gestartet.",detail(identityKey));
      log.info("[WINDOWS-ELEVATED] {} fordert native Windows-UAC-Zustimmung an; Backend-lokal-Erkennung bestätigt.",identityKey);
      boolean[] found={false};boolean[] elevatedConfirmed={false};boolean[] persisted={false};int[] softwareCount={0};
      try{
        windowsLocal.scanElevatedUac(true,OffsetDateTime.now().toString(),
          d->{found[0]=true;elevatedConfirmed[0]=d.protocol()!=null&&d.protocol().contains("Administratorrechte: Ja");
            if(!ownsWindowsRun(identityKey,run)){log.info("[WINDOWS-RUN] {}: überholtes Administrator-Ergebnis wird verworfen.",identityKey);return;}
            registrations.recordVerifiedInventoryHit(d.address(),d.hardwareAddress(),d.serialNumber(),d.name(),d.type(),d.manufacturer(),d.protocol(),d.status());persisted[0]=true;
            String resolved=registrations.resolveIdentityKey(d.address(),d.hardwareAddress(),d.serialNumber(),d.name());if(resolved!=null)softwareCount[0]=softwareInventory.replaceWindowsFromDetails(resolved,windowsLocal.consumeLatestSoftwareDetails());},
          event->{},"uac-"+identityKey,0);
        if(!found[0])return new LinuxActionResult(false,"Erhöhte lokale Windows-Inventarisierung wurde abgebrochen oder lieferte keine Daten.",detail(identityKey));
        if(!persisted[0])return new LinuxActionResult(false,"Administratorlauf wurde durch einen höherwertigen/neuen Lauf ersetzt; sein Ergebnis wurde verworfen.",detail(identityKey));
        if(!elevatedConfirmed[0])return new LinuxActionResult(false,"UAC-Lauf lieferte Inventardaten, aber der erhöhte Token wurde nicht bestätigt (Administratorrechte != Ja).",detail(identityKey));
        return new LinuxActionResult(true,"Windows wurde nach nativer UAC-Zustimmung mit bestätigten Administratorrechten inventarisiert; "+softwareCount[0]+" strukturierte Softwareeinträge aktualisiert.",detail(identityKey));
      }finally{finishWindowsRun(identityKey,run);}
    }
    if(username==null||username.isBlank()||password==null||password.isBlank()) throw new IllegalArgumentException("Für einen entfernten erhöhten Windows-Lauf sind Benutzername und Kennwort erforderlich.");
    if(address==null) throw new IllegalArgumentException("Für dieses Gerät ist keine IP-Adresse bekannt.");
    var result=windowsRemote.inspectSingleWithCredentials(address,str(row.get("name")),username,password);
    if(result.isEmpty()) return new LinuxActionResult(false,"Erhöhte Windows-Inventarisierung fehlgeschlagen. Administratorkonto, Kennwort, WinRM und Firewall prüfen.",detail(identityKey));
    var d=result.get(); registrations.recordVerifiedInventoryHit(d.address(),d.hardwareAddress(),d.serialNumber(),d.name(),d.type(),d.manufacturer(),d.protocol(),d.status());
    int softwareCount=softwareInventory.replaceWindowsFromDetails(identityKey,windowsRemote.consumeLatestSoftwareDetails(address));
    return new LinuxActionResult(true,"Windows wurde einmalig mit den angegebenen erhöhten Anmeldedaten inventarisiert; "+softwareCount+" strukturierte Softwareeinträge wurden aktualisiert. Das Kennwort wurde nicht gespeichert.",detail(identityKey));
  }


  public LinuxActionResult repairWindowsBitLockerProvider(String identityKey) {
    Map<String,Object> row=registrations.find(identityKey);
    if(!System.getProperty("os.name","").toLowerCase(Locale.ROOT).contains("win") || !isCurrentWindowsHost(identityKey,row))
      return new LinuxActionResult(false,"BitLocker-Provider-Reparatur ist nur für den backend-lokalen Windows-Rechner verfügbar.",detail(identityKey));

    String windir=System.getenv("WINDIR");
    if(windir==null||windir.isBlank()) windir="C:\\Windows";
    java.nio.file.Path mof=java.nio.file.Path.of(windir,"System32","wbem","win32_encryptablevolume.mof");
    if(!java.nio.file.Files.exists(mof))
      return new LinuxActionResult(false,"BitLocker-WMI-Provider kann nicht repariert werden: win32_encryptablevolume.mof wurde nicht gefunden.",detail(identityKey));

    try{
      String mofPath=mof.toAbsolutePath().toString().replace("'","''");
      String script=
        "$ErrorActionPreference='Stop';"+
        "$mof='"+mofPath+"';"+
        "$x=Start-Process -FilePath 'mofcomp.exe' -ArgumentList @($mof) -Verb RunAs -PassThru -Wait;"+
        "if($x.ExitCode -ne 0){exit $x.ExitCode}";
      Process process=new ProcessBuilder("powershell.exe","-NoLogo","-NoProfile","-NonInteractive","-ExecutionPolicy","Bypass","-Command",script)
        .redirectErrorStream(true).start();

      if(!process.waitFor(120,java.util.concurrent.TimeUnit.SECONDS)){
        process.destroyForcibly();
        return new LinuxActionResult(false,"BitLocker-WMI-Provider-Reparatur hat das Zeitlimit überschritten.",detail(identityKey));
      }
      if(process.exitValue()!=0)
        return new LinuxActionResult(false,"mofcomp konnte den BitLocker-WMI-Provider nicht erfolgreich registrieren.",detail(identityKey));

      String verify=
        "$ErrorActionPreference='Stop';"+
        "Get-CimInstance -Namespace 'root\\CIMV2\\Security\\MicrosoftVolumeEncryption' -ClassName Win32_EncryptableVolume -ErrorAction Stop|Select-Object -First 1|Out-Null;";
      Process check=new ProcessBuilder("powershell.exe","-NoLogo","-NoProfile","-NonInteractive","-ExecutionPolicy","Bypass","-Command",verify)
        .redirectErrorStream(true).start();

      if(!check.waitFor(60,java.util.concurrent.TimeUnit.SECONDS)){
        check.destroyForcibly();
        return new LinuxActionResult(false,"Provider wurde registriert, die anschließende Namespace-Prüfung lief jedoch in ein Timeout.",detail(identityKey));
      }
      if(check.exitValue()!=0)
        return new LinuxActionResult(false,"mofcomp wurde ausgeführt, der Namespace root\\CIMV2\\Security\\MicrosoftVolumeEncryption ist aber weiterhin nicht lesbar.",detail(identityKey));

      return new LinuxActionResult(true,"BitLocker-WMI-Provider wurde erfolgreich neu registriert. MicrosoftVolumeEncryption ist wieder verfügbar.",detail(identityKey));
    }catch(Exception e){
      log.warn("[BITLOCKER-REPAIR] Reparatur fehlgeschlagen: {}",e.getMessage());
      return new LinuxActionResult(false,"BitLocker-WMI-Provider-Reparatur fehlgeschlagen: "+e.getMessage(),detail(identityKey));
    }
  }

  public LinuxActionResult runWindowsSystemInventory(String identityKey) {
    Map<String,Object> row=registrations.find(identityKey);
    if(!System.getProperty("os.name","").toLowerCase(Locale.ROOT).contains("win") || !isCurrentWindowsHost(identityKey,row))
      return new LinuxActionResult(false,"SYSTEM-Inventarisierung ist nur für den backend-lokalen Windows-Rechner verfügbar.",detail(identityKey));
    WindowsRun run=beginWindowsRun(identityKey,3,"SYSTEM");
    log.info("[WINDOWS-SYSTEM] {} fordert einmalige UAC-Zustimmung für eine temporäre SYSTEM-Aufgabe an.",identityKey);
    boolean[] found={false};boolean[] systemConfirmed={false};boolean[] persisted={false};int[] softwareCount={0};
    try{
      windowsLocal.scanSystemTask(true,OffsetDateTime.now().toString(),
        d->{found[0]=true;systemConfirmed[0]=d.protocol()!=null&&d.protocol().contains("SYSTEM-Rechte: Ja");
          if(!ownsWindowsRun(identityKey,run)){log.info("[WINDOWS-RUN] {}: überholtes SYSTEM-Ergebnis wird verworfen.",identityKey);return;}
          registrations.recordVerifiedInventoryHit(d.address(),d.hardwareAddress(),d.serialNumber(),d.name(),d.type(),d.manufacturer(),d.protocol(),d.status());persisted[0]=true;
          String resolved=registrations.resolveIdentityKey(d.address(),d.hardwareAddress(),d.serialNumber(),d.name());
          if(resolved!=null)softwareCount[0]=softwareInventory.replaceWindowsFromDetails(resolved,windowsLocal.consumeLatestSoftwareDetails());},
        event->{},"system-"+identityKey,0);
      if(!found[0])return new LinuxActionResult(false,"SYSTEM-Inventarisierung wurde abgebrochen oder lieferte keine Daten.",detail(identityKey));
      if(!persisted[0])return new LinuxActionResult(false,"SYSTEM-Lauf wurde durch einen neueren SYSTEM-Lauf ersetzt; sein Ergebnis wurde verworfen.",detail(identityKey));
      if(!systemConfirmed[0])return new LinuxActionResult(false,"SYSTEM-Lauf lieferte Inventardaten, aber NT AUTHORITY\\SYSTEM wurde nicht bestätigt.",detail(identityKey));
      return new LinuxActionResult(true,"Windows wurde über eine temporäre Aufgabe mit bestätigten NT AUTHORITY\\SYSTEM-Rechten inventarisiert; "+softwareCount[0]+" strukturierte Softwareeinträge aktualisiert. Die temporäre Aufgabe wurde anschließend entfernt.",detail(identityKey));
    }finally{finishWindowsRun(identityKey,run);}
  }


  /** 40k33b8: Reiner SSH-Verbindungstest, ohne vollständige Inventarisierung. */
  /** 40k36p6: Verbindungstest direkt aus den Erkennungsquellen, ohne registrierte Geräteidentität. */
  public LinuxNetworkDiscoveryService.SshTestResult testLinuxSshHost(String host) {
    if (host == null || host.isBlank()) throw new IllegalArgumentException("Testhost fehlt.");
    String target=host.trim();
    var test=linux.testSshConnection(target);
    if(!test.reachable()) return test;
    String suffix=autoInventoryVerifiedSshHost(target, null);
    return new LinuxNetworkDiscoveryService.SshTestResult(test.sshConfigured(),test.reachable(),test.tcpReachable(),test.handshakeOk(),test.authOk(),test.message()+suffix,test.checkedAt());
  }

  /** 40k36q4b: Ein erfolgreicher SSH-Zugang ist zugleich eine verwertbare
   * Betriebssystemquelle. Darwin/Linux werden automatisch unterschieden und
   * unmittelbar inventarisiert; nur tatsächlich empfangene Inventardaten werden
   * als bestätigte SSH-Quelle persistiert. */
  public String autoInventoryVerifiedSshHost(String host, String nameHint){
    try {
      var macTest=macOs.test(host);
      if(macTest.reachable() && macTest.macOs()){
        var mac=macOs.inventoryDetailed(host,nameHint,null);
        if(mac.isPresent()){
          var snapshot=mac.get(); var d=snapshot.device();
          registrations.recordVerifiedInventoryHit(d.address(),d.hardwareAddress(),d.serialNumber(),d.name(),d.type(),d.manufacturer(),d.protocol(),d.status());
          String macIdentity=registrations.resolveIdentityKey(d.address(),d.hardwareAddress(),d.serialNumber(),d.name());
          if(macIdentity!=null) softwareInventory.replaceMacOs(macIdentity,snapshot.software());
          return " Automatische Inventarisierung: macOS erkannt und erfolgreich über SSH inventarisiert; Quelle SSH · macOS gespeichert.";
        }
        return " macOS wurde per SSH erkannt, die automatische Inventarisierung lieferte jedoch keine Daten.";
      }
      var linuxResult=linux.inspectSingle(host,nameHint==null?"":nameHint,"SSH bestätigt",false);
      if(linuxResult.isPresent()){
        var d=linuxResult.get();
        registrations.recordVerifiedInventoryHit(d.address(),d.hardwareAddress(),d.serialNumber(),d.name(),d.type(),d.manufacturer(),d.protocol(),d.status());
        String linuxIdentity=registrations.resolveIdentityKey(d.address(),d.hardwareAddress(),d.serialNumber(),d.name());
        int softwareCount=0;
        if(linuxIdentity!=null) softwareCount=softwareInventory.replaceLinuxFromDetails(linuxIdentity,linux.consumeLatestSoftwareDetails(host));
        return " Automatische Inventarisierung: Linux erkannt und erfolgreich über SSH inventarisiert; Quelle SSH · Linux gespeichert; "+softwareCount+" Softwareeinträge aktualisiert.";
      }
      return " SSH ist erreichbar und authentifiziert, aber das Ziel konnte nicht als Linux oder macOS bestätigt werden.";
    } catch(Exception e){
      log.warn("[SSH-AUTO-INVENTORY] {} konnte nach erfolgreichem SSH-Test nicht automatisch inventarisiert werden: {}",host,e.getMessage());
      return " SSH ist erreichbar; automatische Inventarisierung fehlgeschlagen: "+(e.getMessage()==null?e.getClass().getSimpleName():e.getMessage());
    }
  }

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
    var raw=adb.status();
    boolean usb=builtinSettings.enabled("ANDROID_USB"), wlan=builtinSettings.enabled("ANDROID_WLAN");
    var devices=raw.devices().stream().filter(d->d.network()?wlan:usb).toList();
    int authorized=(int)devices.stream().filter(d->"device".equals(d.state())).count();
    int unauthorized=(int)devices.stream().filter(d->"unauthorized".equals(d.state())).count();
    int offline=(int)devices.stream().filter(d->"offline".equals(d.state())).count();
    int other=devices.size()-authorized-unauthorized-offline;
    log.info("[DISCOVERY][40k37e52][ANDROID][ROUTES] USB={}, WLAN={}, visibleDevices={}",usb,wlan,devices.size());
    return new AndroidAdbService.AdbStatus(raw.available(),raw.path(),raw.version(),raw.serverActive(),devices,authorized,unauthorized,offline,other,raw.checkedAt());
  }

  /** 40k34b: modernes Wireless-Debugging-Pairing. Der Code verlässt diese Methode nicht in Logs/Fehlern. */
  public AndroidAdbService.ActionResult pairAndroidDevice(String host, int pairingPort, String pairingCode) {
    if(!builtinSettings.enabled("ANDROID_WLAN")) return new AndroidAdbService.ActionResult(false,"Android-WLAN ist in den Erkennungswegen deaktiviert.");
    return adb.pair(host, pairingPort, pairingCode);
  }

  /**
   * 40k34b: Stellt eine ADB-Verbindung her (klassisches TCP/IP oder nach erfolgreichem
   * Pairing). Wird optional ein Identitätsschlüssel übergeben, wird die Verbindungsadresse
   * für die spätere automatische Wiederverbindung an genau dieser Identität gespeichert -
   * es entsteht dabei nie ein neues Gerät.
   */
  public AndroidAdbService.ActionResult connectAndroidDevice(String identityKey, String host, int port) {
    if(!builtinSettings.enabled("ANDROID_WLAN")) return new AndroidAdbService.ActionResult(false,"Android-WLAN ist in den Erkennungswegen deaktiviert.");
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
    if(adbHost!=null && adbPortObj!=null && !builtinSettings.enabled("ANDROID_WLAN"))
      return new LinuxActionResult(false,"Android-WLAN ist in den Erkennungswegen deaktiviert.",detail(identityKey));
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

  public LinuxActionResult runAndroidElevatedInventory(String identityKey) {
    Map<String,Object> row = registrations.find(identityKey);
    String adbHost = str(row.get("adbHost"));
    Object adbPortObj = row.get("adbPort");
    if(adbHost!=null && adbPortObj!=null && !builtinSettings.enabled("ANDROID_WLAN"))
      return new LinuxActionResult(false,"Android-WLAN ist in den Erkennungswegen deaktiviert.",detail(identityKey));
    if (adbHost == null || adbPortObj == null) return new LinuxActionResult(false, "Für dieses Gerät ist noch keine ADB-Verbindung bekannt.", detail(identityKey));
    String serial = adbHost + ":" + ((Number) adbPortObj).intValue();
    var inventory = adb.inventorizeElevated(serial);
    if (!inventory.success()) return new LinuxActionResult(false, "Vertiefte Android-Inventarisierung lieferte keine Daten.", detail(identityKey));
    StringBuilder protocol = new StringBuilder("ADB (WLAN)");
    for (var e : inventory.fields().entrySet()) protocol.append(" · ").append(e.getKey()).append(": ").append(e.getValue());
    protocol.append(" · Inventarisierungsstatus: erfolgreich · Inventarisierungsdauer: ").append(inventory.durationMs() / 1000.0).append(" s");
    registrations.recordDiscoveryHit(str(row.get("address")), str(row.get("hardwareAddress")), inventory.fields().get("Seriennummer"),
      firstNonBlank(inventory.fields().get("Gerätename"), inventory.fields().get("Modell"), str(row.get("name"))),
      str(row.get("deviceType")), firstNonBlank(inventory.fields().get("Hersteller"), str(row.get("manufacturer"))), protocol.toString(), "ONLINE");
    return new LinuxActionResult(true, "Android-Berechtigungen wurden geprüft und die vertiefte Inventarisierung durchgeführt.", detail(identityKey));
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
    if(adbHost!=null && adbPortObj!=null && !builtinSettings.enabled("ANDROID_WLAN"))
      return new AppInventoryRunResult(false,"DEAKTIVIERT","Android-WLAN ist in den Erkennungswegen deaktiviert.",0,0,0,null);
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

  /** 40k37c: iPadOS ist nun eine eigene, lokal inventarisierbare Apple-Mobilplattform. */
  private static final List<String> SUPPORTED_PLATFORMS = List.of("Windows", "Linux", "Android", "macOS", "iOS", "iPadOS");
  private final java.util.concurrent.ConcurrentMap<String,Thread> runningPlatformThreads = new java.util.concurrent.ConcurrentHashMap<>();
  // 40k37e55g-fix2: nur Rueckkanal des bereits verifizierten Apple-Inventars zur
  // bestehenden Plattformlauf-Historie; keine neue Discovery-/Inventarisierungslogik.
  private final ThreadLocal<String> appleInventoryCanonicalIdentity = new ThreadLocal<>();
  private static String platformRunKey(String identityKey,String platform){return identityKey+"||"+platform;}


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
    // 40k37e56a7fix6: Einzelaufrufe bleiben bewusst plattformoffen. Ein Benutzer darf
    // jede Plattform manuell gegen jedes bekannte Gerät testen. Die automatische
    // Auswahl bleibt davon getrennt und nutzt weiterhin nur passende Plattformen.
    // Ein unpassender manueller Lauf muss ehrlich fehlschlagen bzw. ohne Daten enden
    // und darf keine fremden Plattformdaten persistieren.
    // 40k34r: verhindert doppelte, gleichzeitig laufende Inventarisierungen derselben
    // Plattform für dasselbe Gerät - gilt sowohl für die manuelle als auch die
    // automatische Auslösung (keine zweite, separate Prüfung nötig).
    if (platformInventory.isRunning(identityKey, platform)) {
      return new PlatformInventoryResult(false, "LAEUFT", "Für diese Plattform läuft bereits eine Inventarisierung - bitte warten, bis sie abgeschlossen ist.", platform);
    }
    long runId = platformInventory.startRun(identityKey, platform, actor);
    String activeKey=platformRunKey(identityKey,platform);
    runningPlatformThreads.put(activeKey,Thread.currentThread());
    try {
      if(Thread.currentThread().isInterrupted()) throw new InterruptedException("Inventarisierung wurde abgebrochen.");
      PlatformInventoryResult result = switch (platform) {
        case "Android" -> dispatchAndroidPlatformInventory(identityKey);
        case "Linux" -> dispatchLinuxPlatformInventory(identityKey, row);
        case "Windows" -> dispatchWindowsPlatformInventory(identityKey, row);
        case "macOS" -> dispatchMacOsPlatformInventory(identityKey);
        case "iOS", "iPadOS" -> dispatchAppleMobilePlatformInventory(identityKey, platform);
        default -> new PlatformInventoryResult(false, "FEHLGESCHLAGEN", "Unbekannte Plattform: " + platform, platform);
      };
      String currentIdentityKey=("iOS".equals(platform)||"iPadOS".equals(platform))
        ? appleInventoryCanonicalIdentity.get()
        : registrations.resolveIdentityKey(str(row.get("address")),str(row.get("hardwareAddress")),str(row.get("serialNumber")),str(row.get("name")));
      if(currentIdentityKey==null) currentIdentityKey=registrations.resolveIdentityKey(
        str(row.get("address")),str(row.get("hardwareAddress")),str(row.get("serialNumber")),str(row.get("name")));
      if(currentIdentityKey!=null&&!currentIdentityKey.equals(identityKey)){
        platformInventory.moveRunToIdentity(runId,currentIdentityKey);
        log.info("[PLATFORM-INVENTORY] Lauf {} wurde nach verifizierter Inventarisierung von {} auf kanonische Identität {} umgehängt.",
          runId,identityKey,currentIdentityKey);
      }
      if(platformInventory.isCancelled(runId)||Thread.currentThread().isInterrupted()){
        Thread.currentThread().interrupt();
        log.info("[PLATFORM-INVENTORY] {} für {}: Ergebnis verworfen, weil der Lauf zwischenzeitlich abgebrochen wurde.",platform,identityKey);
        return new PlatformInventoryResult(false,"ABGEBROCHEN","Inventarisierung wurde vom Benutzer abgebrochen.",platform);
      }
      platformInventory.finishRun(runId, result.status(), result.message());
      log.info("[PLATFORM-INVENTORY] {} für {} durch {}: {} - {}", platform, identityKey, actor, result.status(), result.message());
      return result;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      platformInventory.finishRun(runId, "ABGEBROCHEN", "Inventarisierung wurde vom Benutzer abgebrochen.");
      log.info("[PLATFORM-INVENTORY] {} für {} wurde abgebrochen.", platform, identityKey);
      return new PlatformInventoryResult(false, "ABGEBROCHEN", "Inventarisierung wurde vom Benutzer abgebrochen.", platform);
    } catch (Exception e) {
      if(Thread.currentThread().isInterrupted()){
        platformInventory.finishRun(runId, "ABGEBROCHEN", "Inventarisierung wurde vom Benutzer abgebrochen.");
        return new PlatformInventoryResult(false, "ABGEBROCHEN", "Inventarisierung wurde vom Benutzer abgebrochen.", platform);
      }
      platformInventory.finishRun(runId, "FEHLGESCHLAGEN", e.getMessage());
      log.warn("[PLATFORM-INVENTORY] {} für {} fehlgeschlagen: {}", platform, identityKey, e.getMessage());
      return new PlatformInventoryResult(false, "FEHLGESCHLAGEN", e.getMessage(), platform);
    } finally {
      appleInventoryCanonicalIdentity.remove();
      runningPlatformThreads.remove(activeKey,Thread.currentThread());
    }
  }

  public Map<String,Object> cancelPlatformInventory(String identityKey,String platform){
    int marked=platformInventory.cancelRunning(identityKey,platform,"Inventarisierung wurde vom Benutzer abgebrochen.");
    if(platform==null||platform.isBlank()){
      runningPlatformThreads.forEach((key,thread)->{if(key.startsWith(identityKey+"||"))thread.interrupt();});
    } else {
      Thread thread=runningPlatformThreads.get(platformRunKey(identityKey,platform)); if(thread!=null)thread.interrupt();
    }
    return Map.of("cancelled",marked>0,"count",marked,"identityKey",identityKey,"platform",platform==null?"":platform);
  }

  public Map<String,Object> cancelAllPlatformInventories(){
    int marked=platformInventory.cancelAllRunning("Inventarisierung wurde vom Benutzer abgebrochen.");
    runningPlatformThreads.values().forEach(Thread::interrupt);
    return Map.of("cancelled",marked>0,"count",marked);
  }


  private PlatformInventoryResult dispatchMacOsPlatformInventory(String identityKey) {
    LinuxActionResult r=runMacOsInventory(identityKey);
    return new PlatformInventoryResult(r.success(),r.success()?"ERFOLGREICH":"FEHLGESCHLAGEN",r.message(),"macOS");
  }

  /** 40k37e2: iOS/iPadOS-Inventarisierung über Apple-Mobile USB/WLAN; keine lokale OS-Erkennung. */
  private PlatformInventoryResult dispatchAppleMobilePlatformInventory(String identityKey, String platform) {
    String source="iPadOS".equals(platform)?"IPADOS_USB_WLAN":"IOS_USB_WLAN";
    if(!builtinSettings.enabled(source)) return new PlatformInventoryResult(false,"DEAKTIVIERT",platform+"-Erkennung über USB/WLAN ist in den Erkennungsquellen deaktiviert.",platform);
    AppleMobileDiscoveryService.InventoryResult r=appleMobile.inventory(identityKey);
    if(r.identityKey()!=null&&!r.identityKey().isBlank()) appleInventoryCanonicalIdentity.set(r.identityKey());
    int softwareCount=0;
    if(r.success()) softwareCount=softwareInventory.replaceAppleMobile(identityKey,platform,appleMobile.consumeLatestSoftwareInventory(identityKey));
    String message=r.message()+(r.success()?" "+softwareCount+" strukturierte Softwareeinträge gespeichert.":"");
    return new PlatformInventoryResult(r.success(),r.state(),message,platform);
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

  private PlatformInventoryResult dispatchLinuxPlatformInventory(String identityKey, Map<String,Object> row) {
    String address = str(row.get("address"));
    String hardwareAddress=str(row.get("hardwareAddress"));
    String name=str(row.get("name"));

    // 40k36t7: Der lokale GAM-Host hat Vorrang vor SSH. SSH ist ausschließlich
    // der Transportweg für entfernte Linux-Systeme.
    boolean localEnabled=builtinSettings.enabled("LINUX_INVENTORY");
    boolean sshEnabled=builtinSettings.enabled("LINUX_NETWORK");

    if(linuxLocal.isLocalTarget(address,hardwareAddress,name) && localEnabled){
      log.info("[PLATFORM-INVENTORY] Linux {} wird über Backend-lokal inventarisiert; Quelle LINUX_INVENTORY ist aktiviert.",identityKey);
      final DiscoveredDevice[] found={null};
      linuxLocal.scan(true,OffsetDateTime.now().toString(),dev->found[0]=dev,
        event->{}, "platform-local-"+System.nanoTime(), 0);
      if(found[0]==null)
        return new PlatformInventoryResult(false,"ERKENNUNG_FEHLGESCHLAGEN",
          "Lokale Linux-Inventarisierung wurde gestartet, lieferte aber keine verwertbaren Daten.","Linux");

      DiscoveredDevice dev=found[0];
      long persistStarted=System.currentTimeMillis();
      registrations.recordVerifiedInventoryHit(dev.address(),dev.hardwareAddress(),dev.serialNumber(),
        dev.name(),dev.type(),dev.manufacturer(),dev.protocol(),dev.status());
      String resolved=registrations.resolveIdentityKey(dev.address(),dev.hardwareAddress(),dev.serialNumber(),dev.name());
      long identityPersistMs=System.currentTimeMillis()-persistStarted;
      long softwareStarted=System.currentTimeMillis();
      int softwareCount=0;
      String cacheKey=dev.address()!=null?dev.address():dev.name();
      if(resolved!=null) softwareCount=softwareInventory.replaceLinuxFromDetails(resolved,linuxLocal.consumeLatestSoftwareDetails(cacheKey));
      long softwarePersistMs=System.currentTimeMillis()-softwareStarted;
      log.info("[LINUX-LOCAL-TIMING] {} Persistierung: Identität={} ms, Software={} ms ({} Einträge)",
        cacheKey,identityPersistMs,softwarePersistMs,softwareCount);
      return new PlatformInventoryResult(true,"ERFOLGREICH",
        "Linux-Inventarisierung über Backend-lokal erfolgreich; "+softwareCount+
        " strukturierte Softwareeinträge aktualisiert. Persistierung: Identität "+
        identityPersistMs+" ms, Software "+softwarePersistMs+" ms.","Linux");
    }

    // 40k36t8: Ist Backend-lokal deaktiviert, darf der lokale Host absichtlich
    // in den SSH-Pfad fallen. Ist auch die SSH-/Linux-Netzwerkquelle deaktiviert,
    // darf überhaupt kein Linux-Transportweg gestartet werden.
    if(!sshEnabled){
      String reason=localEnabled
        ?"SSH/Linux-Netzwerk-Inventarisierung ist deaktiviert."
        :"Backend-lokal und SSH/Linux-Netzwerk-Inventarisierung sind deaktiviert.";
      log.info("[PLATFORM-INVENTORY] Linux {} übersprungen: {}",identityKey,reason);
      return new PlatformInventoryResult(false,"DEAKTIVIERT",reason,"Linux");
    }

    if (address == null)
      return new PlatformInventoryResult(false, "NICHT_ERREICHBAR",
        "Für die Linux-SSH-Inventarisierung ist keine IP-Adresse bekannt.", "Linux");

    // Remote oder bewusst lokal-über-SSH: bestehender SSH-Pfad.
    if(!linux.hasConfiguredSshAccess(address)){
      return new PlatformInventoryResult(false,"NICHT_EINGERICHTET",
        "SSH-Inventarisierung ist aktiviert, aber für dieses Ziel ist noch kein SSH-Zugang konfiguriert.","Linux");
    }
    log.info("[PLATFORM-INVENTORY] Linux {} wird über SSH inventarisiert (Backend-lokal aktiviert: {}, SSH-Quelle aktiviert: {}).",
      identityKey,localEnabled,sshEnabled);
    linux.invalidateCache(address);
    var discovered = linux.inspectSingle(address, name, null, false);
    if (discovered.isEmpty()) {
      var ssh=linux.testSshConnection(address);
      if (ssh.reachable())
        return new PlatformInventoryResult(false, "ERKENNUNG_FEHLGESCHLAGEN",
          "SSH ist erreichbar und authentifiziert, aber das Ziel konnte nicht als Linux-System bestätigt werden.", "Linux");
      return new PlatformInventoryResult(false, "NICHT_ERREICHBAR",
        "SSH-Verbindung oder Anmeldung fehlgeschlagen: " + ssh.message(), "Linux");
    }
    var dev = discovered.get();
    long persistStarted=System.currentTimeMillis();
    registrations.recordVerifiedInventoryHit(dev.address(), dev.hardwareAddress(), dev.serialNumber(),
      dev.name(), dev.type(), dev.manufacturer(), dev.protocol(), dev.status());
    String resolved=registrations.resolveIdentityKey(dev.address(),dev.hardwareAddress(),dev.serialNumber(),dev.name());
    long identityPersistMs=System.currentTimeMillis()-persistStarted;
    int softwareCount=0;
    long softwareStarted=System.currentTimeMillis();
    if(resolved!=null) softwareCount=softwareInventory.replaceLinuxFromDetails(resolved,linux.consumeLatestSoftwareDetails(address));
    long softwarePersistMs=System.currentTimeMillis()-softwareStarted;
    log.info("[LINUX-TIMING] {} Persistierung: Identität={} ms, Software={} ms ({} Einträge)",
      address,identityPersistMs,softwarePersistMs,softwareCount);
    return new PlatformInventoryResult(true, "ERFOLGREICH",
      "Linux-Inventarisierung über SSH erfolgreich; "+softwareCount+" strukturierte Softwareeinträge aktualisiert. "+
      "Persistierung: Identität "+identityPersistMs+" ms, Software "+softwarePersistMs+" ms.", "Linux");
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
  /** 40k36t51: Physische lokale Windows-Identitaet unabhaengig von Discovery-Herkunft erkennen. */
  /** 40k36t56: Physische lokale Windows-Identitaet robust erkennen.
   * Hostname, lokale IP-Adressen und lokale MAC-Adressen werden gemeinsam geprüft.
   * Ein Treffer bedeutet: Backend-lokal hat immer Vorrang vor WinRM. */
  private boolean isCurrentWindowsHost(String identityKey, Map<String,Object> row){
    if(!System.getProperty("os.name","").toLowerCase(Locale.ROOT).contains("win")) return false;
    if(row==null || row.isEmpty()) return false;

    String rowName=str(row.get("name"));
    String rowAddress=str(row.get("address"));
    String rowMac=normalizeWindowsMac(str(row.get("hardwareAddress")));
    String identityMac=null;
    if(identityKey!=null && identityKey.regionMatches(true,0,"mac:",0,4))
      identityMac=normalizeWindowsMac(identityKey.substring(4));

    java.util.Set<String> localNames=new java.util.LinkedHashSet<>();
    java.util.Set<String> localAddresses=new java.util.LinkedHashSet<>();
    java.util.Set<String> localMacs=new java.util.LinkedHashSet<>();

    try{
      java.net.InetAddress localhost=java.net.InetAddress.getLocalHost();
      if(localhost.getHostName()!=null&&!localhost.getHostName().isBlank())
        localNames.add(localhost.getHostName().trim().toLowerCase(Locale.ROOT));
      if(localhost.getCanonicalHostName()!=null&&!localhost.getCanonicalHostName().isBlank())
        localNames.add(localhost.getCanonicalHostName().trim().toLowerCase(Locale.ROOT));
    }catch(Exception ignored){}

    String computerName=System.getenv("COMPUTERNAME");
    if(computerName!=null&&!computerName.isBlank())
      localNames.add(computerName.trim().toLowerCase(Locale.ROOT));

    try{
      java.util.Enumeration<java.net.NetworkInterface> interfaces=java.net.NetworkInterface.getNetworkInterfaces();
      while(interfaces!=null&&interfaces.hasMoreElements()){
        java.net.NetworkInterface nic=interfaces.nextElement();

        try{
          byte[] hw=nic.getHardwareAddress();
          if(hw!=null&&hw.length>0){
            StringBuilder b=new StringBuilder();
            for(byte v:hw) b.append(String.format("%02X",v));
            localMacs.add(b.toString());
          }
        }catch(Exception ignored){}

        java.util.Enumeration<java.net.InetAddress> addresses=nic.getInetAddresses();
        while(addresses!=null&&addresses.hasMoreElements()){
          java.net.InetAddress address=addresses.nextElement();
          if(address.isLoopbackAddress()) continue;
          String value=address.getHostAddress();
          if(value==null||value.isBlank()) continue;
          int zone=value.indexOf('%');
          if(zone>0) value=value.substring(0,zone);
          localAddresses.add(value.trim().toLowerCase(Locale.ROOT));
        }
      }
    }catch(Exception ignored){}

    if(rowName!=null&&!rowName.isBlank()){
      String candidate=rowName.trim().toLowerCase(Locale.ROOT);
      if(localNames.contains(candidate)) return true;

      String candidateShort=windowsShortHost(candidate);
      for(String localName:localNames)
        if(candidateShort.equals(windowsShortHost(localName))) return true;
    }

    if(rowAddress!=null&&!rowAddress.isBlank()){
      String candidate=rowAddress.trim().toLowerCase(Locale.ROOT);
      int zone=candidate.indexOf('%');
      if(zone>0) candidate=candidate.substring(0,zone);
      if(localAddresses.contains(candidate)) return true;
    }

    if(rowMac!=null&&!rowMac.isBlank()&&localMacs.contains(rowMac)) return true;
    // 40k36t116: Nach Identitaets-Merges kann hardwareAddress im aktuellen Row fehlen
    // oder nicht mehr die lokale NIC repraesentieren, waehrend der stabile Identity-Key
    // bereits als mac:<adresse> vorliegt. Dieser Key ist deshalb gleichwertige lokale
    // Evidenz und verhindert, dass WinRM den aktivierten Backend-lokal-Pfad verdraengt.
    if(identityMac!=null&&!identityMac.isBlank()&&localMacs.contains(identityMac)) return true;

    log.debug("[WINDOWS-LOCAL-MATCH] Kein Backend-lokal-Treffer: identity={} name={} address={} rowMac={} identityMac={} localNames={} localAddresses={} localMacs={}",
      identityKey,rowName,rowAddress,rowMac,identityMac,localNames,localAddresses,localMacs);
    return false;
  }

  private static String windowsShortHost(String value){
    if(value==null) return "";
    String v=value.trim().toLowerCase(Locale.ROOT);
    int dot=v.indexOf('.');
    return dot>0?v.substring(0,dot):v;
  }

  private static String normalizeWindowsMac(String value){
    if(value==null) return null;
    return value.replaceAll("[^0-9A-Fa-f]","").toUpperCase(Locale.ROOT);
  }


  private PlatformInventoryResult dispatchWindowsPlatformInventory(String identityKey, Map<String,Object> row) {
    String address = str(row.get("address"));
    String protocol = str(row.get("protocol"));
    boolean localEnabled = builtinSettings.enabled("WINDOWS_INVENTORY");
    boolean localHost = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    boolean localEvidence = identityKey.startsWith("windows-local:")
      || (protocol != null && protocol.contains("Windows-Inventarisierung / modulare PowerShell-Abfragen"))
      || (localHost && isCurrentWindowsHost(identityKey,row));

    // 40k36t25: Wie bei Linux/macOS darf sich die Fernvariante nicht vor einen
    // aktivierten lokalen Inventarisierer drängen. Zusammengeführte Identitäten
    // verlieren ggf. das windows-local:-Präfix; deshalb zählt auch die bereits
    // gespeicherte lokale Windows-Quelle als lokale Evidenz.
    if (localEnabled && localHost && localEvidence) {
      log.info("[PLATFORM-INVENTORY] Windows {} ist der aktuelle Backend-Host: Backend-lokal hat Vorrang; WinRM wird trotz vorhandener Konfiguration nicht verwendet.", identityKey);
      WindowsRun run=beginWindowsRun(identityKey,1,"Standard");
      if(run.priority()>1)return new PlatformInventoryResult(false,"HÖHERWERTIGER_LAUF_AKTIV","Ein höherwertiger Windows-Inventarlauf ("+run.mode()+") ist bereits aktiv; der Standardlauf wurde nicht gestartet.","Windows");
      boolean[] found={false};boolean[] persisted={false};int[] softwareCount={0};
      try{
        windowsLocal.scan(true,OffsetDateTime.now().toString(),
          d->{found[0]=true;if(!ownsWindowsRun(identityKey,run)){log.info("[WINDOWS-RUN] {}: überholtes Standard-Ergebnis wird verworfen.",identityKey);return;}
            registrations.recordVerifiedInventoryHit(d.address(),d.hardwareAddress(),d.serialNumber(),d.name(),d.type(),d.manufacturer(),d.protocol(),d.status());persisted[0]=true;
            String resolved=registrations.resolveIdentityKey(d.address(),d.hardwareAddress(),d.serialNumber(),d.name());if(resolved!=null)softwareCount[0]=softwareInventory.replaceWindowsFromDetails(resolved,windowsLocal.consumeLatestSoftwareDetails());},
          event->{}, "manual-"+identityKey,0);
        if(found[0]&&!persisted[0])return new PlatformInventoryResult(false,"ÜBERHOLT","Der Standardlauf wurde durch einen höherwertigen/neuen Windows-Inventarlauf ersetzt; sein Ergebnis wurde verworfen.","Windows");
        return found[0]?new PlatformInventoryResult(true,"ERFOLGREICH","Lokale Windows-Inventarisierung erfolgreich; "+softwareCount[0]+" strukturierte Softwareeinträge aktualisiert.","Windows"):new PlatformInventoryResult(false,"FEHLGESCHLAGEN","Lokale Windows-Inventarisierung lieferte kein Ergebnis (läuft GAM auf einem Windows-Host?).","Windows");
      }finally{finishWindowsRun(identityKey,run);}
    }

    if (address == null) return new PlatformInventoryResult(false, "NICHT_ERREICHBAR", "Für dieses Gerät ist keine IP-Adresse bekannt.", "Windows");
    String windowsName=str(row.get("name"));
    if (!windowsRemote.hasConfiguredAccess(address,windowsName,identityKey)) {
      return new PlatformInventoryResult(false, "NICHT_EINGERICHTET",
        "WinRM ist für dieses Windows-Gerät nicht konfiguriert bzw. keinem passenden Geräte-/Hostprofil zugeordnet. Eine Ferninventarisierung wird deshalb nicht gestartet.", "Windows");
    }
    var discovered = windowsRemote.inspectSingle(address,windowsName,identityKey);
    if (discovered.isEmpty()) return new PlatformInventoryResult(false, "NICHT_ERREICHBAR",
      "Windows-Gerät war über den konfigurierten WinRM-Zugang nicht erreichbar oder die Anmeldung schlug fehl.", "Windows");
    var d = discovered.get();
    registrations.recordVerifiedInventoryHit(d.address(), d.hardwareAddress(), d.serialNumber(), d.name(), d.type(), d.manufacturer(), d.protocol(), d.status());
    String windowsIdentity=registrations.resolveIdentityKey(d.address(),d.hardwareAddress(),d.serialNumber(),d.name());
    int windowsSoftwareCount=0;
    if(windowsIdentity!=null) windowsSoftwareCount=softwareInventory.replaceWindowsFromDetails(windowsIdentity,windowsRemote.consumeLatestSoftwareDetails(address));
    return new PlatformInventoryResult(true, "ERFOLGREICH",
      "Windows-Ferninventarisierung (WinRM) erfolgreich; "+windowsSoftwareCount+" strukturierte Softwareeinträge aktualisiert.", "Windows");
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
    String platform = preferredPlatform(row, deviceType, protocol);

    // 40k37e56a7fix4: Eine bereits eindeutig klassifizierte Apple-Mobile-Plattform
    // ist exklusiv. Eine bekannte IP oder ein zufällig für dieselbe Adresse
    // konfiguriertes SSH-/WinRM-Profil darf iPhone/iPad nicht zusätzlich durch
    // Linux, Windows, Android oder macOS routen. Apple-Erkennung und -Inventar
    // selbst bleiben unverändert; hier wird ausschließlich der gemeinsame
    // Plattform-Dispatcher begrenzt.
    if ("iOS".equals(platform) || "iPadOS".equals(platform)) {
      log.debug("[AUTO-INVENTORY] Apple-Mobile-Plattform {} eindeutig für {} -> exklusives Plattform-Routing", platform, row.get("identityKey"));
      return List.of(platform);
    }
    String protocolLower = protocol == null ? "" : protocol.toLowerCase(Locale.ROOT);
    String identityKey=str(row.get("identityKey"));
    String address=str(row.get("address"));
    String deviceTypeLower=deviceType==null?"":deviceType.toLowerCase(Locale.ROOT);

    // 40k37t11: Android folgt jetzt derselben konservativen Evidenzlogik.
    // Ein bloßes platformOf=Android bzw. ein allgemeiner Mobilgeräte-Hinweis
    // darf keine automatische ADB-Vollinventarisierung auslösen.
    boolean androidStrongEvidence=(identityKey!=null&&(
        identityKey.startsWith("android-adb:")
        || identityKey.startsWith("adb:")
      ))
      || protocolLower.contains("android bestätigt")
      || protocolLower.contains("adb-inventar")
      || protocolLower.contains("adb · android")
      || protocolLower.contains("ro.build.version.release")
      || protocolLower.contains("ro.product.model")
      || protocolLower.contains("getprop")
      || protocolLower.contains("android sdk")
      || deviceTypeLower.contains("android");
    boolean adbConfigured=row.get("adbHost")!=null && !str(row.get("adbHost")).isBlank();
    boolean weakAndroidOnly="Android".equals(platform)&&!androidStrongEvidence&&!adbConfigured;
    boolean androidLikely="Android".equals(platform)&&(androidStrongEvidence||adbConfigured);

    // 40k36t9: Linux wie Windows konservativ behandeln. Ein schwacher Linux-Hinweis
    // (z.B. generische Netzwerkdienste, SNMP-Text, Hostname oder bloßes platformOf=Linux)
    // darf keine automatische Vollinventarisierung auslösen.
    boolean linuxLocalEvidence=(identityKey!=null&&identityKey.startsWith("linux-local:"))
      || protocolLower.contains("linux bestätigt: ja")
      || protocolLower.contains("distributions-id:")
      || protocolLower.contains("ssh-inventar")
      || protocolLower.contains("backend-lokal · linux")
      || protocolLower.contains("linux backend-lokal")
      || protocolLower.contains("/etc/os-release")
      || protocolLower.contains("kernel: linux")
      || deviceTypeLower.contains("computer / linux")
      || deviceTypeLower.contains("server / linux-system")
      || deviceTypeLower.contains("linux-system");
    boolean sshConfigured=address!=null&&linux.hasConfiguredSshAccess(address);
    boolean weakLinuxOnly="Linux".equals(platform)&&!linuxLocalEvidence&&!sshConfigured;
    boolean linuxLikely="Linux".equals(platform)&&(linuxLocalEvidence||sshConfigured);
    boolean windowsLocalEvidence=(identityKey!=null&&identityKey.startsWith("windows-local:"))
      || protocolLower.contains("windows-inventarisierung")
      || protocolLower.contains("windows lokal")
      || protocolLower.contains("wmi")
      || protocolLower.contains("powershell")
      || protocolLower.contains("winrm")
      || deviceTypeLower.contains("computer / windows")
      || deviceTypeLower.contains("windows-system");
    boolean winRmConfigured=address!=null&&windowsRemote.hasConfiguredAccess(address);
    boolean weakWindowsOnly="Windows".equals(platform)&&!windowsLocalEvidence&&!winRmConfigured;
    boolean windowsLikely="Windows".equals(platform)&&(windowsLocalEvidence||winRmConfigured);

    // 40k36t10: macOS folgt derselben konservativen Evidenzlogik wie Windows/Linux.
    boolean macLocalEvidence=(identityKey!=null&&identityKey.startsWith("macos-local:"))
      || protocolLower.contains("macos bestätigt: ja")
      || protocolLower.contains("ssh · macos")
      || protocolLower.contains("macos backend-lokal")
      || protocolLower.contains("backend-lokal · macos")
      || protocolLower.contains("darwin")
      || protocolLower.contains("sw_vers")
      || protocolLower.contains("produkt: macos")
      || deviceTypeLower.contains("computer / macos")
      || deviceTypeLower.contains("notebook / macos")
      || deviceTypeLower.contains("mini-pc / macos")
      || deviceTypeLower.contains("workstation / macos");
    boolean macSshConfigured=address!=null&&macOs.hasConfiguredSshAccess(address);
    boolean weakMacOnly="macOS".equals(platform)&&!macLocalEvidence&&!macSshConfigured;
    boolean macLikely="macOS".equals(platform)&&(macLocalEvidence||macSshConfigured);

    // 40k37c: Apple-Mobile nur bei eindeutiger iOS/iPadOS-Klassifikation.
    // Trust/Pairing und tatsächliche Erreichbarkeit werden im Apple-Dispatcher geprüft.
    boolean appleMobileLikely="iOS".equals(platform)||"iPadOS".equals(platform);

    log.debug("[AUTO-INVENTORY] Plattformermittlung für deviceType='{}': platformOf()='{}' -> Android={} (starke Evidenz={}, ADB konfiguriert={}, schwacher Android-Hinweis allein={}), Linux={} (starke Evidenz={}, SSH konfiguriert={}, schwacher Linux-Hinweis allein={}), Windows={} (starke Evidenz={}, WinRM konfiguriert={}, schwacher Windows-Hinweis allein={}), macOS={} (starke Evidenz={}, SSH konfiguriert={}, schwacher macOS-Hinweis allein={})",
      deviceType, platform,
      androidLikely,androidStrongEvidence,adbConfigured,weakAndroidOnly,
      linuxLikely,linuxLocalEvidence,sshConfigured,weakLinuxOnly,
      windowsLikely,windowsLocalEvidence,winRmConfigured,weakWindowsOnly,
      macLikely,macLocalEvidence,macSshConfigured,weakMacOnly);
    List<String> platforms = new ArrayList<>();
    if (androidLikely) platforms.add("Android");
    if (linuxLikely) platforms.add("Linux");
    if (windowsLikely) platforms.add("Windows");
    if (macLikely) platforms.add("macOS");
    if (appleMobileLikely) platforms.add(platform);
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
  public Map<String,Object> globalPlatformInventoryStatus(){ return platformInventory.globalStatus(); }

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
   * entfernt bei einer alias-basierten Auswahl den betreffenden Alias und bei einem
   * Merge-Schnappschuss die zugehörige IP-/MAC-Netzwerkidentität vom Ursprungsgerät.
   * Damit entstehen wieder zwei sauber getrennte Geräteidentitäten, ohne die übrigen
   * Informationen des Ursprungsgeräts zu verlieren.
   */
  public Map<String,Object> split(String identityKey, String ref, String actor) {
    registrations.find(identityKey); // wirft, falls Identität nicht existiert
    String name, deviceType = null, address = null, mac = null, serial = null, manufacturer = null, discoveryProtocol = null;
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
      // 40k37e54-fix2: Beim Wiederauftrennen eines Merge-Snapshots die echte
      // Quellenprovenienz des abgespaltenen Datensatzes wiederherstellen. Sonst wurde
      // der neue Datensatz nur als "Wiederauftrennung (manuell)" registriert und ein
      // spaeter erneut erkannter FRITZ!Box-/ARP-Treffer lediglich als pending Quelle
      // behandelt bzw. wirkte im Frontend nicht mehr wie der urspruengliche Treffer.
      discoveryProtocol = str(e.get("discoveryProtocol"));
      if (discoveryProtocol == null) discoveryProtocol = str(e.get("protocol"));
    } else {
      throw new IllegalArgumentException("Unbekannte Auswahl.");
    }
    if (name == null || name.isBlank()) throw new IllegalArgumentException("Für die Wiederauftrennung wird mindestens ein Name benötigt.");

    String restoredProtocol = discoveryProtocol == null
      ? "Wiederauftrennung (manuell)"
      : discoveryProtocol;
    registrations.register(address, mac, serial, name, deviceType, manufacturer, restoredProtocol, "UNBEKANNT");
    String newKey = registrations.resolveIdentityKey(address, mac, serial, name);
    if (newKey == null) newKey = DiscoveryRegistrationRepository.resolveKey(address, mac, serial, name);
    if (ref.startsWith("ALIAS:")) registrations.removeAlias(identityKey, name);
    if (ref.startsWith("SNAPSHOT:")) registrations.removeNetworkIdentity(identityKey, address, mac);
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

  public Map<String,Object> removeDiscoverySource(String identityKey, String source) {
    return registrations.removeDiscoverySource(identityKey, source);
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
      truthy(row.get("manualDeviceType")), categoryParts[0], categoryParts[1], preferredPlatform(row, deviceType, protocol),
      str(row.get("address")), str(row.get("hardwareAddress")), str(row.get("serialNumber")),
      str(row.get("manufacturer")), str(row.get("status")), row.get("firstSeenAt"), row.get("lastSeenAt"),
      number(row.get("detectionCount")), (int) number(row.get("lastScanHits")), sources, sources.size(),
      aliases, aliases.size(), mergeCount, registrations.networkAddressesFor(identityKey),
      registrations.networkHardwareAddressesFor(identityKey), confidence.label(), confidence.reasons(),
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
  private static String preferredPlatform(Map<String,Object> row, String deviceType, String protocol) {
    // fix42: Ein verifizierter Transport ist verlaesslicher als eine historisch
    // falsch gespeicherte Kategorie oder eine manuelle Plattform-Vermutung.
    String verified = verifiedPlatform(protocol);
    if (verified != null) return verified;
    String preferred=str(row.get("preferredPlatform"));
    if (preferred != null) return preferred;
    String name=nullToEmpty(str(row.get("name"))).toLowerCase(Locale.ROOT);
    // Alte Netzwerkkategorien wie 'Tablet / iPadOS' nicht als Apple-Beleg
    // uebernehmen, wenn schon der Name deutlich auf Android verweist.
    if ((name.startsWith("android-") || name.contains("galaxy-") || name.contains("samsung")
        || name.contains("doogee") || name.contains("fossibot"))
        && !nullToEmpty(protocol).toLowerCase(Locale.ROOT).contains("apple mobile")) return "Android";
    if (deviceType != null && (deviceType.toLowerCase(Locale.ROOT).contains("ipados")
        || deviceType.toLowerCase(Locale.ROOT).contains("ios"))
        && protocol != null && (protocol.toLowerCase(Locale.ROOT).contains("fritz")
        || protocol.toLowerCase(Locale.ROOT).contains("arp"))) return null;
    return platformOf(deviceType,protocol);
  }

  /** Ausschliesslich positive technische Plattformnachweise, keine Hostnamen. */
  public static String verifiedPlatform(String protocol) {
    String p = nullToEmpty(protocol).toLowerCase(Locale.ROOT);
    if (p.contains("adb") || p.contains("android usb") || p.contains("android wlan")) return "Android";
    if (p.contains("apple mobile") || p.contains("coredevice") || p.contains("libimobiledevice")
        || p.contains("usbmux") || p.contains("lockdown") || p.contains("userspace_rsd")) {
      if (p.contains("ipados") || p.contains("ipad")) return "iPadOS";
      if (p.contains("iphone") || p.contains("ios")) return "iOS";
      return null; // Apple erkannt, Geraeteklasse noch unbekannt
    }
    if (p.contains("winrm") || p.contains("windows wmi") || p.contains("windows lokal")) return "Windows";
    if (p.contains("macos lokal") || p.contains("macos ssh")) return "macOS";
    if (p.contains("linux lokal") || p.contains("linux ssh")) return "Linux";
    return null;
  }

  public static String platformOf(String deviceType, String protocol) {
    String verified=verifiedPlatform(protocol);
    if (verified!=null) return verified;
    String type=nullToEmpty(deviceType).toLowerCase(Locale.ROOT);
    String source=nullToEmpty(protocol).toLowerCase(Locale.ROOT);
    // Eine allgemeine Netzwerkquelle belegt keine iPadOS-Plattform.
    // Gleiches gilt fuer die Kategorie 'Smartphones & Tablets'.
    if (type.contains("android") || type.contains("chromecast") || type.contains("fire tv") || type.contains("shield")) return "Android";
    if (type.contains("ipados") || type.contains("ipad")) return "iPadOS";
    if (type.contains("iphone") || type.contains("ios")) return "iOS";
    if (type.contains("windows") || type.contains("wmi") || source.contains("winrm")) return "Windows";
    if (type.contains("macos") || type.contains("mac os")) return "macOS";
    if (type.contains("linux")) return "Linux";
    if (type.contains("home assistant")) return "Home Assistant";
    if (type.contains("proxmox")) return "Proxmox";
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
