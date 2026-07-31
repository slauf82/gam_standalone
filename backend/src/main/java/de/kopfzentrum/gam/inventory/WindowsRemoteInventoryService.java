package de.kopfzentrum.gam.inventory;

import io.cloudsoft.winrm4j.client.WinRmClientContext;
import io.cloudsoft.winrm4j.winrm.WinRmTool;
import io.cloudsoft.winrm4j.winrm.WinRmToolResponse;
import org.apache.http.client.config.AuthSchemes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 40k34q: Remote-Windows-Inventarisierung über WinRM (Windows Remote Management) -
 * das von Microsoft bereitgestellte Standard-Fernverwaltungsprotokoll für Windows,
 * analog zu SSH bei Linux und ADB bei Android. Nutzt die etablierte, quelloffene
 * Bibliothek <a href="https://github.com/cloudsoft/winrm4j">winrm4j</a>
 * ({@code io.cloudsoft.windows:winrm4j}) - kein selbst entwickeltes WinRM-/SOAP-
 * Protokoll, keine proprietäre Eigenlösung.
 *
 * Architektur bewusst analog zu {@link LinuxNetworkDiscoveryService} gehalten:
 * EIN gemeinsam konfigurierter Zugang über dieselbe {@code setting()}-Konvention
 * (Property oder Umgebungsvariable, mit Fallback) wie bei SSH - keine zweite,
 * abweichende Zugangsdatenverwaltung. EIN einziges PowerShell-Sammelskript pro
 * Lauf statt vieler Einzelbefehle (Performance-Vorgabe: Remote-Aufrufe
 * minimieren). Rückgabeform {@code Optional<DiscoveredDevice>} - identisch zu
 * {@code LinuxNetworkDiscoveryService.inspectSingle()} - damit der zentrale
 * Plattform-Dispatcher ({@code DeviceIdentityService}) beide Plattformen mit
 * derselben, bereits bestehenden Übernahmelogik (recordDiscoveryHit()) behandeln
 * kann - keine Windows-Sonderbehandlung im Dispatcher.
 *
 * Architektur bewusst erweiterbar: {@link #inspectSingle} kapselt ausschließlich
 * die WinRM-spezifischen Details. Ein späteres zweites Remote-Verfahren (z.B.
 * eine reine CIM-über-HTTPS-Variante oder ein anderer Port) könnte als weitere
 * Methode dieser Klasse ergänzt werden, ohne den Dispatcher zu ändern.
 */
@Service
public class WindowsRemoteInventoryService {
  private static final Logger log = LoggerFactory.getLogger(WindowsRemoteInventoryService.class);
  private final WinRmSettingsRepository settings;

  public WindowsRemoteInventoryService(WinRmSettingsRepository settings) {
    this.settings = settings;
  }

  public record WinRmTestResult(boolean configured, boolean reachable, String message, String checkedAt) {}

  /**
   * 40k34q: EIN PowerShell-Sammelskript für den gesamten, im Auftrag geforderten
   * Umfang (Computername, Betriebssystem/Edition/Version/Build/Architektur,
   * Hersteller/Modell, BIOS, CPU, RAM, Laufwerke, Netzwerkkarten, installierte
   * Software, angemeldete Benutzer) - EIN Remote-Aufruf statt vieler Einzelabfragen.
   * Nutzt CIM (Get-CimInstance), den von Microsoft empfohlenen Nachfolger von WMI -
   * funktioniert über WinRM ohne zusätzliche DCOM-Firewallregeln.
   */
  private static final String INVENTORY_SCRIPT = """
    $ErrorActionPreference='SilentlyContinue'
    $os=Get-CimInstance Win32_OperatingSystem
    $cs=Get-CimInstance Win32_ComputerSystem
    $bios=Get-CimInstance Win32_BIOS
    $cpu=Get-CimInstance Win32_Processor | Select-Object -First 1
    $nic=Get-CimInstance Win32_NetworkAdapterConfiguration -Filter "IPEnabled=True" | Select-Object -First 1
    $disks=Get-CimInstance Win32_LogicalDisk -Filter "DriveType=3" | ForEach-Object { "$($_.DeviceID) $([math]::Round($_.Size/1GB))GB frei=$([math]::Round($_.FreeSpace/1GB))GB" }
    $nics=Get-CimInstance Win32_NetworkAdapterConfiguration -Filter "IPEnabled=True" | ForEach-Object { "$($_.Description) $($_.IPAddress -join ',')" }
    $sw=Get-ItemProperty 'HKLM:\\Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\*','HKLM:\\Software\\Wow6432Node\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\*' | Where-Object { $_.DisplayName } | Select-Object -ExpandProperty DisplayName -Unique
    $users=(query user 2>$null)
    "COMPUTERNAME=$($cs.Name)"
    "OS_CAPTION=$($os.Caption)"
    "OS_VERSION=$($os.Version)"
    "OS_BUILD=$($os.BuildNumber)"
    "ARCHITECTURE=$($os.OSArchitecture)"
    "MANUFACTURER=$($cs.Manufacturer)"
    "MODEL=$($cs.Model)"
    "SERIAL=$($bios.SerialNumber)"
    "BIOS=$($bios.Manufacturer) $($bios.SMBIOSBIOSVersion)"
    "CPU=$($cpu.Name)"
    "CPU_CORES=$($cpu.NumberOfCores)"
    "RAM_GB=$([math]::Round($cs.TotalPhysicalMemory/1GB))"
    "MAC=$($nic.MACAddress)"
    "DISKS=$($disks -join '; ')"
    "NICS=$($nics -join '; ')"
    "SOFTWARE_COUNT=$($sw.Count)"
    "SOFTWARE_SAMPLE=$(($sw | Select-Object -First 15) -join '; ')"
    "LOGGED_IN_USERS=$(($users) -join ' | ')"
    """;

  private boolean configured() {
    var s = settings.load();
    return s.enabled() && !s.username().isBlank();
  }

  private WinRmTool.Builder builderFor(String host, WinRmClientContext context) {
    var s = settings.load();
    WinRmTool.Builder builder = WinRmTool.Builder.builder(host, s.username(), s.password());
    builder.setAuthenticationScheme(AuthSchemes.NTLM);
    builder.port(s.port());
    builder.useHttps(s.https());
    if (s.https()) builder.disableCertificateChecks(true);
    builder.context(context);
    return builder;
  }

  /** 40k34q: reiner Erreichbarkeits-/Anmeldetest, analog zu LinuxNetworkDiscoveryService.testSshConnection(). */
  public WinRmTestResult testConnection(String host) {
    String now = OffsetDateTime.now().toString();
    if (!configured()) {
      log.debug("WinRM-Verbindungstest für {} übersprungen: kein gemeinsamer WinRM-Zugang konfiguriert", host);
      return new WinRmTestResult(false, false, "Keine aktive WinRM-Quelle mit Benutzername konfiguriert.", now);
    }
    WinRmClientContext context = WinRmClientContext.newInstance();
    try {
      WinRmTool tool = builderFor(host, context).build();
      tool.setOperationTimeout(5000L);
      WinRmToolResponse response = tool.executeCommand("hostname");
      boolean reachable = response.getStatusCode() == 0;
      log.debug("WinRM-Verbindungstest für {}: {}", host, reachable ? "erreichbar, Anmeldung erfolgreich" : "Status " + response.getStatusCode());
      return new WinRmTestResult(true, reachable,
        reachable ? "WinRM erreichbar, Anmeldung erfolgreich." : "WinRM antwortete mit Status " + response.getStatusCode() + ".", now);
    } catch (Exception e) {
      log.debug("WinRM-Verbindungstest für {} fehlgeschlagen: {}", host, e.getMessage());
      return new WinRmTestResult(true, false, "WinRM nicht erreichbar oder Anmeldung fehlgeschlagen: " + e.getMessage(), now);
    } finally {
      context.shutdown();
    }
  }

  /**
   * 40k34q: vollständige Remote-Inventarisierung über EIN PowerShell-Sammelskript.
   * Rückgabeform identisch zu LinuxNetworkDiscoveryService.inspectSingle() -
   * ermöglicht dieselbe Übernahmelogik (recordDiscoveryHit()) im Dispatcher.
   *
   * @param ip bekannte IPv4-Adresse des Zielgeräts
   * @param hostnameHint bereits bekannter Name, falls WinRM selbst keinen liefert
   */
  public Optional<DiscoveredDevice> inspectSingle(String ip, String hostnameHint) {
    if (!configured()) {
      log.debug("Windows-Ferninventarisierung für {} übersprungen: kein gemeinsamer WinRM-Zugang konfiguriert", ip);
      return Optional.empty();
    }
    WinRmClientContext context = WinRmClientContext.newInstance();
    try {
      WinRmTool tool = builderFor(ip, context).build();
      tool.setOperationTimeout(20000L);
      WinRmToolResponse response = tool.executePs(INVENTORY_SCRIPT);
      if (response.getStatusCode() != 0) {
        log.info("[WINDOWS-REMOTE] Inventarisierung für {} fehlgeschlagen (Status {}): {}", ip, response.getStatusCode(), trim(response.getStdErr()));
        return Optional.empty();
      }
      Map<String,String> data = parse(response.getStdOut());
      String computerName = data.getOrDefault("COMPUTERNAME", hostnameHint != null ? hostnameHint : ip);
      String protocol = "Windows-Ferninventarisierung (WinRM) / PowerShell-Sammelabfrage"
        + detail("Edition", data.get("OS_CAPTION")) + detail("Version", data.get("OS_VERSION"))
        + detail("Build", data.get("OS_BUILD")) + detail("Architektur", data.get("ARCHITECTURE"))
        + detail("Hersteller", data.get("MANUFACTURER")) + detail("Modell", data.get("MODEL"))
        + detail("BIOS", data.get("BIOS")) + detail("CPU", data.get("CPU"))
        + detail("CPU-Kerne", data.get("CPU_CORES")) + detail("Arbeitsspeicher", plausibleGb(data.get("RAM_GB")))
        + detail("Laufwerke", data.get("DISKS")) + detail("Netzwerkkarten", data.get("NICS"))
        + detail("Installierte Software (Anzahl)", data.get("SOFTWARE_COUNT"))
        + detail("Installierte Software (Beispiele)", data.get("SOFTWARE_SAMPLE"))
        + detail("Angemeldete Benutzer", data.get("LOGGED_IN_USERS"))
        + " · Windows-Ferninventarisierungsstatus: erfolgreich · Letzte Ferninventarisierung: " + OffsetDateTime.now();
      String mac = blankToNull(data.get("MAC"));
      String serial = blankToNull(data.get("SERIAL"));
      return Optional.of(new DiscoveredDevice(
        "windows-remote:" + ip, computerName, "Windows-PC", ip, mac, protocol, "ONLINE",
        blankToNull(data.get("MANUFACTURER")), serial, OffsetDateTime.now().toString(), false));
    } catch (Exception e) {
      log.info("[WINDOWS-REMOTE] Inventarisierung für {} fehlgeschlagen: {}", ip, e.getMessage());
      return Optional.empty();
    } finally {
      context.shutdown();
    }
  }

  private static Map<String,String> parse(String stdout) {
    Map<String,String> result = new LinkedHashMap<>();
    if (stdout == null) return result;
    for (String line : stdout.split("\\r?\\n")) {
      int eq = line.indexOf('=');
      if (eq <= 0) continue;
      String key = line.substring(0, eq).trim();
      String value = line.substring(eq + 1).trim();
      if (!value.isEmpty()) result.put(key, value);
    }
    return result;
  }

  private static String detail(String label, String value) {
    return (value == null || value.isBlank()) ? "" : " · " + label + ": " + value;
  }

  private static String plausibleGb(String raw) {
    if (raw == null || raw.isBlank()) return null;
    try { return Integer.parseInt(raw.trim()) > 0 ? raw.trim() + " GB" : null; } catch (Exception e) { return null; }
  }

  private static String blankToNull(String value) { return value == null || value.isBlank() ? null : value; }

  private static String trim(String value) {
    if (value == null) return "";
    return value.length() > 300 ? value.substring(0, 300) + "…" : value;
  }
}
