package de.kopfzentrum.gam.inventory;

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

    runModule(powershell, "SYSTEM", systemScript(), FAST_TIMEOUT_SECONDS, data, successfulModules, failedModules, diagnostics, sessionId, currentCount);
    runModule(powershell, "BIOS_BOARD", biosBoardScript(), FAST_TIMEOUT_SECONDS, data, successfulModules, failedModules, diagnostics, sessionId, currentCount);
    runModule(powershell, "CPU_MEMORY", cpuMemoryScript(), FAST_TIMEOUT_SECONDS, data, successfulModules, failedModules, diagnostics, sessionId, currentCount);
    runModule(powershell, "STORAGE", storageScript(), FAST_TIMEOUT_SECONDS, data, successfulModules, failedModules, diagnostics, sessionId, currentCount);
    runModule(powershell, "NETWORK", networkScript(), FAST_TIMEOUT_SECONDS, data, successfulModules, failedModules, diagnostics, sessionId, currentCount);
    runModule(powershell, "SOFTWARE", softwareScript(), SOFTWARE_TIMEOUT_SECONDS, data, successfulModules, failedModules, diagnostics, sessionId, currentCount);
    runModule(powershell, "SECURITY", securityScript(), SECURITY_TIMEOUT_SECONDS, data, successfulModules, failedModules, diagnostics, sessionId, currentCount);
    runModule(powershell, "MAINTENANCE", maintenanceScript(), FAST_TIMEOUT_SECONDS, data, successfulModules, failedModules, diagnostics, sessionId, currentCount);
    runModule(powershell, "UPDATES", updateScript(), UPDATE_TIMEOUT_SECONDS, data, successfulModules, failedModules, diagnostics, sessionId, currentCount);

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
    String ipv4 = value(data, "IPV4", null);
    String ipv6 = value(data, "IPV6", null);
    String mac = normalizeMac(value(data, "MAC", null));
    String adapters = value(data, "ADAPTERS", null);
    String softwareCount = value(data, "SOFTWARE_COUNT", null);
    String softwareRegistryCount = value(data, "SOFTWARE_REGISTRY_COUNT", null);
    String softwareSourceCounts = value(data, "SOFTWARE_SOURCE_COUNTS", null);
    String software = value(data, "SOFTWARE", null);
    String roles = value(data, "FEATURES", null);
    String antivirus = value(data, "ANTIVIRUS", null);
    String defenderStatus = value(data, "DEFENDER", null);
    String firewall = value(data, "FIREWALL", null);
    String tpm = value(data, "TPM", null);
    String secureBoot = value(data, "SECURE_BOOT", null);
    String bitLocker = value(data, "BITLOCKER", null);
    String pendingUpdatesRaw = value(data, "PENDING_UPDATES", failedModules.contains("UPDATES") ? "Zeitüberschreitung / nicht verfügbar" : null);
    String pendingUpdates = numericWithUnit(pendingUpdatesRaw, "Updates");
    String lastHotfix = value(data, "LAST_HOTFIX", null);
    String rebootPending = value(data, "REBOOT_PENDING", null);
    String uac = value(data, "UAC", null);
    String rdp = value(data, "RDP", null);
    String localAdmins = value(data, "LOCAL_ADMINS", null);

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
        + detail("Datenträger", disks) + detail("Laufwerke", volumes)
        + detail("IPv6", ipv6) + detail("Netzwerkadapter", adapters)
        + detail("Installierte Software", softwareCount == null ? software : softwareCount + " Programme"
            + (softwareRegistryCount == null || softwareRegistryCount.equals(softwareCount) ? "" : " (" + softwareRegistryCount + " Registry-Einträge)")
            + (software == null ? "" : " – " + software))
        + detail("Softwarequellen", softwareSourceCounts)
        + detail("Windows-Rollen/Features", roles) + detail("Antivirus", antivirus)
        + detail("Microsoft Defender", defenderStatus) + detail("Firewall", firewall)
        + detail("TPM", tpm) + detail("Secure Boot", secureBoot) + detail("BitLocker", bitLocker)
        + detail("Ausstehende Updates", pendingUpdates) + detail("Letzter Hotfix", lastHotfix)
        + detail("Neustart ausstehend", rebootPending) + detail("UAC", uac)
        + detail("Remote Desktop", rdp) + detail("Lokale Administratoren", localAdmins)
        + detail("Erfolgreiche Module", String.join(", ", successfulModules))
        + detail("Nicht verfügbare Module", failedModules.isEmpty() ? null : String.join(", ", failedModules));

    out.accept(new DiscoveredDevice(
        "windows-local:" + stableId(computerName, serial, mac), computerName, "Windows-PC", ipv4, mac,
        protocol, "ONLINE", displayManufacturer, serial, now, false));

    String result = caption + (build == null ? "" : " · Build " + build)
        + (processor == null ? "" : " · " + processor) + (ram == null ? "" : " · " + ram + " RAM")
        + " · Module " + successfulModules.size() + "/9";
    diagnostic(diagnostics, sessionId, "WINDOWS_INVENTORY", failedModules.isEmpty() ? "COMPLETED" : "PARTIAL",
        result + (failedModules.isEmpty() ? " erkannt" : " erkannt; nicht verfügbar: " + String.join(", ", failedModules)), currentCount + 1);
  }

  private static void runModule(
      String executable,
      String module,
      String script,
      long timeout,
      Map<String, String> target,
      List<String> successes,
      List<String> failures,
      Consumer<DeviceDiscoveryService.DiscoveryDiagnosticEvent> diagnostics,
      String sessionId,
      int currentCount) {
    String phase = "WINDOWS_INVENTORY_" + module;
    diagnostic(diagnostics, sessionId, phase, "RUNNING", moduleLabel(module) + " werden gelesen", currentCount);
    try {
      Map<String, String> result = runKeyValueModule(executable, script, timeout);
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

  private static Map<String, String> runKeyValueModule(String executable, String script, long timeoutSeconds) throws IOException, InterruptedException {
    String prefix = "$OutputEncoding=[Console]::OutputEncoding=[System.Text.UTF8Encoding]::new();"
        + "$ProgressPreference='SilentlyContinue';$InformationPreference='SilentlyContinue';"
        + "$VerbosePreference='SilentlyContinue';$DebugPreference='SilentlyContinue';";
    List<String> rows = command(powerShellCommand(executable, prefix + script), timeoutSeconds);
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
        function Emit($k,$v){if($null -ne $v -and ([string]$v).Trim() -ne ''){$clean=(([string]$v)-replace '[\r\n]',' ').Trim();$b64=[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($clean));[Console]::Out.WriteLine(('GAMKV|'+$k+'|'+$b64))}}
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
        function Emit($k,$v){if($null -ne $v -and ([string]$v).Trim() -ne ''){$clean=(([string]$v)-replace '[\r\n]',' ').Trim();$b64=[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($clean));[Console]::Out.WriteLine(('GAMKV|'+$k+'|'+$b64))}}
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
        function Emit($k,$v){if($null -ne $v -and ([string]$v).Trim() -ne ''){$clean=(([string]$v)-replace '[\r\n]',' ').Trim();$b64=[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($clean));[Console]::Out.WriteLine(('GAMKV|'+$k+'|'+$b64))}}
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
        $ErrorActionPreference='Stop'
        function Emit($k,$v){if($null -ne $v -and ([string]$v).Trim() -ne ''){$clean=(([string]$v)-replace '[\r\n]',' ').Trim();$b64=[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($clean));[Console]::Out.WriteLine(('GAMKV|'+$k+'|'+$b64))}}
        $disks=Get-CimInstance Win32_DiskDrive|ForEach-Object{$s=if($_.Size){[math]::Round($_.Size/1GB,1)}else{0};('{0} ({1} GB, {2})'-f $_.Model,$s,$_.MediaType)}
        $volumes=Get-CimInstance Win32_LogicalDisk -Filter 'DriveType=3'|ForEach-Object{$s=if($_.Size){[math]::Round($_.Size/1GB,1)}else{0};$f=if($_.FreeSpace){[math]::Round($_.FreeSpace/1GB,1)}else{0};('{0} {1}/{2} GB frei'-f $_.DeviceID,$f,$s)}
        Emit 'DISKS' ($disks -join '; ')
        Emit 'VOLUMES' ($volumes -join '; ')
        """;
  }

  private static String networkScript() {
    return """
        $ErrorActionPreference='Stop'
        function Emit($k,$v){if($null -ne $v -and ([string]$v).Trim() -ne ''){$clean=(([string]$v)-replace '[\r\n]',' ').Trim();$b64=[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($clean));[Console]::Out.WriteLine(('GAMKV|'+$k+'|'+$b64))}}
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
        $ErrorActionPreference='Stop'
        function Emit($k,$v){if($null -ne $v -and ([string]$v).Trim() -ne ''){$clean=(([string]$v)-replace '[\r\n]',' ').Trim();$b64=[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($clean));[Console]::Out.WriteLine(('GAMKV|'+$k+'|'+$b64))}}
        $locations=@(
          @{Name='HKLM 64-Bit';Path='HKLM:/Software/Microsoft/Windows/CurrentVersion/Uninstall/*'},
          @{Name='HKLM 32-Bit';Path='HKLM:/Software/WOW6432Node/Microsoft/Windows/CurrentVersion/Uninstall/*'},
          @{Name='HKCU 64-Bit';Path='HKCU:/Software/Microsoft/Windows/CurrentVersion/Uninstall/*'},
          @{Name='HKCU 32-Bit';Path='HKCU:/Software/WOW6432Node/Microsoft/Windows/CurrentVersion/Uninstall/*'}
        )
        $records=@()
        $sourceCounts=@()
        foreach($location in $locations){
          $items=@(Get-ItemProperty $location.Path -ErrorAction SilentlyContinue|Where-Object{$_.DisplayName -and ([string]$_.DisplayName).Trim()})
          $sourceCounts+=($location.Name+': '+$items.Count)
          foreach($item in $items){$records+=[PSCustomObject]@{DisplayName=([string]$item.DisplayName).Trim();Source=$location.Name}}
        }
        # Registry-Zweige enthalten häufig dieselbe Installation mehrfach. Für die
        # Administratoransicht werden deshalb eindeutige Anzeigenamen aufgelistet;
        # die Rohzahl bleibt separat nachvollziehbar.
        $names=@($records|ForEach-Object{$_.DisplayName}|Sort-Object -Unique)
        Emit 'SOFTWARE_REGISTRY_COUNT' $records.Count
        Emit 'SOFTWARE_COUNT' $names.Count
        Emit 'SOFTWARE_CAPTURED_COUNT' $names.Count
        Emit 'SOFTWARE_SOURCE_COUNTS' ($sourceCounts -join '; ')
        Emit 'SOFTWARE' ($names -join '; ')
        try{Emit 'FEATURES' ((Get-WindowsOptionalFeature -Online -ErrorAction Stop|Where-Object{$_.State -eq 'Enabled'}|Select-Object -ExpandProperty FeatureName)-join '; ')}catch{}
        """;
  }

  private static String securityScript() {
    return """
        $ErrorActionPreference='SilentlyContinue'
        function Emit($k,$v){if($null -ne $v -and ([string]$v).Trim() -ne ''){$clean=(([string]$v)-replace '[\r\n]',' ').Trim();$b64=[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($clean));[Console]::Out.WriteLine(('GAMKV|'+$k+'|'+$b64))}}
        try{Emit 'ANTIVIRUS' ((Get-CimInstance -Namespace root/SecurityCenter2 -ClassName AntivirusProduct -ErrorAction Stop|ForEach-Object{$_.displayName}|Select-Object -Unique)-join '; ')}catch{}
        try{$m=Get-MpComputerStatus -ErrorAction Stop;Emit 'DEFENDER' ('Echtzeitschutz '+$(if($m.RealTimeProtectionEnabled){'aktiv'}else{'aus'})+', Signatur '+$m.AntivirusSignatureVersion+', Alter '+$m.AntivirusSignatureAge+' Tage')}catch{}
        try{Emit 'FIREWALL' ((Get-NetFirewallProfile -ErrorAction Stop|ForEach-Object{$_.Name+': '+$(if($_.Enabled){'aktiv'}else{'aus'})})-join '; ')}catch{}
        try{$t=Get-Tpm -ErrorAction Stop;Emit 'TPM' ('vorhanden='+$t.TpmPresent+', bereit='+$t.TpmReady+', aktiviert='+$t.TpmEnabled)}catch{}
        try{Emit 'SECURE_BOOT' $(if(Confirm-SecureBootUEFI -ErrorAction Stop){'aktiv'}else{'aus'})}catch{Emit 'SECURE_BOOT' 'Nicht unterstützt / nicht verfügbar'}
        try{Emit 'BITLOCKER' ((Get-BitLockerVolume -ErrorAction Stop|ForEach-Object{$_.MountPoint+': '+$_.VolumeStatus+', Schutz '+$_.ProtectionStatus})-join '; ')}catch{}
        try{$v=(Get-ItemProperty 'HKLM:/SOFTWARE/Microsoft/Windows/CurrentVersion/Policies/System' -Name EnableLUA -ErrorAction Stop).EnableLUA;Emit 'UAC' $(if($v -eq 1){'aktiv'}else{'aus'})}catch{}
        try{$v=(Get-ItemProperty 'HKLM:/SYSTEM/CurrentControlSet/Control/Terminal Server' -Name fDenyTSConnections -ErrorAction Stop).fDenyTSConnections;Emit 'RDP' $(if($v -eq 0){'aktiviert'}else{'deaktiviert'})}catch{}
        try{Emit 'LOCAL_ADMINS' ((Get-LocalGroupMember -SID 'S-1-5-32-544' -ErrorAction Stop|ForEach-Object{$_.Name})-join '; ')}catch{}
        """;
  }

  private static String maintenanceScript() {
    return """
        $ErrorActionPreference='SilentlyContinue'
        function Emit($k,$v){if($null -ne $v -and ([string]$v).Trim() -ne ''){$clean=(([string]$v)-replace '[\r\n]',' ').Trim();$b64=[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($clean));[Console]::Out.WriteLine(('GAMKV|'+$k+'|'+$b64))}}
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

  private static List<String> powerShellCommand(String executable, String script) {
    String encoded = Base64.getEncoder().encodeToString(script.getBytes(StandardCharsets.UTF_16LE));
    return List.of(executable, "-NoLogo", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass", "-EncodedCommand", encoded);
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
