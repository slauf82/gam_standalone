package de.kopfzentrum.gam.inventory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Schritt 40k32b12: konsistente Softwarezählung mit Roh-/Eindeutig-Zählung und Quellenanalyse,
 * individuellen Zeitlimits und fehlertoleranter Software-/Update-Erkennung.
 * Jede Informationsgruppe läuft in einem eigenen PowerShell-Prozess mit
 * eigenem Timeout. Fehler einer Gruppe verhindern keine Teilergebnisse.
 */
@Service
public class WindowsInventoryDiscoveryService {
  private static final Logger log=LoggerFactory.getLogger(WindowsInventoryDiscoveryService.class);
  // 40k36t51: strukturierte Software des letzten backend-lokalen Windows-Laufs.
  // Es existiert pro GAM-Backend nur ein lokaler Windows-Host; der Wert wird nach
  // erfolgreicher Persistenz konsumiert und nicht als Zugangsdaten-/Geheimnis gespeichert.
  private volatile String latestSoftwareDetails;

  public record LocalWindowsTestResult(boolean success, String message, String checkedAt, String mechanism, String userContext, long durationMs) {}

  public LocalWindowsTestResult testLocalConnection(){
    long started=System.nanoTime();
    if(!isWindows()){
      return new LocalWindowsTestResult(false, "GAM wird nicht unter Windows ausgeführt.", OffsetDateTime.now().toString(), "nicht verfügbar", "", TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-started));
    }
    String powershell = commandExists("powershell.exe") ? "powershell.exe" : commandExists("pwsh.exe") ? "pwsh.exe" : null;
    if(powershell==null){
      return new LocalWindowsTestResult(false, "PowerShell wurde nicht gefunden.", OffsetDateTime.now().toString(), "nicht verfügbar", "", TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-started));
    }
    String mechanism = "Windows PowerShell / Current Context (SSPI-äquivalenter Fastpath)";
    try{
      Process process=new ProcessBuilder(powershell,"-NoProfile","-NonInteractive","-ExecutionPolicy","Bypass","-Command",
        "$ErrorActionPreference='Stop'; [Console]::WriteLine((whoami)); [Console]::WriteLine($PSVersionTable.PSVersion.ToString())")
        .redirectErrorStream(true).start();
      boolean finished=process.waitFor(15,TimeUnit.SECONDS);
      if(!finished){ process.destroyForcibly(); throw new IOException("lokaler PowerShell-Test nach 15 s abgebrochen"); }
      String output=new String(process.getInputStream().readAllBytes(),StandardCharsets.UTF_8).trim();
      String[] lines=output.split("\\R");
      String user=lines.length>0?lines[0].trim():"";
      boolean success=process.exitValue()==0 && !user.isBlank();
      String message=success
        ? "Lokaler Windows-Zugriff erfolgreich. Bevorzugter Mechanismus: "+mechanism+(user.isBlank()?"":" · Kontext: "+user)+"."
        : "Lokaler Windows-Zugriff über "+mechanism+" fehlgeschlagen"+(output.isBlank()?".":": "+output.replaceAll("\\s+"," ").trim());
      return new LocalWindowsTestResult(success,message,OffsetDateTime.now().toString(),mechanism,user,TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-started));
    }catch(Exception e){
      return new LocalWindowsTestResult(false,"Lokaler Windows-Zugriff über "+mechanism+" fehlgeschlagen: "+safeMessage(e),OffsetDateTime.now().toString(),mechanism,"",TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-started));
    }
  }

  public String consumeLatestSoftwareDetails(){
    String value=latestSoftwareDetails;
    latestSoftwareDetails=null;
    return value;
  }

  private static final long FAST_TIMEOUT_SECONDS = 8;
  private static final long SOFTWARE_TIMEOUT_SECONDS = 30;
  private static final long SECURITY_TIMEOUT_SECONDS = 20;
  private static final long UPDATE_TIMEOUT_SECONDS = 60;

  public void scan(
      boolean enabled,
      String now,
      Consumer<DiscoveredDevice> out,
      Consumer<DeviceDiscoveryService.DiscoveryDiagnosticEvent> diagnostics,
      String sessionId,
      int currentCount) {
    scanInternal(enabled,now,out,diagnostics,sessionId,currentCount,false,false);
  }

  public void scanElevatedUac(boolean enabled,String now,Consumer<DiscoveredDevice> out,Consumer<DeviceDiscoveryService.DiscoveryDiagnosticEvent> diagnostics,String sessionId,int currentCount){
    scanInternal(enabled,now,out,diagnostics,sessionId,currentCount,true,false);
  }

  public void scanSystemTask(boolean enabled,String now,Consumer<DiscoveredDevice> out,Consumer<DeviceDiscoveryService.DiscoveryDiagnosticEvent> diagnostics,String sessionId,int currentCount){
    scanInternal(enabled,now,out,diagnostics,sessionId,currentCount,false,true);
  }

  private void scanInternal(boolean enabled,String now,Consumer<DiscoveredDevice> out,Consumer<DeviceDiscoveryService.DiscoveryDiagnosticEvent> diagnostics,String sessionId,int currentCount,boolean elevatedUac,boolean systemTask) {
    if (!enabled) {
      diagnostic(diagnostics, sessionId, "WINDOWS_INVENTORY", "SKIPPED", "Windows-Inventarisierung ist deaktiviert", currentCount);
      return;
    }
    if (!isWindows()) {
      diagnostic(diagnostics, sessionId, "WINDOWS_INVENTORY", "SKIPPED", "GAM wird nicht unter Windows ausgeführt", currentCount);
      return;
    }

    String powershell = commandExists("powershell.exe") ? "powershell.exe" : commandExists("pwsh.exe") ? "pwsh.exe" : null;
    if (powershell == null) {
      diagnostic(diagnostics, sessionId, "WINDOWS_INVENTORY", "FAILED", "PowerShell wurde nicht gefunden", currentCount);
      return;
    }

    diagnostic(diagnostics, sessionId, "WINDOWS_INVENTORY", "RUNNING", "Modulare Windows-Inventarisierung wird gestartet", currentCount);
    Map<String, String> data = new LinkedHashMap<>();
    List<String> successfulModules = new ArrayList<>();
    List<String> failedModules = new ArrayList<>();

    if(systemTask){
      diagnostic(diagnostics, sessionId, "WINDOWS_INVENTORY_SYSTEM", "RUNNING",
        "Temporäre SYSTEM-Aufgabe für die vollständige Windows-Inventarisierung wird angefordert", currentCount);
      try{
        Map<String,String> elevated = runKeyValueSystemTask(powershell,elevatedFullInventoryScript(),300L);
        data.putAll(elevated);
        markElevatedModuleResults(data,successfulModules,failedModules,diagnostics,sessionId,currentCount);
        log.info("[WINDOWS-SYSTEM] Vollständigkeitsprüfung: Module erfolgreich={} fehlgeschlagen={} Felder={}",
          successfulModules,failedModules,data.size());
        diagnostic(diagnostics, sessionId, "WINDOWS_INVENTORY_SYSTEM", "COMPLETED",
          "Windows-Inventarisierung wurde einmalig unter NT AUTHORITY\\SYSTEM ausgeführt", currentCount);
      }catch(Exception exception){
        diagnostic(diagnostics, sessionId, "WINDOWS_INVENTORY_SYSTEM", "FAILED",
          "SYSTEM-Gesamtinventarlauf fehlgeschlagen: "+safeMessage(exception), currentCount);
      }
    }else if(elevatedUac){
      diagnostic(diagnostics, sessionId, "WINDOWS_INVENTORY_UAC", "RUNNING",
        "Einmalige UAC-Freigabe für die vollständige Windows-Inventarisierung wird angefordert", currentCount);
      try{
        Map<String,String> elevated = runKeyValueModule(
          powershell,
          elevatedFullInventoryScript(),
          300L,
          true);
        data.putAll(elevated);
        markElevatedModuleResults(data,successfulModules,failedModules,diagnostics,sessionId,currentCount);
        log.info("[WINDOWS-UAC] Vollständigkeitsprüfung: Module erfolgreich={} fehlgeschlagen={} Felder={}",
          successfulModules,failedModules,data.size());
        diagnostic(diagnostics, sessionId, "WINDOWS_INVENTORY_UAC", "COMPLETED",
          "Erhöhte Windows-Inventarisierung wurde in einem einzigen UAC-Prozess ausgeführt", currentCount);
      }catch(Exception exception){
        diagnostic(diagnostics, sessionId, "WINDOWS_INVENTORY_UAC", "FAILED",
          "Erhöhter Gesamtinventarlauf fehlgeschlagen: "+safeMessage(exception), currentCount);
      }
    }else{
      // 40k36t124: Backend-lokal verwendet zuerst exakt dieselbe Vollinventar-
      // Scriptbasis wie der nachweislich funktionierende SSPI-WinRM-Pfad. Der
      // Transport bleibt lokal: ein Current-Context-PowerShell-Prozess, keine
      // Remoteverbindung. Nur wenn dieser SSPI-aequivalente Fastpath keine
      // verwertbaren Grunddaten liefert, folgt der bisherige modulare Fallback.
      long fastStart=System.nanoTime();
      boolean fastOk=false;
      try{
        diagnostic(diagnostics, sessionId, "WINDOWS_INVENTORY_FAST", "RUNNING",
          "Windows Backend-lokal: SSPI-aequivalenter Current-Context-Fastpath wird gestartet", currentCount);
        Map<String,String> combined=runKeyValueModuleFile(powershell,remoteFullInventoryScript(),300L);
        if(combined.isEmpty()) throw new IOException("gemeinsamer Current-Context-Lauf lieferte keine Werte");
        data.putAll(combined);
        markCurrentContextModuleResults(data,successfulModules,failedModules,diagnostics,sessionId,currentCount);
        fastOk=!data.isEmpty() && data.get("COMPUTER_NAME")!=null;
        long ms=TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-fastStart);
        log.info("[WINDOWS-LOCAL-T124] Route=SSPI_EQUIVALENT_CURRENT_CONTEXT_FILE Success={} DurationMs={} ModuleOk={} ModuleFailed={} Felder={}",
          fastOk,ms,successfulModules.size(),failedModules.size(),data.size());
        diagnostic(diagnostics, sessionId, "WINDOWS_INVENTORY_FAST", fastOk?"COMPLETED":"PARTIAL",
          "SSPI-aequivalenter Current-Context-Lauf nach "+ms+" ms "+(fastOk?"erfolgreich":"ohne ausreichende Grunddaten"), currentCount);
      }catch(Exception exception){
        long ms=TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-fastStart);
        log.warn("[WINDOWS-LOCAL-T124] Route=SSPI_EQUIVALENT_CURRENT_CONTEXT_FILE Success=false DurationMs={} Fehler={}",ms,safeMessage(exception));
        diagnostic(diagnostics, sessionId, "WINDOWS_INVENTORY_FAST", "PARTIAL",
          "SSPI-aequivalenter Current-Context-Fastpath fehlgeschlagen; modularer Fallback folgt: "+safeMessage(exception), currentCount);
      }

      if(!fastOk){
        data.clear();
        successfulModules.clear();
        failedModules.clear();
        long legacyStart=System.nanoTime();
        log.info("[WINDOWS-LOCAL-T120] Route=MODULAR_FALLBACK Start=true");
        runModule(powershell, "SYSTEM", systemScript(), FAST_TIMEOUT_SECONDS, false, data, successfulModules, failedModules, diagnostics, sessionId, currentCount);
        runModule(powershell, "BIOS_BOARD", biosBoardScript(), FAST_TIMEOUT_SECONDS, false, data, successfulModules, failedModules, diagnostics, sessionId, currentCount);
        runModule(powershell, "CPU_MEMORY", cpuMemoryScript(), FAST_TIMEOUT_SECONDS, false, data, successfulModules, failedModules, diagnostics, sessionId, currentCount);
        runModule(powershell, "STORAGE", storageScript(), FAST_TIMEOUT_SECONDS, false, data, successfulModules, failedModules, diagnostics, sessionId, currentCount);
        runModule(powershell, "NETWORK", networkScript(), FAST_TIMEOUT_SECONDS, false, data, successfulModules, failedModules, diagnostics, sessionId, currentCount);
        runModule(powershell, "SOFTWARE", softwareScript(), SOFTWARE_TIMEOUT_SECONDS, false, data, successfulModules, failedModules, diagnostics, sessionId, currentCount);
        runModule(powershell, "SECURITY", securityScript(), SECURITY_TIMEOUT_SECONDS, false, data, successfulModules, failedModules, diagnostics, sessionId, currentCount);
        runModule(powershell, "MAINTENANCE", maintenanceScript(), FAST_TIMEOUT_SECONDS, false, data, successfulModules, failedModules, diagnostics, sessionId, currentCount);
        runModule(powershell, "UPDATES", updateScript(), UPDATE_TIMEOUT_SECONDS, false, data, successfulModules, failedModules, diagnostics, sessionId, currentCount);
        log.info("[WINDOWS-LOCAL-T120] Route=MODULAR_FALLBACK DurationMs={} ModuleOk={} ModuleFailed={} Felder={}",
          TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-legacyStart),successfulModules.size(),failedModules.size(),data.size());
      }
    }

    if (data.isEmpty()) {
      diagnostic(diagnostics, sessionId, "WINDOWS_INVENTORY", "FAILED", "PowerShell lieferte in keinem Modul verwertbare Daten", currentCount);
      return;
    }

    String computerName = value(data, "COMPUTER_NAME", localHostName());
    String caption = value(data, "OS_CAPTION", "Windows");
    String version = value(data, "OS_VERSION", null);
    String build = value(data, "OS_BUILD", null);
    String architecture = value(data, "OS_ARCH", null);
    String installDate = value(data, "OS_INSTALL_DATE", null);
    String lastBoot = value(data, "OS_LAST_BOOT", null);
    String manufacturer = value(data, "SYSTEM_MANUFACTURER", null);
    String model = value(data, "SYSTEM_MODEL", null);
    String systemType = value(data, "SYSTEM_TYPE", null);
    String domain = value(data, "DOMAIN", null);
    String user = value(data, "USER_NAME", null);
    String serial = value(data, "BIOS_SERIAL", null);
    String biosVersion = value(data, "BIOS_VERSION", null);
    String biosDate = value(data, "BIOS_DATE", null);
    String boardManufacturer = value(data, "BOARD_MANUFACTURER", null);
    String boardProduct = value(data, "BOARD_PRODUCT", null);
    String processor = value(data, "CPU_NAME", null);
    String cores = value(data, "CPU_CORES", null);
    String threads = value(data, "CPU_THREADS", null);
    String ram = value(data, "RAM", null);
    String ramMaximum = value(data, "RAM_MAX", null);
    String ramSlotsTotal = value(data, "RAM_SLOTS_TOTAL", null);
    String ramSlotsUsed = value(data, "RAM_SLOTS_USED", null);
    String ramSlotsFree = value(data, "RAM_SLOTS_FREE", null);
    String ramType = value(data, "RAM_TYPE", null);
    String ramSpeed = value(data, "RAM_SPEED", null);
    String ramEcc = value(data, "RAM_ECC", null);
    String ramModules = value(data, "RAM_MODULES", null);
    String ramAssessment = value(data, "RAM_ASSESSMENT", null);
    String cpuClock = value(data, "CPU_CLOCK", null);
    String disks = value(data, "DISKS", null);
    String volumes = value(data, "VOLUMES", null);
    String networkVolumes = value(data, "NETWORK_VOLUMES", null);
    String opticalVolumes = value(data, "OPTICAL_VOLUMES", null);
    String removableVolumes = value(data, "REMOVABLE_VOLUMES", null);
    String ipv4 = value(data, "IPV4", null);
    String ipv6 = value(data, "IPV6", null);
    String mac = normalizeMac(value(data, "MAC", null));
    String adapters = value(data, "ADAPTERS", null);
    String softwareCount = value(data, "SOFTWARE_COUNT", null);
    String softwareRegistryCount = value(data, "SOFTWARE_REGISTRY_COUNT", null);
    String softwareSourceCounts = value(data, "SOFTWARE_SOURCE_COUNTS", null);
    String software = value(data, "SOFTWARE", null);
    String appxUserCount = value(data, "APPX_USER_COUNT", null);
    String appxOtherUsersCount = value(data, "APPX_OTHER_USERS_COUNT", null);
    String appxSystemCount = value(data, "APPX_SYSTEM_COUNT", null);
    String softwareUserScope = value(data, "SOFTWARE_USER_SCOPE", null);
    String softwareProfileHives = value(data, "SOFTWARE_PROFILE_HIVES", null);
    String softwareBaselineStatus = value(data, "SOFTWARE_BASELINE_STATUS", null);
    String appxUserScope = value(data, "APPX_USER_SCOPE", null);
    String appxAllUsersReadable = value(data, "APPX_ALLUSERS_READABLE", null);
    String appxProvisionedReadable = value(data, "APPX_PROVISIONED_READABLE", null);
    String appxDiagnostic = value(data, "APPX_DIAGNOSTIC", null);
    String roles = value(data, "FEATURES", null);
    String antivirus = value(data, "ANTIVIRUS", null);
    String defenderStatus = value(data, "DEFENDER", null);
    String firewall = value(data, "FIREWALL", null);
    String tpm = value(data, "TPM", null);
    String secureBoot = value(data, "SECURE_BOOT", null);
    String bitLocker = value(data, "BITLOCKER_VOLUMES", value(data, "BITLOCKER", null));
    String bitLockerSummary = value(data, "BITLOCKER_SUMMARY", null);
    String bitLockerDiagnostic = value(data, "BITLOCKER_DIAGNOSTIC", null);
    String systemVolumeEncrypted = value(data, "SYSTEM_VOLUME_ENCRYPTED", null);
    String storagePools = value(data, "STORAGE_POOLS", null);
    String virtualDisks = value(data, "VIRTUAL_DISKS", null);
    String physicalDisks = value(data, "PHYSICAL_DISKS", null);
    String pendingUpdatesRaw = value(data, "PENDING_UPDATES", failedModules.contains("UPDATES") ? "Zeitüberschreitung / nicht verfügbar" : null);
    String pendingUpdates = numericWithUnit(pendingUpdatesRaw, "Updates");
    String lastHotfix = value(data, "LAST_HOTFIX", null);
    String rebootPending = value(data, "REBOOT_PENDING", null);
    String uac = value(data, "UAC", null);
    String rdp = value(data, "RDP", null);
    String localAdmins = value(data, "LOCAL_ADMINS", null);
    String permissionUser = value(data, "PERMISSION_USER", null);
    String permissionAdmin = value(data, "PERMISSION_ADMIN", null);
    String permissionSystem = value(data, "PERMISSION_SYSTEM", null);
    String permissionIntegrity = value(data, "PERMISSION_INTEGRITY", null);
    String permissionCim = value(data, "PERMISSION_CIM", null);
    String permissionBitLocker = value(data, "PERMISSION_BITLOCKER", null);
    String permissionServices = value(data, "PERMISSION_SERVICES", null);
    String inventoryLevel = value(data, "INVENTORY_LEVEL", null);
    String permissionLimitations = value(data, "PERMISSION_LIMITATIONS", null);
    String permissionMode = "Ja".equalsIgnoreCase(permissionSystem) ? "SYSTEM" :
        ("Ja".equalsIgnoreCase(permissionAdmin) ? "Administrator" : "Standard");
    latestSoftwareDetails = value(data, "SOFTWARE_DETAILS", null);

    String displayManufacturer = join(" ", manufacturer, model);
    String protocol = "Windows-Inventarisierung / modulare PowerShell-Abfragen"
        + detail("Edition", caption) + detail("Version", version) + detail("Build", build)
        + detail("Architektur", architecture) + detail("Installation", installDate)
        + detail("Letzter Neustart", lastBoot) + detail("Systemtyp", systemType)
        + detail("Domäne/Arbeitsgruppe", domain) + detail("Benutzer", user)
        + detail("BIOS", join(" / ", biosVersion, biosDate))
        + detail("Mainboard", join(" ", boardManufacturer, boardProduct))
        + detail("Prozessor", processor)
        + detail("CPU", countLabel(cores, "Kerne") + countSeparator(cores, threads) + countLabel(threads, "Threads") + suffix(cpuClock, " · "))
        + detail("Arbeitsspeicher", ram)
        + detail("RAM-Typ", join(" / ", ramType, ramSpeed))
        + detail("Maximal unterstützter RAM", ramMaximum)
        + detail("RAM-Steckplätze", slotSummary(ramSlotsUsed, ramSlotsTotal, ramSlotsFree))
        + detail("RAM-Fehlerkorrektur", ramEcc)
        + detail("RAM-Module", ramModules)
        + detail("RAM-Erweiterbarkeit", ramAssessment)
        + detail("Datenträger", disks) + detail("Lokale Laufwerke", volumes)
        + detail("Netzlaufwerke", networkVolumes)
        + detail("Optische Laufwerke", opticalVolumes)
        + detail("Wechseldatenträger", removableVolumes)
        + detail("Physische Datenträger", physicalDisks)
        + detail("Storage Pools", storagePools) + detail("Virtuelle Datenträger", virtualDisks)
        + detail("IPv6", ipv6) + detail("Netzwerkadapter", adapters)
        + detail("Installierte Software", softwareCount == null ? software : softwareCount + " Programme"
            + (softwareRegistryCount == null || softwareRegistryCount.equals(softwareCount) ? "" : " (" + softwareRegistryCount + " Registry-Einträge)")
            + (software == null ? "" : " – " + software))
        + detail("Softwarequellen", softwareSourceCounts)
        + detail("Benutzer-Software-Erfassung", softwareUserScope)
        + detail("Zusätzlich geladene Benutzerprofile", softwareProfileHives)
        + detail("Software-Basisstatus", softwareBaselineStatus)
        + detail("Windows Store-/AppX-Apps", (appxUserCount==null&&appxOtherUsersCount==null&&appxSystemCount==null)?null:
            (appxUserCount==null?"0":appxUserCount)+" aktueller Benutzer · "
            +(appxOtherUsersCount==null?"0":appxOtherUsersCount)+" weitere Benutzer · "
            +(appxSystemCount==null?"0":appxSystemCount)+" System-/Provisioned-Apps")
        + detail("AppX-Benutzerumfang", appxUserScope)
        + detail("AppX AllUsers lesbar", appxAllUsersReadable)
        + detail("Provisioned AppX lesbar", appxProvisionedReadable)
        + detail("AppX/MSIX-Diagnose", appxDiagnostic)
        + detail("Windows-Rollen/Features", roles) + detail("Antivirus", antivirus)
        + detail("Microsoft Defender", defenderStatus) + detail("Firewall", firewall)
        + detail("TPM", tpm) + detail("Secure Boot", secureBoot)
        + detail("BitLocker-Zusammenfassung", bitLockerSummary)
        + detail("BitLocker-Volumes", bitLocker)
        + detail("Systemlaufwerk verschlüsselt", systemVolumeEncrypted)
        + detail("BitLocker-Diagnose", bitLockerDiagnostic)
        + detail("Ausstehende Updates", pendingUpdates) + detail("Letzter Hotfix", lastHotfix)
        + detail("Neustart ausstehend", rebootPending) + detail("UAC", uac)
        + detail("Remote Desktop", rdp) + detail("Lokale Administratoren", localAdmins)
        + detail("Berechtigungsbenutzer", permissionUser)
        + detail("Administratorrechte", permissionAdmin)
        + detail("SYSTEM-Rechte", permissionSystem)
        + detail("Integritätsstufe", permissionIntegrity)
        + detail("CIM lesbar", permissionCim)
        + detail("BitLocker lesbar", permissionBitLocker)
        + detail("Dienste lesbar", permissionServices)
        + detail("Inventarisierungstiefe", inventoryLevel)
        + detail("Berechtigungsmodus", permissionMode)
        + detail("Berechtigungseinschränkungen", permissionLimitations)
        + detail("Erfolgreiche Module", String.join(", ", successfulModules))
        + detail("Nicht verfügbare Module", failedModules.isEmpty() ? "keine" : String.join(", ", failedModules));

    out.accept(new DiscoveredDevice(
        "windows-local:" + stableId(computerName, serial, mac), computerName, "Windows-PC", ipv4, mac,
        protocol, "ONLINE", displayManufacturer, serial, now, false));

    String result = caption + (build == null ? "" : " · Build " + build)
        + (processor == null ? "" : " · " + processor) + (ram == null ? "" : " · " + ram + " RAM")
        + " · Module " + successfulModules.size() + "/9";
    diagnostic(diagnostics, sessionId, "WINDOWS_INVENTORY", failedModules.isEmpty() ? "COMPLETED" : "PARTIAL",
        result + (failedModules.isEmpty() ? " erkannt" : " erkannt; nicht verfügbar: " + String.join(", ", failedModules)), currentCount + 1);
  }

  private static String elevatedFullInventoryScript(){
    String[] names={"SYSTEM","BIOS_BOARD","CPU_MEMORY","STORAGE","NETWORK","SOFTWARE","SECURITY","MAINTENANCE","UPDATES"};
    String[] scripts={systemScript(),biosBoardScript(),cpuMemoryScript(),storageScript(),networkScript(),softwareScript(),securityScript(),maintenanceScript(),updateScript()};
    StringBuilder b=new StringBuilder();
    b.append("$ErrorActionPreference='Continue';");
    for(int i=0;i<scripts.length;i++){
      String module=names[i];
      // 40k36t70: Im UAC-Wrapper muss GAMKV durch die Success-Pipeline laufen,
      // weil der Wrapper diese Pipeline in result.txt umleitet. Direkte
      // [Console]::Out.WriteLine()-Ausgaben umgehen die Pipeline und gingen in t58 verloren.
      String elevatedScript=scripts[i].replace(
        "Write-Output ('GAMKV|'+$k+'|'+$b64)",
        "Write-Output ('GAMKV|'+$k+'|'+$b64)"
      );
      b.append("try{ & { ").append(elevatedScript).append(" } ; ");
      b.append("$m=[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes('OK'));");
      b.append("Write-Output ('GAMKV|MODULE_").append(module).append("|'+$m)");
      b.append(" }catch{ ");
      b.append("$m=[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes(('FAILED: '+$_.Exception.Message)));");
      b.append("Write-Output ('GAMKV|MODULE_").append(module).append("|'+$m)");
      b.append(" };");
    }
    return b.toString();
  }

  /** 40k36t124: Ergebnisbewertung des lokalen SSPI-aequivalenten Fastpaths.
   *  remoteFullInventoryScript() liefert bewusst dieselbe Nutzlast wie der
   *  funktionierende SSPI-WinRM-Pfad, jedoch ohne MODULE_* Wrappermarker. */
  private static void markCurrentContextModuleResults(
      Map<String,String> data,
      List<String> successes,
      List<String> failures,
      Consumer<DeviceDiscoveryService.DiscoveryDiagnosticEvent> diagnostics,
      String sessionId,
      int currentCount){
    String[][] probes={
      {"SYSTEM","COMPUTER_NAME","OS_CAPTION"},
      {"BIOS_BOARD","BIOS_VERSION","BOARD_PRODUCT"},
      {"CPU_MEMORY","CPU_NAME","RAM"},
      {"STORAGE","DISK_DETAILS","VOLUME_DETAILS","SYSTEM_DRIVE"},
      {"NETWORK","IPV4","MAC"},
      {"SOFTWARE","SOFTWARE_COUNT","SOFTWARE_DETAILS"},
      {"SECURITY","DEFENDER_STATUS","FIREWALL"},
      {"MAINTENANCE","OS_LAST_BOOT","REBOOT_PENDING"},
      {"UPDATES","PENDING_UPDATES","LAST_HOTFIX"}
    };
    for(String[] probe:probes){
      boolean ok=false;
      for(int i=1;i<probe.length;i++){
        String value=data.get(probe[i]);
        if(value!=null && !value.isBlank()){ok=true;break;}
      }
      if(ok){
        successes.add(probe[0]);
        diagnostic(diagnostics,sessionId,"WINDOWS_INVENTORY_"+probe[0],"COMPLETED",
          moduleLabel(probe[0])+" im gemeinsamen Current-Context-Prozess gelesen",currentCount);
      }else{
        failures.add(probe[0]);
        diagnostic(diagnostics,sessionId,"WINDOWS_INVENTORY_"+probe[0],"PARTIAL",
          moduleLabel(probe[0])+" lieferte im gemeinsamen Current-Context-Prozess keine Nutzlast",currentCount);
      }
    }
  }

  private static void markElevatedModuleResults(
      Map<String,String> data,
      List<String> successes,
      List<String> failures,
      Consumer<DeviceDiscoveryService.DiscoveryDiagnosticEvent> diagnostics,
      String sessionId,
      int currentCount){
    String[] names={"SYSTEM","BIOS_BOARD","CPU_MEMORY","STORAGE","NETWORK","SOFTWARE","SECURITY","MAINTENANCE","UPDATES"};
    for(String module:names){
      String marker=data.remove("MODULE_"+module);
      boolean ok="OK".equalsIgnoreCase(marker);
      boolean softwarePayloadComplete="SOFTWARE".equals(module)
          && data.get("SOFTWARE_DETAILS")!=null && !data.get("SOFTWARE_DETAILS").isBlank()
          && data.get("SOFTWARE_BASELINE_STATUS")!=null
          && data.get("SOFTWARE_COUNT")!=null
          && data.get("SOFTWARE_CAPTURED_COUNT")!=null
          && data.get("SOFTWARE_COUNT").equals(data.get("SOFTWARE_CAPTURED_COUNT"));
      if(ok || softwarePayloadComplete){
        successes.add(module);
        if(!ok && softwarePayloadComplete){
          log.warn("[WINDOWS-UAC-MODULE] SOFTWARE meldete nach vollständiger Nutzlast noch einen optionalen Teilfehler: {}. "
              +"Die bereits vollständig gelieferte strukturierte Software bleibt gültig.",marker==null?"kein Status":marker);
          diagnostic(diagnostics,sessionId,"WINDOWS_INVENTORY_"+module,"COMPLETED",
            "Software vollständig übernommen; optionaler Nachlauf meldete: "+(marker==null?"kein Status":marker),currentCount);
        }else{
          diagnostic(diagnostics,sessionId,"WINDOWS_INVENTORY_"+module,"COMPLETED",
            moduleLabel(module)+" erfolgreich im gemeinsamen UAC-Prozess gelesen",currentCount);
        }
      }else{
        failures.add(module);
        log.warn("[WINDOWS-UAC-MODULE] {} fehlgeschlagen: {}",module,marker==null?"kein Status":marker);
        diagnostic(diagnostics,sessionId,"WINDOWS_INVENTORY_"+module,"PARTIAL",
          moduleLabel(module)+" im gemeinsamen UAC-Prozess nicht vollständig: "+(marker==null?"kein Status":marker),currentCount);
      }
    }
  }

  private static void runModule(
      String executable,
      String module,
      String script,
      long timeout,
      boolean elevatedUac,
      Map<String, String> target,
      List<String> successes,
      List<String> failures,
      Consumer<DeviceDiscoveryService.DiscoveryDiagnosticEvent> diagnostics,
      String sessionId,
      int currentCount) {
    String phase = "WINDOWS_INVENTORY_" + module;
    diagnostic(diagnostics, sessionId, phase, "RUNNING", moduleLabel(module) + " werden gelesen", currentCount);
    try {
      Map<String, String> result = runKeyValueModule(executable, script, timeout, elevatedUac);
      if (result.isEmpty()) throw new IOException("keine Werte zurückgegeben");
      if ("SOFTWARE".equals(module)) {
        String expected = result.get("SOFTWARE_COUNT");
        String captured = result.get("SOFTWARE_CAPTURED_COUNT");
        if (expected != null && captured != null && !expected.equals(captured)) {
          throw new IOException("Softwareliste unvollständig: " + captured + " von " + expected + " Programmen erfasst");
        }
      }
      target.putAll(result);
      successes.add(module);
      String completedMessage = "SOFTWARE".equals(module) && result.get("SOFTWARE_CAPTURED_COUNT") != null
          ? result.get("SOFTWARE_CAPTURED_COUNT") + " Programme vollständig erfasst"
          : result.size() + " Werte gelesen";
      diagnostic(diagnostics, sessionId, phase, "COMPLETED", completedMessage, currentCount);
    } catch (Exception exception) {
      failures.add(module);
      diagnostic(diagnostics, sessionId, phase, "PARTIAL", "Übersprungen: " + safeMessage(exception), currentCount);
    }
  }

  /** 40k36t123: großer lokaler Current-Context-Lauf aus einer temporaeren PS1-Datei.
   *  Die Scriptdatei vermeidet sowohl die Windows-CreateProcess-Kommandozeilenbegrenzung
   *  als auch Encoding-/Parserprobleme von Windows PowerShell 5.1 bei -Command -. */
  private static Map<String, String> runKeyValueModuleFile(String executable, String script, long timeoutSeconds) throws IOException, InterruptedException {
    String prefix = "try{$OutputEncoding=[System.Text.UTF8Encoding]::new();try{[Console]::OutputEncoding=$OutputEncoding}catch{}}catch{};"
        + "$ProgressPreference='SilentlyContinue';$InformationPreference='SilentlyContinue';"
        + "$VerbosePreference='SilentlyContinue';$DebugPreference='SilentlyContinue';";
    java.nio.file.Path tempScript = java.nio.file.Files.createTempFile("gam-windows-local-", ".ps1");
    try {
      // UTF-8 mit BOM ist fuer Windows PowerShell 5.1 die robusteste Dateikodierung.
      byte[] body=(prefix + script).getBytes(StandardCharsets.UTF_8);
      byte[] bomBody=new byte[body.length+3];
      bomBody[0]=(byte)0xEF;bomBody[1]=(byte)0xBB;bomBody[2]=(byte)0xBF;
      System.arraycopy(body,0,bomBody,3,body.length);
      java.nio.file.Files.write(tempScript,bomBody);

      Process process = new ProcessBuilder(List.of(executable, "-NoLogo", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass", "-File", tempScript.toAbsolutePath().toString()))
          .redirectErrorStream(true).start();
      java.io.ByteArrayOutputStream outputBuffer = new java.io.ByteArrayOutputStream();
      java.util.concurrent.atomic.AtomicReference<IOException> readerFailure = new java.util.concurrent.atomic.AtomicReference<>();
      Thread outputReader = Thread.ofPlatform().daemon(true).name("gam-powershell-file-output-reader").start(() -> {
        try (java.io.InputStream input = process.getInputStream()) {
          input.transferTo(outputBuffer);
        } catch (IOException exception) {
          readerFailure.set(exception);
        }
      });

      if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
        process.destroyForcibly();
        outputReader.join(2000L);
        throw new IOException("Zeitueberschreitung nach " + timeoutSeconds + " Sekunden");
      }
      outputReader.join(5000L);
      if (outputReader.isAlive()) {
        process.destroyForcibly();
        throw new IOException("PowerShell-Ausgabe konnte nicht vollstaendig gelesen werden");
      }
      if (readerFailure.get() != null) {
        throw new IOException("PowerShell-Ausgabe konnte nicht gelesen werden", readerFailure.get());
      }

      String output = outputBuffer.toString(StandardCharsets.UTF_8);
      Map<String,String> result=parseKeyValueRows(Arrays.stream(output.split("\\R")).toList());
      if(result.isEmpty()){
        String excerpt=output.replace('\r',' ').replace('\n',' ').trim();
        if(excerpt.length()>900) excerpt=excerpt.substring(0,900)+"...";
        throw new IOException("gemeinsamer Current-Context-Lauf lieferte keine Werte; PowerShell="+(excerpt.isBlank()?"<leer>":excerpt));
      }
      if (process.exitValue() != 0) {
        String excerpt=output.replace('\r',' ').replace('\n',' ').trim();
        if(excerpt.length()>900) excerpt=excerpt.substring(0,900)+"...";
        log.warn("[WINDOWS-LOCAL-T123] PowerShell Exit={} trotz verwertbarer GAMKV-Daten; Ausgabe={}",process.exitValue(),excerpt);
      }
      return result;
    } finally {
      try { java.nio.file.Files.deleteIfExists(tempScript); } catch (IOException ignored) {}
    }
  }

  private static Map<String, String> runKeyValueModule(String executable, String script, long timeoutSeconds, boolean elevatedUac) throws IOException, InterruptedException {
    String prefix = "$OutputEncoding=[Console]::OutputEncoding=[System.Text.UTF8Encoding]::new();"
        + "$ProgressPreference='SilentlyContinue';$InformationPreference='SilentlyContinue';"
        + "$VerbosePreference='SilentlyContinue';$DebugPreference='SilentlyContinue';";
    List<String> rows = elevatedUac ? commandElevatedUac(executable,prefix+script,timeoutSeconds) : command(powerShellCommand(executable, prefix + script), timeoutSeconds);
    Map<String, String> result = new LinkedHashMap<>();

    // 40k32b8: Nicht mehr von physischen Zeilenumbrüchen abhängig sein.
    // PowerShell kann mehrere GAMKV-Datensätze in einem Ausgabeblock mit Leerzeichen
    // zusammenführen. Jeder Marker wird deshalb unabhängig vom Zeilenlayout erkannt.
    String combined = String.join("\n", rows)
        .replace("\uFEFF", "")
        .replace("#< CLIXML", " ");
    java.util.regex.Matcher markerMatcher = java.util.regex.Pattern
        .compile("GAMKV\\|([A-Z0-9_]+)\\|([A-Za-z0-9+/=]+)(?=\\s+GAMKV\\||\\s*<|\\s*$)")
        .matcher(combined);
    while (markerMatcher.find()) {
      String key = markerMatcher.group(1);
      String encodedValue = markerMatcher.group(2);
      try {
        String value = new String(Base64.getDecoder().decode(encodedValue), StandardCharsets.UTF_8).trim();
        if (!value.isBlank()) result.put(key, value);
      } catch (IllegalArgumentException ignored) {
        // Ein beschädigter Einzelwert darf die übrigen Modulergebnisse nicht verwerfen.
      }
    }
    if (!result.isEmpty()) return result;

    // Kompatibilitäts-Fallback für ältere oder manuell angepasste Skripte.
    // Mehrere KEY=VALUE-Blöcke in einer physisch zusammengeklebten Zeile werden getrennt.
    String cleaned = rows.stream()
        .map(WindowsInventoryDiscoveryService::cleanPowerShellRow)
        .filter(value -> value != null && !value.isBlank())
        .reduce((left, right) -> left + "\n" + right)
        .orElse("");
    java.util.regex.Matcher legacyMatcher = java.util.regex.Pattern
        .compile("(?ms)(?:^|\\s)([A-Z][A-Z0-9_]+)=(.*?)(?=\\s+[A-Z][A-Z0-9_]+=|$)")
        .matcher(cleaned);
    while (legacyMatcher.find()) {
      String key = legacyMatcher.group(1).trim();
      String value = legacyMatcher.group(2).trim();
      if (!value.isBlank()) result.put(key, value);
    }
    return result;
  }

  private static String systemScript() {
    return """
        $ErrorActionPreference='Stop'
        $cs=Get-CimInstance Win32_ComputerSystem
        $os=Get-CimInstance Win32_OperatingSystem
        function Emit($k,$v){if($null -ne $v -and ([string]$v).Trim() -ne ''){$clean=(([string]$v)-replace '[\r\n]',' ').Trim();$b64=[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($clean));Write-Output ('GAMKV|'+$k+'|'+$b64)}}
        Emit 'COMPUTER_NAME' $env:COMPUTERNAME
        Emit 'OS_CAPTION' $os.Caption
        Emit 'OS_VERSION' $os.Version
        Emit 'OS_BUILD' $os.BuildNumber
        Emit 'OS_ARCH' $os.OSArchitecture
        if($os.InstallDate){Emit 'OS_INSTALL_DATE' $os.InstallDate.ToString('yyyy-MM-dd HH:mm')}
        if($os.LastBootUpTime){Emit 'OS_LAST_BOOT' $os.LastBootUpTime.ToString('yyyy-MM-dd HH:mm')}
        Emit 'SYSTEM_MANUFACTURER' $cs.Manufacturer
        Emit 'SYSTEM_MODEL' $cs.Model
        Emit 'SYSTEM_TYPE' $cs.SystemType
        Emit 'DOMAIN' $cs.Domain
        Emit 'USER_NAME' $cs.UserName
        """;
  }

  private static String biosBoardScript() {
    return """
        $ErrorActionPreference='Stop'
        $bios=Get-CimInstance Win32_BIOS
        $board=Get-CimInstance Win32_BaseBoard|Select-Object -First 1
        function Emit($k,$v){if($null -ne $v -and ([string]$v).Trim() -ne ''){$clean=(([string]$v)-replace '[\r\n]',' ').Trim();$b64=[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($clean));Write-Output ('GAMKV|'+$k+'|'+$b64)}}
        Emit 'BIOS_SERIAL' $bios.SerialNumber
        Emit 'BIOS_VERSION' ($bios.SMBIOSBIOSVersion -join ', ')
        if($bios.ReleaseDate){Emit 'BIOS_DATE' $bios.ReleaseDate.ToString('yyyy-MM-dd')}
        Emit 'BOARD_MANUFACTURER' $board.Manufacturer
        Emit 'BOARD_PRODUCT' $board.Product
        """;
  }

  private static String cpuMemoryScript() {
    return """
        $ErrorActionPreference='Stop'
        $cpu=Get-CimInstance Win32_Processor|Select-Object -First 1
        $arrays=@(Get-CimInstance Win32_PhysicalMemoryArray -ErrorAction SilentlyContinue)
        $modules=@(Get-CimInstance Win32_PhysicalMemory -ErrorAction SilentlyContinue)
        function Emit($k,$v){if($null -ne $v -and ([string]$v).Trim() -ne ''){$clean=(([string]$v)-replace '[\r\n]',' ').Trim();$b64=[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($clean));Write-Output ('GAMKV|'+$k+'|'+$b64)}}
        function MemoryType($n){switch([int]$n){20{'DDR'}21{'DDR2'}24{'DDR3'}26{'DDR4'}34{'DDR5'}default{'Unbekannt'}}}
        function EccType($n){switch([int]$n){3{'Keine'}4{'Parity'}5{'Single-Bit ECC'}6{'Multi-Bit ECC'}7{'CRC'}default{'Unbekannt'}}}
        Emit 'CPU_NAME' $cpu.Name
        Emit 'CPU_CORES' $cpu.NumberOfCores
        Emit 'CPU_THREADS' $cpu.NumberOfLogicalProcessors
        if($cpu.MaxClockSpeed){Emit 'CPU_CLOCK' ($cpu.MaxClockSpeed.ToString()+' MHz')}
        $installed=($modules|Measure-Object Capacity -Sum).Sum
        if($installed){Emit 'RAM' (([math]::Round($installed/1GB,1)).ToString()+' GB')}
        $array=$arrays|Select-Object -First 1
        $maxKb=if($array.MaxCapacityEx -and [uint64]$array.MaxCapacityEx -gt 0){[uint64]$array.MaxCapacityEx}elseif($array.MaxCapacity){[uint64]$array.MaxCapacity}else{0}
        if($maxKb -gt 0){Emit 'RAM_MAX' (([math]::Round(($maxKb*1KB)/1GB,1)).ToString()+' GB')}
        $slots=if($array.MemoryDevices){[int]$array.MemoryDevices}else{$modules.Count}
        Emit 'RAM_SLOTS_TOTAL' $slots
        Emit 'RAM_SLOTS_USED' $modules.Count
        Emit 'RAM_SLOTS_FREE' ([math]::Max(0,$slots-$modules.Count))
        $types=@($modules|ForEach-Object{MemoryType $_.SMBIOSMemoryType}|Where-Object{$_ -ne 'Unbekannt'}|Select-Object -Unique)
        if($types.Count){Emit 'RAM_TYPE' ($types -join ', ')}
        $speeds=@($modules|ForEach-Object{if($_.ConfiguredClockSpeed){$_.ConfiguredClockSpeed}elseif($_.Speed){$_.Speed}}|Where-Object{$_}|Select-Object -Unique)
        if($speeds.Count){Emit 'RAM_SPEED' (($speeds|ForEach-Object{([string]$_)+' MT/s'}) -join ', ')}
        if($array){Emit 'RAM_ECC' (EccType $array.MemoryErrorCorrection)}
        $moduleText=@($modules|ForEach-Object{
          $cap=if($_.Capacity){([math]::Round($_.Capacity/1GB,1)).ToString()+' GB'}else{'?'}
          $speed=if($_.ConfiguredClockSpeed){$_.ConfiguredClockSpeed}elseif($_.Speed){$_.Speed}else{$null}
          $speedText=if($speed){([string]$speed)+' MT/s'}else{$null}
          $parts=@($_.DeviceLocator,$cap,(MemoryType $_.SMBIOSMemoryType),$speedText,$_.Manufacturer,$_.PartNumber)|Where-Object{$_ -and ([string]$_).Trim()}
          $parts -join ' · '
        })
        if($moduleText.Count){Emit 'RAM_MODULES' ($moduleText -join '; ')}
        if($installed -and $maxKb -gt 0){
          $maxBytes=$maxKb*1KB
          if($installed -ge $maxBytes){$assessment='Maximale Speicherkapazität bereits erreicht'}
          elseif($slots -gt $modules.Count){$assessment=('Aufrüstung möglich: '+($slots-$modules.Count)+' freie Steckplätze, bis '+([math]::Round($maxBytes/1GB,1))+' GB unterstützt')}
          else{$assessment=('Aufrüstung nur durch Austausch vorhandener Module möglich, bis '+([math]::Round($maxBytes/1GB,1))+' GB unterstützt')}
          Emit 'RAM_ASSESSMENT' $assessment
        }
        """;
  }

  private static String storageScript() {
    return """
        $ErrorActionPreference='SilentlyContinue'
        function Emit($k,$v){if($null -ne $v -and ([string]$v).Trim() -ne ''){$clean=(([string]$v)-replace '[\r\n]',' ').Trim();$b64=[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($clean));Write-Output ('GAMKV|'+$k+'|'+$b64)}}
        $disks=@(Get-Disk -ErrorAction SilentlyContinue|ForEach-Object{
          $size=if($_.Size){[math]::Round($_.Size/1GB,1)}else{0}
          $boot=if($_.IsBoot){', Boot'}else{''};$system=if($_.IsSystem){', System'}else{''}
          ('Datenträger {0}: {1} · {2} GB · {3} · {4}{5}{6}' -f $_.Number,$_.FriendlyName,$size,$_.BusType,$_.PartitionStyle,$boot,$system)
        })
        if(-not $disks){$disks=@(Get-CimInstance Win32_DiskDrive|ForEach-Object{$size=if($_.Size){[math]::Round($_.Size/1GB,1)}else{0};('{0} · {1} GB · {2}'-f $_.Model,$size,$_.MediaType)})}
        $logical=@(Get-CimInstance Win32_LogicalDisk -ErrorAction SilentlyContinue)
        function FormatLogicalDrive($v){
          $name=if($v.VolumeName){' ['+$v.VolumeName+']'}else{''}
          $fs=if($v.FileSystem){' · '+$v.FileSystem}else{''}
          $capacity=''
          if($null -ne $v.Size -and [double]$v.Size -gt 0){
            $size=[math]::Round([double]$v.Size/1GB,1)
            $free=if($null -ne $v.FreeSpace){[math]::Round([double]$v.FreeSpace/1GB,1)}else{$null}
            $capacity=if($null -ne $free){' · '+$free+'/'+$size+' GB frei/gesamt'}else{' · '+$size+' GB gesamt'}
          }
          return ($v.DeviceID+$name+$fs+$capacity)
        }
        $localVolumes=@($logical|Where-Object{$_.DriveType -eq 3}|ForEach-Object{FormatLogicalDrive $_})
        $networkVolumes=@($logical|Where-Object{$_.DriveType -eq 4}|ForEach-Object{
          # Netzlaufwerke: absichtlich keine Kapazität aus potentiell synthetischen Windows-Werten übernehmen.
          $name=if($_.VolumeName){' ['+$_.VolumeName+']'}else{''}
          $fs=if($_.FileSystem){' · '+$_.FileSystem}else{''}
          $provider=if($_.ProviderName){' · '+$_.ProviderName}else{''}
          ($_.DeviceID+$name+$fs+$provider)
        })
        $opticalVolumes=@($logical|Where-Object{$_.DriveType -eq 5}|ForEach-Object{FormatLogicalDrive $_})
        $removableVolumes=@($logical|Where-Object{$_.DriveType -eq 2}|ForEach-Object{FormatLogicalDrive $_})
        $knownLocal=@{}; foreach($entry in $localVolumes){if($entry -match '^([A-Z]:)'){$knownLocal[$Matches[1]]=$true}}
        @(Get-Volume -ErrorAction SilentlyContinue|Where-Object{$_.DriveType -eq 'Fixed' -and $_.DriveLetter})|ForEach-Object{
          $drive=$_.DriveLetter+':'
          if(-not $knownLocal.ContainsKey($drive)){
            $size=if($_.Size){[math]::Round($_.Size/1GB,1)}else{$null};$free=if($_.SizeRemaining){[math]::Round($_.SizeRemaining/1GB,1)}else{$null}
            $cap=if($null -ne $size -and $null -ne $free){' · '+$free+'/'+$size+' GB frei/gesamt'}else{''}
            $label=if($_.FileSystemLabel){' ['+$_.FileSystemLabel+']'}else{''};$fs=if($_.FileSystem){' · '+$_.FileSystem}else{''}
            $localVolumes+=($drive+$label+$fs+$cap);$knownLocal[$drive]=$true
          }
        }
        Emit 'DISKS' ($disks -join '; ')
        Emit 'VOLUMES' ($localVolumes -join '; ')
        Emit 'NETWORK_VOLUMES' ($networkVolumes -join '; ')
        Emit 'OPTICAL_VOLUMES' ($opticalVolumes -join '; ')
        Emit 'REMOVABLE_VOLUMES' ($removableVolumes -join '; ')
        try{
          $pools=@(Get-StoragePool -ErrorAction Stop|Where-Object{$_.IsPrimordial -eq $false}|ForEach-Object{('{0}: Zustand {1}, Gesundheit {2}, Kapazität {3} GB, frei {4} GB'-f $_.FriendlyName,$_.OperationalStatus,$_.HealthStatus,[math]::Round($_.Size/1GB,1),[math]::Round(($_.Size-$_.AllocatedSize)/1GB,1))})
          Emit 'STORAGE_POOLS' ($pools -join '; ')
          $virtual=@(Get-VirtualDisk -ErrorAction SilentlyContinue|ForEach-Object{('{0}: {1}, {2}, {3} GB, Zustand {4}/{5}'-f $_.FriendlyName,$_.ResiliencySettingName,$_.ProvisioningType,[math]::Round($_.Size/1GB,1),$_.OperationalStatus,$_.HealthStatus)})
          Emit 'VIRTUAL_DISKS' ($virtual -join '; ')
          $physical=@(Get-PhysicalDisk -ErrorAction SilentlyContinue|ForEach-Object{('{0}: {1}, {2}, {3} GB, Zustand {4}'-f $_.FriendlyName,$_.MediaType,$_.BusType,[math]::Round($_.Size/1GB,1),$_.HealthStatus)})
          Emit 'PHYSICAL_DISKS' ($physical -join '; ')
        }catch{}
        $bitLockerRead=$false
        $bitLockerPrimaryError=''

        try{
          $managePath=$null
          if(Test-Path "$env:WINDIR\\Sysnative\\manage-bde.exe"){$managePath="$env:WINDIR\\Sysnative\\manage-bde.exe"}
          elseif(Test-Path "$env:WINDIR\\System32\\manage-bde.exe"){$managePath="$env:WINDIR\\System32\\manage-bde.exe"}
          else{$cmd=Get-Command manage-bde.exe -ErrorAction SilentlyContinue;if($cmd){$managePath=$cmd.Source}}

          if($managePath){
            $raw=@(& $managePath -status 2>&1)
            $manageExit=$LASTEXITCODE
            $compact=(($raw|Out-String)-replace '[\\r\\n]+',' ').Trim()
            Emit 'BITLOCKER_MANAGE_BDE_PATH' $managePath
            Emit 'BITLOCKER_MANAGE_BDE_EXIT' $manageExit

            if($manageExit -eq 0 -and $raw.Count -gt 0){
              $text=($raw -join "`n")
              $volumeMatches=[regex]::Matches($text,'(?ms)^\\s*Volume\\s+"?([A-Z]:)"?.*?(?=^\\s*Volume\\s+"?[A-Z]:"?|\\z)')
              $rows=@();$encrypted=0;$protected=0;$locked=0;$osProtected=$false

              foreach($vm in $volumeMatches){
                $mount=$vm.Groups[1].Value
                $body=$vm.Value
                function ReadManageValue($body,$names){
                  foreach($name in $names){
                    $m=[regex]::Match($body,'(?im)^\\s*'+[regex]::Escape($name)+'\\s*:\\s*(.+?)\\s*$')
                    if($m.Success){return $m.Groups[1].Value.Trim()}
                  }
                  return ''
                }

                $conversion=ReadManageValue $body @('Conversion Status','Konvertierungsstatus')
                $percentage=ReadManageValue $body @('Percentage Encrypted','Verschlüsselt (Prozent)','Verschlüsselt')
                $method=ReadManageValue $body @('Encryption Method','Verschlüsselungsmethode')
                $protection=ReadManageValue $body @('Protection Status','Schutzstatus')
                $lock=ReadManageValue $body @('Lock Status','Sperrungsstatus','Sperrstatus')
                $autoUnlock=ReadManageValue $body @('Automatic Unlock','Automatische Entsperrung')
                $keyProtectors=ReadManageValue $body @('Key Protectors','Schlüsselschutzvorrichtungen')

                $isEncrypted=$false
                $conversionNorm=($conversion+'').Trim()
                $percentageNorm=($percentage+'').Trim()

                if($conversionNorm -match 'Fully Decrypted|Vollständig entschlüsselt|Vollstaendig entschluesselt'){
                  $isEncrypted=$false
                }elseif($percentageNorm -match '([0-9]+(?:[\\.,][0-9]+)?)'){
                  $pct=[double](($Matches[1]-replace ',','.'))
                  $isEncrypted=($pct -gt 0)
                }elseif($conversionNorm -match 'Fully Encrypted|Vollständig verschlüsselt|Vollstaendig verschluesselt|Encryption In Progress|Verschlüsselung wird ausgeführt|Entschlüsselung wird ausgeführt|Decryption In Progress'){
                  $isEncrypted=$true
                }

                if($isEncrypted){$encrypted++}
                if($protection -match 'Protection On|Schutz ist aktiviert|Schutz aktiviert|Ein'){$protected++}
                if($lock -match 'Locked|Gesperrt'){$locked++}
                if($mount -ieq $env:SystemDrive -and $protection -match 'Protection On|Schutz ist aktiviert|Schutz aktiviert|Ein'){$osProtected=$true}

                $parts=@()
                if($conversion){$parts+=('Status '+$conversion)}
                if($protection){$parts+=('Schutz '+$protection)}
                if($lock){$parts+=('Sperre '+$lock)}
                if($method -and $method -notmatch '^(Kein|None)$'){$parts+=('Methode '+$method)}
                if($percentage){$parts+=('Verschlüsselt '+$percentage)}
                if($autoUnlock){$parts+=('Auto-Unlock '+$autoUnlock)}
                if($keyProtectors -and $keyProtectors -notmatch '^(Keine gefunden|None found)$'){$parts+=('Protektoren '+$keyProtectors)}
                $rows+=($mount+' – '+($parts -join ', '))
              }

              if(-not $rows.Count -and $compact){$rows+=('manage-bde: '+$compact)}
              if($rows.Count){
                Emit 'BITLOCKER_VOLUMES' ($rows -join '; ')

                Emit 'BITLOCKER_SUMMARY' ('{0} Volumes · {1} verschlüsselt · {2} geschützt · {3} gesperrt' -f $rows.Count,$encrypted,$protected,$locked)
                Emit 'SYSTEM_VOLUME_ENCRYPTED' $(if($osProtected){'Ja'}else{'Nein'})
                Emit 'BITLOCKER_METHOD' 'manage-bde'
                Emit 'BITLOCKER_SUMMARY_SOURCE' 'aus den finalen Volume-Zeilen berechnet'
                $bitLockerRead=$true
              }
            }elseif($compact){
              $bitLockerPrimaryError=$compact
            }
          }else{
            $bitLockerPrimaryError='manage-bde.exe nicht gefunden'
          }
        }catch{
          $bitLockerPrimaryError=(($_.Exception.Message)-replace '[\\r\\n]+',' ').Trim()
        }

        if(-not $bitLockerRead){
          try{
            $job=Start-Job -ScriptBlock { Get-BitLockerVolume -ErrorAction Stop }
            if(-not (Wait-Job -Job $job -Timeout 10)){
              Stop-Job -Job $job -ErrorAction SilentlyContinue
              Remove-Job -Job $job -Force -ErrorAction SilentlyContinue
              throw 'Get-BitLockerVolume Timeout nach 10 Sekunden'
            }
            $bl=@(Receive-Job -Job $job -ErrorAction Stop)
            Remove-Job -Job $job -Force -ErrorAction SilentlyContinue
            $rows=@();$encrypted=0;$protected=0;$locked=0;$osProtected=$false;$removable=0
            foreach($v in $bl){
              $mount=if($v.MountPoint){$v.MountPoint}else{'ohne Mountpoint'}
              $protectors=@($v.KeyProtector|ForEach-Object{$_.KeyProtectorType.ToString()}|Sort-Object -Unique)
              $protectorText=if($protectors.Count){$protectors -join ','}else{'keine'}
              if($v.VolumeStatus -ne 'FullyDecrypted'){$encrypted++}
              if($v.ProtectionStatus -eq 'On'){$protected++}
              if($v.LockStatus -eq 'Locked'){$locked++}
              if($v.VolumeType -eq 'OperatingSystem' -and $v.ProtectionStatus -eq 'On'){$osProtected=$true}
              $driveType=(Get-Volume -DriveLetter ($mount.TrimEnd(':')) -ErrorAction SilentlyContinue).DriveType
              if($driveType -eq 'Removable'){$removable++}
              $parts=@('Typ '+$v.VolumeType,'Status '+$v.VolumeStatus,'Schutz '+$v.ProtectionStatus,'Sperre '+$v.LockStatus)
              if($v.EncryptionMethod -and $v.EncryptionMethod.ToString() -ne 'None'){$parts+=('Methode '+$v.EncryptionMethod)}
              if($null -ne $v.EncryptionPercentage){$parts+=('Verschlüsselt '+$v.EncryptionPercentage+'%')}
              if($null -ne $v.AutoUnlockEnabled){$parts+=('Auto-Unlock '+$v.AutoUnlockEnabled)}
              if($protectorText -ne 'keine'){$parts+=('Protektoren '+$protectorText)}
              $rows+=($mount+' – '+($parts -join ', '))
            }
            Emit 'BITLOCKER_VOLUMES' ($rows -join '; ')
            Emit 'BITLOCKER_SUMMARY' ('{0} Volumes · {1} verschlüsselt · {2} geschützt · {3} gesperrt · {4} BitLocker To Go' -f $bl.Count,$encrypted,$protected,$locked,$removable)
            Emit 'SYSTEM_VOLUME_ENCRYPTED' $(if($osProtected){'Ja'}else{'Nein'})
            Emit 'BITLOCKER_METHOD' 'Get-BitLockerVolume'
            if($bitLockerPrimaryError){Emit 'BITLOCKER_PRIMARY_ERROR' $bitLockerPrimaryError}
            $bitLockerRead=$true
          }catch{
            $fallbackError=(($_.Exception.Message)-replace '[\\r\\n]+',' ').Trim()
            $combinedError=@($bitLockerPrimaryError,$fallbackError)|Where-Object{$_}
            Emit 'BITLOCKER_DIAGNOSTIC' ('BitLocker-Daten nicht verfügbar: '+($combinedError -join ' | '))
          }
        }

        Emit 'BITLOCKER_READABLE' $(if($bitLockerRead){'Ja'}else{'Nein'})
        if($bitLockerRead){Emit 'BITLOCKER_DIAGNOSTIC' ('BitLocker-Status vollständig erfasst über '+$(if($bitLockerMethod){$bitLockerMethod}else{'Windows-Lesepfad'}))}
        """;
  }

  private static String networkScript() {
    return """
        $ErrorActionPreference='Stop'
        function Emit($k,$v){if($null -ne $v -and ([string]$v).Trim() -ne ''){$clean=(([string]$v)-replace '[\r\n]',' ').Trim();$b64=[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($clean));Write-Output ('GAMKV|'+$k+'|'+$b64)}}
        $nets=Get-CimInstance Win32_NetworkAdapterConfiguration|Where-Object{$_.IPEnabled -and $_.IPAddress}
        $net=$nets|Select-Object -First 1
        $ipv4=if($net.IPAddress){$net.IPAddress|Where-Object{$_ -match '^[0-9]+([.][0-9]+){3}$'}|Select-Object -First 1}
        $ipv6=if($net.IPAddress){$net.IPAddress|Where-Object{$_ -match ':' -and $_ -notmatch '^fe80:'}|Select-Object -First 1}
        Emit 'IPV4' $ipv4
        Emit 'IPV6' $ipv6
        Emit 'MAC' $net.MACAddress
        $adapterRows=@(Get-CimInstance Win32_NetworkAdapter|Where-Object{$_.NetEnabled}|ForEach-Object{$speed=if($_.Speed){([math]::Round($_.Speed/1MB,0)).ToString()+' Mbit/s'}else{$null};(@($_.Name,$speed)|Where-Object{$_})-join ' · '}|Select-Object -Unique)
        Emit 'ADAPTERS' ($adapterRows -join '; ')
        """;
  }

  private static String softwareScript() {
    return """
        $ErrorActionPreference='SilentlyContinue'
        function Emit($k,$v){if($null -ne $v -and ([string]$v).Trim() -ne ''){$clean=(([string]$v)-replace '[\r\n]',' ').Trim();$b64=[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($clean));Write-Output ('GAMKV|'+$k+'|'+$b64)}}
        function ResolveSidName($sid){
          try{return (New-Object Security.Principal.SecurityIdentifier($sid)).Translate([Security.Principal.NTAccount]).Value}catch{return $sid}
        }
        function AddRegistryLocation($name,$path,$scopeName,$recordList,$sourceList){
          $items=@(Get-ItemProperty $path -ErrorAction SilentlyContinue|Where-Object{$_.DisplayName -and ([string]$_.DisplayName).Trim()})
          [void]$sourceList.Add(('{0}: {1}' -f [string]$name,[int]$items.Count))
          foreach($item in $items){
            [void]$recordList.Add([PSCustomObject]@{
              DisplayName=([string]$item.DisplayName).Trim()
              Version=([string]$item.DisplayVersion).Trim()
              Package=([string]$item.PSChildName).Trim()
              Path=([string]$item.InstallLocation).Trim()
              Source=$name
              Scope='USER'
              ScopeName=$scopeName
            })
          }
        }

        $identity=[Security.Principal.WindowsIdentity]::GetCurrent()
        $currentUser=$identity.Name
        $principal=New-Object Security.Principal.WindowsPrincipal($identity)
        $isAdmin=$principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
        $isSystem=$identity.User.Value -eq 'S-1-5-18'
        $forceDeepUsers=([string]$env:GAM_FORCE_DEEP_USERS) -eq '1'
        $deepUsers=$isAdmin -or $isSystem -or $forceDeepUsers

        $records=New-Object System.Collections.ArrayList
        $sourceCounts=New-Object System.Collections.ArrayList

        # Systemweite klassische Anwendungen.
        foreach($location in @(
          @{Name='HKLM 64-Bit';Path='HKLM:/Software/Microsoft/Windows/CurrentVersion/Uninstall/*'},
          @{Name='HKLM 32-Bit';Path='HKLM:/Software/WOW6432Node/Microsoft/Windows/CurrentVersion/Uninstall/*'}
        )){
          $items=@(Get-ItemProperty $location.Path -ErrorAction SilentlyContinue|Where-Object{$_.DisplayName -and ([string]$_.DisplayName).Trim()})
          [void]$sourceCounts.Add(('{0}: {1}' -f [string]$location.Name,[int]$items.Count))
          foreach($item in $items){
            [void]$records.Add([PSCustomObject]@{
              DisplayName=([string]$item.DisplayName).Trim();Version=([string]$item.DisplayVersion).Trim()
              Package=([string]$item.PSChildName).Trim();Path=([string]$item.InstallLocation).Trim()
              Source=$location.Name;Scope='SYSTEM';ScopeName=''
            })
          }
        }

        # Aktueller Benutzer über HKCU.
        AddRegistryLocation 'HKCU 64-Bit' 'HKCU:/Software/Microsoft/Windows/CurrentVersion/Uninstall/*' $currentUser $records $sourceCounts
        AddRegistryLocation 'HKCU 32-Bit' 'HKCU:/Software/WOW6432Node/Microsoft/Windows/CurrentVersion/Uninstall/*' $currentUser $records $sourceCounts

        # Alle bereits geladenen echten Benutzer-Hives unter HKU. Die SID des
        # aktuellen Benutzers wird ausgelassen, da HKCU denselben Hive abbildet.
        $loadedSids=@()
        $currentSid=$identity.User.Value
        if($deepUsers){
          $loadedSids=@(Get-ChildItem Registry::HKEY_USERS -ErrorAction SilentlyContinue|ForEach-Object{$_.PSChildName}|Where-Object{$_ -match '^S-1-5-21-' -and $_ -notmatch '_Classes$'})
          foreach($sid in $loadedSids){
            if($sid -eq $currentSid){continue}
            $account=ResolveSidName $sid
            AddRegistryLocation ('HKU '+$sid+' 64-Bit') ('Registry::HKEY_USERS\\'+$sid+'\\Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\*') $account $records $sourceCounts
            AddRegistryLocation ('HKU '+$sid+' 32-Bit') ('Registry::HKEY_USERS\\'+$sid+'\\Software\\WOW6432Node\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\*') $account $records $sourceCounts
          }
        }

        # Admin/SYSTEM: auch nicht geladene lokale Profile über NTUSER.DAT lesen.
        # Nur von GAM selbst geladene Hives werden anschließend wieder entladen.
        $profileHivesLoaded=0
        $profileHivesFailed=0
        if($deepUsers){
          $profiles=@(Get-CimInstance Win32_UserProfile -ErrorAction SilentlyContinue|Where-Object{
            $_.SID -match '^S-1-5-21-' -and $_.LocalPath -and -not $_.Special
          })
          foreach($profile in $profiles){
            $sid=[string]$profile.SID
            if($sid -eq $currentSid -or $loadedSids -contains $sid){continue}
            $ntuser=Join-Path ([string]$profile.LocalPath) 'NTUSER.DAT'
            if(-not (Test-Path $ntuser)){continue}
            $tempKey=('GAM_INV_{0}' -f ([string]$sid -replace '[^A-Za-z0-9_]','_'))
            $regKey=('HKU\\{0}' -f [string]$tempKey)
            $loaded=$false
            try{
              & reg.exe load $regKey $ntuser 2>$null|Out-Null
              if($LASTEXITCODE -eq 0){
                $loaded=$true;$profileHivesLoaded++
                $account=ResolveSidName $sid
                AddRegistryLocation ('PROFILE '+$sid+' 64-Bit') ('Registry::HKEY_USERS\\'+$tempKey+'\\Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\*') $account $records $sourceCounts
                AddRegistryLocation ('PROFILE '+$sid+' 32-Bit') ('Registry::HKEY_USERS\\'+$tempKey+'\\Software\\WOW6432Node\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\*') $account $records $sourceCounts
              }else{$profileHivesFailed++}
            }catch{$profileHivesFailed++}
            finally{
              if($loaded){
                [gc]::Collect();[gc]::WaitForPendingFinalizers()
                & reg.exe unload $regKey 2>$null|Out-Null
              }
            }
          }
        }

        $names=@($records|ForEach-Object{$_.DisplayName}|Sort-Object -Unique)
        Emit 'SOFTWARE_REGISTRY_COUNT' $records.Count
        Emit 'SOFTWARE_COUNT' $names.Count
        Emit 'SOFTWARE_CAPTURED_COUNT' $names.Count
        Emit 'SOFTWARE_SOURCE_COUNTS' ($sourceCounts -join '; ')
        Emit 'SOFTWARE' ($names -join '; ')
        Emit 'SOFTWARE_USER_SCOPE' $(if($deepUsers){'Alle erreichbaren Benutzerprofile'}else{'Aktueller Benutzer'})
        if($deepUsers){Emit 'SOFTWARE_PROFILE_HIVES' (('{0} zusätzlich geladen · {1} fehlgeschlagen' -f [int]$profileHivesLoaded,[int]$profileHivesFailed))}

        $details=@($records|ForEach-Object{
          @($_.Scope,$_.ScopeName,$_.DisplayName,$_.Version,$_.Package,$_.Path,$_.Source) -join '|'
        })

        # 40k36t84: Baseline SOFORT ausgeben. Falls ein optionaler AppX/MSIX-
        # Lesepfad später scheitert, bleiben HKLM/HKCU/HKU/PROFILE strukturiert
        # verfügbar und werden nicht mehr zusammen mit dem optionalen Teil verloren.
        if($details.Count){Emit 'SOFTWARE_DETAILS' ($details -join [char]30)}
        Emit 'SOFTWARE_BASELINE_STATUS' (('Registry-Basis gespeichert: {0} strukturierte Einträge' -f [int]$details.Count))

        # 40k36t83/t84: AppX/MSIX robust und diagnostizierbar.
        # SYSTEM besitzt kein normales interaktives AppX-Benutzerprofil. Deshalb
        # wird bei erhöhten Läufen grundsätzlich -AllUsers verwendet und die
        # Benutzerzuordnung aus PackageUserInformation ausgewertet.
        $appxFatalError=''
        try{
          $appxUser=@()
          $appxOtherUsers=@()
          $appxErrors=@()
          $appxAllUsersReadable=$false
          if($deepUsers){
            try{
              $allAppx=@(Get-AppxPackage -AllUsers -ErrorAction Stop|Where-Object{$_.Name -and $_.PackageFullName})
              $appxAllUsersReadable=$true
              foreach($pkg in $allAppx){
                $userInfos=@($pkg.PackageUserInformation)
                if($userInfos.Count -eq 0){
                  $appxOtherUsers+=[PSCustomObject]@{
                    Scope='USER';ScopeName='Alle Benutzer / Zuordnung nicht verfügbar'
                    DisplayName=[string]$pkg.Name;Version=[string]$pkg.Version
                    Package=[string]$pkg.PackageFullName;Path=[string]$pkg.InstallLocation
                    Source='Windows Store / AppX / MSIX (AllUsers)'
                  }
                  continue
                }
                foreach($ui in $userInfos){
                  # UserSecurityId ist je nach PowerShell-/Windows-Version ein SID-
                  # Objekt oder Text. ToString() ist zuverlässiger als ein direkter Cast.
                  $sid=''
                  try{if($null -ne $ui.UserSecurityId){$sid=$ui.UserSecurityId.ToString()}}catch{}
                  if(-not $sid){try{if($null -ne $ui.UserSid){$sid=$ui.UserSid.ToString()}}catch{}}
                  $account=if($sid){ResolveSidName $sid}else{'Benutzerzuordnung nicht verfügbar'}
                  $row=[PSCustomObject]@{
                    Scope='USER';ScopeName=$account
                    DisplayName=[string]$pkg.Name;Version=[string]$pkg.Version
                    Package=[string]$pkg.PackageFullName;Path=[string]$pkg.InstallLocation
                    Source='Windows Store / AppX / MSIX (AllUsers)'
                  }
                  if($sid -and $sid -eq $currentSid){$appxUser+=$row}else{$appxOtherUsers+=$row}
                }
              }
            }catch{
              $appxErrors+=(('AllUsers: {0}' -f [string]$_.Exception.Message))
              # 40k36t115: SSPI kann ohne erhoehten Token laufen. Wenn -AllUsers
              # deshalb nicht lesbar ist, wenigstens den aktuellen Benutzer erfassen,
              # statt AppX komplett leer zu lassen.
              try{
                $appxUser=@(Get-AppxPackage -ErrorAction Stop|Where-Object{$_.Name -and $_.PackageFullName}|ForEach-Object{
                  [PSCustomObject]@{
                    Scope='USER';ScopeName=$currentUser
                    DisplayName=[string]$_.Name;Version=[string]$_.Version
                    Package=[string]$_.PackageFullName;Path=[string]$_.InstallLocation
                    Source='Windows Store / AppX / MSIX (CurrentUser-Fallback)'
                  }
                })
              }catch{$appxErrors+=(('CurrentUser-Fallback: {0}' -f [string]$_.Exception.Message))}
            }
          }else{
            try{
              $appxUser=@(Get-AppxPackage -ErrorAction Stop|Where-Object{$_.Name -and $_.PackageFullName}|ForEach-Object{
                [PSCustomObject]@{
                  Scope='USER';ScopeName=$currentUser
                  DisplayName=[string]$_.Name;Version=[string]$_.Version
                  Package=[string]$_.PackageFullName;Path=[string]$_.InstallLocation
                  Source='Windows Store / AppX / MSIX'
                }
              })
            }catch{$appxErrors+=(('CurrentUser: {0}' -f [string]$_.Exception.Message))}
          }

          # Provisionierte Pakete sind die Basis für neu angelegte Benutzerprofile.
          $appxSystem=@()
          $appxProvisionedReadable=$false
          try{
            $appxSystem=@(Get-AppxProvisionedPackage -Online -ErrorAction Stop|Where-Object{$_.DisplayName -and $_.PackageName}|ForEach-Object{
              [PSCustomObject]@{
                Scope='SYSTEM';ScopeName=''
                DisplayName=[string]$_.DisplayName
                Version=if($_.Version){[string]$_.Version}else{''}
                Package=[string]$_.PackageName;Path=''
                Source='Windows provisionierte AppX / MSIX'
              }
            })
            $appxProvisionedReadable=$true
          }catch{$appxErrors+=(('Provisioned: {0}' -f [string]$_.Exception.Message))}

          # Deduplizieren pro Benutzer/Scope/Paket, nicht benutzerübergreifend.
          $appxUser=@($appxUser|Sort-Object ScopeName,Package -Unique)
          $appxOtherUsers=@($appxOtherUsers|Sort-Object ScopeName,Package -Unique)
          $appxSystem=@($appxSystem|Sort-Object Package -Unique)

          Emit 'APPX_USER_COUNT' $appxUser.Count
          Emit 'APPX_OTHER_USERS_COUNT' $appxOtherUsers.Count
          Emit 'APPX_SYSTEM_COUNT' $appxSystem.Count
          Emit 'APPX_USER_SCOPE' $(if($deepUsers){'Alle Benutzer'}else{'Aktueller Benutzer'})
          Emit 'APPX_ALLUSERS_READABLE' $(if($deepUsers){if($appxAllUsersReadable){'Ja'}else{'Nein'}}else{'Nicht angefordert'})
          Emit 'APPX_PROVISIONED_READABLE' $(if($appxProvisionedReadable){'Ja'}else{'Nein'})
          Emit 'APPX_DIAGNOSTIC' $(if($appxErrors.Count){$appxErrors -join ' | '}else{'AppX/MSIX vollständig über die vorgesehenen Windows-Lesepfade erfasst'})

          $appxDetails=@($appxUser+$appxOtherUsers+$appxSystem|ForEach-Object{
            @($_.Scope,$_.ScopeName,$_.DisplayName,$_.Version,$_.Package,$_.Path,$_.Source) -join '|'
          })
          $allDetails=@($details+$appxDetails)
          if($allDetails.Count){Emit 'SOFTWARE_DETAILS' ($allDetails -join [char]30)}
        }catch{
          $appxFatalError=$_.Exception.Message
          Emit 'APPX_DIAGNOSTIC' (('Optionaler AppX/MSIX-Teilfehler: {0}' -f [string]$appxFatalError))
          Emit 'APPX_ALLUSERS_READABLE' 'Nein'
          Emit 'APPX_PROVISIONED_READABLE' 'Nein'
          # SOFTWARE_DETAILS wurde bereits mit der Registry-Basis ausgegeben.
        }
        try{Emit 'FEATURES' ((Get-WindowsOptionalFeature -Online -ErrorAction Stop|Where-Object{$_.State -eq 'Enabled'}|Select-Object -ExpandProperty FeatureName)-join '; ')}catch{}
        """;
  }

  private static String securityScript() {
    return """
        $ErrorActionPreference='SilentlyContinue'
        function Emit($k,$v){if($null -ne $v -and ([string]$v).Trim() -ne ''){$clean=(([string]$v)-replace '[\r\n]',' ').Trim();$b64=[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($clean));Write-Output ('GAMKV|'+$k+'|'+$b64)}}
        try{Emit 'ANTIVIRUS' ((Get-CimInstance -Namespace root/SecurityCenter2 -ClassName AntivirusProduct -ErrorAction Stop|ForEach-Object{$_.displayName}|Select-Object -Unique)-join '; ')}catch{}
        try{$m=Get-MpComputerStatus -ErrorAction Stop;Emit 'DEFENDER' ('Echtzeitschutz '+$(if($m.RealTimeProtectionEnabled){'aktiv'}else{'aus'})+', Signatur '+$m.AntivirusSignatureVersion+', Alter '+$m.AntivirusSignatureAge+' Tage')}catch{}
        try{Emit 'FIREWALL' ((Get-NetFirewallProfile -ErrorAction Stop|ForEach-Object{$_.Name+': '+$(if($_.Enabled){'aktiv'}else{'aus'})})-join '; ')}catch{}
        try{$t=Get-Tpm -ErrorAction Stop;Emit 'TPM' ('vorhanden='+$t.TpmPresent+', bereit='+$t.TpmReady+', aktiviert='+$t.TpmEnabled)}catch{}
        try{Emit 'SECURE_BOOT' $(if(Confirm-SecureBootUEFI -ErrorAction Stop){'aktiv'}else{'aus'})}catch{Emit 'SECURE_BOOT' 'Nicht unterstützt / nicht verfügbar'}
        # BitLocker wird zentral im STORAGE-Modul über manage-bde erfasst; hier kein zweiter Provider-Aufruf.
        try{$v=(Get-ItemProperty 'HKLM:/SOFTWARE/Microsoft/Windows/CurrentVersion/Policies/System' -Name EnableLUA -ErrorAction Stop).EnableLUA;Emit 'UAC' $(if($v -eq 1){'aktiv'}else{'aus'})}catch{}
        try{$v=(Get-ItemProperty 'HKLM:/SYSTEM/CurrentControlSet/Control/Terminal Server' -Name fDenyTSConnections -ErrorAction Stop).fDenyTSConnections;Emit 'RDP' $(if($v -eq 0){'aktiviert'}else{'deaktiviert'})}catch{}
        try{Emit 'LOCAL_ADMINS' ((Get-LocalGroupMember -SID 'S-1-5-32-544' -ErrorAction Stop|ForEach-Object{$_.Name})-join '; ')}catch{}
        $identity=[Security.Principal.WindowsIdentity]::GetCurrent()
        $principal=New-Object Security.Principal.WindowsPrincipal($identity)
        $isAdmin=$principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
        $isSystem=$identity.User.Value -eq 'S-1-5-18'
        $integrity='Unbekannt';try{$who=(whoami /groups|Select-String 'S-1-16-([0-9]+)').Matches.Groups[1].Value;$integrity=switch([int]$who){12288{'Hoch'}16384{'System'}8192{'Mittel'}4096{'Niedrig'}default{'Unbekannt'}}}catch{}
        $canCim=$false;try{Get-CimInstance Win32_OperatingSystem -ErrorAction Stop|Out-Null;$canCim=$true}catch{}
        $canBitLocker=$false
        $bitLockerMethod='Nicht verfügbar'
        $bitLockerError=''
        try{
          $managePath=$null
          if(Test-Path "$env:WINDIR\\Sysnative\\manage-bde.exe"){$managePath="$env:WINDIR\\Sysnative\\manage-bde.exe"}
          elseif(Test-Path "$env:WINDIR\\System32\\manage-bde.exe"){$managePath="$env:WINDIR\\System32\\manage-bde.exe"}
          else{$manage=Get-Command manage-bde.exe -ErrorAction SilentlyContinue;if($manage){$managePath=$manage.Source}}

          if($managePath){
            $probe=@(& $managePath -status 2>&1)
            if($LASTEXITCODE -eq 0 -and $probe.Count -gt 0){
              $canBitLocker=$true;$bitLockerMethod='manage-bde'
            }else{
              $bitLockerError=(($probe|Out-String)-replace '[\\r\\n]+',' ').Trim()
            }
          }

          if(-not $canBitLocker){
            $cmd=Get-Command Get-BitLockerVolume -ErrorAction SilentlyContinue
            if($cmd){
              try{
                $job=Start-Job -ScriptBlock { Get-BitLockerVolume -ErrorAction Stop }
                if(-not (Wait-Job -Job $job -Timeout 10)){
                  Stop-Job -Job $job -ErrorAction SilentlyContinue
                  Remove-Job -Job $job -Force -ErrorAction SilentlyContinue
                  throw 'Get-BitLockerVolume Timeout nach 10 Sekunden'
                }
                Receive-Job -Job $job -ErrorAction Stop|Out-Null
                Remove-Job -Job $job -Force -ErrorAction SilentlyContinue
                $canBitLocker=$true;$bitLockerMethod='Get-BitLockerVolume'
              }catch{
                $secondary=(($_.Exception.Message)-replace '[\\r\\n]+',' ').Trim()
                if($bitLockerError){$bitLockerError=$bitLockerError+' | '+$secondary}else{$bitLockerError=$secondary}
              }
            }elseif(-not $bitLockerError){
              $bitLockerError='Weder manage-bde noch Get-BitLockerVolume verfügbar'
            }
          }
        }catch{
          $bitLockerError=(($_.Exception.Message)-replace '[\\r\\n]+',' ').Trim()
        }
        if($canBitLocker){$bitLockerError=''}
        $canServices=$false;try{Get-Service -ErrorAction Stop|Select-Object -First 1|Out-Null;$canServices=$true}catch{}
        $level=if($isSystem){'Vollzugriff'}elseif($isAdmin){'Administrator'}elseif($canCim -and $canServices){'Erweitert'}else{'Standard'}
        $limits=@();if(-not $isAdmin){$limits+='Keine Administratorrechte'};if(-not $canBitLocker){$limits+='BitLocker-Details nicht vollstaendig lesbar'+$(if($bitLockerError){' ('+$bitLockerError+')'}else{''})};if(-not $canServices){$limits+='Dienstinformationen eingeschränkt'}
        Emit 'PERMISSION_USER' $identity.Name
        Emit 'PERMISSION_ADMIN' $(if($isAdmin){'Ja'}else{'Nein'})
        Emit 'PERMISSION_SYSTEM' $(if($isSystem){'Ja'}else{'Nein'})
        Emit 'PERMISSION_INTEGRITY' $integrity
        Emit 'PERMISSION_CIM' $(if($canCim){'Ja'}else{'Nein'})
        Emit 'PERMISSION_BITLOCKER' $(if($canBitLocker){'Ja'}else{'Nein'})
        Emit 'PERMISSION_BITLOCKER_METHOD' $bitLockerMethod
        Emit 'PERMISSION_BITLOCKER_ERROR' $bitLockerError
        Emit 'PERMISSION_SERVICES' $(if($canServices){'Ja'}else{'Nein'})
        Emit 'INVENTORY_LEVEL' $level
        Emit 'PERMISSION_LIMITATIONS' $(if($limits.Count){$limits -join '; '}else{'keine'})
        """;
  }

  private static String maintenanceScript() {
    return """
        $ErrorActionPreference='SilentlyContinue'
        function Emit($k,$v){if($null -ne $v -and ([string]$v).Trim() -ne ''){$clean=(([string]$v)-replace '[\r\n]',' ').Trim();$b64=[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($clean));Write-Output ('GAMKV|'+$k+'|'+$b64)}}
        try{$h=Get-HotFix|Sort-Object InstalledOn -Descending|Select-Object -First 1;if($h){Emit 'LAST_HOTFIX' ($h.HotFixID+' ('+$h.InstalledOn.ToString('yyyy-MM-dd')+')')}}catch{}
        Emit 'REBOOT_PENDING' $(if((Test-Path 'HKLM:/SOFTWARE/Microsoft/Windows/CurrentVersion/Component Based Servicing/RebootPending')-or (Test-Path 'HKLM:/SOFTWARE/Microsoft/Windows/CurrentVersion/WindowsUpdate/Auto Update/RebootRequired')){'Ja'}else{'Nein'})
        """;
  }

  private static String updateScript() {
    return """
        $ErrorActionPreference='Stop'
        $s=New-Object -ComObject Microsoft.Update.Session
        $searcher=$s.CreateUpdateSearcher()
        $count=[string]($searcher.Search('IsInstalled=0 and IsHidden=0').Updates.Count)
        $b64=[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($count))
        [Console]::Out.WriteLine(('GAMKV|PENDING_UPDATES|'+$b64))
        """;
  }

  /** 40k36t45: dieselben modularen Windows-Abfragen auch fuer WinRM verwenden. */
  static String remoteFullInventoryScript() {
    return "try{$OutputEncoding=[System.Text.UTF8Encoding]::new();try{[Console]::OutputEncoding=$OutputEncoding}catch{}}catch{};"
        + "$ProgressPreference='SilentlyContinue';$InformationPreference='SilentlyContinue';"
        + "$VerbosePreference='SilentlyContinue';$DebugPreference='SilentlyContinue';"
        + systemScript() + biosBoardScript() + cpuMemoryScript() + storageScript()
        + networkScript() + softwareScript() + securityScript() + maintenanceScript()
        + updateScript();
  }

  private static List<String> powerShellCommand(String executable, String script) {
    String encoded = Base64.getEncoder().encodeToString(script.getBytes(StandardCharsets.UTF_16LE));
    return List.of(executable, "-NoLogo", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass", "-EncodedCommand", encoded);
  }

  private static Map<String,String> runKeyValueSystemTask(String executable,String script,long timeoutSeconds) throws IOException,InterruptedException {
    String prefix="$OutputEncoding=[Console]::OutputEncoding=[System.Text.UTF8Encoding]::new();"
        +"$ProgressPreference='SilentlyContinue';$InformationPreference='SilentlyContinue';"
        +"$VerbosePreference='SilentlyContinue';$DebugPreference='SilentlyContinue';";
    List<String> rows=commandSystemTask(executable,prefix+script,timeoutSeconds);
    return parseKeyValueRows(rows);
  }

  private static Map<String,String> parseKeyValueRows(List<String> rows){
    Map<String,String> result=new LinkedHashMap<>();
    String combined=String.join("\n",rows).replace("\uFEFF","").replace("#< CLIXML"," ");
    java.util.regex.Matcher markerMatcher=java.util.regex.Pattern
        .compile("GAMKV\\|([A-Z0-9_]+)\\|([A-Za-z0-9+/=]+)(?=\\s+GAMKV\\||\\s*<|\\s*$)")
        .matcher(combined);
    while(markerMatcher.find()){
      try{
        String value=new String(Base64.getDecoder().decode(markerMatcher.group(2)),StandardCharsets.UTF_8).trim();
        if(!value.isBlank()) result.put(markerMatcher.group(1),value);
      }catch(IllegalArgumentException ignored){}
    }
    return result;
  }

  private static List<String> commandSystemTask(String executable,String script,long timeoutSeconds) throws IOException,InterruptedException {
    java.nio.file.Path dir=java.nio.file.Files.createTempDirectory("gam-system-");
    String taskName="GAM-Inventory-"+java.util.UUID.randomUUID();
    java.nio.file.Path payload=dir.resolve("payload.ps1");
    java.nio.file.Path wrapper=dir.resolve("system-wrapper.ps1");
    java.nio.file.Path out=dir.resolve("result.txt");
    java.nio.file.Path status=dir.resolve("status.txt");
    java.nio.file.Path trace=dir.resolve("trace.txt");
    java.nio.file.Path launcherFile=dir.resolve("create-system-task.ps1");

    log.info("[WINDOWS-SYSTEM] Anforderung empfangen. Task={} Temp={}",taskName,dir);
    java.nio.file.Files.writeString(payload,script,StandardCharsets.UTF_8);
    log.info("[WINDOWS-SYSTEM] Payload geschrieben: {} ({} Zeichen)",payload,script.length());
    String p=payload.toAbsolutePath().toString().replace("'","''");
    String o=out.toAbsolutePath().toString().replace("'","''");
    String st=status.toAbsolutePath().toString().replace("'","''");
    String tr=trace.toAbsolutePath().toString().replace("'","''");
    String w=wrapper.toAbsolutePath().toString().replace("'","''");
    String lf=launcherFile.toAbsolutePath().toString().replace("'","''");
    String exe=executable.replace("'","''");
    String tn=taskName.replace("'","''");

    String wrapperScript=
      "$ErrorActionPreference='Continue';"+
      "try{"+
      "('SYSTEM_WRAPPER_STARTED '+(Get-Date -Format o))|Set-Content -LiteralPath '"+tr+"' -Encoding utf8;"+
      "$id=[Security.Principal.WindowsIdentity]::GetCurrent();"+
      "$pobj=New-Object Security.Principal.WindowsPrincipal($id);$admin=$pobj.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator);"+
      "('USER='+$id.Name+' SID='+$id.User.Value+' ADMIN='+$admin)|Add-Content -LiteralPath '"+tr+"' -Encoding utf8;"+
      "if($id.User.Value -ne 'S-1-5-18'){throw ('SYSTEM-Kontext nicht bestätigt: '+$id.Name+' / '+$id.User.Value)};"+
      "& '"+p+"' *>&1|ForEach-Object{$_|Out-String -Width 100000}|Out-File -LiteralPath '"+o+"' -Encoding utf8 -Width 100000;"+
      "('PAYLOAD_DONE '+(Get-Date -Format o))|Add-Content -LiteralPath '"+tr+"' -Encoding utf8;"+
      "'OK'|Set-Content -LiteralPath '"+st+"' -Encoding ascii"+
      "}catch{"+
      "('SYSTEM_WRAPPER_EXCEPTION='+($_|Out-String -Width 100000))|Add-Content -LiteralPath '"+tr+"' -Encoding utf8;"+
      "('GAM_SYSTEM_EXCEPTION='+($_|Out-String -Width 100000))|Out-File -LiteralPath '"+o+"' -Encoding utf8 -Append -Width 100000;"+
      "'FAILED'|Set-Content -LiteralPath '"+st+"' -Encoding ascii"+
      "}";
    java.nio.file.Files.writeString(wrapper,wrapperScript,StandardCharsets.UTF_8);
    log.info("[WINDOWS-SYSTEM] Wrapper geschrieben: {}",wrapper);

    // UAC is used only to create/start/delete this one-shot SYSTEM task.
    String launcherScript=
      "$ErrorActionPreference='Stop';"+
      "$task='"+tn+"';$wrapper='"+w+"';$status='"+st+"';"+
      "try{"+
      "$action=New-ScheduledTaskAction -Execute '"+exe+"' -Argument ('-NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File \"'+$wrapper+'\"');"+
      "$principal=New-ScheduledTaskPrincipal -UserId 'S-1-5-18' -LogonType ServiceAccount -RunLevel Highest;"+
      "$settings=New-ScheduledTaskSettingsSet -ExecutionTimeLimit (New-TimeSpan -Minutes 6);"+
      "$trigger=New-ScheduledTaskTrigger -Once -At ((Get-Date).AddMinutes(5));"+
      "Register-ScheduledTask -TaskName $task -Action $action -Principal $principal -Trigger $trigger -Settings $settings -Force|Out-Null;"+
      "Start-ScheduledTask -TaskName $task;"+
      "Write-Output ('GAM_SYSTEM_TASK_STARTED='+$task);"+
      "$deadline=(Get-Date).AddMinutes(6);"+
      "while((Get-Date) -lt $deadline -and -not (Test-Path -LiteralPath $status)){Start-Sleep -Milliseconds 500};"+
      "if(-not (Test-Path -LiteralPath $status)){throw 'SYSTEM-Aufgabe meldete innerhalb des Zeitlimits keinen Status'};"+
      "Write-Output ('GAM_SYSTEM_TASK_STATUS='+(Get-Content -LiteralPath $status -Raw).Trim())"+
      "}catch{Write-Output ('GAM_SYSTEM_TASK_ERROR='+($_|Out-String -Width 100000));exit 1}"+
      "finally{"+
      "try{Stop-ScheduledTask -TaskName $task -ErrorAction SilentlyContinue}catch{};"+
      "try{Unregister-ScheduledTask -TaskName $task -Confirm:$false -ErrorAction SilentlyContinue}catch{};"+
      "Write-Output ('GAM_SYSTEM_TASK_CLEANUP='+$task)"+
      "}";
    java.nio.file.Files.writeString(launcherFile,launcherScript,StandardCharsets.UTF_8);

    String launcher=
      "$ErrorActionPreference='Stop';"+
      "$args=@('-NoLogo','-NoProfile','-NonInteractive','-ExecutionPolicy','Bypass','-File','"+lf+"');"+
      "try{$x=Start-Process -FilePath 'powershell.exe' -ArgumentList $args -Verb RunAs -PassThru -Wait;exit $x.ExitCode}"+
      "catch{if($_.Exception.NativeErrorCode -eq 1223){exit 1223};Write-Output ($_|Out-String -Width 100000);exit 1}";

    log.info("[WINDOWS-SYSTEM] Eine UAC-Sitzung startet den kompletten SYSTEM-One-Shot (Registrieren → Starten → Warten → Löschen). Task={} Temp={}",taskName,dir);
    Process proc=new ProcessBuilder(powerShellCommand("powershell.exe",launcher)).redirectErrorStream(true).start();
    java.io.ByteArrayOutputStream launchBuf=new java.io.ByteArrayOutputStream();
    Thread reader=Thread.ofPlatform().daemon(true).name("gam-system-task-launcher").start(()->{
      try{proc.getInputStream().transferTo(launchBuf);}catch(Exception ignored){}
    });
    if(!proc.waitFor(timeoutSeconds+90L,TimeUnit.SECONDS)){
      proc.destroyForcibly();reader.join(2000L);
      throw new IOException("SYSTEM-One-Shot-Lauf überschritt das Zeitlimit");
    }
    reader.join(2000L);
    if(proc.exitValue()==1223) throw new IOException("UAC-Abfrage für SYSTEM-Inventarisierung wurde abgebrochen");
    if(proc.exitValue()!=0) throw new IOException("SYSTEM-Aufgabe konnte nicht erstellt werden: "+launchBuf.toString(StandardCharsets.UTF_8).strip());

    long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(timeoutSeconds);
    while(System.nanoTime()<deadline && !java.nio.file.Files.exists(status)) Thread.sleep(500L);

    String state=java.nio.file.Files.exists(status)?java.nio.file.Files.readString(status,StandardCharsets.UTF_8).trim():"";
    String output=java.nio.file.Files.exists(out)?java.nio.file.Files.readString(out,StandardCharsets.UTF_8):"";
    String traceText=java.nio.file.Files.exists(trace)?java.nio.file.Files.readString(trace,StandardCharsets.UTF_8):"";
    log.info("[WINDOWS-SYSTEM] Ergebnis task={} state={} outputChars={} trace={}",taskName,state,output.length(),
      traceText.isBlank()?"<leer>":traceText.replaceAll("[\r\n]+"," | "));

    log.info("[WINDOWS-SYSTEM] One-Shot-Launcher beendet. Aufgabe {} wurde innerhalb derselben UAC-Sitzung bereinigt.",taskName);

    IOException resultError=null;
    if(!"OK".equalsIgnoreCase(state)){
      resultError=!output.isBlank()?new IOException(output.strip()):
        new IOException("SYSTEM-Inventarisierung lieferte keinen OK-Status. "+traceText.strip());
    }else if(output.isBlank()){
      resultError=new IOException("SYSTEM-Inventarisierung lieferte keine Inventardaten");
    }
    try{
      java.nio.file.Files.deleteIfExists(payload);java.nio.file.Files.deleteIfExists(wrapper);
      java.nio.file.Files.deleteIfExists(out);java.nio.file.Files.deleteIfExists(status);
      java.nio.file.Files.deleteIfExists(trace);java.nio.file.Files.deleteIfExists(launcherFile);
      java.nio.file.Files.deleteIfExists(dir);
      log.info("[WINDOWS-SYSTEM] Temporäre Dateien entfernt. Task={}",taskName);
    }catch(Exception cleanupError){
      log.warn("[WINDOWS-SYSTEM] Temp-Cleanup fehlgeschlagen Task={}: {}",taskName,cleanupError.getMessage());
    }
    if(resultError!=null) throw resultError;
    return Arrays.stream(output.split("\\R")).toList();
  }


  private static List<String> commandElevatedUac(String executable,String script,long timeoutSeconds) throws IOException,InterruptedException {
    java.nio.file.Path dir=java.nio.file.Files.createTempDirectory("gam-uac-");
    java.nio.file.Path payload=dir.resolve("payload.ps1");
    java.nio.file.Path wrapper=dir.resolve("elevated-wrapper.ps1");
    java.nio.file.Path out=dir.resolve("result.txt");
    java.nio.file.Path status=dir.resolve("status.txt");
    java.nio.file.Path trace=dir.resolve("trace.txt");

    log.info("[WINDOWS-UAC] Anforderung empfangen. Temp={}",dir);
    java.nio.file.Files.writeString(payload,script,StandardCharsets.UTF_8);
    log.info("[WINDOWS-UAC] Payload geschrieben: {} ({} Zeichen)",payload,script.length());

    String p=payload.toAbsolutePath().toString().replace("'","''");
    String o=out.toAbsolutePath().toString().replace("'","''");
    String st=status.toAbsolutePath().toString().replace("'","''");
    String tr=trace.toAbsolutePath().toString().replace("'","''");

    String wrapperScript=
      "$ErrorActionPreference='Continue';"+
      "try{"+
      "  ('WRAPPER_STARTED '+(Get-Date -Format o)) | Set-Content -LiteralPath '"+tr+"' -Encoding utf8;"+
      "  $id=[Security.Principal.WindowsIdentity]::GetCurrent();"+
      "  $pobj=New-Object Security.Principal.WindowsPrincipal($id);"+
      "  $isAdmin=$pobj.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator);"+
      "  ('USER='+$id.Name+' ADMIN='+$isAdmin) | Add-Content -LiteralPath '"+tr+"' -Encoding utf8;"+
      "  & '"+p+"' *>&1 | ForEach-Object { $_ | Out-String -Width 100000 } | Out-File -LiteralPath '"+o+"' -Encoding utf8 -Width 100000;"+
      "  ('PAYLOAD_DONE '+(Get-Date -Format o)) | Add-Content -LiteralPath '"+tr+"' -Encoding utf8;"+
      "  'OK' | Set-Content -LiteralPath '"+st+"' -Encoding ascii"+
      "}catch{"+
      "  ('WRAPPER_EXCEPTION='+($_ | Out-String -Width 100000)) | Add-Content -LiteralPath '"+tr+"' -Encoding utf8;"+
      "  ('GAM_UAC_EXCEPTION='+($_ | Out-String -Width 100000)) | Out-File -LiteralPath '"+o+"' -Encoding utf8 -Append -Width 100000;"+
      "  'FAILED' | Set-Content -LiteralPath '"+st+"' -Encoding ascii; exit 1"+
      "}";
    java.nio.file.Files.writeString(wrapper,wrapperScript,StandardCharsets.UTF_8);
    log.info("[WINDOWS-UAC] Wrapper geschrieben: {}",wrapper);

    String exe=executable.replace("'","''");
    String w=wrapper.toAbsolutePath().toString().replace("'","''");
    String launcher=
      "$ErrorActionPreference='Stop';"+
      "try{"+
      "  Write-Output 'GAM_UAC_LAUNCH_BEGIN';"+
      "  $wrapper='"+w+"';"+
      "  $args=@('-NoLogo','-NoProfile','-NonInteractive','-ExecutionPolicy','Bypass','-File',$wrapper);"+
      "  $x=Start-Process -FilePath '"+exe+"' -ArgumentList $args -Verb RunAs -PassThru;"+
      "  Write-Output ('GAM_UAC_PID='+$x.Id);"+
      "  $x.WaitForExit();"+
      "  Write-Output ('GAM_UAC_EXIT='+$x.ExitCode);"+
      "  exit $x.ExitCode"+
      "}catch{"+
      "  if($_.Exception.NativeErrorCode -eq 1223){Write-Output 'GAM_UAC_CANCELLED';exit 1223};"+
      "  Write-Output ('GAM_UAC_LAUNCH_ERROR='+($_ | Out-String -Width 100000));exit 1"+
      "}";

    log.info("[WINDOWS-UAC] Start-Process -Verb RunAs wird aufgerufen. Executable={} Wrapper={}",executable,wrapper);
    Process proc=new ProcessBuilder(powerShellCommand("powershell.exe",launcher)).redirectErrorStream(true).start();
    log.info("[WINDOWS-UAC] Launcher-Prozess gestartet PID={}",proc.pid());

    java.io.ByteArrayOutputStream launchBuf=new java.io.ByteArrayOutputStream();
    Thread reader=Thread.ofPlatform().daemon(true).name("gam-uac-launcher-output").start(()->{
      try{proc.getInputStream().transferTo(launchBuf);}catch(Exception ignored){}
    });

    if(!proc.waitFor(timeoutSeconds,TimeUnit.SECONDS)){
      proc.destroyForcibly();
      reader.join(2000L);
      log.warn("[WINDOWS-UAC] Timeout nach {} Sekunden. Temp={}",timeoutSeconds,dir);
      throw new IOException("UAC-Inventarisierung: Zeitüberschreitung nach "+timeoutSeconds+" Sekunden");
    }
    reader.join(2000L);

    String launcherOutput=launchBuf.toString(StandardCharsets.UTF_8);
    log.info("[WINDOWS-UAC] Launcher beendet Exit={} Ausgabe={}",proc.exitValue(),launcherOutput.replaceAll("[\r\n]+"," | "));

    if(proc.exitValue()==1223 || launcherOutput.contains("GAM_UAC_CANCELLED")){
      log.info("[WINDOWS-UAC] UAC wurde vom Benutzer abgebrochen.");
      throw new IOException("UAC-Abfrage wurde vom Benutzer abgebrochen.");
    }

    boolean statusExists=java.nio.file.Files.exists(status);
    boolean outputExists=java.nio.file.Files.exists(out);
    boolean traceExists=java.nio.file.Files.exists(trace);
    log.info("[WINDOWS-UAC] Dateien nach Launcher: status={} output={} trace={} temp={}",statusExists,outputExists,traceExists,dir);

    String state=statusExists?java.nio.file.Files.readString(status,StandardCharsets.UTF_8).trim():"";
    String output=outputExists?java.nio.file.Files.readString(out,StandardCharsets.UTF_8):"";
    String traceText=traceExists?java.nio.file.Files.readString(trace,StandardCharsets.UTF_8):"";
    log.info("[WINDOWS-UAC] Trace: {}",traceText.isBlank()?"<leer>":traceText.replaceAll("[\r\n]+"," | "));

    if(!"OK".equalsIgnoreCase(state)){
      log.warn("[WINDOWS-UAC] Kein OK-Status. state='{}' outputChars={} launcherExit={}",state,output.length(),proc.exitValue());
      if(!output.isBlank()) throw new IOException(output.strip());
      if(!launcherOutput.isBlank()) throw new IOException(launcherOutput.strip());
      throw new IOException("Erhöhter PowerShell-Prozess lieferte keinen erfolgreichen Status.");
    }

    if(output.isBlank()){
      log.warn("[WINDOWS-UAC] Status OK, aber Ergebnisdatei leer. Temp={}",dir);
      throw new IOException("Erhöhter PowerShell-Prozess lieferte keine Inventardaten.");
    }

    log.info("[WINDOWS-UAC] Erhöhter Prozess erfolgreich. Ergebniszeichen={}",output.length());

    try{
      java.nio.file.Files.deleteIfExists(payload);
      java.nio.file.Files.deleteIfExists(wrapper);
      java.nio.file.Files.deleteIfExists(out);
      java.nio.file.Files.deleteIfExists(status);
      java.nio.file.Files.deleteIfExists(trace);
      java.nio.file.Files.deleteIfExists(dir);
    }catch(Exception cleanup){
      log.debug("[WINDOWS-UAC] Temp-Cleanup fehlgeschlagen: {}",cleanup.getMessage());
    }

    return Arrays.stream(output.split("\\R")).toList();
  }


  private static List<String> command(List<String> command, long timeoutSeconds) throws IOException, InterruptedException {
    Process process = new ProcessBuilder(command).redirectErrorStream(true).start();

    // 40k32b11: stdout sofort parallel leeren. Bei großen Ergebnissen (z. B. 244
    // Programmnamen) kann der Betriebssystem-Pipe-Puffer sonst volllaufen. PowerShell
    // blockiert dann beim Schreiben, während Java noch auf process.waitFor() wartet.
    // Der frühere Ablauf ließ deshalb praktisch nur den bereits gepufferten Anfang der
    // Softwareliste sichtbar werden – auf dem Testsystem reproduzierbar rund 20 Einträge.
    java.io.ByteArrayOutputStream outputBuffer = new java.io.ByteArrayOutputStream();
    java.util.concurrent.atomic.AtomicReference<IOException> readerFailure =
        new java.util.concurrent.atomic.AtomicReference<>();
    Thread outputReader = Thread.ofPlatform().daemon(true).name("gam-powershell-output-reader").start(() -> {
      try (java.io.InputStream input = process.getInputStream()) {
        input.transferTo(outputBuffer);
      } catch (IOException exception) {
        readerFailure.set(exception);
      }
    });

    if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
      process.destroyForcibly();
      outputReader.join(2000L);
      throw new IOException("Zeitüberschreitung nach " + timeoutSeconds + " Sekunden");
    }
    outputReader.join(5000L);
    if (outputReader.isAlive()) {
      process.destroyForcibly();
      throw new IOException("PowerShell-Ausgabe konnte nicht vollständig gelesen werden");
    }
    if (readerFailure.get() != null) {
      throw new IOException("PowerShell-Ausgabe konnte nicht gelesen werden", readerFailure.get());
    }

    String output = outputBuffer.toString(StandardCharsets.UTF_8);
    // Manche PowerShell-/CIM-Module liefern verwertbare GAMKV-Daten, setzen aber wegen
    // einer zusätzlichen nichtterminierenden Meldung trotzdem Exitcode 1. In diesem Fall
    // dürfen die bereits ermittelten Werte nicht als kompletter Modulfehler verworfen werden.
    if (process.exitValue() != 0 && !output.contains("GAMKV|")) {
      throw new IOException(output.isBlank() ? "PowerShell-Abfrage fehlgeschlagen" : output.strip());
    }
    return Arrays.stream(output.split("\\R")).toList();
  }

  private static boolean commandExists(String executable) {
    try {
      Process process = new ProcessBuilder("cmd.exe", "/c", "where " + executable).redirectErrorStream(true).start();
      return process.waitFor(2, TimeUnit.SECONDS) && process.exitValue() == 0;
    } catch (Exception ignored) { return false; }
  }

  private static String moduleLabel(String module) {
    return switch (module) {
      case "SYSTEM" -> "System- und Betriebssystemdaten";
      case "BIOS_BOARD" -> "BIOS- und Mainboarddaten";
      case "CPU_MEMORY" -> "Prozessor- und Arbeitsspeicherdaten";
      case "STORAGE" -> "Datenträger- und Laufwerksdaten";
      case "NETWORK" -> "Netzwerkdaten";
      case "SOFTWARE" -> "Software- und Windows-Feature-Daten";
      case "SECURITY" -> "Sicherheitsdaten";
      case "MAINTENANCE" -> "Wartungsdaten";
      case "UPDATES" -> "Windows-Update-Daten";
      default -> module;
    };
  }

  private static String value(Map<String, String> values, String key, String fallback) {
    String value = values.get(key);
    return value == null || value.isBlank() ? fallback : value.trim();
  }

  private static String cleanPowerShellRow(String row) {
    if (row == null) return null;
    String value = row.replace("\uFEFF", "").trim();
    if (value.isBlank() || value.startsWith("#< CLIXML") || value.startsWith("<Objs")
        || value.startsWith("</Objs") || value.startsWith("<Obj ") || value.startsWith("<TN")
        || value.startsWith("<MS") || value.startsWith("<PR ")) return null;
    int xmlStart = value.indexOf("<Objs ");
    if (xmlStart >= 0) value = value.substring(0, xmlStart).trim();
    int clixmlStart = value.indexOf("#< CLIXML");
    if (clixmlStart >= 0) value = value.substring(0, clixmlStart).trim();
    return value;
  }

  private static String numericWithUnit(String value, String unit) {
    if (value == null || value.isBlank()) return value;
    return value.matches("\\d+") ? value + " " + unit : value;
  }

  private static String slotSummary(String used, String total, String free) {
    if ((used == null || used.isBlank()) && (total == null || total.isBlank())) return null;
    String base = (used == null ? "?" : used) + " von " + (total == null ? "?" : total) + " belegt";
    return free == null || free.isBlank() ? base : base + " · " + free + " frei";
  }

  private static String suffix(String value, String prefix) {
    return value == null || value.isBlank() ? "" : prefix + value;
  }

  private static String normalizeMac(String value) { return value == null ? null : value.replace('-', ':').toUpperCase(Locale.ROOT); }
  private static String stableId(String computerName, String serial, String mac) {
    String value = serial != null ? serial : mac != null ? mac : computerName;
    return value.replaceAll("[^A-Za-z0-9._-]", "_").toLowerCase(Locale.ROOT);
  }
  private static String detail(String label, String value) { return value == null || value.isBlank() ? "" : " · " + label + ": " + value; }
  private static String countLabel(String value, String label) { return value == null || value.isBlank() ? "" : value + " " + label; }
  private static String countSeparator(String left, String right) { return left == null || left.isBlank() || right == null || right.isBlank() ? "" : " / "; }
  private static String join(String separator, String... values) { return Arrays.stream(values).filter(value -> value != null && !value.isBlank()).distinct().reduce((a, b) -> a + separator + b).orElse(null); }
  private static String localHostName() { try { return InetAddress.getLocalHost().getHostName(); } catch (Exception ignored) { return "Windows-PC"; } }
  private static boolean isWindows() { return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win"); }
  private static String safeMessage(Exception exception) { String value = exception.getMessage(); return value == null || value.isBlank() ? exception.getClass().getSimpleName() : value; }
  private static void diagnostic(Consumer<DeviceDiscoveryService.DiscoveryDiagnosticEvent> out, String sessionId, String phase, String status, String message, int count) {
    out.accept(new DeviceDiscoveryService.DiscoveryDiagnosticEvent(sessionId, OffsetDateTime.now().toString(), phase, status, message, count));
  }
}
