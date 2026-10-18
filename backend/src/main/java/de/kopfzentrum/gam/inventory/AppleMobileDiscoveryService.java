package de.kopfzentrum.gam.inventory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jna.Function;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.net.URI;
import java.net.NetworkInterface;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * 40k37a6: strikt Apple-gefilterte Basiserkennung mit physischem USB-Anker und Interface-Deduplizierung.
 *
 * Normalfall ist ein nicht-jailbroken Geraet. Wenn libimobiledevice vorhanden ist,
 * werden ueber usbmuxd/lockdown nur lesende Basisinformationen abgefragt. Fehlen
 * die Werkzeuge, wird auf Windows wenigstens ein physisch angeschlossenes Apple-
 * USB-Geraet als Hinweis erkannt; Pairing/Inventar werden dabei bewusst NICHT als
 * erfolgreich vorgetaeuscht.
 */
@Service
public class AppleMobileDiscoveryService {
  // e17: Der zweite parallele CoreDevice-Bootstrap bleibt logisch vorhanden, wird aber
  // waehrend eines aktiven Laufs deaktiviert. So kollidieren die zwei bekannten Discovery-Laeufe nicht.
  private static final java.util.concurrent.atomic.AtomicBoolean COREDEVICE_BOOTSTRAP_ACTIVE_E20 = new java.util.concurrent.atomic.AtomicBoolean(false);
  // 40k37e25-fix2: Ein GAM-Suchlauf darf den Apple-CoreDevice-Pfad nicht kurz danach
  // ein zweites Mal starten. Der zweite Status/Discovery-Aufruf hatte e24/e25 reproduzierbar
  // in den laufenden RemotePairing-/RSD-Zustand hineingegriffen. Backend-Neustart setzt den
  // Schutz bewusst zurueck; fuer einen manuellen Retest reicht daher ein Neustart.
  private static final java.util.concurrent.atomic.AtomicLong COREDEVICE_LAST_ATTEMPT_MS_E25 = new java.util.concurrent.atomic.AtomicLong(0L);
  private static final long COREDEVICE_DUPLICATE_SUPPRESS_MS_E25 = 5L * 60L * 1000L;
  private static final java.util.concurrent.atomic.AtomicReference<String> COREDEVICE_SELECTED_UDID_E25 = new java.util.concurrent.atomic.AtomicReference<>("");
  // 40k37e33: normale Basisdaten aus dem RemotePairing-Handshake; kein Developer Mode.
  private static final java.util.concurrent.atomic.AtomicReference<String> COREDEVICE_REMOTEPAIRING_BASIS_E27 = new java.util.concurrent.atomic.AtomicReference<>("");
  // 40k37e56c-fix6: Backend ist die Wahrheit fuer den letzten expliziten WLAN-Pairing-Versuch.
  private static final java.util.concurrent.atomic.AtomicReference<PairingResult> LAST_WIFI_PAIRING_E56C_FIX6 = new java.util.concurrent.atomic.AtomicReference<>();
  // 40k37e33: bestaetigter produktiver WLAN-Treffer bleibt fuer Folge-/Parallelaufrufe erhalten.
  private static final java.util.concurrent.atomic.AtomicReference<AppleDevice> COREDEVICE_CONFIRMED_WIFI_E30 = new java.util.concurrent.atomic.AtomicReference<>();
  // 40k37e33: echte IPv4-Adresse des bestaetigten CoreDevice-WLAN-Endpunkts.
  private static final java.util.concurrent.atomic.AtomicReference<String> COREDEVICE_CONFIRMED_WIFI_IP_E33 = new java.util.concurrent.atomic.AtomicReference<>();
  // 40k37e54-fix8: explizit per UAC gestarteter Admin-Tunnel. Nur der Tunnelprozess
  // laeuft erhoeht; GAM selbst bleibt im normalen Benutzerkontext.
  private static final java.util.concurrent.atomic.AtomicReference<String> ADMIN_RSD_HOST_E54F8 = new java.util.concurrent.atomic.AtomicReference<>("");
  private static final java.util.concurrent.atomic.AtomicReference<String> ADMIN_RSD_PORT_E54F8 = new java.util.concurrent.atomic.AtomicReference<>("");
  private static final java.util.concurrent.atomic.AtomicReference<String> ADMIN_RSD_UDID_E54F8 = new java.util.concurrent.atomic.AtomicReference<>("");
  private static final java.util.concurrent.atomic.AtomicLong ADMIN_RSD_PID_E54F8 = new java.util.concurrent.atomic.AtomicLong(0L);
  // 40k37e54-fix12: Backend-Guard gegen ueberlappende UAC-Startanforderungen.
  private static final java.util.concurrent.atomic.AtomicBoolean ADMIN_RSD_STARTING_E54F12 = new java.util.concurrent.atomic.AtomicBoolean(false);
  private static final Logger log = LoggerFactory.getLogger(AppleMobileDiscoveryService.class);
  private static final Object APPLE_TRACE_LOCK = new Object();
  private static volatile boolean appleTraceInitialized = false;
  private static final ThreadLocal<ApplePerformanceProfile> APPLE_PERFORMANCE = new ThreadLocal<>();

  private static final class ApplePerformanceProfile {
    final long startedMs = System.currentTimeMillis();
    final String startedAt = OffsetDateTime.now().toString();
    final String identityKey;
    String udid;
    final List<Map<String,Object>> phases = new ArrayList<>();
    final List<Map<String,Object>> toolCalls = new ArrayList<>();
    ApplePerformanceProfile(String identityKey){ this.identityKey=identityKey; }
  }

  // d8: negative optionale IORegistry-Capabilities pro Prozess merken.
  private static final Set<String> APPLE_OPTIONAL_PROBE_UNSUPPORTED = java.util.concurrent.ConcurrentHashMap.newKeySet();
  private final DiscoveryRegistrationRepository registrations;

  public AppleMobileDiscoveryService(DiscoveryRegistrationRepository registrations) {
    this.registrations = registrations;
    log.info("GAM BUILD | 40k37e56c-fix30 | AppleMobileDiscoveryService loaded | pairingRoute=PAIRING_ONLY");
  }

  /** 40k37c48: kompakter, eigenstaendiger Apple-Ablauftrace als Alternative zum sehr grossen Backend-Log. */
  private void appleTrace(String event, String details) {
    try {
      Path logDir = findGamRoot().resolve("logs");
      Files.createDirectories(logDir);
      Path trace = logDir.resolve("apple-mobile-debug.log");
      synchronized (APPLE_TRACE_LOCK) {
        if (!appleTraceInitialized) {
          Path previous = logDir.resolve("apple-mobile-debug.log.1");
          if (Files.exists(trace)) {
            try { Files.move(trace, previous, StandardCopyOption.REPLACE_EXISTING); }
            catch (Exception ignored) { }
          }
          Files.writeString(trace, "GAM Apple Mobile Debug 40k37e25\nstarted=" + OffsetDateTime.now() + "\n", StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
          appleTraceInitialized = true;
        }
        String safe = redactTraceDetails(details == null ? "" : details);
        Files.writeString(trace, OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) + " | " + event + (safe.isBlank()?"":" | "+safe) + "\n", StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
      }
    } catch (Exception ignored) {
      // Diagnoseprotokoll darf niemals den eigentlichen Apple-Ablauf beeinflussen.
    }
  }

  private void performancePhase(String name,long startedMs,boolean success,String details){
    ApplePerformanceProfile profile=APPLE_PERFORMANCE.get();
    if(profile==null) return;
    LinkedHashMap<String,Object> item=new LinkedHashMap<>();
    item.put("name",name); item.put("durationMs",Math.max(0,System.currentTimeMillis()-startedMs));
    item.put("success",success); if(details!=null&&!details.isBlank()) item.put("details",details);
    profile.phases.add(item);
  }

  private void performanceTool(String event,String command,int timeoutSeconds,long startedMs,CommandResult result){
    ApplePerformanceProfile profile=APPLE_PERFORMANCE.get();
    if(profile==null) return;
    LinkedHashMap<String,Object> item=new LinkedHashMap<>();
    item.put("event",event); item.put("command",command); item.put("timeoutSeconds",timeoutSeconds);
    item.put("durationMs",Math.max(0,System.currentTimeMillis()-startedMs)); item.put("success",result!=null&&result.ok());
    item.put("timeout",result!=null&&result.exitCode()==Integer.MIN_VALUE);
    if(result!=null) item.put("exitCode",formatExitCode(result.exitCode()));
    profile.toolCalls.add(item);
  }

  private InventoryResult finishInventoryE3(ApplePerformanceProfile profile,InventoryResult result){
    try {
      writePerformanceProfileE3(profile,result.success(),result.state());
      writeAppleRecoveryDiagnosticBundleE3(result);
    } finally { APPLE_PERFORMANCE.remove(); }
    return result;
  }

  private void writePerformanceProfileE3(ApplePerformanceProfile profile,boolean success,String state){
    if(profile==null) return;
    try{
      Path logDir=findGamRoot().resolve("logs"); Files.createDirectories(logDir);
      long totalMs=Math.max(0,System.currentTimeMillis()-profile.startedMs);
      LinkedHashMap<String,Object> payload=new LinkedHashMap<>();
      payload.put("generatedAt",OffsetDateTime.now().toString()); payload.put("build","40k37e25");
      payload.put("purpose","40k37e25: Apple identity/assignment hardening: prefer stable UDID/serial/WiFi-MAC matches, reject contradictory single-device fallback, keep USB/WLAN transport changes on the same registered identity, and retain d20 error hardening plus d19 fast path");
      payload.put("identityKey",profile.identityKey); payload.put("udid",profile.udid); payload.put("startedAt",profile.startedAt);
      payload.put("success",success); payload.put("state",state); payload.put("totalDurationMs",totalMs);
      payload.put("phases",profile.phases); payload.put("toolCalls",profile.toolCalls);
      Path json=logDir.resolve("apple-mobile-performance-e3.json");
      writeJsonAtomicE3(json,payload);

      StringBuilder text=new StringBuilder();
      text.append("Apple Performance Profil (40k37e25)\n");
      text.append("Start: ").append(profile.startedAt).append("\n");
      text.append("Gesamt: ").append(totalMs).append(" ms\n\nPhasen:\n");
      for(Map<String,Object> x:profile.phases) text.append(String.format(Locale.ROOT,"%-42s %8s ms  %s%n",String.valueOf(x.get("name")),String.valueOf(x.get("durationMs")),Boolean.TRUE.equals(x.get("success"))?"OK":"FEHLER"));
      text.append("\nWerkzeugaufrufe:\n");
      for(Map<String,Object> x:profile.toolCalls) text.append(String.format(Locale.ROOT,"%-52s %8s ms  %s%s%n",String.valueOf(x.get("event")),String.valueOf(x.get("durationMs")),Boolean.TRUE.equals(x.get("success"))?"OK":"FEHLER",Boolean.TRUE.equals(x.get("timeout"))?" / TIMEOUT":""));
      Files.writeString(logDir.resolve("apple-mobile-performance-e3.txt"),text.toString(),StandardCharsets.UTF_8,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING);
      appleTrace("E4 PERFORMANCE","totalMs="+totalMs+", phases="+profile.phases.size()+", toolCalls="+profile.toolCalls.size());
    }catch(Exception e){ appleTrace("E4 PERFORMANCE ERROR",e.getClass().getSimpleName()+": "+traceValue(e.getMessage())); }
  }


  /** 40k37e25: Diagnose-/Recovery-Härtung.
   * JSON-Snapshots werden erst in eine temporäre Datei geschrieben und danach atomar ersetzt,
   * damit ein Prozessabbruch keine halb geschriebenen Diagnose-/Cache-Dateien hinterlässt. */
  private void writeJsonAtomicE3(Path target,Object payload) throws Exception {
    Files.createDirectories(target.getParent());
    Path tmp=target.resolveSibling(target.getFileName().toString()+".tmp");
    try {
      new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(),payload);
      try {
        Files.move(tmp,target,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
      } catch(java.nio.file.AtomicMoveNotSupportedException e) {
        Files.move(tmp,target,StandardCopyOption.REPLACE_EXISTING);
      }
    } finally {
      try { Files.deleteIfExists(tmp); } catch(Exception ignored) { }
    }
  }

  /** e3: Ein kompakter, reproduzierbarer Apple-Diagnosebund pro Inventarlauf.
   * Nur bereits erzeugte GAM-Diagnosedateien werden aufgenommen; es werden keine zusätzlichen
   * Geräteabfragen gestartet und damit auch keine neue Laufzeit/Timeout-Kaskade erzeugt. */
  private void writeAppleRecoveryDiagnosticBundleE3(InventoryResult result){
    try{
      Path logDir=findGamRoot().resolve("logs"); Files.createDirectories(logDir);
      // Von einem harten Prozessabbruch übrig gebliebene e3-Tempdateien sind nie gültige Snapshots.
      try(var stream=Files.list(logDir)){
        stream.filter(p->p.getFileName().toString().startsWith("apple-mobile-") && p.getFileName().toString().endsWith(".tmp"))
          .forEach(p->{ try{Files.deleteIfExists(p);}catch(Exception ignored){} });
      }
      Path target=logDir.resolve("apple-mobile-diagnostic-e3.zip");
      Path tmp=logDir.resolve("apple-mobile-diagnostic-e3.zip.tmp");
      List<String> names=List.of(
        "apple-mobile-performance-e3.json","apple-mobile-performance-e3.txt",
        "apple-mobile-inventory-e3.json","apple-mobile-software-e3.json",
        "apple-mobile-installation-proxy-native-e3.json",
        "apple-mobile-installation-proxy-native-e3.zip",
        "apple-mobile-installation-proxy-cli-e3.json",
        "apple-mobile-installation-proxy-cli-e3.zip",
        "apple-mobile-debug.log","apple-mobile-debug.log.1"
      );
      try(java.util.zip.ZipOutputStream zout=new java.util.zip.ZipOutputStream(Files.newOutputStream(tmp,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING))){
        String manifest="GAM Apple Mobile Diagnose 40k37e25\ncreated="+OffsetDateTime.now()+"\nstate="+
          (result==null?"UNKNOWN":result.state())+"\nsuccess="+(result!=null&&result.success())+
          "\nHinweis=Bundle enthält nur bereits erzeugte Diagnoseartefakte; keine zusätzlichen Geräteabfragen.\n";
        zout.putNextEntry(new java.util.zip.ZipEntry("MANIFEST.txt"));
        zout.write(manifest.getBytes(StandardCharsets.UTF_8)); zout.closeEntry();
        for(String name:names){
          Path p=logDir.resolve(name);
          if(!Files.isRegularFile(p) || p.equals(target) || p.equals(tmp)) continue;
          zout.putNextEntry(new java.util.zip.ZipEntry(name));
          Files.copy(p,zout); zout.closeEntry();
        }
      }
      try { Files.move(tmp,target,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE); }
      catch(java.nio.file.AtomicMoveNotSupportedException e){ Files.move(tmp,target,StandardCopyOption.REPLACE_EXISTING); }
      appleTrace("E4 RECOVERY DIAGNOSTIC BUNDLE","archive="+target.getFileName()+", state="+(result==null?"UNKNOWN":result.state()));
    }catch(Exception e){
      appleTrace("E4 RECOVERY DIAGNOSTIC ERROR",e.getClass().getSimpleName()+": "+traceValue(e.getMessage()));
    }
  }

  private static String redactTraceDetails(String value) {
    if (value == null || value.isBlank()) return "";
    // Signierte CDN-Querystrings/Tokens nicht dauerhaft protokollieren.
    return value.replaceAll("(?i)([?&](?:sig|signature|token|auth|authorization|se|sp|sr|skn))=[^&\\s]+", "$1=<redacted>");
  }

  public record Status(boolean nativeToolsAvailable, boolean ideviceIdAvailable, boolean ideviceInfoAvailable,
                       boolean idevicePairAvailable, boolean appleDevicesInstalled, int connectedDeviceCount, int pairedDeviceCount,
                       String mechanism, String message, String checkedAt, List<AppleDevice> devices) {}

  public record TestResult(boolean success, String state, String message, String checkedAt,
                           String mechanism, int connectedDeviceCount, int pairedDeviceCount,
                           long durationMs, List<AppleDevice> devices) {}

  /** 40k37e54-fix8: Zustand des separat per UAC gestarteten privilegierten RSD-Tunnels. */
  public record AdminTunnelStatus(boolean active, String state, String message, String checkedAt,
                                  String udid, String host, int port, long pid) {}

  /** 40k37b: expliziter Trust/Pairing-Schritt; PnP-Sichtbarkeit allein zaehlt nie als Trust. */
  public record PairingResult(boolean success, String state, String message, String checkedAt,
                              String udid, boolean paired, long durationMs) {}

  /** 40k37e25: normale Inventarisierung plus Lockdown-, Health-/Storage- und App-Erkundung. */
  public record InventoryResult(boolean success, String state, String message, String checkedAt,
                                String identityKey, AppleDevice device, int fieldCount, long durationMs) {}

  /** 40k37d1: lesender Diagnose-/Erkundungslauf. Keine Daten werden veraendert. */
  public record InventoryProbeResult(boolean success, String state, String message, String checkedAt,
                                     String udid, int defaultFieldCount, Map<String,String> defaultFields,
                                     Map<String,Map<String,String>> domains, long durationMs) {}

  /** 40k37e25: maximaler, rein lesender Diagnose-/Health-Probelauf ueber diagnostics_relay/IORegistry. */
  public record ExtendedInventoryProbeResult(boolean success, String state, String message, String checkedAt,
                                             String udid, boolean diagnosticsToolAvailable,
                                             Map<String,Map<String,String>> sections, int scalarFieldCount, long durationMs) {}

  /** 40k37c3: Installation der portablen Windows-Toolchain direkt in GAM. */
  public record ToolchainInstallResult(boolean success, String state, String message, String checkedAt,
                                       String version, String targetDirectory, boolean checksumVerified, long durationMs) {}

  /** 40k37c45: geführte Online-/Offline-Bereitstellung der Apple-Windows-Unterstützung. */
  public record AppleDevicesInstallResult(boolean success, String state, String message, String checkedAt,
                                          boolean installed, boolean wingetAvailable, boolean storeOpened, long durationMs) {}
  public record AppleOfflineDepotStatus(String directory, boolean reachable, int packageCount, boolean applePackagePresent,
                                        boolean toolchainPresent, String message, String checkedAt) {}
  public record AppleOfflineDepotResult(boolean success, String state, String message, String checkedAt,
                                        String directory, int packageCount, boolean toolchainPresent, long durationMs) {}
  /** 40k37c45: reine Online-Metadatenpruefung. Sie veraendert das lokale Depot niemals. */
  public record AppleDepotUpdateStatus(boolean success, String state, String message, String checkedAt,
                                       boolean updateAvailable, String localMainPackage, String onlineMainPackage,
                                       List<String> onlinePackages, List<String> missingOrChangedPackages) {}

  private static final String APPLE_DEVICES_STORE_ID = "9NP83LWLPZ9K";
  private static final String RG_ADGUARD_ENDPOINT = "https://store.rg-adguard.net/api/GetFiles";
  private static final String APPX4FUN_APPLE_DEVICES_PAGE = "https://www.appx4fun.com/xap/56771/";
  private static final String MICROSOFT_PACKAGE_RETRIEVAL_ENDPOINT = "https://sfdataservice.microsoft.com/package-retrieval/Packages";

  // 40k37c45: nur fuer passive Vergleichsdiagnose zweier Microsoft-Bridge-Abrufe; niemals ausgefuehrt.
  private byte[] previousMicrosoftBridgeBinary;
  private String previousMicrosoftBridgeSha256;

  private static final String TOOLCHAIN_TAG = "v20260802-74585f8"; // 40k37c4: existierender, verifizierter Windows-Binary-Release
  private static final String TOOLCHAIN_RELEASE_API = "https://api.github.com/repos/jrjr/libimobiledevice-windows/releases/tags/" + TOOLCHAIN_TAG;
  private static final String IDEVICEINSTALLER_TAG = "1.2.0";
  private static final String IDEVICEINSTALLER_RELEASE_API = "https://api.github.com/repos/libimobiledevice/ideviceinstaller/releases/tags/" + IDEVICEINSTALLER_TAG;
  // 40k37e25: ideviceinstaller wird als isolierte, vollstaendige MSYS2-UCRT64-Runtime bereitgestellt.
  // Der Einstiegspunkt selbst bleibt hart gepinnt; alle Runtime-Abhaengigkeiten werden aus der
  // offiziellen UCRT64-Repository-Datenbank rekursiv aufgeloest und gegen die dort publizierten
  // SHA-256-Pruefsummen validiert. d8 nutzt mehrere von MSYS2 gelistete Mirrors plus persistenten
  // GAM-Paketcache, damit ein einzelner HTTP-5xx/Timeout einen Lauf nicht mehr stoppt.
  private static final List<String> MSYS2_UCRT_REPO_BASES = List.of(
      "https://mirror.msys2.org/mingw/ucrt64/",
      "https://repo.msys2.org/mingw/ucrt64/",
      "https://ftp2.osuosl.org/pub/msys2/mingw/ucrt64/",
      "https://mirror.yandex.ru/mirrors/msys2/mingw/ucrt64/",
      "https://mirror.accum.se/mirror/msys2.org/mingw/ucrt64/"
  );
  private static final String MSYS2_IDEVICEINSTALLER_PACKAGE = "mingw-w64-ucrt-x86_64-ideviceinstaller";
  private static final String MSYS2_IDEVICEINSTALLER_PINNED_VERSION = "1.1.1-2";
  private static final String MSYS2_IDEVICEINSTALLER_UCRT_SHA256 = "9316914c76a8cdfb33ac8d1e8bf63dfbe5db9a1badbe035459abc6365e4bb509";
  // 40k37e25: isolierter A/B-Test gegen eine aktuelle Windows-Suite. Die d10-/MSYS2-1.1.1-Runtime
  // bleibt bewusst unangetastet. Der Drittanbieter-Build ist nur ein Diagnosepfad und wird vor Nutzung
  // gegen den auf der GitHub-Release-Seite publizierten SHA-256 geprüft. Zusätzlich muss die enthaltene
  // ideviceinstaller-CLI zur modernen 1.2.x-Syntax passen; andernfalls wird sie nicht verwendet.
  private static final String D11_MODERN_SUITE_URL = "https://github.com/L1ghtmann/libimobiledevice/releases/download/suite-exe-12052025/libimobile-suite-latest_x86_64-mingw64.tar.xz";
  private static final String D11_MODERN_SUITE_SHA256 = "c906b134dd4b7981eb201af06ebad72d131b846eaf0acbe9edda509436cf8512";
  private static final String D11_MODERN_SUITE_LABEL = "L1ghtmann suite-exe-12052025 / upstream 2025-12-04";
  private final java.util.concurrent.ConcurrentHashMap<String,List<SoftwareItem>> latestSoftwareInventory = new java.util.concurrent.ConcurrentHashMap<>();

  public record AppleDevice(String udid, String name, String platform, String productType, String modelDisplayName,
                            String productVersion, String buildVersion, String serialNumber,
                            String modelNumber, String hardwareModel, String cpuArchitecture,
                            String wifiAddress, String bluetoothAddress, String timeZone,
                            boolean paired, String pairingState, String transport) {}

  /** 40k37a1: physisch sichtbares Apple-USB-Gerät aus Windows-PnP. */
  private record WindowsUsbDevice(String instanceId, String name, String platform, String manufacturer, String status,
                                  String containerId, String parentId, String productType, String modelDisplayName,
                                  String evidence) {}

  /** 40k37e55c: physisch sichtbares Apple-USB-Gerät unter Linux, unabhängig von Lockdown/usbmuxd. */
  private record LinuxAppleUsbDevice(String bus, String device, String productId, String description) {}

  /** 40k37e36: reiner Capability-Status ohne Geraeteabfrage.
   * DeviceDiscoveryService.capabilities() darf keinen zweiten Apple-Discovery-Lauf
   * mehr ausloesen. Die echte USB/WLAN-Abfrage findet ausschliesslich in scan()/status() statt. */
  public Status capabilityStatus() {
    boolean id = commandExists("idevice_id");
    boolean info = commandExists("ideviceinfo");
    boolean pair = commandExists("idevicepair");
    String hostOs=System.getProperty("os.name","").toLowerCase(Locale.ROOT);
    boolean linux=hostOs.contains("linux");
    boolean mac=hostOs.contains("mac");
    boolean macSupportReady=false;
    if(mac && id && info && pair) {
      try {
        Path rt=findGamRoot().toAbsolutePath().normalize().resolve("tools").resolve("apple-coredevice").resolve("runtime");
        Path pmd=resolveCoreDevicePmd3E55h(rt);
        macSupportReady=pmd!=null && Files.isRegularFile(pmd);
      } catch(Exception ignored) {}
    }
    boolean appleDevices = linux ? (id && info && run(4,"sh","-lc","command -v usbmuxd >/dev/null 2>&1").ok())
      : (mac ? macSupportReady : windowsAppleDevicesInstalled());
    String mechanism = id && info ? (linux ? "Linux System-Toolchain / libimobiledevice / usbmuxd" : "libimobiledevice / USB + CoreDevice-WLAN verfuegbar")
      : (appleDevices ? "Windows Apple Devices vorhanden; native Apple-Mobile-Werkzeuge unvollstaendig"
      : (linux ? "Linux Apple-Mobile-Systempakete unvollstaendig" : "Apple-Mobile-Werkzeuge nicht verfuegbar"));
    String message = "40k37e36 Capability-Status: keine Geraeteabfrage ausgefuehrt; Apple-Discovery erfolgt einmalig im Suchlauf.";
    log.info("[DISCOVERY][40k37e36][APPLE_MOBILE][CAPABILITY_ONLY] idevice_id={}, ideviceinfo={}, idevicepair={}, appleDevices={}, deviceProbe=false", id, info, pair, appleDevices);
    return new Status(id && info, id, info, pair, appleDevices, 0, 0, mechanism, message, OffsetDateTime.now().toString(), List.of());
  }

  public Status status() { return status(true,true); }

  /** 40k37e52: USB und WLAN sind gleichwertige, unabhaengig schaltbare Apple-Transportwege. */
  public Status status(boolean usbEnabled, boolean wlanEnabled) {
    logFix11AppleEntry("STATUS_ENTER", usbEnabled, wlanEnabled);
    boolean id = commandExists("idevice_id");
    boolean info = commandExists("ideviceinfo");
    boolean pair = commandExists("idevicepair");
    String hostOs=System.getProperty("os.name","").toLowerCase(Locale.ROOT);
    boolean linux=hostOs.contains("linux");
    boolean mac=hostOs.contains("mac");
    boolean macSupportReady=false;
    if(mac && id && info && pair) {
      try {
        Path rt=findGamRoot().toAbsolutePath().normalize().resolve("tools").resolve("apple-coredevice").resolve("runtime");
        Path pmd=resolveCoreDevicePmd3E55h(rt);
        macSupportReady=pmd!=null && Files.isRegularFile(pmd);
      } catch(Exception ignored) {}
    }
    boolean appleDevices = linux ? (id && info && run(4,"sh","-lc","command -v usbmuxd >/dev/null 2>&1").ok())
      : (mac ? macSupportReady : windowsAppleDevicesInstalled());
    if (!linux && appleDevices && id) ensureAppleMobileRuntimeReady("status");
    List<AppleDevice> usbDevices = List.of();
    List<AppleDevice> networkDevices = List.of();
    if (id && info) {
      if (usbEnabled) {
      log.info("[DISCOVERY][40k37e52][APPLE_MOBILE][USB] ROUTE_ENABLED=true");
      log.info("[DISCOVERY][40k37e36][APPLE_MOBILE][USB][PRESENCE_CHECK] START");
      try {
        CommandResult usbPresence = runTool(8, "idevice_id", "-l");
        List<String> usbUdids = usbPresence.ok() ? parseUdidLines(usbPresence.stdout()) : List.of();
        log.info("[DISCOVERY][40k37e36][APPLE_MOBILE][USB][PRESENCE_CHECK] ok={}, devices={}", usbPresence.ok(), usbUdids.size());
        if (linux && usbUdids.isEmpty()) {
          // e55g: Ausschliesslich die unterste Linux-Schicht reparieren. Wenn Linux das
          // Apple-USB-Geraet physisch sieht, usbmuxd aber noch keinen Socket/Prozess hat,
          // darf GAM den bereits installierten Systemdienst initialisieren. Es wird kein
          // synthetisches Apple-Geraet erzeugt; danach wird derselbe eingefrorene e54-
          // PRESENCE_CHECK (idevice_id -l) lediglich einmal erneut ausgefuehrt.
          List<LinuxAppleUsbDevice> physical = readLinuxAppleUsbDevices();
          boolean physicalApple = !physical.isEmpty();
          if (physicalApple && ensureLinuxUsbmuxdRuntimeE55g()) {
            usbPresence = runTool(8, "idevice_id", "-l");
            usbUdids = usbPresence.ok() ? parseUdidLines(usbPresence.stdout()) : List.of();
            log.info("[DISCOVERY][40k37e55g][APPLE_MOBILE][USB][PRESENCE_RETRY] ok={}, devices={}", usbPresence.ok(), usbUdids.size());
          }
          CommandResult mux = run(4,"sh","-lc","test -S /run/usbmuxd -o -S /var/run/usbmuxd");
          CommandResult proc = run(4,"sh","-lc","pgrep -x usbmuxd >/dev/null 2>&1");
          String cause = !physicalApple ? "NO_PHYSICAL_APPLE_USB_DEVICE"
              : (!(mux.ok() || proc.ok()) ? "USBMUXD_NOT_ACTIVE_OR_SOCKET_MISSING"
              : (usbPresence.ok() ? "DEVICE_NOT_EXPOSED_TO_USBMUXD" : "IDEVICE_ID_FAILED"));
          log.info("[DISCOVERY][40k37e55f][APPLE_MOBILE][USB][LINUX_REACHABILITY] cause={}, physicalApple={}, usbmuxdSocket={}, usbmuxdProcess={}, ideviceExit={}, ideviceError={}",
              cause, physicalApple, mux.ok(), proc.ok(), usbPresence.exitCode(), firstNonBlank(usbPresence.stderr(),"-").replaceAll("\\s+"," ").trim());
        }
        if (usbUdids.isEmpty()) {
          log.info("[DISCOVERY][40k37e36][APPLE_MOBILE][USB][FAST_SKIP] reason=NO_NATIVE_APPLE_USB_DEVICE; action=CONTINUE_WLAN_ONLY");
        } else {
          log.info("[DISCOVERY][40k37e36][APPLE_MOBILE][USB] START_FULL; devices={}", usbUdids.size());
          usbDevices = readNativeDevices(pair, usbUdids);
          log.info("[DISCOVERY][40k37e36][APPLE_MOBILE][USB] RESULT: devices={}", usbDevices.size());
          // 40k37e51-fix5: Ein physischer USB-Lauf erzeugt sofort eine persistente,
          // vollstaendige Referenz. status()/scan() ist der reale Discovery-Pfad; fix3
          // schrieb die Referenz irrtuemlich nur aus inventory(), das hier nicht laeuft.
          for (AppleDevice usbDevice : usbDevices) {
            persistFullUsbReferenceE51Fix4(usbDevice);
          }
        }
      } catch (Exception ex) {
        log.warn("[DISCOVERY][40k37e36][APPLE_MOBILE][USB] FEHLER: {}", ex.toString(), ex);
      }
      } else {
        log.info("[DISCOVERY][40k37e52][APPLE_MOBILE][USB] ROUTE_ENABLED=false; action=SKIP_USB_COMPLETELY");
      }
      // WLAN ist absichtlich unabhängig vom USB-Ergebnis. Ein Fehler im USB-Zweig darf
      // die Netzwerk-Erkennung nicht mehr überspringen.
      if (wlanEnabled) {
        log.info("[DISCOVERY][40k37e52][APPLE_MOBILE][WLAN] ROUTE_ENABLED=true");
        log.info("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN] START");
        try {
          networkDevices = readNativeNetworkDevices();
          log.info("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN] RESULT: devices={}", networkDevices.size());
        } catch (Exception ex) {
          log.warn("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN] FEHLER: {}", ex.toString(), ex);
        }
      } else {
        log.info("[DISCOVERY][40k37e52][APPLE_MOBILE][WLAN] ROUTE_ENABLED=false; action=SKIP_WLAN_COMPLETELY");
      }
    } else {
      log.info("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE] Native USB/WLAN-Abfrage übersprungen: idevice_id={}, ideviceinfo={}", id, info);
    }
    // 40k37e25: USB und WLAN bleiben bewusst getrennte Discovery-Treffer.
    // Die allgemeine GAM-Identitäts-/Merge-Pipeline darf sie wie bei anderen Plattformen
    // anhand stabiler Übereinstimmungen zu einem Gerät mit mehreren Quellen zusammenführen.
    List<AppleDevice> devices = new ArrayList<>(usbDevices.size()+networkDevices.size());
    devices.addAll(usbDevices);
    devices.addAll(networkDevices);
    List<WindowsUsbDevice> windowsUsbDevices = usbEnabled ? readWindowsUsbDevices() : List.of();
    List<LinuxAppleUsbDevice> linuxUsbDevices = (linux && usbEnabled) ? readLinuxAppleUsbDevices() : List.of();
    int paired = (int) devices.stream().filter(AppleDevice::paired).count();
    int connected = !devices.isEmpty() ? devices.size() : windowsUsbDevices.size(); // e55d: lsusb-Hinweis ist kein Apple-Mobile-Treffer
    boolean windowsUsbHint = !windowsUsbDevices.isEmpty();
    boolean linuxUsbHint = !linuxUsbDevices.isEmpty();
    String mechanism;
    if (id && info && !devices.isEmpty()) mechanism = networkDevices.isEmpty()
      ? "libimobiledevice / usbmuxd / lockdown"
      : (usbDevices.isEmpty() ? "libimobiledevice / Netzwerk / lockdown" : "libimobiledevice / USB + Netzwerk / lockdown");
    else if (id && info && windowsUsbHint) mechanism = "libimobiledevice + Windows PnP USB-Fallback (Lockdown noch nicht verfügbar)";
    else if (windowsUsbHint) mechanism = "Windows PnP Apple-Mobile-USB-Erkennung (native Pairing-Werkzeuge fehlen)";
    else if (id && info && linuxUsbHint) mechanism = "Linux USB / Apple 05ac + libimobiledevice/usbmuxd (Lockdown noch nicht verfügbar)";
    else if (linuxUsbHint) mechanism = "Linux USB / Apple 05ac (libimobiledevice/usbmuxd noch nicht verfügbar)";
    else mechanism = id && info ? "libimobiledevice / usbmuxd / lockdown" : "Apple-Mobile-Werkzeuge nicht verfuegbar";

    String message;
    if (!devices.isEmpty()) {
      message = devices.size()+" Apple-Mobile-Treffer nativ erkannt, davon "+paired+" mit bestaetigtem Pairing/Trust; USB="+usbDevices.size()+", Netzwerk="+networkDevices.size()+".";
    } else if (windowsUsbHint) {
      message = windowsUsbDevices.size()+" Apple-Mobilgeraet(e) physisch per USB unter Windows erkannt. Das Geraet wird im Verzeichnis angezeigt; fuer UDID, Trust/Pairing und tiefere Basisdaten werden die Apple-Mobile-Werkzeuge benoetigt.";
    } else if (linuxUsbHint) {
      CommandResult mux=run(4,"sh","-lc","pgrep -x usbmuxd >/dev/null 2>&1 || systemctl is-active --quiet usbmuxd.service 2>/dev/null");
      CommandResult sock=run(4,"sh","-lc","test -S /var/run/usbmuxd -o -S /run/usbmuxd");
      String layer=!mux.ok()?"usbmuxd läuft nicht":(!sock.ok()?"usbmuxd läuft, aber sein Socket ist nicht verfügbar":"usbmuxd/Socket sind verfügbar, aber idevice_id erhält noch keine UDID (USB-/udev-Zugriff oder Trust/Pairing noch nicht hergestellt)");
      message = "Linux erkennt "+linuxUsbDevices.size()+" Apple-USB-Geraet(e) physisch. "+layer+". Lockdown ist deshalb noch nicht erreichbar; die Apple-Inventarisierung wurde nicht ausgeführt.";
    } else if (id && info) {
      message = "Native Apple-Mobile-Werkzeuge sind verfuegbar, aktuell wurde aber kein iPhone/iPad ueber usbmuxd oder Windows-PnP erkannt.";
    } else {
      message = System.getProperty("os.name","").toLowerCase(Locale.ROOT).contains("linux")
        ? "libimobiledevice/usbmuxd sind auf diesem Linux-GAM-Host noch nicht vollstaendig verfuegbar. Die Bereitstellung erfolgt ueber den erkannten System-Paketmanager."
        : "libimobiledevice ist auf diesem GAM-Host noch nicht verfuegbar und Windows meldet aktuell kein angeschlossenes Apple-Mobilgeraet.";
    }
    return new Status(id && info, id, info, pair, appleDevices, connected, paired, mechanism, message, OffsetDateTime.now().toString(), devices);
  }

  public TestResult testConnection() {
    log.warn("FIX33 TESTCONNECTION INTERCEPT | action=PAIRING_ONLY");
    return testPairingConnectionFix33();
  }

  /** fix33: hard routing bridge. Both the historical /test endpoint and any direct
   * testConnection() caller enter the existing pairing-only implementation here. */
  public TestResult testPairingConnectionFix33() {
    log.warn("FIX33 PAIRING BRIDGE | next=testPairingConnectionFix30 | pairingOnly=true");
    appleTrace("FIX33 PAIRING BRIDGE", "next=testPairingConnectionFix30, pairingOnly=true");
    return testPairingConnectionFix30();
  }

  /** fix30: Dedicated endpoint path for Apple-Gerät prüfen. This deliberately bypasses
   * the old status()/inventory/network test path and executes pairing only. */
  public TestResult testPairingConnectionFix30() {
    long started = System.currentTimeMillis();
    log.info("FIX30 ACTIVE PAIRING ENDPOINT | action=testPairingConnectionFix30 | pairingOnly=true");
    appleTrace("FIX30 ACTIVE PAIRING ENDPOINT", "action=testPairingConnectionFix30, pairingOnly=true");
    appleTrace("ENTER pairingTest", "action=testPairingConnectionFix30, fix30=PAIRING_ONLY");
    tracePairingEnvironment("testConnection");

    // fix28: "Apple-Geraet pruefen" ist die Pairing-Aktion. Sie darf weder die
    // Inventarisierung noch die allgemeine WLAN-Suche ausloesen. pair() erledigt
    // ausschliesslich USB-Erkennung -> Lockdown-Trust/Pairing -> RemotePairing ueber
    // dasselbe USB-Kabel. USERSPACE_RSD wird danach separat von Suche/Inventarisierung genutzt.
    PairingResult r=pair(null);
    int connected=(r.udid()!=null && !r.udid().isBlank())?1:0;
    int paired=r.paired()?1:0;
    String mechanism=r.success()
      ? "libimobiledevice / usbmuxd / lockdown + CoreDevice RemotePairing"
      : "libimobiledevice / usbmuxd / lockdown";
    String state=r.state();
    // Die bestehende UI kennt TRUST_REQUIRED; der aktive pair()-Pfad liefert
    // TRUST_CONFIRM_REQUIRED. Fuer den Teststatus bleibt die praezisere Form erhalten.
    appleTrace("EXIT pairingTest", "success="+r.success()+", state="+state+", mechanism="+mechanism+", connected="+connected+", paired="+paired+", fix30=PAIRING_ONLY");
    return new TestResult(r.success(),state,r.message(),r.checkedAt(),mechanism,connected,paired,
      System.currentTimeMillis()-started,List.of());
  }

  public PairingResult pair(String requestedUdid) {
    log.warn("FIX32 SERVICE PAIR ENTRY | requestedUdidPresent={} | codeSource={}", requestedUdid != null && !requestedUdid.isBlank(), AppleMobileDiscoveryService.class.getProtectionDomain().getCodeSource().getLocation());
    long started=System.currentTimeMillis();
    log.warn("[DISCOVERY][40k37e56c-fix11][APPLE_MOBILE][PAIR_SERVICE] event=ENTER requestedUdidPresent={} os={} thread={}", requestedUdid!=null && !requestedUdid.isBlank(), System.getProperty("os.name",""), Thread.currentThread().getName());
    logFix11AppleEntry("PAIR_SERVICE_ENTER", true, true);
    appleTrace("ENTER pair", "requestedUdid="+traceUdid(requestedUdid));
    tracePairingEnvironment("pair-start");
    String idTool=resolveTool("idevice_id"), pairTool=resolveTool("idevicepair"), infoTool=resolveTool("ideviceinfo");
    appleTrace("PAIR TOOLS", "idevice_id="+traceValue(idTool)+", idevicepair="+traceValue(pairTool)+", ideviceinfo="+traceValue(infoTool));
    if (idTool==null || pairTool==null) {
      appleTrace("EXIT pair", "state=TOOLS_MISSING");
      return new PairingResult(false,"TOOLS_MISSING",
        "Das iPad ist per USB sichtbar, aber GAM kann Trust/Pairing noch nicht ueber Lockdown pruefen: idevice_id/idevicepair fehlen. Die aktuelle Apple-Geräte-App bleibt der Windows-Treiberweg; iTunes ist nicht erforderlich.",
        OffsetDateTime.now().toString(), blankToNull(requestedUdid), false, System.currentTimeMillis()-started);
    }
    List<String> ids=connectedUdidsWithRetry("pair", 5, 850);
    if (ids.isEmpty()) {
      List<WindowsUsbDevice> pnpDevices=readWindowsUsbDevices();
      List<LinuxAppleUsbDevice> linuxUsbDevices=readLinuxAppleUsbDevices();
      tracePairingEnvironment("pair-no-lockdown-before-bootstrap");
      appleTrace("PAIR STEP 2", "idevice_id returned 0 devices, windowsPnP="+pnpDevices.size()+", linuxAppleUsb="+linuxUsbDevices.size());

      // 40k37c47: Wenn Windows genau ein Apple-USB-Gerät sieht, idevice_id/usbmuxd aber
      // noch keine UDID liefert, darf der Workflow nicht vor dem eigentlichen Pairing-Aufruf
      // abbrechen. idevicepair kann bei genau einem angeschlossenen Gerät ohne -u gestartet
      // werden und damit den Lockdown-Trust-Handshake initialisieren. Danach wird idevice_id
      // erneut geprüft. So erreicht die Pairing-Anforderung das iPad auch in diesem
      // Bootstrap-Zustand.
      if (pnpDevices.size()==1 || linuxUsbDevices.size()==1) {
        appleTrace("PAIR STEP 3", "bootstrap pair without UDID because exactly one physical Apple USB device is present; windowsPnP="+pnpDevices.size()+", linuxAppleUsb="+linuxUsbDevices.size());
        CommandResult bootstrap=runToolTraced(35,"PAIR REQUEST BOOTSTRAP","idevicepair","pair");
        appleTrace("PAIR STEP 4", "bootstrap pair finished, ok="+bootstrap.ok()+", stdout="+traceValue(limitTrace(bootstrap.stdout()))+", stderr="+traceValue(limitTrace(bootstrap.stderr())));
        ids=connectedUdidsWithRetry("pair-after-bootstrap", 10, 1000);
        if(ids.isEmpty()) {
          String combined=(bootstrap.stdout()+" "+bootstrap.stderr()).toLowerCase(Locale.ROOT);
          boolean trustHint=trustConfirmationRequired(combined) || windowsUsbHintPresent() || !linuxUsbDevices.isEmpty();
          String state=trustHint?"TRUST_CONFIRM_REQUIRED":"LOCKDOWN_UNAVAILABLE";
          String msg=trustHint
            ? "GAM hat idevicepair trotz noch fehlender Lockdown-UDID direkt gestartet. Bitte das iPad entsperrt und den Bildschirm aktiv lassen und ausdrücklich „Diesem Computer vertrauen?“ bestätigen. Der Foto-/Video-Dialog von Windows ist nicht ausreichend. Falls der Trust-Dialog nicht erscheint, USB einmal neu verbinden und Trust / Pairing erneut starten."
            : "Das Betriebssystem erkennt das Apple-USB-Gerät, aber usbmuxd/Lockdown liefert weiterhin keine UDID. Die Pairing-Anforderung wurde versucht; Details stehen in logs/apple-mobile-debug.log.";
          appleTrace("EXIT pair", "state="+state+", bootstrap=true, windowsPnP="+pnpDevices.size());
          return new PairingResult(false,state,msg,OffsetDateTime.now().toString(),null,false,System.currentTimeMillis()-started);
        }
      } else {
        int physicalCount=Math.max(pnpDevices.size(),linuxUsbDevices.size());
        String state=physicalCount==0?"NO_DEVICE":"SELECT_DEVICE";
        appleTrace("EXIT pair", "state="+state+", windowsPnP="+pnpDevices.size()+", linuxAppleUsb="+linuxUsbDevices.size());
        String msg=physicalCount==0
          ? "Kein Apple-Geraet ist ueber Lockdown oder die physische USB-Erkennung erreichbar. Bitte iPad entsperren, USB neu verbinden und erneut suchen."
          : "Mehrere Apple-USB-Geräte sind physisch sichtbar, aber Lockdown liefert noch keine UDID. Bitte nur das gewünschte Gerät angeschlossen lassen und Pairing erneut starten.";
        return new PairingResult(false,state,msg,OffsetDateTime.now().toString(),null,false,System.currentTimeMillis()-started);
      }
    }
    String udid=blankToNull(requestedUdid);
    if (udid==null) {
      if(ids.size()!=1) {
        appleTrace("EXIT pair", "state=SELECT_DEVICE, count="+ids.size());
        return new PairingResult(false,"SELECT_DEVICE","Mehrere Apple-Geraete sind erreichbar; fuer Pairing muss die UDID eindeutig sein.",OffsetDateTime.now().toString(),null,false,System.currentTimeMillis()-started);
      }
      udid=ids.get(0);
    }
    CommandResult before=runToolTraced(12,"PAIR VALIDATE BEFORE","idevicepair","-u",udid,"validate");
    if(pairingSucceeded(before)) {
      // 40k37e56c-fix8: Ein normales Apple-Pairing ist erst komplett, wenn nach dem
      // USB-/Lockdown-Trust auch der RemotePairing-Record fuer den spaeteren
      // USERSPACE_RSD-WLAN-Transport erzeugt wurde. Dieser Oberbau ist auf Windows,
      // Linux/CachyOS und macOS identisch; nur resolveCoreDevicePmd3E55h() loest den
      // plattformspezifischen Tool-Unterbau auf.
      PairingResult complete=completeUsbAndRemotePairingE56cFix8(udid,started,true);
      appleTrace("EXIT pair", "state="+complete.state()+", alreadyValid=true, remotePairing=true, udid="+traceUdid(udid));
      return complete;
    }

    // 40k37c46: idevicepair pair ist die eigentliche Lockdown-Anforderung, die den
    // iOS/iPadOS-Dialog "Diesem Computer vertrauen?" ausloesen soll. Danach wird
    // bewusst einige Sekunden validiert, damit der Benutzer die Abfrage am entsperrten
    // Geraet bestaetigen kann, ohne Pairing mehrfach anklicken zu muessen.
    CommandResult pr=runToolTraced(35,"PAIR REQUEST","idevicepair","-u",udid,"pair");
    CommandResult after=null;
    boolean ok=false;
    for(int attempt=1; attempt<=10; attempt++){
      if(attempt>1){ try { Thread.sleep(1000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; } }
      after=runToolTraced(12,"PAIR VALIDATE AFTER attempt="+attempt,"idevicepair","-u",udid,"validate");
      if(pairingSucceeded(after)){ ok=true; break; }
      if(attempt==3 || attempt==7) tracePairingEnvironment("pair-waiting-trust-"+attempt);
    }
    if(after==null) after=new CommandResult(false,"","Pairing-Pruefung wurde unterbrochen");
    String combined=(pr.stdout()+" "+pr.stderr()+" "+after.stdout()+" "+after.stderr()).toLowerCase(Locale.ROOT);
    boolean trustHint=trustConfirmationRequired(combined) || (!ok && windowsUsbHintPresent());
    String state=ok?"READY":(trustHint?"TRUST_CONFIRM_REQUIRED":"PAIRING_FAILED");
    String msg;
    if(ok) {
      msg="Trust/Pairing wurde erfolgreich hergestellt und ueber Lockdown validiert.";
    } else if(state.equals("TRUST_CONFIRM_REQUIRED")) {
      msg="GAM hat die Lockdown-Vertrauensanforderung an das iPad gesendet. Bitte das iPad entsperrt und den Bildschirm aktiv lassen und dort ausdrücklich „Diesem Computer vertrauen?“ bestätigen. Der separate Dialog zum Anzeigen/Zulassen von Fotos und Videos ist nur die Windows-/PTP-Medienfreigabe und reicht für GAM nicht aus. Wenn der Trust-Dialog nicht erscheint: USB-Kabel einmal trennen und neu verbinden, danach Trust / Pairing erneut starten. Bleibt der Dialog weiterhin aus, Apple-Geräte einmal öffnen bzw. Windows neu starten und anschließend erneut prüfen.";
    } else {
      msg="Pairing konnte nicht validiert werden: "+firstNonBlank(pr.stderr(),pr.stdout(),after.stderr(),after.stdout(),"unbekannter Fehler")+". Bitte iPad entsperrt lassen, USB neu verbinden und danach erneut Pairing starten.";
    }
    appleTrace("PAIR TRUST GUIDANCE", "state="+state+", trustHint="+trustHint+", photoDialogIsNotLockdown=true, validateWaitAttempts=10");
    if(ok) {
      PairingResult complete=completeUsbAndRemotePairingE56cFix8(udid,started,false);
      appleTrace("EXIT pair", "state="+complete.state()+", success="+complete.success()+", usbPairing=true, remotePairing=true, udid="+traceUdid(udid));
      return complete;
    }
    appleTrace("EXIT pair", "state="+state+", success=false, udid="+traceUdid(udid));
    return new PairingResult(false,state,msg,OffsetDateTime.now().toString(),udid,false,System.currentTimeMillis()-started);
  }

  /** 40k37e56c-fix8: Gemeinsamer Apple-Pairing-Oberbau fuer alle Hostplattformen.
   * Schritt 1 (idevicepair/Lockdown) ist zu diesem Zeitpunkt bereits validiert.
   * Schritt 2 erzeugt ueber denselben vorhandenen pymobiledevice3-Pfad den
   * RemotePairing-Record, den der bestehende USERSPACE_RSD-WLAN-Pfad spaeter nutzt.
   * Es wird absichtlich KEIN remote browse und KEIN neuer WLAN-Discovery-Pfad benutzt. */
  private PairingResult completeUsbAndRemotePairingE56cFix8(String udid,long started,boolean usbAlreadyValid) {
    log.info("[DISCOVERY][40k37e56c-fix10][APPLE_MOBILE][PAIRING] event=APPLE_PAIR_START udid={} usbRequired=true",maskUdid(udid));
    log.info("[DISCOVERY][40k37e56c-fix10][APPLE_MOBILE][PAIRING] event=USB_LOCKDOWN_PAIR_OK udid={} alreadyValid={}",maskUdid(udid),usbAlreadyValid);
    try {
      Path runtime=findGamRoot().toAbsolutePath().normalize().resolve("tools").resolve("apple-coredevice").resolve("runtime");
      Path pmd=resolveCoreDevicePmd3E55h(runtime);
      // fix12: Wenn auf Linux nur pymobiledevice3 fehlt, stellt GAM den gemeinsamen
      // distributionsunabhaengigen Unterbau jetzt selbst her, bevor RemotePairing beginnt.
      String hostOs=System.getProperty("os.name","").toLowerCase(Locale.ROOT);
      if((pmd==null || !Files.isRegularFile(pmd)) && hostOs.contains("linux")) {
        ToolchainInstallResult bootstrap=linuxToolchainPackageManagerE55(started);
        log.info("[DISCOVERY][40k37e56c-fix12][APPLE_MOBILE][PAIRING] event=LINUX_COREDEVICE_BOOTSTRAP success={} state={} message={}",bootstrap.success(),bootstrap.state(),limitTrace(bootstrap.message()));
        pmd=resolveCoreDevicePmd3E55h(runtime);
      }
      // fix35: Auf macOS denselben bereits bewaehrten GAM-eigenen PMD3-Unterbau wiederherstellen,
      // der von macOsToolchainE56() seit 40k37e56 unter
      // tools/apple-coredevice/runtime/macos-venv angelegt wird. Der Pairing-Pfad darf nicht
      // bei PMD3_MISSING abbrechen, wenn die Runtime nach einem Overlay/Versionswechsel im
      // aktuellen GAM-Root lediglich neu erzeugt werden muss. Keine Aenderung an USERSPACE_RSD.
      if((pmd==null || !Files.isRegularFile(pmd)) && hostOs.contains("mac")) {
        log.info("[DISCOVERY][40k37e56c-fix35][APPLE_MOBILE][PAIRING] event=MACOS_PMD3_RESTORE_START runtime={}",runtime);
        ToolchainInstallResult bootstrap=macOsToolchainE56(started);
        log.info("[DISCOVERY][40k37e56c-fix35][APPLE_MOBILE][PAIRING] event=MACOS_PMD3_RESTORE_RESULT success={} state={} message={}",bootstrap.success(),bootstrap.state(),limitTrace(bootstrap.message()));
        pmd=resolveCoreDevicePmd3E55h(runtime);
      }
      log.info("[DISCOVERY][40k37e56c-fix35][APPLE_MOBILE][PAIRING] event=USB_REMOTE_PAIR_RUNTIME udid={} runtime={} executable={} exists={}",maskUdid(udid),runtime,pmd,pmd!=null && Files.isRegularFile(pmd));
      if(pmd==null || !Files.isRegularFile(pmd)) {
        log.warn("[DISCOVERY][40k37e56c-fix8][APPLE_MOBILE][PAIRING] phase=REMOTEPAIRING result=PMD3_MISSING udid={} os={}",maskUdid(udid),System.getProperty("os.name",""));
        return new PairingResult(false,"REMOTE_PAIRING_TOOLS_MISSING",
          "USB-/Lockdown-Pairing ist gueltig, aber der gemeinsame RemotePairing-Schritt konnte nicht gestartet werden, weil die GAM-pymobiledevice3-Runtime fehlt.",
          OffsetDateTime.now().toString(),udid,true,System.currentTimeMillis()-started);
      }
      log.info("[DISCOVERY][40k37e56c-fix10][APPLE_MOBILE][PAIRING] event=USB_REMOTE_PAIR_START udid={} os={} executable={} usbRequired=true",maskUdid(udid),System.getProperty("os.name",""),pmd);
      CommandResult rp=runProcessE20(90,Map.of(),pmd.toString(),"--no-color","lockdown","remotepairing","--pair");
      String data=(rp.stdout()+"\n"+rp.stderr()).trim();
      String low=data.toLowerCase(Locale.ROOT);
      boolean remoteOk=rp.ok()&&!low.contains("traceback")&&!low.contains("error ")&&!low.contains("no device");
      if(remoteOk) cacheRemotePairingBasisE27(data,udid,"NORMAL_PAIR_E56C_FIX8");
      log.info("[DISCOVERY][40k37e56c-fix10][APPLE_MOBILE][PAIRING] event={} exit={} udid={} data={}",remoteOk?"USB_REMOTE_PAIR_OK":"USB_REMOTE_PAIR_FAILED",rp.exitCode(),maskUdid(udid),limitTrace(data));
      if(!remoteOk) return new PairingResult(false,"REMOTE_PAIRING_FAILED",
        "USB-/Lockdown-Pairing ist gueltig, aber das anschliessende RemotePairing ueber USB fuer USERSPACE_RSD ist fehlgeschlagen: "+firstNonBlank(rp.stderr(),rp.stdout(),"keine Detailmeldung"),
        OffsetDateTime.now().toString(),udid,true,System.currentTimeMillis()-started);

      // fix12: Pairing endet hier bewusst. USERSPACE_RSD-Pruefung und Inventarisierung
      // gehoeren in die separat gestarteten Such-/Inventarisierungsaktionen.
      log.info("[DISCOVERY][40k37e56c-fix12][APPLE_MOBILE][PAIRING] event=APPLE_PAIR_COMPLETE udid={} lockdownPairing=true remotePairing=true usbInventoryReady=true userspaceRsdPairingReady=true",maskUdid(udid));
      return new PairingResult(true,"READY",
        (usbAlreadyValid?"Trust/USB-Pairing war bereits gueltig. ":"Trust/USB-Pairing wurde erfolgreich hergestellt. ")+
        "RemotePairing wurde im selben Pairing-Ablauf ebenfalls ueber USB abgeschlossen. Beide Pairings sind bereit. USERSPACE_RSD wird erst bei der separat gestarteten Suche bzw. Inventarisierung geprueft.",
        OffsetDateTime.now().toString(),udid,true,System.currentTimeMillis()-started);
    } catch(Exception ex) {
      log.warn("[DISCOVERY][40k37e56c-fix8][APPLE_MOBILE][PAIRING] phase=REMOTEPAIRING result=ERROR udid={} error={}",maskUdid(udid),ex.toString());
      return new PairingResult(false,"REMOTE_PAIRING_ERROR",
        "USB-/Lockdown-Pairing ist gueltig, aber RemotePairing konnte nicht abgeschlossen werden: "+ex.getMessage(),
        OffsetDateTime.now().toString(),udid,true,System.currentTimeMillis()-started);
    }
  }

  /** 40k37e51: iOS/iPadOS 27+ kann die Erstkopplung vollstaendig ueber WLAN
   * device-initiiert durchfuehren. Dieser Pfad ist bewusst unabhaengig von USB und wird
   * bis zu einem realen iOS-27-Test als implementiert, aber noch nicht validiert markiert. */
  /** 40k37e54-fix11: Read-only RemotePairing-Preflight vor UAC; keine Inventarisierung. */
  private boolean adminRemotePairingPreflightE54Fix11(Path pmd, String udid) {
    try {
      Map<String,String> e=new HashMap<>();
      e.put("PYMOBILEDEVICE3_USERSPACE","1");
      e.put("PYMOBILEDEVICE3_DEFAULT_FALLBACK","userspace");
      log.info("[DISCOVERY][40k37e54-fix11][APPLE_MOBILE][ADMIN_RSD] state=PRECHECK, udid={}, action=READ_ONLY_USERSPACE_READY",maskUdid(udid));
      CommandResult r=runProcessE20(75,e,pmd.toString(),"--no-color","apps","install-record","com.apple.Preferences","--userspace");
      String data=(r.stdout()+"\n"+r.stderr()).trim(), low=data.toLowerCase(Locale.ROOT);
      boolean ok=r.ok() && !data.isBlank() && !low.contains("traceback") && !low.contains("no device") && !low.contains("device is not connected") && !low.contains("error: ") && (low.contains("com.apple.preferences") || low.contains("bundle") || low.contains("identifier") || low.contains("install"));
      log.info("[DISCOVERY][40k37e54-fix11][APPLE_MOBILE][ADMIN_RSD] state={}, udid={}, processOk={}, exit={}",ok?"REMOTEPAIRING_READY":"PRECHECK_FAILED",maskUdid(udid),r.ok(),r.exitCode());
      return ok;
    } catch(Exception ex) {
      log.warn("[DISCOVERY][40k37e54-fix11][APPLE_MOBILE][ADMIN_RSD] state=PRECHECK_FAILED, udid={}, error={}",maskUdid(udid),ex.toString());
      return false;
    }
  }

  /** 40k37e54-fix16: Migriert nur den Apple-eigenen pymobiledevice3 RemotePairing-Record.
   * Wichtig gegenueber fix15: Backups liegen AUSSERHALB von ~/.pymobiledevice3, weil
   * pymobiledevice3 dort auch Dateien mit remote_*-Praefix als Pairing-Kandidaten scannt.
   * Der alte Record wird vor dem promptlosen USB-Re-Pair wirklich entfernt. */
  private String ensureAltIrkRemotePairingE54Fix16(Path pmd, String udid) {
    Path record=null, backup=null, legacyFix15Backup=null;
    try {
      Path pairDir=Path.of(System.getProperty("user.home",".")).resolve(".pymobiledevice3");
      record=pairDir.resolve("remote_"+udid+".plist");
      legacyFix15Backup=pairDir.resolve("remote_"+udid+".plist.gam-fix15-backup");
      Files.createDirectories(pairDir);
      Path backupDir=findGamRoot().toAbsolutePath().normalize().resolve("logs").resolve("apple-pairing-backups");
      Files.createDirectories(backupDir);
      backup=backupDir.resolve("remote_"+udid+".plist.pre-fix16.bak");

      // Ein von fix15 im Pairing-Verzeichnis hinterlassenes Backup darf die Bonjour/altIRK-
      // Auswahl niemals beeinflussen, auch dann nicht, wenn der Hauptrecord bereits aktuell ist.
      if(Files.isRegularFile(legacyFix15Backup)) {
        Path archived=backupDir.resolve("remote_"+udid+".plist.fix15-stale.bak");
        Files.move(legacyFix15Backup,archived,StandardCopyOption.REPLACE_EXISTING);
        log.info("[DISCOVERY][40k37e54-fix16][APPLE_MOBILE][ADMIN_RSD] state=STALE_BACKUP_QUARANTINED, udid={}, destination={}",maskUdid(udid),archived.getFileName());
      }
      if(Files.isRegularFile(record)) {
        byte[] raw=Files.readAllBytes(record);
        String probe=new String(raw,StandardCharsets.ISO_8859_1).toLowerCase(Locale.ROOT);
        if(probe.contains("altirk")) {
          log.info("[DISCOVERY][40k37e54-fix16][APPLE_MOBILE][ADMIN_RSD] state=PAIR_RECORD_CURRENT, udid={}, altIRK=true",maskUdid(udid));
          return "OK";
        }
      }
      if(Files.isRegularFile(record)) {
        Files.copy(record,backup,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.COPY_ATTRIBUTES);
        log.info("[DISCOVERY][40k37e54-fix16][APPLE_MOBILE][ADMIN_RSD] state=PAIR_RECORD_BACKUP, udid={}, backupDir=GAM_LOGS",maskUdid(udid));
        Files.delete(record);
        log.info("[DISCOVERY][40k37e54-fix16][APPLE_MOBILE][ADMIN_RSD] state=STALE_PAIR_RECORD_REMOVED, udid={}",maskUdid(udid));
      }

      log.info("[DISCOVERY][40k37e54-fix16][APPLE_MOBILE][ADMIN_RSD] state=PAIR_RECORD_REFRESH_START, udid={}, action=LOCKDOWN_REMOTEPAIRING_USB_PAIR",maskUdid(udid));
      CommandResult pair=runProcessE20(75,Map.of(),pmd.toString(),"--no-color","lockdown","remotepairing","--pair");
      boolean alt=false;
      if(Files.isRegularFile(record)) {
        try {
          byte[] raw=Files.readAllBytes(record);
          alt=new String(raw,StandardCharsets.ISO_8859_1).toLowerCase(Locale.ROOT).contains("altirk");
        } catch(Exception ignored) {}
      }
      log.info("[DISCOVERY][40k37e54-fix16][APPLE_MOBILE][ADMIN_RSD] state=PAIR_RECORD_REFRESH_RESULT, udid={}, processOk={}, exit={}, recordExists={}, altIRK={}",maskUdid(udid),pair.ok(),pair.exitCode(),Files.isRegularFile(record),alt);
      if(!pair.ok() || !alt) {
        Files.deleteIfExists(record);
        if(backup!=null && Files.isRegularFile(backup)) Files.copy(backup,record,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.COPY_ATTRIBUTES);
        log.warn("[DISCOVERY][40k37e54-fix16][APPLE_MOBILE][ADMIN_RSD] state=PAIR_RECORD_ROLLBACK, udid={}, reason={}",maskUdid(udid),!pair.ok()?"PAIR_FAILED":"ALTIRK_MISSING");
        return "PAIR_REFRESH_FAILED";
      }
      if(!adminRemotePairingPreflightE54Fix11(pmd,udid)) {
        Files.deleteIfExists(record);
        if(backup!=null && Files.isRegularFile(backup)) Files.copy(backup,record,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.COPY_ATTRIBUTES);
        log.warn("[DISCOVERY][40k37e54-fix16][APPLE_MOBILE][ADMIN_RSD] state=PAIR_RECORD_ROLLBACK, udid={}, reason=USERSPACE_RECHECK_FAILED",maskUdid(udid));
        return "USERSPACE_RECHECK_FAILED";
      }
      log.info("[DISCOVERY][40k37e54-fix16][APPLE_MOBILE][ADMIN_RSD] state=PAIR_RECORD_MIGRATED, udid={}, altIRK=true, userspaceRecheck=true, backupOutsidePairDir=true",maskUdid(udid));
      return "OK";
    } catch(Exception ex) {
      try {
        if(record!=null) Files.deleteIfExists(record);
        if(record!=null && backup!=null && Files.isRegularFile(backup)) Files.copy(backup,record,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.COPY_ATTRIBUTES);
      } catch(Exception ignored) {}
      log.warn("[DISCOVERY][40k37e54-fix16][APPLE_MOBILE][ADMIN_RSD] state=PAIR_RECORD_MIGRATION_EXCEPTION, udid={}, error={}, rollbackAttempted={}",maskUdid(udid),ex.toString(),backup!=null);
      return "PAIR_REFRESH_FAILED";
    }
  }

  /** 40k37e54-fix12: Ein Klick/eine Anforderung erzeugt hoechstens einen UAC-Startlauf. */
  public AdminTunnelStatus startAdminTunnel() {
    if(!ADMIN_RSD_STARTING_E54F12.compareAndSet(false,true)) {
      log.info("[DISCOVERY][40k37e54-fix12][APPLE_MOBILE][ADMIN_RSD] state=START_ALREADY_RUNNING, action=REJECT_DUPLICATE");
      return new AdminTunnelStatus(false,"STARTING","Der Admin-Tunnel wird bereits gestartet. Ein doppelter Start wurde verhindert.",OffsetDateTime.now().toString(),null,null,0,0);
    }
    try { return startAdminTunnelE54Fix20(); }
    finally { ADMIN_RSD_STARTING_E54F12.set(false); }
  }

  /** 40k37e54-fix20: Live-Erfassung des privilegierten Lockdown/CoreDevice-Adminpfads.
   * Fuer iOS/iPadOS >= 17.4 wird der real verifizierte Aufruf
   * `lockdown start-tunnel --udid <UDID> --script-mode` verwendet. Der zuvor erzwungene
   * `--mobdev2`-Pfad ist entfernt; RemotePairing-Records werden nicht veraendert. */
  private synchronized AdminTunnelStatus startAdminTunnelE54Fix20() {
    if(!System.getProperty("os.name","").toLowerCase(Locale.ROOT).contains("win"))
      return new AdminTunnelStatus(false,"UNSUPPORTED","Der Admin-Tunnel mit UAC ist nur unter Windows verfuegbar.",OffsetDateTime.now().toString(),null,null,0,0);
    AdminTunnelStatus existing=adminTunnelStatus();
    if(existing.active()) return existing;
    try {
      Path root=findGamRoot().toAbsolutePath().normalize();
      Path pmd=root.resolve("tools").resolve("apple-coredevice").resolve("runtime").resolve("bin").resolve("pymobiledevice3.exe");
      if(!Files.isRegularFile(pmd)) return new AdminTunnelStatus(false,"PMD3_MISSING","Die GAM-pymobiledevice3-Runtime fehlt.",OffsetDateTime.now().toString(),null,null,0,0);
      String udid=COREDEVICE_SELECTED_UDID_E25.get();
      if(udid==null||udid.isBlank()) udid=recoverPersistedCoreDeviceUdidE40();
      if(udid==null||udid.isBlank()) return new AdminTunnelStatus(false,"NO_UDID","Kein bekanntes Apple-Geraet fuer den Admin-Tunnel gefunden. Bitte zuerst Apple-WLAN/Pairing pruefen.",OffsetDateTime.now().toString(),null,null,0,0);

      // 40k37e54-fix17: Der Admin-Pfad darf nicht versehentlich mit der alten 11.12.5-Runtime
      // laufen. 11.16.3 enthaelt die Windows/mobdev2-Korrekturen fuer private
      // WLAN-Adressen und Pair-Records. Bei direktem Klick auf den Admin-Button aktualisieren
      // wir deshalb dieselbe isolierte GAM-Runtime, bevor der read-only Preflight beginnt.
      Path runtime=root.resolve("tools").resolve("apple-coredevice").resolve("runtime");
      Path requiredMarker=runtime.resolve("pymobiledevice3-11.16.3.ready");
      if(!Files.isRegularFile(requiredMarker)) {
        Path bootstrap=root.resolve("tools").resolve("apple-coredevice").resolve("bootstrap-windows.ps1");
        log.info("[DISCOVERY][40k37e54-fix20][APPLE_MOBILE][ADMIN_RSD] state=RUNTIME_UPDATE_REQUIRED, targetPmd3=11.16.3");
        CommandResult update=runProcessE20(180,Map.of(),"powershell.exe","-NoProfile","-NonInteractive","-ExecutionPolicy","Bypass","-File",bootstrap.toString(),"-GamRoot",root.toString(),"-Udid",udid);
        log.info("[DISCOVERY][40k37e54-fix20][APPLE_MOBILE][ADMIN_RSD] state=RUNTIME_UPDATE_RESULT, targetPmd3=11.16.3, ok={}, exit={}, data={}",update.ok(),update.exitCode(),limitTrace((update.stdout()+"\n"+update.stderr()).trim()));
        if(!update.ok() || !Files.isRegularFile(requiredMarker) || !Files.isRegularFile(pmd))
          return new AdminTunnelStatus(false,"RUNTIME_UPDATE_FAILED","Die isolierte Apple-Runtime konnte nicht auf pymobiledevice3 11.16.3 aktualisiert werden.",OffsetDateTime.now().toString(),maskUdid(udid),null,0,0);
      }
      if(!adminRemotePairingPreflightE54Fix11(pmd,udid)) return new AdminTunnelStatus(false,"PRECHECK_FAILED","Apple-WLAN/Userspace-RSD ist noch nicht bereit. Der Admin-Tunnel wurde nicht gestartet.",OffsetDateTime.now().toString(),maskUdid(udid),null,0,0);
      // 40k37e54-fix19: Der reale A/B-Test auf Windows hat den funktionierenden Aufruf belegt:
      // lockdown start-tunnel --udid <UDID> --script-mode. --mobdev2 darf hier NICHT erzwungen
      // werden; es scheitert auf demselben Host/Geraet bereits bei lockdown info mit
      // "Device is not connected". Pairing-Records bleiben unveraendert.
      log.info("[DISCOVERY][40k37e54-fix20][APPLE_MOBILE][ADMIN_RSD] state=PAIR_RECORD_POLICY, udid={}, route=LOCKDOWN_COREDEVICE, remotePairingMutation=DISABLED, deviceSelection=UDID",maskUdid(udid));
      String productVersion="";
      try {
        Path basis=root.resolve("logs").resolve("apple-mobile-coredevice-e27-basis.json");
        if(Files.isRegularFile(basis)) {
          String b=Files.readString(basis,StandardCharsets.UTF_8);
          java.util.regex.Matcher vm=java.util.regex.Pattern.compile("\"(?:HumanReadableProductVersionString|ProductVersion)\"\\s*:\\s*\"([^\"]+)\"").matcher(b);
          if(vm.find()) productVersion=vm.group(1);
        }
      } catch(Exception ignored) {}
      int osMajor=0, osMinor=0;
      try { String[] vp=productVersion.split("\\."); osMajor=Integer.parseInt(vp[0]); if(vp.length>1) osMinor=Integer.parseInt(vp[1]); } catch(Exception ignored) {}
      boolean modernLockdown=(osMajor>17)||(osMajor==17&&osMinor>=4);
      log.info("[DISCOVERY][40k37e54-fix20][APPLE_MOBILE][ADMIN_RSD] state=TRANSPORT_VERSION_SELECT, udid={}, productVersion={}, route={}",maskUdid(udid),productVersion.isBlank()?"UNKNOWN":productVersion,modernLockdown?"LOCKDOWN_COREDEVICEPROXY":"CLASSIC_REMOTEPAIRING_WIFI");
      Path state=root.resolve("logs").resolve("apple-admin-tunnel"); Files.createDirectories(state);
      Path out=state.resolve("stdout.log"), err=state.resolve("stderr.log"), pid=state.resolve("pid.txt"), supervisorPid=state.resolve("supervisor-pid.txt"), endpointFile=state.resolve("endpoint.txt"), udidFile=state.resolve("udid.txt"), exitFile=state.resolve("exit.txt"), lifecycle=state.resolve("lifecycle.log"), heartbeat=state.resolve("heartbeat.txt"), stopRequest=state.resolve("stop-request.txt"), fix23Marker=state.resolve("fix23-lifecycle.marker"), wrapper=state.resolve("start-admin-tunnel.ps1");
      for(Path f:List.of(out,err,pid,supervisorPid,endpointFile,exitFile,lifecycle,heartbeat,stopRequest,fix23Marker)) Files.deleteIfExists(f);
      Files.writeString(fix23Marker,"40k37e54-fix24",StandardCharsets.UTF_8);
      Files.writeString(udidFile,udid,StandardCharsets.UTF_8);
      String ps="$ErrorActionPreference='Stop'\n"+
        "$exe='"+pmd.toString().replace("'","''")+"'\n"+
        "$udid='"+udid.replace("'","''")+"'\n"+
        "$out='"+out.toString().replace("'","''")+"'\n"+
        "$err='"+err.toString().replace("'","''")+"'\n"+
        "$pidfile='"+pid.toString().replace("'","''")+"'\n"+
        "$supervisor='"+supervisorPid.toString().replace("'","''")+"'\n"+
        "$exitfile='"+exitFile.toString().replace("'","''")+"'\n"+
        "$endpointfile='"+endpointFile.toString().replace("'","''")+"'\n"+
        "$life='"+lifecycle.toString().replace("'","''")+"'\n"+
        "$heartbeat='"+heartbeat.toString().replace("'","''")+"'\n"+
        "$stoprequest='"+stopRequest.toString().replace("'","''")+"'\n"+
        "function Mark([string]$m){ Add-Content -LiteralPath $life -Value ((Get-Date -Format o)+' '+$m) -Encoding UTF8 }\n"+
        "Set-Content -LiteralPath $supervisor -Value $PID -Encoding ASCII\n"+
        "Mark ('SUPERVISOR_STARTED pid='+$PID)\n"+
        // fix19: Keine mobdev2-Probes mehr. Der manuelle A/B-Test hat bereits gezeigt,
        // dass mobdev2 fuer dieses Geraet auf Windows nicht der funktionierende Auswahlpfad ist.
        (modernLockdown
          ? "$a=@('--no-color','lockdown','start-tunnel','--udid',$udid,'--script-mode')\n"+
            "Mark ('PMD3_START exe='+$exe+' args=--no-color lockdown start-tunnel --udid <masked> --script-mode transport=ADMIN_LOCKDOWN_COREDEVICEPROXY_RSD expectedUdid=<masked>')\n"
          : "$a=@('--no-color','remote','start-tunnel','--connection-type','wifi','--udid',$udid,'--protocol','tcp','--script-mode')\n"+
            "Mark ('PMD3_START exe='+$exe+' args=--no-color remote start-tunnel --connection-type wifi --udid <masked> --protocol tcp --script-mode transport=ADMIN_CLASSIC_WIFI_RSD expectedUdid=<masked>')\n")+
        "try {\n"+
        "  try { $Host.UI.RawUI.WindowTitle='GAM 2.0 - Apple Admin-Tunnel - NICHT SCHLIESSEN' } catch {}\n"+
        "  Write-Host '============================================================'\n"+
        "  Write-Host ' GAM 2.0 - Apple Admin-Tunnel fuer die Inventarisierung'\n"+
        "  Write-Host '============================================================'\n"+
        "  Write-Host ''\n"+
        "  Write-Host ' Tunnel wird aufgebaut / ist fuer die Inventarisierung aktiv.'\n"+
        "  Write-Host ' Bitte dieses Fenster NICHT schliessen!'\n"+
        "  Write-Host ' Den Tunnel bitte ueber GAM mit Admin-Tunnel beenden stoppen.'\n"+
        "  Write-Host ''\n"+
        "  Write-Host '============================================================'\n"+
        "  $psi=New-Object System.Diagnostics.ProcessStartInfo\n"+
        "  $psi.FileName=$exe\n"+
        "  $psi.UseShellExecute=$false\n"+
        "  $psi.CreateNoWindow=$true\n"+
        "  $psi.RedirectStandardOutput=$true\n"+
        "  $psi.RedirectStandardError=$true\n"+
        "  $psi.Arguments=(($a | ForEach-Object { '\"' + ($_ -replace '\"','\\\"') + '\"' }) -join ' ')\n"+
        "  $p=New-Object System.Diagnostics.Process\n"+
        "  $p.StartInfo=$psi\n"+
        "  [void]$p.Start()\n"+
        "  Set-Content -LiteralPath $pidfile -Value $p.Id -Encoding ASCII\n"+
        "  Mark ('PMD3_STARTED pid='+$p.Id)\n"+
        "  $stdoutTask=$p.StandardOutput.ReadLineAsync()\n"+
        "  $stderrTask=$p.StandardError.ReadLineAsync()\n"+
        "  while(-not $p.HasExited){\n"+
        "    if(Test-Path -LiteralPath $stoprequest){ Mark ('STOP_REQUEST_RECEIVED pid='+$p.Id); try { $p.Kill($true) } catch { try { $p.Kill() } catch {} }; break }\n"+
        "    Set-Content -LiteralPath $heartbeat -Value ([DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()) -Encoding ASCII\n"+
        "    if($stdoutTask.IsCompleted){ $line=$stdoutTask.Result; if($null -ne $line){ Add-Content -LiteralPath $out -Value $line -Encoding UTF8; if($line -match '^\\s*([^\\s]+)\\s+(\\d+)\\s*$'){ Set-Content -LiteralPath $endpointfile -Value ($Matches[1]+[Environment]::NewLine+$Matches[2]) -Encoding UTF8; Mark ('RSD_ENDPOINT_CAPTURED host='+$Matches[1]+' port='+$Matches[2]); Write-Host (' RSD-Endpunkt: '+$Matches[1]+' '+$Matches[2]) }; $stdoutTask=$p.StandardOutput.ReadLineAsync() } }\n"+
        "    if($stderrTask.IsCompleted){ $line=$stderrTask.Result; if($null -ne $line){ Add-Content -LiteralPath $err -Value $line -Encoding UTF8; Write-Host $line; $stderrTask=$p.StandardError.ReadLineAsync() } }\n"+
        "    Start-Sleep -Milliseconds 50\n"+
        "  }\n"+
        "  while(-not $stdoutTask.IsCompleted){ Start-Sleep -Milliseconds 10 }; if($null -ne $stdoutTask.Result){ Add-Content -LiteralPath $out -Value $stdoutTask.Result -Encoding UTF8 }\n"+
        "  while(-not $stderrTask.IsCompleted){ Start-Sleep -Milliseconds 10 }; if($null -ne $stderrTask.Result){ Add-Content -LiteralPath $err -Value $stderrTask.Result -Encoding UTF8 }\n"+
        "  Set-Content -LiteralPath $exitfile -Value $p.ExitCode -Encoding ASCII\n"+
        "  Mark ('PMD3_EXIT pid='+$p.Id+' exit='+$p.ExitCode)\n"+
        "} catch {\n"+
        "  Set-Content -LiteralPath $exitfile -Value ('LAUNCH_ERROR '+$_.Exception.Message) -Encoding UTF8\n"+
        "  Mark ('LAUNCH_ERROR '+$_.Exception.Message)\n"+
        "  throw\n"+
        "}\n";
      Files.writeString(wrapper,ps,StandardCharsets.UTF_8);
      log.info("[DISCOVERY][40k37e54-fix20][APPLE_MOBILE][ADMIN_RSD] state=START_REQUEST, udid={}, wrapper={}",maskUdid(udid),wrapper);
      String command="Start-Process -FilePath 'powershell.exe' -Verb RunAs -ArgumentList @('-NoProfile','-ExecutionPolicy','Bypass','-File','"+wrapper.toString().replace("'","''")+"')";
      Process launcher=new ProcessBuilder("powershell.exe","-NoProfile","-Command",command).start();
      launcher.waitFor(15,TimeUnit.SECONDS);
      log.info("[DISCOVERY][40k37e54-fix20][APPLE_MOBILE][ADMIN_RSD] state=UAC_LAUNCH_RETURNED, launcherExit={}, udid={}",launcher.isAlive()?"RUNNING":launcher.exitValue(),maskUdid(udid));
      String[] endpoint=null; long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(30);
      while(System.nanoTime()<deadline && endpoint==null) {
        if(Files.isRegularFile(endpointFile)) {
          try {
            List<String> epLines=Files.readAllLines(endpointFile,StandardCharsets.UTF_8);
            if(epLines.size()>=2 && !epLines.get(0).trim().isBlank() && epLines.get(1).trim().matches("\\d+")) endpoint=new String[]{epLines.get(0).trim(),epLines.get(1).trim()};
          } catch(Exception ignored) {}
        }
        if(endpoint==null && Files.isRegularFile(out)) for(String line:Files.readAllLines(out,StandardCharsets.UTF_8)) { endpoint=parseRsdEndpointE25(line); if(endpoint!=null) break; }
        if(endpoint==null && Files.isRegularFile(exitFile)) break;
        if(endpoint==null) Thread.sleep(250);
      }
      long tunnelPid=0, supPid=0; try { tunnelPid=Long.parseLong(Files.readString(pid).trim()); } catch(Exception ignored) {} try { supPid=Long.parseLong(Files.readString(supervisorPid).trim()); } catch(Exception ignored) {}
      if(endpoint==null) {
        String exit=Files.isRegularFile(exitFile)?Files.readString(exitFile,StandardCharsets.UTF_8).trim():"<noch kein Exitcode>";
        String stderr=Files.isRegularFile(err)?Files.readString(err,StandardCharsets.UTF_8).trim():"";
        String life=Files.isRegularFile(lifecycle)?Files.readString(lifecycle,StandardCharsets.UTF_8).trim():"";
        log.error("[DISCOVERY][40k37e54-fix20][APPLE_MOBILE][ADMIN_RSD] state=NO_ENDPOINT, udid={}, supervisorPid={}, tunnelPid={}, exit={}, stderr={}, lifecycle={}",maskUdid(udid),supPid,tunnelPid,exit,stderr.replace("\r"," ").replace("\n"," | "),life.replace("\r"," ").replace("\n"," | "));
        return new AdminTunnelStatus(false,"START_FAILED","UAC wurde ausgeloest, aber der Admin-Tunnel lieferte keinen RSD-Endpunkt. Diagnose: logs/apple-admin-tunnel/lifecycle.log, stderr.log und exit.txt.",OffsetDateTime.now().toString(),maskUdid(udid),null,0,tunnelPid);
      }
      Files.writeString(endpointFile,endpoint[0]+System.lineSeparator()+endpoint[1],StandardCharsets.UTF_8);
      log.info("[DISCOVERY][40k37e54-fix20][APPLE_MOBILE][ADMIN_RSD] state=ENDPOINT_RECEIVED, udid={}, host={}, port={}, supervisorPid={}, tunnelPid={}",maskUdid(udid),endpoint[0],endpoint[1],supPid,tunnelPid);
      CommandResult verify=runProcessE20(30,Map.of(),pmd.toString(),"--no-color","remote","rsd-info","--rsd",endpoint[0],endpoint[1]);
      boolean verified=semanticReadOnlyJsonE28(verify,(verify.stdout()+"\n"+verify.stderr()).trim());
      log.info("[DISCOVERY][40k37e54-fix20][APPLE_MOBILE][ADMIN_RSD] state=VERIFY, udid={}, ok={}, exit={}",maskUdid(udid),verified,verify.exitCode());
      if(!verified) return new AdminTunnelStatus(false,"VERIFY_FAILED","Admin-Tunnel wurde gestartet, der RSD-Endpunkt konnte aber nicht verifiziert werden. Der Prozess bleibt fuer die Diagnose bestehen.",OffsetDateTime.now().toString(),maskUdid(udid),endpoint[0],Integer.parseInt(endpoint[1]),tunnelPid);
      ADMIN_RSD_HOST_E54F8.set(endpoint[0]); ADMIN_RSD_PORT_E54F8.set(endpoint[1]); ADMIN_RSD_UDID_E54F8.set(udid); ADMIN_RSD_PID_E54F8.set(tunnelPid);
      log.info("[DISCOVERY][40k37e54-fix20][APPLE_MOBILE][ADMIN_RSD] state=ACTIVE, udid={}, host={}, port={}, supervisorPid={}, tunnelPid={}, lifecycle=ENDPOINT_VERIFIED",maskUdid(udid),endpoint[0],endpoint[1],supPid,tunnelPid);
      return adminTunnelStatus();
    } catch(Exception e) {
      log.warn("[DISCOVERY][40k37e54-fix20][APPLE_MOBILE][ADMIN_RSD] state=START_EXCEPTION, error={}",e.toString());
      return new AdminTunnelStatus(false,"START_FAILED","Admin-Tunnel konnte nicht gestartet werden: "+e.getMessage(),OffsetDateTime.now().toString(),null,null,0,0);
    }
  }

  /** 40k37e54-fix24: Kooperativer Stop ohne zweites UAC; der bereits erhoehte Supervisor beendet seinen pmd3-Kindprozess. */
  public synchronized AdminTunnelStatus stopAdminTunnel() {
    Path state=findGamRoot().resolve("logs").resolve("apple-admin-tunnel");
    AdminTunnelStatus before=adminTunnelStatus();
    long tunnelPid=ADMIN_RSD_PID_E54F8.get(), supervisorPid=0;
    try { if(Files.isRegularFile(state.resolve("supervisor-pid.txt"))) supervisorPid=Long.parseLong(Files.readString(state.resolve("supervisor-pid.txt")).trim()); } catch(Exception ignored) {}
    log.info("[DISCOVERY][40k37e54-fix24][APPLE_MOBILE][ADMIN_RSD] state=STOP_REQUEST, supervisorPid={}, tunnelPid={}, endpoint={}:{}",supervisorPid,tunnelPid,before.host(),before.port());
    boolean killRequested=false;
    try {
      long target=supervisorPid>0?supervisorPid:tunnelPid;
      if(target>0 && System.getProperty("os.name","").toLowerCase(Locale.ROOT).contains("win")) {
        // fix24: Kein zweites UAC im Normalfall. Der bereits erhoehte Supervisor beobachtet
        // diese GAM-eigene Stop-Datei und beendet seinen eigenen pmd3-Kindprozess selbst.
        Files.writeString(state.resolve("stop-request.txt"),OffsetDateTime.now().toString(),StandardCharsets.UTF_8);
        killRequested=true;
        log.info("[DISCOVERY][40k37e54-fix24][APPLE_MOBILE][ADMIN_RSD] state=STOP_SIGNAL_SENT, method=ELEVATED_SUPERVISOR_COOPERATIVE");
      } else if(target>0) killRequested=ProcessHandle.of(target).map(ProcessHandle::destroy).orElse(true);
    } catch(Exception ex) { log.warn("[DISCOVERY][40k37e54-fix24][APPLE_MOBILE][ADMIN_RSD] state=STOP_KILL_ERROR, error={}",ex.toString()); }
    boolean endpointDead=true;
    if(before.host()!=null && before.port()>0) {
      endpointDead=false;
      for(int i=0;i<12;i++) {
        try {
          Path pmd=findGamRoot().resolve("tools").resolve("apple-coredevice").resolve("runtime").resolve("bin").resolve("pymobiledevice3.exe");
          CommandResult verify=runProcessE20(5,Map.of(),pmd.toString(),"--no-color","remote","rsd-info","--rsd",before.host(),String.valueOf(before.port()));
          if(!semanticReadOnlyJsonE28(verify,(verify.stdout()+"\n"+verify.stderr()).trim())) { endpointDead=true; break; }
        } catch(Exception ex) { endpointDead=true; break; }
        try { Thread.sleep(500); } catch(InterruptedException ie) { Thread.currentThread().interrupt(); break; }
      }
    }
    if(endpointDead) {
      log.info("[DISCOVERY][40k37e54-fix24][APPLE_MOBILE][ADMIN_RSD] state=TUNNEL_PROCESS_TERMINATED, killRequested={}, supervisorPid={}, tunnelPid={}",killRequested,supervisorPid,tunnelPid);
      log.info("[DISCOVERY][40k37e54-fix24][APPLE_MOBILE][ADMIN_RSD] state=ENDPOINT_DEAD");
      ADMIN_RSD_HOST_E54F8.set(""); ADMIN_RSD_PORT_E54F8.set(""); ADMIN_RSD_UDID_E54F8.set(""); ADMIN_RSD_PID_E54F8.set(0L);
      try { for(String n:List.of("endpoint.txt","pid.txt","supervisor-pid.txt","udid.txt","heartbeat.txt","stop-request.txt","fix23-lifecycle.marker")) Files.deleteIfExists(state.resolve(n)); } catch(Exception ignored) {}
      log.info("[DISCOVERY][40k37e54-fix24][APPLE_MOBILE][ADMIN_RSD] state=STOPPED");
      return new AdminTunnelStatus(false,"STOPPED","Admin-Tunnel ist beendet.",OffsetDateTime.now().toString(),null,null,0,0);
    }
    log.warn("[DISCOVERY][40k37e54-fix24][APPLE_MOBILE][ADMIN_RSD] state=STOP_FAILED, endpointStillAlive=true");
    return new AdminTunnelStatus(true,"ACTIVE","Admin-Tunnel ist weiterhin aktiv; Beenden wurde nicht bestaetigt.",OffsetDateTime.now().toString(),before.udid(),before.host(),before.port(),tunnelPid);
  }

  public AdminTunnelStatus adminTunnelStatus() {
    long pid=ADMIN_RSD_PID_E54F8.get(); String host=ADMIN_RSD_HOST_E54F8.get(), port=ADMIN_RSD_PORT_E54F8.get(), udid=ADMIN_RSD_UDID_E54F8.get();
    try {
      Path state=findGamRoot().resolve("logs").resolve("apple-admin-tunnel");
      if((host==null||host.isBlank()) && Files.isRegularFile(state.resolve("endpoint.txt"))) {
        List<String> ep=Files.readAllLines(state.resolve("endpoint.txt"),StandardCharsets.UTF_8);
        if(ep.size()>=2) { host=ep.get(0).trim(); port=ep.get(1).trim(); }
      }
      if((udid==null||udid.isBlank()) && Files.isRegularFile(state.resolve("udid.txt"))) udid=Files.readString(state.resolve("udid.txt"),StandardCharsets.UTF_8).trim();
      if(pid<=0 && Files.isRegularFile(state.resolve("pid.txt"))) try { pid=Long.parseLong(Files.readString(state.resolve("pid.txt"),StandardCharsets.UTF_8).trim()); } catch(Exception ignored) {}
    } catch(Exception ignored) {}
    int p=0; try { p=Integer.parseInt(port); } catch(Exception ignored) {}
    boolean processVisible=pid>0 && ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false);
    boolean endpointAlive=false;
    if(host!=null&&!host.isBlank()&&p>0) try {
      Path pmd=findGamRoot().resolve("tools").resolve("apple-coredevice").resolve("runtime").resolve("bin").resolve("pymobiledevice3.exe");
      if(Files.isRegularFile(pmd)) {
        CommandResult verify=runProcessE20(8,Map.of(),pmd.toString(),"--no-color","remote","rsd-info","--rsd",host,String.valueOf(p));
        endpointAlive=semanticReadOnlyJsonE28(verify,(verify.stdout()+"\n"+verify.stderr()).trim());
      }
    } catch(Exception ignored) {}
    boolean alive=endpointAlive; // fix23: fix21-Startlogik bleibt massgeblich; keine Abhaengigkeit von ProcessHandle-Sichtbarkeit ueber UAC hinweg.
    try {
      Path state=findGamRoot().resolve("logs").resolve("apple-admin-tunnel");
      Path marker=state.resolve("fix23-lifecycle.marker"), hb=state.resolve("heartbeat.txt");
      if(alive && Files.isRegularFile(marker) && Files.isRegularFile(hb)) {
        long beat=Long.parseLong(Files.readString(hb,StandardCharsets.UTF_8).trim());
        long age=System.currentTimeMillis()-beat;
        if(age>5000) {
          alive=false;
          log.warn("[DISCOVERY][40k37e54-fix23][APPLE_MOBILE][ADMIN_RSD] state=LOST, reason=SUPERVISOR_HEARTBEAT_STALE, heartbeatAgeMs={}, endpointAlive={}",age,endpointAlive);
        }
      }
    } catch(Exception ignored) {}
    if(alive) {
      ADMIN_RSD_HOST_E54F8.set(host); ADMIN_RSD_PORT_E54F8.set(String.valueOf(p)); ADMIN_RSD_UDID_E54F8.set(udid==null?"":udid); ADMIN_RSD_PID_E54F8.set(pid);
    } else if(pid>0 || (host!=null&&!host.isBlank())) {
      log.warn("[DISCOVERY][40k37e54-fix9][APPLE_MOBILE][ADMIN_RSD] state=LOST, pid={}, processVisible={}, endpointAlive={}, host={}, port={}",pid,processVisible,endpointAlive,host,p);
      ADMIN_RSD_PID_E54F8.set(0L); ADMIN_RSD_HOST_E54F8.set(""); ADMIN_RSD_PORT_E54F8.set("");
    }
    return new AdminTunnelStatus(alive,alive?"ACTIVE":"STOPPED",alive?"Admin-Tunnel aktiv.":"Admin-Tunnel nicht aktiv.",OffsetDateTime.now().toString(),alive?maskUdid(udid):null,alive?host:null,alive?p:0,alive?pid:0);
  }

  /** 40k37e56c: Explizites WLAN-Pairing fuer Apple-Geraete.
   * Bei einem bereits per USB vertrauten iPhone/iPad wird der von pymobiledevice3
   * vorgesehene Lockdown-RemotePairing-Bootstrap verwendet. Ohne USB bleibt fuer
   * iOS/iPadOS 27+ der vorhandene pair-host-Pfad als drahtloser Fallback erhalten.
   * Die Aktion fuehrt keine Inventarisierung aus. */
  /** 40k37e56c-fix6: Liefert den letzten WLAN-Pairing-Versuch unabhaengig von normalen USB-/Quellentests. */
  public PairingResult wifiPairingStatus() {
    PairingResult r=LAST_WIFI_PAIRING_E56C_FIX6.get();
    return r!=null?r:new PairingResult(false,"NOT_RUN","WLAN-Pairing wurde seit Backend-Start noch nicht durchgefuehrt.",null,null,false,0L);
  }

  private PairingResult rememberWifiPairingE56cFix6(PairingResult r) {
    LAST_WIFI_PAIRING_E56C_FIX6.set(r);
    log.info("[DISCOVERY][40k37e56c-fix6][APPLE_MOBILE][WLAN_PAIRING_STATUS] state={}, success={}, paired={}, udid={}, message={}",
      r.state(),r.success(),r.paired(),maskUdid(r.udid()),limitTrace(r.message()));
    return r;
  }

  public PairingResult pairWifi(String requestedUdid) {
    long started=System.currentTimeMillis();
    log.info("[DISCOVERY][40k37e56c-fix7][APPLE_MOBILE][WLAN_PAIRING_RAW] phase=START requestedUdid={}",maskUdid(requestedUdid));
    try {
      Path runtime=findGamRoot().toAbsolutePath().normalize().resolve("tools").resolve("apple-coredevice").resolve("runtime");
      Path pmd=resolveCoreDevicePmd3E55h(runtime);
      if(pmd==null || !Files.isRegularFile(pmd))
        return rememberWifiPairingE56cFix6(new PairingResult(false,"PMD3_MISSING","WLAN-Pairing benoetigt die GAM-pymobiledevice3-Runtime.",OffsetDateTime.now().toString(),blankToNull(requestedUdid),false,System.currentTimeMillis()-started));
      List<String> ids=connectedUdidsWithRetry("wifi-pair",3,500);
      String udid=blankToNull(requestedUdid);
      if(udid==null && ids.size()==1) udid=ids.get(0);
      final String pairingUdid=udid;
      if(pairingUdid!=null && !ids.isEmpty() && ids.stream().anyMatch(x->x.equalsIgnoreCase(pairingUdid))) {
        log.info("[DISCOVERY][40k37e56c][APPLE_MOBILE][WLAN_PAIRING] route=USB_LOCKDOWN_REMOTEPAIRING, udid={}",maskUdid(udid));
        CommandResult r=runProcessE20(90,Map.of(),pmd.toString(),"--no-color","lockdown","remotepairing","--pair");
        String data=(r.stdout()+"\n"+r.stderr()).trim();
        String lower=data.toLowerCase(Locale.ROOT);
        boolean ok=r.ok()&&!lower.contains("traceback")&&!lower.contains("error ")&&!lower.contains("no device");
        boolean wifiReachable=false;
        String browseDetail="";
        if(ok) {
          cacheRemotePairingBasisE27(data,udid,"MANUAL_WIFI_PAIR_E56C_FIX3");
          CommandResult browse=runProcessE20(30,Map.of(),pmd.toString(),"--no-color","remote","browse");
          browseDetail=(browse.stdout()+"\n"+browse.stderr()).trim();
          cacheCoreDeviceWifiIpE33(browseDetail,udid);
          String browseLower=browseDetail.toLowerCase(Locale.ROOT);
          wifiReachable=browse.ok() && !browseLower.contains("traceback") && !browseLower.contains("no device")
            && (browseLower.contains(udid.toLowerCase(Locale.ROOT)) || browseLower.contains("producttype")
                || browseLower.contains("devicename") || browseLower.contains("rsd") || browseLower.contains("tunnel"));
          log.info("[DISCOVERY][40k37e56c-fix3][APPLE_MOBILE][WLAN_PAIRING_VERIFY] pairingOk=true, wlanReachable={}, browseExit={}, udid={}, data={}",wifiReachable,browse.exitCode(),maskUdid(udid),limitTrace(browseDetail));
        }
        String successMessage=wifiReachable
          ? "WLAN-Pairing erfolgreich. CoreDevice/RemotePairing sieht das Geraet bereits ueber WLAN. USB kann jetzt getrennt werden."
          : "WLAN-Pairing erfolgreich. Der RemotePairing-Schritt wurde abgeschlossen; CoreDevice/WLAN ist in der direkten Anschlusspruefung aber noch nicht erreichbar. Damit ist Pairing von der anschliessenden WLAN-Erkennung getrennt diagnostizierbar.";
        String rawPair=limitTrace(firstNonBlank(data,"keine Ausgabe"));
        String rawBrowse=limitTrace(firstNonBlank(browseDetail,"nicht ausgefuehrt/keine Ausgabe"));
        String diagnosticMessage=(ok?successMessage:"WLAN-Pairing ueber USB konnte nicht abgeschlossen werden.")
          +" | Route=USB_LOCKDOWN_REMOTEPAIRING | Pair exit="+r.exitCode()+" | Pair raw="+rawPair
          +" | Remote browse="+(ok?rawBrowse:"nicht ausgefuehrt");
        log.info("[DISCOVERY][40k37e56c-fix7][APPLE_MOBILE][WLAN_PAIRING_RAW] phase=RESULT state={}, success={}, paired={}, pairExit={}, wlanReachable={}, pairRaw={}, browseRaw={}",
          ok?(wifiReachable?"WIFI_PAIR_COMPLETED_REACHABLE":"WIFI_PAIR_COMPLETED_NOT_REACHABLE"):"WIFI_PAIR_FAILED",ok,ok,r.exitCode(),wifiReachable,rawPair,rawBrowse);
        return rememberWifiPairingE56cFix6(new PairingResult(ok,ok?(wifiReachable?"WIFI_PAIR_COMPLETED_REACHABLE":"WIFI_PAIR_COMPLETED_NOT_REACHABLE"):"WIFI_PAIR_FAILED",
          diagnosticMessage,OffsetDateTime.now().toString(),udid,ok,System.currentTimeMillis()-started));
      }
      if(ids.size()>1 && udid==null)
        return rememberWifiPairingE56cFix6(new PairingResult(false,"SELECT_DEVICE","Mehrere Apple-USB-Geraete sind verbunden. Bitte das gewuenschte Geraet eindeutig auswaehlen.",OffsetDateTime.now().toString(),null,false,System.currentTimeMillis()-started));
      log.info("[DISCOVERY][40k37e56c][APPLE_MOBILE][WLAN_PAIRING] route=WIRELESS_PAIR_HOST_27_PLUS, usbDeviceCount={}",ids.size());
      return rememberWifiPairingE56cFix6(pairHostWifiIos27());
    } catch(Exception ex) {
      log.warn("[DISCOVERY][40k37e56c][APPLE_MOBILE][WLAN_PAIRING] result=ERROR error={}",ex.toString());
      return rememberWifiPairingE56cFix6(new PairingResult(false,"WIFI_PAIR_ERROR","WLAN-Pairing fehlgeschlagen: "+ex.getMessage(),OffsetDateTime.now().toString(),blankToNull(requestedUdid),false,System.currentTimeMillis()-started));
    }
  }

  public PairingResult pairHostWifiIos27() {
    long started=System.currentTimeMillis();
    try {
      Path root=findGamRoot().toAbsolutePath().normalize();
      Path pmdExe=root.resolve("tools").resolve("apple-coredevice").resolve("runtime").resolve("bin").resolve("pymobiledevice3.exe");
      if(!Files.isRegularFile(pmdExe)) return new PairingResult(false,"PMD3_MISSING","WLAN-Erstpairing fuer iOS/iPadOS 27+ benoetigt die GAM-pymobiledevice3-Runtime.",OffsetDateTime.now().toString(),null,false,System.currentTimeMillis()-started);
      log.info("[DISCOVERY][40k37e51][APPLE_MOBILE][WLAN_INITIAL_PAIRING] action=PAIR_HOST, usbRequired=false, minimumOs=27, validationState=IMPLEMENTED_UNTESTED");
      CommandResult help=runProcessE20(15,Map.of(),pmdExe.toString(),"--no-color","remote","pair-host","--help");
      String hd=(help.stdout()+"\n"+help.stderr()).toLowerCase(Locale.ROOT);
      if(!help.ok() || !hd.contains("ios 27")) return new PairingResult(false,"PAIR_HOST_UNSUPPORTED","Die installierte pymobiledevice3-Version bietet den erwarteten iOS-27+-pair-host-Pfad nicht an.",OffsetDateTime.now().toString(),null,false,System.currentTimeMillis()-started);
      CommandResult r=runProcessE20(180,Map.of(),pmdExe.toString(),"--no-color","remote","pair-host");
      String data=(r.stdout()+"\n"+r.stderr()).trim();
      String lower=data.toLowerCase(Locale.ROOT);
      boolean ok=r.ok() && !lower.contains("traceback") && !lower.contains("error:");
      log.info("[DISCOVERY][40k37e51][APPLE_MOBILE][WLAN_INITIAL_PAIRING] result={}, exit={}, data={}",ok?"COMPLETED":"FAILED",r.exitCode(),limitTrace(data));
      return new PairingResult(ok,ok?"WIFI_PAIR_HOST_COMPLETED":"WIFI_PAIR_HOST_FAILED",ok?"WLAN-Erstpairing wurde von pymobiledevice3 abgeschlossen. Der iOS/iPadOS-27+-Pfad ist implementiert, bleibt bis zum ersten realen 27+-Test in GAM als noch nicht praktisch validiert markiert.":"WLAN-Erstpairing wurde gestartet, aber nicht erfolgreich abgeschlossen: "+firstNonBlank(r.stderr(),r.stdout(),"keine Detailmeldung"),OffsetDateTime.now().toString(),null,ok,System.currentTimeMillis()-started);
    } catch(Exception ex) {
      log.warn("[DISCOVERY][40k37e51][APPLE_MOBILE][WLAN_INITIAL_PAIRING] result=ERROR error={}",ex.toString());
      return new PairingResult(false,"WIFI_PAIR_HOST_ERROR","WLAN-Erstpairing fehlgeschlagen: "+ex.getMessage(),OffsetDateTime.now().toString(),null,false,System.currentTimeMillis()-started);
    }
  }

  public PairingResult validatePairing(String requestedUdid) {
    long started=System.currentTimeMillis();
    appleTrace("ENTER validatePairing", "requestedUdid="+traceUdid(requestedUdid));
    tracePairingEnvironment("validate-start");
    if (!commandExists("idevice_id") || !commandExists("idevicepair")) {
      appleTrace("EXIT validatePairing", "state=TOOLS_MISSING");
      return new PairingResult(false,"TOOLS_MISSING","Lockdown-Pairing kann ohne idevice_id/idevicepair nicht validiert werden.",OffsetDateTime.now().toString(),blankToNull(requestedUdid),false,System.currentTimeMillis()-started);
    }
    List<String> ids=connectedUdidsWithRetry("validate", 5, 850); String udid=blankToNull(requestedUdid); if(udid==null&&ids.size()==1)udid=ids.get(0);
    if(udid==null) {
      String state=ids.isEmpty()?"NO_DEVICE":"SELECT_DEVICE";
      tracePairingEnvironment("validate-no-lockdown");
      appleTrace("EXIT validatePairing", "state="+state+", nativeIds="+ids.size()+", windowsPnP="+(!readWindowsUsbDevices().isEmpty()));
      return new PairingResult(false,state,ids.isEmpty()?"Kein Apple-Geraet ueber Lockdown erreichbar. Falls Windows das iPad bereits fuer Fotos/Explorer sieht, ist das noch keine Lockdown-Vertrauensbeziehung. Bitte Gerätesuche ausführen, iPad entsperren und Trust / Pairing starten.":"Mehrere Apple-Geraete erreichbar; UDID erforderlich.",OffsetDateTime.now().toString(),null,false,System.currentTimeMillis()-started);
    }
    CommandResult vr=runToolTraced(15,"PAIR VALIDATE","idevicepair","-u",udid,"validate"); boolean ok=pairingSucceeded(vr);
    String combined=(vr.stdout()+" "+vr.stderr()).toLowerCase(Locale.ROOT);
    boolean trustHint=trustConfirmationRequired(combined) || (!ok && windowsUsbHintPresent());
    String state=ok?"READY":(trustHint?"TRUST_CONFIRM_REQUIRED":"TRUST_REQUIRED");
    String msg=ok?"Trust/Pairing ist gueltig."
      : trustHint?"Das iPad ist erreichbar, aber die Lockdown-Vertrauensbestätigung fehlt noch. Bitte „Diesem Computer vertrauen?“ am entsperrten iPad bestätigen. Eine Freigabe nur für Fotos/Videos reicht nicht aus. Wenn der Trust-Dialog nicht erscheint, USB einmal neu verbinden und Trust / Pairing starten."
      :"Trust/Pairing ist nicht gueltig oder das iPad ist fuer Lockdown nicht erreichbar: "+firstNonBlank(vr.stderr(),vr.stdout(),"keine Detailmeldung");
    appleTrace("EXIT validatePairing", "state="+state+", success="+ok+", udid="+traceUdid(udid));
    return new PairingResult(ok,state,msg,OffsetDateTime.now().toString(),udid,ok,System.currentTimeMillis()-started);
  }

  private static boolean trustConfirmationRequired(String text){
    String x=text==null?"":text.toLowerCase(Locale.ROOT);
    return x.contains("trust") || x.contains("passcode") || x.contains("user denied") ||
      x.contains("user declined") || x.contains("please accept") || x.contains("pairing dialog") ||
      x.contains("invalid hostid") || x.contains("not paired") || x.contains("pair record");
  }

  /** 40k37e25: Ergebnis der Apple-Identitätszuordnung. Stabile Apple-Kennungen haben
   * Vorrang vor Transport-/Einzelgerät-Fallbacks. Ein Widerspruch wird bewusst nicht
   * automatisch überschrieben. */
  private record AppleIdentitySelection(AppleDevice device,String state,String reason,boolean conflict) {}

  private AppleIdentitySelection selectAppleDeviceForRegisteredIdentity(String rowAddress,String rowSerial,String rowHardware,String rowProtocol,List<AppleDevice> devices){
    if(devices==null || devices.isEmpty()) return new AppleIdentitySelection(null,"NO_DEVICE","Kein Apple-Mobilgerät ist aktuell erreichbar.",false);
    String expectedUdid=extractProtocolDetail(rowProtocol,"UDID");
    if(expectedUdid==null && rowAddress!=null){
      for(AppleDevice d:devices) if(rowAddress.toLowerCase(Locale.ROOT).contains(d.udid().toLowerCase(Locale.ROOT))) { expectedUdid=d.udid(); break; }
    }
    String expectedSerial=cleanIdentityValue(rowSerial);
    String expectedWifi=normalizeHardwareAddress(rowHardware);
    List<AppleDevice> strong=new ArrayList<>();
    List<String> reasons=new ArrayList<>();
    for(AppleDevice d:devices){
      boolean udidMatch=expectedUdid!=null && expectedUdid.equalsIgnoreCase(d.udid());
      boolean serialMatch=expectedSerial!=null && ((d.serialNumber()!=null && expectedSerial.equalsIgnoreCase(d.serialNumber())) || expectedSerial.equalsIgnoreCase(d.udid()));
      boolean wifiMatch=expectedWifi!=null && d.wifiAddress()!=null && expectedWifi.equals(normalizeHardwareAddress(d.wifiAddress()));
      boolean protocolUdid=rowProtocol!=null && d.udid()!=null && rowProtocol.toLowerCase(Locale.ROOT).contains(d.udid().toLowerCase(Locale.ROOT));
      if(udidMatch || serialMatch || wifiMatch || protocolUdid){
        strong.add(d);
        List<String> r=new ArrayList<>();
        if(udidMatch||protocolUdid) r.add("UDID");
        if(serialMatch) r.add("Seriennummer");
        if(wifiMatch) r.add("WiFi-MAC");
        reasons.add(String.join("+",r));
      }
    }
    if(strong.size()==1) return new AppleIdentitySelection(strong.get(0),"STABLE_MATCH","Stabile Zuordnung über "+reasons.get(0)+"; Transportweg USB/WLAN ist dafür unerheblich.",false);
    if(strong.size()>1) return new AppleIdentitySelection(null,"AMBIGUOUS_STABLE_MATCH","Mehrere erreichbare Apple-Geräte passen auf gespeicherte stabile Kennungen; automatische Zuordnung wurde aus Sicherheitsgründen gestoppt.",true);

    boolean hasStableExpectation=expectedUdid!=null || expectedSerial!=null || expectedWifi!=null;
    if(devices.size()==1){
      AppleDevice only=devices.get(0);
      if(hasStableExpectation){
        return new AppleIdentitySelection(null,"STABLE_ID_CONFLICT","Das einzige erreichbare Apple-Gerät widerspricht der bereits gespeicherten stabilen Identität (UDID/Seriennummer/WiFi-MAC). GAM ordnet es deshalb nicht automatisch zu.",true);
      }
      return new AppleIdentitySelection(only,"SINGLE_DEVICE_FALLBACK","Noch keine stabile Apple-Kennung gespeichert; eindeutiger Ein-Gerät-Fallback wird verwendet und durch das erfolgreiche Inventar verifiziert.",false);
    }
    return new AppleIdentitySelection(null,"SELECTION_REQUIRED","Mehrere Apple-Mobilgeräte sind erreichbar und keine stabile gespeicherte Kennung liefert eine eindeutige Zuordnung.",false);
  }

  private static String extractProtocolDetail(String protocol,String label){
    if(protocol==null || label==null) return null;
    String needle=label+":";
    for(String part:protocol.split("\\s*[·|]\\s*")){
      String t=part.trim();
      if(t.regionMatches(true,0,needle,0,needle.length())) return cleanIdentityValue(t.substring(needle.length()));
    }
    return null;
  }

  private static String cleanIdentityValue(String value){
    if(value==null) return null;
    String v=value.trim();
    if(v.isEmpty() || "null".equalsIgnoreCase(v) || v.startsWith("SERIAL=") || v.contains("CHASSIS=")) return null;
    return v;
  }

  private static String normalizeHardwareAddress(String value){
    String v=cleanIdentityValue(value);
    return v==null?null:v.replace("-","").replace(":","").replace(".","").toUpperCase(Locale.ROOT);
  }

  /**
   * 40k37c: liest nur normale Lockdown/ideviceinfo-Basiswerte und übernimmt
   * sie als verifizierte Inventardaten in die bestehende GAM-Identität.
   */
  public InventoryResult inventory(String identityKey) {
    long started=System.currentTimeMillis();
    String key=blankToNull(identityKey);
    ApplePerformanceProfile performance=key==null?null:new ApplePerformanceProfile(key);
    if(performance!=null) APPLE_PERFORMANCE.set(performance);
    if(key==null) return finishInventoryE3(performance,new InventoryResult(false,"FEHLGESCHLAGEN","Geräteidentität fehlt.",OffsetDateTime.now().toString(),null,null,0,System.currentTimeMillis()-started));

    Map<String,Object> row;
    long phaseStarted=System.currentTimeMillis();
    try {
      row=registrations.find(key);
      if(row==null) row=Map.of();
      performancePhase("Registrierte Geräteidentität laden",phaseStarted,true,"fields="+row.size());
    } catch(Exception e) {
      performancePhase("Registrierte Geräteidentität laden",phaseStarted,false,e.getClass().getSimpleName());
      return finishInventoryE3(performance,new InventoryResult(false,"FEHLGESCHLAGEN","Registrierte Geräteidentität konnte nicht geladen werden: "+firstNonBlank(e.getMessage(),e.getClass().getSimpleName()),OffsetDateTime.now().toString(),key,null,0,System.currentTimeMillis()-started));
    }
    if(!commandExists("idevice_id") || !commandExists("ideviceinfo") || !commandExists("idevicepair"))
      return finishInventoryE3(performance,new InventoryResult(false,"TOOLS_MISSING","Die GAM-lokale libimobiledevice-Toolchain ist unvollständig; idevice_id, ideviceinfo und idevicepair werden benötigt.",OffsetDateTime.now().toString(),key,null,0,System.currentTimeMillis()-started));

    phaseStarted=System.currentTimeMillis();
    List<AppleDevice> devices;
    try { devices=readNativeDevices(true); }
    catch(Exception e) {
      performancePhase("Apple-Gerät erkennen / Basisdaten",phaseStarted,false,e.getClass().getSimpleName());
      return finishInventoryE3(performance,new InventoryResult(false,"NICHT_ERREICHBAR","Apple-Geräteerkennung ist fehlgeschlagen: "+firstNonBlank(e.getMessage(),e.getClass().getSimpleName()),OffsetDateTime.now().toString(),key,null,0,System.currentTimeMillis()-started));
    }
    performancePhase("Apple-Gerät erkennen / Basisdaten",phaseStarted,!devices.isEmpty(),"devices="+devices.size());
    if(devices.isEmpty()) return finishInventoryE3(performance,new InventoryResult(false,"NICHT_ERREICHBAR","Kein Apple-Mobilgerät ist aktuell über usbmuxd/Lockdown erreichbar.",OffsetDateTime.now().toString(),key,null,0,System.currentTimeMillis()-started));

    String rowAddress=str(row.get("address"));
    String rowSerial=str(row.get("serialNumber"));
    String rowProtocol=str(row.get("protocol"));
    String rowHardware=str(row.get("hardwareAddress"));
    AppleIdentitySelection identitySelection=selectAppleDeviceForRegisteredIdentity(rowAddress,rowSerial,rowHardware,rowProtocol,devices);
    AppleDevice selected=identitySelection.device();
    performancePhase("Apple-Identität / Zuordnung",System.currentTimeMillis(),selected!=null,"state="+identitySelection.state()+", reason="+identitySelection.reason());
    appleTrace("E4 IDENTITY MATCH","state="+identitySelection.state()+", reason="+traceValue(identitySelection.reason())+", candidates="+devices.size()+", selected="+(selected==null?"none":traceUdid(selected.udid())));
    if(selected==null) {
      String state=identitySelection.conflict()?"IDENTITAETSKONFLIKT":"AUSWAHL_ERFORDERLICH";
      return finishInventoryE3(performance,new InventoryResult(false,state,identitySelection.reason(),OffsetDateTime.now().toString(),key,null,0,System.currentTimeMillis()-started));
    }
    if(!selected.paired()) return finishInventoryE3(performance,new InventoryResult(false,"NICHT_AUTORISIERT","Trust/Pairing ist für dieses Apple-Gerät nicht gültig. Bitte zuerst Pairing prüfen bzw. herstellen.",OffsetDateTime.now().toString(),key,selected,0,System.currentTimeMillis()-started));

    performance.udid=selected.udid();

    // e3: Das Lockdown-Basisinventar ist der harte Mindestanker. Wenn das Gerät nach der
    // Erkennung abgezogen wurde, Trust verloren ging oder ideviceinfo nicht mehr antwortet,
    // werden weder leere Teilbestände gespeichert noch teure Folgepfade gestartet.
    phaseStarted=System.currentTimeMillis();
    InventoryProbeResult probe;
    try { probe=probeInventory(selected.udid()); }
    catch(Exception e) {
      performancePhase("Lockdown-Basisinventar / Domains",phaseStarted,false,e.getClass().getSimpleName());
      appleTrace("E4 HARDEN CORE ERROR",e.getClass().getSimpleName()+": "+traceValue(e.getMessage()));
      return finishInventoryE3(performance,new InventoryResult(false,"FEHLGESCHLAGEN","Lockdown-Basisinventar ist unerwartet fehlgeschlagen: "+firstNonBlank(e.getMessage(),e.getClass().getSimpleName()),OffsetDateTime.now().toString(),key,selected,0,System.currentTimeMillis()-started));
    }
    performancePhase("Lockdown-Basisinventar / Domains",phaseStarted,probe.success(),"state="+probe.state()+", defaultFields="+probe.defaultFieldCount()+", domains="+probe.domains().size());
    if(!probe.success()) {
      appleTrace("E4 HARDEN CORE STOP","state="+probe.state()+", message="+traceValue(probe.message()));
      return finishInventoryE3(performance,new InventoryResult(false,probe.state(),"Apple-Inventarisierung wurde vor optionalen Folgeabfragen beendet: "+probe.message(),OffsetDateTime.now().toString(),key,selected,0,System.currentTimeMillis()-started));
    }

    // e3: Health/Diagnose und Software sind voneinander unabhängige Zusatzbereiche.
    // Ein Ausfall darf das erfolgreiche Kerninventar nicht vernichten.
    phaseStarted=System.currentTimeMillis();
    ExtendedInventoryProbeResult extendedProbe;
    try { extendedProbe=probeExtendedInventory(selected.udid()); }
    catch(Exception e) {
      extendedProbe=new ExtendedInventoryProbeResult(false,"FEHLGESCHLAGEN","Diagnose-/Health-Abfrage ist fehlgeschlagen: "+firstNonBlank(e.getMessage(),e.getClass().getSimpleName()),OffsetDateTime.now().toString(),selected.udid(),commandExists("idevicediagnostics"),Map.of(),0,System.currentTimeMillis()-phaseStarted);
      appleTrace("E4 HARDEN EXTENDED ERROR",e.getClass().getSimpleName()+": "+traceValue(e.getMessage()));
    }
    performancePhase("Diagnose / Health / IORegistry",phaseStarted,extendedProbe.success(),"state="+extendedProbe.state()+", sections="+extendedProbe.sections().size()+", fields="+extendedProbe.scalarFieldCount());

    phaseStarted=System.currentTimeMillis();
    SoftwareProbeResult softwareProbe;
    try { softwareProbe=probeSoftware(selected.udid()); }
    catch(Exception e) {
      softwareProbe=new SoftwareProbeResult(false,"FEHLGESCHLAGEN","Software-Inventarisierung ist fehlgeschlagen: "+firstNonBlank(e.getMessage(),e.getClass().getSimpleName()),List.of(),0,0,System.currentTimeMillis()-phaseStarted);
      appleTrace("E4 HARDEN SOFTWARE ERROR",e.getClass().getSimpleName()+": "+traceValue(e.getMessage()));
    }
    performancePhase("Software / installation_proxy",phaseStarted,softwareProbe.success(),"state="+softwareProbe.state()+", apps="+softwareProbe.items().size()+", user="+softwareProbe.userCount()+", system="+softwareProbe.systemCount());
    // 40k37e51-fix3: Der erfolgreiche USB-Lauf ist die Referenz fuer die WLAN-Paritaet.
    // Es werden ausschliesslich bereits gelesene Inventardaten gespeichert; keine Zusatzabfrage.
    writeUsbParityReferenceE51Fix3(selected.udid(),probe,extendedProbe,softwareProbe);
    if(softwareProbe.success() && !softwareProbe.items().isEmpty()) {
      latestSoftwareInventory.put(key,softwareProbe.items());
    } else {
      appleTrace("E4 HARDEN SOFTWARE CACHE","action=preserve-previous, state="+softwareProbe.state()+", apps="+softwareProbe.items().size());
    }

    String platform=selected.platform();
    String type="iPadOS".equals(platform)?"Tablet / iPadOS":"Smartphone / iOS";
    String displayName=firstNonBlank(selected.modelDisplayName(),selected.name(),selected.productType(),str(row.get("name")),"Apple-Mobilgerät");
    String protocol="Apple Mobile / Basisinventarisierung / libimobiledevice / usbmuxd / lockdown (40k37e25)"
      + detail("UDID",selected.udid()) + detail("Gerätename",selected.name()) + detail("Plattform",platform)
      + detail("ProductType",selected.productType()) + detail("Modell",selected.modelDisplayName())
      + detail("OS-Version",selected.productVersion()) + detail("Build",selected.buildVersion())
      + detail("Seriennummer",selected.serialNumber()) + detail("Modellnummer",selected.modelNumber())
      + detail("Hardwaremodell",selected.hardwareModel()) + detail("CPU-Architektur",selected.cpuArchitecture())
      + detail("WiFi-MAC",selected.wifiAddress()) + detail("Bluetooth-MAC",selected.bluetoothAddress())
      + detail("Zeitzone",selected.timeZone()) + detail("Trust/Pairing",selected.pairingState())
      + detail("Identitätszuordnung",identitySelection.reason())
      + detail("Lockdown-Standardfelder",String.valueOf(probe.defaultFieldCount()))
      + detail("Lockdown-Zusatzdomains",String.valueOf(probe.domains().size()))
      + detail("Diagnose-/Health-Status",extendedProbe.state())
      + detail("Diagnose-/Health-Felder",String.valueOf(extendedProbe.scalarFieldCount()))
      + detail("Diagnose-/Health-Bereiche",String.valueOf(extendedProbe.sections().size()))
      + extendedProtocolDetails(extendedProbe)
      + detail("Software-Status",softwareProbe.state())
      + (softwareProbe.success()
          ? detail("Apps Benutzer",String.valueOf(softwareProbe.userCount())) + detail("Apps System",String.valueOf(softwareProbe.systemCount())) + detail("Apps Gesamt",String.valueOf(softwareProbe.items().size()))
          : detail("Apps","nicht aktualisiert; vorheriger gültiger Bestand bleibt erhalten"));

    phaseStarted=System.currentTimeMillis();
    try {
      registrations.recordVerifiedInventoryHit(firstNonBlank(rowAddress,"usb:"+selected.udid()), blankToNull(selected.wifiAddress()),
        firstNonBlank(blankToNull(selected.serialNumber()),selected.udid()), displayName, type, "Apple", protocol, "ONLINE");
      performancePhase("Inventardaten in GAM speichern",phaseStarted,true,"");
    } catch(Exception e) {
      performancePhase("Inventardaten in GAM speichern",phaseStarted,false,e.getClass().getSimpleName());
      appleTrace("E4 HARDEN PERSIST ERROR",e.getClass().getSimpleName()+": "+traceValue(e.getMessage()));
      return finishInventoryE3(performance,new InventoryResult(false,"SPEICHERFEHLER","Apple-Daten wurden gelesen, konnten aber nicht sicher in GAM gespeichert werden: "+firstNonBlank(e.getMessage(),e.getClass().getSimpleName()),OffsetDateTime.now().toString(),key,selected,0,System.currentTimeMillis()-started));
    }

    int fields=0;
    String[] vals={selected.udid(),selected.name(),platform,selected.productType(),selected.modelDisplayName(),selected.productVersion(),selected.buildVersion(),
      selected.serialNumber(),selected.modelNumber(),selected.hardwareModel(),selected.cpuArchitecture(),selected.wifiAddress(),selected.bluetoothAddress(),selected.timeZone(),selected.pairingState()};
    for(String value:vals) if(value!=null&&!value.isBlank()) fields++;
    phaseStarted=System.currentTimeMillis();
    String modelText=firstNonBlank(selected.modelDisplayName(),selected.productType(),"Apple-Mobilgerät");
    boolean fullSuccess=extendedProbe.success() && softwareProbe.success();
    String finalState=fullSuccess?"ERFOLGREICH":"TEILERFOLG";
    List<String> warnings=new ArrayList<>();
    if(!extendedProbe.success()) warnings.add("Diagnose/Health: "+extendedProbe.state());
    if(!softwareProbe.success()) warnings.add("Software: "+softwareProbe.state());
    performancePhase("Gesamtergebnis aufbereiten",phaseStarted,true,"state="+finalState+", fields="+fields+(warnings.isEmpty()?"":", warnings="+warnings.size()));
    String softwareSummary=softwareProbe.success()
      ? softwareProbe.items().size()+" Apps ("+softwareProbe.userCount()+" Benutzer / "+softwareProbe.systemCount()+" System) ermittelt"
      : "Softwarebestand nicht aktualisiert";
    String message="Apple-Inventarisierung 40k37e33-fix8 "+(fullSuccess?"erfolgreich":"mit Teilresultat")+": "+modelText+", "+fields+" Kernfelder übernommen; "+probe.defaultFieldCount()+" Lockdown-Standardfelder, "+probe.domains().size()+" Zusatzdomains, "+extendedProbe.scalarFieldCount()+" Diagnose-/Health-Felder und "+softwareSummary+".";
    if(!warnings.isEmpty()) message += " Nicht erfolgreiche Zusatzbereiche: "+String.join(", ",warnings)+". Vorhandene gültige Daten wurden beibehalten.";
    // 40k37e55g-fix2: recordVerifiedInventoryHit kann die verifizierten Apple-Daten
    // (vor allem Seriennummer/UDID) einer bereits vorhandenen kanonischen Identität
    // zuordnen. Dem Aufrufer deshalb den danach tatsächlich gültigen Identity-Key
    // zurückgeben, damit der Plattform-Inventarisierungslauf an genau diesem Gerät
    // historisiert wird und nicht unter einer vorläufigen Discovery-Identität bleibt.
    String canonicalIdentityKey=registrations.resolveIdentityKey(
      firstNonBlank(rowAddress,"usb:"+selected.udid()), blankToNull(selected.wifiAddress()),
      firstNonBlank(blankToNull(selected.serialNumber()),selected.udid()), displayName);
    if(canonicalIdentityKey==null) canonicalIdentityKey=key;
    return finishInventoryE3(performance,new InventoryResult(true,finalState,message,OffsetDateTime.now().toString(),canonicalIdentityKey,selected,fields,System.currentTimeMillis()-started));
  }

  /**
   * 40k37d1: ermittelt zuerst, was ein normales gepairtes iOS/iPadOS-Geraet
   * tatsaechlich ueber Lockdown preisgibt. Bewusst nur lesende ideviceinfo-Abfragen.
   * Unbekannte/gesperrte Domains sind kein Fehler fuer den Gesamtlauf.
   */
  public InventoryProbeResult probeInventory(String requestedUdid) {
    long started=System.currentTimeMillis();
    long subStarted=System.currentTimeMillis();
    boolean hasIdeviceId=commandExists("idevice_id");
    boolean hasIdeviceInfo=commandExists("ideviceinfo");
    performancePhase("Lockdown/Sub: Tool-Verfügbarkeit",subStarted,hasIdeviceId&&hasIdeviceInfo,"idevice_id="+hasIdeviceId+", ideviceinfo="+hasIdeviceInfo);
    if(!hasIdeviceId || !hasIdeviceInfo)
      return new InventoryProbeResult(false,"TOOLS_MISSING","idevice_id/ideviceinfo fehlen.",OffsetDateTime.now().toString(),blankToNull(requestedUdid),0,Map.of(),Map.of(),System.currentTimeMillis()-started);
    subStarted=System.currentTimeMillis();
    List<String> ids=connectedUdidsWithRetry("d10-inventory-probe",3,500);
    performancePhase("Lockdown/Sub: verbundene UDIDs",subStarted,!ids.isEmpty(),"ids="+ids.size());
    String udid=blankToNull(requestedUdid);
    if(udid==null && ids.size()==1) udid=ids.get(0);
    if(udid==null || !ids.contains(udid))
      return new InventoryProbeResult(false,ids.size()>1?"AUSWAHL_ERFORDERLICH":"NICHT_ERREICHBAR",ids.size()>1?"Mehrere Apple-Geraete verbunden; UDID angeben.":"Kein passendes Apple-Geraet ueber Lockdown erreichbar.",OffsetDateTime.now().toString(),udid,0,Map.of(),Map.of(),System.currentTimeMillis()-started);

    subStarted=System.currentTimeMillis();
    CommandResult validation=runToolTraced(12,"E4 PAIR VALIDATE","idevicepair","-u",udid,"validate");
    performancePhase("Lockdown/Sub: Pairing validieren",subStarted,pairingSucceeded(validation),"");
    if(!pairingSucceeded(validation))
      return new InventoryProbeResult(false,"NICHT_AUTORISIERT","Trust/Pairing ist nicht gueltig.",OffsetDateTime.now().toString(),udid,0,Map.of(),Map.of(),System.currentTimeMillis()-started);

    subStarted=System.currentTimeMillis();
    CommandResult base=runToolTraced(20,"E4 IDEVICEINFO DEFAULT","ideviceinfo","-u",udid);
    Map<String,String> defaults=base.ok()?parseInfo(base.stdout()):Map.of();
    performancePhase("Lockdown/Sub: ideviceinfo Default",subStarted,base.ok(),"fields="+defaults.size());
    LinkedHashMap<String,Map<String,String>> domains=new LinkedHashMap<>();
    // d6: die vier auf dem realen iPad bestaetigten, inhaltlich relevanten Domains bleiben
    // im schnellen Standardlauf. Weitere historisch/geraeteabhaengige Domains werden weiterhin
    // geprueft, aber nur mit kurzem 2-s-Capability-Timeout statt den bisherigen 12 Sekunden.
    String[] fastDomains={
      "com.apple.mobile.battery","com.apple.mobile.sync_data_class",
      "com.apple.mobile.restriction","com.apple.mobile.wireless_lockdown"
    };
    // e3: Nur optionale Domains, die auf dem realen iPad in d17 schnell und erfolgreich
    // geantwortet haben. Die vier nachweislich leeren 2-s-Timeouts
    // (disk_usage, disk_usage.factory, mobile.iTunes, international) gehoeren nicht mehr
    // in den produktiven Inventarpfad. Sie koennen spaeter in einer expliziten
    // Diagnosefunktion weiterhin gezielt untersucht werden.
    String[] optionalDomains={
      "com.apple.mobile.iTunes.accessories","com.apple.mobile.iTunes.store",
      "com.apple.mobile.data_sync","com.apple.mobile.tethered_sync",
      "com.apple.mobile.lockdown_cache"
    };
    subStarted=System.currentTimeMillis();
    for(String domain:fastDomains){
      CommandResult r=runToolTraced(6,"E4 IDEVICEINFO DOMAIN "+domain,"ideviceinfo","-u",udid,"-q",domain);
      Map<String,String> values=r.ok()?parseInfo(r.stdout()):Map.of();
      if(!values.isEmpty()) domains.put(domain,Map.copyOf(values));
    }
    performancePhase("Lockdown/Sub: Kern-Domains",subStarted,true,"count="+fastDomains.length);
    subStarted=System.currentTimeMillis();
    for(String domain:optionalDomains){
      CommandResult r=runToolTraced(2,"E4 IDEVICEINFO OPTIONAL "+domain,"ideviceinfo","-u",udid,"-q",domain);
      Map<String,String> values=r.ok()?parseInfo(r.stdout()):Map.of();
      if(!values.isEmpty()) domains.put(domain,Map.copyOf(values));
    }
    performancePhase("Lockdown/Sub: schnelle optionale Domains",subStarted,true,"count="+optionalDomains.length);
    appleTrace("E4 INVENTORY PROBE SUMMARY","udid="+traceValue(udid)+", defaultFields="+defaults.size()+", readableDomains="+domains.size()+", domains="+traceValue(String.join(",",domains.keySet())));
    boolean ok=base.ok()&&!defaults.isEmpty();
    return new InventoryProbeResult(ok,ok?"ERFOLGREICH":"FEHLGESCHLAGEN",ok?"Normale Lockdown-Inventardaten wurden ausgelesen; "+defaults.size()+" Standardfelder und "+domains.size()+" zusaetzliche Domains sind auf diesem Geraet lesbar.":"ideviceinfo lieferte keine auswertbaren Standarddaten.",OffsetDateTime.now().toString(),udid,defaults.size(),Map.copyOf(defaults),Map.copyOf(domains),System.currentTimeMillis()-started);
  }


  /**
   * 40k37e25: maximaler normaler, rein lesender Hardware-/Health-Probelauf ohne Jailbreak.
   * diagnostics_relay exponiert - geraete-/iOS-abhaengig - All/WiFi/GasGauge/NAND sowie
   * IORegistry-Eintraege wie ASPStorage und AppleARMPMUCharger. AppleSmartBattery ist im normalen e3-Pfad bewusst nicht mehr enthalten, weil GasGauge die benoetigten Batteriedaten liefert und der IORegistry-Aufruf auf dem Test-iPad nur in den Timeout lief. Nicht verfuegbare Bereiche
   * sind kein Fehler und werden nicht erfunden.
   */
  public ExtendedInventoryProbeResult probeExtendedInventory(String requestedUdid) {
    long started=System.currentTimeMillis();
    String udid=blankToNull(requestedUdid);
    if(udid==null) return new ExtendedInventoryProbeResult(false,"UDID_FEHLT","UDID fehlt.",OffsetDateTime.now().toString(),null,false,Map.of(),0,System.currentTimeMillis()-started);
    if(!commandExists("idevicediagnostics")) {
      appleTrace("E4 EXTENDED", "idevicediagnostics fehlt; Health-/NAND-/IORegistry-Probe uebersprungen");
      return new ExtendedInventoryProbeResult(false,"TOOL_MISSING","idevicediagnostics ist in der GAM-lokalen Toolchain nicht vorhanden.",OffsetDateTime.now().toString(),udid,false,Map.of(),0,System.currentTimeMillis()-started);
    }
    LinkedHashMap<String,Map<String,String>> sections=new LinkedHashMap<>();
    String[][] probes={
      {"Diagnostics/All","diagnostics","All"},
      {"Diagnostics/WiFi","diagnostics","WiFi"},
      {"Diagnostics/GasGauge","diagnostics","GasGauge"},
      {"Diagnostics/NAND","diagnostics","NAND"},
      {"IORegistry/ASPStorage","ioregentry","ASPStorage"},
      {"IORegistry/AppleARMPMUCharger","ioregentry","AppleARMPMUCharger"}
    };
    int total=0;
    for(String[] probe:probes){
      boolean ioRegistry=probe[0].startsWith("IORegistry/");
      String capabilityKey=udid+"|"+probe[0];
      if(ioRegistry && APPLE_OPTIONAL_PROBE_UNSUPPORTED.contains(capabilityKey)){
        appleTrace("E4 OPTIONAL PROBE SKIP","section="+probe[0]+", reason=previously-unavailable-in-this-process");
        continue;
      }
      int timeout=ioRegistry?2:8;
      CommandResult r=runToolTraced(timeout,"E4 "+probe[0],"idevicediagnostics","-u",udid,probe[1],probe[2]);
      Map<String,String> flat=r.ok()?flattenPlistScalars(r.stdout()):Map.of();
      if(!flat.isEmpty()){
        sections.put(probe[0],Map.copyOf(flat));
        total+=flat.size();
        appleTrace("E4 EXTENDED SECTION", "section="+probe[0]+", fields="+flat.size()+", keys="+traceValue(String.join(",",flat.keySet())));
      } else if(ioRegistry){
        APPLE_OPTIONAL_PROBE_UNSUPPORTED.add(capabilityKey);
      }
    }
    writeExtendedInventorySnapshot(udid,sections);
    appleTrace("E4 EXTENDED SUMMARY","udid="+traceValue(udid)+", sections="+sections.size()+", scalarFields="+total);
    boolean ok=!sections.isEmpty();
    return new ExtendedInventoryProbeResult(ok,ok?"ERFOLGREICH":"NICHT_VERFUEGBAR",
      ok?total+" skalare Diagnose-/Health-Werte aus "+sections.size()+" Bereichen gelesen.":"Das Gerät bzw. iPadOS hat über diagnostics_relay keine auswertbaren Diagnose-/Health-Werte geliefert.",
      OffsetDateTime.now().toString(),udid,true,Map.copyOf(sections),total,System.currentTimeMillis()-started);
  }

  private Map<String,String> flattenPlistScalars(String xml){
    LinkedHashMap<String,String> out=new LinkedHashMap<>();
    if(xml==null||xml.isBlank()) return out;
    try{
      var factory=javax.xml.parsers.DocumentBuilderFactory.newInstance();
      configureSafeApplePlistFactory(factory);
      var builder=factory.newDocumentBuilder();
      builder.setEntityResolver((publicId,systemId) -> new org.xml.sax.InputSource(new java.io.StringReader("")));
      var doc=builder.parse(new org.xml.sax.InputSource(new java.io.StringReader(xml)));
      org.w3c.dom.Element root=doc.getDocumentElement();
      org.w3c.dom.Node value=null;
      for(org.w3c.dom.Node n=root.getFirstChild();n!=null;n=n.getNextSibling()) if(n.getNodeType()==org.w3c.dom.Node.ELEMENT_NODE){ value=n; break; }
      flattenPlistNode(value,"",out,0);
    }catch(Exception e){ appleTrace("E4 PLIST PARSE ERROR",e.getClass().getSimpleName()+": "+traceValue(e.getMessage())); }
    return out;
  }

  private void flattenPlistNode(org.w3c.dom.Node node,String path,Map<String,String> out,int depth){
    if(node==null||depth>24||out.size()>=2000) return;
    String name=node.getNodeName();
    if("dict".equals(name)){
      String key=null;
      for(org.w3c.dom.Node n=node.getFirstChild();n!=null;n=n.getNextSibling()){
        if(n.getNodeType()!=org.w3c.dom.Node.ELEMENT_NODE) continue;
        if("key".equals(n.getNodeName())) key=n.getTextContent();
        else if(key!=null){ String p=path.isBlank()?key:path+"/"+key; flattenPlistNode(n,p,out,depth+1); key=null; }
      }
    } else if("array".equals(name)){
      int i=0; for(org.w3c.dom.Node n=node.getFirstChild();n!=null;n=n.getNextSibling()) if(n.getNodeType()==org.w3c.dom.Node.ELEMENT_NODE) flattenPlistNode(n,path+"["+(i++)+"]",out,depth+1);
    } else if("string".equals(name)||"integer".equals(name)||"real".equals(name)||"date".equals(name)||"data".equals(name)){
      if(!path.isBlank()) out.put(path,nvlTrim(node.getTextContent()));
    } else if("true".equals(name)||"false".equals(name)){
      if(!path.isBlank()) out.put(path,name);
    }
  }

  /** 40k37e25: Apple-Plists enthalten regulaer eine DOCTYPE-Zeile. Die Deklaration darf
   * deshalb vorhanden sein, externe DTDs/Entities werden aber weiterhin strikt blockiert. */
  private void configureSafeApplePlistFactory(javax.xml.parsers.DocumentBuilderFactory factory) throws Exception {
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",false);
    factory.setFeature("http://xml.org/sax/features/external-general-entities",false);
    factory.setFeature("http://xml.org/sax/features/external-parameter-entities",false);
    try { factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd",false); } catch(Exception ignored) { }
    factory.setXIncludeAware(false);
    factory.setExpandEntityReferences(false);
    try { factory.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_DTD, ""); } catch(Exception ignored) { }
    try { factory.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_SCHEMA, ""); } catch(Exception ignored) { }
  }

  private static String nvlTrim(String s){ return s==null?"":s.trim(); }

  private void writeExtendedInventorySnapshot(String udid,Map<String,Map<String,String>> sections){
    try{
      Path logDir=findGamRoot().resolve("logs"); Files.createDirectories(logDir);
      Path file=logDir.resolve("apple-mobile-inventory-e3.json");
      LinkedHashMap<String,Object> payload=new LinkedHashMap<>();
      payload.put("generatedAt",OffsetDateTime.now().toString()); payload.put("udid",udid); payload.put("sections",sections);
      writeJsonAtomicE3(file,payload);
      appleTrace("E4 SNAPSHOT","file="+file.getFileName()+", sections="+sections.size()+", atomic=true");
    }catch(Exception e){ appleTrace("E4 SNAPSHOT ERROR",e.getClass().getSimpleName()+": "+traceValue(e.getMessage())); }
  }

  /** Nur kompakte, interessante Health-/Storage-Werte in die normale Geraetedetailzeile uebernehmen.
   * Der vollstaendige Rohbestand bleibt im d6-Snapshot und in der Probe-API erhalten. */
  private String extendedProtocolDetails(ExtendedInventoryProbeResult probe){
    if(probe==null||probe.sections()==null||probe.sections().isEmpty()) return "";
    StringBuilder b=new StringBuilder(); int added=0;
    for(var sec:probe.sections().entrySet()) for(var e:sec.getValue().entrySet()){
      String k=e.getKey().toLowerCase(Locale.ROOT), v=blankToNull(e.getValue());
      if(v==null) continue;
      boolean interesting=k.contains("capacity")||k.contains("free")||k.contains("total")||k.contains("used")||k.contains("wear")||k.contains("life")||k.contains("health")||k.contains("cycle")||k.contains("nand")||k.contains("storage")||k.contains("temperature")||k.contains("voltage")||k.contains("current")||k.contains("serial");
      if(interesting && added<40){ b.append(detail("Apple "+sec.getKey()+" / "+e.getKey(),v)); added++; }
    }
    return b.toString();
  }

  /** 40k37e25: App-Inventarisierung bevorzugt direkt über libimobiledevice/installation_proxy.
   * ideviceinstaller ist nur noch Diagnose-/Fallbackpfad und keine Voraussetzung mehr. */
  public SoftwareProbeResult probeSoftware(String udid) {
    long started=System.currentTimeMillis();
    List<SoftwareItem> all=new ArrayList<>();

    NativeInstallationProxyProbeResult nativeDirect=probeInstallationProxyNativeD14(udid);
    String selectedPath="D17_NATIVE_INSTALLATION_PROXY";

    if(!nativeDirect.items().isEmpty()) {
      all.addAll(nativeDirect.items());
      appleTrace("E4 SOFTWARE PRIMARY","nativeDirect="+nativeDirect.items().size()+", ideviceinstaller=not-needed");
      writeSoftwareDirectSnapshotD16(udid,nativeDirect,all,selectedPath);
    } else {
      appleTrace("E4 SOFTWARE FALLBACK","native installation_proxy lieferte keine Apps; ideviceinstaller-Diagnose/Fallback wird aktiviert");
      if(!ideviceInstallerUsable()) {
        ToolchainInstallResult supplement=installIdeviceInstallerSupplement();
        appleTrace("E4 SOFTWARE TOOL RESULT", "success="+supplement.success()+", state="+supplement.state()+", message="+traceValue(supplement.message()));
      }
      if(ideviceInstallerUsable()) {
        IdeviceInstallerCliMode cliMode=detectIdeviceInstallerCliMode();
        if(cliMode==IdeviceInstallerCliMode.LEGACY_1_1){
          LegacySoftwareProbeResult legacy=probeLegacySoftwareMatrix(udid);
          ModernSoftwareProbeResult modern=probeModernSoftwareMatrixD11(udid);
          InstallationProxyCliProbeResult cliMatrix=probeInstallationProxyCliMatrixD13(udid);
          List<SoftwareItem> selected=!modern.items().isEmpty()?modern.items():(!cliMatrix.items().isEmpty()?cliMatrix.items():legacy.items());
          all.addAll(selected);
          selectedPath=!modern.items().isEmpty()?"MODERN_1_2_X":(!cliMatrix.items().isEmpty()?"D17_CLI_MATRIX":"LEGACY_1_1");
          writeSoftwareComparisonSnapshotD14(udid,legacy,modern,cliMatrix,nativeDirect,selected,selectedPath);
        } else {
          collectSoftwareScope(udid,"USER",all,cliMode);
          collectSoftwareScope(udid,"SYSTEM",all,cliMode);
        }
      }
    }

    LinkedHashMap<String,SoftwareItem> unique=new LinkedHashMap<>();
    for(SoftwareItem x:all) unique.put(x.scopeType()+"\u0000"+firstNonBlank(x.bundleId(),x.name()),x);
    List<SoftwareItem> items=List.copyOf(unique.values());
    int users=(int)items.stream().filter(x->"USER".equalsIgnoreCase(x.scopeType())).count();
    int systems=(int)items.stream().filter(x->"SYSTEM".equalsIgnoreCase(x.scopeType())).count();
    int unknown=(int)items.stream().filter(x->!"USER".equalsIgnoreCase(x.scopeType())&&!"SYSTEM".equalsIgnoreCase(x.scopeType())).count();
    appleTrace("E4 SOFTWARE SUMMARY","udid="+traceValue(udid)+", user="+users+", system="+systems+", unknown="+unknown+", total="+items.size()+", selected="+selectedPath);
    if(!"D17_NATIVE_INSTALLATION_PROXY".equals(selectedPath)) writeSoftwareSnapshotD11(udid,detectIdeviceInstallerCliModeSafeD16(),items);
    boolean ok=!items.isEmpty();
    return new SoftwareProbeResult(ok,ok?"ERFOLGREICH":"LEER",items.size()+" Apps direkt/ersatzweise aus installation_proxy gelesen ("+users+" Benutzer / "+systems+" System / "+unknown+" nicht von Apple typisiert).",items,users,systems,System.currentTimeMillis()-started);
  }

  private IdeviceInstallerCliMode detectIdeviceInstallerCliModeSafeD16(){
    try { return ideviceInstallerUsable()?detectIdeviceInstallerCliMode():IdeviceInstallerCliMode.UNKNOWN; }
    catch(Exception e){ return IdeviceInstallerCliMode.UNKNOWN; }
  }

  private void writeSoftwareDirectSnapshotD16(String udid,NativeInstallationProxyProbeResult nativeDirect,List<SoftwareItem> selected,String selectedPath){
    try{
      Path logDir=findGamRoot().resolve("logs"); Files.createDirectories(logDir);
      Path file=logDir.resolve("apple-mobile-software-e3.json");
      LinkedHashMap<String,Object> payload=new LinkedHashMap<>();
      payload.put("generatedAt",OffsetDateTime.now().toString()); payload.put("udid",udid);
      payload.put("purpose","40k37e25: direkter read-only libimobiledevice-installation_proxy-Hauptpfad; ideviceinstaller nur Fallback/Diagnose");
      LinkedHashMap<String,Object> nativeData=new LinkedHashMap<>();
      nativeData.put("available",nativeDirect.available()); nativeData.put("libraryDirectory",nativeDirect.libraryDirectory()); nativeData.put("attempts",nativeDirect.attempts()); nativeData.put("items",nativeDirect.items()); nativeData.put("message",nativeDirect.message());
      payload.put("installationProxyNativeD17",nativeData); payload.put("selectedPath",selectedPath); payload.put("selectedItems",selected);
      writeJsonAtomicE3(file,payload);
      appleTrace("E4 SOFTWARE SNAPSHOT","file="+file.getFileName()+", native="+nativeDirect.items().size()+", selected="+selectedPath);
    }catch(Exception e){ appleTrace("E4 SOFTWARE SNAPSHOT ERROR",e.getClass().getSimpleName()+": "+traceValue(e.getMessage())); }
  }

  public List<SoftwareItem> consumeLatestSoftwareInventory(String identityKey){
    List<SoftwareItem> x=latestSoftwareInventory.remove(identityKey);
    return x==null?List.of():x;
  }

  private enum IdeviceInstallerCliMode { MODERN, LEGACY_1_1, UNKNOWN }
  private volatile IdeviceInstallerCliMode ideviceInstallerCliMode;

  /** 40k37e25: ideviceinstaller 1.1.x und neuere CLI-Generationen unterscheiden sich
   * bei der App-Listen-Syntax. GAM erkennt die vom lokal installierten Werkzeug
   * angebotene Syntax aus --help und kapselt sie hier, statt eine Version zu raten. */
  private IdeviceInstallerCliMode detectIdeviceInstallerCliMode(){
    IdeviceInstallerCliMode cached=ideviceInstallerCliMode;
    if(cached!=null) return cached;
    CommandResult help=runToolTraced(10,"E4 IDEVICEINSTALLER CAPABILITIES","ideviceinstaller","--help");
    String text=((help.stdout()==null?"":help.stdout())+"\n"+(help.stderr()==null?"":help.stderr())).toLowerCase(Locale.ROOT);
    IdeviceInstallerCliMode detected;
    if(text.contains("list_user") || text.contains("--list-apps") || text.contains("-l, --list-apps")) detected=IdeviceInstallerCliMode.LEGACY_1_1;
    else if(text.contains("commands:") && text.contains("--user") && text.contains("--system") && text.contains("list")) detected=IdeviceInstallerCliMode.MODERN;
    else detected=IdeviceInstallerCliMode.UNKNOWN;
    ideviceInstallerCliMode=detected;
    appleTrace("E4 IDEVICEINSTALLER CLI","mode="+detected+", helpOk="+help.ok()+", fingerprint="+traceValue(limitTrace(text)));
    return detected;
  }


  /** 40k37e25: ideviceinstaller 1.1.1 wird auf aktuellem iPadOS mit mehreren
   * dokumentierten, rein lesenden Listenvarianten geprüft. Der reale d9-Test
   * zeigte: list_user+xml liefert erfolgreich eine leere Liste, list_system+xml
   * kann dagegen hängen. Darum werden Default- und list_all-Varianten als
   * diagnostischer Fallback genutzt, ohne App-Typen aus Bundle-IDs zu erraten. */
  private LegacySoftwareProbeResult probeLegacySoftwareMatrix(String udid){
    List<SoftwareItem> items=new ArrayList<>();
    List<Map<String,Object>> attempts=new ArrayList<>();

    CommandResult user=runLegacySoftwareAttempt(attempts,udid,"USER_XML",15,"-l","-o","list_user","-o","xml");
    List<SoftwareItem> parsedUser=parseSoftwareResultIfXml(user,"USER","USER_XML");
    items.addAll(parsedUser);

    if(parsedUser.isEmpty()){
      CommandResult def=runLegacySoftwareAttempt(attempts,udid,"DEFAULT_XML",15,"-l","-o","xml");
      List<SoftwareItem> parsedDefault=parseSoftwareResultIfXml(def,"USER","DEFAULT_XML");
      if(!parsedDefault.isEmpty()){
        appleTrace("E4 SOFTWARE LEGACY FALLBACK","DEFAULT_XML lieferte "+parsedDefault.size()+" Benutzer-Apps; die 1.1.1-Dokumentation definiert User als Default.");
        items.addAll(parsedDefault);
      }
    }

    boolean haveSystem=items.stream().anyMatch(x->"SYSTEM".equalsIgnoreCase(x.scopeType()));
    CommandResult all=runLegacySoftwareAttempt(attempts,udid,"ALL_XML",20,"-l","-o","list_all","-o","xml");
    List<SoftwareItem> parsedAll=parseSoftwareResultIfXml(all,"AUTO","ALL_XML");
    if(!parsedAll.isEmpty()){
      items.addAll(parsedAll);
      haveSystem=parsedAll.stream().anyMatch(x->"SYSTEM".equalsIgnoreCase(x.scopeType()));
    }

    if(!haveSystem){
      CommandResult system=runLegacySoftwareAttempt(attempts,udid,"SYSTEM_XML",15,"-l","-o","list_system","-o","xml");
      items.addAll(parseSoftwareResultIfXml(system,"SYSTEM","SYSTEM_XML"));
    }

    if(items.isEmpty()){
      runLegacySoftwareAttempt(attempts,udid,"USER_TEXT",12,"-l","-o","list_user");
      runLegacySoftwareAttempt(attempts,udid,"DEFAULT_TEXT",12,"-l");
      runLegacySoftwareAttempt(attempts,udid,"ALL_TEXT",15,"-l","-o","list_all");
    }

    LinkedHashMap<String,SoftwareItem> unique=new LinkedHashMap<>();
    for(SoftwareItem x:items) unique.put(x.scopeType()+"\u0000"+firstNonBlank(x.bundleId(),x.name()),x);
    List<SoftwareItem> result=List.copyOf(unique.values());
    int users=(int)result.stream().filter(x->"USER".equalsIgnoreCase(x.scopeType())).count();
    int systems=(int)result.stream().filter(x->"SYSTEM".equalsIgnoreCase(x.scopeType())).count();
    writeLegacySoftwareAttemptsSnapshotD11(udid,attempts,result);
    appleTrace("E4 SOFTWARE LEGACY MATRIX","attempts="+attempts.size()+", user="+users+", system="+systems+", total="+result.size());
    return new LegacySoftwareProbeResult(result,users,systems);
  }

  private CommandResult runLegacySoftwareAttempt(List<Map<String,Object>> attempts,String udid,String name,int timeout,String... args){
    List<String> cmd=new ArrayList<>();
    cmd.add("-u"); cmd.add(udid); cmd.addAll(Arrays.asList(args));
    long started=System.currentTimeMillis();
    CommandResult r=runToolTraced(timeout,"E4 IDEVICEINSTALLER LEGACY PROBE "+name,"ideviceinstaller",cmd.toArray(String[]::new));
    long duration=System.currentTimeMillis()-started;
    LinkedHashMap<String,Object> a=new LinkedHashMap<>();
    a.put("name",name); a.put("ok",r.ok()); a.put("exitCode",r.exitCode()); a.put("durationMs",duration);
    a.put("stdoutBlank",r.stdout()==null||r.stdout().isBlank());
    a.put("stderr",limitTrace(r.stderr()));
    a.put("stdoutPreview",limitTrace(r.stdout()));
    attempts.add(a);
    appleTrace("E4 SOFTWARE LEGACY ATTEMPT","name="+name+", ok="+r.ok()+", exitCode="+r.exitCode()+", durationMs="+duration+", stdoutBlank="+(r.stdout()==null||r.stdout().isBlank()));
    return r;
  }

  private List<SoftwareItem> parseSoftwareResultIfXml(CommandResult r,String scope,String probeName){
    if(r==null||!r.ok()||r.stdout()==null||r.stdout().isBlank()) return List.of();
    String x=r.stdout().trim();
    if(!x.startsWith("<?xml")&&!x.contains("<plist")){
      appleTrace("E4 SOFTWARE LEGACY NONXML","probe="+probeName+", preview="+traceValue(limitTrace(x)));
      return List.of();
    }
    List<SoftwareItem> parsed=parseSoftwarePlist(x,scope);
    appleTrace("E4 SOFTWARE LEGACY PARSED","probe="+probeName+", scope="+scope+", entries="+parsed.size());
    return parsed;
  }

  private void writeLegacySoftwareAttemptsSnapshotD11(String udid,List<Map<String,Object>> attempts,List<SoftwareItem> items){
    try{
      Path logDir=findGamRoot().resolve("logs"); Files.createDirectories(logDir);
      Path file=logDir.resolve("apple-mobile-software-e3-legacy.json");
      LinkedHashMap<String,Object> payload=new LinkedHashMap<>();
      payload.put("generatedAt",OffsetDateTime.now().toString());
      payload.put("udid",udid); payload.put("cli","LEGACY_1_1"); payload.put("attempts",attempts); payload.put("items",items);
      writeJsonAtomicE3(file,payload);
      appleTrace("E4 SOFTWARE SNAPSHOT","file="+file.getFileName()+", attempts="+attempts.size()+", items="+items.size());
    }catch(Exception e){ appleTrace("E4 SOFTWARE SNAPSHOT ERROR",e.getClass().getSimpleName()+": "+traceValue(e.getMessage())); }
  }

  private void writeSoftwareSnapshotD11(String udid,IdeviceInstallerCliMode mode,List<SoftwareItem> items){
    if(mode==IdeviceInstallerCliMode.LEGACY_1_1) return; // Legacy-Matrix schreibt bereits die ausführlichere Datei.
    try{
      Path logDir=findGamRoot().resolve("logs"); Files.createDirectories(logDir);
      Path file=logDir.resolve("apple-mobile-software-e3.json");
      LinkedHashMap<String,Object> payload=new LinkedHashMap<>();
      payload.put("generatedAt",OffsetDateTime.now().toString()); payload.put("udid",udid); payload.put("cli",String.valueOf(mode)); payload.put("items",items);
      writeJsonAtomicE3(file,payload);
      appleTrace("E4 SOFTWARE SNAPSHOT","file="+file.getFileName()+", items="+items.size());
    }catch(Exception e){ appleTrace("E4 SOFTWARE SNAPSHOT ERROR",e.getClass().getSimpleName()+": "+traceValue(e.getMessage())); }
  }

  private record LegacySoftwareProbeResult(List<SoftwareItem> items,int userCount,int systemCount) {}

  private record ModernSoftwareProbeResult(boolean available,String version,String toolPath,List<Map<String,Object>> attempts,List<SoftwareItem> items,int userCount,int systemCount,String message) {}

  /** 40k37e25: isolierter moderner A/B-Pfad. Die bestehende 1.1.1-Runtime wird weder ersetzt noch
   * in PATH umsortiert. Die neue Suite liegt separat unter tools/libimobiledevice/ideviceinstaller-modern-d11. */
  private ModernSoftwareProbeResult probeModernSoftwareMatrixD11(String udid){
    List<Map<String,Object>> attempts=new ArrayList<>();
    try{
      Path tool=ensureModernIdeviceInstallerD11();
      if(tool==null) return new ModernSoftwareProbeResult(false,"", "",attempts,List.of(),0,0,"Moderne Suite nicht verfügbar.");
      CommandResult ver=run(10,tool.toString(),"--version");
      CommandResult help=run(10,tool.toString(),"--help");
      String fingerprint=((ver.stdout()==null?"":ver.stdout())+" "+(ver.stderr()==null?"":ver.stderr())+" "+(help.stdout()==null?"":help.stdout())+" "+(help.stderr()==null?"":help.stderr())).toLowerCase(Locale.ROOT);
      boolean modern=help.ok() && fingerprint.contains("list") && fingerprint.contains("--user") && fingerprint.contains("--system");
      String version=firstNonBlank(blankToNull(ver.stdout()),blankToNull(ver.stderr()),"unknown");
      appleTrace("E4 MODERN SUITE CAPABILITIES","tool="+traceValue(tool.toString())+", version="+traceValue(limitTrace(version))+", modernCli="+modern);
      if(!modern) return new ModernSoftwareProbeResult(false,version,tool.toString(),attempts,List.of(),0,0,"Geladene Suite bietet nicht die erwartete moderne list-CLI.");

      List<SoftwareItem> items=new ArrayList<>();
      CommandResult user=runModernSoftwareAttemptD11(attempts,tool,udid,"USER_XML",20,"list","--user","--xml");
      List<SoftwareItem> parsedUser=parseSoftwareResultIfXml(user,"USER","MODERN_USER_XML");
      items.addAll(parsedUser);
      if(parsedUser.isEmpty()){
        CommandResult def=runModernSoftwareAttemptD11(attempts,tool,udid,"DEFAULT_XML",20,"list","--xml");
        items.addAll(parseSoftwareResultIfXml(def,"USER","MODERN_DEFAULT_XML"));
      }
      CommandResult all=runModernSoftwareAttemptD11(attempts,tool,udid,"ALL_XML",20,"list","--all","--xml");
      items.addAll(parseSoftwareResultIfXml(all,"AUTO","MODERN_ALL_XML"));
      boolean haveSystem=items.stream().anyMatch(x->"SYSTEM".equalsIgnoreCase(x.scopeType()));
      if(!haveSystem){
        CommandResult system=runModernSoftwareAttemptD11(attempts,tool,udid,"SYSTEM_XML",15,"list","--system","--xml");
        items.addAll(parseSoftwareResultIfXml(system,"SYSTEM","MODERN_SYSTEM_XML"));
      }
      if(items.isEmpty()){
        runModernSoftwareAttemptD11(attempts,tool,udid,"USER_TEXT",12,"list","--user");
        runModernSoftwareAttemptD11(attempts,tool,udid,"DEFAULT_TEXT",12,"list");
        runModernSoftwareAttemptD11(attempts,tool,udid,"ALL_TEXT",15,"list","--all");
      }
      LinkedHashMap<String,SoftwareItem> unique=new LinkedHashMap<>();
      for(SoftwareItem x:items) unique.put(x.scopeType()+"\u0000"+firstNonBlank(x.bundleId(),x.name()),x);
      List<SoftwareItem> result=List.copyOf(unique.values());
      int users=(int)result.stream().filter(x->"USER".equalsIgnoreCase(x.scopeType())).count();
      int systems=(int)result.stream().filter(x->"SYSTEM".equalsIgnoreCase(x.scopeType())).count();
      appleTrace("E4 SOFTWARE MODERN MATRIX","attempts="+attempts.size()+", user="+users+", system="+systems+", total="+result.size());
      return new ModernSoftwareProbeResult(true,version,tool.toString(),attempts,result,users,systems,"Moderner 1.2.x-Testpfad ausgeführt.");
    }catch(Exception e){
      appleTrace("E4 SOFTWARE MODERN ERROR",e.getClass().getSimpleName()+": "+traceValue(e.getMessage()));
      return new ModernSoftwareProbeResult(false,"","",attempts,List.of(),0,0,firstNonBlank(e.getMessage(),e.getClass().getSimpleName()));
    }
  }

  private CommandResult runModernSoftwareAttemptD11(List<Map<String,Object>> attempts,Path tool,String udid,String name,int timeout,String... args){
    List<String> cmd=new ArrayList<>();
    cmd.add("-u"); cmd.add(udid); cmd.addAll(Arrays.asList(args));
    long started=System.currentTimeMillis();
    List<String> argv=new ArrayList<>(); argv.add(tool.toString()); argv.addAll(cmd);
    CommandResult r=run(timeout,argv.toArray(String[]::new));
    long duration=System.currentTimeMillis()-started;
    LinkedHashMap<String,Object> a=new LinkedHashMap<>();
    a.put("name",name); a.put("ok",r.ok()); a.put("exitCode",r.exitCode()); a.put("durationMs",duration);
    a.put("stdoutBlank",r.stdout()==null||r.stdout().isBlank()); a.put("stderr",limitTrace(r.stderr())); a.put("stdoutPreview",limitTrace(r.stdout()));
    attempts.add(a);
    appleTrace("E4 SOFTWARE MODERN ATTEMPT","name="+name+", ok="+r.ok()+", exitCode="+r.exitCode()+", durationMs="+duration+", stdoutBlank="+(r.stdout()==null||r.stdout().isBlank()));
    return r;
  }

  private Path ensureModernIdeviceInstallerD11() throws Exception{
    if(!System.getProperty("os.name","").toLowerCase(Locale.ROOT).contains("win")) return null;
    Path root=findGamRoot().toAbsolutePath().normalize();
    Path target=root.resolve("tools").resolve("libimobiledevice").resolve("ideviceinstaller-modern-d11");
    Path existing=target.resolve("ideviceinstaller.exe");
    if(Files.isRegularFile(existing)){
      CommandResult smoke=run(10,existing.toString(),"--help");
      if(smoke.ok()){ appleTrace("E4 MODERN SUITE","source=existing, tool="+traceValue(existing.toString())); return existing; }
    }
    Path cache=root.resolve("tools").resolve("libimobiledevice").resolve("ideviceinstaller-cache").resolve("d11-modern");
    Files.createDirectories(cache); Files.createDirectories(target.getParent());
    Path archive=cache.resolve("libimobile-suite-latest_x86_64-mingw64.tar.xz");
    if(!Files.isRegularFile(archive) || !D11_MODERN_SUITE_SHA256.equalsIgnoreCase(sha256(archive))){
      HttpClient client=HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(java.time.Duration.ofSeconds(20)).build();
      Path tmp=Files.createTempFile("gam-d11-modern-suite-", ".tar.xz");
      try{
        downloadTo(client,D11_MODERN_SUITE_URL,tmp,120);
        String actual=sha256(tmp);
        if(!D11_MODERN_SUITE_SHA256.equalsIgnoreCase(actual)) throw new SecurityException("SHA-256-Prüfung der d11-Modern-Suite fehlgeschlagen: "+actual);
        Files.copy(tmp,archive,StandardCopyOption.REPLACE_EXISTING);
      }finally{ try{Files.deleteIfExists(tmp);}catch(Exception ignored){} }
    }
    String cachedSha=sha256(archive);
    if(!D11_MODERN_SUITE_SHA256.equalsIgnoreCase(cachedSha)) throw new SecurityException("Ungültiger d11-Modern-Suite-Cache.");
    Path stage=Files.createTempDirectory("gam-d11-modern-suite-");
    Path fresh=target.resolveSibling("ideviceinstaller-modern-d11-new");
    try{
      CommandResult extract=run(90,"tar.exe","-xf",archive.toString(),"-C",stage.toString());
      if(!extract.ok()) throw new IllegalStateException("d11-Modern-Suite konnte nicht entpackt werden: "+firstNonBlank(extract.stderr(),extract.stdout()));
      Path exe=null;
      try(var walk=Files.walk(stage,8)){ exe=walk.filter(Files::isRegularFile).filter(p->"ideviceinstaller.exe".equalsIgnoreCase(p.getFileName().toString())).findFirst().orElse(null); }
      if(exe==null) throw new IllegalStateException("d11-Modern-Suite enthält ideviceinstaller.exe nicht.");
      Path bin=exe.getParent();
      deleteRecursively(fresh); copyDirectory(bin,fresh);
      Path candidate=fresh.resolve("ideviceinstaller.exe");
      CommandResult smoke=run(12,candidate.toString(),"--help");
      if(!smoke.ok()) throw new IllegalStateException("d11-Modern-Suite startet nicht: ExitCode "+formatExitCode(smoke.exitCode()));
      String h=((smoke.stdout()==null?"":smoke.stdout())+" "+(smoke.stderr()==null?"":smoke.stderr())).toLowerCase(Locale.ROOT);
      if(!(h.contains("list")&&h.contains("--user")&&h.contains("--system"))) throw new IllegalStateException("d11-Modern-Suite enthält nicht die erwartete moderne ideviceinstaller-CLI.");
      deleteRecursively(target);
      try{Files.move(fresh,target,StandardCopyOption.ATOMIC_MOVE);}catch(Exception e){Files.move(fresh,target,StandardCopyOption.REPLACE_EXISTING);}
      appleTrace("E4 MODERN SUITE","source=download, label="+D11_MODERN_SUITE_LABEL+", sha256Verified=true, tool="+traceValue(target.resolve("ideviceinstaller.exe").toString()));
      return target.resolve("ideviceinstaller.exe");
    }finally{ try{deleteRecursively(stage);}catch(Exception ignored){} try{if(Files.exists(fresh))deleteRecursively(fresh);}catch(Exception ignored){} }
  }

  private void writeSoftwareComparisonSnapshotD14(String udid,LegacySoftwareProbeResult legacy,ModernSoftwareProbeResult modern,InstallationProxyCliProbeResult cliMatrix,NativeInstallationProxyProbeResult nativeDirect,List<SoftwareItem> selected,String selectedPath){
    try{
      Path logDir=findGamRoot().resolve("logs"); Files.createDirectories(logDir);
      Path file=logDir.resolve("apple-mobile-software-e3.json");
      LinkedHashMap<String,Object> payload=new LinkedHashMap<>();
      payload.put("generatedAt",OffsetDateTime.now().toString()); payload.put("udid",udid);
      payload.put("purpose","Vergleich ideviceinstaller 1.1.1/1.2.x plus direkter read-only libimobiledevice-installation_proxy-Zugriff");
      payload.put("legacy",Map.of("cli","LEGACY_1_1","items",legacy.items(),"userCount",legacy.userCount(),"systemCount",legacy.systemCount()));
      LinkedHashMap<String,Object> modernData=new LinkedHashMap<>();
      modernData.put("available",modern.available()); modernData.put("version",modern.version()); modernData.put("toolPath",modern.toolPath()); modernData.put("suite",D11_MODERN_SUITE_LABEL); modernData.put("suiteSha256",D11_MODERN_SUITE_SHA256); modernData.put("attempts",modern.attempts()); modernData.put("items",modern.items()); modernData.put("userCount",modern.userCount()); modernData.put("systemCount",modern.systemCount()); modernData.put("message",modern.message());
      payload.put("modern",modernData);
      LinkedHashMap<String,Object> rawData=new LinkedHashMap<>();
      rawData.put("available",cliMatrix.available()); rawData.put("toolPath",cliMatrix.toolPath()); rawData.put("attempts",cliMatrix.attempts()); rawData.put("items",cliMatrix.items()); rawData.put("message",cliMatrix.message());
      payload.put("installationProxyCliD17",rawData);
      LinkedHashMap<String,Object> nativeData=new LinkedHashMap<>();
      nativeData.put("available",nativeDirect.available()); nativeData.put("libraryDirectory",nativeDirect.libraryDirectory()); nativeData.put("attempts",nativeDirect.attempts()); nativeData.put("items",nativeDirect.items()); nativeData.put("message",nativeDirect.message());
      payload.put("installationProxyNativeD17",nativeData); payload.put("selectedPath",selectedPath); payload.put("selectedItems",selected);
      writeJsonAtomicE3(file,payload);
      appleTrace("E4 SOFTWARE SNAPSHOT","file="+file.getFileName()+", legacy="+legacy.items().size()+", modern="+modern.items().size()+", selected="+selectedPath);
    }catch(Exception e){ appleTrace("E4 SOFTWARE SNAPSHOT ERROR",e.getClass().getSimpleName()+": "+traceValue(e.getMessage())); }
  }


  private record NativeInstallationProxyProbeResult(boolean available,String libraryDirectory,List<Map<String,Object>> attempts,List<SoftwareItem> items,String message) {}
  private record NativeD14AttemptResult(String name,boolean ok,boolean timeout,int errorCode,String xml,String error,long durationMs) {}

  /** 40k37e25: direkter, rein lesender Zugriff auf libimobiledevice installation_proxy.
   * ideviceinstaller.exe ist hier bewusst nicht beteiligt. Die DLLs stammen aus derselben
   * bereits SHA256-verifizierten modernen d11-Suite. */
  private NativeInstallationProxyProbeResult probeInstallationProxyNativeD14(String udid){
    List<Map<String,Object>> attempts=new ArrayList<>();
    try{
      Path tool=ensureModernIdeviceInstallerD11();
      if(tool==null) return new NativeInstallationProxyProbeResult(false,"",attempts,List.of(),"Moderne Windows-Suite nicht verfügbar.");
      Path dir=tool.getParent();
      Path mobile=findNativeDllD14(dir,"libimobiledevice-1.0.dll","libimobiledevice.dll");
      Path plist=findNativeDllD14(dir,"libplist-2.0.dll","libplist.dll");
      if(mobile==null||plist==null){
        String msg="Native DLLs fehlen: libimobiledevice="+(mobile!=null)+", libplist="+(plist!=null);
        appleTrace("E4 NATIVE INSTALLATION_PROXY UNAVAILABLE",msg+", dir="+traceValue(dir.toString()));
        writeInstallationProxyNativeSnapshotD14(udid,dir,attempts,List.of(),msg);
        return new NativeInstallationProxyProbeResult(false,dir.toString(),attempts,List.of(),msg);
      }
      Path rawDir=findGamRoot().resolve("logs").resolve("apple-mobile-installation-proxy-native-e3");
      recreateDiagnosticDirectoryD16(rawDir);
      List<SoftwareItem> recovered=new ArrayList<>();

      // d16 Produktionspfad: Ein Browse mit ApplicationType=Any und expliziten
      // Rueckgabeattributen reicht auf dem realen iPad fuer System- UND User-Apps.
      // USER/SYSTEM wird weiterhin ausschließlich aus Apples ApplicationType gelesen.
      NativeD14AttemptResult anyAttrs=runNativeD14Timed(dir,mobile,plist,udid,"BROWSE_ANY_EXPLICIT",12,"BROWSE","Any",true);
      recordNativeAttemptD14(attempts,rawDir,anyAttrs);
      recovered.addAll(parseSoftwareResultXmlD14(anyAttrs.xml(),"AUTO","D17_NATIVE_BROWSE_ANY_EXPLICIT"));

      // Nur wenn der regulaere Any-Browse keine App-Daten liefert, folgen die
      // diagnostischen/fallback-faehigen Varianten. So bleibt der Normalpfad klein.
      if(recovered.isEmpty()){
        appleTrace("E4 NATIVE INSTALLATION_PROXY FALLBACK","BROWSE_ANY_EXPLICIT lieferte keine Apps; erweiterte read-only Diagnose wird aktiviert");

        NativeD14AttemptResult userAttrs=runNativeD14Timed(dir,mobile,plist,udid,"BROWSE_USER_EXPLICIT",12,"BROWSE","User",true);
        recordNativeAttemptD14(attempts,rawDir,userAttrs);
        recovered.addAll(parseSoftwareResultXmlD14(userAttrs.xml(),"USER","D17_NATIVE_BROWSE_USER_EXPLICIT"));

        NativeD14AttemptResult lookupAll=runNativeD14Timed(dir,mobile,plist,udid,"LOOKUP_ALL",12,"LOOKUP",null,false);
        recordNativeAttemptD14(attempts,rawDir,lookupAll);
        recovered.addAll(parseSoftwareResultXmlD14(lookupAll.xml(),"AUTO","D17_NATIVE_LOOKUP_ALL"));

        NativeD14AttemptResult nullBrowse=runNativeD14Timed(dir,mobile,plist,udid,"BROWSE_NULL_OPTIONS",10,"BROWSE",null,false);
        recordNativeAttemptD14(attempts,rawDir,nullBrowse);
        recovered.addAll(parseSoftwareResultXmlD14(nullBrowse.xml(),"AUTO","D17_NATIVE_BROWSE_NULL_OPTIONS"));
      }

      LinkedHashMap<String,SoftwareItem> unique=new LinkedHashMap<>();
      for(SoftwareItem x:recovered) unique.put(x.scopeType()+"\u0000"+firstNonBlank(x.bundleId(),x.name()),x);
      List<SoftwareItem> items=List.copyOf(unique.values());
      int users=(int)items.stream().filter(x->"USER".equalsIgnoreCase(x.scopeType())).count();
      int systems=(int)items.stream().filter(x->"SYSTEM".equalsIgnoreCase(x.scopeType())).count();
      int unknown=items.size()-users-systems;
      String msg="Direkter libimobiledevice-installation_proxy-Produktionspfad abgeschlossen: "+items.size()+" Apps ("+users+" Benutzer / "+systems+" System / "+unknown+" nicht typisiert).";
      writeInstallationProxyNativeSnapshotD14(udid,dir,attempts,items,msg);
      zipDiagnosticDirectoryD16(rawDir,findGamRoot().resolve("logs").resolve("apple-mobile-installation-proxy-native-e3.zip"));
      appleTrace("E4 NATIVE INSTALLATION_PROXY SUMMARY","attempts="+attempts.size()+", recovered="+items.size()+", user="+users+", system="+systems+", unknown="+unknown+", libDir="+traceValue(dir.toString()));
      return new NativeInstallationProxyProbeResult(true,dir.toString(),attempts,items,msg);
    }catch(Throwable e){
      String msg=e.getClass().getSimpleName()+": "+firstNonBlank(e.getMessage(),"ohne Detailmeldung");
      appleTrace("E4 NATIVE INSTALLATION_PROXY ERROR",traceValue(msg));
      return new NativeInstallationProxyProbeResult(false,"",attempts,List.of(),msg);
    }
  }

  private Path findNativeDllD14(Path dir,String... names){
    if(dir==null||!Files.isDirectory(dir)) return null;
    for(String name:names){ Path p=dir.resolve(name); if(Files.isRegularFile(p)) return p; }
    try(var walk=Files.list(dir)){
      List<Path> dlls=walk.filter(Files::isRegularFile).filter(p->p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".dll")).toList();
      for(Path p:dlls){
        String n=p.getFileName().toString().toLowerCase(Locale.ROOT);
        for(String wanted:names){
          String stem=wanted.toLowerCase(Locale.ROOT).replace(".dll","");
          if(n.startsWith(stem)) return p;
        }
      }
    }catch(Exception ignored){}
    return null;
  }

  private NativeD14AttemptResult runNativeD14Timed(Path libDir,Path mobileDll,Path plistDll,String udid,String name,int timeoutSec,String operation,String applicationType,boolean returnAttributes){
    ExecutorService executor=Executors.newSingleThreadExecutor(r->{ Thread t=new Thread(r,"gam-d14-instproxy-"+name.toLowerCase(Locale.ROOT)); t.setDaemon(true); return t; });
    long started=System.currentTimeMillis();
    Future<NativeD14AttemptResult> future=executor.submit(()->runNativeD14Once(libDir,mobileDll,plistDll,udid,name,operation,applicationType,returnAttributes));
    try{
      return future.get(timeoutSec,TimeUnit.SECONDS);
    }catch(TimeoutException e){
      future.cancel(true);
      return new NativeD14AttemptResult(name,false,true,Integer.MIN_VALUE,"","Timeout after "+timeoutSec+"s",System.currentTimeMillis()-started);
    }catch(Exception e){
      return new NativeD14AttemptResult(name,false,false,-1,"",firstNonBlank(e.getMessage(),e.getClass().getSimpleName()),System.currentTimeMillis()-started);
    }finally{
      executor.shutdownNow();
    }
  }

  private NativeD14AttemptResult runNativeD14Once(Path libDir,Path mobileDll,Path plistDll,String udid,String name,String operation,String applicationType,boolean returnAttributes){
    long started=System.currentTimeMillis();
    Pointer device=null,client=null,options=null,result=null;
    NativeLibrary mobile=null,plist=null;
    int err=0;
    try{
      NativeLibrary.addSearchPath("imobiledevice_d14",libDir.toString());
      NativeLibrary.addSearchPath("plist_d14",libDir.toString());
      if(System.getProperty("os.name","").toLowerCase(Locale.ROOT).contains("win")){
        try{
          NativeLibrary kernel=NativeLibrary.getInstance("kernel32");
          kernel.getFunction("SetDllDirectoryW").invokeInt(new Object[]{new WString(libDir.toString())});
        }catch(Throwable ignored){}
      }
      mobile=NativeLibrary.getInstance(mobileDll.toAbsolutePath().toString());
      plist=NativeLibrary.getInstance(plistDll.toAbsolutePath().toString());
      Function ideviceNew=mobile.getFunction("idevice_new_with_options");
      Function startService=mobile.getFunction("instproxy_client_start_service");
      Function browse=mobile.getFunction("instproxy_browse");
      Function lookup=mobile.getFunction("instproxy_lookup");
      Function plistNewDict=plist.getFunction("plist_new_dict");
      Function plistNewString=plist.getFunction("plist_new_string");
      Function plistNewArray=plist.getFunction("plist_new_array");
      Function plistDictSet=plist.getFunction("plist_dict_set_item");
      Function plistArrayAppend=plist.getFunction("plist_array_append_item");
      Function plistToXml=plist.getFunction("plist_to_xml");
      Function plistMemFree=plist.getFunction("plist_mem_free");

      PointerByReference deviceRef=new PointerByReference();
      err=ideviceNew.invokeInt(new Object[]{deviceRef,udid,6});
      if(err!=0||deviceRef.getValue()==null) return new NativeD14AttemptResult(name,false,false,err,"","idevice_new_with_options failed: "+err,System.currentTimeMillis()-started);
      device=deviceRef.getValue();
      PointerByReference clientRef=new PointerByReference();
      err=startService.invokeInt(new Object[]{device,clientRef,"GAM-40k37e25"});
      if(err!=0||clientRef.getValue()==null) return new NativeD14AttemptResult(name,false,false,err,"","instproxy_client_start_service failed: "+err,System.currentTimeMillis()-started);
      client=clientRef.getValue();

      if(applicationType!=null||returnAttributes){
        options=plistNewDict.invokePointer(new Object[]{});
        if(applicationType!=null){
          Pointer v=plistNewString.invokePointer(new Object[]{applicationType});
          plistDictSet.invokeVoid(new Object[]{options,"ApplicationType",v});
        }
        if(returnAttributes){
          Pointer attrs=plistNewArray.invokePointer(new Object[]{});
          for(String attr:List.of("CFBundleIdentifier","CFBundleDisplayName","CFBundleShortVersionString","CFBundleVersion","ApplicationType","ApplicationTypeString","Path","Container")){
            Pointer v=plistNewString.invokePointer(new Object[]{attr});
            plistArrayAppend.invokeVoid(new Object[]{attrs,v});
          }
          plistDictSet.invokeVoid(new Object[]{options,"ReturnAttributes",attrs});
        }
      }

      PointerByReference resultRef=new PointerByReference();
      if("LOOKUP".equals(operation)) err=lookup.invokeInt(new Object[]{client,null,options,resultRef});
      else err=browse.invokeInt(new Object[]{client,options,resultRef});
      result=resultRef.getValue();
      if(err!=0||result==null) return new NativeD14AttemptResult(name,false,false,err,"","installation_proxy "+operation+" failed: "+err,System.currentTimeMillis()-started);

      PointerByReference xmlRef=new PointerByReference();
      IntByReference lenRef=new IntByReference();
      int plistErr=plistToXml.invokeInt(new Object[]{result,xmlRef,lenRef});
      Pointer xmlPtr=xmlRef.getValue();
      String xml="";
      if(plistErr==0&&xmlPtr!=null){
        int len=Math.max(0,lenRef.getValue());
        byte[] bytes=xmlPtr.getByteArray(0,len);
        xml=new String(bytes,StandardCharsets.UTF_8);
        plistMemFree.invokeVoid(new Object[]{xmlPtr});
      }
      if(plistErr!=0) return new NativeD14AttemptResult(name,false,false,plistErr,"","plist_to_xml failed: "+plistErr,System.currentTimeMillis()-started);
      return new NativeD14AttemptResult(name,true,false,0,xml,"",System.currentTimeMillis()-started);
    }catch(Throwable e){
      return new NativeD14AttemptResult(name,false,false,err,"",e.getClass().getSimpleName()+": "+firstNonBlank(e.getMessage(),"native call failed"),System.currentTimeMillis()-started);
    }finally{
      try{ if(result!=null&&plist!=null) plist.getFunction("plist_free").invokeVoid(new Object[]{result}); }catch(Throwable ignored){}
      try{ if(options!=null&&plist!=null) plist.getFunction("plist_free").invokeVoid(new Object[]{options}); }catch(Throwable ignored){}
      try{ if(client!=null&&mobile!=null) mobile.getFunction("instproxy_client_free").invokeInt(new Object[]{client}); }catch(Throwable ignored){}
      try{ if(device!=null&&mobile!=null) mobile.getFunction("idevice_free").invokeInt(new Object[]{device}); }catch(Throwable ignored){}
      try{ if(plist!=null) plist.close(); }catch(Throwable ignored){}
      try{ if(mobile!=null) mobile.close(); }catch(Throwable ignored){}
    }
  }

  private void recordNativeAttemptD14(List<Map<String,Object>> attempts,Path rawDir,NativeD14AttemptResult r){
    String safe=r.name().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]+","-");
    Path xmlFile=rawDir.resolve(safe+".plist.xml");
    Path errFile=rawDir.resolve(safe+".error.txt");
    try{
      Files.writeString(xmlFile,r.xml()==null?"":r.xml(),StandardCharsets.UTF_8,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING);
      Files.writeString(errFile,r.error()==null?"":r.error(),StandardCharsets.UTF_8,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING);
    }catch(Exception e){ appleTrace("E4 NATIVE INSTALLATION_PROXY WRITE ERROR","name="+r.name()+", error="+traceValue(e.getMessage())); }
    LinkedHashMap<String,Object> a=new LinkedHashMap<>();
    a.put("name",r.name()); a.put("ok",r.ok()); a.put("timeout",r.timeout()); a.put("errorCode",r.errorCode()); a.put("durationMs",r.durationMs());
    a.put("xmlBytes",r.xml()==null?0:r.xml().getBytes(StandardCharsets.UTF_8).length); a.put("xmlFile",xmlFile.getFileName().toString()); a.put("errorFile",errFile.getFileName().toString());
    a.put("xmlPreview",limitTrace(r.xml())); a.put("error",limitTrace(r.error()));
    a.put("xmlArrayEmpty",r.xml()!=null&&r.xml().contains("<array/>")); a.put("xmlHasDict",r.xml()!=null&&r.xml().contains("<dict>"));
    attempts.add(a);
    appleTrace("E4 NATIVE INSTALLATION_PROXY ATTEMPT","name="+r.name()+", ok="+r.ok()+", timeout="+r.timeout()+", errorCode="+r.errorCode()+", durationMs="+r.durationMs()+", xmlBytes="+a.get("xmlBytes"));
  }

  private List<SoftwareItem> parseSoftwareResultXmlD14(String xml,String scope,String probe){
    if(xml==null||xml.isBlank()) return List.of();
    List<SoftwareItem> normal=parseSoftwarePlist(xml,scope);
    if(!normal.isEmpty()) return normal;
    List<SoftwareItem> out=new ArrayList<>();
    try{
      var factory=javax.xml.parsers.DocumentBuilderFactory.newInstance(); configureSafeApplePlistFactory(factory);
      var builder=factory.newDocumentBuilder(); builder.setEntityResolver((publicId,systemId)->new org.xml.sax.InputSource(new java.io.StringReader("")));
      var doc=builder.parse(new org.xml.sax.InputSource(new java.io.StringReader(xml)));
      var rootDicts=doc.getElementsByTagName("dict");
      if(rootDicts.getLength()>0){
        org.w3c.dom.Node root=rootDicts.item(0); String pendingKey=null;
        for(org.w3c.dom.Node n=root.getFirstChild();n!=null;n=n.getNextSibling()){
          if(n.getNodeType()!=org.w3c.dom.Node.ELEMENT_NODE) continue;
          if("key".equals(n.getNodeName())){ pendingKey=n.getTextContent(); continue; }
          if(pendingKey!=null&&"dict".equals(n.getNodeName())){
            Map<String,String> m=parseDirectDictD14(n);
            String bundle=firstNonBlank(m.get("CFBundleIdentifier"),pendingKey);
            String type=scope;
            String appleType=firstNonBlank(m.get("ApplicationType"),m.get("ApplicationTypeString"));
            if("System".equalsIgnoreCase(appleType)) type="SYSTEM"; else if("User".equalsIgnoreCase(appleType)) type="USER"; else if("AUTO".equalsIgnoreCase(type)) type="UNKNOWN";
            if(!bundle.isBlank()) out.add(new SoftwareItem(type,firstNonBlank(m.get("CFBundleDisplayName"),m.get("CFBundleName"),bundle),firstNonBlank(m.get("CFBundleShortVersionString"),m.get("CFBundleVersion")),bundle,blankToNull(m.get("Path")),"Apple installation_proxy · direct libimobiledevice"));
            pendingKey=null;
          }else pendingKey=null;
        }
      }
    }catch(Exception e){ appleTrace("E4 NATIVE SOFTWARE PARSE ERROR","probe="+probe+", error="+traceValue(e.getMessage())); }
    appleTrace("E4 NATIVE SOFTWARE PARSED","probe="+probe+", scope="+scope+", entries="+out.size());
    return out;
  }

  private Map<String,String> parseDirectDictD14(org.w3c.dom.Node d){
    LinkedHashMap<String,String> m=new LinkedHashMap<>(); String key=null;
    for(org.w3c.dom.Node n=d.getFirstChild();n!=null;n=n.getNextSibling()){
      if(n.getNodeType()!=org.w3c.dom.Node.ELEMENT_NODE) continue;
      if("key".equals(n.getNodeName())) key=n.getTextContent();
      else if(key!=null){ if("string".equals(n.getNodeName())||"integer".equals(n.getNodeName())) m.put(key,n.getTextContent()); key=null; }
    }
    return m;
  }

  private void writeInstallationProxyNativeSnapshotD14(String udid,Path libDir,List<Map<String,Object>> attempts,List<SoftwareItem> items,String message){
    try{
      Path file=findGamRoot().resolve("logs").resolve("apple-mobile-installation-proxy-native-e3.json");
      LinkedHashMap<String,Object> payload=new LinkedHashMap<>();
      payload.put("generatedAt",OffsetDateTime.now().toString()); payload.put("udid",udid);
      payload.put("purpose","Direkter read-only Zugriff auf libimobiledevice installation_proxy ohne ideviceinstaller.exe");
      payload.put("libraryDirectory",libDir==null?"":libDir.toString()); payload.put("suite",D11_MODERN_SUITE_LABEL); payload.put("suiteSha256",D11_MODERN_SUITE_SHA256);
      payload.put("rawArchive",findGamRoot().resolve("logs").resolve("apple-mobile-installation-proxy-native-e3.zip").toString());
      payload.put("attempts",attempts); payload.put("items",items);
      payload.put("userCount",items.stream().filter(x->"USER".equalsIgnoreCase(x.scopeType())).count());
      payload.put("systemCount",items.stream().filter(x->"SYSTEM".equalsIgnoreCase(x.scopeType())).count());
      payload.put("unknownCount",items.stream().filter(x->!"USER".equalsIgnoreCase(x.scopeType())&&!"SYSTEM".equalsIgnoreCase(x.scopeType())).count());
      payload.put("message",message);
      writeJsonAtomicE3(file,payload);
      appleTrace("E4 NATIVE INSTALLATION_PROXY SNAPSHOT","file="+file.getFileName()+", attempts="+attempts.size()+", items="+items.size());
    }catch(Exception e){ appleTrace("E4 NATIVE INSTALLATION_PROXY SNAPSHOT ERROR",e.getClass().getSimpleName()+": "+traceValue(e.getMessage())); }
  }

  private record InstallationProxyCliProbeResult(boolean available,String toolPath,List<Map<String,Object>> attempts,List<SoftwareItem> items,String message) {}

  /** 40k37e25: kontrollierte, rein lesende ideviceinstaller-1.2.0-Matrix ohne --debug.
   * d12 zeigte, dass --debug unter dem verwendeten Windows/mingw64-Build die Prozesse blockiert.
   * Daher werden dokumentierte list-Varianten, explizite ReturnAttributes und zwei
   * zulässige Argumentreihenfolgen gegeneinander getestet. Treffer werden sofort
   * als normale Softwareinventardaten übernommen; USER/SYSTEM wird weiterhin nur
   * aus Apple ApplicationType/ApplicationTypeString bzw. dem expliziten Scope gesetzt. */
  private InstallationProxyCliProbeResult probeInstallationProxyCliMatrixD13(String udid){
    List<Map<String,Object>> attempts=new ArrayList<>();
    try{
      Path tool=ensureModernIdeviceInstallerD11();
      if(tool==null) return new InstallationProxyCliProbeResult(false,"",attempts,List.of(),"Moderne 1.2.0-Suite nicht verfügbar.");
      List<SoftwareItem> recovered=new ArrayList<>();
      Path dir=findGamRoot().resolve("logs").resolve("apple-mobile-installation-proxy-cli-e3");
      recreateDiagnosticDirectoryD16(dir);

      // Bekannte funktionierende Referenz aus d11/d12: schnelle USER-Antwort, bislang leeres Array.
      CommandResult baseline=runInstallationProxyCliAttemptD13(attempts,dir,tool,udid,"USER_BASELINE",12,
          "list","--user","--xml");
      recovered.addAll(parseSoftwareResultIfXml(baseline,"USER","D14_USER_BASELINE"));

      // Minimaler ReturnAttributes-Satz.
      CommandResult idOnly=runInstallationProxyCliAttemptD13(attempts,dir,tool,udid,"USER_ID_ONLY",12,
          "list","--user","--xml","-a","CFBundleIdentifier");
      recovered.addAll(parseSoftwareResultIfXml(idOnly,"USER","D14_USER_ID_ONLY"));

      // Praxisrelevanter Satz inklusive der von Apple gelieferten Typfelder.
      CommandResult explicit=runInstallationProxyCliAttemptD13(attempts,dir,tool,udid,"USER_EXPLICIT",12,
          "list","--user","--xml",
          "-a","CFBundleIdentifier","-a","CFBundleDisplayName","-a","CFBundleShortVersionString",
          "-a","CFBundleVersion","-a","ApplicationType","-a","ApplicationTypeString",
          "-a","Path","-a","Container");
      recovered.addAll(parseSoftwareResultIfXml(explicit,"USER","D14_USER_EXPLICIT"));

      // Gegenprobe: globale Optionen vor dem Subcommand, Attribute ebenfalls vor list.
      CommandResult preAttrs=runInstallationProxyCliAttemptD13(attempts,dir,tool,udid,"USER_ATTRIBUTES_BEFORE_LIST",12,
          "-a","CFBundleIdentifier","-a","CFBundleDisplayName","-a","CFBundleShortVersionString",
          "-a","ApplicationType","list","--user","--xml");
      recovered.addAll(parseSoftwareResultIfXml(preAttrs,"USER","D14_USER_ATTRIBUTES_BEFORE_LIST"));

      // Ohne XML: trennt Browse-Ergebnis vom XML-Formatter.
      CommandResult textExplicit=runInstallationProxyCliAttemptD13(attempts,dir,tool,udid,"USER_TEXT_EXPLICIT",12,
          "list","--user","-a","CFBundleIdentifier","-a","CFBundleDisplayName",
          "-a","CFBundleShortVersionString","-a","ApplicationType");
      recovered.addAll(parseSoftwareTextD13(textExplicit,"USER","D14_USER_TEXT_EXPLICIT"));

      // ALL/SYSTEM mit minimalen Attributen: bewusst begrenzt, da diese Pfade bisher hängen.
      CommandResult all=runInstallationProxyCliAttemptD13(attempts,dir,tool,udid,"ALL_ID_TYPE",15,
          "list","--all","--xml","-a","CFBundleIdentifier","-a","ApplicationType","-a","ApplicationTypeString");
      recovered.addAll(parseSoftwareResultIfXml(all,"AUTO","D14_ALL_ID_TYPE"));
      CommandResult system=runInstallationProxyCliAttemptD13(attempts,dir,tool,udid,"SYSTEM_ID_TYPE",15,
          "list","--system","--xml","-a","CFBundleIdentifier","-a","ApplicationType","-a","ApplicationTypeString");
      recovered.addAll(parseSoftwareResultIfXml(system,"SYSTEM","D14_SYSTEM_ID_TYPE"));

      LinkedHashMap<String,SoftwareItem> unique=new LinkedHashMap<>();
      for(SoftwareItem x:recovered) unique.put(x.scopeType()+"\u0000"+firstNonBlank(x.bundleId(),x.name()),x);
      List<SoftwareItem> recoveredItems=List.copyOf(unique.values());
      writeInstallationProxyCliSnapshotD13(udid,tool,attempts,recoveredItems,dir);
      appleTrace("E4 INSTALLATION_PROXY CLI SUMMARY","attempts="+attempts.size()+", recovered="+recoveredItems.size()+", dir="+traceValue(dir.toString()));
      return new InstallationProxyCliProbeResult(true,tool.toString(),attempts,recoveredItems,"1.2.0-CLI-Matrix ohne --debug abgeschlossen.");
    }catch(Exception e){
      appleTrace("E4 INSTALLATION_PROXY CLI ERROR",e.getClass().getSimpleName()+": "+traceValue(e.getMessage()));
      return new InstallationProxyCliProbeResult(false,"",attempts,List.of(),firstNonBlank(e.getMessage(),e.getClass().getSimpleName()));
    }
  }

  private CommandResult runInstallationProxyCliAttemptD13(List<Map<String,Object>> attempts,Path dir,Path tool,String udid,String name,int timeout,String... args){
    List<String> argv=new ArrayList<>();
    argv.add(tool.toString()); argv.add("-u"); argv.add(udid); argv.addAll(Arrays.asList(args));
    long started=System.currentTimeMillis();
    CommandResult r=run(timeout,argv.toArray(String[]::new));
    long duration=System.currentTimeMillis()-started;
    String safe=name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]+","-");
    Path stdoutFile=dir.resolve(safe+".stdout.txt");
    Path stderrFile=dir.resolve(safe+".stderr.txt");
    try{
      Files.writeString(stdoutFile,r.stdout()==null?"":r.stdout(),StandardCharsets.UTF_8,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING);
      Files.writeString(stderrFile,r.stderr()==null?"":r.stderr(),StandardCharsets.UTF_8,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING);
    }catch(Exception e){ appleTrace("E4 INSTALLATION_PROXY CLI WRITE ERROR","name="+name+", error="+traceValue(e.getMessage())); }
    LinkedHashMap<String,Object> a=new LinkedHashMap<>();
    a.put("name",name); a.put("arguments",List.copyOf(Arrays.asList(args))); a.put("ok",r.ok()); a.put("exitCode",r.exitCode()); a.put("durationMs",duration);
    a.put("stdoutBytes",r.stdout()==null?0:r.stdout().getBytes(StandardCharsets.UTF_8).length);
    a.put("stderrBytes",r.stderr()==null?0:r.stderr().getBytes(StandardCharsets.UTF_8).length);
    a.put("stdoutFile",stdoutFile.getFileName().toString()); a.put("stderrFile",stderrFile.getFileName().toString());
    a.put("stdoutPreview",limitTrace(r.stdout())); a.put("stderrPreview",limitTrace(r.stderr()));
    a.put("xmlArrayEmpty",r.stdout()!=null && r.stdout().contains("<array/>"));
    a.put("xmlHasDict",r.stdout()!=null && r.stdout().contains("<dict>"));
    attempts.add(a);
    appleTrace("E4 INSTALLATION_PROXY CLI ATTEMPT","name="+name+", ok="+r.ok()+", exitCode="+r.exitCode()+", durationMs="+duration+", stdoutBytes="+a.get("stdoutBytes")+", stderrBytes="+a.get("stderrBytes"));
    return r;
  }

  private List<SoftwareItem> parseSoftwareTextD13(CommandResult r,String scope,String probeName){
    if(r==null||!r.ok()||r.stdout()==null||r.stdout().isBlank()) return List.of();
    String[] lines=r.stdout().split("\\R");
    if(lines.length<2) return List.of();
    String[] header=lines[0].split(",\\s*",-1);
    Map<String,Integer> idx=new LinkedHashMap<>();
    for(int i=0;i<header.length;i++) idx.put(header[i].trim(),i);
    List<SoftwareItem> out=new ArrayList<>();
    for(int i=1;i<lines.length;i++){
      String line=lines[i].trim();
      if(line.isEmpty()) continue;
      String[] cols=line.split(",\\s*",-1);
      String bundle=textColumnD13(cols,idx,"CFBundleIdentifier");
      if(bundle.isBlank()) continue;
      String name=firstNonBlank(textColumnD13(cols,idx,"CFBundleDisplayName"),bundle);
      String version=firstNonBlank(textColumnD13(cols,idx,"CFBundleShortVersionString"),textColumnD13(cols,idx,"CFBundleVersion"));
      String appleType=firstNonBlank(textColumnD13(cols,idx,"ApplicationType"),textColumnD13(cols,idx,"ApplicationTypeString"));
      String type=scope;
      if("System".equalsIgnoreCase(appleType)) type="SYSTEM";
      else if("User".equalsIgnoreCase(appleType)) type="USER";
      out.add(new SoftwareItem(type,name,version,bundle,null,"Apple installation_proxy · Quelle nicht ermittelbar"));
    }
    appleTrace("E4 SOFTWARE TEXT PARSED","probe="+probeName+", scope="+scope+", entries="+out.size()+", header="+traceValue(lines[0]));
    return out;
  }

  private static String textColumnD13(String[] cols,Map<String,Integer> idx,String key){
    Integer i=idx.get(key);
    return i==null||i<0||i>=cols.length?"":cols[i].trim();
  }

  private void writeInstallationProxyCliSnapshotD13(String udid,Path tool,List<Map<String,Object>> attempts,List<SoftwareItem> items,Path dir){
    try{
      Path file=findGamRoot().resolve("logs").resolve("apple-mobile-installation-proxy-cli-e3.json");
      LinkedHashMap<String,Object> payload=new LinkedHashMap<>();
      payload.put("generatedAt",OffsetDateTime.now().toString()); payload.put("udid",udid);
      payload.put("purpose","Rein lesende ideviceinstaller-1.2.0-CLI-Matrix ohne --debug; explizite ReturnAttributes und Argumentreihenfolgen");
      payload.put("toolPath",tool.toString()); payload.put("suite",D11_MODERN_SUITE_LABEL); payload.put("suiteSha256",D11_MODERN_SUITE_SHA256);
      payload.put("rawArchive",findGamRoot().resolve("logs").resolve("apple-mobile-installation-proxy-cli-e3.zip").toString()); payload.put("attempts",attempts); payload.put("items",items);
      writeJsonAtomicE3(file,payload);
      zipDiagnosticDirectoryD16(dir,findGamRoot().resolve("logs").resolve("apple-mobile-installation-proxy-cli-e3.zip"));
      appleTrace("E4 INSTALLATION_PROXY CLI SNAPSHOT","file="+file.getFileName()+", attempts="+attempts.size()+", items="+items.size());
    }catch(Exception e){ appleTrace("E4 INSTALLATION_PROXY CLI SNAPSHOT ERROR",e.getClass().getSimpleName()+": "+traceValue(e.getMessage())); }
  }

  /** 40k37e25: Diagnose-Rohdaten werden fuer den Upload automatisch als ZIP abgelegt. */
  private void recreateDiagnosticDirectoryD16(Path dir) throws Exception {
    if(Files.exists(dir)) deleteRecursively(dir);
    Files.createDirectories(dir);
  }

  private void zipDiagnosticDirectoryD16(Path dir,Path zipFile){
    if(dir==null||!Files.isDirectory(dir)) return;
    try{
      Files.createDirectories(zipFile.getParent());
      Path tmp=zipFile.resolveSibling(zipFile.getFileName().toString()+".tmp");
      Files.deleteIfExists(tmp);
      try(ZipOutputStream zos=new ZipOutputStream(Files.newOutputStream(tmp,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING))){
        try(var walk=Files.walk(dir)){
          for(Path p:walk.filter(Files::isRegularFile).toList()){
            String entry=dir.getFileName()+"/"+dir.relativize(p).toString().replace('\\','/');
            zos.putNextEntry(new ZipEntry(entry));
            Files.copy(p,zos);
            zos.closeEntry();
          }
        }
      }
      Files.move(tmp,zipFile,StandardCopyOption.REPLACE_EXISTING);
      deleteRecursively(dir);
      appleTrace("E4 DIAGNOSTIC ZIP","archive="+zipFile.getFileName()+", rawDirectoryRemoved=true");
    }catch(Exception e){ appleTrace("E4 DIAGNOSTIC ZIP ERROR","archive="+zipFile.getFileName()+", error="+traceValue(e.getMessage())); }
  }

  private int collectSoftwareScope(String udid,String scope,List<SoftwareItem> out,IdeviceInstallerCliMode preferredMode){
    IdeviceInstallerCliMode mode=preferredMode==null?IdeviceInstallerCliMode.UNKNOWN:preferredMode;
    CommandResult r=runSoftwareScopeCommand(udid,scope,mode);
    if(!r.ok() && looksLikeCliSyntaxMismatch(r)){
      IdeviceInstallerCliMode fallback=mode==IdeviceInstallerCliMode.LEGACY_1_1?IdeviceInstallerCliMode.MODERN:IdeviceInstallerCliMode.LEGACY_1_1;
      appleTrace("E4 IDEVICEINSTALLER FALLBACK","scope="+scope+", from="+mode+", to="+fallback+", reason=cli-syntax-mismatch");
      r=runSoftwareScopeCommand(udid,scope,fallback);
      if(r.ok()) ideviceInstallerCliMode=fallback;
    }
    if(!r.ok()||r.stdout()==null||r.stdout().isBlank()) {
      appleTrace("E4 SOFTWARE "+scope,"entries=0, commandOk="+r.ok()+", stderr="+traceValue(limitTrace(r.stderr())));
      return 0;
    }
    List<SoftwareItem> parsed=parseSoftwarePlist(r.stdout(),scope);
    out.addAll(parsed);
    appleTrace("E4 SOFTWARE "+scope,"entries="+parsed.size()+", cli="+ideviceInstallerCliMode);
    return parsed.size();
  }

  private CommandResult runSoftwareScopeCommand(String udid,String scope,IdeviceInstallerCliMode mode){
    boolean system="SYSTEM".equalsIgnoreCase(scope);
    if(mode==IdeviceInstallerCliMode.LEGACY_1_1){
      return runToolTraced(45,"E4 IDEVICEINSTALLER "+scope+" LEGACY","ideviceinstaller","-u",udid,"-l","-o",system?"list_system":"list_user","-o","xml");
    }
    return runToolTraced(45,"E4 IDEVICEINSTALLER "+scope+" MODERN","ideviceinstaller","-u",udid,"list",system?"--system":"--user","--xml");
  }

  private boolean looksLikeCliSyntaxMismatch(CommandResult r){
    if(r==null) return false;
    String x=((r.stdout()==null?"":r.stdout())+" "+(r.stderr()==null?"":r.stderr())).toLowerCase(Locale.ROOT);
    return x.contains("unknown option") || x.contains("unrecognized option") || x.contains("usage:");
  }

  private List<SoftwareItem> parseSoftwarePlist(String xml,String scope){
    List<SoftwareItem> result=new ArrayList<>();
    LinkedHashSet<String> observedAttributeKeys=new LinkedHashSet<>();
    try{
      var factory=javax.xml.parsers.DocumentBuilderFactory.newInstance();
      configureSafeApplePlistFactory(factory);
      var builder=factory.newDocumentBuilder();
      builder.setEntityResolver((publicId,systemId) -> new org.xml.sax.InputSource(new java.io.StringReader("")));
      var doc=builder.parse(new org.xml.sax.InputSource(new java.io.StringReader(xml)));
      var dicts=doc.getElementsByTagName("dict");
      for(int i=0;i<dicts.getLength();i++){
        org.w3c.dom.Node d=dicts.item(i); LinkedHashMap<String,String> m=new LinkedHashMap<>(); String key=null;
        for(org.w3c.dom.Node n=d.getFirstChild();n!=null;n=n.getNextSibling()){
          if(n.getNodeType()!=org.w3c.dom.Node.ELEMENT_NODE) continue;
          if("key".equals(n.getNodeName())) key=n.getTextContent();
          else if(key!=null){ if("string".equals(n.getNodeName())||"integer".equals(n.getNodeName())) m.put(key,n.getTextContent()); key=null; }
        }
        observedAttributeKeys.addAll(m.keySet());
        String bundle=blankToNull(m.get("CFBundleIdentifier"));
        if(bundle==null) continue;
        String name=firstNonBlank(blankToNull(m.get("CFBundleDisplayName")),blankToNull(m.get("CFBundleName")),bundle);
        String version=firstNonBlank(blankToNull(m.get("CFBundleShortVersionString")),blankToNull(m.get("CFBundleVersion")));
        String effectiveScope=scope;
        if("AUTO".equalsIgnoreCase(scope)){
          String applicationType=firstNonBlank(blankToNull(m.get("ApplicationType")),blankToNull(m.get("ApplicationTypeString")));
          if("System".equalsIgnoreCase(applicationType)) effectiveScope="SYSTEM";
          else if("User".equalsIgnoreCase(applicationType)) effectiveScope="USER";
          else effectiveScope="UNKNOWN";
        }
        String source=appleSoftwareSource(effectiveScope,m);
        result.add(new SoftwareItem(effectiveScope,name,version,bundle,blankToNull(m.get("Path")),source));
      }
    }catch(Exception e){ appleTrace("E4 SOFTWARE PARSE ERROR",e.getClass().getSimpleName()+": "+traceValue(e.getMessage())); }
    appleTrace("E4 SOFTWARE ATTRIBUTE KEYS","scope="+scope+", keys="+traceValue(limitTrace(String.join(",",observedAttributeKeys))));
    return result;
  }


  /**
   * 40k37d3: Die Herkunft wird nur dann konkret benannt, wenn installation_proxy
   * selbst ein entsprechendes skalares Attribut liefert. Keine Ableitung aus
   * Bundle-ID, Signatur oder Installationspfad. Damit bleiben alternative EU-
   * Marktplätze, Web-Distribution usw. sichtbar, sobald iPadOS sie exponiert.
   */
  private String appleSoftwareSource(String scope,Map<String,String> attributes){
    if("SYSTEM".equalsIgnoreCase(scope)) return "Apple System / Firmware";
    if(attributes!=null){
      for(var e:attributes.entrySet()){
        String key=blankToNull(e.getKey()), value=blankToNull(e.getValue());
        if(key==null||value==null) continue;
        String k=key.toLowerCase(Locale.ROOT);
        if(k.contains("marketplace")||k.contains("distributor")||k.contains("distribution")||
           k.contains("installsource")||k.contains("install_source")||k.contains("installationsource")||
           k.contains("sourceidentifier")||k.contains("source_identifier")||k.contains("origin")){
          String source="Apple App-Quelle · "+key+": "+value;
          appleTrace("E4 SOFTWARE SOURCE", "attribute="+traceValue(key)+", value="+traceValue(value));
          return source;
        }
      }
    }
    return "Apple installation_proxy · Quelle nicht ermittelbar";
  }

  private List<String> connectedUdids(){ return connectedUdidsWithRetry("native-scan",1,0); }

  /** 40k37c45: Lockdown/usbmuxd kann direkt nach einer Apple-/USB-Neuinitialisierung
   * einige Augenblicke spaeter als Windows-PnP bereit sein. Darum wird bei expliziten
   * Pairing-Aktionen kurz und begrenzt erneut geprueft, niemals endlos. */
  private List<String> connectedUdidsWithRetry(String context,int attempts,long delayMs){
    List<String> ids=List.of();
    for(int i=1;i<=Math.max(1,attempts);i++){
      long idStarted=System.currentTimeMillis();
      CommandResult r=runToolTraced(12,"IDEVICE_ID "+context+" attempt="+i,"idevice_id","-l");
      if(r.ok()){
        List<String> found=new ArrayList<>();
        for(String line:r.stdout().split("\\R")) if(line!=null&&!line.trim().isBlank()) found.add(line.trim());
        ids=List.copyOf(found);
      } else ids=List.of();

      // 40k37e25: Ein erfolgreicher Lockdown/usbmuxd-Treffer ist bereits die
      // praezisere Quelle fuer die verbundenen Apple-Geraete. Bis d18 wurde fuer
      // jede Debug-Zeile trotzdem synchron die teure Windows-PnP-Detailabfrage
      // ausgefuehrt. Auf dem realen iPad kostete genau dieser reine Diagnoseaufruf
      // rund 16 Sekunden, obwohl idevice_id selbst nach ~66 ms fertig war.
      // PnP bleibt vollstaendig als Fallback erhalten, wird aber nur noch gelesen,
      // wenn Lockdown KEINE UDID geliefert hat.
      String windowsPnpDetail="skipped(lockdown-ids-present)";
      if(ids.isEmpty()){
        long pnpStarted=System.currentTimeMillis();
        int pnpCount=readWindowsUsbDevices().size();
        windowsPnpDetail=String.valueOf(pnpCount);
        performancePhase("Lockdown/Sub: Windows-PnP-Fallback attempt="+i,pnpStarted,true,"devices="+pnpCount+", context="+context);
      }
      appleTrace("LOCKDOWN DISCOVERY", "context="+context+", attempt="+i+", ids="+ids.size()+", ideviceIdMs="+(System.currentTimeMillis()-idStarted)+", windowsPnP="+windowsPnpDetail);
      if(!ids.isEmpty()||i>=attempts) break;
      try { Thread.sleep(delayMs); } catch(InterruptedException ie){ Thread.currentThread().interrupt(); break; }
    }
    return ids;
  }


  private static boolean pairingSucceeded(CommandResult r){ String x=(r.stdout()+" "+r.stderr()).toLowerCase(Locale.ROOT); return r.ok()&&(x.contains("success")||x.contains("validated")||x.contains("valid")); }
  private boolean windowsAppleDevicesInstalled(){
    if(!System.getProperty("os.name","").toLowerCase(Locale.ROOT).contains("win"))return false;
    String ps="$p=Get-AppxPackage -ErrorAction SilentlyContinue | Where-Object { $_.Name -match '(?i)Apple.*Devices|AppleInc.*AppleDevices' }; if($p){'YES'}";
    CommandResult r=run(8,"powershell.exe","-NoProfile","-NonInteractive","-Command",ps); return r.ok()&&r.stdout().toUpperCase(Locale.ROOT).contains("YES");
  }

  /**
   * 40k37c48: Nach einer direkten MSIX/AppX-Installation ist Apple Devices zwar registriert
   * und der USB-Treiber bereits aktiv, der paketierte Apple-Mobile-Device-Unterbau kann aber
   * bis zur ersten App-Aktivierung noch fehlen. Der reale Testfall zeigte dann
   * "idevice_id -l -> Unable to retrieve device list". GAM initialisiert deshalb die
   * registrierte App automatisch ueber shell:AppsFolder und wartet anschliessend begrenzt,
   * bis libimobiledevice die usbmuxd-Geraeteliste wieder abfragen kann.
   */
  private boolean ensureAppleMobileRuntimeReady(String context){
    if(!System.getProperty("os.name","").toLowerCase(Locale.ROOT).contains("win")) return true;
    if(!windowsAppleDevicesInstalled() || !commandExists("idevice_id")) return false;

    CommandResult before=runToolTraced(8,"APPLE RUNTIME IDEVICE_ID BEFORE","idevice_id","-l");
    if(before.ok()) {
      appleTrace("APPLE RUNTIME READY", "context="+context+", alreadyReady=true, devices="+traceValue(limitTrace(before.stdout())));
      return true;
    }

    String combined=(before.stdout()+" "+before.stderr()).toLowerCase(Locale.ROOT);
    boolean listUnavailable=combined.contains("unable to retrieve device list") || combined.contains("usbmux");
    appleTrace("APPLE RUNTIME INIT", "context="+context+", listUnavailable="+listUnavailable+", detail="+traceValue(limitTrace(firstNonBlank(before.stderr(),before.stdout()))));

    // Nicht direkt aus C:\Program Files\WindowsApps starten: das fuehrt bei paketierten
    // Apps zu Zugriff-verweigert. Stattdessen den von Windows registrierten AppsFolder-Weg nutzen.
    String ps="$ErrorActionPreference='Stop'; "+
      "$p=Get-AppxPackage -Name 'AppleInc.AppleDevices' -ErrorAction Stop | Select-Object -First 1; "+
      "if(-not $p){throw 'Apple Devices package not found'}; "+
      "$appId=$p.PackageFamilyName+'!App'; "+
      "Start-Process -FilePath 'explorer.exe' -ArgumentList ('shell:AppsFolder\\'+$appId); "+
      "'ACTIVATED|'+$appId";
    CommandResult activation=run(15,"powershell.exe","-NoProfile","-NonInteractive","-ExecutionPolicy","Bypass","-Command",ps);
    appleTrace("APPLE RUNTIME ACTIVATE", "context="+context+", ok="+activation.ok()+", stdout="+traceValue(limitTrace(activation.stdout()))+", stderr="+traceValue(limitTrace(activation.stderr())));
    if(!activation.ok()) return false;

    for(int attempt=1; attempt<=12; attempt++){
      try { Thread.sleep(attempt==1?700L:500L); } catch(InterruptedException e){ Thread.currentThread().interrupt(); return false; }
      CommandResult check=runToolTraced(8,"APPLE RUNTIME IDEVICE_ID AFTER "+attempt,"idevice_id","-l");
      if(check.ok()){
        appleTrace("APPLE RUNTIME READY", "context="+context+", attempt="+attempt+", devices="+traceValue(limitTrace(check.stdout())));
        return true;
      }
    }
    appleTrace("APPLE RUNTIME FAILED", "context="+context+", reason=idevice_id remained unavailable after packaged app activation");
    return false;
  }


  /**
   * 40k37c45: installiert die von Apple vorgesehene Windows-Basis für iPhone/iPad.
   * Bevorzugt wird WinGet mit der offiziellen Microsoft-Store-ID. Falls WinGet fehlt
   * oder der Store die unbeaufsichtigte Installation nicht zulässt, öffnet GAM
   * gezielt die offizielle Produktseite im Microsoft Store und führt den Benutzer weiter.
   */
  public AppleDevicesInstallResult installAppleDevices(){
    long started=System.currentTimeMillis();
    String hostOs=System.getProperty("os.name","").toLowerCase(Locale.ROOT);
    if(hostOs.contains("linux")) return installLinuxAppleDeviceSupportE55b(started);
    if(hostOs.contains("mac")) {
      ToolchainInstallResult mac=macOsToolchainE56(started);
      return new AppleDevicesInstallResult(mac.success(),mac.state(),mac.message(),OffsetDateTime.now().toString(),mac.success(),false,false,System.currentTimeMillis()-started);
    }
    if(!hostOs.contains("win"))
      return new AppleDevicesInstallResult(false,"UNSUPPORTED","Die Apple-Geräteunterstützung wird auf dieser Plattform in diesem Build noch nicht automatisch eingerichtet.",OffsetDateTime.now().toString(),false,false,false,System.currentTimeMillis()-started);
    if(windowsAppleDevicesInstalled()) {
      boolean runtime=ensureAppleMobileRuntimeReady("install-already-present");
      String msg=runtime
        ? "Apple-Geräte ist bereits installiert und der Apple-Mobile-Device-Unterbau wurde von GAM automatisch initialisiert/geprüft."
        : "Apple-Geräte ist bereits installiert. Die Apple-Mobile-Device-Laufzeit konnte noch nicht bestätigt werden; GAM prüft sie beim nächsten Apple-Gerätetest erneut automatisch.";
      return new AppleDevicesInstallResult(true,runtime?"ALREADY_INSTALLED_READY":"ALREADY_INSTALLED_RUNTIME_PENDING",msg,OffsetDateTime.now().toString(),true,commandExistsExternal("winget"),false,System.currentTimeMillis()-started);
    }

    // 40k37c45: Offline-/LAN-Depot hat Vorrang vor Internet. Damit funktioniert derselbe
    // Installationsbutton auch in vollständig abgeschotteten Unternehmensnetzen.
    AppleOfflineDepotStatus depotStatus=appleOfflineDepotStatus();
    if(depotStatus.reachable() && depotStatus.applePackagePresent()) {
      AppleOfflineDepotResult local=installAppleFromOfflineDepot();
      if(local.success() || windowsAppleDevicesInstalled()) {
        boolean runtime=ensureAppleMobileRuntimeReady("install-offline-depot");
        return new AppleDevicesInstallResult(runtime,runtime?"OFFLINE_INSTALLED_READY":"OFFLINE_INSTALLED_RUNTIME_FAILED",runtime?"Apple-Geräte wurde aus dem lokalen GAM-Softwaredepot installiert und die Apple-Mobile-Device-Laufzeit automatisch initialisiert.":"Apple-Geräte wurde installiert, aber die Apple-Mobile-Device-Laufzeit konnte nicht automatisch initialisiert werden.",OffsetDateTime.now().toString(),true,commandExistsExternal("winget"),false,System.currentTimeMillis()-started);
      }
      log.warn("Apple-Mobile-Setup 40k37c45: lokales Depot war vorhanden, Installation aber nicht vollständig: {}",local.message());
    }

    // 40k37c45: Der normale "Apple-Geräte installieren"-Button muss denselben PRS-/Depotpfad
    // verwenden wie "Offline-Paket bereitstellen". Dadurch ist der echte Microsoft-PRS-Resolver
    // nicht nur implementiert, sondern auch in den tatsächlich benutzten Installationsablauf eingebunden.
    log.info("Apple-Mobile-Setup 40k37c45: kein installierbares lokales Apple-Devices-Paket vorhanden; starte zuerst Microsoft-PRS-/Depotvorbereitung für {}.", APPLE_DEVICES_STORE_ID);
    AppleOfflineDepotResult prepared=prepareAppleOfflineDepot();
    log.info("Apple-Mobile-Setup 40k37c45: PRS-/Depotvorbereitung beendet: success={}, state={}, packages={}, message={}",
        prepared.success(),prepared.state(),prepared.packageCount(),prepared.message());
    if(prepared.success()) {
      AppleOfflineDepotResult local=installAppleFromOfflineDepot();
      log.info("Apple-Mobile-Setup 40k37c45: Installation aus frisch vorbereitetem Depot: success={}, state={}, message={}",local.success(),local.state(),local.message());
      if(local.success() || windowsAppleDevicesInstalled()) {
        boolean runtime=ensureAppleMobileRuntimeReady("install-prs-depot");
        return new AppleDevicesInstallResult(runtime,runtime?"PRS_DEPOT_INSTALLED_READY":"PRS_DEPOT_RUNTIME_FAILED",runtime?"Apple-Geräte wurde über Microsoft PRS installiert und die Apple-Mobile-Device-Laufzeit automatisch initialisiert.":"Apple-Geräte wurde über Microsoft PRS installiert, aber die Apple-Mobile-Device-Laufzeit konnte noch nicht automatisch initialisiert werden.",OffsetDateTime.now().toString(),true,commandExistsExternal("winget"),false,System.currentTimeMillis()-started);
      }
    } else {
      log.info("Apple-Mobile-Setup 40k37c45: PRS-/Depotpfad war nicht erfolgreich; erst jetzt folgt der bisherige WinGet/Store-Fallback.");
    }

    boolean winget=commandExistsExternal("winget");
    String installDetail="";
    if(winget){
      log.info("Apple-Mobile-Setup 40k37c45: installiere Apple-Geräte über WinGet/Microsoft Store (ID={})",APPLE_DEVICES_STORE_ID);
      CommandResult r=run(180,"winget","install","--id",APPLE_DEVICES_STORE_ID,"--source","msstore",
        "--accept-package-agreements","--accept-source-agreements","--disable-interactivity");
      installDetail=firstNonBlank(r.stderr(),r.stdout());
      if(windowsAppleDevicesInstalled()) {
        boolean runtime=ensureAppleMobileRuntimeReady("install-winget-store");
        return new AppleDevicesInstallResult(runtime,runtime?"INSTALLED_READY":"INSTALLED_RUNTIME_FAILED",runtime?"Apple-Geräte wurde über den Microsoft Store installiert und die Apple-Mobile-Device-Laufzeit automatisch initialisiert.":"Apple-Geräte wurde installiert, aber die Apple-Mobile-Device-Laufzeit konnte noch nicht automatisch initialisiert werden.",OffsetDateTime.now().toString(),true,true,false,System.currentTimeMillis()-started);
      }
    }

    boolean opened=openAppleDevicesStore();
    String msg=opened
      ? "Die offizielle Microsoft-Store-Seite für Apple-Geräte wurde geöffnet. Bitte dort 'Installieren' wählen und anschließend in GAM 'Apple-Gerät prüfen' ausführen."
      : "Apple-Geräte konnte nicht automatisch installiert werden. Bitte Microsoft Store öffnen, nach 'Apple-Geräte' suchen und die App von Apple installieren.";
    if(!installDetail.isBlank() && !opened) msg += " WinGet: "+installDetail.replaceAll("\\s+"," ").trim();
    return new AppleDevicesInstallResult(false,opened?"STORE_OPENED":"INSTALL_FAILED",msg,OffsetDateTime.now().toString(),false,winget,opened,System.currentTimeMillis()-started);
  }


  /** 40k37e55b: Linux-Gegenstück zum Windows-Schritt "Apple-Geräte installieren".
   * Linux benötigt keine Apple-EXE und keinen separaten Windows-Treiber. Die Gerätebasis
   * besteht aus Kernel-USB-Sichtbarkeit, udev/usbmuxd und libimobiledevice. GAM prüft diese
   * Schichten einzeln und darf auf Ubuntu/Debian den systemweiten usbmuxd-Dienst über
   * polkit neu initialisieren. GAM selbst bleibt unprivilegiert. */
  /** e55g: Linux-Laufzeitadapter unterhalb der eingefrorenen e54-Apple-Logik.
   * Startet nur einen bereits installierten usbmuxd-Systemdienst, wenn ein physisches
   * Apple-USB-Geraet vorhanden ist. Kein Pairing, kein Merge, keine Inventarisierung und
   * kein Ersatz fuer idevice_id. Ohne vorhandene Rechte wird nur sauber abgebrochen;
   * waehrend eines normalen Suchlaufs wird bewusst kein Polkit-Dialog erzwungen. */
  private boolean ensureLinuxUsbmuxdRuntimeE55g() {
    if(!System.getProperty("os.name","").toLowerCase(Locale.ROOT).contains("linux")) return false;
    if(!run(4,"sh","-lc","command -v usbmuxd >/dev/null 2>&1").ok()) return false;

    CommandResult socketBefore=run(4,"sh","-lc","test -S /run/usbmuxd -o -S /var/run/usbmuxd");
    CommandResult processBefore=run(4,"sh","-lc","pgrep -x usbmuxd >/dev/null 2>&1");
    if(socketBefore.ok() || processBefore.ok()) return true;

    boolean systemctl=run(4,"sh","-lc","command -v systemctl >/dev/null 2>&1").ok();
    if(!systemctl) {
      log.info("[DISCOVERY][40k37e55g][APPLE_MOBILE][LINUX_USBMUXD] action=RUNTIME_START result=NO_SYSTEMCTL");
      return false;
    }

    // Kein sudo/pkexec im Discovery-Lauf. Wenn GAM bereits die erforderlichen Rechte hat
    // (z.B. aktueller Linux-Teststart als root), funktioniert systemctl direkt. Andernfalls
    // bleibt die explizite Installations-/Einrichtungsaktion fuer Polkit zustaendig.
    CommandResult start=run(12,"systemctl","start","usbmuxd.service");
    if(!start.ok()) {
      CommandResult restart=run(12,"systemctl","restart","usbmuxd.service");
      if(!restart.ok()) {
        log.info("[DISCOVERY][40k37e55g][APPLE_MOBILE][LINUX_USBMUXD] action=RUNTIME_START result=FAILED exit={} error={}",
            restart.exitCode(),firstNonBlank(restart.stderr(),start.stderr(),"-").replaceAll("\s+"," ").trim());
        return false;
      }
    }

    // Das USB-Ereignis kann vor dem Daemon-Start erfolgt sein. Nur wenn udevadm ohne
    // Privileg-Eskalation bereits nutzbar ist, wird das vorhandene USB-Ereignis erneut
    // angestossen; fremde Regeln oder Berechtigungen werden nicht veraendert.
    if(run(4,"sh","-lc","command -v udevadm >/dev/null 2>&1").ok()) {
      run(8,"udevadm","trigger","--subsystem-match=usb");
    }
    try { Thread.sleep(700L); } catch(InterruptedException ie) { Thread.currentThread().interrupt(); }

    CommandResult socketAfter=run(4,"sh","-lc","test -S /run/usbmuxd -o -S /var/run/usbmuxd");
    CommandResult processAfter=run(4,"sh","-lc","pgrep -x usbmuxd >/dev/null 2>&1");
    boolean ready=socketAfter.ok() || processAfter.ok();
    log.info("[DISCOVERY][40k37e55g][APPLE_MOBILE][LINUX_USBMUXD] action=RUNTIME_START result={} socket={} process={}",
        ready?"READY":"NOT_READY",socketAfter.ok(),processAfter.ok());
    return ready;
  }

  private AppleDevicesInstallResult installLinuxAppleDeviceSupportE55b(long started) {
    try {
      String distro="Linux";
      CommandResult dr=run(4,"sh","-lc",". /etc/os-release 2>/dev/null; printf '%s' \"${PRETTY_NAME:-${ID:-Linux}}\"");
      if(dr.ok()&&!dr.stdout().trim().isBlank()) distro=dr.stdout().trim();

      boolean lsusbAvailable=run(4,"sh","-lc","command -v lsusb >/dev/null 2>&1").ok();
      if(!lsusbAvailable) {
        boolean pkexec=run(4,"sh","-lc","command -v pkexec >/dev/null 2>&1").ok();
        boolean aptGet=run(4,"sh","-lc","command -v apt-get >/dev/null 2>&1").ok();
        if(pkexec&&aptGet) {
          log.info("[DISCOVERY][40k37e55e][APPLE_MOBILE][LINUX_DEVICE_SUPPORT] action=INSTALL_USB_DIAGNOSTICS manager=apt package=usbutils");
          run(300,"pkexec","apt-get","install","-y","usbutils");
          lsusbAvailable=run(4,"sh","-lc","command -v lsusb >/dev/null 2>&1").ok();
        }
      }

      CommandResult usb=lsusbAvailable?run(8,"sh","-lc","lsusb 2>/dev/null | grep -iE 'Apple|05ac:' || true"):new CommandResult(false,"","lsusb fehlt",-1);
      boolean appleUsbVisible=!usb.stdout().trim().isBlank();
      boolean usbmuxdBinary=run(4,"sh","-lc","command -v usbmuxd >/dev/null 2>&1").ok();
      boolean systemctl=run(4,"sh","-lc","command -v systemctl >/dev/null 2>&1").ok();
      boolean usbmuxdActive=!systemctl || run(5,"sh","-lc","systemctl is-active --quiet usbmuxd.service 2>/dev/null || systemctl is-active --quiet usbmuxd.socket 2>/dev/null").ok();
      boolean ideviceId=resolveTool("idevice_id")!=null;

      log.info("[DISCOVERY][40k37e55e][APPLE_MOBILE][LINUX_DEVICE_SUPPORT] distro={}, lsusb={}, appleUsbVisible={}, usbmuxdBinary={}, usbmuxdActive={}, ideviceId={}",
          distro,lsusbAvailable,appleUsbVisible,usbmuxdBinary,usbmuxdActive,ideviceId);

      // usbmuxd ist die Linux-Vermittlungsschicht zwischen USB/udev und Lockdown. Ein Neustart
      // ist nach einer frischen Paketinstallation sinnvoll und entspricht dem Windows-Schritt,
      // bei dem der Apple-Mobile-Unterbau initialisiert wird. Nur dieser Dienst wird erhöht.
      if(usbmuxdBinary && systemctl && !usbmuxdActive) {
        boolean pkexec=run(4,"sh","-lc","command -v pkexec >/dev/null 2>&1").ok();
        if(pkexec) {
          log.info("[DISCOVERY][40k37e55e][APPLE_MOBILE][LINUX_DEVICE_SUPPORT] action=START_USBMUXD privilege=pkexec");
          CommandResult start=run(30,"pkexec","systemctl","restart","usbmuxd.service");
          if(!start.ok()) run(30,"pkexec","systemctl","start","usbmuxd.socket");
          usbmuxdActive=run(5,"sh","-lc","systemctl is-active --quiet usbmuxd.service 2>/dev/null || systemctl is-active --quiet usbmuxd.socket 2>/dev/null").ok();
        }
      }

      // Nach Paket-/Dienstinitialisierung udev-Ereignisse neu anstoßen. Keine fremden Regeln
      // werden geschrieben und keine Benutzergruppen verändert.
      boolean pkexec=run(4,"sh","-lc","command -v pkexec >/dev/null 2>&1").ok();
      boolean udevadm=run(4,"sh","-lc","command -v udevadm >/dev/null 2>&1").ok();
      if(pkexec&&udevadm) {
        run(20,"pkexec","udevadm","control","--reload-rules");
        run(20,"pkexec","udevadm","trigger","--subsystem-match=usb");
      }

      // e55d: Nach dem udev-Trigger usbmuxd erneut initialisieren. Auf Debian/Ubuntu
      // wird usbmuxd typischerweise durch das Apple-USB-Ereignis aktiviert; bei einer
      // frischen Paketinstallation kann das Ereignis jedoch vor der Installation der
      // Regeln erfolgt sein. Nur der Systemdienst wird privilegiert gestartet.
      if(appleUsbVisible && usbmuxdBinary && systemctl && pkexec) {
        log.info("[DISCOVERY][40k37e55e][APPLE_MOBILE][LINUX_USBMUXD] action=RESTART_AFTER_UDEV");
        run(30,"pkexec","systemctl","restart","usbmuxd.service");
        try { Thread.sleep(900L); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
      }
      boolean usbmuxdSocket=run(4,"sh","-lc","test -S /var/run/usbmuxd -o -S /run/usbmuxd").ok();
      boolean usbmuxdProcess=run(4,"sh","-lc","pgrep -x usbmuxd >/dev/null 2>&1").ok();
      usbmuxdActive=usbmuxdActive || usbmuxdSocket || usbmuxdProcess || (systemctl && run(5,"sh","-lc","systemctl is-active --quiet usbmuxd.service 2>/dev/null || systemctl is-active --quiet usbmuxd.socket 2>/dev/null").ok());
      log.info("[DISCOVERY][40k37e55e][APPLE_MOBILE][LINUX_USBMUXD] binary={}, active={}, process={}, socket={}",usbmuxdBinary,usbmuxdActive,usbmuxdProcess,usbmuxdSocket);

      CommandResult list=ideviceId?runTool(10,"idevice_id","-l"):new CommandResult(false,"","idevice_id fehlt",-1);
      int devices=list.ok()?parseUdidLines(list.stdout()).size():0;
      log.info("[DISCOVERY][40k37e55e][APPLE_MOBILE][LINUX_IDEVICE] ok={}, devices={}, stderr={}",list.ok(),devices,firstNonBlank(list.stderr(),"-").replaceAll("\\s+"," ").trim());
      if(appleUsbVisible && devices==0) {
        // 40k37e55e: genau die unterste Linux-Schicht diagnostizieren, ohne die e54-Apple-Logik anzufassen.
        CommandResult node=run(5,"sh","-lc","d=$(lsusb -d 05ac: 2>/dev/null | head -n1); b=$(printf '%s' \"$d\" | sed -n 's/^Bus \\([0-9][0-9]*\\) Device \\([0-9][0-9]*\\):.*/\\1/p'); n=$(printf '%s' \"$d\" | sed -n 's/^Bus \\([0-9][0-9]*\\) Device \\([0-9][0-9]*\\):.*/\\2/p'); [ -n \"$b\" ] && [ -n \"$n\" ] && stat -c '%A %U:%G %n' /dev/bus/usb/$b/$n 2>/dev/null || true");
        CommandResult rule=run(5,"sh","-lc","test -r /usr/lib/udev/rules.d/39-usbmuxd.rules -o -r /lib/udev/rules.d/39-usbmuxd.rules");
        String cause=!usbmuxdActive?"USBMUXD_NOT_ACTIVE":(!usbmuxdSocket?"USBMUXD_SOCKET_MISSING":(!rule.ok()?"UDEV_RULE_MISSING":"DEVICE_NOT_EXPOSED_TO_USBMUXD"));
        log.info("[DISCOVERY][40k37e55e][APPLE_MOBILE][LINUX_REACHABILITY] cause={}, udevRule={}, deviceNode={}, ideviceExit={}, ideviceError={}",cause,rule.ok(),firstNonBlank(node.stdout(),"-").replaceAll("\\s+"," ").trim(),list.exitCode(),firstNonBlank(list.stderr(),"-").replaceAll("\\s+"," ").trim());
      }
      boolean ready=lsusbAvailable&&usbmuxdBinary&&ideviceId&&usbmuxdActive;
      String state;
      String msg;
      if(devices>0) {
        state="LINUX_DEVICE_SUPPORT_READY";
        msg="Linux-Apple-Geräteunterstützung ist betriebsbereit: USB/udev, usbmuxd und libimobiledevice arbeiten zusammen; "+devices+" Apple-Gerät(e) sind über idevice_id sichtbar.";
      } else if(appleUsbVisible) {
        state="LINUX_USB_VISIBLE_LOCKDOWN_PENDING";
        String cause=!usbmuxdActive?"usbmuxd ist nicht aktiv":(!usbmuxdSocket?"usbmuxd ist aktiv, aber sein Socket fehlt":"usbmuxd und Socket sind aktiv, aber das Apple-Gerät wird noch nicht an libimobiledevice durchgereicht");
        msg="Linux erkennt das Apple-Gerät physisch, aber Lockdown ist noch nicht erreichbar: "+cause+". Die Apple-Inventarisierung wurde nicht ausgeführt. Details: logs/apple-mobile-debug.log.";
      } else if(ready) {
        state="LINUX_DEVICE_SUPPORT_READY_NO_DEVICE";
        msg="Linux-Apple-Geräteunterstützung ist eingerichtet (USB/udev, usbmuxd, libimobiledevice). Aktuell ist jedoch kein Apple-Gerät physisch über USB sichtbar.";
      } else {
        state="LINUX_DEVICE_SUPPORT_INCOMPLETE";
        msg="Linux-Apple-Geräteunterstützung ist noch unvollständig: lsusb="+lsusbAvailable+", usbmuxd="+usbmuxdBinary+", usbmuxdAktiv="+usbmuxdActive+", idevice_id="+ideviceId+". Unter "+distro+" werden diese Komponenten über den System-Paketmanager verwaltet.";
      }
      log.info("[DISCOVERY][40k37e55e][APPLE_MOBILE][LINUX_DEVICE_SUPPORT] result={}, appleUsbVisible={}, ideviceDevices={}",state,appleUsbVisible,devices);
      return new AppleDevicesInstallResult(devices>0||ready,state,msg,OffsetDateTime.now().toString(),ready,false,false,System.currentTimeMillis()-started);
    } catch(Exception e) {
      log.warn("[DISCOVERY][40k37e55e][APPLE_MOBILE][LINUX_DEVICE_SUPPORT] failed",e);
      return new AppleDevicesInstallResult(false,"LINUX_DEVICE_SUPPORT_FAILED","Linux-Apple-Geräteunterstützung konnte nicht vollständig geprüft/eingerichtet werden: "+firstNonBlank(e.getMessage(),e.toString()),OffsetDateTime.now().toString(),false,false,false,System.currentTimeMillis()-started);
    }
  }


  private Path appleOfflineDepotDirectory() {
    try {
      Path root=findGamRoot();
      Path cfg=root.resolve("config").resolve("apple-offline-depot.txt");
      if(Files.isRegularFile(cfg)) {
        String v=Files.readString(cfg,StandardCharsets.UTF_8).trim();
        if(!v.isBlank()) return Path.of(v).toAbsolutePath().normalize();
      }
      String env=System.getenv().getOrDefault("GAM_SOFTWARE_DEPOT","").trim();
      if(!env.isBlank()) return Path.of(env).resolve("apple").resolve("apple-devices").toAbsolutePath().normalize();
      return root.resolve("software-depot").resolve("apple").resolve("apple-devices").toAbsolutePath().normalize();
    } catch(Exception e) { throw new IllegalStateException("Apple-Offline-Depot konnte nicht bestimmt werden: "+e.getMessage(),e); }
  }

  public AppleOfflineDepotStatus appleOfflineDepotStatus(){
    try {
      Path d=appleOfflineDepotDirectory();
      boolean reachable=Files.exists(d) ? Files.isDirectory(d) : Files.isDirectory(d.getParent());
      List<Path> pkgs=listAppxPackages(d);
      boolean main=pkgs.stream().anyMatch(this::looksLikePrimaryApplePackage);
      Path tc=d.resolve("libimobiledevice");
      boolean tools=Files.isRegularFile(tc.resolve("idevice_id.exe"))&&Files.isRegularFile(tc.resolve("ideviceinfo.exe"))&&Files.isRegularFile(tc.resolve("idevicepair.exe"));
      String msg=!reachable?"Depot ist nicht erreichbar.":main?(tools?"Apple-Offlinepaket ist vollständig vorbereitet.":"Apple-Geräte-Paket vorhanden; Pairing-Toolchain fehlt noch."):"Depot erreichbar, aber noch kein Apple-Geräte-Paket vorhanden.";
      return new AppleOfflineDepotStatus(d.toString(),reachable,pkgs.size(),main,tools,msg,OffsetDateTime.now().toString());
    } catch(Exception e){ return new AppleOfflineDepotStatus("",false,0,false,false,e.getMessage(),OffsetDateTime.now().toString()); }
  }

  public AppleOfflineDepotStatus configureAppleOfflineDepot(String directory){
    if(directory==null||directory.trim().isBlank()) throw new IllegalArgumentException("Bitte einen lokalen Ordner oder UNC-Pfad für das GAM-Softwaredepot angeben.");
    try{
      Path d=Path.of(directory.trim()).toAbsolutePath().normalize();
      Files.createDirectories(d);
      Path cfg=findGamRoot().resolve("config").resolve("apple-offline-depot.txt");
      Files.createDirectories(cfg.getParent()); Files.writeString(cfg,d.toString(),StandardCharsets.UTF_8,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING);
      return appleOfflineDepotStatus();
    }catch(Exception e){ throw new IllegalStateException("Depot konnte nicht gespeichert werden: "+e.getMessage(),e); }
  }

  /** 40k37c45: prueft ausschliesslich PRS-Metadaten. Keine Paketdatei wird geladen, ersetzt oder geloescht. */
  public AppleDepotUpdateStatus checkAppleOfflineDepotUpdates(){
    try{
      if(!System.getProperty("os.name","").toLowerCase(Locale.ROOT).contains("win"))
        return new AppleDepotUpdateStatus(false,"UNSUPPORTED_OS","Die Online-Aktualisierungspruefung ist derzeit fuer Windows vorgesehen.",OffsetDateTime.now().toString(),false,"","",List.of(),List.of());
      Path depot=appleOfflineDepotDirectory(); Files.createDirectories(depot.resolve("packages"));
      List<PrsPackage> online=resolvePrsPackagesMetadata("C43-UPDATE-CHECK");
      List<Path> local=listAppxPackages(depot);
      Set<String> localNames=new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
      Map<String,Long> localSizes=new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
      for(Path f:local){ localNames.add(f.getFileName().toString()); try{ localSizes.put(f.getFileName().toString(),Files.size(f)); }catch(Exception ignored){} }
      List<String> expected=new ArrayList<>(), missing=new ArrayList<>();
      String onlineMain="";
      for(int i=0;i<online.size();i++){
        PrsPackage pkg=online.get(i);
        String name=prsFileName(pkg);
        expected.add(name);
        if(pkg.main()) onlineMain=name;
        Long localSize=localSizes.get(name);
        if(!localNames.contains(name) || (pkg.sizeInBytes()>0 && (localSize==null || localSize.longValue()!=pkg.sizeInBytes()))) missing.add(name);
      }
      String localMain=local.stream().filter(this::looksLikePrimaryApplePackage).map(x->x.getFileName().toString()).findFirst().orElse("");
      boolean update=!missing.isEmpty();
      String msg;
      String state;
      if(localMain.isBlank()) { state="DEPOT_NOT_PREPARED"; msg="Im lokalen Depot liegt noch kein Apple-Devices-Hauptpaket. Bitte das Offline-Paket zuerst bereitstellen."; }
      else if(update) { state="UPDATE_AVAILABLE"; msg="Microsoft PRS meldet einen neueren oder geaenderten Paketsatz. Das lokale Depot bleibt unveraendert, bis 'Depot aktualisieren' ausdruecklich bestaetigt wird."; }
      else { state="UP_TO_DATE"; msg="Das lokale Apple-Softwaredepot entspricht dem aktuell von Microsoft PRS gemeldeten Paketsatz."; }
      appleTrace("C43 UPDATE CHECK", "state="+state+", updateAvailable="+update+", localMain="+traceValue(localMain)+", onlineMain="+traceValue(onlineMain)+", expected="+expected.size()+", missingOrChanged="+missing.size());
      return new AppleDepotUpdateStatus(true,state,msg,OffsetDateTime.now().toString(),update,localMain,onlineMain,List.copyOf(expected),List.copyOf(missing));
    }catch(Exception e){
      appleTrace("C43 UPDATE CHECK FAILED", e.getClass().getName()+": "+firstNonBlank(e.getMessage(),e.toString(),"unbekannter Fehler"));
      return new AppleDepotUpdateStatus(false,"CHECK_FAILED",firstNonBlank(e.getMessage(),e.toString(),"Aktualisierungspruefung fehlgeschlagen."),OffsetDateTime.now().toString(),false,"","",List.of(),List.of());
    }
  }

  /** 40k37c45: explizite, vom Benutzer bestaetigte Depotaktualisierung. Der neue Satz wird
   * zuerst vollstaendig in einem Staging-Verzeichnis geladen und validiert. Erst danach wird
   * das aktive packages-Verzeichnis umgeschaltet; bei einem Fehler bleibt der alte Satz erhalten. */
  public AppleOfflineDepotResult updateAppleOfflineDepot(){
    long started=System.currentTimeMillis();
    appleTrace("ENTER updateAppleOfflineDepot", "explicitUserAction=true, storeId="+APPLE_DEVICES_STORE_ID);
    Path depot=appleOfflineDepotDirectory();
    Path target=depot.resolve("packages");
    Path backup=depot.resolve("packages-backup-c43");
    Path stageRoot=depot.resolve("packages-staging-c43");
    Path stagePackages=stageRoot.resolve("packages");
    try{
      AppleDepotUpdateStatus check=checkAppleOfflineDepotUpdates();
      if(!check.success()) throw new IllegalStateException(check.message());
      if(!check.updateAvailable()){
        appleTrace("EXIT updateAppleOfflineDepot", "state=NO_UPDATE, changed=false");
        List<Path> current=listAppxPackages(depot);
        return new AppleOfflineDepotResult(true,"NO_UPDATE","Das lokale Apple-Softwaredepot ist bereits aktuell. Es wurden keine Dateien veraendert.",OffsetDateTime.now().toString(),depot.toString(),current.size(),appleOfflineDepotStatus().toolchainPresent(),System.currentTimeMillis()-started);
      }
      deleteRecursively(stageRoot); Files.createDirectories(stagePackages);
      PackageLinkResult staged=tryPrepareViaMicrosoftPackageRetrievalService(stagePackages);
      if(!staged.success()) throw new IllegalStateException("Der neue PRS-Paketsatz konnte nicht vollstaendig vorbereitet werden: "+staged.message());
      List<Path> stagedFiles=listAppxPackages(stageRoot);
      stagedFiles=rejectInvalidDepotPackages(stageRoot,stagedFiles);
      if(stagedFiles.isEmpty()||stagedFiles.stream().noneMatch(this::looksLikePrimaryApplePackage))
        throw new IllegalStateException("Der validierte Staging-Satz enthaelt kein Apple-Devices-Hauptpaket.");
      validateDepotPackageSignatures(stagedFiles);
      appleTrace("C43 UPDATE STAGED", "packages="+stagedFiles.size()+", downloaded="+staged.downloaded());

      deleteRecursively(backup);
      boolean oldMoved=false;
      try{
        if(Files.exists(target)){ Files.move(target,backup,StandardCopyOption.REPLACE_EXISTING); oldMoved=true; }
        try{ Files.move(stagePackages,target,StandardCopyOption.ATOMIC_MOVE); }
        catch(AtomicMoveNotSupportedException ex){ Files.move(stagePackages,target,StandardCopyOption.REPLACE_EXISTING); }
      }catch(Exception swapError){
        if(Files.exists(target)) deleteRecursively(target);
        if(oldMoved&&Files.exists(backup)) Files.move(backup,target,StandardCopyOption.REPLACE_EXISTING);
        throw new IllegalStateException("Depot-Umschaltung fehlgeschlagen; der vorherige Paketsatz wurde beibehalten: "+swapError.getMessage(),swapError);
      }
      deleteRecursively(backup);
      deleteRecursively(stageRoot);
      List<Path> finalFiles=listAppxPackages(depot);
      validateDepotPackageSignatures(finalFiles);
      writeDepotManifest(depot,finalFiles);
      writeDepotImportHint(depot,false,"40k37c45: explizit bestaetigte PRS-Depotaktualisierung abgeschlossen.");
      appleTrace("EXIT updateAppleOfflineDepot", "state=UPDATED, packages="+finalFiles.size()+", explicitUserAction=true");
      return new AppleOfflineDepotResult(true,"UPDATED","Das Apple-Softwaredepot wurde nach ausdruecklicher Bestaetigung auf den aktuellen Microsoft-PRS-Paketsatz aktualisiert. Der vorherige Satz blieb bis zur vollstaendigen Validierung aktiv.",OffsetDateTime.now().toString(),depot.toString(),finalFiles.size(),appleOfflineDepotStatus().toolchainPresent(),System.currentTimeMillis()-started);
    }catch(Exception e){
      try{ deleteRecursively(stageRoot); }catch(Exception ignored){}
      log.warn("Apple-Mobile-Offlineaktualisierung 40k37c44 fehlgeschlagen",e);
      appleTrace("FAIL updateAppleOfflineDepot", e.getClass().getName()+": "+firstNonBlank(e.getMessage(),e.toString(),"unbekannter Fehler"));
      return new AppleOfflineDepotResult(false,"UPDATE_FAILED",firstNonBlank(e.getMessage(),e.toString(),"Depotaktualisierung fehlgeschlagen."),OffsetDateTime.now().toString(),depot.toString(),0,appleOfflineDepotStatus().toolchainPresent(),System.currentTimeMillis()-started);
    }
  }

  /** 40k37c45: gemeinsamer, read-only PRS-Metadatenabruf fuer Update-Pruefung. */
  private List<PrsPackage> resolvePrsPackagesMetadata(String tracePrefix) throws Exception{
    String architecture=resolveWindowsArchitectureForPrs();
    String platformName=resolveWindowsDeviceFamilyForPrs();
    long platformVersion=resolveWindowsDeviceFamilyVersionForPrs();
    String q=MICROSOFT_PACKAGE_RETRIEVAL_ENDPOINT+"?productId="+URLEncoder.encode(APPLE_DEVICES_STORE_ID,StandardCharsets.UTF_8)
        +"&architecture="+URLEncoder.encode(architecture,StandardCharsets.UTF_8)
        +"&platformName="+URLEncoder.encode(platformName,StandardCharsets.UTF_8)
        +"&platformVersion="+platformVersion;
    HttpClient client=HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(java.time.Duration.ofSeconds(20)).build();
    HttpRequest req=HttpRequest.newBuilder(URI.create(q)).header("Accept","application/json").header("User-Agent","GAM-Standalone/2.1.0 (40k37c44)").timeout(java.time.Duration.ofSeconds(60)).GET().build();
    HttpResponse<byte[]> resp=client.send(req,HttpResponse.BodyHandlers.ofByteArray());
    appleTrace(tracePrefix+" PRS METADATA", "http="+resp.statusCode()+", architecture="+architecture+", platform="+platformName+", bytes="+(resp.body()==null?0:resp.body().length));
    if(resp.statusCode()<200||resp.statusCode()>=300||resp.body()==null||resp.body().length==0)
      throw new IOException("Microsoft PRS Metadatenabruf fehlgeschlagen: HTTP "+resp.statusCode());
    JsonNode root=new ObjectMapper().readTree(resp.body());
    JsonNode productIdNode=jsonField(root,"ProductId","productId");
    if(productIdNode!=null&&!productIdNode.asText("").isBlank()&&!APPLE_DEVICES_STORE_ID.equalsIgnoreCase(productIdNode.asText().trim()))
      throw new SecurityException("Microsoft PRS lieferte eine unerwartete ProductId: "+productIdNode.asText());
    List<PrsPackage> resolved=new ArrayList<>();
    JsonNode main=jsonField(root,"Package","package");
    if(main!=null&&!main.isNull()) resolved.add(parsePrsPackage(main,true));
    JsonNode deps=jsonField(root,"Dependencies","dependencies");
    if(deps!=null&&deps.isArray()) for(JsonNode d:deps) if(d!=null&&!d.isNull()) resolved.add(parsePrsPackage(d,false));
    if(resolved.isEmpty()||resolved.stream().noneMatch(PrsPackage::main)) throw new IOException("Microsoft PRS lieferte keinen vollstaendigen Apple-Devices-Paketsatz.");
    return List.copyOf(resolved);
  }

  /** 40k37c45: Online-Rechner bereitet ein reproduzierbares Apple-Offline-Depot vor.
   * Primär löst GAM Apple Devices direkt über Microsoft PRS auf. PRS-Paketbytes werden nur
   * vom exakt produktgebundenen Microsoft-Retrieval-Pfad oder den bekannten Microsoft-CDNs
   * akzeptiert und anschließend über Größe/Digest, Container, Authenticode und Apple-Identität
   * validiert. WinGet/Store und Community-Linkgeneratoren bleiben nachgelagerte Fallbacks. */
  public AppleOfflineDepotResult prepareAppleOfflineDepot(){
    long started=System.currentTimeMillis();
    appleTrace("ENTER prepareAppleOfflineDepot", "thread="+Thread.currentThread().getName()+", storeId="+APPLE_DEVICES_STORE_ID);
    try{
      if(!System.getProperty("os.name","").toLowerCase(Locale.ROOT).contains("win"))
        throw new IllegalStateException("Offline-Paketvorbereitung ist derzeit für Windows vorgesehen.");

      Path depot=appleOfflineDepotDirectory(); Files.createDirectories(depot);
      Path packages=depot.resolve("packages"); Files.createDirectories(packages);
      log.info("Apple-Mobile-Offline 40k37c45: Offline-Paketvorbereitung gestartet; Depot={}, Store-ID={}. PRS wird vor WinGet/Store und Community-Fallbacks geprüft.",depot,APPLE_DEVICES_STORE_ID);
      appleTrace("C42-DEPOT-PREPARE", "depot="+depot+", packages="+packages+", depotPathLength="+depot.toString().length());

      // 40k37c45: Der vom UI verwendete /depot/prepare-Pfad ruft PRS jetzt unmittelbar
      // als allererste Online-Aufloesung auf. So kann kein vorgelagerter Toolchain-/Store-
      // Schritt den eigentlichen Microsoft-PRS-Test verdecken. Das Ergebnis wird danach
      // nochmals kompakt protokolliert, damit es auch bei sehr langen Fallback-Logs sichtbar bleibt.
      List<Path> pkgFiles=listAppxPackages(depot);
      pkgFiles=rejectInvalidDepotPackages(depot,pkgFiles);
      boolean validMain=pkgFiles.stream().anyMatch(this::looksLikePrimaryApplePackage);
      // 40k37c45: Ein bereits vorbereitetes Depot wird durch "Offline-Paket bereitstellen"
      // niemals still aktualisiert. Zuerst werden nur PRS-Metadaten verglichen. Ist ein neuer
      // oder geaenderter Satz verfuegbar, muss der Benutzer die separate Update-Aktion bestaetigen.
      if(validMain){
        AppleDepotUpdateStatus update=checkAppleOfflineDepotUpdates();
        if(!update.success()){
          appleTrace("C43 PREPARE UPDATE CHECK FAILED", "state=NO_CHANGE, reason="+traceValue(update.message()));
          boolean toolsNow=appleOfflineDepotStatus().toolchainPresent();
          return new AppleOfflineDepotResult(false,"UPDATE_CHECK_FAILED_NO_CHANGE","Das Offline-Depot ist bereits vorbereitet. Die Online-Versionspruefung war nicht eindeutig; aus Sicherheitsgruenden hat GAM keine vorhandene Depotdatei veraendert.",OffsetDateTime.now().toString(),depot.toString(),pkgFiles.size(),toolsNow,System.currentTimeMillis()-started);
        }
        if(update.updateAvailable()){
          appleTrace("C43 PREPARE UPDATE BLOCKED", "state=UPDATE_AVAILABLE_CONFIRM_REQUIRED, localMain="+traceValue(update.localMainPackage())+", onlineMain="+traceValue(update.onlineMainPackage()));
          boolean toolsNow=appleOfflineDepotStatus().toolchainPresent();
          return new AppleOfflineDepotResult(true,"UPDATE_AVAILABLE_CONFIRM_REQUIRED","Das Offline-Depot ist bereits vorbereitet. Microsoft PRS meldet neuere oder geaenderte Dateien; GAM hat nichts veraendert. Bitte 'Depot aktualisieren' nur bei gewuenschtem Update ausdruecklich bestaetigen.",OffsetDateTime.now().toString(),depot.toString(),pkgFiles.size(),toolsNow,System.currentTimeMillis()-started);
        }
      }
      PackageLinkResult prs=null;
      // 40k37c45: Ein vorhandenes Hauptpaket beweist noch keine Depot-Vollstaendigkeit.
      // PRS wird deshalb bei jeder expliziten Depot-Vorbereitung abgefragt, damit die aktuell
      // benoetigten Dependencies ermittelt, vorhandene Pakete validiert und fehlende Pakete
      // nachgeladen werden. tryPrepareViaMicrosoftPackageRetrievalService() wiederverwendet
      // bereits gueltige Dateien und laedt sie nicht erneut.
      log.info("Apple-Mobile-Offline 40k37c45: C42-PRS-DIRECT START fuer {} ueber /apple-devices/depot/prepare; vorhandenes Hauptpaket verhindert die Dependency-Pruefung nicht.",APPLE_DEVICES_STORE_ID);
      appleTrace("C42-PRS-DIRECT START", "endpoint=/discovery/apple-mobile/apple-devices/depot/prepare, storeId="+APPLE_DEVICES_STORE_ID+", validMainBefore="+validMain);
      prs=tryPrepareViaMicrosoftPackageRetrievalService(packages);
      log.info("Apple-Mobile-Offline 40k37c45: C42-PRS-DIRECT RESULT state={}, success={}, downloaded={}, message={}",prs.state(),prs.success(),prs.downloaded(),prs.message());
      appleTrace("C42-PRS-DIRECT RESULT", "state="+prs.state()+", success="+prs.success()+", downloaded="+prs.downloaded()+", message="+prs.message());
      pkgFiles=listAppxPackages(depot);
      pkgFiles=rejectInvalidDepotPackages(depot,pkgFiles);
      validMain=pkgFiles.stream().anyMatch(this::looksLikePrimaryApplePackage);

      ToolchainInstallResult tc=installToolchain();
      if(!tc.success()) throw new IllegalStateException("Pairing-Toolchain konnte nicht vorbereitet werden: "+tc.message());
      Path source=Path.of(tc.targetDirectory()); Path target=depot.resolve("libimobiledevice");
      copyDirectory(source,target);

      if(validMain && prs!=null && prs.success()){
        validateDepotPackageSignatures(pkgFiles);
        writeDepotManifest(depot,pkgFiles);
        writeDepotImportHint(depot,false,prs.message());
        appleTrace("EXIT prepareAppleOfflineDepot", "state=READY_MICROSOFT_PRS, packages="+pkgFiles.size()+", downloaded="+prs.downloaded());
        return new AppleOfflineDepotResult(true,"READY_MICROSOFT_PRS","Apple-Geräteunterstützung wurde direkt über Microsofts Package Retrieval Service aufgelöst. Hauptpaket, Abhängigkeiten und Pairing-Toolchain liegen geprüft im Offline-Depot.",OffsetDateTime.now().toString(),depot.toString(),pkgFiles.size(),true,System.currentTimeMillis()-started);
      }
      // Ein vorhandenes Hauptpaket allein darf nicht mehr zu READY fuehren. Falls PRS die
      // Vollstaendigkeit nicht bestaetigen konnte, laufen die nachgelagerten Fallbacks weiter.
      if(prs!=null) log.info("Apple-Mobile-Offline 40k37c45: Microsoft Package Retrieval Service nicht erfolgreich: {}",prs.message());

      if(!commandExistsExternal("winget")){
        PackageLinkResult rg=tryPrepareViaPackageLinkGenerators(packages);
        pkgFiles=listAppxPackages(depot);
        if(rg.success() && pkgFiles.stream().anyMatch(this::looksLikePrimaryApplePackage)){
          validateDepotPackageSignatures(pkgFiles); writeDepotManifest(depot,pkgFiles); writeDepotImportHint(depot,false,rg.message());
          return new AppleOfflineDepotResult(true,"READY_PACKAGE_LINK","Apple-Geräteunterstützung wurde über Microsoft-CDN-Links des Paket-Linkgenerator-Fallbacks vorbereitet. Pakete und Pairing-Toolchain liegen jetzt im Offline-Depot.",OffsetDateTime.now().toString(),depot.toString(),pkgFiles.size(),true,System.currentTimeMillis()-started);
        }
        writeDepotImportHint(depot,true,"WinGet ist nicht verfügbar. Paket-Linkgenerator-Fallback: "+rg.message());
        return new AppleOfflineDepotResult(false,"MANUAL_IMPORT_REQUIRED","Die Pairing-Toolchain wurde vorbereitet. WinGet fehlt und der abgesicherte Paket-Linkgenerator-Fallback konnte Apple Devices nicht automatisch übernehmen. Das original signierte Apple-Devices-Paket ("+APPLE_DEVICES_STORE_ID+") kann weiterhin nach 'packages' kopiert und danach erneut geprüft werden.",OffsetDateTime.now().toString(),depot.toString(),pkgFiles.size(),true,System.currentTimeMillis()-started);
      }

      CommandResult product=run(45,"winget","show","--id",APPLE_DEVICES_STORE_ID,"--source","msstore","--accept-source-agreements");
      String productText=(product.stdout()+" "+product.stderr()).toLowerCase(Locale.ROOT);
      if(!productText.contains(APPLE_DEVICES_STORE_ID.toLowerCase(Locale.ROOT)) || !productText.contains("apple devices")){
        writeDepotImportHint(depot,true,"Microsoft Store konnte die erwartete Apple-Devices-Produktidentität nicht bestätigen.");
        return new AppleOfflineDepotResult(false,"PRODUCT_IDENTITY_FAILED","GAM hat den Store-Download aus Sicherheitsgründen abgebrochen: Apple Devices "+APPLE_DEVICES_STORE_ID+" wurde nicht eindeutig bestätigt.",OffsetDateTime.now().toString(),depot.toString(),0,true,System.currentTimeMillis()-started);
      }

      log.info("Apple-Mobile-Offline 40k37c45: lade Apple Devices ({}) und Abhängigkeiten nach {}",APPLE_DEVICES_STORE_ID,packages);
      CommandResult dl=run(300,"winget","download","--id",APPLE_DEVICES_STORE_ID,"--source","msstore","--download-directory",packages.toString(),"--skip-license","--accept-package-agreements","--accept-source-agreements","--disable-interactivity");
      pkgFiles=listAppxPackages(depot);
      validMain=pkgFiles.stream().anyMatch(this::looksLikePrimaryApplePackage);
      if(!validMain){
        String detail=firstNonBlank(dl.stderr(),dl.stdout(),"unbekannter Fehler");
        String low=detail.toLowerCase(Locale.ROOT);
        boolean auth=low.contains("entra")||low.contains("authentifiz")||low.contains("authentication")||low.contains("lizenzadministrator")||low.contains("license administrator")||low.contains("globalen administrators")||low.contains("global administrator");
        if(auth){
          log.info("Apple-Mobile-Offline 40k37c45: Microsoft Store verlangt Entra; versuche abgesicherten Paket-Linkgenerator-Fallback.");
          PackageLinkResult rg=tryPrepareViaPackageLinkGenerators(packages);
          pkgFiles=listAppxPackages(depot);
          validMain=pkgFiles.stream().anyMatch(this::looksLikePrimaryApplePackage);
          if(rg.success() && validMain){
            validateDepotPackageSignatures(pkgFiles); writeDepotManifest(depot,pkgFiles); writeDepotImportHint(depot,false,"WinGet/Store verlangte Entra. Fallback erfolgreich: "+rg.message());
            return new AppleOfflineDepotResult(true,"READY_PACKAGE_LINK","Microsoft Store verlangte Entra. GAM hat deshalb den abgesicherten Paket-Linkgenerator-Fallback verwendet und die Pakete ausschließlich von Microsoft-CDN-Zielen übernommen. Offline-Depot ist bereit.",OffsetDateTime.now().toString(),depot.toString(),pkgFiles.size(),true,System.currentTimeMillis()-started);
          }
          writeDepotImportHint(depot,true,detail+"\n\nPaket-Linkgenerator-Fallback: "+rg.message());
          return new AppleOfflineDepotResult(false,"PACKAGE_LINK_FALLBACK_FAILED","Microsoft Store verlangt Entra und der abgesicherte Paket-Linkgenerator-Fallback konnte nicht automatisch abgeschlossen werden. Der manuelle Import bleibt möglich. Fallback-Hinweis: "+rg.message(),OffsetDateTime.now().toString(),depot.toString(),pkgFiles.size(),true,System.currentTimeMillis()-started);
        }
        writeDepotImportHint(depot,true,detail);
        return new AppleOfflineDepotResult(false,"MANUAL_IMPORT_REQUIRED","Microsoft Store hat kein verwendbares Apple-Devices-Paket abgelegt. Der manuelle Import in 'packages' bleibt möglich. Details: "+detail.replaceAll("\\s+"," ").trim(),OffsetDateTime.now().toString(),depot.toString(),pkgFiles.size(),true,System.currentTimeMillis()-started);
      }

      validateDepotPackageSignatures(pkgFiles);
      writeDepotManifest(depot,pkgFiles); writeDepotImportHint(depot,false,"");
      return new AppleOfflineDepotResult(true,"READY","Apple-Geräteunterstützung wurde vorbereitet: Apple Devices/Abhängigkeiten plus GAM-Pairing-Toolchain. Das Depot ist offline nutzbar.",OffsetDateTime.now().toString(),depot.toString(),pkgFiles.size(),true,System.currentTimeMillis()-started);
    }catch(Exception e){ log.warn("Apple-Mobile-Offlinepaket 40k37c44 fehlgeschlagen",e); return new AppleOfflineDepotResult(false,"PREPARE_FAILED",e.getMessage(),OffsetDateTime.now().toString(),safeDepotPath(),0,false,System.currentTimeMillis()-started); }
  }

  /** 40k37c45: vollständig lokaler Installationsweg für abgeschottete Windows-Rechner. */
  public AppleOfflineDepotResult installAppleFromOfflineDepot(){
    long started=System.currentTimeMillis();
    try{
      Path depot=appleOfflineDepotDirectory(); List<Path> pkgs=listAppxPackages(depot);
      if(pkgs.isEmpty()) throw new IllegalStateException("Im konfigurierten Depot wurden keine MSIX/AppX-Pakete gefunden.");
      verifyDepotManifestIfPresent(depot);
      Path main=pkgs.stream().filter(this::looksLikePrimaryApplePackage).findFirst().orElse(null);
      if(main==null) throw new IllegalStateException("Im Depot wurde kein eindeutig als Apple Devices identifizierbares Hauptpaket gefunden (erwartete Store-ID "+APPLE_DEVICES_STORE_ID+"). Fremde Apple-Pakete werden aus Sicherheitsgründen nicht installiert.");
      List<Path> deps=new ArrayList<>(); for(Path p:pkgs) if(!p.equals(main)) deps.add(p);
      String depArray=deps.stream().map(p->"'"+psQuote(p.toString())+"'").reduce((a,b)->a+","+b).orElse("");
      String ps="$ErrorActionPreference='Stop'; $main='"+psQuote(main.toString())+"'; $deps=@("+depArray+"); if($deps.Count -gt 0){Add-AppxPackage -Path $main -DependencyPath $deps -ForceApplicationShutdown}else{Add-AppxPackage -Path $main -ForceApplicationShutdown}; 'OK'";
      CommandResult r=run(180,"powershell.exe","-NoProfile","-NonInteractive","-ExecutionPolicy","Bypass","-Command",ps);
      if(!r.ok()&&!windowsAppleDevicesInstalled()) throw new IllegalStateException("Lokale Apple-Geräte-Installation fehlgeschlagen: "+firstNonBlank(r.stderr(),r.stdout(),"unbekannter Fehler"));
      Path src=depot.resolve("libimobiledevice"); Path dst=findGamRoot().resolve("tools").resolve("libimobiledevice");
      if(Files.isDirectory(src)) copyDirectory(src,dst);
      boolean tools=Files.isRegularFile(dst.resolve("idevice_id.exe"))&&Files.isRegularFile(dst.resolve("ideviceinfo.exe"))&&Files.isRegularFile(dst.resolve("idevicepair.exe"));
      boolean installed=windowsAppleDevicesInstalled();
      boolean runtime=installed&&tools&&ensureAppleMobileRuntimeReady("offline-depot-direct");
      boolean complete=installed&&tools&&runtime;
      return new AppleOfflineDepotResult(complete,complete?"INSTALLED_READY":installed&&tools?"RUNTIME_INIT_FAILED":"PARTIAL",complete?"Apple-Geräteunterstützung und Pairing-Werkzeuge wurden vollständig aus dem lokalen GAM-Depot installiert; die Apple-Mobile-Device-Laufzeit wurde automatisch initialisiert. Kein Internetzugang wurde benötigt.":installed&&tools?"Apple-Geräte und Pairing-Werkzeuge sind installiert, aber die Apple-Mobile-Device-Laufzeit konnte noch nicht initialisiert werden.":"Lokale Installation wurde ausgeführt, aber GAM erkennt noch nicht alle Komponenten.",OffsetDateTime.now().toString(),depot.toString(),pkgs.size(),tools,System.currentTimeMillis()-started);
    }catch(Exception e){ log.warn("Apple-Mobile-Offlineinstallation 40k37c44 fehlgeschlagen",e); return new AppleOfflineDepotResult(false,"INSTALL_FAILED",e.getMessage(),OffsetDateTime.now().toString(),safeDepotPath(),0,false,System.currentTimeMillis()-started); }
  }

  private record PackageLinkResult(boolean success,String state,String message,int downloaded) {}
  private record RgLink(URI uri,String fileName) {}

  private record PrsPackage(URI uri,String fullName,long sizeInBytes,String digest,boolean main) {}
  private enum FileConfidence { FULL, REDUCED_CONFIDENCE }
  private record PrsValidationResult(FileConfidence confidence,boolean digestPublished,boolean digestMatched,String actualSha256) {}

  /** 40k37c45: direkter Resolver über denselben offiziellen Microsoft Package Retrieval
   * Service, den StoreInstaller intern verwendet. Der Service liefert nur Metadaten;
   * Paketbytes werden weiterhin ausschließlich von der strikten Microsoft-CDN-Allowlist
   * akzeptiert und anschließend kryptographisch/inhaltlich geprüft. */
  private PackageLinkResult tryPrepareViaMicrosoftPackageRetrievalService(Path packages){
    try{
      String architecture=resolveWindowsArchitectureForPrs();
      String platformName=resolveWindowsDeviceFamilyForPrs();
      long platformVersion=resolveWindowsDeviceFamilyVersionForPrs();
      appleTrace("ENTER runMicrosoftPRSNow", "architecture="+architecture+", platform="+platformName+", platformVersion="+Long.toUnsignedString(platformVersion));
      String q=MICROSOFT_PACKAGE_RETRIEVAL_ENDPOINT+"?productId="+URLEncoder.encode(APPLE_DEVICES_STORE_ID,StandardCharsets.UTF_8)
          +"&architecture="+URLEncoder.encode(architecture,StandardCharsets.UTF_8)
          +"&platformName="+URLEncoder.encode(platformName,StandardCharsets.UTF_8)
          +"&platformVersion="+platformVersion;
      URI requestUri=URI.create(q);
      log.info("Apple-Mobile-Offline 40k37c45: Microsoft PRS GET {} (architecture={}, platform={}, platformVersion={})",
          MICROSOFT_PACKAGE_RETRIEVAL_ENDPOINT+"?productId="+APPLE_DEVICES_STORE_ID,architecture,platformName,platformVersion);
      appleTrace("Microsoft PRS GET", MICROSOFT_PACKAGE_RETRIEVAL_ENDPOINT+"?productId="+APPLE_DEVICES_STORE_ID+", architecture="+architecture+", platform="+platformName+", platformVersion="+Long.toUnsignedString(platformVersion));
      HttpClient client=HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(java.time.Duration.ofSeconds(20)).build();
      HttpRequest req=HttpRequest.newBuilder(requestUri)
          .header("Accept","application/json")
          .header("User-Agent","GAM-Standalone/2.1.0 (40k37c44)")
          .timeout(java.time.Duration.ofSeconds(60)).GET().build();
      HttpResponse<byte[]> resp=client.send(req,HttpResponse.BodyHandlers.ofByteArray());
      log.info("Apple-Mobile-Offline 40k37c45: Microsoft PRS Antwort HTTP={}, Content-Type={}, Bytes={}",resp.statusCode(),resp.headers().firstValue("Content-Type").orElse(""),resp.body()==null?0:resp.body().length);
      appleTrace("Microsoft PRS Antwort", "http="+resp.statusCode()+", contentType="+resp.headers().firstValue("Content-Type").orElse("")+", bytes="+(resp.body()==null?0:resp.body().length));
      if(resp.statusCode()==404) return new PackageLinkResult(false,"MICROSOFT_PRS_404","Microsoft PRS kennt das Produkt für diese Plattformparameter nicht.",0);
      if(resp.statusCode()==204) return new PackageLinkResult(false,"MICROSOFT_PRS_204","Microsoft PRS meldet für diese Plattformparameter kein Paket.",0);
      if(resp.statusCode()<200||resp.statusCode()>=300) return new PackageLinkResult(false,"MICROSOFT_PRS_HTTP","Microsoft PRS antwortete mit HTTP "+resp.statusCode()+".",0);
      if(resp.body()==null||resp.body().length==0) return new PackageLinkResult(false,"MICROSOFT_PRS_EMPTY","Microsoft PRS lieferte eine leere Antwort.",0);

      JsonNode root=new ObjectMapper().readTree(resp.body());
      JsonNode productIdNode=jsonField(root,"ProductId","productId");
      if(productIdNode!=null&&!productIdNode.asText("").isBlank()&&!APPLE_DEVICES_STORE_ID.equalsIgnoreCase(productIdNode.asText().trim()))
        throw new SecurityException("Microsoft PRS lieferte eine unerwartete ProductId: "+productIdNode.asText());
      List<PrsPackage> resolved=new ArrayList<>();
      JsonNode main=jsonField(root,"Package","package");
      if(main!=null&&!main.isNull()) resolved.add(parsePrsPackage(main,true));
      JsonNode deps=jsonField(root,"Dependencies","dependencies");
      if(deps!=null&&deps.isArray()) for(JsonNode d:deps) if(d!=null&&!d.isNull()) resolved.add(parsePrsPackage(d,false));
      if(resolved.isEmpty()||resolved.stream().noneMatch(PrsPackage::main))
        return new PackageLinkResult(false,"MICROSOFT_PRS_INCOMPLETE","Microsoft PRS lieferte kein Hauptpaket.",0);
      appleTrace("PRS JSON", "productId="+(productIdNode==null?"":traceValue(productIdNode.asText("")))+", packages="+resolved.size()+", dependencies="+Math.max(0,resolved.size()-1));

      int downloaded=0;
      Path shortTmp=findGamRoot().resolve("tmp").resolve("apple-prs");
      Files.createDirectories(shortTmp);
      int pkgIndex=0;
      for(PrsPackage pkg:resolved){
        pkgIndex++;
        boolean directCdn=isAllowedMicrosoftPackageCdnHost(pkg.uri());
        boolean prsRetrieval=isAllowedMicrosoftPrsPackageSource(pkg.uri());
        String prsSourceClass=directCdn?"PACKAGE_CDN":prsRetrieval?(pkg.main()?"PRS_RETRIEVAL_PRODUCT":"PRS_RETRIEVAL_DEPENDENCY"):"REJECT";
        appleTrace("PRS PACKAGE", "index="+pkgIndex+", main="+pkg.main()+", host="+safeHost(pkg.uri())+", path="+safePath(pkg.uri())+", sourceClass="+prsSourceClass+", fullName="+traceValue(pkg.fullName())+", sizeInBytes="+pkg.sizeInBytes()+", digest="+redactDigest(pkg.digest()));
        if(!directCdn&&!prsRetrieval) throw new SecurityException("Microsoft PRS lieferte ein nicht zugelassenes Paket-/Retrieval-Ziel: "+pkg.uri().getHost());
        String filename=prsFileName(pkg);
        Path out=packages.resolve(filename);
        if(out.toAbsolutePath().toString().length()>220){
          filename=shortPrsPackageName(pkg,pkgIndex);
          out=packages.resolve(filename);
        }
        String tempExt=prsPackageExtension(filename);
        Path part=shortTmp.resolve(pkg.main()?"main"+tempExt:String.format(Locale.ROOT,"dep%02d%s",pkgIndex,tempExt));
        appleTrace("PRS PATH", "main="+pkg.main()+", final="+out+", finalLength="+out.toAbsolutePath().toString().length()+", temp="+part+", tempLength="+part.toAbsolutePath().toString().length());
        if(Files.exists(out)){
          try{
            PrsValidationResult existingValidation=validatePrsDownloadedPackage(out,pkg,"vorhandenes PRS-Paket");
            log.info("Apple-Mobile-Offline 40k37c45: Microsoft PRS Paket bereits gültig vorhanden: {} (confidence={})",out.getFileName(),existingValidation.confidence());
            appleTrace("PRS DEPOT PRESENT", "main="+pkg.main()+", file="+out.getFileName()+", bytes="+Files.size(out)+", confidence="+existingValidation.confidence());
            continue;
          }catch(Exception stale){
            log.warn("Apple-Mobile-Offline 40k37c45: vorhandenes PRS-Paket wird ersetzt: {}",stale.getMessage());
            Files.deleteIfExists(out);
          }
        }
        Files.deleteIfExists(part);
        log.info("Apple-Mobile-Offline 40k37c45: Microsoft PRS Download {} -> {} (main={}, expectedBytes={})",pkg.uri().getHost(),filename,pkg.main(),pkg.sizeInBytes());
        appleTrace("Microsoft PRS Download", "host="+pkg.uri().getHost()+", file="+filename+", main="+pkg.main()+", expectedBytes="+pkg.sizeInBytes());
        HttpRequest dl=HttpRequest.newBuilder(pkg.uri())
            .header("User-Agent","GAM-Standalone/2.1.0 (40k37c44)")
            .timeout(java.time.Duration.ofMinutes(10)).GET().build();
        HttpResponse<Path> dr=client.send(dl,HttpResponse.BodyHandlers.ofFile(part));
        long received=Files.exists(part)?Files.size(part):0;
        log.info("Apple-Mobile-Offline 40k37c45: Microsoft PRS/CDN Download HTTP={}, final={}, Content-Type={}, Content-Length={}, empfangen={} Bytes",
            dr.statusCode(),dr.uri(),dr.headers().firstValue("Content-Type").orElse(""),dr.headers().firstValue("Content-Length").orElse(""),received);
        appleTrace("Microsoft PRS/CDN Download", "http="+dr.statusCode()+", finalHost="+(dr.uri()==null?"":dr.uri().getHost())+", contentType="+dr.headers().firstValue("Content-Type").orElse("")+", received="+received);
        if(dr.statusCode()<200||dr.statusCode()>=300) throw new IOException("Microsoft-CDN-Download fehlgeschlagen: HTTP "+dr.statusCode());
        boolean finalCdn=isAllowedMicrosoftPackageCdnHost(dr.uri());
        boolean finalPrs=isAllowedMicrosoftPrsPackageSource(dr.uri());
        appleTrace("PRS SOURCE VALIDATION", "initialClass="+(directCdn?"PACKAGE_CDN":"PRS_PACKAGE_SOURCE")+", finalClass="+(finalCdn?"PACKAGE_CDN":finalPrs?"PRS_PACKAGE_SOURCE":"REJECT")+", finalHost="+safeHost(dr.uri())+", finalPath="+safePath(dr.uri()));
        if(!finalCdn&&!finalPrs) throw new SecurityException("PRS-Download verließ die erlaubten Microsoft-Paketquellen: "+dr.uri());
        PrsValidationResult validation=validatePrsDownloadedPackage(part,pkg,"neu geladenes PRS-Paket");
        appleTrace("PRS VALIDATION OK", "main="+pkg.main()+", size="+Files.size(part)+", confidence="+validation.confidence()+", digestPublished="+validation.digestPublished()+", digestMatched="+validation.digestMatched()+", authenticode=true, signerTrust=VALID, appleIdentity="+pkg.main());
        try{ Files.move(part,out,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }
        catch(AtomicMoveNotSupportedException e){ Files.move(part,out,StandardCopyOption.REPLACE_EXISTING); }
        appleTrace("PRS DEPOT STORED", "main="+pkg.main()+", file="+out.getFileName()+", bytes="+Files.size(out)+", destination="+out);
        downloaded++;
      }
      List<Path> files=listAppxPackages(packages.getParent());
      boolean ok=files.stream().anyMatch(this::looksLikePrimaryApplePackage);
      PackageLinkResult result=new PackageLinkResult(ok,ok?"MICROSOFT_PRS_READY":"MICROSOFT_PRS_NO_MAIN",
          ok?"Microsoft PRS hat "+resolved.size()+" Paket(e) aufgelöst; "+downloaded+" neu geladen und alle vorhandenen Kandidaten geprüft.":"PRS-Abruf abgeschlossen, aber kein gültiges Apple-Devices-Hauptpaket gefunden.",downloaded);
      appleTrace("EXIT runMicrosoftPRSNow", "state="+result.state()+", success="+result.success()+", downloaded="+result.downloaded()+", resolved="+resolved.size());
      return result;
    }catch(Exception e){
      log.warn("Apple-Mobile-Offline 40k37c45: Microsoft Package Retrieval Service fehlgeschlagen: {}",e.toString());
      appleTrace("FAIL runMicrosoftPRSNow", e.getClass().getName()+": "+firstNonBlank(e.getMessage(),e.toString(),"unbekannter Fehler"));
      return new PackageLinkResult(false,"MICROSOFT_PRS_FAILED",firstNonBlank(e.getMessage(),e.toString(),"unbekannter Fehler"),0);
    }
  }

  private PrsPackage parsePrsPackage(JsonNode n,boolean main) throws Exception{
    String url=jsonText(n,"Url","url");
    if(url.isBlank()) throw new SecurityException("Microsoft PRS Paket ohne URL.");
    URI uri=URI.create(url);
    if(!uri.isAbsolute()||!("https".equalsIgnoreCase(uri.getScheme())||"http".equalsIgnoreCase(uri.getScheme())))
      throw new SecurityException("Microsoft PRS Paket-URL ist nicht absolut/http(s).");
    String full=jsonText(n,"FullName","fullName");
    String digest=jsonText(n,"Digest","digest");
    long size=jsonLong(n,"SizeInBytes","sizeInBytes");
    return new PrsPackage(uri,full,size,digest,main);
  }

  private PrsValidationResult validatePrsDownloadedPackage(Path file,PrsPackage pkg,String source) throws Exception{
    long actualSize=Files.size(file);
    if(pkg.sizeInBytes()>0 && actualSize!=pkg.sizeInBytes())
      throw new SecurityException("Dateigröße stimmt nicht: erwartet "+pkg.sizeInBytes()+" Bytes, erhalten "+actualSize+" Bytes ("+source+").");
    appleTrace("PRS SIZE OK", "file="+file.getFileName()+", main="+pkg.main()+", bytes="+actualSize+", source="+source);

    if(!looksLikeZipContainer(file)) throw new SecurityException("Datei ist kein gültiger ZIP/MSIX/AppX-Container ("+source+").");
    appleTrace("PRS CONTAINER OK", "file="+file.getFileName()+", main="+pkg.main()+", source="+source);

    String expected=normalizePrsSha256(pkg.digest());
    String actualSha="";
    boolean digestPublished=!expected.isBlank();
    boolean digestMatched=false;
    FileConfidence confidence=FileConfidence.FULL;
    if(digestPublished){
      actualSha=sha256(file);
      digestMatched=actualSha.equalsIgnoreCase(expected);
      if(digestMatched){
        appleTrace("PRS DIGEST MATCH", "file="+file.getFileName()+", main="+pkg.main()+", algorithm=SHA-256, confidence=FULL");
      }else{
        confidence=FileConfidence.REDUCED_CONFIDENCE;
        log.warn("Apple-Mobile-Offline 40k37c45: PRS SHA-256 weicht ab; Paket wird nicht allein deshalb verworfen. Datei={}, erwartet={}, erhalten={}. Weitere harte Prüfungen bleiben verpflichtend.",file.getFileName(),expected,actualSha);
        appleTrace("PRS DIGEST MISMATCH", "file="+file.getFileName()+", main="+pkg.main()+", expected="+expected+", actual="+actualSha+", confidence=REDUCED_CONFIDENCE");
      }
    } else if(!pkg.digest().isBlank()) {
      confidence=FileConfidence.REDUCED_CONFIDENCE;
      log.info("Apple-Mobile-Offline 40k37c45: PRS Digest-Format wird nicht als SHA-256 interpretiert; weitere harte Prüfungen bleiben verpflichtend (Digest={}).",redactDigest(pkg.digest()));
      appleTrace("PRS DIGEST SKIP", "file="+file.getFileName()+", main="+pkg.main()+", reason=unsupported-format, digest="+redactDigest(pkg.digest())+", confidence=REDUCED_CONFIDENCE");
    } else {
      confidence=FileConfidence.REDUCED_CONFIDENCE;
      appleTrace("PRS DIGEST SKIP", "file="+file.getFileName()+", main="+pkg.main()+", reason=not-published, confidence=REDUCED_CONFIDENCE");
    }

    appleTrace("PRS AUTHENTICODE START", "file="+file.getFileName()+", main="+pkg.main()+", source="+source);
    AuthenticodeInfo auth=verifyAuthenticodePackage(file);
    appleTrace("PRS AUTHENTICODE STATUS", "file="+file.getFileName()+", status="+traceValue(auth.status()));
    appleTrace("PRS AUTHENTICODE SIGNER", "file="+file.getFileName()+", subject="+traceValue(auth.subject()));
    appleTrace("PRS AUTHENTICODE ISSUER", "file="+file.getFileName()+", issuer="+traceValue(auth.issuer()));
    appleTrace("PRS AUTHENTICODE THUMBPRINT", "file="+file.getFileName()+", thumbprint="+traceValue(auth.thumbprint()));
    appleTrace("PRS AUTHENTICODE TRUST", "file="+file.getFileName()+", accepted="+auth.trustClass());
    appleTrace("PRS AUTHENTICODE OK", "file="+file.getFileName()+", main="+pkg.main()+", trust="+auth.trustClass());

    if(pkg.main()){
      if(!looksLikePrimaryApplePackage(file)) throw new SecurityException("PRS-Hauptpaket ist nicht eindeutig Apple Devices.");
      appleTrace("PRS APPLE IDENTITY OK", "file="+file.getFileName()+", fullName="+traceValue(pkg.fullName()));
    }

    appleTrace("PRS PACKAGE PLAUSIBILITY", "file="+file.getFileName()+", main="+pkg.main()+", accepted=true, confidence="+confidence+", evidence=source+size+container+authenticode"+(pkg.main()?"+appleIdentity":"")+(digestMatched?"+digest":""));
    return new PrsValidationResult(confidence,digestPublished,digestMatched,actualSha);
  }

  private static String normalizePrsSha256(String digest){
    if(digest==null) return "";
    String d=digest.trim();
    if(d.regionMatches(true,0,"sha256:",0,7)) d=d.substring(7).trim();
    if(d.matches("(?i)[0-9a-f]{64}")) return d.toLowerCase(Locale.ROOT);
    // Microsoft PRS liefert Digest bei Apple Devices als Base64-kodierten 32-Byte-SHA-256.
    try{
      byte[] raw=Base64.getDecoder().decode(d);
      if(raw.length==32){
        StringBuilder hex=new StringBuilder(64);
        for(byte b:raw) hex.append(String.format(Locale.ROOT,"%02x",b));
        return hex.toString();
      }
    }catch(IllegalArgumentException ignored){}
    return "";
  }
  private static String redactDigest(String d){ if(d==null||d.isBlank())return ""; String x=d.trim(); return x.length()<=20?x:x.substring(0,12)+"…"+x.substring(x.length()-6); }
  private static JsonNode jsonField(JsonNode n,String... names){
    if(n==null||!n.isObject()) return null;
    for(String name:names){ JsonNode x=n.get(name); if(x!=null) return x; }
    Iterator<Map.Entry<String,JsonNode>> it=n.fields();
    while(it.hasNext()){ var e=it.next(); for(String name:names) if(e.getKey().equalsIgnoreCase(name)) return e.getValue(); }
    return null;
  }
  private static String jsonText(JsonNode n,String... names){ JsonNode x=jsonField(n,names); return x==null||x.isNull()?"":x.asText("").trim(); }
  private static long jsonLong(JsonNode n,String... names){ JsonNode x=jsonField(n,names); if(x==null||x.isNull())return 0; if(x.isNumber())return x.asLong(); try{return Long.parseLong(x.asText().trim());}catch(Exception e){return 0;} }
  private static String prsFileName(PrsPackage p){
    String full=p.fullName()==null?"":p.fullName().trim();
    String lower=full.toLowerCase(Locale.ROOT);
    if(lower.endsWith(".msix")||lower.endsWith(".msixbundle")||lower.endsWith(".appx")||lower.endsWith(".appxbundle")) return safeFileName(full);
    String byUri=fileNameFromUri(p.uri());
    String lu=byUri.toLowerCase(Locale.ROOT);
    if(lu.endsWith(".msix")||lu.endsWith(".msixbundle")||lu.endsWith(".appx")||lu.endsWith(".appxbundle")) return safeFileName(byUri);
    String base=full.isBlank()?(p.main()?"AppleDevices":"dependency"):full;
    return safeFileName(base)+(p.main()?".msixbundle":".appx");
  }

  /** 40k37c45: Die temporäre PRS-Datei behält eine echte MSIX/AppX-Endung.
   * Windows/Get-AuthenticodeSignature kann dadurch den Pakettyp korrekt erkennen, während
   * der kurze Stagingpfad unter <GAM>/tmp/apple-prs erhalten bleibt. */
  private static String prsPackageExtension(String filename){
    String n=filename==null?"":filename.toLowerCase(Locale.ROOT);
    if(n.endsWith(".msixbundle")) return ".msixbundle";
    if(n.endsWith(".appxbundle")) return ".appxbundle";
    if(n.endsWith(".msix")) return ".msix";
    if(n.endsWith(".appx")) return ".appx";
    return ".msix";
  }

  private static String shortPrsPackageName(PrsPackage p,int index){
    String original=prsFileName(p).toLowerCase(Locale.ROOT);
    String ext=original.endsWith(".msixbundle")?".msixbundle":original.endsWith(".appxbundle")?".appxbundle":original.endsWith(".msix")?".msix":".appx";
    return p.main()?"AppleDevices"+ext:String.format(Locale.ROOT,"dep%02d%s",index,ext);
  }

  private String resolveWindowsArchitectureForPrs(){
    String a=System.getenv("PROCESSOR_ARCHITEW6432"); if(a==null||a.isBlank()) a=System.getenv("PROCESSOR_ARCHITECTURE");
    if(a==null||a.isBlank()) a=System.getProperty("os.arch","");
    String x=a.toLowerCase(Locale.ROOT);
    if(x.contains("arm64")||x.contains("aarch64")) return "arm64";
    if(x.contains("64")) return "x64";
    if(x.contains("86")) return "x86";
    return "x64";
  }
  private String resolveWindowsDeviceFamilyForPrs(){
    try{
      String ps="[Windows.System.Profile.AnalyticsInfo,Windows.System.Profile,ContentType=WindowsRuntime]::VersionInfo.DeviceFamily";
      CommandResult r=run(10,"powershell.exe","-NoProfile","-NonInteractive","-Command",ps);
      String v=r.stdout().trim(); if(r.ok()&&!v.isBlank()) return v.split("\\R")[0].trim();
    }catch(Exception ignored){}
    return "Windows.Desktop";
  }
  private long resolveWindowsDeviceFamilyVersionForPrs(){
    try{
      String ps="[Windows.System.Profile.AnalyticsInfo,Windows.System.Profile,ContentType=WindowsRuntime]::VersionInfo.DeviceFamilyVersion";
      CommandResult r=run(10,"powershell.exe","-NoProfile","-NonInteractive","-Command",ps);
      if(r.ok()) return Long.parseUnsignedLong(r.stdout().trim().split("\\R")[0].trim());
    }catch(Exception ignored){}
    // Windows encodes DeviceFamilyVersion as four 16-bit components: major.minor.build.revision.
    try{
      String ver=System.getProperty("os.version",""); String[] p=ver.split("\\.");
      long major=p.length>0?Long.parseLong(p[0]):10, minor=p.length>1?Long.parseLong(p[1]):0, build=p.length>2?Long.parseLong(p[2]):0, rev=p.length>3?Long.parseLong(p[3]):0;
      return (major<<48)|(minor<<32)|(build<<16)|rev;
    }catch(Exception e){ return (10L<<48); }
  }

  /** 40k37c45: zentraler aktiver Aufloesungspfad. Sobald GAM an irgendeiner Stelle
   * Paketlinks beschaffen muss, wird PRS selbst aktiv ausgeloest. Wir warten nicht mehr darauf,
   * dass StoreInstaller/WinGet den Microsoft-Package-Retrieval-Pfad indirekt erreicht.
   * Erst wenn der GAM-eigene PRS-Versuch scheitert, folgen rg-adguard und Appx4Fun. */
  private PackageLinkResult tryPrepareViaPackageLinkGenerators(Path packages){
    appleTrace("ENTER packageLinkGenerators", "PRS wird vor allen Community-Fallbacks aktiv ausgeloest");
    log.info("Apple-Mobile-Offline 40k37c45: C42-PRS-FORCE START – gemeinsamer Paketauflösungspfad löst Microsoft PRS jetzt selbst aktiv aus (Store-ID={}).",APPLE_DEVICES_STORE_ID);
    PackageLinkResult prs=tryPrepareViaMicrosoftPackageRetrievalService(packages);
    log.info("Apple-Mobile-Offline 40k37c45: C42-PRS-FORCE RESULT state={}, success={}, downloaded={}, message={}",prs.state(),prs.success(),prs.downloaded(),prs.message());
    appleTrace("C42-PRS-FORCE RESULT", "state="+prs.state()+", success="+prs.success()+", downloaded="+prs.downloaded()+", message="+prs.message());
    if(prs.success()) return prs;

    log.info("Apple-Mobile-Offline 40k37c45: C42-PRS-FORCE – PRS nicht erfolgreich; erst jetzt folgen Community-Linkgeneratoren.");
    PackageLinkResult rg=tryPrepareViaRgAdguard(packages);
    if(rg.success()) return rg;
    PackageLinkResult appx=tryPrepareViaAppx4Fun(packages);
    if(appx.success()) return appx;
    return new PackageLinkResult(false,"PACKAGE_LINK_GENERATORS_FAILED",
        "Microsoft PRS: "+prs.message()+" | rg-adguard: "+rg.message()+" | Appx4Fun: "+appx.message(),prs.downloaded()+rg.downloaded()+appx.downloaded());
  }

  private PackageLinkResult tryPrepareViaRgAdguard(Path packages){
    try{
      HttpClient client=HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(java.time.Duration.ofSeconds(20)).build();
      String form="type=ProductId&url="+URLEncoder.encode(APPLE_DEVICES_STORE_ID,StandardCharsets.UTF_8)+"&ring=RP&lang=en-US";
      HttpRequest req=HttpRequest.newBuilder(URI.create(RG_ADGUARD_ENDPOINT)).header("Content-Type","application/x-www-form-urlencoded").header("User-Agent","GAM-Standalone/2.1.0 (40k37c44)").timeout(java.time.Duration.ofSeconds(45)).POST(HttpRequest.BodyPublishers.ofString(form)).build();
      HttpResponse<String> res=client.send(req,HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      if(res.statusCode()<200||res.statusCode()>=300) return new PackageLinkResult(false,"HTTP_ERROR","Linkgenerator antwortete mit HTTP "+res.statusCode()+".",0);
      List<RgLink> links=parseRgAdguardLinks(res.body());
      List<RgLink> selected=links.stream().filter(this::isUsefulAppleDepotLink).toList();
      if(selected.isEmpty()) return new PackageLinkResult(false,"NO_LINKS","Keine geeigneten Apple-Devices-/Abhängigkeitslinks wurden gefunden.",0);
      Files.createDirectories(packages);
      int downloaded=0;
      for(RgLink l:selected){
        if(!isAllowedMicrosoftPackageCdnHost(l.uri())) throw new SecurityException("Nicht zugelassenes Downloadziel vom Linkgenerator: "+l.uri().getHost());
        Path out=packages.resolve(safeFileName(l.fileName())).normalize();
        if(!out.startsWith(packages.normalize())) throw new SecurityException("Ungültiger Paketdateiname.");
        if(Files.isRegularFile(out)&&Files.size(out)>0){ downloaded++; continue; }
        HttpRequest dl=HttpRequest.newBuilder(l.uri()).header("User-Agent","GAM-Standalone/2.1.0 (40k37c44)").timeout(java.time.Duration.ofMinutes(5)).GET().build();
        HttpResponse<Path> dr=client.send(dl,HttpResponse.BodyHandlers.ofFile(out));
        if(dr.statusCode()<200||dr.statusCode()>=300){ Files.deleteIfExists(out); continue; }
        verifyAuthenticodePackage(out);
        downloaded++;
      }
      List<Path> files=listAppxPackages(packages);
      boolean main=files.stream().anyMatch(this::looksLikePrimaryApplePackage);
      if(!main) return new PackageLinkResult(false,"IDENTITY_FAILED","Microsoft-CDN-Pakete wurden geladen, aber kein Paket ließ sich eindeutig als Apple Devices identifizieren.",downloaded);
      return new PackageLinkResult(true,"READY","rg-adguard lieferte "+selected.size()+" relevante Links; "+downloaded+" Paket(e) wurden von zugelassenen Microsoft-CDN-Zielen übernommen und geprüft.",downloaded);
    }catch(Exception e){
      log.warn("Apple-Mobile-Offline 40k37c45: Paket-Linkgenerator-Fallback fehlgeschlagen: {}",e.toString());
      return new PackageLinkResult(false,"FAILED",firstNonBlank(e.getMessage(),e.toString(),"unbekannter Fehler"),0);
    }
  }

  /** 40k37c45: session-/formularfähiger Appx4Fun-Fallback.
   * Appx4Fun dient ausschließlich zur Ermittlung eines temporären Microsoft-CDN-Links.
   * Der eigentliche Paketdownload muss von einem explizit zugelassenen Microsoft-Delivery-/Update-CDN-Host kommen.
   * Danach werden Hash (falls veröffentlicht), Authenticode und Apple-Devices-Identität geprüft. */
  private PackageLinkResult tryPrepareViaAppx4Fun(Path packages){
    try{
      CookieManager cookies=new CookieManager(null,CookiePolicy.ACCEPT_ORIGINAL_SERVER);
      HttpClient client=HttpClient.newBuilder()
          .cookieHandler(cookies)
          .followRedirects(HttpClient.Redirect.NORMAL)
          .connectTimeout(java.time.Duration.ofSeconds(20)).build();

      URI pageUri=URI.create(APPX4FUN_APPLE_DEVICES_PAGE);
      HttpRequest pageReq=browserRequest(pageUri,pageUri).GET().build();
      HttpResponse<String> page=client.send(pageReq,HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      if(page.statusCode()<200||page.statusCode()>=300)
        return new PackageLinkResult(false,"APPX4FUN_HTTP_ERROR","Appx4Fun antwortete mit HTTP "+page.statusCode()+".",0);

      String html=page.body()==null?"":page.body();
      String expectedName=extractAppleDevicesMsixBundleName(html);
      if(expectedName.isBlank())
        return new PackageLinkResult(false,"APPX4FUN_IDENTITY_FAILED","Appx4Fun-Seite enthält keinen eindeutig als AppleInc.AppleDevices bezeichneten MsixBundle-Dateinamen.",0);

      String publishedSha256=extractPublishedSha256(html);
      Long publishedSize=extractPublishedFileSize(html);

      // 1) Direkt sichtbare Microsoft-CDN-URLs oder Appx4Fun-Generatorlinks.
      List<URI> candidates=extractDownloadCandidates(page.uri(),html);
      logAppxCandidates("Startseite",candidates);

      // 2) Relevante HTML-Formulare wie ein Browser absenden (Hidden-Felder, Submit-Button, Session/Cookies).
      List<AppxForm> forms=parseDownloadForms(page.uri(),html);
      logAppxForms("Startseite",forms);
      for(AppxForm form:forms){
        try{
          log.info("Apple-Mobile-Offline 40k37c45: Appx4Fun Formular senden: {} {} Felder={}",form.method(),form.action(),form.fields().keySet());
          HttpResponse<String> fr=submitAppxForm(client,form,page.uri());
          log.info("Apple-Mobile-Offline 40k37c45: Appx4Fun Formularantwort: HTTP={}, final={}, Content-Type={}, Bytes={}",fr.statusCode(),fr.uri(),fr.headers().firstValue("Content-Type").orElse(""),fr.body()==null?0:fr.body().getBytes(StandardCharsets.UTF_8).length);
          URI direct=findMicrosoftCdnUri(fr.uri(),fr.body());
          if(direct!=null) candidates.add(0,direct);
          List<URI> formCandidates=extractDownloadCandidates(fr.uri(),fr.body());
          logAppxCandidates("Formularantwort "+form.action(),formCandidates);
          candidates.addAll(formCandidates);
        }catch(Exception ex){
          log.info("Apple-Mobile-Offline 40k37c45: Appx4Fun-Formular konnte nicht verarbeitet werden: {}",ex.toString());
        }
      }

      // 3) Kandidaten auflösen. Appx4Fun darf nur Linkgenerator sein; Endziel muss Microsoft sein.
      LinkedHashSet<URI> unique=new LinkedHashSet<>(candidates);
      Files.createDirectories(packages);
      int candidateNo=0;
      for(URI candidate:unique){
        candidateNo++;
        try{
          log.info("Apple-Mobile-Offline 40k37c45: Appx4Fun Kandidat {}: {}",candidateNo,candidate);
          URI finalUri=resolveAppx4FunToMicrosoft(client,candidate,page.uri());
          log.info("Apple-Mobile-Offline 40k37c45: Appx4Fun Kandidat {} aufgelöst zu: {}",candidateNo,finalUri);
          if(finalUri==null||!isAllowedMicrosoftPackageCdnHost(finalUri)) continue;

          Path out=packages.resolve(safeFileName(expectedName)).normalize();
          if(!out.startsWith(packages.normalize())) throw new SecurityException("Ungültiger Apple-Devices-Paketdateiname.");
          Path part=packages.resolve(safeFileName(expectedName)+".part").normalize();
          Files.deleteIfExists(part);

          // Ein bereits vorhandenes Ziel wird nur wiederverwendet, wenn es die vollständige
          // Sicherheitskette besteht. Andernfalls wird es verworfen und neu beschafft.
          if(Files.isRegularFile(out)&&Files.size(out)>0){
            try{
              validateDownloadedApplePackage(out,publishedSize,publishedSha256,"vorhandenes Paket");
              return new PackageLinkResult(true,"READY_APPX4FUN","Bereits vorhandenes Apple-Devices-Paket wurde vollständig geprüft und kann verwendet werden.",1);
            }catch(Exception stale){
              log.info("Apple-Mobile-Offline 40k37c45: vorhandenes Apple-Devices-Paket ist ungültig und wird ersetzt: {}",stale.toString());
              moveRejectedPackage(packages.getParent(),out,"existing-invalid");
            }
          }

          HttpRequest dl=browserRequest(finalUri,page.uri())
              .header("Accept","application/octet-stream,application/vnd.ms-appx,*/*")
              .timeout(java.time.Duration.ofMinutes(10)).GET().build();
          HttpResponse<Path> dr=client.send(dl,HttpResponse.BodyHandlers.ofFile(part));
          long received=Files.isRegularFile(part)?Files.size(part):0L;
          String contentType=dr.headers().firstValue("Content-Type").orElse("");
          String contentLength=dr.headers().firstValue("Content-Length").orElse("");
          log.info("Apple-Mobile-Offline 40k37c45: Appx4Fun/CDN Download HTTP={}, final={}, Content-Type={}, Content-Length={}, empfangen={} Bytes, erwartet={} Bytes",
              dr.statusCode(),dr.uri(),contentType,contentLength,received,publishedSize==null?"unbekannt":publishedSize);
          if(dr.statusCode()<200||dr.statusCode()>=300){ Files.deleteIfExists(part); continue; }
          if(contentType.toLowerCase(Locale.ROOT).contains("text/html")){
            Files.deleteIfExists(part);
            throw new SecurityException("Microsoft-CDN-Kandidat lieferte HTML statt eines MSIX/AppX-Pakets ("+received+" Bytes).");
          }

          validateDownloadedApplePackage(part,publishedSize,publishedSha256,"neu geladener CDN-Kandidat");
          try{
            Files.move(part,out,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
          }catch(AtomicMoveNotSupportedException ex){
            Files.move(part,out,StandardCopyOption.REPLACE_EXISTING);
          }

          return new PackageLinkResult(true,"READY_APPX4FUN",
              "Appx4Fun-Generator wurde mit Session/Formularzustand verarbeitet. Apple Devices wurde direkt vom Microsoft-CDN geladen"+
              (!publishedSha256.isBlank()?", SHA-256 verglichen":"")+
              ", signiert geprüft und eindeutig identifiziert.",1);
        }catch(Exception one){
          log.info("Apple-Mobile-Offline 40k37c45: Appx4Fun-Kandidat nicht verwendbar: {}",one.toString());
        }
      }

      return new PackageLinkResult(false,"APPX4FUN_NO_MICROSOFT_CDN",
          "Appx4Fun wurde erreicht und Generator/Formulare wurden ausgewertet, aber kein sicher auf einen zugelassenen Microsoft-CDN-Host auflösbarer Apple-Devices-Download konnte ermittelt werden.",0);
    }catch(Exception e){
      log.warn("Apple-Mobile-Offline 40k37c45: Appx4Fun-Fallback fehlgeschlagen: {}",e.toString());
      return new PackageLinkResult(false,"APPX4FUN_FAILED",firstNonBlank(e.getMessage(),e.toString(),"unbekannter Fehler"),0);
    }
  }

  private void validateDownloadedApplePackage(Path file,Long publishedSize,String publishedSha256,String source) throws Exception{
    long size=Files.size(file);
    if(publishedSize!=null && publishedSize>0 && size!=publishedSize)
      throw new SecurityException("Dateigröße stimmt nicht: erwartet "+publishedSize+" Bytes, erhalten "+size+" Bytes ("+source+").");
    if(!looksLikeZipContainer(file))
      throw new SecurityException("Datei ist kein gültiger ZIP/MSIX/AppX-Container (PK-Header fehlt, "+size+" Bytes, "+source+").");
    if(!publishedSha256.isBlank()){
      String actual=sha256(file);
      if(!actual.equalsIgnoreCase(publishedSha256)){
        log.warn("Apple-Mobile-Offline 40k37c45: veröffentlichte SHA-256 weicht ab; Datei wird nicht allein deshalb verworfen. erwartet={}, erhalten={}. Authenticode und Apple-Identität bleiben verpflichtend.",publishedSha256,actual);
        appleTrace("PACKAGE DIGEST MISMATCH", "file="+file.getFileName()+", expected="+publishedSha256+", actual="+actual+", confidence=REDUCED_CONFIDENCE");
      }else{
        appleTrace("PACKAGE DIGEST MATCH", "file="+file.getFileName()+", confidence=FULL");
      }
    }
    verifyAuthenticodePackage(file);
    if(!looksLikePrimaryApplePackage(file))
      throw new SecurityException("Paket ist nicht eindeutig Apple Devices ("+source+").");
  }

  private boolean looksLikeZipContainer(Path file){
    try(InputStream in=Files.newInputStream(file)){
      byte[] sig=in.readNBytes(4);
      return sig.length>=4 && sig[0]=='P' && sig[1]=='K' &&
          ((sig[2]==3&&sig[3]==4)||(sig[2]==5&&sig[3]==6)||(sig[2]==7&&sig[3]==8));
    }catch(Exception e){ return false; }
  }

  private List<Path> rejectInvalidDepotPackages(Path depot,List<Path> packages){
    List<Path> accepted=new ArrayList<>();
    for(Path p:packages){
      try{
        if(!looksLikeZipContainer(p)) throw new SecurityException("kein ZIP/MSIX/AppX-Container");
        verifyAuthenticodePackage(p);
        accepted.add(p);
      }catch(Exception e){
        log.warn("Apple-Mobile-Offline 40k37c45: vorhandenes Depotpaket {} ist ungültig und wird aussortiert: {}",p.getFileName(),e.getMessage());
        try{ moveRejectedPackage(depot,p,"validation-failed"); }
        catch(Exception move){ log.warn("Apple-Mobile-Offline 40k37c45: ungültiges Paket konnte nicht verschoben werden: {}",move.toString()); }
      }
    }
    return accepted;
  }

  private void moveRejectedPackage(Path depot,Path file,String reason) throws IOException{
    if(file==null||!Files.exists(file)) return;
    Path rejected=depot.resolve("rejected");
    Files.createDirectories(rejected);
    String base=file.getFileName().toString();
    String stamp=String.valueOf(System.currentTimeMillis());
    Path target=rejected.resolve(base+"."+reason+"."+stamp);
    Files.move(file,target,StandardCopyOption.REPLACE_EXISTING);
  }

  private record AppxForm(URI action,String method,LinkedHashMap<String,String> fields) {}

  private HttpRequest.Builder browserRequest(URI uri,URI referer){
    HttpRequest.Builder b=HttpRequest.newBuilder(uri)
        .header("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/152 Safari/537.36 GAM-Standalone/2.1.0-40k37c44")
        .header("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
        .header("Accept-Language","de-DE,de;q=0.9,en;q=0.7")
        .header("Cache-Control","no-cache")
        .timeout(java.time.Duration.ofSeconds(60));
    if(referer!=null) b.header("Referer",referer.toString());
    return b;
  }

  private String extractAppleDevicesMsixBundleName(String html){
    if(html==null) return "";
    java.util.regex.Matcher m=java.util.regex.Pattern.compile("(?i)(AppleInc\\.AppleDevices_[A-Za-z0-9._~\\-]+\\.MsixBundle)").matcher(html);
    return m.find()?htmlDecode(m.group(1)):"";
  }

  private String extractPublishedSha256(String html){
    if(html==null) return "";
    java.util.regex.Matcher m=java.util.regex.Pattern.compile("(?is)SHA\\s*256\\s*:?\\s*</?[^>]*>*\\s*([a-f0-9]{64})|SHA\\s*256\\s*:?\\s*([a-f0-9]{64})").matcher(html);
    if(!m.find()) return "";
    return firstNonBlank(m.group(1),m.group(2),"").trim();
  }

  private Long extractPublishedFileSize(String html){
    if(html==null) return null;
    java.util.regex.Matcher m=java.util.regex.Pattern.compile("(?is)File\\s*Size\\s*:[^\\r\\n<]*\\(([0-9]{6,})\\s*Bytes\\)").matcher(html);
    if(m.find()){ try{return Long.parseLong(m.group(1));}catch(Exception e){return null;} }
    m=java.util.regex.Pattern.compile("(?is)File\\s*Size.*?\\(([0-9]{6,})\\s*Bytes\\)").matcher(html);
    if(m.find()){ try{return Long.parseLong(m.group(1));}catch(Exception e){return null;} }
    return null;
  }

  private List<AppxForm> parseDownloadForms(URI base,String html){
    if(html==null||html.isBlank()) return List.of();
    List<AppxForm> out=new ArrayList<>();
    java.util.regex.Matcher fm=java.util.regex.Pattern.compile("(?is)<form\\b([^>]*)>(.*?)</form>").matcher(html);
    while(fm.find()){
      String attrs=fm.group(1), body=fm.group(2);
      String hay=(attrs+" "+body).replaceAll("<[^>]+>"," ").toLowerCase(Locale.ROOT);
      if(!(hay.contains("download")||hay.contains("package")||hay.contains("link"))) continue;

      String action=htmlAttribute(attrs,"action");
      String method=firstNonBlank(htmlAttribute(attrs,"method"),"GET").toUpperCase(Locale.ROOT);
      URI actionUri=action.isBlank()?base:base.resolve(htmlDecode(action));
      String host=actionUri.getHost()==null?"":actionUri.getHost().toLowerCase(Locale.ROOT);
      if(!(host.equals("appx4fun.com")||host.endsWith(".appx4fun.com"))) continue;

      LinkedHashMap<String,String> fields=new LinkedHashMap<>();
      java.util.regex.Matcher im=java.util.regex.Pattern.compile("(?is)<input\\b([^>]*)>").matcher(body);
      while(im.find()){
        String ia=im.group(1);
        String name=htmlAttribute(ia,"name");
        if(name.isBlank()) continue;
        String type=htmlAttribute(ia,"type").toLowerCase(Locale.ROOT);
        if(type.equals("checkbox")||type.equals("radio")){
          String checked=htmlAttribute(ia,"checked");
          if(checked.isBlank()&&!ia.toLowerCase(Locale.ROOT).contains(" checked")) continue;
        }
        if(type.equals("file")) continue;
        fields.put(name,htmlDecode(htmlAttribute(ia,"value")));
      }

      java.util.regex.Matcher bm=java.util.regex.Pattern.compile("(?is)<button\\b([^>]*)>(.*?)</button>").matcher(body);
      while(bm.find()){
        String ba=bm.group(1);
        String label=bm.group(2).replaceAll("<[^>]+>"," ").replaceAll("\\s+"," ").trim().toLowerCase(Locale.ROOT);
        String name=htmlAttribute(ba,"name");
        if(!name.isBlank()&&(label.contains("download")||label.contains("generate")||label.contains("link")))
          fields.put(name,htmlDecode(htmlAttribute(ba,"value")));
      }
      out.add(new AppxForm(actionUri,method,fields));
    }
    return out;
  }

  private HttpResponse<String> submitAppxForm(HttpClient client,AppxForm form,URI referer) throws Exception{
    String encoded=form.fields().entrySet().stream()
        .map(e->URLEncoder.encode(e.getKey(),StandardCharsets.UTF_8)+"="+URLEncoder.encode(firstNonBlank(e.getValue(),""),StandardCharsets.UTF_8))
        .collect(java.util.stream.Collectors.joining("&"));
    if(form.method().equals("POST")){
      HttpRequest req=browserRequest(form.action(),referer)
          .header("Content-Type","application/x-www-form-urlencoded")
          .POST(HttpRequest.BodyPublishers.ofString(encoded)).build();
      return client.send(req,HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
    String sep=form.action().getQuery()==null?"?":"&";
    URI u=URI.create(form.action().toString()+(encoded.isBlank()?"":sep+encoded));
    HttpRequest req=browserRequest(u,referer).GET().build();
    return client.send(req,HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
  }

  /** 40k37c45: Kandidaten werden bewusst auf die konkrete Apple-Devices-Appx4Fun-Seite begrenzt.
   * Kein generisches Crawling von top/, total-downloads/ oder fremden /xap/<id>/ mehr. */
  private List<URI> extractDownloadCandidates(URI base,String html){
    if(html==null||html.isBlank()) return List.of();
    LinkedHashSet<URI> out=new LinkedHashSet<>();
    String decoded=htmlDecode(html);

    // Bereits direkt sichtbare, echte Microsoft-Paket-CDN-URLs dürfen sofort durch.
    java.util.regex.Matcher raw=java.util.regex.Pattern.compile("(?i)https?://[^\"'<>\s]+").matcher(decoded);
    while(raw.find()){
      try{
        URI u=URI.create(raw.group().replace("&amp;","&"));
        if(isAllowedMicrosoftPackageCdnHost(u)) out.add(u);
      }catch(Exception ignored){}
    }

    // Appx4Fun-spezifisch: nur go.php des aktuellen Posts und der #generate-Anker derselben Seite.
    java.util.regex.Matcher a=java.util.regex.Pattern.compile("(?is)<a\s+([^>]*)>(.*?)</a>").matcher(html);
    while(a.find()){
      String attrs=a.group(1);
      String href=htmlAttribute(attrs,"href");
      if(href.isBlank()) continue;
      try{
        URI u=base.resolve(htmlDecode(href));
        String host=u.getHost()==null?"":u.getHost().toLowerCase(Locale.ROOT);
        String path=u.getPath()==null?"":u.getPath();
        String query=u.getRawQuery()==null?"":u.getRawQuery();
        String fragment=u.getFragment()==null?"":u.getFragment();

        if(isAllowedMicrosoftPackageCdnHost(u)){ out.add(u); continue; }
        if(host.equals("appx4fun.com")||host.endsWith(".appx4fun.com")){
          if(path.equalsIgnoreCase("/go.php") && query.toLowerCase(Locale.ROOT).contains("post=56771")){
            out.add(u); continue;
          }
          if(path.equalsIgnoreCase("/xap/56771/") && fragment.equalsIgnoreCase("generate")){
            out.add(u); continue;
          }
        }
      }catch(Exception ignored){}
    }

    // Manche Seiten schreiben go.php-URLs nur in Script/JSON statt in ein <a>.
    java.util.regex.Matcher go=java.util.regex.Pattern.compile("(?i)(?:https?://www\\.appx4fun\\.com)?/go\\.php\\?[^\"'<>\\s]*post=56771[^\"'<>\\s]*").matcher(decoded);
    while(go.find()){
      try{ out.add(base.resolve(go.group().replace("&amp;","&"))); }catch(Exception ignored){}
    }

    return new ArrayList<>(out);
  }

  private URI decodeAppx4FunTarget(URI appxUri){
    if(appxUri==null||appxUri.getRawQuery()==null) return null;
    for(String part:appxUri.getRawQuery().split("&")){
      int eq=part.indexOf('=');
      if(eq<=0) continue;
      String key=URLDecoder.decode(part.substring(0,eq),StandardCharsets.UTF_8);
      if(!key.equalsIgnoreCase("link")) continue;
      String value=URLDecoder.decode(part.substring(eq+1),StandardCharsets.UTF_8);
      try{ return URI.create(value); }catch(Exception ignored){ return null; }
    }
    return null;
  }

  private boolean isMicrosoftInstallerBridge(URI u){
    if(u==null||u.getHost()==null) return false;
    String h=u.getHost().toLowerCase(Locale.ROOT);
    String p=u.getPath()==null?"":u.getPath().toLowerCase(Locale.ROOT);
    return h.equals("get.microsoft.com") && p.startsWith("/installer/download/");
  }

  private void logAppxCandidates(String context,List<URI> candidates){
    if(candidates==null||candidates.isEmpty()){
      log.info("Apple-Mobile-Offline 40k37c45: Appx4Fun {}: keine Kandidaten gefunden.",context);
      return;
    }
    int i=0;
    for(URI u:new LinkedHashSet<>(candidates)){
      i++;
      log.info("Apple-Mobile-Offline 40k37c45: Appx4Fun {} Kandidat {}: {} [Paket-CDN={}]",context,i,u,isAllowedMicrosoftPackageCdnHost(u));
    }
  }

  private void logAppxForms(String context,List<AppxForm> forms){
    if(forms==null||forms.isEmpty()){
      log.info("Apple-Mobile-Offline 40k37c45: Appx4Fun {}: keine Download-/Generatorformulare gefunden.",context);
      return;
    }
    int i=0;
    for(AppxForm f:forms){
      i++;
      log.info("Apple-Mobile-Offline 40k37c45: Appx4Fun {} Formular {}: {} {} Felder={}",context,i,f.method(),f.action(),f.fields().keySet());
    }
  }

  /** 40k37c45: produktspezifischer Resolver.
   * Erlaubt nur /xap/56771/#generate, go.php?post=56771 und Microsofts Installer-Brücke. */
  private URI resolveAppx4FunToMicrosoft(HttpClient client,URI start,URI referer) throws Exception{
    URI cur=start;
    URI ref=referer;
    LinkedHashSet<String> visited=new LinkedHashSet<>();
    for(int i=0;i<8 && cur!=null;i++){
      if(!visited.add(cur.toString())){
        log.info("Apple-Mobile-Offline 40k37c45: Appx4Fun Resolver beendet Schleife bei {}",cur);
        return null;
      }
      if(isAllowedMicrosoftPackageCdnHost(cur)) return cur;

      String host=cur.getHost()==null?"":cur.getHost().toLowerCase(Locale.ROOT);
      String path=cur.getPath()==null?"":cur.getPath();

      // go.php enthält das eigentliche Ziel bereits URL-kodiert. Dieses Ziel zuerst auswerten,
      // statt Appx4Fun quer über die Webseite weiter zu crawlen.
      if((host.equals("appx4fun.com")||host.endsWith(".appx4fun.com")) && path.equalsIgnoreCase("/go.php")){
        String q=cur.getRawQuery()==null?"":cur.getRawQuery().toLowerCase(Locale.ROOT);
        if(!q.contains("post=56771")) return null;
        URI embedded=decodeAppx4FunTarget(cur);
        log.info("Apple-Mobile-Offline 40k37c45: Appx4Fun go.php Ziel dekodiert: {}",embedded);
        if(embedded!=null){
          if(isAllowedMicrosoftPackageCdnHost(embedded)) return embedded;
          if(isMicrosoftInstallerBridge(embedded)){ cur=embedded; continue; }
        }
      }

      // Microsoft get.microsoft.com ist nur eine kontrollierte Zwischenstation. 40k37c44
      // behandelt die Antwort als Binärdaten und analysiert sie ausschließlich passiv.
      if(isMicrosoftInstallerBridge(cur)){
        log.info("Apple-Mobile-Offline 40k37c45: Microsoft Installer-Brücke: GET {}",cur);
        HttpRequest r=browserRequest(cur,ref).header("Accept","application/octet-stream,*/*").GET().build();
        HttpResponse<byte[]> resp=client.send(r,HttpResponse.BodyHandlers.ofByteArray());
        URI effective=resp.uri();
        byte[] bridge=resp.body()==null?new byte[0]:resp.body();
        String contentType=resp.headers().firstValue("Content-Type").orElse("");
        log.info("Apple-Mobile-Offline 40k37c45: Microsoft Installer-Brücke Antwort: HTTP={}, final={}, Content-Type={}, Bytes={}",resp.statusCode(),effective,contentType,bridge.length);
        if(isAllowedMicrosoftPackageCdnHost(effective)) return effective;
        URI cdn=inspectMicrosoftInstallerBridgeBinary(effective,bridge,contentType);
        if(cdn!=null) return cdn;
        return null;
      }

      if(!(host.equals("appx4fun.com")||host.endsWith(".appx4fun.com"))) return null;
      boolean sameProduct=path.equalsIgnoreCase("/xap/56771/");
      if(!sameProduct && !path.equalsIgnoreCase("/go.php")) return null;

      log.info("Apple-Mobile-Offline 40k37c45: Appx4Fun Produkt-Resolver Hop {}: GET {} (Referer={})",i+1,cur,ref);
      HttpRequest r=browserRequest(cur,ref).GET().build();
      HttpResponse<String> resp=client.send(r,HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      URI effective=resp.uri();
      String body=resp.body()==null?"":resp.body();
      log.info("Apple-Mobile-Offline 40k37c45: Appx4Fun Produkt-Resolver Hop {} Antwort: HTTP={}, final={}, Content-Type={}, Bytes={}",i+1,resp.statusCode(),effective,resp.headers().firstValue("Content-Type").orElse(""),body.getBytes(StandardCharsets.UTF_8).length);
      if(isAllowedMicrosoftPackageCdnHost(effective)) return effective;
      URI direct=findMicrosoftCdnUri(effective,body);
      if(direct!=null) return direct;

      List<URI> nested=extractDownloadCandidates(effective,body);
      logAppxCandidates("Produkt-Resolver Hop "+(i+1),nested);

      URI next=null;
      // Priorität 1: go.php mit link=get.microsoft.com/installer/download/...
      for(URI u:nested){
        if(u.getPath()!=null && u.getPath().equalsIgnoreCase("/go.php")){
          URI embedded=decodeAppx4FunTarget(u);
          if(isMicrosoftInstallerBridge(embedded)){ next=u; break; }
        }
      }
      // Priorität 2: sonstiger go.php-Kandidat desselben Posts.
      if(next==null) next=nested.stream().filter(u->u.getPath()!=null&&u.getPath().equalsIgnoreCase("/go.php")).findFirst().orElse(null);
      // Priorität 3: #generate derselben Apple-Seite.
      if(next==null) next=nested.stream().filter(u->"generate".equalsIgnoreCase(u.getFragment())).findFirst().orElse(null);
      if(next==null) return null;
      ref=effective;
      cur=next;
    }
    return null;
  }

  /** 40k37c45: passive Strukturanalyse der von get.microsoft.com gelieferten Installer-Brücke.
   * Die Datei wird niemals ausgeführt. Neben Hash/Authenticode werden PE-Kopf, Versionsdaten
   * und relevante ASCII-/UTF-16-Strings ausgewertet. Als Ergebnis akzeptieren wir weiterhin
   * ausschließlich einen Link auf den strikten Microsoft-Paket-CDN. */
  private URI inspectMicrosoftInstallerBridgeBinary(URI base,byte[] data,String contentType){
    if(data==null||data.length==0) return null;
    Path tmp=null;
    try{
      String magic=bridgeMagic(data);
      MessageDigest md=MessageDigest.getInstance("SHA-256");
      StringBuilder hs=new StringBuilder();
      for(byte b:md.digest(data)) hs.append(String.format("%02x",b));
      log.info("Apple-Mobile-Offline 40k37c45: Microsoft Installer-Brücke Binärdiagnose: Typ={}, Content-Type={}, Bytes={}, SHA-256={}",magic,contentType,data.length,hs);

      if("PE/EXE".equals(magic)){
        logPeSummary(data);
        inspectDotNetMetadata(data);
        compareMicrosoftBridgeBinary(data,hs.toString());
      }

      // Nur zur Signatur- und Versionsinformationsprüfung temporär speichern; niemals starten.
      if(System.getProperty("os.name","").toLowerCase(Locale.ROOT).contains("win")){
        tmp=Files.createTempFile("gam-apple-microsoft-bridge-",magic.equals("PE/EXE")?".exe":".bin");
        Files.write(tmp,data,StandardOpenOption.TRUNCATE_EXISTING);
        String ps="$p='"+psQuote(tmp.toString())+"'; " +
            "$s=Get-AuthenticodeSignature -LiteralPath $p; " +
            "Write-Output ('Status='+$s.Status); if($s.SignerCertificate){Write-Output ('Signer='+$s.SignerCertificate.Subject); Write-Output ('Thumbprint='+$s.SignerCertificate.Thumbprint)}; " +
            "$v=(Get-Item -LiteralPath $p).VersionInfo; if($v){Write-Output ('FileDescription='+$v.FileDescription); Write-Output ('ProductName='+$v.ProductName); Write-Output ('CompanyName='+$v.CompanyName); Write-Output ('FileVersion='+$v.FileVersion); Write-Output ('ProductVersion='+$v.ProductVersion); Write-Output ('OriginalFilename='+$v.OriginalFilename)}";
        CommandResult sig=run(20,"powershell.exe","-NoProfile","-NonInteractive","-Command",ps);
        String sigText=(firstNonBlank(sig.stdout(),sig.stderr(),"keine Signaturinformation")).replace("\r"," ").replace("\n"," | ").trim();
        log.info("Apple-Mobile-Offline 40k37c45: Microsoft Installer-Brücke Authenticode/Version: {}",sigText);
      }

      // Zuerst weiterhin direkt nach einem bereits eingebetteten, strikt erlaubten CDN-Link suchen.
      URI cdn=findMicrosoftCdnUri(base,new String(data,StandardCharsets.ISO_8859_1));
      if(cdn==null && data.length>=2) cdn=findMicrosoftCdnUri(base,new String(data,StandardCharsets.UTF_16LE));
      if(cdn!=null){
        log.info("Apple-Mobile-Offline 40k37c45: Microsoft Installer-Brücke enthält strikten Paket-CDN-Kandidaten: {}",cdn);
        return cdn;
      }

      // c22: strukturierte, rein passive String-Sichtung. Nur relevante Treffer loggen,
      // damit Hardwarelogs nicht mit beliebigen Binärstrings überflutet werden.
      List<String> hints=extractBridgeRelevantStrings(data);
      if(hints.isEmpty()){
        log.info("Apple-Mobile-Offline 40k37c45: Microsoft Installer-Brücke: keine relevanten Store/API/Paket-Strings extrahierbar.");
      }else{
        log.info("Apple-Mobile-Offline 40k37c45: Microsoft Installer-Brücke: {} relevante strukturierte String-Hinweise gefunden.",hints.size());
        int n=0;
        for(String h:hints){
          if(++n>40){ log.info("Apple-Mobile-Offline 40k37c45: weitere String-Hinweise unterdrückt (gesamt={}).",hints.size()); break; }
          log.info("Apple-Mobile-Offline 40k37c45: Bridge-Hinweis {}: {}",n,h);
          URI hinted=uriFromBridgeHint(h);
          if(hinted!=null && isAllowedMicrosoftPackageCdnHost(hinted)){
            log.info("Apple-Mobile-Offline 40k37c45: Bridge-Hinweis {} ist strikter Paket-CDN-Link: {}",n,hinted);
            return hinted;
          }
        }
      }

      log.info("Apple-Mobile-Offline 40k37c45: Microsoft Installer-Brücke enthält noch keinen direkt nutzbaren strikten Paket-CDN-Link; Binärdatei wurde nicht ausgeführt.");
    }catch(Exception ex){
      log.info("Apple-Mobile-Offline 40k37c45: Microsoft Installer-Brücke Strukturanalyse fehlgeschlagen: {}",ex.toString());
    }finally{
      if(tmp!=null) try{Files.deleteIfExists(tmp);}catch(Exception ignored){}
    }
    return null;
  }

  private void logPeSummary(byte[] d){
    try{
      if(d.length<0x40) return;
      int pe=le32(d,0x3c);
      if(pe<0 || pe+24>d.length || d[pe]!='P'||d[pe+1]!='E'||d[pe+2]!=0||d[pe+3]!=0) return;
      int machine=le36(d,pe+4);
      int sections=le36(d,pe+6);
      long timestamp=Integer.toUnsignedLong(le32(d,pe+8));
      int optSize=le36(d,pe+20);
      int opt=pe+24;
      int magic=(opt+2<=d.length)?le36(d,opt):0;
      int subsystem=(opt+70<=d.length)?le36(d,opt+68):-1;
      List<String> names=new ArrayList<>();
      int sh=opt+optSize;
      for(int i=0;i<sections && i<32 && sh+40<=d.length;i++,sh+=40){
        int end=sh; while(end<sh+8 && end<d.length && d[end]!=0) end++;
        String name=new String(d,sh,Math.max(0,end-sh),StandardCharsets.ISO_8859_1).trim();
        if(!name.isBlank()) names.add(name);
      }
      log.info("Apple-Mobile-Offline 40k37c45: Microsoft Installer-Brücke PE: Machine=0x{}, Sections={}, OptionalMagic=0x{}, Subsystem={}, TimeDateStamp={}, SectionNames={}",
          Integer.toHexString(machine),sections,Integer.toHexString(magic),subsystem,timestamp,names);
    }catch(Exception ex){
      log.info("Apple-Mobile-Offline 40k37c45: PE-Kopfanalyse nicht möglich: {}",ex.toString());
    }
  }

  /** 40k37c45: liest den CLR-Header und den ECMA-335-Metadatenroot rein binär.
   * Es wird keine Assembly geladen und kein Code aus der Bridge ausgeführt. */
  private void inspectDotNetMetadata(byte[] d){
    try{
      PeLayout pe=parsePeLayout(d);
      if(pe==null){ log.info("Apple-Mobile-Offline 40k37c45: CLR-Metadaten: kein auswertbares PE-Layout."); return; }
      int dd=pe.optionalOffset()+(pe.pe32Plus()?112:96);
      int comEntry=dd+(14*8);
      if(comEntry+8>d.length){ log.info("Apple-Mobile-Offline 40k37c45: CLR-Metadaten: COM-Descriptor nicht vorhanden."); return; }
      long clrRva=Integer.toUnsignedLong(le32(d,comEntry));
      long clrSize=Integer.toUnsignedLong(le32(d,comEntry+4));
      if(clrRva==0){ log.info("Apple-Mobile-Offline 40k37c45: CLR-Metadaten: PE besitzt keinen CLR-Header."); return; }
      int clrOff=rvaToOffset(pe,clrRva);
      if(clrOff<0||clrOff+16>d.length){ log.info("Apple-Mobile-Offline 40k37c45: CLR-Metadaten: CLR-RVA 0x{} nicht abbildbar.",Long.toHexString(clrRva)); return; }
      int major=le36(d,clrOff+4), minor=le36(d,clrOff+6);
      long mdRva=Integer.toUnsignedLong(le32(d,clrOff+8));
      long mdSize=Integer.toUnsignedLong(le32(d,clrOff+12));
      int mdOff=rvaToOffset(pe,mdRva);
      log.info("Apple-Mobile-Offline 40k37c45: CLR-Header: RVA=0x{}, Size={}, Runtime={}.{}, MetadataRVA=0x{}, MetadataSize={}",Long.toHexString(clrRva),clrSize,major,minor,Long.toHexString(mdRva),mdSize);
      if(mdOff<0||mdOff+20>d.length||le32(d,mdOff)!=0x424A5342){ log.info("Apple-Mobile-Offline 40k37c45: CLR-Metadatenroot nicht gueltig/abbildbar (Offset={}).",mdOff); return; }
      int verLen=le32(d,mdOff+12);
      if(verLen<0||verLen>1024||mdOff+16+verLen+4>d.length) return;
      String runtime=new String(d,mdOff+16,verLen,StandardCharsets.UTF_8).replace("\0","").trim();
      int pos=align4(mdOff+16+verLen);
      if(pos+4>d.length) return;
      int streams=le36(d,pos+2); pos+=4;
      LinkedHashMap<String,int[]> streamMap=new LinkedHashMap<>();
      for(int i=0;i<streams && i<32 && pos+8<=d.length;i++){
        int off=le32(d,pos), size=le32(d,pos+4); pos+=8;
        int nameStart=pos, nameEnd=pos;
        while(nameEnd<d.length && nameEnd-nameStart<32 && d[nameEnd]!=0) nameEnd++;
        if(nameEnd>=d.length) break;
        String name=new String(d,nameStart,nameEnd-nameStart,StandardCharsets.US_ASCII);
        pos=align4(nameEnd+1);
        streamMap.put(name,new int[]{off,size});
      }
      log.info("Apple-Mobile-Offline 40k37c45: CLR-Metadaten: RuntimeVersion={}, Streams={}",runtime,streamMap.keySet());
      int[] strings=streamMap.get("#Strings");
      if(strings!=null){
        int start=mdOff+strings[0], end=Math.min(d.length,start+Math.max(0,strings[1]));
        if(start>=0&&start<end){
          LinkedHashSet<String> interesting=new LinkedHashSet<>();
          int i=start;
          while(i<end){
            int j=i; while(j<end&&d[j]!=0) j++;
            if(j>i){
              String x=new String(d,i,j-i,StandardCharsets.UTF_8).trim();
              String l=x.toLowerCase(Locale.ROOT);
              if(x.length()>=4 && x.length()<=400 && (l.contains("store")||l.contains("catalog")||l.contains("product")||l.contains("download")||l.contains("package")||l.contains("manifest")||l.contains("endpoint")||l.contains("uri")||l.contains("http")||l.contains("wpm")||l.contains("license")||l.contains("framework")||l.contains("installer"))) interesting.add(x);
            }
            i=j+1;
          }
          log.info("Apple-Mobile-Offline 40k37c45: CLR-#Strings: {} relevante Metadaten-Namen gefunden.",interesting.size());
          int n=0; for(String x:interesting){ if(++n>80){ log.info("Apple-Mobile-Offline 40k37c45: weitere CLR-Metadaten-Namen unterdrueckt (gesamt={}).",interesting.size()); break; } log.info("Apple-Mobile-Offline 40k37c45: CLR-Metadaten-Hinweis {}: {}",n,x); }
        }
      }
      inspectTargetClrMethodIl(d,mdOff,streamMap);
    }catch(Exception ex){ log.info("Apple-Mobile-Offline 40k37c45: CLR-Metadatenanalyse fehlgeschlagen: {}",ex.toString()); }
  }

  /** 40k37c45: verfolgt gezielt relevante CLR-MethodDef-Eintraege bis in ihre IL-Koerper.
   * Es wird ausschliesslich die ECMA-335-Dateistruktur gelesen; die Assembly wird weder geladen noch ausgefuehrt. */
  private void inspectTargetClrMethodIl(byte[] d,int mdOff,Map<String,int[]> streamMap){
    try{
      int[] tables=streamMap.get("#~");
      int[] strings=streamMap.get("#Strings");
      int[] us=streamMap.get("#US");
      if(tables==null||strings==null){ log.info("Apple-Mobile-Offline 40k37c45: CLR-IL: #~/#Strings fehlen; MethodDef-Analyse nicht moeglich."); return; }
      int t0=mdOff+tables[0], tend=Math.min(d.length,t0+Math.max(0,tables[1]));
      if(t0<0||t0+24>tend) return;
      int heapSizes=d[t0+6]&255;
      long valid=le64(d,t0+8);
      int pos=t0+24;
      long[] rows=new long[64];
      for(int table=0;table<64;table++) if(((valid>>>table)&1L)!=0){ if(pos+4>tend) return; rows[table]=Integer.toUnsignedLong(le32(d,pos)); pos+=4; }
      int tableData=pos;
      int strIx=(heapSizes&1)!=0?4:2, guidIx=(heapSizes&2)!=0?4:2, blobIx=(heapSizes&4)!=0?4:2;
      int[] rowSize=new int[64];
      rowSize[0]=2+strIx+guidIx*3;
      rowSize[1]=codedIndexSize(rows,new int[]{0,26,35,1},2)+strIx+strIx;
      rowSize[2]=4+strIx+strIx+codedIndexSize(rows,new int[]{2,1,27},2)+simpleIndexSize(rows,4)+simpleIndexSize(rows,6);
      rowSize[3]=simpleIndexSize(rows,4);
      rowSize[4]=2+strIx+blobIx;
      rowSize[5]=simpleIndexSize(rows,6);
      rowSize[6]=4+2+2+strIx+blobIx+simpleIndexSize(rows,8);
      rowSize[7]=simpleIndexSize(rows,8);
      rowSize[8]=2+2+strIx;
      rowSize[9]=simpleIndexSize(rows,2)+codedIndexSize(rows,new int[]{2,1,27},2);
      rowSize[10]=codedIndexSize(rows,new int[]{2,1,26,6,27},3)+strIx+blobIx; // MemberRef
      // 40k37c45: Tabellen bis MethodSpec vollstaendig genug berechnen, damit FieldDef,
      // MemberRef und MethodSpec auch in realen StoreInstaller-Assemblies korrekt adressiert werden.
      rowSize[11]=2+2+codedIndexSize(rows,new int[]{4,8,23},2)+blobIx; // Constant
      rowSize[12]=codedIndexSize(rows,new int[]{6,4,1,2,8,9,10,0,14,23,20,17,26,27,32,35,38,39,40,42,44,43},5)+codedIndexSize(rows,new int[]{6,10},3)+blobIx; // CustomAttribute
      rowSize[13]=codedIndexSize(rows,new int[]{4,8},1)+blobIx; // FieldMarshal
      rowSize[14]=2+codedIndexSize(rows,new int[]{2,6,32},2)+blobIx; // DeclSecurity
      rowSize[15]=2+4+simpleIndexSize(rows,2); // ClassLayout
      rowSize[16]=4+simpleIndexSize(rows,4); // FieldLayout
      rowSize[17]=blobIx; // StandAloneSig
      rowSize[18]=simpleIndexSize(rows,2)+simpleIndexSize(rows,20); // EventMap
      rowSize[19]=simpleIndexSize(rows,20); // EventPtr
      rowSize[20]=2+strIx+codedIndexSize(rows,new int[]{2,1,27},2); // Event
      rowSize[21]=simpleIndexSize(rows,2)+simpleIndexSize(rows,23); // PropertyMap
      rowSize[22]=simpleIndexSize(rows,23); // PropertyPtr
      rowSize[23]=2+strIx+blobIx; // Property
      rowSize[24]=2+simpleIndexSize(rows,6)+codedIndexSize(rows,new int[]{20,23},1); // MethodSemantics
      rowSize[25]=simpleIndexSize(rows,2)+2*codedIndexSize(rows,new int[]{6,10},1); // MethodImpl
      rowSize[26]=strIx; // ModuleRef
      rowSize[27]=blobIx; // TypeSpec
      rowSize[28]=2+codedIndexSize(rows,new int[]{4,6},1)+strIx+simpleIndexSize(rows,26); // ImplMap
      rowSize[29]=4+simpleIndexSize(rows,4); // FieldRVA
      rowSize[30]=8; rowSize[31]=4; // ENCLog / ENCMap
      rowSize[32]=4+2+2+2+2+4+blobIx+strIx+strIx; // Assembly
      rowSize[33]=4; rowSize[34]=12; // AssemblyProcessor / AssemblyOS
      rowSize[35]=2+2+2+2+4+blobIx+strIx+strIx+blobIx; // AssemblyRef
      rowSize[36]=4+simpleIndexSize(rows,35);
      rowSize[37]=12+simpleIndexSize(rows,35);
      rowSize[38]=4+strIx+blobIx; // File
      rowSize[39]=4+4+strIx+strIx+codedIndexSize(rows,new int[]{38,35,39},2); // ExportedType
      rowSize[40]=4+4+strIx+codedIndexSize(rows,new int[]{38,35,39},2); // ManifestResource
      rowSize[41]=simpleIndexSize(rows,2)+simpleIndexSize(rows,2); // NestedClass
      rowSize[42]=2+2+codedIndexSize(rows,new int[]{2,6},1)+strIx; // GenericParam
      rowSize[43]=codedIndexSize(rows,new int[]{6,10},1)+blobIx; // MethodSpec
      rowSize[44]=simpleIndexSize(rows,42)+codedIndexSize(rows,new int[]{2,1,27},2); // GenericParamConstraint
      int[] start=new int[64]; int cur=tableData;
      for(int table=0;table<64;table++){
        start[table]=cur;
        if(rows[table]>0 && rowSize[table]>0){ long next=(long)cur+rows[table]*rowSize[table]; if(next>Integer.MAX_VALUE||next>tend) { if(table<=10) return; break; } cur=(int)next; }
        else if(rows[table]>0 && table<=10){ log.info("Apple-Mobile-Offline 40k37c45: CLR-IL: unbekannte Zeilengroesse fuer Tabelle {} vor MethodDef-Auswertung.",table); return; }
      }
      Map<Integer,String> methodNames=new HashMap<>();
      Map<Integer,Long> methodRvas=new HashMap<>();
      Map<Integer,String> methodOwners=new HashMap<>();
      Set<String> targets=Set.of("FindPackageInCatalogAsync","GetProductEntitlementAsync","DownloadOneCoreAsync","SendDownloadAsync","ConnectToPackageAsync","DownloadAllAsync","DownloadFrameworkIntoSlotAsync","AddPackageByUriAsync","CreateFindPackagesOptions","CreatePackageMatchFilter","get_Uri","get_SizeInBytes","get_FullName","get_DownloadData","set_Uri","set_SizeInBytes","set_FullName","DownloadDataTaskAsync");
      int mstart=start[6], msize=rowSize[6];
      for(int rid=1;rid<=rows[6] && rid<=200000;rid++){
        int o=mstart+(rid-1)*msize; if(o<0||o+msize>d.length) break;
        long rva=Integer.toUnsignedLong(le32(d,o));
        int nameIx=readIndex(d,o+8,strIx);
        String name=readHeapString(d,mdOff+strings[0],strings[1],nameIx);
        methodNames.put(rid,name); methodRvas.put(rid,rva);
      }
      // 40k37c45: ordnet MethodDefs ihrem TypeDef zu. Dadurch werden gleichnamige Getter/
      // Hilfsmethoden (z.B. get_Uri) im Log nicht mehr kontextlos dargestellt.
      int tdStartOwners=start[2], tdSizeOwners=rowSize[2];
      int tdOrRefOwners=codedIndexSize(rows,new int[]{2,1,27},2), fieldIxOwners=simpleIndexSize(rows,4), methodIxOwners=simpleIndexSize(rows,6);
      for(int tr=1;tr<=rows[2] && tr<=100000;tr++){
        int o=tdStartOwners+(tr-1)*tdSizeOwners; if(o<0||o+tdSizeOwners>d.length) break;
        String tn=readHeapString(d,mdOff+strings[0],strings[1],readIndex(d,o+4,strIx));
        String ns=readHeapString(d,mdOff+strings[0],strings[1],readIndex(d,o+4+strIx,strIx));
        String owner=(ns==null||ns.isBlank()?tn:ns+"."+tn);
        int methodListOff=o+4+strIx+strIx+tdOrRefOwners+fieldIxOwners; int first=readIndex(d,methodListOff,methodIxOwners);
        int next=(int)rows[6]+1; if(tr<rows[2]){ int no=tdStartOwners+tr*tdSizeOwners; next=readIndex(d,no+4+strIx+strIx+tdOrRefOwners+fieldIxOwners,methodIxOwners); }
        for(int mr=Math.max(1,first);mr<next&&mr<=rows[6];mr++) methodOwners.put(mr,owner);
      }
      PeLayout pe=parsePeLayout(d);
      for(int rid=1;rid<=rows[6] && rid<=200000;rid++){
        String name=methodNames.get(rid);
        if(targets.contains(name)){
          long rva=methodRvas.getOrDefault(rid,0L); int off=rvaToOffset(pe,rva);
          String owner=methodOwners.getOrDefault(rid,"?");
          log.info("Apple-Mobile-Offline 40k37c45: CLR-MethodDef Ziel: {}.{} Token=0x{} RVA=0x{} FileOffset={}.",owner,name,String.format("%08x",0x06000000|rid),Long.toHexString(rva),off);
          inspectIlBody(d,off,owner+"."+name,methodNames,mdOff,streamMap,rows,start,rowSize,strIx,blobIx,us);
        }
      }
      // 40k37c45: Call-Sites der beiden entscheidenden Bruecken suchen. Damit wird sichtbar,
      // welcher Wert als packageId in FindPackageInCatalogAsync fliesst und wo DownloadData
      // fuer den spaeteren HTTP-Downloader erzeugt bzw. befuellt wird.
      inspectClrCallSites(d,pe,methodNames,methodRvas,methodOwners,mdOff,streamMap,rows,start,rowSize,strIx,blobIx,us,
          Set.of("FindPackageInCatalogAsync","DownloadDataTaskAsync","get_DownloadData"));

      // 40k37c45: Struktur der DTO-/Download-Datentypen separat ausgeben. Das ist rein passive
      // Metadatenanalyse und hilft, ProductId/PackageId sowie Uri/Size/FullName zusammenzufuehren.
      inspectClrInterestingTypes(d,pe,methodNames,methodRvas,mdOff,streamMap,rows,start,rowSize,strIx,blobIx,us,
          Set.of("PackageWithDependenciesDto","PackageInfo","PackageDto","ProductV1"));

      // 40k37c45: jetzt nicht mehr nach dem mehrdeutigen BCL-Namen DownloadDataTaskAsync suchen,
      // sondern von den tatsaechlichen StoreInstaller-Datentraegern und _connectedPackage aus.
      // So werden die Methoden sichtbar, die CatalogPackage in PackageInfo/PackageDto (Uri/Size/FullName)
      // umsetzen und damit die letzte Bruecke zum HTTP-Downloader bilden.
      inspectClrPackageDataFlow(d,pe,methodNames,methodRvas,methodOwners,mdOff,streamMap,rows,start,rowSize,strIx,blobIx,us);

      // Bei async C# steckt die eigentliche Logik regelmaessig in MoveNext der erzeugten State-Machine.
      int tdStart=start[2], tdSize=rowSize[2]; int tdOrRef=codedIndexSize(rows,new int[]{2,1,27},2), fieldIx=simpleIndexSize(rows,4), methodIx=simpleIndexSize(rows,6);
      for(int tr=1;tr<=rows[2] && tr<=100000;tr++){
        int o=tdStart+(tr-1)*tdSize; if(o<0||o+tdSize>d.length) break;
        int nameIx=readIndex(d,o+4,strIx); String typeName=readHeapString(d,mdOff+strings[0],strings[1],nameIx);
        String target=null; for(String t:targets) if(typeName.contains("<"+t+">")||typeName.contains(t)){ target=t; break; }
        if(target==null) continue;
        int methodListOff=o+4+strIx+strIx+tdOrRef+fieldIx; int first=readIndex(d,methodListOff,methodIx);
        int next=(int)rows[6]+1; if(tr<rows[2]){ int no=tdStart+tr*tdSize; next=readIndex(d,no+4+strIx+strIx+tdOrRef+fieldIx,methodIx); }
        for(int mr=Math.max(1,first);mr<next&&mr<=rows[6];mr++) if("MoveNext".equals(methodNames.get(mr))){
          long rva=methodRvas.getOrDefault(mr,0L); int off=rvaToOffset(pe,rva); String label=target+"/MoveNext";
          log.info("Apple-Mobile-Offline 40k37c45: CLR-Async-StateMachine Ziel: Typ={} Methode=MoveNext Token=0x{} RVA=0x{} FileOffset={}.",typeName,String.format("%08x",0x06000000|mr),Long.toHexString(rva),off);
          inspectIlBody(d,off,label,methodNames,mdOff,streamMap,rows,start,rowSize,strIx,blobIx,us);
        }
      }
    }catch(Exception ex){ log.info("Apple-Mobile-Offline 40k37c45: CLR-IL-Methodenanalyse fehlgeschlagen: {}",ex.toString()); }
  }

  /** 40k37c45: findet passiv alle CLR-Methoden, die die interessanten WPM-/DownloadData-Methoden aufrufen. */
  private void inspectClrCallSites(byte[] d,PeLayout pe,Map<Integer,String> methodNames,Map<Integer,Long> methodRvas,Map<Integer,String> methodOwners,
      int mdOff,Map<String,int[]> streamMap,long[] rows,int[] start,int[] rowSize,int strIx,int blobIx,int[] us,Set<String> targetNames){
    try{
      int hits=0;
      for(int rid=1;rid<=rows[6]&&rid<=200000;rid++){
        long rva=methodRvas.getOrDefault(rid,0L); if(rva==0) continue;
        int off=rvaToOffset(pe,rva); if(off<0||off>=d.length) continue;
        int b=d[off]&255, codeStart, codeSize;
        if((b&3)==2){ codeSize=b>>>2; codeStart=off+1; }
        else if((b&3)==3&&off+12<=d.length){ int flags=le36(d,off),hdr=((flags>>>12)&15)*4; codeSize=le32(d,off+4); codeStart=off+hdr; }
        else continue;
        if(codeSize<0||codeSize>1024*1024||codeStart<0||codeStart+codeSize>d.length) continue;
        boolean matched=false; String matchName=null; int p=codeStart,end=codeStart+codeSize;
        while(p<end){
          int op=d[p++]&255, op2=-1; if(op==0xfe){ if(p>=end) break; op2=d[p++]&255; }
          int kind=ilOperandKind(op,op2), n=ilOperandBytes(kind,d,p,end); if(n<0||p+n>end) break;
          if(kind==6 && (op==0x28||op==0x6f||op==0x73)){
            int token=le32(d,p),table=(token>>>24)&255,tr=token&0xffffff;
            String resolved=resolveClrTokenName(d,table,tr,methodNames,mdOff,streamMap,rows,start,rowSize,strIx,blobIx);
            if(resolved!=null) for(String t:targetNames) if(resolved.contains(t)){ matched=true; matchName=resolved; break; }
          }
          p+=n; if(matched) break;
        }
        if(matched){
          String owner=methodOwners.getOrDefault(rid,"?"); String mn=methodNames.getOrDefault(rid,"MethodDef#"+rid);
          log.info("Apple-Mobile-Offline 40k37c45: CLR-CALLSITE Treffer {}: {}.{} -> {} Token=0x{}.",++hits,owner,mn,matchName,String.format("%08x",0x06000000|rid));
          // Call-Sites vollstaendig dekodieren; gerade hier liegen ProductId/PackageId-Getter und DTO-Zugriffe.
          inspectIlBody(d,off,"CALLSITE "+owner+"."+mn,methodNames,mdOff,streamMap,rows,start,rowSize,strIx,blobIx,us);
          if(hits>=40){ log.info("Apple-Mobile-Offline 40k37c45: CLR-CALLSITE weitere Treffer unterdrueckt."); break; }
        }
      }
      log.info("Apple-Mobile-Offline 40k37c45: CLR-CALLSITE Analyse abgeschlossen, Treffer={}.",hits);
    }catch(Exception ex){ log.info("Apple-Mobile-Offline 40k37c45: CLR-CALLSITE Analyse fehlgeschlagen: {}",ex.toString()); }
  }

  /** 40k37c45: listet Felder/Methoden ausgewaehlter StoreInstaller-Typen und dekodiert relevante Bodies. */
  private void inspectClrInterestingTypes(byte[] d,PeLayout pe,Map<Integer,String> methodNames,Map<Integer,Long> methodRvas,
      int mdOff,Map<String,int[]> streamMap,long[] rows,int[] start,int[] rowSize,int strIx,int blobIx,int[] us,Set<String> wanted){
    try{
      int[] strings=streamMap.get("#Strings"); if(strings==null) return;
      int tdOrRef=codedIndexSize(rows,new int[]{2,1,27},2), fieldIx=simpleIndexSize(rows,4), methodIx=simpleIndexSize(rows,6);
      for(int tr=1;tr<=rows[2]&&tr<=100000;tr++){
        int o=start[2]+(tr-1)*rowSize[2]; if(o<0||o+rowSize[2]>d.length) break;
        String type=resolveTypeDefName(d,tr,mdOff,strings,rows,start,rowSize,strIx); boolean match=false;
        for(String w:wanted) if(type!=null&&type.contains(w)){ match=true; break; } if(!match) continue;
        int fieldListOff=o+4+strIx+strIx+tdOrRef, firstField=readIndex(d,fieldListOff,fieldIx), nextField=(int)rows[4]+1;
        int methodListOff=fieldListOff+fieldIx, firstMethod=readIndex(d,methodListOff,methodIx), nextMethod=(int)rows[6]+1;
        if(tr<rows[2]){
          int no=start[2]+tr*rowSize[2]; nextField=readIndex(d,no+4+strIx+strIx+tdOrRef,fieldIx);
          nextMethod=readIndex(d,no+4+strIx+strIx+tdOrRef+fieldIx,methodIx);
        }
        log.info("Apple-Mobile-Offline 40k37c45: CLR-TYPE {} TypeDef#{} Felder={} Methoden={}.",type,tr,Math.max(0,nextField-firstField),Math.max(0,nextMethod-firstMethod));
        int shown=0;
        for(int fr=Math.max(1,firstField);fr<nextField&&fr<=rows[4]&&shown<80;fr++,shown++){
          String f=resolveClrTokenName(d,4,fr,methodNames,mdOff,streamMap,rows,start,rowSize,strIx,blobIx);
          log.info("Apple-Mobile-Offline 40k37c45: CLR-TYPE {} Feld {}: {} [0x{}].",type,shown+1,f,String.format("%08x",0x04000000|fr));
        }
        shown=0;
        for(int mr=Math.max(1,firstMethod);mr<nextMethod&&mr<=rows[6]&&shown<120;mr++,shown++){
          String mn=methodNames.getOrDefault(mr,"MethodDef#"+mr);
          log.info("Apple-Mobile-Offline 40k37c45: CLR-TYPE {} Methode {}: {} [0x{}].",type,shown+1,mn,String.format("%08x",0x06000000|mr));
          String l=mn.toLowerCase(Locale.ROOT);
          if(l.equals(".ctor")||l.contains("productid")||l.contains("packageid")||l.contains("downloaddata")||l.contains("uri")||l.contains("size")||l.contains("fullname")){
            long rva=methodRvas.getOrDefault(mr,0L); int mo=rvaToOffset(pe,rva); if(rva!=0&&mo>=0)
              inspectIlBody(d,mo,"TYPE "+type+"."+mn,methodNames,mdOff,streamMap,rows,start,rowSize,strIx,blobIx,us);
          }
        }
      }
    }catch(Exception ex){ log.info("Apple-Mobile-Offline 40k37c45: CLR-TYPE Analyse fehlgeschlagen: {}",ex.toString()); }
  }

  /** 40k37c45: fokussierte Vorwaertsanalyse ab WpmInstaller._connectedPackage und den
   * StoreInstaller-eigenen PackageInfo/PackageDto-Datentraegern. Rein passiv: die EXE wird nie geladen
   * oder ausgefuehrt. Erfasst Feldzugriffe sowie Konstruktor-/Setter-/Getter-Aufrufe und dekodiert den
   * kompletten aufrufenden IL-Body, damit die Herkunft von Uri, SizeInBytes und FullName sichtbar wird. */
  private void inspectClrPackageDataFlow(byte[] d,PeLayout pe,Map<Integer,String> methodNames,Map<Integer,Long> methodRvas,Map<Integer,String> methodOwners,
      int mdOff,Map<String,int[]> streamMap,long[] rows,int[] start,int[] rowSize,int strIx,int blobIx,int[] us){
    try{
      int hits=0;
      for(int rid=1;rid<=rows[6]&&rid<=200000;rid++){
        long rva=methodRvas.getOrDefault(rid,0L); if(rva==0) continue;
        int off=rvaToOffset(pe,rva); if(off<0||off>=d.length) continue;
        int b=d[off]&255, codeStart, codeSize;
        if((b&3)==2){ codeSize=b>>>2; codeStart=off+1; }
        else if((b&3)==3&&off+12<=d.length){ int flags=le36(d,off),hdr=((flags>>>12)&15)*4; codeSize=le32(d,off+4); codeStart=off+hdr; }
        else continue;
        if(codeSize<0||codeSize>1024*1024||codeStart<0||codeStart+codeSize>d.length) continue;
        LinkedHashSet<String> reasons=new LinkedHashSet<>();
        int p=codeStart,end=codeStart+codeSize;
        while(p<end){
          int op=d[p++]&255,op2=-1; if(op==0xfe){ if(p>=end) break; op2=d[p++]&255; }
          int kind=ilOperandKind(op,op2),n=ilOperandBytes(kind,d,p,end); if(n<0||p+n>end) break;
          if(kind==6){
            int token=le32(d,p),table=(token>>>24)&255,tr=token&0xffffff;
            String r=resolveClrTokenName(d,table,tr,methodNames,mdOff,streamMap,rows,start,rowSize,strIx,blobIx);
            if(r!=null){
              String l=r.toLowerCase(Locale.ROOT);
              if(l.contains("w pminstaller._connectedpackage".replace(" ",""))||
                 l.contains("packageinfo.set_uri")||l.contains("packageinfo.set_sizeinbytes")||l.contains("packageinfo.set_fullname")||
                 l.contains("packagedto.set_uri")||l.contains("packagedto.set_sizeinbytes")||l.contains("packagedto.set_fullname")||
                 l.contains("packageinfo..ctor")||l.contains("packagedto..ctor")||
                 l.contains("microsoft.management.deployment.catalogpackage")||
                 l.contains("microsoft.management.deployment.packageversioninfo")||
                 l.contains("microsoft.management.deployment.packageversionid")||
                 l.contains("microsoft.management.deployment.packagecatalog")) reasons.add(r);
            }
          }
          p+=n;
        }
        if(!reasons.isEmpty()){
          String owner=methodOwners.getOrDefault(rid,"?"),mn=methodNames.getOrDefault(rid,"MethodDef#"+rid);
          // trivial getter/setter bodies der Datentraeger selbst nicht als Datenfluss-Treffer aufblasen
          boolean trivial=(owner.contains("PackageInfo")||owner.contains("PackageDto")) && (mn.startsWith("get_")||mn.startsWith("set_"));
          if(trivial) continue;
          log.info("Apple-Mobile-Offline 40k37c45: CLR-PACKAGE-FLOW Treffer {}: {}.{} Token=0x{} Gruende={}",++hits,owner,mn,String.format("%08x",0x06000000|rid),reasons);
          inspectIlBody(d,off,"PACKAGE-FLOW "+owner+"."+mn,methodNames,mdOff,streamMap,rows,start,rowSize,strIx,blobIx,us);
          if(hits>=60){ log.info("Apple-Mobile-Offline 40k37c45: CLR-PACKAGE-FLOW weitere Treffer unterdrueckt."); break; }
        }
      }
      log.info("Apple-Mobile-Offline 40k37c45: CLR-PACKAGE-FLOW Analyse abgeschlossen, Treffer={}.",hits);
    }catch(Exception ex){ log.info("Apple-Mobile-Offline 40k37c45: CLR-PACKAGE-FLOW Analyse fehlgeschlagen: {}",ex.toString()); }
  }

  private void inspectIlBody(byte[] d,int off,String method,Map<Integer,String> methodNames,int mdOff,Map<String,int[]> streamMap,long[] rows,int[] start,int[] rowSize,int strIx,int blobIx,int[] us){
    try{
      if(off<0||off>=d.length) return;
      int b=d[off]&255, codeStart, codeSize;
      if((b&3)==2){ codeSize=b>>>2; codeStart=off+1; }
      else if((b&3)==3 && off+12<=d.length){ int flags=le36(d,off); int hdr=((flags>>>12)&15)*4; codeSize=le32(d,off+4); codeStart=off+hdr; }
      else { log.info("Apple-Mobile-Offline 40k37c45: CLR-IL {}: unbekanntes Method-Headerformat 0x{}.",method,Integer.toHexString(b)); return; }
      if(codeSize<0||codeSize>1024*1024||codeStart<0||codeStart+codeSize>d.length) return;
      LinkedHashSet<String> calls=new LinkedHashSet<>(), literals=new LinkedHashSet<>();
      for(int i=codeStart;i+4<codeStart+codeSize;i++){
        int op=d[i]&255;
        if(op==0x28||op==0x6f||op==0x73){
          int token=le32(d,i+1); int table=(token>>>24)&255, rid=token&0x00ffffff;
          String resolved=resolveClrTokenName(d,table,rid,methodNames,mdOff,streamMap,rows,start,rowSize,strIx,blobIx);
          if(resolved!=null) calls.add((op==0x28?"call ":op==0x6f?"callvirt ":"newobj ")+resolved+" [0x"+String.format("%08x",token)+"]");
        }else if(op==0x72 && us!=null){
          int token=le32(d,i+1); if(((token>>>24)&255)==0x70){ String x=readUserString(d,mdOff+us[0],us[1],token&0x00ffffff); if(x!=null&&!x.isBlank()) literals.add(x); }
        }
      }
      log.info("Apple-Mobile-Offline 40k37c45: CLR-IL {}: CodeSize={}, aufgeloesteAufrufe={}, StringLiterale={}.",method,codeSize,calls.size(),literals.size());
      int n=0; for(String x:calls){ if(++n>60){ log.info("Apple-Mobile-Offline 40k37c45: CLR-IL {}: weitere Aufrufe unterdrueckt (gesamt={}).",method,calls.size()); break; } log.info("Apple-Mobile-Offline 40k37c45: CLR-IL {} Aufruf {}: {}",method,n,x); }
      // 40k37c45: echter IL-Schrittdecoder fuer die Katalog-/DownloadData-Kette. Anders als der
      // bisherige Byte-Scan springt er ueber Operanden hinweg und zeigt damit auch die Werte,
      // die unmittelbar vor put_Field/put_Option/put_Value auf den Evaluation Stack gelegt werden.
      if(method.contains("FindPackageInCatalogAsync")||method.contains("CreateFindPackagesOptions")||
          method.contains("CreatePackageMatchFilter")||method.contains("get_Uri")||method.contains("get_SizeInBytes")||
          method.contains("get_FullName")||method.contains("get_DownloadData")||method.contains("DownloadDataTaskAsync")||
          method.contains("DownloadOneCoreAsync/MoveNext")||method.startsWith("CALLSITE ")||method.startsWith("TYPE ")||method.startsWith("PACKAGE-FLOW "))
        decodeIlInstructions(d,codeStart,codeSize,method,methodNames,mdOff,streamMap,rows,start,rowSize,strIx,blobIx,us);
      n=0; for(String x:literals){
        String l=x.toLowerCase(Locale.ROOT);
        boolean focused=method.contains("CreateFindPackagesOptions")||method.contains("CreatePackageMatchFilter")||method.contains("get_Uri")||method.contains("get_SizeInBytes")||method.contains("get_FullName");
        if(!focused && !(l.contains("http")||l.contains("store")||l.contains("catalog")||l.contains("product")||l.contains("package")||l.contains("download")||l.contains("license")||l.contains("uri")||l.contains("wpm"))) continue;
        if(++n>40) break;
        log.info("Apple-Mobile-Offline 40k37c45: CLR-IL {} Literal {}: {}",method,n,x.replace("\r"," ").replace("\n"," "));
      }
    }catch(Exception ex){ log.info("Apple-Mobile-Offline 40k37c45: CLR-IL {} Analyse fehlgeschlagen: {}",method,ex.toString()); }
  }


  /** 40k37c45: linearer ECMA-335 IL-Decoder fuer Diagnosezwecke. Keine Assembly-Ausfuehrung,
   * kein Reflection/Loading. Ziel ist die Operand-/Konstantenfolge rund um die WPM-Aufrufe. */
  private void decodeIlInstructions(byte[] d,int codeStart,int codeSize,String method,Map<Integer,String> methodNames,int mdOff,Map<String,int[]> streamMap,long[] rows,int[] start,int[] rowSize,int strIx,int blobIx,int[] us){
    try{
      int end=codeStart+codeSize, p=codeStart, count=0;
      while(p<end && count<500){
        int ins=p, op=d[p++]&255, op2=-1;
        if(op==0xfe){ if(p>=end) break; op2=d[p++]&255; }
        String name=ilOpcodeName(op,op2); int kind=ilOperandKind(op,op2); int n=ilOperandBytes(kind,d,p,end);
        if(n<0||p+n>end){ log.info("Apple-Mobile-Offline 40k37c45: CLR-IL-DECODE {} IL_{}: {} <abgebrochen: Operand ausserhalb Method Body>",method,String.format("%04x",ins-codeStart),name); break; }
        String operand="";
        if(kind==1) operand=Integer.toString((byte)d[p]);
        else if(kind==2) operand=Integer.toString(le32(d,p));
        else if(kind==3) operand=Long.toString(le64(d,p));
        else if(kind==4) operand="IL_"+String.format("%04x",(p+1-codeStart)+(byte)d[p]);
        else if(kind==5) operand="IL_"+String.format("%04x",(p+4-codeStart)+le32(d,p));
        else if(kind==6){ int token=le32(d,p), table=(token>>>24)&255,rid=token&0xffffff; String r=resolveClrTokenName(d,table,rid,methodNames,mdOff,streamMap,rows,start,rowSize,strIx,blobIx); operand="0x"+String.format("%08x",token)+(r==null?"":" => "+r); }
        else if(kind==7){ int token=le32(d,p); String x=(us!=null&&((token>>>24)&255)==0x70)?readUserString(d,mdOff+us[0],us[1],token&0xffffff):null; operand="0x"+String.format("%08x",token)+(x==null?"":" => \""+x.replace("\r"," ").replace("\n"," ")+"\""); }
        else if(kind==8) operand=Integer.toString(d[p]&255);
        else if(kind==9) operand=Integer.toString(le36(d,p));
        else if(kind==10){ int c=le32(d,p); StringBuilder b=new StringBuilder("cases=").append(c); int q=p+4; for(int i=0;i<c&&i<32;i++,q+=4) b.append(i==0?" [":",").append("IL_").append(String.format("%04x",(p+4+4*c-codeStart)+le32(d,q))); if(c>0)b.append(']'); operand=b.toString(); }
        else if(kind==11) operand=Float.toString(Float.intBitsToFloat(le32(d,p)));
        else if(kind==12) operand=Double.toString(Double.longBitsToDouble(le64(d,p)));
        // Short-form integer constants have no encoded operand; expose their effective value.
        if(op2<0 && op>=0x16 && op<=0x1e) operand=Integer.toString(op-0x16);
        else if(op2<0 && op==0x15) operand="-1";
        log.info("Apple-Mobile-Offline 40k37c45: CLR-IL-DECODE {} IL_{}: {}{}",method,String.format("%04x",ins-codeStart),name,operand.isEmpty()?"":" "+operand);
        p+=n; count++;
      }
      log.info("Apple-Mobile-Offline 40k37c45: CLR-IL-DECODE {}: {} Instruktionen linear dekodiert.",method,count);
    }catch(Exception ex){ log.info("Apple-Mobile-Offline 40k37c45: CLR-IL-DECODE {} fehlgeschlagen: {}",method,ex.toString()); }
  }

  /* Operand-Kind: 0 none, 1 i1, 2 i4, 3 i8, 4 short branch, 5 branch, 6 metadata token,
     7 user string, 8 u1 var, 9 u2 var, 10 switch, 11 r4, 12 r8. */
  private int ilOperandKind(int op,int op2){
    if(op==0xfe){
      return switch(op2){
        case 0x06,0x07,0x15,0x16,0x1c -> 6; // ldftn/ldvirtftn/initobj/constrained/sizeof
        case 0x09,0x0a,0x0b,0x0c,0x0d,0x0e -> 9; // ldarg/ldarga/starg/ldloc/ldloca/stloc
        default -> 0;
      };
    }
    if(op>=0x0e&&op<=0x13) return 8; // short var
    if(op==0x1f) return 1; if(op==0x20) return 2; if(op==0x21) return 3; if(op==0x22) return 11; if(op==0x23) return 12;
    if(op==0x27||op==0x28||op==0x29||op==0x6f||op==0x70||op==0x71||op==0x73||op==0x74||op==0x75||op==0x79||
       op==0x7b||op==0x7c||op==0x7d||op==0x7e||op==0x7f||op==0x80||op==0x81||op==0x8c||op==0x8d||op==0x8f||
       op==0xa3||op==0xa4||op==0xa5||op==0xc2||op==0xc6||op==0xd0) return 6;
    if(op==0x72) return 7;
    if((op>=0x2b&&op<=0x37)||op==0xde) return 4;
    if((op>=0x38&&op<=0x44)||op==0xdd) return 5;
    if(op==0x45) return 10;
    return 0;
  }
  private int ilOperandBytes(int kind,byte[] d,int p,int end){
    return switch(kind){ case 0->0; case 1,4,8->1; case 2,5,6,7,11->4; case 3,12->8; case 9->2; case 10->{ if(p+4>end) yield -1; long c=Integer.toUnsignedLong(le32(d,p)); long n=4+4*c; yield n>Integer.MAX_VALUE?-1:(int)n; } default->-1; };
  }
  private String ilOpcodeName(int op,int op2){
    if(op==0xfe) return switch(op2){case 0x01->"ceq";case 0x02->"cgt";case 0x03->"cgt.un";case 0x04->"clt";case 0x05->"clt.un";case 0x06->"ldftn";case 0x07->"ldvirtftn";case 0x09->"ldarg";case 0x0a->"ldarga";case 0x0b->"starg";case 0x0c->"ldloc";case 0x0d->"ldloca";case 0x0e->"stloc";case 0x11->"endfilter";case 0x12->"unaligned.";case 0x13->"volatile.";case 0x14->"tail.";case 0x15->"initobj";case 0x16->"constrained.";case 0x17->"cpblk";case 0x18->"initblk";case 0x1a->"rethrow";case 0x1c->"sizeof";case 0x1e->"readonly.";default->"fe."+String.format("%02x",op2);};
    return switch(op){case 0x00->"nop";case 0x01->"break";case 0x02->"ldarg.0";case 0x03->"ldarg.1";case 0x04->"ldarg.2";case 0x05->"ldarg.3";case 0x06->"ldloc.0";case 0x07->"ldloc.1";case 0x08->"ldloc.2";case 0x09->"ldloc.3";case 0x0a->"stloc.0";case 0x0b->"stloc.1";case 0x0c->"stloc.2";case 0x0d->"stloc.3";case 0x0e->"ldarg.s";case 0x0f->"ldarga.s";case 0x10->"starg.s";case 0x11->"ldloc.s";case 0x12->"ldloca.s";case 0x13->"stloc.s";case 0x14->"ldnull";case 0x15->"ldc.i4.m1";case 0x16->"ldc.i4.0";case 0x17->"ldc.i4.1";case 0x18->"ldc.i4.2";case 0x19->"ldc.i4.3";case 0x1a->"ldc.i4.4";case 0x1b->"ldc.i4.5";case 0x1c->"ldc.i4.6";case 0x1d->"ldc.i4.7";case 0x1e->"ldc.i4.8";case 0x1f->"ldc.i4.s";case 0x20->"ldc.i4";case 0x21->"ldc.i8";case 0x22->"ldc.r4";case 0x23->"ldc.r8";case 0x25->"dup";case 0x26->"pop";case 0x27->"jmp";case 0x28->"call";case 0x29->"calli";case 0x2a->"ret";case 0x2b->"br.s";case 0x2c->"brfalse.s";case 0x2d->"brtrue.s";case 0x2e->"beq.s";case 0x2f->"bge.s";case 0x30->"bgt.s";case 0x31->"ble.s";case 0x32->"blt.s";case 0x33->"bne.un.s";case 0x34->"bge.un.s";case 0x35->"bgt.un.s";case 0x36->"ble.un.s";case 0x37->"blt.un.s";case 0x38->"br";case 0x39->"brfalse";case 0x3a->"brtrue";case 0x3b->"beq";case 0x3c->"bge";case 0x3d->"bgt";case 0x3e->"ble";case 0x3f->"blt";case 0x40->"bne.un";case 0x41->"bge.un";case 0x42->"bgt.un";case 0x43->"ble.un";case 0x44->"blt.un";case 0x45->"switch";case 0x58->"add";case 0x59->"sub";case 0x5a->"mul";case 0x5b->"div";case 0x5d->"rem";case 0x5f->"and";case 0x60->"or";case 0x61->"xor";case 0x62->"shl";case 0x63->"shr";case 0x64->"shr.un";case 0x65->"neg";case 0x66->"not";case 0x69->"conv.i4";case 0x6a->"conv.i8";case 0x6c->"conv.r8";case 0x6d->"conv.u4";case 0x6e->"conv.u8";case 0x6f->"callvirt";case 0x70->"cpobj";case 0x71->"ldobj";case 0x72->"ldstr";case 0x73->"newobj";case 0x74->"castclass";case 0x75->"isinst";case 0x76->"conv.r.un";case 0x79->"unbox";case 0x7a->"throw";case 0x7b->"ldfld";case 0x7c->"ldflda";case 0x7d->"stfld";case 0x7e->"ldsfld";case 0x7f->"ldsflda";case 0x80->"stsfld";case 0x81->"stobj";case 0x8c->"box";case 0x8d->"newarr";case 0x8e->"ldlen";case 0x8f->"ldelema";case 0xa2->"stelem.ref";case 0xa3->"ldelem";case 0xa4->"stelem";case 0xa5->"unbox.any";case 0xc2->"refanyval";case 0xc3->"ckfinite";case 0xc6->"mkrefany";case 0xd0->"ldtoken";case 0xdc->"endfinally";case 0xdd->"leave";case 0xde->"leave.s";default->"op."+String.format("%02x",op);};
  }

  /** 40k37c45: vollqualifizierte Aufloesung der fuer die WPM-Kette wichtigen CLR-Tokens. */
  private String resolveClrTokenName(byte[] d,int table,int rid,Map<Integer,String> methodNames,int mdOff,Map<String,int[]> streamMap,long[] rows,int[] start,int[] rowSize,int strIx,int blobIx){
    try{
      if(rid<=0) return null;
      int[] strings=streamMap.get("#Strings"); if(strings==null) return null;
      if(table==1) return resolveTypeRefName(d,rid,mdOff,strings,rows,start,rowSize,strIx);
      if(table==2) return resolveTypeDefName(d,rid,mdOff,strings,rows,start,rowSize,strIx);
      if(table==4){
        if(rid>rows[4]) return "FieldDef#"+rid;
        int o=start[4]+(rid-1)*rowSize[4];
        String name=readHeapString(d,mdOff+strings[0],strings[1],readIndex(d,o+2,strIx));
        String owner=resolveFieldOwner(d,rid,mdOff,strings,rows,start,rowSize,strIx);
        return (owner==null||owner.isBlank()?"FieldDef":owner)+"."+name;
      }
      if(table==6){
        String name=methodNames.getOrDefault(rid,"MethodDef#"+rid);
        String owner=resolveMethodOwner(d,rid,mdOff,strings,rows,start,rowSize,strIx);
        return (owner==null||owner.isBlank()?"MethodDef":owner)+"."+name;
      }
      if(table==10 && rid<=rows[10]){
        int o=start[10]+(rid-1)*rowSize[10];
        int parentSize=codedIndexSize(rows,new int[]{2,1,26,6,27},3);
        int rawParent=readIndex(d,o,parentSize), tag=rawParent&7, pr=rawParent>>>3;
        String owner=switch(tag){
          case 0 -> resolveTypeDefName(d,pr,mdOff,strings,rows,start,rowSize,strIx);
          case 1 -> resolveTypeRefName(d,pr,mdOff,strings,rows,start,rowSize,strIx);
          case 2 -> resolveModuleRefName(d,pr,mdOff,strings,rows,start,rowSize,strIx);
          case 3 -> resolveClrTokenName(d,6,pr,methodNames,mdOff,streamMap,rows,start,rowSize,strIx,blobIx);
          case 4 -> "TypeSpec#"+pr;
          default -> "MemberRefParent(tag="+tag+",rid="+pr+")";
        };
        int nameIx=readIndex(d,o+parentSize,strIx);
        String name=readHeapString(d,mdOff+strings[0],strings[1],nameIx);
        return (owner==null||owner.isBlank()?"MemberRef":owner)+"."+name;
      }
      if(table==26) return resolveModuleRefName(d,rid,mdOff,strings,rows,start,rowSize,strIx);
      if(table==27) return "TypeSpec#"+rid;
      if(table==43 && rid<=rows[43]){
        int o=start[43]+(rid-1)*rowSize[43];
        int ixSize=codedIndexSize(rows,new int[]{6,10},1), raw=readIndex(d,o,ixSize), tag=raw&1, mr=raw>>>1;
        String target=resolveClrTokenName(d,tag==0?6:10,mr,methodNames,mdOff,streamMap,rows,start,rowSize,strIx,blobIx);
        return "MethodSpec:"+(target==null?(tag==0?"MethodDef#":"MemberRef#")+mr:target);
      }
      return "table"+table+"#"+rid;
    }catch(Exception e){ return null; }
  }

  private String resolveTypeDefName(byte[] d,int rid,int mdOff,int[] strings,long[] rows,int[] start,int[] rowSize,int strIx){
    if(rid<=0||rid>rows[2]) return "TypeDef#"+rid;
    int o=start[2]+(rid-1)*rowSize[2];
    String n=readHeapString(d,mdOff+strings[0],strings[1],readIndex(d,o+4,strIx));
    String ns=readHeapString(d,mdOff+strings[0],strings[1],readIndex(d,o+4+strIx,strIx));
    return ns==null||ns.isBlank()?n:ns+"."+n;
  }

  private String resolveTypeRefName(byte[] d,int rid,int mdOff,int[] strings,long[] rows,int[] start,int[] rowSize,int strIx){
    if(rid<=0||rid>rows[1]) return "TypeRef#"+rid;
    int o=start[1]+(rid-1)*rowSize[1];
    int scope=codedIndexSize(rows,new int[]{0,26,35,1},2);
    String n=readHeapString(d,mdOff+strings[0],strings[1],readIndex(d,o+scope,strIx));
    String ns=readHeapString(d,mdOff+strings[0],strings[1],readIndex(d,o+scope+strIx,strIx));
    return ns==null||ns.isBlank()?n:ns+"."+n;
  }

  private String resolveModuleRefName(byte[] d,int rid,int mdOff,int[] strings,long[] rows,int[] start,int[] rowSize,int strIx){
    if(rid<=0||rid>rows[26]) return "ModuleRef#"+rid;
    int o=start[26]+(rid-1)*rowSize[26];
    return "ModuleRef:"+readHeapString(d,mdOff+strings[0],strings[1],readIndex(d,o,strIx));
  }

  private String resolveFieldOwner(byte[] d,int fieldRid,int mdOff,int[] strings,long[] rows,int[] start,int[] rowSize,int strIx){
    int tdOrRef=codedIndexSize(rows,new int[]{2,1,27},2), fieldIx=simpleIndexSize(rows,4);
    for(int tr=1;tr<=rows[2]&&tr<=100000;tr++){
      int o=start[2]+(tr-1)*rowSize[2], listOff=o+4+strIx+strIx+tdOrRef;
      int first=readIndex(d,listOff,fieldIx), next=(int)rows[4]+1;
      if(tr<rows[2]) next=readIndex(d,start[2]+tr*rowSize[2]+4+strIx+strIx+tdOrRef,fieldIx);
      if(fieldRid>=first&&fieldRid<next) return resolveTypeDefName(d,tr,mdOff,strings,rows,start,rowSize,strIx);
    }
    return null;
  }

  private String resolveMethodOwner(byte[] d,int methodRid,int mdOff,int[] strings,long[] rows,int[] start,int[] rowSize,int strIx){
    int tdOrRef=codedIndexSize(rows,new int[]{2,1,27},2), fieldIx=simpleIndexSize(rows,4), methodIx=simpleIndexSize(rows,6);
    for(int tr=1;tr<=rows[2]&&tr<=100000;tr++){
      int o=start[2]+(tr-1)*rowSize[2], listOff=o+4+strIx+strIx+tdOrRef+fieldIx;
      int first=readIndex(d,listOff,methodIx), next=(int)rows[6]+1;
      if(tr<rows[2]) next=readIndex(d,start[2]+tr*rowSize[2]+4+strIx+strIx+tdOrRef+fieldIx,methodIx);
      if(methodRid>=first&&methodRid<next) return resolveTypeDefName(d,tr,mdOff,strings,rows,start,rowSize,strIx);
    }
    return null;
  }

  private int simpleIndexSize(long[] rows,int table){ return table>=0&&table<rows.length&&rows[table]<65536?2:4; }
  private int codedIndexSize(long[] rows,int[] tables,int tagBits){ long max=0; for(int t:tables) if(t>=0&&t<rows.length) max=Math.max(max,rows[t]); long limit=1L<<(16-tagBits); return max<limit?2:4; }
  private int readIndex(byte[] d,int o,int size){ return size==2?le36(d,o):le32(d,o); }
  private long le64(byte[] d,int o){ return Integer.toUnsignedLong(le32(d,o)) | (Integer.toUnsignedLong(le32(d,o+4))<<32); }
  private String readHeapString(byte[] d,int base,int size,int ix){ if(ix<=0||ix>=size||base+ix<0||base+ix>=d.length) return ""; int s=base+ix,e=s,lim=Math.min(d.length,base+size); while(e<lim&&d[e]!=0)e++; return new String(d,s,e-s,StandardCharsets.UTF_8); }
  private String readUserString(byte[] d,int base,int size,int ix){
    try{ int p=base+ix,lim=Math.min(d.length,base+size); if(ix<=0||p<base||p>=lim)return null; int first=d[p++]&255,len; if((first&0x80)==0)len=first; else if((first&0xC0)==0x80){ if(p>=lim)return null; len=((first&0x3f)<<8)|(d[p++]&255); } else { if(p+2>=lim)return null; len=((first&0x1f)<<24)|((d[p++]&255)<<16)|((d[p++]&255)<<8)|(d[p++]&255); } if(len<=1||p+len>lim)return null; int textLen=len-1; return new String(d,p,textLen-(textLen%2),StandardCharsets.UTF_16LE).replace("\0","").trim(); }catch(Exception e){ return null; }
  }

  private record PeSection(long virtualAddress,long virtualSize,long rawOffset,long rawSize,String name){}
  private record PeLayout(int optionalOffset,boolean pe32Plus,List<PeSection> sections,int securityOffset,int securitySize){}

  private PeLayout parsePeLayout(byte[] d){
    try{
      if(d.length<0x40||d[0]!='M'||d[1]!='Z') return null;
      int pe=le32(d,0x3c); if(pe<0||pe+24>d.length||d[pe]!='P'||d[pe+1]!='E') return null;
      int count=le36(d,pe+6), optSize=le36(d,pe+20), opt=pe+24; if(opt+optSize>d.length) return null;
      int magic=le36(d,opt); boolean plus=magic==0x20b; if(!plus&&magic!=0x10b) return null;
      int dd=opt+(plus?112:96); int secOff=0,secSize=0;
      if(dd+(5*8)<=d.length){ secOff=le32(d,dd+4*8); secSize=le32(d,dd+4*8+4); }
      List<PeSection> sections=new ArrayList<>(); int sh=opt+optSize;
      for(int i=0;i<count&&i<96&&sh+40<=d.length;i++,sh+=40){
        int ne=sh; while(ne<sh+8&&d[ne]!=0) ne++; String name=new String(d,sh,ne-sh,StandardCharsets.US_ASCII);
        sections.add(new PeSection(Integer.toUnsignedLong(le32(d,sh+12)),Integer.toUnsignedLong(le32(d,sh+8)),Integer.toUnsignedLong(le32(d,sh+20)),Integer.toUnsignedLong(le32(d,sh+16)),name));
      }
      return new PeLayout(opt,plus,sections,secOff,secSize);
    }catch(Exception e){ return null; }
  }

  private int rvaToOffset(PeLayout pe,long rva){
    for(PeSection s:pe.sections()){
      long span=Math.max(s.virtualSize(),s.rawSize());
      if(rva>=s.virtualAddress()&&rva<s.virtualAddress()+span){ long off=s.rawOffset()+(rva-s.virtualAddress()); return off<=Integer.MAX_VALUE?(int)off:-1; }
    }
    return rva<=Integer.MAX_VALUE?(int)rva:-1;
  }

  private int align4(int n){ return (n+3)&~3; }

  /** 40k37c45: vergleicht zwei nacheinander gelieferte StoreInstaller-Binaerdateien.
   * Separat wird ausgewiesen, ob Unterschiede ausschliesslich im Authenticode-Zertifikatsblob liegen. */
  private synchronized void compareMicrosoftBridgeBinary(byte[] current,String currentSha){
    try{
      byte[] prev=previousMicrosoftBridgeBinary; String prevSha=previousMicrosoftBridgeSha256;
      previousMicrosoftBridgeBinary=Arrays.copyOf(current,current.length); previousMicrosoftBridgeSha256=currentSha;
      if(prev==null){ log.info("Apple-Mobile-Offline 40k37c45: Bridge-Vergleich: erste Stichprobe gespeichert (SHA-256={}).",currentSha); return; }
      int min=Math.min(prev.length,current.length), diff=0, first=-1,last=-1;
      for(int i=0;i<min;i++) if(prev[i]!=current[i]){ diff++; if(first<0) first=i; last=i; }
      diff+=Math.abs(prev.length-current.length);
      PeLayout a=parsePeLayout(prev), b=parsePeLayout(current);
      int certStart=-1,certEnd=-1;
      if(a!=null&&b!=null&&a.securityOffset()==b.securityOffset()&&a.securitySize()==b.securitySize()&&a.securityOffset()>0){ certStart=a.securityOffset(); certEnd=Math.min(min,certStart+a.securitySize()); }
      int outside=0;
      for(int i=0;i<min;i++) if(prev[i]!=current[i] && !(certStart>=0&&i>=certStart&&i<certEnd)) outside++;
      outside+=Math.abs(prev.length-current.length);
      log.info("Apple-Mobile-Offline 40k37c45: Bridge-Vergleich: vorherSHA={}, aktuellSHA={}, vorherBytes={}, aktuellBytes={}, abweichendeBytes={}, ersteAbweichung={}, letzteAbweichung={}, ZertifikatOffset={}, ZertifikatSize={}, AbweichungenAusserhalbZertifikat={}",prevSha,currentSha,prev.length,current.length,diff,first,last,certStart,a==null?0:a.securitySize(),outside);
      if(diff==0) log.info("Apple-Mobile-Offline 40k37c45: Bridge-Vergleich Ergebnis: beide StoreInstaller-Binaerdateien sind byteidentisch.");
      else if(outside==0) log.info("Apple-Mobile-Offline 40k37c45: Bridge-Vergleich Ergebnis: Unterschiede liegen ausschliesslich im Authenticode-Zertifikatsbereich.");
      else log.info("Apple-Mobile-Offline 40k37c45: Bridge-Vergleich Ergebnis: {} Byte-Unterschiede liegen ausserhalb des Authenticode-Zertifikatsbereichs; dynamische Nutzdaten sind damit moeglich.",outside);
    }catch(Exception ex){ log.info("Apple-Mobile-Offline 40k37c45: Bridge-Vergleich fehlgeschlagen: {}",ex.toString()); }
  }

  private int le36(byte[] d,int o){ return (d[o]&255)|((d[o+1]&255)<<8); }
  private int le32(byte[] d,int o){ return (d[o]&255)|((d[o+1]&255)<<8)|((d[o+2]&255)<<16)|((d[o+3]&255)<<24); }

  private List<String> extractBridgeRelevantStrings(byte[] data){
    LinkedHashSet<String> out=new LinkedHashSet<>();
    collectRelevantBridgeStrings(out,extractPrintableAscii(data));
    collectRelevantBridgeStrings(out,extractPrintableUtf16Le(data));
    return new ArrayList<>(out);
  }

  private void collectRelevantBridgeStrings(Set<String> out,List<String> strings){
    for(String raw:strings){
      String s=raw.trim();
      if(s.length()<4 || s.length()>1200) continue;
      String l=s.toLowerCase(Locale.ROOT);
      boolean relevant=l.contains("9np83lwlpz9k")||l.contains("microsoft.com")||l.contains("windowsupdate")||
          l.contains("delivery.mp")||l.contains("storeedge")||l.contains("storecatalog")||l.contains("displaycatalog")||
          l.contains("ms-windows-store")||l.contains("msix")||l.contains("appx")||l.contains("packagefamily")||
          l.contains("productid")||l.contains("installer")||l.contains("manifest")||l.contains("catalog")||
          l.contains("download")||l.contains("endpoint")||l.contains("https://")||l.contains("http://");
      if(relevant) out.add(s);
      if(out.size()>=200) break;
    }
  }

  private List<String> extractPrintableAscii(byte[] d){
    List<String> out=new ArrayList<>(); StringBuilder b=new StringBuilder();
    for(byte x:d){ int c=x&255; if(c>=32&&c<=126){ b.append((char)c); } else { if(b.length()>=4) out.add(b.toString()); b.setLength(0); } }
    if(b.length()>=4) out.add(b.toString()); return out;
  }

  private List<String> extractPrintableUtf16Le(byte[] d){
    List<String> out=new ArrayList<>(); StringBuilder b=new StringBuilder();
    for(int i=0;i+1<d.length;i+=2){ int lo=d[i]&255, hi=d[i+1]&255; if(hi==0 && lo>=32&&lo<=126){ b.append((char)lo); } else { if(b.length()>=4) out.add(b.toString()); b.setLength(0); } }
    if(b.length()>=4) out.add(b.toString()); return out;
  }

  private URI uriFromBridgeHint(String hint){
    if(hint==null) return null;
    java.util.regex.Matcher m=java.util.regex.Pattern.compile("(?i)https?://[^\"'<>\s]+").matcher(hint);
    while(m.find()){
      try{ return URI.create(m.group().replace("&amp;","&")); }catch(Exception ignored){}
    }
    return null;
  }

  private String bridgeMagic(byte[] d){
    if(d.length>=2 && d[0]=='M' && d[1]=='Z') return "PE/EXE";
    if(d.length>=4 && d[0]=='P' && d[1]=='K' && (d[2]==3||d[2]==5||d[2]==7)) return "ZIP/MSIX/AppX";
    if(d.length>=4 && d[0]==0x7f && d[1]=='E' && d[2]=='L' && d[3]=='F') return "ELF";
    return "unbekannt("+String.format("%02x%02x%02x%02x",d[0]&255,d.length>1?d[1]&255:0,d.length>2?d[2]&255:0,d.length>3?d[3]&255:0)+")";
  }

  private URI findMicrosoftCdnUri(URI base,String body){
    if(body==null) return null;
    java.util.regex.Matcher direct=java.util.regex.Pattern.compile("(?i)https?://[^\\\"'<>\\s]+").matcher(htmlDecode(body));
    while(direct.find()){
      try{
        URI u=URI.create(direct.group().replace("&amp;","&"));
        if(isAllowedMicrosoftPackageCdnHost(u)) return u;
      }catch(Exception ignored){}
    }
    return null;
  }

  private String htmlAttribute(String attrs,String name){
    if(attrs==null) return "";
    java.util.regex.Matcher m=java.util.regex.Pattern.compile("(?is)\\b"+java.util.regex.Pattern.quote(name)+"\\s*=\\s*([\\\"'])(.*?)\\1").matcher(attrs);
    if(m.find()) return m.group(2);
    m=java.util.regex.Pattern.compile("(?is)\\b"+java.util.regex.Pattern.quote(name)+"\\s*=\\s*([^\\s>]+)").matcher(attrs);
    return m.find()?m.group(1):"";
  }

  private List<RgLink> parseRgAdguardLinks(String html){
    if(html==null||html.isBlank()) return List.of();
    List<RgLink> out=new ArrayList<>();
    java.util.regex.Matcher m=java.util.regex.Pattern.compile("(?is)<a\\s+[^>]*href=[\\\"']([^\\\"']+)[\\\"'][^>]*>(.*?)</a>").matcher(html);
    while(m.find()){
      String href=htmlDecode(m.group(1).trim());
      String text=htmlDecode(m.group(2).replaceAll("<[^>]+>","").trim());
      try{
        URI u=URI.create(href);
        String name=!text.isBlank()?text:fileNameFromUri(u);
        String low=name.toLowerCase(Locale.ROOT);
        if(low.endsWith(".msix")||low.endsWith(".msixbundle")||low.endsWith(".appx")||low.endsWith(".appxbundle")) out.add(new RgLink(u,name));
      }catch(Exception ignored){}
    }
    return out;
  }
  private boolean isUsefulAppleDepotLink(RgLink l){
    String n=l.fileName().toLowerCase(Locale.ROOT);
    boolean pkg=n.endsWith(".msix")||n.endsWith(".msixbundle")||n.endsWith(".appx")||n.endsWith(".appxbundle");
    if(!pkg||!isAllowedMicrosoftPackageCdnHost(l.uri())) return false;
    if(n.contains("appleinc.appledevices")||n.contains("appledevices")||n.contains(APPLE_DEVICES_STORE_ID.toLowerCase(Locale.ROOT))) return true;
    return n.contains("microsoft.vclibs")||n.contains("microsoft.ui.xaml")||n.contains("microsoft.windowsappruntime")||n.contains("microsoft.net.native.framework")||n.contains("microsoft.net.native.runtime");
  }
  /** 40k37c45: Microsoft PRS liefert Hauptpaket und Abhängigkeiten direkt von
   * sfdataservice.microsoft.com. Die Freigabe bleibt bewusst pfadgebunden:
   * - Hauptpaket nur unter products/<Apple-Devices-StoreId>/
   * - PRS-Abhängigkeiten nur unter productpackages/dependencies/
   * Die Methode wird ausschließlich im bereits produktgebundenen PRS-Ablauf verwendet;
   * beliebige andere sfdataservice-Pfade und HTTP bleiben unzulässig. */
  private boolean isAllowedMicrosoftPrsPackageSource(URI uri){
    if(uri==null||uri.getHost()==null||!"https".equalsIgnoreCase(uri.getScheme())) return false;
    String h=uri.getHost().toLowerCase(Locale.ROOT);
    if(!h.equals("sfdataservice.microsoft.com")) return false;
    String path=uri.getPath()==null?"":uri.getPath();
    String productPrefix="/package-retrieval-files/productpackages/products/"+APPLE_DEVICES_STORE_ID+"/";
    String dependencyPrefix="/package-retrieval-files/productpackages/dependencies/";
    return path.regionMatches(true,0,productPrefix,0,productPrefix.length())
        || path.regionMatches(true,0,dependencyPrefix,0,dependencyPrefix.length());
  }

  private static String safeHost(URI uri){ return uri==null||uri.getHost()==null?"":uri.getHost(); }
  private static String safePath(URI uri){ return uri==null||uri.getPath()==null?"":uri.getPath(); }
  private static String traceValue(String value){
    if(value==null) return "";
    String v=value.replace('\r',' ').replace('\n',' ').trim();
    return v.length()>180?v.substring(0,180)+"...":v;
  }

  private static String maskUdid(String udid){
    if(udid==null || udid.isBlank()) return "";
    String v=udid.trim();
    if(v.length()<=8) return "***";
    return v.substring(0,4)+"..."+v.substring(v.length()-4);
  }

  /** 40k37c45: Paketbytes dürfen nicht mehr von beliebigen Microsoft-Webseiten kommen.
   * Erlaubt sind nur bekannte Delivery-/Update-CDN-Familien. www.microsoft.com & Co. sind
   * höchstens Zwischenziele und niemals finale MSIX/AppX-Quelle. */
  private boolean isAllowedMicrosoftPackageCdnHost(URI uri){
    String h=uri==null||uri.getHost()==null?"":uri.getHost().toLowerCase(Locale.ROOT);
    return h.equals("dl.delivery.mp.microsoft.com")||h.endsWith(".dl.delivery.mp.microsoft.com")||
        h.equals("delivery.mp.microsoft.com")||h.endsWith(".delivery.mp.microsoft.com")||
        h.equals("windowsupdate.com")||h.endsWith(".windowsupdate.com")||
        h.endsWith(".microsoftusercontent.com");
  }

  private boolean isMicrosoftWebHost(URI uri){
    String h=uri==null||uri.getHost()==null?"":uri.getHost().toLowerCase(Locale.ROOT);
    return h.equals("microsoft.com")||h.endsWith(".microsoft.com")||
        h.equals("aka.ms")||h.endsWith(".aka.ms")||
        h.equals("windows.com")||h.endsWith(".windows.com");
  }
  private record AuthenticodeInfo(String status,String subject,String issuer,String thumbprint,String trustClass) {}
  private void validateDepotPackageSignatures(List<Path> files) throws Exception { for(Path p:files) verifyAuthenticodePackage(p); }
  /** 40k37c45: Für Store-/PRS-MSIX ist der menschenlesbare Zertifikats-CN keine belastbare
   * Herstellerkennung. Windows muss die Authenticode-Signatur als Valid bestätigen; Apple/Microsoft
   * werden weiterhin diagnostisch klassifiziert, GUID-/opaque Subjects bleiben als VALID_OTHER
   * zulässig. Die eigentliche Produktbindung erfolgt separat über Microsoft-PRS-Quelle, PRS-Digest
   * und die Apple-Devices-Paketidentität. */
  private AuthenticodeInfo verifyAuthenticodePackage(Path p) throws Exception {
    if(!System.getProperty("os.name","").toLowerCase(Locale.ROOT).contains("win"))
      return new AuthenticodeInfo("SKIPPED_NON_WINDOWS","","","","SKIPPED");
    String ps="$s=Get-AuthenticodeSignature -LiteralPath '"+psQuote(p.toString())+"'; " +
        "$status=[string]$s.Status; " +
        "$subject=if($s.SignerCertificate){[string]$s.SignerCertificate.Subject}else{''}; " +
        "$issuer=if($s.SignerCertificate){[string]$s.SignerCertificate.Issuer}else{''}; " +
        "$thumb=if($s.SignerCertificate){[string]$s.SignerCertificate.Thumbprint}else{''}; " +
        "Write-Output ($status+'|||'+$subject+'|||'+$issuer+'|||'+$thumb); " +
        "if($s.Status -ne 'Valid'){exit 7}";
    CommandResult r=run(20,"powershell.exe","-NoProfile","-NonInteractive","-Command",ps);
    String line=firstNonBlank(r.stdout(),r.stderr(),"").replace("\r","").split("\n",2)[0];
    String[] parts=line.split("\\|\\|\\|",-1);
    String status=parts.length>0?parts[0].trim():"";
    String subject=parts.length>1?parts[1].trim():"";
    String issuer=parts.length>2?parts[2].trim():"";
    String thumbprint=parts.length>3?parts[3].trim():"";
    if(!r.ok() || !"Valid".equalsIgnoreCase(status))
      throw new SecurityException("Authenticode-Prüfung fehlgeschlagen für "+p.getFileName()+": status="+firstNonBlank(status,"UNKNOWN")+", "+firstNonBlank(r.stderr(),"Signatur ungültig"));
    String lower=(subject+" "+issuer).toLowerCase(Locale.ROOT);
    String trustClass=lower.contains("microsoft")?"MICROSOFT":lower.contains("apple")?"APPLE":"VALID_OTHER";
    return new AuthenticodeInfo(status,subject,issuer,thumbprint,trustClass);
  }
  private static String htmlDecode(String s){ return s.replace("&amp;","&").replace("&quot;","\"").replace("&#39;","'").replace("&lt;","<").replace("&gt;",">"); }
  private static String fileNameFromUri(URI u){ String p=u.getPath(); if(p==null||p.isBlank()) return "package.msix"; int i=p.lastIndexOf('/'); return i>=0?p.substring(i+1):p; }
  private static String safeFileName(String n){ String x=n.replaceAll("[\\\\/:*?\\\"<>|]","_").trim(); return x.isBlank()?"package.msix":x; }

  private String safeDepotPath(){ try{return appleOfflineDepotDirectory().toString();}catch(Exception e){return "";} }
  private List<Path> listAppxPackages(Path root) throws Exception { if(root==null||!Files.isDirectory(root)) return List.of(); try(var w=Files.walk(root,6)){ return w.filter(Files::isRegularFile).filter(p->{String n=p.getFileName().toString().toLowerCase(Locale.ROOT); return n.endsWith(".msix")||n.endsWith(".msixbundle")||n.endsWith(".appx")||n.endsWith(".appxbundle");}).toList(); } }
  private boolean looksLikePrimaryApplePackage(Path p){
    String n=p.getFileName().toString().toLowerCase(Locale.ROOT);
    boolean dependency=n.contains("vclibs")||n.contains("ui.xaml")||n.contains("native")||n.contains("framework")||n.contains("runtime");
    if(dependency) return false;
    // 40k37c45: niemals nur auf "apple" matchen – das würde Apple TV/Music ebenfalls akzeptieren.
    if(n.contains(APPLE_DEVICES_STORE_ID.toLowerCase(Locale.ROOT))||n.contains("appledevices")||n.contains("apple_devices")||n.contains("apple-devices")) return true;
    return packageManifestLooksLikeAppleDevices(p);
  }
  private boolean packageManifestLooksLikeAppleDevices(Path p){
    String n=p.getFileName().toString().toLowerCase(Locale.ROOT);
    if(!(n.endsWith(".msix")||n.endsWith(".appx")||n.endsWith(".msixbundle")||n.endsWith(".appxbundle"))) return false;
    try(java.util.zip.ZipFile z=new java.util.zip.ZipFile(p.toFile())){
      java.util.zip.ZipEntry e=z.getEntry("AppxManifest.xml");
      if(e==null) e=z.getEntry("AppxMetadata/AppxBundleManifest.xml");
      if(e==null)return false;
      String x=new String(z.getInputStream(e).readAllBytes(),StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
      return x.contains("apple") && (x.contains("appledevices")||x.contains("apple devices")||x.contains("apple-devices"));
    }catch(Exception ignored){return false;}
  }
  private long sizeQuiet(Path p){try{return Files.size(p);}catch(Exception e){return 0L;}}
  private static String psQuote(String s){return s.replace("'","''");}
  private static void copyDirectory(Path source,Path target) throws Exception { if(Files.exists(target)) deleteRecursively(target); Files.createDirectories(target); try(var w=Files.walk(source)){ for(Path p:w.toList()){ Path r=source.relativize(p), out=target.resolve(r); if(Files.isDirectory(p))Files.createDirectories(out); else Files.copy(p,out,StandardCopyOption.REPLACE_EXISTING); } } }
  private void writeDepotManifest(Path depot,List<Path> files) throws Exception {
    StringBuilder b=new StringBuilder();
    b.append("GAM Apple Offline Depot 40k37c44\n");
    b.append("created=").append(OffsetDateTime.now()).append("\n");
    b.append("storeId=").append(APPLE_DEVICES_STORE_ID).append("\n");
    b.append("toolchain=").append(TOOLCHAIN_TAG).append("\n");
    for(Path p:files) b.append("sha256=").append(sha256(p)).append("  ").append(depot.relativize(p)).append("\n");
    Files.writeString(depot.resolve("MANIFEST-40k37c44.txt"),b.toString(),StandardCharsets.UTF_8,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING);
  }


  private void writeDepotImportHint(Path depot, boolean manualRequired, String detail) throws Exception {
    StringBuilder b=new StringBuilder();
    b.append("GAM Apple Offline Depot 40k37c44\n\n");
    b.append("Apple Devices Microsoft Store ID: ").append(APPLE_DEVICES_STORE_ID).append("\n");
    b.append("Pakete ablegen unter: ").append(depot.resolve("packages")).append("\n");
    b.append("Benötigt: original signiertes Apple-Devices MSIX/AppX/MSIXBundle/AppXBundle plus alle erforderlichen Abhängigkeiten.\n");
    b.append("Danach in GAM erneut 'Offline-Paket bereitstellen' ausführen; GAM erkennt, prüft, hasht und finalisiert das Depot.\n");
    b.append("Die GAM-libimobiledevice-Toolchain liegt separat unter: ").append(depot.resolve("libimobiledevice")).append("\n");
    if(manualRequired) b.append("\nStatus: manueller/zentraler Paketimport erforderlich.\n");
    if(detail!=null&&!detail.isBlank()) b.append("\nLetzter Store-/WinGet-Hinweis:\n").append(detail).append("\n");
    Files.writeString(depot.resolve("README-OFFLINE-IMPORT-40k37c44.txt"),b.toString(),StandardCharsets.UTF_8,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING);
  }

  private void verifyDepotManifestIfPresent(Path depot) throws Exception {
    Path manifest=depot.resolve("MANIFEST-40k37c44.txt");
    if(!Files.isRegularFile(manifest)) {
      Path legacy14=depot.resolve("MANIFEST-40k37c14.txt");
      Path legacy13=depot.resolve("MANIFEST-40k37c13.txt");
      if(Files.isRegularFile(legacy14)) manifest=legacy14;
      else if(Files.isRegularFile(legacy13)) manifest=legacy13;
      else return;
    }
    for(String line:Files.readAllLines(manifest,StandardCharsets.UTF_8)) {
      if(!line.startsWith("sha256=")) continue;
      String x=line.substring(7); int sep=x.indexOf("  ");
      if(sep<1) continue;
      String expected=x.substring(0,sep).trim().toLowerCase(Locale.ROOT);
      Path file=depot.resolve(x.substring(sep+2).trim()).normalize();
      if(!file.startsWith(depot.normalize())||!Files.isRegularFile(file)) throw new SecurityException("Depot-Manifest verweist auf eine fehlende/ungültige Datei: "+file);
      String actual=sha256(file);
      if(!expected.equals(actual)) throw new SecurityException("SHA-256-Prüfung des Offline-Pakets fehlgeschlagen: "+file.getFileName());
    }
  }

  private boolean openAppleDevicesStore(){
    String uri="ms-windows-store://pdp/?ProductId="+APPLE_DEVICES_STORE_ID;
    CommandResult r=run(8,"cmd.exe","/c","start","",uri);
    return r.ok();
  }

  /** commandExists() ist absichtlich auf Apple-Tools spezialisiert; für WinGet
   * brauchen wir eine reine System-PATH-Prüfung ohne GAM-Toolchain-Auflösung. */
  private boolean commandExistsExternal(String name){
    if(!System.getProperty("os.name","").toLowerCase(Locale.ROOT).contains("win")) return false;
    CommandResult r=run(6,"cmd.exe","/c","where",name);
    return r.ok()&&!r.stdout().trim().isBlank();
  }

  public void scan(boolean iosEnabled, boolean ipadosEnabled, boolean usbEnabled, boolean wlanEnabled, String now,
                   Consumer<DiscoveredDevice> consumer,
                   Consumer<DeviceDiscoveryService.DiscoveryDiagnosticEvent> diagnosticConsumer,
                   String sessionId, int currentCount) {
    if (!iosEnabled && !ipadosEnabled) {
      diagnostic(diagnosticConsumer, sessionId, "APPLE_MOBILE", "SKIPPED", "iOS- und iPadOS-Quelle sind deaktiviert", currentCount);
      return;
    }
    diagnostic(diagnosticConsumer, sessionId, "APPLE_MOBILE", "RUNNING",
      "Apple-Mobile USB/WLAN startet: IOS_USB_WLAN="+iosEnabled+", IPADOS_USB_WLAN="+ipadosEnabled, currentCount);
    appleTrace("E6 APPLE_MOBILE PHASE START", "iosEnabled="+iosEnabled+", ipadosEnabled="+ipadosEnabled+", currentCount="+currentCount);
    log.info("[DISCOVERY][40k37e52][APPLE_MOBILE] scan() START: IOS_USB_WLAN={}, IPADOS_USB_WLAN={}, USB={}, WLAN={}, currentCount={}", iosEnabled, ipadosEnabled, usbEnabled, wlanEnabled, currentCount);
    log.warn("[DISCOVERY][40k37e56c-fix11][APPLE_MOBILE][SOURCE_REGISTRATION] event=SCAN_ENTRY sessionId={} iosEnabled={} ipadosEnabled={} usbEnabled={} wlanEnabled={} os={}", sessionId, iosEnabled, ipadosEnabled, usbEnabled, wlanEnabled, System.getProperty("os.name",""));
    logFix11AppleEntry("DISCOVERY_SCAN_ENTRY", usbEnabled, wlanEnabled);
    Status s;
    try {
      log.info("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE] status() START");
      s = status(usbEnabled,wlanEnabled);
      log.info("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE] status() RESULT: nativeDevices={}, connected={}, paired={}, mechanism={}",
        s.devices().size(), s.connectedDeviceCount(), s.pairedDeviceCount(), s.mechanism());
    } catch (Exception ex) {
      log.error("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE] status() FEHLER: {}", ex.toString(), ex);
      diagnostic(diagnosticConsumer, sessionId, "APPLE_MOBILE", "PARTIAL", "Apple-Mobile-Status konnte nicht vollständig gelesen werden: "+ex.getMessage(), currentCount);
      return;
    }
    appleTrace("E6 APPLE_MOBILE PHASE RESULT", "nativeDevices="+s.devices().size()+", connected="+s.connectedDeviceCount()+", paired="+s.pairedDeviceCount()+", mechanism="+traceValue(s.mechanism()));
    int emitted = 0;
    for (AppleDevice a : s.devices()) {
      if ("iPadOS".equals(a.platform()) && !ipadosEnabled) continue;
      if ("iOS".equals(a.platform()) && !iosEnabled) continue;
      boolean networkTransport="NETWORK".equalsIgnoreCase(a.transport());
      String protocol = (networkTransport
        ? "Apple Mobile / CoreDevice / USERSPACE_RSD / WLAN (40k37e34)"
        : "Apple Mobile / libimobiledevice / usbmuxd / lockdown")
        + detail("Plattform", a.platform()) + detail("Produkt", a.productType()) + detail("Modell", a.modelDisplayName())
        + detail("OS-Version", a.productVersion()) + detail("Build", a.buildVersion())
        + detail("Modellnummer", a.modelNumber()) + detail("Hardwaremodell", a.hardwareModel())
        + detail("CPU-Architektur", a.cpuArchitecture()) + detail("WiFi-MAC", a.wifiAddress())
        + detail("Bluetooth-MAC", a.bluetoothAddress()) + detail("Zeitzone", a.timeZone())
        + detail("Trust/Pairing", a.pairingState());
      String serial = firstNonBlank(a.serialNumber(), a.udid());
      String mac = blankToNull(a.wifiAddress());
      String address = networkTransport ? blankToNull(COREDEVICE_CONFIRMED_WIFI_IP_E33.get()) : null;
      // USB-/PnP-Kennungen sind keine Netzwerkadressen und duerfen deshalb nicht in
      // gam_discovery_device_network_identities bzw. "IP-Adresse(n)" landen.
      boolean registered = registrations.resolveIdentityKey(address, mac, serial, a.name()) != null;
      String discoveryId = networkTransport ? "apple-mobile-wlan:"+a.udid() : "apple-mobile-usb:"+a.udid();
      log.info("[DISCOVERY][40k37e34][APPLE_MOBILE][EMIT_SOURCE] source={}, id={}, udid={}, address={}, serial={}",
        networkTransport?"WLAN":"USB", discoveryId, maskUdid(a.udid()), address, serial);
      consumer.accept(new DiscoveredDevice(discoveryId, firstNonBlank(a.modelDisplayName(), a.name(), a.productType(), "Apple-Mobilgeraet"),
        "iPadOS".equals(a.platform()) ? "Tablet / iPadOS" : "Smartphone / iOS", address, mac,
        protocol, a.paired()?"ERKANNT":"VERTRAUEN_ERFORDERLICH", "Apple", serial, now, registered));
      emitted++;
    }

    // 40k37a1: USB-Sichtbarkeit ist bewusst von Lockdown/Pairing entkoppelt.
    // Wenn libimobiledevice noch keine native Identität liefert, erzeugen wir aus
    // Windows-PnP trotzdem ein echtes Discovery-Gerät. Es bleibt sichtbar und
    // trägt transparent den eingeschränkten Status statt vollständig zu verschwinden.
    if (s.devices().isEmpty()) {
      for (WindowsUsbDevice usb : readWindowsUsbDevices()) {
        if ("iPadOS".equals(usb.platform()) && !ipadosEnabled) continue;
        if ("iOS".equals(usb.platform()) && !iosEnabled) continue;
        // Bei uneindeutiger Apple-Mobile-PnP-Bezeichnung nur dann übernehmen,
        // wenn mindestens eine der beiden Apple-Mobile-Quellen aktiv ist.
        if ("iOS/iPadOS".equals(usb.platform()) && !iosEnabled && !ipadosEnabled) continue;
        String physicalId = firstNonBlank(usb.containerId(), physicalAppleUsbKey(usb.instanceId()), usb.instanceId());
        String safeId = physicalId.replaceAll("[^A-Za-z0-9._-]", "_");
        String address = "usb-pnp:" + safeId;
        String type = "iPadOS".equals(usb.platform()) ? "Tablet / iPadOS"
          : "iOS".equals(usb.platform()) ? "Smartphone / iOS" : "Apple-Mobilgeraet / iOS-iPadOS";
        String displayName = firstNonBlank(usb.modelDisplayName(), usb.name(), "iPadOS".equals(usb.platform()) ? "iPad" : "iOS".equals(usb.platform()) ? "iPhone" : "Apple-Mobilgeraet");
        String protocol = "Apple Mobile / USB / Windows PnP (40k37a9)"
          + detail("Plattform", usb.platform()) + detail("PnP-Status", usb.status())
          + detail("ContainerId", usb.containerId()) + detail("ParentId", usb.parentId())
          + detail("ProductType", usb.productType()) + detail("Modell", usb.modelDisplayName())
          + detail("InstanceId", usb.instanceId()) + detail("Windows-Modellhinweis", usb.evidence())
          + " · Trust/Pairing: noch nicht ueber Lockdown bestaetigt";
        boolean registered = registrations.resolveIdentityKey(address, null, null, displayName) != null;
        consumer.accept(new DiscoveredDevice("apple-usb:"+safeId, displayName, type, address, null, protocol,
          "USB_ERKANNT_TRUST_OFFEN", firstNonBlank(usb.manufacturer(), "Apple"), null, now, registered));
        emitted++;
      }
    }

    // 40k37e55e: Linux-lsusb ist ausschliesslich Unterbau-/Treiberdiagnose.
    // Es erzeugt bewusst KEIN Apple-Mobile-Geraet. Ein Apple-Treffer entsteht erst,
    // wenn die unveraenderte native e54-Logik eine echte UDID ueber usbmuxd/
    // libimobiledevice liefert. So bleibt e54 oberhalb der Linux-Adapterschicht unberuehrt.

    String diagStatus = emitted > 0 ? (s.pairedDeviceCount() > 0 ? "COMPLETED" : "PARTIAL") : (s.nativeToolsAvailable() ? "COMPLETED" : "SKIPPED");
    diagnostic(diagnosticConsumer, sessionId, "APPLE_MOBILE", diagStatus,
      emitted+" Apple-Mobilgeraet(e) fuer aktive Quelle(n) erkannt; Pairing bestaetigt bei "+s.pairedDeviceCount()+" Geraet(en). "+s.message(), currentCount+emitted);
  }

  /** 40k37e55c: Linux-USB-Hardwarebeweis bleibt strikt in der USB-Quelle.
   * Er dient nur als Bootstrap-Hinweis, bis usbmuxd die stabile UDID liefert; keine
   * Netzwerk-/FRITZ-/ARP-Daten werden übernommen. Bei deaktiviertem USB-Schalter wird
   * diese Methode vom Discovery-Pfad nicht aufgerufen. */
  private List<LinuxAppleUsbDevice> readLinuxAppleUsbDevices() {
    if(!System.getProperty("os.name","").toLowerCase(Locale.ROOT).contains("linux")) return List.of();
    CommandResult r=run(8,"sh","-lc","command -v lsusb >/dev/null 2>&1 && lsusb -d 05ac: 2>/dev/null || true");
    if(r.stdout().isBlank()) return List.of();
    List<LinuxAppleUsbDevice> out=new ArrayList<>();
    java.util.regex.Pattern pat=java.util.regex.Pattern.compile("^Bus\\s+(\\d+)\\s+Device\\s+(\\d+):\\s+ID\\s+05ac:([0-9a-fA-F]{4})\\s*(.*)$");
    for(String line:r.stdout().split("\\R")){
      java.util.regex.Matcher m=pat.matcher(line.trim());
      if(m.matches()) out.add(new LinuxAppleUsbDevice(m.group(1),m.group(2),m.group(3).toLowerCase(Locale.ROOT),m.group(4).trim()));
    }
    log.info("[DISCOVERY][40k37e55c][APPLE_MOBILE][USB][LINUX_PHYSICAL] devices={}, descriptions={}",out.size(),out.stream().map(LinuxAppleUsbDevice::description).toList());
    return out;
  }

  private List<AppleDevice> readNativeDevices(boolean pairAvailable) {
    CommandResult ids = runTool(12, "idevice_id", "-l");
    if (!ids.ok()) return List.of();
    return readNativeDevices(pairAvailable, parseUdidLines(ids.stdout()));
  }

  /** 40k37e36: Vollabfrage fuer bereits durch den USB-Praesenzcheck bestaetigte UDIDs.
   * Dadurch wird idevice_id -l im normalen Discovery-Lauf nicht doppelt ausgefuehrt. */
  private List<AppleDevice> readNativeDevices(boolean pairAvailable, List<String> confirmedUsbUdids) {
    if (confirmedUsbUdids == null || confirmedUsbUdids.isEmpty()) return List.of();
    List<AppleDevice> out = new ArrayList<>();
    for (String udid : confirmedUsbUdids) {
      CommandResult info = runTool(15, "ideviceinfo", "-u", udid);
      Map<String,String> m = parseInfo(info.stdout());
      boolean paired = false;
      String pairingState = "Nicht geprueft";
      if (pairAvailable) {
        CommandResult pair = runTool(12, "idevicepair", "-u", udid, "validate");
        String combined = (pair.stdout()+" "+pair.stderr()).trim();
        paired = pair.ok() && combined.toLowerCase(Locale.ROOT).contains("success");
        pairingState = paired ? "Vertrauen/Pairing bestaetigt" : "Vertrauen erforderlich oder Pairing ungueltig";
      } else {
        // ideviceinfo kann ohne gueltige Lockdown-Pairing-Beziehung keine normalen
        // DeviceValues lesen; brauchbare Werte gelten deshalb als starker Trust-Hinweis.
        paired = info.ok() && !m.isEmpty();
        pairingState = paired ? "Lockdown-Zugriff erfolgreich (Pairing indirekt bestaetigt)" : "Pairing nicht pruefbar";
      }
      String productType = m.getOrDefault("ProductType", "");
      String platform = productType.toLowerCase(Locale.ROOT).startsWith("ipad") ? "iPadOS" : "iOS";
      String modelDisplayName = appleModelDisplayName(productType);
      out.add(new AppleDevice(udid, m.getOrDefault("DeviceName", ""), platform, productType, modelDisplayName,
        m.getOrDefault("ProductVersion", ""), m.getOrDefault("BuildVersion", ""),
        m.getOrDefault("SerialNumber", ""), m.getOrDefault("ModelNumber", ""),
        m.getOrDefault("HardwareModel", ""), m.getOrDefault("CPUArchitecture", ""),
        m.getOrDefault("WiFiAddress", ""), m.getOrDefault("BluetoothAddress", ""),
        m.getOrDefault("TimeZone", ""), paired, pairingState, "USB"));
    }
    return out;
  }

  /**
   * 40k37e25: JVM-sichere Apple-Mobile-Netzwerkerkennung.
   * Der e4-In-Process-JNA-Schreibzugriff auf wireless_lockdown ist entfernt.
   */
  /** 40k37e33: Remote-Browse liefert die echte WLAN-IP zusammen mit der stabilen UDID.
   * Nur ein exakt zur angefragten UDID passender IPv4-Endpunkt wird uebernommen. */
  /** 40k37e53-fix1: Uebernimmt eine physische private IPv4 nur aus Apple-Ausgaben, die
   * gleichzeitig die exakte UDID des Zielgeraets enthalten. Damit kann z.B. der
   * `_remotepairing._tcp`-Bonjour-Endpunkt 192.168.x.x sicher an den Apple-WLAN-Treffer
   * gebunden werden, ohne fremde LAN-Adressen oder RSD-Tunneladressen zu uebernehmen. */
  private void cacheUdidBoundPrivateLanIpE53Fix1(String text, String expectedUdid, String origin) {
    if(text==null || text.isBlank() || expectedUdid==null || expectedUdid.isBlank() || !text.contains(expectedUdid)) return;
    java.util.regex.Matcher m=java.util.regex.Pattern.compile("(?<![0-9.])((?:\\d{1,3}\\.){3}\\d{1,3})(?![0-9.])").matcher(text);
    while(m.find()) {
      String candidate=m.group(1);
      if(isPrivateLanIpv4E33Fix5(candidate)) {
        COREDEVICE_CONFIRMED_WIFI_IP_E33.set(candidate);
        log.info("[DISCOVERY][40k37e53-fix1][APPLE_MOBILE][WLAN][MERGE_IP_CONFIRMED] udid={}, ip={}, origin={}",maskUdid(expectedUdid),candidate,origin);
        return;
      }
    }
  }

  /** 40k37e56c-fix36: Erkennt das aktuelle pymobiledevice3-`remote browse`-Format.
   * Ein Peer gilt nur als Kandidat, wenn Apple ihn zugleich als authentifiziert und
   * verfuegbar meldet. Die eigentliche WLAN-Bestaetigung bleibt der bestehende
   * USERSPACE_RSD-Probe; diese Methode erzeugt selbst keinen Discovery-Treffer. */
  private boolean remoteBrowseHasAuthenticatedAvailablePeerFix36(String browseJson) {
    if(browseJson==null || browseJson.isBlank()) return false;
    try {
      JsonNode root=new ObjectMapper().readTree(browseJson);
      if(!root.isArray()) return false;
      for(JsonNode peer:root) {
        String auth=jsonText(peer.path("authState"),"rawCase").trim();
        String connection=jsonText(peer.path("connectionState"),"rawCase").trim();
        if("authenticated".equalsIgnoreCase(auth) && "available".equalsIgnoreCase(connection)) return true;
      }
    } catch(Exception ex) {
      log.debug("[DISCOVERY][40k37e56c-fix36][APPLE_MOBILE][WLAN][REMOTE_BROWSE_PARSE] {}",ex.getMessage());
    }
    return false;
  }

  private void cacheCoreDeviceWifiIpE33(String browseJson, String expectedUdid) {
    if(browseJson==null || browseJson.isBlank() || expectedUdid==null || expectedUdid.isBlank()) return;
    try {
      JsonNode root=new ObjectMapper().readTree(browseJson);
      JsonNode wifi=root.path("wifi");
      if(!wifi.isArray()) return;
      for(JsonNode item:wifi) {
        String identifier=jsonText(item,"identifier").trim();
        String address=jsonText(item,"address").trim();
        if(identifier.equalsIgnoreCase(expectedUdid) && address.matches("(?:\\d{1,3}\\.){3}\\d{1,3}")) {
          COREDEVICE_CONFIRMED_WIFI_IP_E33.set(address);
          log.info("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN][IP_CONFIRMED] udid={}, ip={}",maskUdid(expectedUdid),address);
          return;
        }
      }
    } catch(Exception ex) {
      log.debug("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN][IP_PARSE] keine verwertbare Remote-Browse-IP: {}",ex.getMessage());
    }
  }

  /** 40k37e35: CoreDevice-WLAN-Erkennung darf nicht von idevice_id -n oder einem aktuell
   * angeschlossenen USB-Geraet abhaengen. Wenn die GAM-eigene pymobiledevice3-Runtime bereits
   * vorhanden ist, wird remote browse read-only direkt abgefragt. Ein dort sichtbarer WLAN-
   * Identifier wird anschliessend ueber denselben USERSPACE_RSD-Pfad semantisch bestaetigt. */
  /**
   * 40k37e37: WLAN-Einstieg ueber den bereits bewaehrten CoreDevice-Tunnel.
   * Moderne iOS/iPadOS-Geraete muessen nicht in `remote browse` erscheinen. Wenn der
   * klassische Browse leer bleibt, darf GAM deshalb direkt `remote start-tunnel -t wifi`
   * versuchen. Ohne --udid uebernimmt pymobiledevice3 die eigentliche WLAN-/RemotePairing-
   * Auswahl. Erst ein lebender Tunnel plus erfolgreiches rsd-info gilt als Treffer.
   */
  private void probeCoreDeviceWifiTunnelWithoutBrowseE37(Map<String,String> env, Path pmdExe) {
    // 40k37e49: Der heutige A/B-Test hat gezeigt, dass pymobiledevice3 pro
    // `remote start-tunnel` nur wenige Discovery-Runden macht. Ein schlafendes iPad
    // kann deshalb erst nach mehreren neuen Prozessen sichtbar werden. GAM wiederholt
    // den echten WLAN-Tunnelaufbau bis zu 20x. Sobald HOST/PORT vorliegen, bleibt
    // exakt dieser Tunnel fuer die komplette RSD-Inventarisierung bestehen.
    String expectedUdid=COREDEVICE_SELECTED_UDID_E25.get();
    if(expectedUdid==null || expectedUdid.isBlank()) expectedUdid=recoverPersistedCoreDeviceUdidE40();
    if(expectedUdid==null || expectedUdid.isBlank()) {
      log.info("[DISCOVERY][40k37e49][APPLE_MOBILE][WLAN][PERSISTENT_RSD] skipped=true, reason=NO_PERSISTED_UDID");
      return;
    }
    // 40k37e50: Standard ist der nicht privilegierte Userspace-RSD-Pfad. Nur wenn
    // GAM bereits erhoeht laeuft, wird der persistente Kernel-Tunnel aus e49 benutzt.
    // Damit erzeugt eine normale GAM-Sitzung keine UAC-Pflicht und ein Admin-Start nutzt
    // automatisch den langlebigen RSD-Tunnel.
    boolean explicitAdmin=adminTunnelStatus().active();
    boolean elevated=isWindowsElevatedE50();
    log.info("[DISCOVERY][40k37e54-fix8][APPLE_MOBILE][WLAN][TRANSPORT_SELECT] explicitAdmin={}, elevatedGam={}, udid={}, preferred={}",
      explicitAdmin,elevated,maskUdid(expectedUdid),explicitAdmin?"EXPLICIT_UAC_ADMIN_RSD":elevated?"PERSISTENT_ADMIN_RSD":"USERSPACE_RSD");
    if(explicitAdmin) {
      if(tryExplicitAdminRsdE54Fix8(env,pmdExe,expectedUdid)) return;
      // fix9: Ein bewusst aktivierter Admin-Tunnel ist fuer diesen Lauf verbindlich.
      // Kein stiller Fallback auf Userspace-RSD, weil sonst ein defekter Admin-Pfad als erfolgreicher Test erscheinen koennte.
      log.error("[DISCOVERY][40k37e54-fix9][APPLE_MOBILE][WLAN][ADMIN_RSD_REQUIRED] result=FAILED, udid={}, fallback=BLOCKED",maskUdid(expectedUdid));
      COREDEVICE_SELECTED_UDID_E25.set(""); COREDEVICE_CONFIRMED_WIFI_E30.set(null);
      return;
    }
    if(!elevated) {
      // 40k37e51-fix2: Der real bewiesene e25-fix4/e26-Userspace-RSD-Pfad ist wieder
      // der primaere WLAN-Transport. Bonjour/remote browse/mobdev2 duerfen ihn NICHT
      // blockieren: in realen Tests waren diese Anzeigen leer, waehrend --userspace
      // trotzdem bereits echte Daten vom iPad lieferte. Ein gesperrtes/ruhendes iPad
      // kann erst nach mehreren Anlaeufen antworten, daher 20 neue Userspace-Prozesse.
      if(tryHistoricalUserspaceRsdRetryE51Fix2(env,pmdExe,expectedUdid)) return;
      // Network-Lockdown bleibt als zweiter, expliziter WLAN-Weg erhalten, aber nicht
      // mehr als Gate vor dem historisch nachgewiesenen Erfolgsfall.
      if(tryMobdev2UserspaceRsdE51Fix1(env,pmdExe,expectedUdid)) return;
      log.info("[DISCOVERY][40k37e51-fix2][APPLE_MOBILE][WLAN][USERSPACE_RSD] result=FAILED, udid={}, historicalRetries=20, networkLockdownTried=true, adminTunnelAvailable=true, hint=IPAD_GGF_ENTSPERREN_ODER_ADMIN_TUNNEL_OPTIONAL",maskUdid(expectedUdid));
    } else {
      if(tryPersistentWifiRsdE49(env,pmdExe,expectedUdid)) return;
    }
    COREDEVICE_SELECTED_UDID_E25.set("");
    COREDEVICE_CONFIRMED_WIFI_E30.set(null);
    log.info("[DISCOVERY][40k37e50][APPLE_MOBILE][WLAN][TRANSPORT] result=FAILED, udid={}, elevated={}, hint={}",
      maskUdid(expectedUdid),elevated,elevated?"IPAD_AKTIVIEREN_ODER_ENTSPERREN":"USERSPACE_FEHLGESCHLAGEN_ADMIN_TUNNEL_KANN_PER_UAC_VERWENDET_WERDEN");
  }


  /** 40k37e51-fix2: Verankert den real erfolgreichen e25-fix4/e26-WLAN-Fall.
   * Jeder Versuch ist ein frischer pymobiledevice3-Userspace-RSD-Aufbau; leere Bonjour-
   * oder Browse-Ergebnisse sind absichtlich kein Gate. Das iPad darf gesperrt sein:
   * dann wird laenger weiterprobiert. Bei Erfolg uebernimmt der historische Sammler
   * unmittelbar Basis-/Capability-Inventarisierung und beendet alternative Apple-Pfade. */
  private boolean tryHistoricalUserspaceRsdRetryE51Fix2(Map<String,String> env, Path pmdExe, String udid) {
    for(int attempt=1; attempt<=20; attempt++) {
      try {
        log.info("[DISCOVERY][40k37e51-fix2][APPLE_MOBILE][WLAN][HISTORICAL_USERSPACE_ATTEMPT] attempt={}/20, udid={}, source=E25_FIX4_E26, bonjourGate=false, browseGate=false, usbRequired=false",attempt,maskUdid(udid));
        if(tryCoreDeviceUserspaceRsdE25(env,pmdExe,udid,"E51_FIX2_E25FIX4_RETRY_"+attempt)) {
          log.info("[DISCOVERY][40k37e51-fix2][APPLE_MOBILE][WLAN][HISTORICAL_USERSPACE_CONFIRMED] attempt={}/20, udid={}, transport=USERSPACE_RSD, source=E25_FIX4_E26",attempt,maskUdid(udid));
          return true;
        }
        if(attempt<20) Thread.sleep(1500);
      } catch(InterruptedException ie) {
        Thread.currentThread().interrupt();
        return false;
      } catch(Exception ex) {
        log.info("[DISCOVERY][40k37e51-fix2][APPLE_MOBILE][WLAN][HISTORICAL_USERSPACE_RESULT] attempt={}/20, success=false, error={}",attempt,ex.toString());
        try { if(attempt<20) Thread.sleep(1500); } catch(InterruptedException ie) { Thread.currentThread().interrupt(); return false; }
      }
    }
    return false;
  }

  /** 40k37e50: Erkennt unter Windows, ob der aktuelle GAM-Prozess mit hohem
   * Integritaetslevel laeuft. S-1-16-12288 = High Mandatory Level. Bei Fehlern
   * konservativ false: Userspace ist der sichere Standard. */
  private boolean isWindowsElevatedE50() {
    if(!System.getProperty("os.name","").toLowerCase(Locale.ROOT).contains("win")) return false;
    try {
      CommandResult r=runProcessE20(8,Map.of(),"whoami","/groups");
      String data=(r.stdout()+"\n"+r.stderr()).toLowerCase(Locale.ROOT);
      boolean elevated=r.ok() && (data.contains("s-1-16-12288") || data.contains("s-1-16-16384"));
      log.info("[DISCOVERY][40k37e50][APPLE_MOBILE][PRIVILEGE] elevated={}, probeOk={}",elevated,r.ok());
      return elevated;
    } catch(Exception ex) {
      log.info("[DISCOVERY][40k37e50][APPLE_MOBILE][PRIVILEGE] elevated=false, probeError={}",ex.toString());
      return false;
    }
  }

  /** 40k37e51-fix1: Nicht privilegierter WLAN-Transport ueber Network-Lockdown.
   * `lockdown start-tunnel --mobdev2 --userspace` zwingt pymobiledevice3 dazu, das
   * bereits gekoppelte Apple-Geraet ueber Bonjour/mobdev2 statt ueber usbmux zu finden.
   * Der gestartete Userspace-Tunnel bleibt fuer die komplette RSD-Inventarisierung am
   * Leben; erst danach wird er beendet. Die erwartete UDID wird ueber rsd-info verifiziert. */
  private boolean tryMobdev2UserspaceRsdE51Fix1(Map<String,String> env, Path pmdExe, String udid) {
    for(int attempt=1;attempt<=8;attempt++) {
      Process tunnel=null;
      ExecutorService io=Executors.newFixedThreadPool(2);
      try {
        log.info("[DISCOVERY][40k37e51-fix1][APPLE_MOBILE][WLAN][NETWORK_LOCKDOWN] attempt={}/8, selector=MOBDEV2, userspace=true, usbRequired=false, expectedUdid={}",attempt,maskUdid(udid));
        ProcessBuilder pb=new ProcessBuilder(pmdExe.toString(),"--no-color","lockdown","start-tunnel","--mobdev2","--userspace","--script-mode");
        if(env!=null) pb.environment().putAll(env);
        pb.environment().remove("PYMOBILEDEVICE3_UDID");
        pb.environment().put("PYMOBILEDEVICE3_USERSPACE","1");
        pb.environment().put("PYMOBILEDEVICE3_DEFAULT_FALLBACK","userspace");
        tunnel=pb.start();
        final Process tp=tunnel;
        Future<String> err=io.submit(() -> readAll(tp.getErrorStream()));
        String endpointLine="";
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(15);
        BufferedReader stdout=new BufferedReader(new InputStreamReader(tp.getInputStream(),StandardCharsets.UTF_8));
        while(System.nanoTime()<deadline && tp.isAlive() && endpointLine.isBlank()) {
          if(stdout.ready()) {
            String line=stdout.readLine();
            if(line!=null && parseRsdEndpointE25(line)!=null) endpointLine=line.trim();
          } else Thread.sleep(100);
        }
        String[] endpoint=parseRsdEndpointE25(endpointLine);
        if(endpoint==null || !tp.isAlive()) {
          String stderr=""; try { if(err.isDone()) stderr=err.get(); } catch(Exception ignored) { }
          log.info("[DISCOVERY][40k37e51-fix1][APPLE_MOBILE][WLAN][NETWORK_LOCKDOWN_RESULT] attempt={}/8, success=false, alive={}, data={}",attempt,tp.isAlive(),limitTrace(stderr));
          if(tp.isAlive()) { tp.destroy(); if(!tp.waitFor(2,TimeUnit.SECONDS)) tp.destroyForcibly(); }
          if(attempt<8) Thread.sleep(1500);
          continue;
        }
        log.info("[DISCOVERY][40k37e51-fix1][APPLE_MOBILE][WLAN][NETWORK_LOCKDOWN_TUNNEL] attempt={}/8, host={}, port={}, pid={}, transport=MOBDEV2_USERSPACE_RSD",attempt,endpoint[0],endpoint[1],tp.pid());
        CommandResult rsd=runProcessE20(60,env,pmdExe.toString(),"--no-color","remote","rsd-info","--rsd",endpoint[0],endpoint[1]);
        String rsdData=(rsd.stdout()+"\n"+rsd.stderr()).trim();
        if(!semanticReadOnlyJsonE28(rsd,rsdData)) {
          log.info("[DISCOVERY][40k37e51-fix1][APPLE_MOBILE][WLAN][NETWORK_LOCKDOWN_VERIFY] semanticOk=false, attempt={}, data={}",attempt,limitTrace(rsdData));
          continue;
        }
        JsonNode root=new ObjectMapper().readTree(rsd.stdout());
        JsonNode props=root.path("Properties");
        String actual=jsonText(props,"UniqueDeviceID").trim();
        if(!actual.isBlank() && !actual.equalsIgnoreCase(udid)) {
          log.warn("[DISCOVERY][40k37e51-fix1][APPLE_MOBILE][WLAN][NETWORK_LOCKDOWN_IDENTITY_MISMATCH] expected={}, actual={}",maskUdid(udid),maskUdid(actual));
          continue;
        }
        if(props.isObject()) {
          COREDEVICE_REMOTEPAIRING_BASIS_E27.set(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(props));
          writeCoreDeviceSnapshotE28("apple-mobile-coredevice-e27-basis.json",COREDEVICE_REMOTEPAIRING_BASIS_E27.get(),"COREDEVICE_E51_FIX1_MOBDEV2_BASIS_SNAPSHOT");
        }
        boolean apps=probeRsdCommandE49(env,pmdExe,endpoint,udid,"APPS",90,"apple-mobile-coredevice-apps.json","apps","list");
        boolean battery=probeRsdCommandE49(env,pmdExe,endpoint,udid,"BATTERY",60,"apple-mobile-coredevice-battery.json","diagnostics","battery","single");
        boolean system=probeRsdCommandE49(env,pmdExe,endpoint,udid,"SYSTEM_STORAGE",60,"apple-mobile-coredevice-system.json","diagnostics","info");
        boolean basis=probeCoreDeviceBasisInventoryE27(udid,"E51_FIX1_MOBDEV2_USERSPACE_RSD");
        COREDEVICE_SELECTED_UDID_E25.set(udid);
        log.info("[DISCOVERY][40k37e51-fix1][APPLE_MOBILE][WLAN][NETWORK_LOCKDOWN_CONFIRMED] udid={}, attempt={}, basis={}, apps={}, battery={}, system={}, transport=MOBDEV2_USERSPACE_RSD",maskUdid(udid),attempt,basis,apps,battery,system);
        return true;
      } catch(Exception ex) {
        log.info("[DISCOVERY][40k37e51-fix1][APPLE_MOBILE][WLAN][NETWORK_LOCKDOWN_RESULT] attempt={}/8, success=false, error={}",attempt,ex.toString());
        try { if(attempt<8) Thread.sleep(1500); } catch(InterruptedException ie) { Thread.currentThread().interrupt(); return false; }
      } finally {
        if(tunnel!=null && tunnel.isAlive()) {
          long pid=tunnel.pid(); tunnel.destroy();
          try { if(!tunnel.waitFor(3,TimeUnit.SECONDS)) tunnel.destroyForcibly(); } catch(InterruptedException ie) { Thread.currentThread().interrupt(); tunnel.destroyForcibly(); }
          log.info("[DISCOVERY][40k37e51-fix1][APPLE_MOBILE][WLAN][NETWORK_LOCKDOWN_TUNNEL_STOP] pid={}, inventoryFinished=true",pid);
        }
        io.shutdownNow();
      }
    }
    return false;
  }

  /** 40k37e49: Robuster, rein drahtloser RemotePairing/RSD-Transport.
   * Kein USB-Bootstrap in diesem Pfad. Der bereits einmalig ueber USB erzeugte
   * RemotePairing-PairRecord wird von pymobiledevice3 remote start-tunnel benutzt. */
  /** 40k37e54-fix8: Nutzt einen bewusst per UAC gestarteten Admin-Tunnel fuer den kompletten Inventarlauf. */
  private boolean tryExplicitAdminRsdE54Fix8(Map<String,String> env, Path pmdExe, String udid) {
    AdminTunnelStatus st=adminTunnelStatus();
    if(!st.active() || st.host()==null || st.port()<=0) return false;
    if(ADMIN_RSD_UDID_E54F8.get()!=null && !ADMIN_RSD_UDID_E54F8.get().isBlank() && !ADMIN_RSD_UDID_E54F8.get().equalsIgnoreCase(udid)) return false;
    String[] endpoint={st.host(),String.valueOf(st.port())};
    CommandResult rsd=runProcessE20(60,env,pmdExe.toString(),"--no-color","remote","rsd-info","--rsd",endpoint[0],endpoint[1]);
    String data=(rsd.stdout()+"\n"+rsd.stderr()).trim();
    if(!semanticReadOnlyJsonE28(rsd,data)) { log.warn("[DISCOVERY][40k37e54-fix8][APPLE_MOBILE][ADMIN_RSD] active=true, verify=false"); return false; }
    try { JsonNode props=new ObjectMapper().readTree(rsd.stdout()).path("Properties"); if(props.isObject()) COREDEVICE_REMOTEPAIRING_BASIS_E27.set(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(props)); } catch(Exception ignored) {}
    boolean apps=probeRsdCommandE49(env,pmdExe,endpoint,udid,"ADMIN_APPS",90,"apple-mobile-admin-rsd-apps.json","apps","list");
    boolean battery=probeRsdCommandE49(env,pmdExe,endpoint,udid,"ADMIN_BATTERY",60,"apple-mobile-admin-rsd-battery.json","diagnostics","battery","single");
    boolean system=probeRsdCommandE49(env,pmdExe,endpoint,udid,"ADMIN_SYSTEM_STORAGE",60,"apple-mobile-admin-rsd-system.json","diagnostics","info");
    boolean basis=probeCoreDeviceBasisInventoryE27(udid,"E54_FIX8_ADMIN_RSD");
    COREDEVICE_SELECTED_UDID_E25.set(udid);
    boolean complete=basis && apps && battery && system;
    log.info("[DISCOVERY][40k37e54-fix21][APPLE_MOBILE][ADMIN_RSD_INVENTORY] result={}, udid={}, endpoint={}:{}, basis={}, apps={}, battery={}, system={}, pid={}, fallback={}",
      complete?"CONFIRMED":"INCOMPLETE",maskUdid(udid),st.host(),st.port(),basis,apps,battery,system,st.pid(),complete?"NOT_NEEDED":"BLOCKED");
    return complete;
  }

  private boolean tryPersistentWifiRsdE49(Map<String,String> env, Path pmdExe, String udid) {
    for(int attempt=1; attempt<=20; attempt++) {
      Process tunnel=null;
      ExecutorService io=Executors.newFixedThreadPool(2);
      try {
        log.info("[DISCOVERY][40k37e49][APPLE_MOBILE][WLAN][TUNNEL_ATTEMPT] attempt={}/20, udid={}, connectionType=wifi, usbRequired=false",attempt,maskUdid(udid));
        ProcessBuilder pb=new ProcessBuilder(pmdExe.toString(),"--no-color","remote","start-tunnel","--connection-type","wifi","--udid",udid,"--script-mode");
        if(env!=null) pb.environment().putAll(env);
        pb.environment().remove("PYMOBILEDEVICE3_UDID"); // Auswahl erfolgt explizit per CLI; kein usbmux-Fallback durch ENV.
        tunnel=pb.start();
        final Process tp=tunnel;
        Future<String> err=io.submit(() -> readAll(tp.getErrorStream()));

        String endpointLine="";
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(12);
        BufferedReader stdout=new BufferedReader(new InputStreamReader(tp.getInputStream(),StandardCharsets.UTF_8));
        while(System.nanoTime()<deadline && tp.isAlive() && endpointLine.isBlank()) {
          if(stdout.ready()) {
            String line=stdout.readLine();
            if(line!=null && parseRsdEndpointE25(line)!=null) endpointLine=line.trim();
          } else Thread.sleep(100);
        }
        String[] endpoint=parseRsdEndpointE25(endpointLine);
        if(endpoint==null || !tp.isAlive()) {
          String stderr="";
          try { if(err.isDone()) stderr=err.get(); } catch(Exception ignored) { }
          log.info("[DISCOVERY][40k37e49][APPLE_MOBILE][WLAN][TUNNEL_ATTEMPT_RESULT] attempt={}/20, success=false, alive={}, data={}",attempt,tp.isAlive(),limitTrace(stderr));
          if(tp.isAlive()) { tp.destroy(); if(!tp.waitFor(2,TimeUnit.SECONDS)) tp.destroyForcibly(); }
          if(attempt<20) Thread.sleep(2000);
          continue;
        }

        log.info("[DISCOVERY][40k37e49][APPLE_MOBILE][WLAN][TUNNEL_CREATED] attempt={}/20, udid={}, host={}, port={}, pid={}, persistentForInventory=true",attempt,maskUdid(udid),endpoint[0],endpoint[1],tp.pid());
        CommandResult rsd=runProcessE20(60,env,pmdExe.toString(),"--no-color","remote","rsd-info","--rsd",endpoint[0],endpoint[1]);
        String rsdData=(rsd.stdout()+"\n"+rsd.stderr()).trim();
        if(!semanticReadOnlyJsonE28(rsd,rsdData)) {
          log.info("[DISCOVERY][40k37e49][APPLE_MOBILE][WLAN][RSD_VERIFY] semanticOk=false, attempt={}, data={}",attempt,limitTrace(rsdData));
          if(tp.isAlive()) { tp.destroy(); if(!tp.waitFor(2,TimeUnit.SECONDS)) tp.destroyForcibly(); }
          if(attempt<20) Thread.sleep(2000);
          continue;
        }

        // RSD Properties enthalten die gleiche belastbare Basisidentitaet, die GAM fuer
        // den regulaeren NETWORK-Treffer benoetigt. Sie wird im vorhandenen e27-Cacheformat gehalten.
        try {
          JsonNode root=new ObjectMapper().readTree(rsd.stdout());
          JsonNode props=root.path("Properties");
          if(props.isObject()) {
            String actual=jsonText(props,"UniqueDeviceID").trim();
            if(!actual.isBlank() && !actual.equalsIgnoreCase(udid)) {
              log.warn("[DISCOVERY][40k37e49][APPLE_MOBILE][WLAN][RSD_IDENTITY_MISMATCH] expected={}, actual={}",maskUdid(udid),maskUdid(actual));
              return false;
            }
            COREDEVICE_REMOTEPAIRING_BASIS_E27.set(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(props));
            writeCoreDeviceSnapshotE28("apple-mobile-coredevice-e27-basis.json",COREDEVICE_REMOTEPAIRING_BASIS_E27.get(),"COREDEVICE_E49_RSD_BASIS_SNAPSHOT");
          }
        } catch(Exception ex) {
          log.warn("[DISCOVERY][40k37e49][APPLE_MOBILE][WLAN][RSD_BASIS_PARSE] error={}",ex.toString());
        }

        boolean apps=probeRsdCommandE49(env,pmdExe,endpoint,udid,"APPS",90,"apple-mobile-coredevice-apps.json","apps","list");
        boolean battery=probeRsdCommandE49(env,pmdExe,endpoint,udid,"BATTERY",60,"apple-mobile-coredevice-battery.json","diagnostics","battery","single");
        boolean system=probeRsdCommandE49(env,pmdExe,endpoint,udid,"SYSTEM_STORAGE",60,"apple-mobile-coredevice-system.json","diagnostics","info");
        boolean basis=probeCoreDeviceBasisInventoryE27(udid,"E49_PERSISTENT_RSD");
        COREDEVICE_SELECTED_UDID_E25.set(udid);
        log.info("[DISCOVERY][40k37e49][APPLE_MOBILE][WLAN][PERSISTENT_RSD] result=CONFIRMED, udid={}, attempt={}, basis={}, apps={}, battery={}, system={}, transport=REMOTEPAIRING_WIFI_RSD",maskUdid(udid),attempt,basis,apps,battery,system);
        return true;
      } catch(Exception ex) {
        log.info("[DISCOVERY][40k37e49][APPLE_MOBILE][WLAN][TUNNEL_ATTEMPT_RESULT] attempt={}/20, success=false, error={}",attempt,ex.toString());
        try { if(attempt<20) Thread.sleep(2000); } catch(InterruptedException ie) { Thread.currentThread().interrupt(); return false; }
      } finally {
        if(tunnel!=null && tunnel.isAlive()) {
          long pid=tunnel.pid();
          tunnel.destroy();
          try { if(!tunnel.waitFor(3,TimeUnit.SECONDS)) tunnel.destroyForcibly(); } catch(InterruptedException ie) { Thread.currentThread().interrupt(); tunnel.destroyForcibly(); }
          log.info("[DISCOVERY][40k37e49][APPLE_MOBILE][WLAN][TUNNEL_STOP] pid={}, inventoryFinished=true",pid);
        }
        io.shutdownNow();
      }
    }
    return false;
  }

  private boolean probeRsdCommandE49(Map<String,String> env, Path pmdExe, String[] endpoint, String udid,
      String capability, int timeoutSeconds, String snapshotName, String... command) {
    try {
      java.util.List<String> args=new java.util.ArrayList<>();
      args.add(pmdExe.toString()); args.add("--no-color"); args.addAll(java.util.Arrays.asList(command));
      args.add("--rsd"); args.add(endpoint[0]); args.add(endpoint[1]);
      CommandResult r=runProcessE20(timeoutSeconds,env,args.toArray(String[]::new));
      String data=(r.stdout()+"\n"+r.stderr()).trim();
      boolean ok=semanticReadOnlyJsonE28(r,data);
      log.info("[DISCOVERY][40k37e49][APPLE_MOBILE][WLAN][RSD_INVENTORY] capability={}, semanticOk={}, exit={}, udid={}, host={}, port={}",capability,ok,r.exitCode(),maskUdid(udid),endpoint[0],endpoint[1]);
      if(ok) writeCoreDeviceSnapshotE28(snapshotName,data,"COREDEVICE_E49_"+capability+"_SNAPSHOT");
      return ok;
    } catch(Exception ex) {
      log.warn("[DISCOVERY][40k37e49][APPLE_MOBILE][WLAN][RSD_INVENTORY] capability={}, semanticOk=false, udid={}, error={}",capability,maskUdid(udid),ex.toString());
      return false;
    }
  }

  /** 40k37e40: Liest ausschliesslich GAMs eigenen, bereits bei erfolgreichem USB-
   * RemotePairing erzeugten Basis-Snapshot. Keine Pairing-Secrets werden geloggt oder
   * neu geschrieben. Der Snapshot ueberlebt einen Backend-Neustart und verbindet damit
   * den einmaligen USB-Bootstrap mit spaeteren reinen WLAN-Laeufen. */
  private String recoverPersistedCoreDeviceUdidE40() {
    try {
      Path snapshot=findGamRoot().resolve("logs").resolve("apple-mobile-coredevice-e27-basis.json");
      if(!Files.isRegularFile(snapshot)) {
        log.info("[DISCOVERY][40k37e40][APPLE_MOBILE][WLAN][COREDEVICE_PERSISTED_PAIR_IDENTITY] found=false, reason=E27_BASIS_SNAPSHOT_MISSING");
        return "";
      }
      String raw=Files.readString(snapshot,StandardCharsets.UTF_8);
      String udid=extractCoreDeviceUdidE37(raw);
      log.info("[DISCOVERY][40k37e40][APPLE_MOBILE][WLAN][COREDEVICE_PERSISTED_PAIR_IDENTITY] found={}, udid={}, file={}",
        !udid.isBlank(),maskUdid(udid),snapshot.toAbsolutePath());
      if(!raw.isBlank()) COREDEVICE_REMOTEPAIRING_BASIS_E27.set(raw);
      return udid;
    } catch(Exception ex) {
      log.warn("[DISCOVERY][40k37e40][APPLE_MOBILE][WLAN][COREDEVICE_PERSISTED_PAIR_IDENTITY] found=false, error={}",ex.toString());
      return "";
    }
  }

  /** 40k37e38: Normalisiert die ersten ueber den lebenden WLAN-Tunnel gelesenen
   * RemotePairing/RSD-Basisfelder fuer den bereits vorhandenen E27-Basisinventarpfad.
   * Es werden nur bekannte, nicht-sensitive Geraeteeigenschaften uebernommen. */
  private void cacheCoreDeviceRsdBasisE38(String raw, String udid, String source) {
    if(raw==null || raw.isBlank()) return;
    try {
      JsonNode root=new ObjectMapper().readTree(raw);
      LinkedHashMap<String,String> wanted=new LinkedHashMap<>();
      String[] keys={"DeviceName","DeviceClass","ProductType","ProductVersion","HumanReadableProductVersionString","BuildVersion","HardwareModel","HWModelStr","CPUArchitecture","UniqueDeviceID","UDID","Identifier"};
      java.util.ArrayDeque<JsonNode> q=new java.util.ArrayDeque<>(); q.add(root);
      while(!q.isEmpty()) {
        JsonNode n=q.removeFirst();
        if(n.isObject()) {
          for(String key:keys) if(!wanted.containsKey(key) && n.has(key) && n.path(key).isValueNode()) {
            String v=n.path(key).asText("").trim(); if(!v.isBlank()) wanted.put(key,v);
          }
          n.elements().forEachRemaining(q::addLast);
        } else if(n.isArray()) n.elements().forEachRemaining(q::addLast);
      }
      if(wanted.isEmpty()) {
        log.info("[DISCOVERY][40k37e38][APPLE_MOBILE][WLAN][COREDEVICE_REMOTEPAIRING_RSD_NORMALIZE] semanticOk=false, udid={}, source={}, reason=NO_KNOWN_BASIS_FIELDS",maskUdid(udid),source);
        return;
      }
      com.fasterxml.jackson.databind.node.ObjectNode basis=new ObjectMapper().createObjectNode();
      wanted.forEach(basis::put);
      String json=new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(basis);
      COREDEVICE_REMOTEPAIRING_BASIS_E27.set(json);
      log.info("[DISCOVERY][40k37e38][APPLE_MOBILE][WLAN][COREDEVICE_REMOTEPAIRING_RSD_NORMALIZE] semanticOk=true, udid={}, source={}, deviceName={}, deviceClass={}, productVersion={}, buildVersion={}, hardwareModel={}, cpuArchitecture={}",
        maskUdid(udid),source,wanted.getOrDefault("DeviceName",""),wanted.getOrDefault("DeviceClass",""),
        firstNonBlank(wanted.getOrDefault("HumanReadableProductVersionString",""),wanted.getOrDefault("ProductVersion","")),
        wanted.getOrDefault("BuildVersion",""),firstNonBlank(wanted.getOrDefault("HardwareModel",""),wanted.getOrDefault("HWModelStr","")),wanted.getOrDefault("CPUArchitecture",""));
    } catch(Exception ex) {
      log.warn("[DISCOVERY][40k37e38][APPLE_MOBILE][WLAN][COREDEVICE_REMOTEPAIRING_RSD_NORMALIZE_ERROR] udid={}, source={}, error={}",maskUdid(udid),source,ex.toString());
    }
  }

  private String extractCoreDeviceUdidE37(String json) {
    if(json==null || json.isBlank()) return "";
    try {
      JsonNode root=new ObjectMapper().readTree(json);
      java.util.ArrayDeque<JsonNode> q=new java.util.ArrayDeque<>(); q.add(root);
      while(!q.isEmpty()) {
        JsonNode n=q.removeFirst();
        if(n.isObject()) {
          java.util.Iterator<java.util.Map.Entry<String,JsonNode>> it=n.fields();
          while(it.hasNext()) {
            java.util.Map.Entry<String,JsonNode> e=it.next();
            String k=e.getKey().toLowerCase(Locale.ROOT).replace("_","").replace("-","");
            JsonNode v=e.getValue();
            if((k.equals("udid") || k.equals("uniquedeviceid") || k.equals("identifier")) && v.isTextual()) {
              String x=v.asText("").trim();
              if(x.length()>=20 && x.matches("[A-Za-z0-9-]+")) return x;
            }
            if(v.isContainerNode()) q.add(v);
          }
        } else if(n.isArray()) for(JsonNode v:n) if(v.isContainerNode()) q.add(v);
      }
    } catch(Exception ignore) { }
    return "";
  }

  /** 40k37e48: Nur Diagnose der lokalen Interfaces. Keine Socket-/mDNS-Eigenimplementierung:
   * Bonjour bleibt vollstaendig Aufgabe von pymobiledevice3. */
  private void logPmd3BonjourInterfacesE48() {
    try {
      java.util.Enumeration<java.net.NetworkInterface> all=java.net.NetworkInterface.getNetworkInterfaces();
      while(all!=null && all.hasMoreElements()) {
        java.net.NetworkInterface ni=all.nextElement();
        if(!ni.isUp() || ni.isLoopback()) continue;
        java.util.List<String> ips=new java.util.ArrayList<>();
        java.util.Enumeration<java.net.InetAddress> addrs=ni.getInetAddresses();
        while(addrs.hasMoreElements()) {
          java.net.InetAddress a=addrs.nextElement();
          if(a instanceof java.net.Inet4Address) ips.add(a.getHostAddress());
        }
        log.info("[DISCOVERY][40k37e48][APPLE_MOBILE][WLAN][PMD3_BONJOUR_INTERFACE] name={}, displayName={}, multicast={}, virtual={}, ipv4={}",
          ni.getName(),ni.getDisplayName(),ni.supportsMulticast(),ni.isVirtual(),ips);
      }
    } catch(Exception ex) {
      log.info("[DISCOVERY][40k37e48][APPLE_MOBILE][WLAN][PMD3_BONJOUR_INTERFACE] diagnosticOk=false, error={}",ex.toString());
    }
  }

  /** 40k37e55h: Plattformadapter fuer die bereits vorhandene CoreDevice-WLAN-Logik.
   * Windows behaelt die isolierte GAM-Runtime. Linux akzeptiert dieselbe Runtime ohne
   * .exe oder eine bereits installierte pymobiledevice3-CLI aus PATH. Es wird nichts
   * installiert und keine zusaetzliche Discovery-Quelle eingefuehrt. */
  /** fix11: reine Eintritts-/Resolver-Diagnose. Veraendert weder Pairing noch Discovery. */
  private void logFix11AppleEntry(String context, boolean usbEnabled, boolean wlanEnabled) {
    try {
      Path runtime=findGamRoot().toAbsolutePath().normalize().resolve("tools").resolve("apple-coredevice").resolve("runtime");
      Path pmd=resolveCoreDevicePmd3E55h(runtime);
      String id=resolveTool("idevice_id");
      String info=resolveTool("ideviceinfo");
      String pair=resolveTool("idevicepair");
      log.warn("[DISCOVERY][40k37e56c-fix11][APPLE_MOBILE][ENTRY_DIAG] context={} os={} usbEnabled={} wlanEnabled={} idevice_id={} ideviceinfo={} idevicepair={} pymobiledevice3={} pmdExists={} gamRoot={} runtime={}",
        context,System.getProperty("os.name",""),usbEnabled,wlanEnabled,id,info,pair,pmd,pmd!=null && Files.isRegularFile(pmd),findGamRoot().toAbsolutePath().normalize(),runtime);
      if(wlanEnabled) {
        log.warn("[DISCOVERY][40k37e56c-fix11][APPLE_MOBILE][USERSPACE_RSD_SOURCE] context={} requested=true resolverReady={} action={}",
          context,pmd!=null && Files.isRegularFile(pmd),(pmd!=null && Files.isRegularFile(pmd))?"ENTER_EXISTING_USERSPACE_RSD_PATH":"DIAGNOSE_PLATFORM_UNDERLAY");
      } else {
        log.warn("[DISCOVERY][40k37e56c-fix11][APPLE_MOBILE][USERSPACE_RSD_SOURCE] context={} requested=false resolverReady={} action=SKIP_BY_SETTING",context,pmd!=null && Files.isRegularFile(pmd));
      }
    } catch(Exception ex) {
      log.warn("[DISCOVERY][40k37e56c-fix11][APPLE_MOBILE][ENTRY_DIAG] context={} result=FAILED error={}",context,ex.toString());
    }
  }

  private Path resolveCoreDevicePmd3E55h(Path runtime) {
    boolean windows=System.getProperty("os.name","").toLowerCase(Locale.ROOT).contains("win");
    log.info("[DISCOVERY][40k37e56c-fix10][APPLE_MOBILE][COREDEVICE_RESOLVER] event=START os={} runtime={}",System.getProperty("os.name",""),runtime);
    Path bundled=runtime.resolve("bin").resolve(windows?"pymobiledevice3.exe":"pymobiledevice3");
    if(Files.isRegularFile(bundled)) return bundled.toAbsolutePath().normalize();
    if(!windows) {
      // 40k37e55j: Linux nutzt eine GAM-eigene venv fuer pymobiledevice3. Python/venv
      // bleiben Systemvoraussetzungen; das Python-Paket selbst ist dadurch versionsfest
      // und veraendert weder Ubuntu-System-Python noch den globalen PATH.
      Path gamVenv=runtime.resolve("linux-venv").resolve("bin").resolve("pymobiledevice3");
      // fix14: Die GAM-eigene venv ist ein persistenter Bestandteil des Linux-Unterbaus.
      // Symlinks/Wrapper aus Python-venv werden ebenfalls akzeptiert; entscheidend ist,
      // dass der Pfad existiert und als Datei aufloesbar ist. Damit wird eine bereits
      // eingerichtete PMD3-Runtime nach Neustart/Update wiederverwendet und niemals nur
      // wegen eines zu strengen isRegularFile()-Checks erneut installiert.
      if(Files.exists(gamVenv) && !Files.isDirectory(gamVenv)) {
        Path resolved=gamVenv.toAbsolutePath().normalize();
        log.info("[DISCOVERY][40k37e56c-fix14][APPLE_MOBILE][COREDEVICE_RESOLVER] event=PERSISTENT_PMD3_FOUND executable={} executableBit={}",resolved,Files.isExecutable(resolved));
        return resolved;
      }
      // 40k37e56: macOS bekommt denselben isolierten pymobiledevice3-Unterbau wie Linux,
      // jedoch in einer eigenen Runtime. Dadurch bleiben Intel (x86_64) und Apple Silicon
      // (arm64/aarch64) voneinander und vom Linux-Teststand getrennt.
      boolean mac=System.getProperty("os.name","").toLowerCase(Locale.ROOT).contains("mac");
      if(mac) {
        Path macVenv=runtime.resolve("macos-venv").resolve("bin").resolve("pymobiledevice3");
        if(Files.exists(macVenv) && !Files.isDirectory(macVenv)) {
          Path resolved=macVenv.toAbsolutePath().normalize();
          log.info("[DISCOVERY][40k37e56c-fix34][APPLE_MOBILE][COREDEVICE_RESOLVER] event=MACOS_PERSISTENT_PMD3_FOUND source=current-root executable={} executableBit={}",resolved,Files.isExecutable(resolved));
          return resolved;
        }

        // fix34: Standalone-Updates duerfen in einen neuen Versionsordner entpackt werden.
        // Die grosse PMD3-venv bleibt dabei absichtlich persistent und muss nicht fuer
        // jeden Fix erneut heruntergeladen werden. Falls der aktuelle Root noch keine
        // macos-venv enthaelt, suchen wir ausschliesslich in GAM-Geschwisterordnern
        // desselben Elternverzeichnisses nach der bereits eingerichteten Runtime.
        Path currentRoot=findGamRoot().toAbsolutePath().normalize();
        Path parent=currentRoot.getParent();
        if(parent!=null && Files.isDirectory(parent)) {
          try (java.util.stream.Stream<Path> siblings=Files.list(parent)) {
            Optional<Path> persistent=siblings
              .filter(Files::isDirectory)
              .filter(x->!x.equals(currentRoot))
              .filter(AppleMobileDiscoveryService::isGamRoot)
              .map(x->x.resolve("tools").resolve("apple-coredevice").resolve("runtime").resolve("macos-venv").resolve("bin").resolve("pymobiledevice3"))
              .filter(x->Files.exists(x) && !Files.isDirectory(x))
              .sorted((a,b)->{
                try { return Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a)); }
                catch(Exception ignored) { return a.toString().compareTo(b.toString()); }
              })
              .findFirst();
            if(persistent.isPresent()) {
              Path resolved=persistent.get().toAbsolutePath().normalize();
              log.info("[DISCOVERY][40k37e56c-fix34][APPLE_MOBILE][COREDEVICE_RESOLVER] event=MACOS_PERSISTENT_PMD3_FOUND source=sibling-gam-root currentRoot={} executable={} executableBit={}",currentRoot,resolved,Files.isExecutable(resolved));
              appleTrace("FIX34 PMD3 RESOLVER","source=sibling-gam-root, currentRoot="+currentRoot+", executable="+resolved);
              return resolved;
            }
          } catch(Exception ex) {
            log.warn("[DISCOVERY][40k37e56c-fix34][APPLE_MOBILE][COREDEVICE_RESOLVER] event=MACOS_PERSISTENT_SCAN_FAILED currentRoot={} error={}",currentRoot,ex.toString());
          }
        }
      }
      String system=resolveTool("pymobiledevice3");
      if(system!=null && !system.isBlank()) {
        try { return Path.of(system).toAbsolutePath().normalize(); } catch(Exception ignore) { }
      }
    }
    log.info("[DISCOVERY][40k37e56c-fix10][APPLE_MOBILE][COREDEVICE_RESOLVER] event=NOT_FOUND os={} runtime={} bundled={} linuxVenv={} macVenv={} pathTool={}",System.getProperty("os.name",""),runtime,bundled,runtime.resolve("linux-venv").resolve("bin").resolve("pymobiledevice3"),runtime.resolve("macos-venv").resolve("bin").resolve("pymobiledevice3"),resolveTool("pymobiledevice3"));
    return null;
  }

  private void probeExistingCoreDeviceWifiE35() {
    try {
      // 40k37e55h: Derselbe bewaehrte Apple-WLAN/CoreDevice-Pfad wie unter Windows
      // wird nun auch unter Linux betreten. Nur die Betriebssystem-Unterlage fuer das
      // pymobiledevice3-Executable ist plattformspezifisch; Discovery, RSD-Verifikation
      // und AppleDevice-Erzeugung bleiben unveraendert.
      Path root=findGamRoot().toAbsolutePath().normalize();
      Path runtime=root.resolve("tools").resolve("apple-coredevice").resolve("runtime");
      Path pmdExe=resolveCoreDevicePmd3E55h(runtime);
      if(pmdExe==null) {
        log.info("[DISCOVERY][40k37e55h][APPLE_MOBILE][WLAN][NO_USB_REMOTEPAIR_SEQUENCE] skipped=true, reason=PMD3_RUNTIME_MISSING, os={}",System.getProperty("os.name",""));
        return;
      }
      log.info("[DISCOVERY][40k37e55h][APPLE_MOBILE][WLAN][COREDEVICE_RUNTIME] executable={}, os={}, route=WINDOWS_REFERENCE_PATH",pmdExe,System.getProperty("os.name",""));
      Map<String,String> env=new LinkedHashMap<>();
      env.put("UV_CACHE_DIR",runtime.resolve("cache").toString());
      env.put("UV_PYTHON_INSTALL_DIR",runtime.resolve("python").toString());
      env.put("UV_TOOL_DIR",runtime.resolve("tools").toString());
      env.put("UV_TOOL_BIN_DIR",runtime.resolve("bin").toString());
      env.put("UV_NO_MODIFY_PATH","1");
      env.put("PYTHONNOUSERSITE","1");
      env.put("PYMOBILEDEVICE3_USERSPACE_TCP_RELAY","1");
      env.put("PYMOBILEDEVICE3_DEFAULT_FALLBACK","userspace");

      // 40k37e48: USB und WLAN bleiben strikt unabhaengige Erkennungsquellen.
      // Dieser Pfad verwendet ausschliesslich pymobiledevice3/Bonjour/RemotePairing
      // und darf weder usbmux noch lockdown als Bootstrap-Voraussetzung verwenden.
      logPmd3BonjourInterfacesE48();

      log.info("[DISCOVERY][40k37e48][APPLE_MOBILE][WLAN][NO_USB_REMOTEPAIR_SEQUENCE] STEP=1A action=PMD3_BONJOUR_REMOTEPAIRING usbRequired=false");
      CommandResult bonjourRp=runProcessE20(20,env,pmdExe.toString(),"--no-color","bonjour","remotepairing");
      String bonjourRpData=(bonjourRp.stdout()+"\n"+bonjourRp.stderr()).trim();
      log.info("[DISCOVERY][40k37e48][APPLE_MOBILE][WLAN][BONJOUR_REMOTEPAIRING] processOk={}, exit={}, data={}",
        bonjourRp.ok(),bonjourRp.exitCode(),limitTrace(bonjourRpData));

      log.info("[DISCOVERY][40k37e48][APPLE_MOBILE][WLAN][NO_USB_REMOTEPAIR_SEQUENCE] STEP=1B action=PMD3_BONJOUR_REMOTEPAIRING_MANUAL_PAIRING usbRequired=false");
      CommandResult bonjourManual=runProcessE20(20,env,pmdExe.toString(),"--no-color","bonjour","remotepairing-manual-pairing");
      String bonjourManualData=(bonjourManual.stdout()+"\n"+bonjourManual.stderr()).trim();
      log.info("[DISCOVERY][40k37e48][APPLE_MOBILE][WLAN][BONJOUR_REMOTEPAIRING_MANUAL] processOk={}, exit={}, data={}",
        bonjourManual.ok(),bonjourManual.exitCode(),limitTrace(bonjourManualData));

      // Kontrollproben: zeigen, ob pymobiledevice3 ueber dasselbe Ethernet/LAN ueberhaupt
      // andere Apple-mDNS-Dienste des WLAN-iPads sieht. Ein leerer RemotePairing-Dienst
      // ist damit von einem generellen Bonjour-/Interface-Problem unterscheidbar.
      log.info("[DISCOVERY][40k37e48][APPLE_MOBILE][WLAN][NO_USB_REMOTEPAIR_SEQUENCE] STEP=1C action=PMD3_BONJOUR_RSD usbRequired=false");
      CommandResult bonjourRsd=runProcessE20(20,env,pmdExe.toString(),"--no-color","bonjour","rsd");
      log.info("[DISCOVERY][40k37e48][APPLE_MOBILE][WLAN][BONJOUR_RSD] processOk={}, exit={}, data={}",bonjourRsd.ok(),bonjourRsd.exitCode(),limitTrace((bonjourRsd.stdout()+"\n"+bonjourRsd.stderr()).trim()));

      log.info("[DISCOVERY][40k37e48][APPLE_MOBILE][WLAN][NO_USB_REMOTEPAIR_SEQUENCE] STEP=1D action=PMD3_BONJOUR_MOBDEV2 usbRequired=false");
      CommandResult bonjourMobdev2=runProcessE20(20,env,pmdExe.toString(),"--no-color","bonjour","mobdev2");
      log.info("[DISCOVERY][40k37e48][APPLE_MOBILE][WLAN][BONJOUR_MOBDEV2] processOk={}, exit={}, data={}",bonjourMobdev2.ok(),bonjourMobdev2.exitCode(),limitTrace((bonjourMobdev2.stdout()+"\n"+bonjourMobdev2.stderr()).trim()));

      log.info("[DISCOVERY][40k37e48][APPLE_MOBILE][WLAN][NO_USB_REMOTEPAIR_SEQUENCE] STEP=2 action=REMOTE_PAIR usbRequired=false elevation=false");
      CommandResult pair=runProcessE20(45,env,pmdExe.toString(),"--no-color","remote","pair");
      String pairData=(pair.stdout()+"\n"+pair.stderr()).trim();
      String pairLower=pairData.toLowerCase(Locale.ROOT);
      boolean pairSemanticOk=pair.ok() && !pairLower.contains("traceback")
        && !pairLower.contains("no devices were found") && !pairLower.contains("device is not connected")
        && !pairLower.contains("device not found") && !pairLower.contains("error: ");
      log.info("[DISCOVERY][40k37e48][APPLE_MOBILE][WLAN][REMOTE_PAIR] processOk={}, semanticOk={}, exit={}, data={}",pair.ok(),pairSemanticOk,pair.exitCode(),limitTrace(pairData));

      log.info("[DISCOVERY][40k37e48][APPLE_MOBILE][WLAN][NO_USB_REMOTEPAIR_SEQUENCE] STEP=3 action=REMOTE_BROWSE pairSemanticOk={}",pairSemanticOk);
      CommandResult browse=runProcessE20(30,env,pmdExe.toString(),"--no-color","remote","browse");
      String browseData=(browse.stdout()+"\n"+browse.stderr()).trim();
      log.info("[DISCOVERY][40k37e48][APPLE_MOBILE][WLAN][REMOTE_BROWSE_AFTER_PAIR] browseOk={}, exit={}, data={}",browse.ok(),browse.exitCode(),limitTrace(browseData));

      JsonNode wifi=null;
      if(browse.ok() && !browse.stdout().isBlank()) {
        try { wifi=new ObjectMapper().readTree(browse.stdout()).path("wifi"); }
        catch(Exception parseEx) { log.info("[DISCOVERY][40k37e48][APPLE_MOBILE][WLAN][REMOTE_BROWSE_AFTER_PAIR] parseOk=false, error={}",parseEx.toString()); }
      }
      if(wifi!=null && wifi.isArray() && !wifi.isEmpty()) {
        for(JsonNode item:wifi) {
          String udid=jsonText(item,"identifier").trim();
          if(udid.isBlank()) continue;
          cacheCoreDeviceWifiIpE33(browse.stdout(),udid);
          log.info("[DISCOVERY][40k37e48][APPLE_MOBILE][WLAN][NO_USB_REMOTEPAIR_SEQUENCE] STEP=4 action=USERSPACE_RSD_VERIFY source=REMOTE_BROWSE_AFTER_PAIR udid={}",maskUdid(udid));
          if(tryCoreDeviceUserspaceRsdE25(env,pmdExe,udid,"NO_USB_REMOTEPAIR_E48")) {
            log.info("[DISCOVERY][40k37e48][APPLE_MOBILE][WLAN][NO_USB_REMOTEPAIR_SEQUENCE] result=CONFIRMED, udid={}, transport=USERSPACE_RSD",maskUdid(udid));
            return;
          }
        }
      }
      log.info("[DISCOVERY][40k37e48][APPLE_MOBILE][WLAN][NO_USB_REMOTEPAIR_SEQUENCE] STEP=4 action=USERSPACE_RSD_AFTER_REMOTEPAIR browseWifiCandidates={} pairSemanticOk={} usbBootstrapUsed=false",(wifi!=null && wifi.isArray()?wifi.size():0),pairSemanticOk);
      probeCoreDeviceWifiTunnelWithoutBrowseE37(env,pmdExe);
    } catch(Exception ex) {
      log.warn("[DISCOVERY][40k37e48][APPLE_MOBILE][WLAN][NO_USB_REMOTEPAIR_SEQUENCE] result=FAILED, error={}",ex.toString());
    }
  }

  private List<AppleDevice> readNativeNetworkDevices() {
    log.info("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN] readNativeNetworkDevices() ENTER");

    // 40k37e54-fix21: Ein bewusst aktiver Admin-RSD ist fuer die Apple-Inventarisierung
    // verbindlich und hat Vorrang VOR Cache-/Userspace-/RemotePairing-Pfaden. Dadurch kann
    // ein bereits bestaetigter WLAN-Cache den Admin-Test nicht mehr umgehen. Der aktive
    // Endpoint wird exakt wiederverwendet; bei Fehler bleibt der Userspace-Fallback gesperrt.
    AdminTunnelStatus adminE54F21=adminTunnelStatus();
    if(adminE54F21.active()) {
      String adminUdid=firstNonBlank(ADMIN_RSD_UDID_E54F8.get(),COREDEVICE_SELECTED_UDID_E25.get(),recoverPersistedCoreDeviceUdidE40());
      Path adminPmd=findGamRoot().toAbsolutePath().normalize()
        .resolve("tools").resolve("apple-coredevice").resolve("runtime")
        .resolve("bin").resolve("pymobiledevice3.exe");
      log.info("[DISCOVERY][40k37e54-fix21][APPLE_MOBILE][WLAN][ADMIN_RSD_PRIORITY] active=true, udid={}, endpoint={}:{}, action=USE_EXACT_ACTIVE_ENDPOINT, fallback=BLOCKED",
        maskUdid(adminUdid),adminE54F21.host(),adminE54F21.port());
      if(adminUdid!=null && !adminUdid.isBlank() && Files.isRegularFile(adminPmd)) {
        Map<String,String> adminEnv=new LinkedHashMap<>();
        if(tryExplicitAdminRsdE54Fix8(adminEnv,adminPmd,adminUdid)) {
          AppleDevice adminDevice=coreDeviceNetworkDeviceE29(adminUdid);
          if(adminDevice!=null) {
            COREDEVICE_CONFIRMED_WIFI_E30.set(adminDevice);
            log.info("[DISCOVERY][40k37e54-fix21][APPLE_MOBILE][WLAN][COREDEVICE_TRANSPORT_SELECTED] udid={}, transport=ADMIN_RSD, endpoint={}:{}, action=STOP_ALTERNATIVE_APPLE_PATHS",
              maskUdid(adminUdid),adminE54F21.host(),adminE54F21.port());
            return List.of(adminDevice);
          }
        }
      }
      log.error("[DISCOVERY][40k37e54-fix21][APPLE_MOBILE][WLAN][ADMIN_RSD_REQUIRED] result=FAILED, udid={}, endpoint={}:{}, fallback=BLOCKED",
        maskUdid(adminUdid),adminE54F21.host(),adminE54F21.port());
      COREDEVICE_SELECTED_UDID_E25.set("");
      return List.of();
    }
    // e30: Nach einem bereits bestaetigten USERSPACE_RSD-Zugriff nicht erneut durch die
    // historischen Netzwerk-/Bootstrap-Pfade laufen. Der bestaetigte Apple-WLAN-Treffer
    // ist fuer die laufende Backend-Instanz die belastbare Discovery-Quelle.
    AppleDevice confirmedWifi=COREDEVICE_CONFIRMED_WIFI_E30.get();
    if(confirmedWifi!=null && confirmedWifi.udid()!=null && !confirmedWifi.udid().isBlank()) {
      // 40k37e51-fix5: Fuer einen USB/WLAN-Paritaetslauf darf ein alter bestaetigter
      // Discovery-Treffer nicht als frischer WLAN-Datensatz gelten. Sobald eine USB-
      // Referenz existiert, wird der bewiesene Userspace-Pfad erneut abgefragt.
      if(hasUsbReferenceE51Fix4(confirmedWifi.udid())) {
        Path pmdExe=findGamRoot().toAbsolutePath().normalize()
            .resolve("tools").resolve("apple-coredevice").resolve("runtime")
            .resolve("bin").resolve("pymobiledevice3.exe");
        if(Files.isRegularFile(pmdExe)) {
          Map<String,String> refreshEnv=new LinkedHashMap<>();
          refreshEnv.put("PYMOBILEDEVICE3_USERSPACE_TCP_RELAY","1");
          refreshEnv.put("PYMOBILEDEVICE3_USERSPACE","1");
          refreshEnv.put("PYMOBILEDEVICE3_DEFAULT_FALLBACK","userspace");
          refreshEnv.put("PYMOBILEDEVICE3_UDID",confirmedWifi.udid());
          log.info("[DISCOVERY][40k37e54][APPLE_MOBILE][PARITY][WLAN_FRESH_READ] udid={}, action=REFRESH_VIA_PROVEN_USERSPACE_RSD",maskUdid(confirmedWifi.udid()));
          if(tryHistoricalUserspaceRsdRetryE51Fix2(refreshEnv,pmdExe,confirmedWifi.udid())) {
            compareUsbWlanParityE51Fix4(confirmedWifi.udid(),"FRESH_WLAN_AFTER_USB_REFERENCE");
            AppleDevice refreshed=COREDEVICE_CONFIRMED_WIFI_E30.get();
            if(refreshed!=null) confirmedWifi=refreshed;
          } else {
            log.info("[DISCOVERY][40k37e54][APPLE_MOBILE][PARITY][WLAN_FRESH_READ] udid={}, result=FAILED, action=PRESERVE_CONFIRMED_DISCOVERY_BUT_NO_PARITY_REPORT",maskUdid(confirmedWifi.udid()));
          }
        }
      }
      log.info("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN][COREDEVICE_CONFIRMED_REUSE] udid={}, platform={}, serial={}, transport=USERSPACE_RSD, action=EMIT_WLAN_DISCOVERY_RESULT",maskUdid(confirmedWifi.udid()),confirmedWifi.platform(),confirmedWifi.serialNumber());
      appleTrace("E30 NETWORK DISCOVERY REUSE","devices=1, transport=USERSPACE_RSD, udid="+traceUdid(confirmedWifi.udid()));
      return List.of(confirmedWifi);
    }
    appleTrace("E13 NETWORK DISCOVERY START","mode=safe-cli-only, command=idevice_id -n");
    CommandResult ids=runToolTraced(8,"E13 NETWORK IDEVICE_ID","idevice_id","-n");
    List<String> networkUdids=parseUdidLines(ids.stdout());
    log.info("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN] idevice_id -n: ok={}, udids={}, stderr={}", ids.ok(), networkUdids.size(), limitTrace(ids.stderr()));
    appleTrace("E13 NETWORK UDIDS","count="+networkUdids.size()+", values="+traceValue(String.join(",",networkUdids))+
      ", exitOk="+ids.ok()+", stdoutEmpty="+ids.stdout().trim().isBlank()+", stderr="+traceValue(limitTrace(ids.stderr())));
    if(!ids.ok()||networkUdids.isEmpty()){
      CommandResult usbIds=runToolTraced(6,"E13 WLAN USB CANDIDATES","idevice_id","-l");
      List<String> usbUdids=parseUdidLines(usbIds.stdout());
      appleTrace("E13 WLAN DIAG PRECONDITION","networkEmpty="+networkUdids.isEmpty()+", usbCandidates="+usbUdids.size()+", usbExitOk="+usbIds.ok());
      for(String usbUdid:usbUdids){
        CommandResult state=runToolTraced(6,"E13 WLAN LOCKDOWN READONLY","ideviceinfo","-u",usbUdid,"-q","com.apple.mobile.wireless_lockdown");
        appleTrace("E13 WLAN LOCKDOWN READONLY RESULT","udid="+traceUdid(usbUdid)+", ok="+state.ok()+
          ", data="+traceValue(limitTrace(state.stdout()))+", stderr="+traceValue(limitTrace(state.stderr())));
        diagnoseAppleNetworkE10(usbUdid,state);
      }
      // 40k37e35: Auch ohne idevice_id-Netzwerk-UDID und ohne USB-Kandidaten den bereits
      // vorhandenen CoreDevice/RemotePairing-WLAN-Pfad direkt pruefen. Damit ist die neue
      // Capability-Diagnose nicht mehr vom klassischen libimobiledevice-Treffer abhaengig.
      if(COREDEVICE_SELECTED_UDID_E25.get().isBlank()) probeExistingCoreDeviceWifiE35();

      // 40k37e33: Moderne iPadOS-Versionen erscheinen nicht zwingend in `idevice_id -n`.
      // Wenn CoreDevice/USERSPACE_RSD im Diagnosepfad erfolgreich selektiert wurde, IST das
      // ein bestaetigter WLAN-Treffer. Er wird deshalb als reguläres NETWORK-Gerät an die
      // allgemeine GAM-Merge-Pipeline gegeben. Die stabile UDID verbindet ihn später mit USB.
      String selected=COREDEVICE_SELECTED_UDID_E25.get();
      if(selected!=null && !selected.isBlank()) {
        AppleDevice coreDeviceWifi=coreDeviceNetworkDeviceE29(selected);
        if(coreDeviceWifi!=null) {
          appleTrace("E29 NETWORK DISCOVERY","devices=1, transport=USERSPACE_RSD, udid="+traceUdid(selected));
          log.info("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN][COREDEVICE_NETWORK_DEVICE] udid={}, platform={}, serial={}, transport=USERSPACE_RSD, action=EMIT_WLAN_DISCOVERY_RESULT",maskUdid(selected),coreDeviceWifi.platform(),coreDeviceWifi.serialNumber());
          return List.of(coreDeviceWifi);
        }
      }
      appleTrace("E13 NETWORK DISCOVERY","devices=0, safeCliOnly=true, nativeWifiWrite=disabled");
      log.info("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN] RESULT leer: keine Netzwerk-UDID und kein bestaetigter CoreDevice-WLAN-Treffer; USB-Kandidaten={}", usbUdids.size());
      return List.of();
    }
    List<AppleDevice> out=new ArrayList<>();
    for(String udid:networkUdids){
      CommandResult info=runToolTraced(10,"E13 NETWORK IDEVICEINFO","ideviceinfo","-n","-u",udid);
      if(!info.ok()||info.stdout().isBlank()){
        appleTrace("E13 NETWORK DEVICE","udid="+traceUdid(udid)+", lockdown=false, stderr="+traceValue(limitTrace(info.stderr())));
        continue;
      }
      Map<String,String> m=parseInfo(info.stdout());
      String productType=m.getOrDefault("ProductType","");
      String platform=productType.toLowerCase(Locale.ROOT).startsWith("ipad")?"iPadOS":"iOS";
      AppleDevice d=new AppleDevice(udid,m.getOrDefault("DeviceName",""),platform,productType,appleModelDisplayName(productType),
        m.getOrDefault("ProductVersion",""),m.getOrDefault("BuildVersion",""),m.getOrDefault("SerialNumber",""),
        m.getOrDefault("ModelNumber",""),m.getOrDefault("HardwareModel",""),m.getOrDefault("CPUArchitecture",""),
        m.getOrDefault("WiFiAddress",""),m.getOrDefault("BluetoothAddress",""),m.getOrDefault("TimeZone",""),
        true,"Netzwerk-Lockdown-Zugriff erfolgreich (Pairing bestaetigt)","NETWORK");
      out.add(d);
      appleTrace("E13 NETWORK DEVICE","udid="+traceUdid(udid)+", lockdown=true, platform="+traceValue(d.platform())+", serial="+traceValue(d.serialNumber()));
    }
    appleTrace("E13 NETWORK DISCOVERY","devices="+out.size()+", transport=network, safeCliOnly=true");
    log.info("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN] RESULT: Netzwerkgeraete={}", out.size());
    return out;
  }

  /** 40k37e33: Erzeugt aus dem bestaetigten CoreDevice/USERSPACE_RSD-Zugang einen
   * regulaeren Apple-WLAN-Discovery-Treffer. Der e27-Cache enthaelt bereits direkt das
   * deviceKVSData-Basisobjekt (nicht mehr den kompletten peerDeviceInfo-Wrapper).
   * Genau dieser Formfehler verhinderte in e29 trotz erfolgreichem RSD den WLAN-Result. */
  private AppleDevice coreDeviceNetworkDeviceE29(String selectedUdid) {
    try {
      AppleDevice cached=COREDEVICE_CONFIRMED_WIFI_E30.get();
      if(cached!=null && selectedUdid.equalsIgnoreCase(cached.udid())) return cached;
      String raw=COREDEVICE_REMOTEPAIRING_BASIS_E27.get();
      if(raw==null || raw.isBlank()) return null;
      JsonNode basis=new ObjectMapper().readTree(raw);
      if(basis==null || basis.isMissingNode() || !basis.isObject()) return null;
      String kvsUdid=jsonText(basis,"UniqueDeviceID").trim();
      // Die Cache-Zuordnung stammt aus exakt dem RemotePairing-Lauf fuer selectedUdid.
      // Falls Apple die UDID im KVS mitsendet, muss sie zwingend uebereinstimmen.
      if(!kvsUdid.isBlank() && !kvsUdid.equalsIgnoreCase(selectedUdid)) {
        log.warn("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN][COREDEVICE_NETWORK_IDENTITY_MISMATCH] selectedUdid={}, basisUdid={}",maskUdid(selectedUdid),maskUdid(kvsUdid));
        return null;
      }
      String productType=jsonText(basis,"ProductType");
      String deviceClass=jsonText(basis,"DeviceClass");
      String platform=(productType.toLowerCase(Locale.ROOT).startsWith("ipad") || "ipad".equalsIgnoreCase(deviceClass))?"iPadOS":"iOS";
      String hw=firstNonBlank(jsonText(basis,"HardwareModel"),jsonText(basis,"HWModelStr"));
      AppleDevice result=new AppleDevice(selectedUdid,
        firstNonBlank(jsonText(basis,"DeviceName"),jsonText(basis,"UserAssignedDeviceName")),
        platform, productType, firstNonBlank(jsonText(basis,"MarketingNameString"),appleModelDisplayName(productType)),
        firstNonBlank(jsonText(basis,"ProductVersion"),jsonText(basis,"HumanReadableProductVersionString")),
        jsonText(basis,"BuildVersion"), jsonText(basis,"SerialNumber"),
        jsonText(basis,"ModelNumber"), hw, jsonText(basis,"CPUArchitecture"),
        jsonText(basis,"WiFiAddress"), jsonText(basis,"BluetoothAddress"), jsonText(basis,"TimeZone"),
        true,"CoreDevice/USERSPACE_RSD-WLAN-Zugriff erfolgreich (RemotePairing bestaetigt)","NETWORK");
      COREDEVICE_CONFIRMED_WIFI_E30.set(result);
      log.info("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN][COREDEVICE_CONFIRMED_WIFI_CACHED] udid={}, platform={}, serial={}, productType={}, transport=ADMIN_RSD",maskUdid(selectedUdid),platform,result.serialNumber(),productType);
      return result;
    } catch(Exception ex) {
      log.warn("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN][COREDEVICE_NETWORK_DEVICE_ERROR] udid={}, error={}",maskUdid(selectedUdid),ex.toString());
      return null;
    }
  }

  /** 40k37e25: rein lesende IPv6-/Bonjour-Diagnose fuer den Apple-WLAN-Uebergang.
   * Keine Pairing- oder Geraetekonfiguration wird veraendert. */
  /** 40k37e33: Konsolidierter WLAN-Einstieg.
   * Wireless-Lockdown dient nur noch als Bootstrap-Signal; historische mDNS/TCP/CIM-
   * Forschungsdiagnosen sind entfernt. Der produktive CoreDevice-Pfad entscheidet selbst,
   * ob RemotePairing/USERSPACE_RSD verfuegbar ist. */
  private void diagnoseAppleNetworkE10(String udid, CommandResult wirelessState) {
    try {
      Map<String,String> w=parseInfo(wirelessState.stdout());
      String service=w.getOrDefault("BonjourFullServiceName","");
      log.info("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN][WIRELESS_LOCKDOWN] udid={}, enabled={}, supportsWifi={}, supportsSyncing={}",
        traceUdid(udid),w.getOrDefault("EnableWifiConnections",""),w.getOrDefault("SupportsWifi",""),w.getOrDefault("SupportsWifiSyncing",""));
      diagnoseAppleCoreDevicePathE15(udid,service);
    } catch(Exception ex) {
      log.warn("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN][WIRELESS_LOCKDOWN_ERROR] udid={}, error={}",maskUdid(udid),ex.toString());
    }
  }

  private void diagnoseAppleCoreDevicePathE15(String udid, String fullServiceName) {
    try {
      CommandResult info=runToolTraced(6,"E15 COREDEVICE IDEVICEINFO USB","ideviceinfo","-u",udid);
      Map<String,String> values=parseInfo(info.stdout());
      String productVersion=values.getOrDefault("ProductVersion","").trim();
      String productType=values.getOrDefault("ProductType","").trim();
      String buildVersion=values.getOrDefault("BuildVersion","").trim();
      // 40k37e33-fix8: e32 hat die historische Bonjour-Instanz-Hilfsmethode bewusst entfernt.
      // supportsRP steht bereits im uebergebenen Service-Namen; direkt auswerten, ohne den alten e10-Forschungspfad wieder einzufuehren.
      String rpVersion="";
      java.util.regex.Matcher rpm=java.util.regex.Pattern.compile("(?i)-supportsRP-([0-9]+)").matcher(firstNonBlank(fullServiceName,""));
      if(rpm.find()) rpVersion=rpm.group(1);

      CommandResult networkIds=runToolTraced(5,"E15 COREDEVICE IDEVICE_ID NETWORK","idevice_id","-n");
      boolean classicNetworkVisible=networkIds.stdout().lines().map(String::trim).anyMatch(x -> x.equalsIgnoreCase(udid));

      String python="";
      String pmd="";
      boolean windows=System.getProperty("os.name","").toLowerCase(Locale.ROOT).contains("win");
      if(windows){
        CommandResult py=run(5,"powershell.exe","-NoProfile","-NonInteractive","-Command",
          "$c=Get-Command python.exe,py.exe -ErrorAction SilentlyContinue | Select-Object -First 1; if($c){$c.Source}");
        python=py.stdout().trim();
        CommandResult pm=run(8,"powershell.exe","-NoProfile","-NonInteractive","-Command",
          "$ErrorActionPreference='SilentlyContinue'; $p=Get-Command pymobiledevice3.exe -ErrorAction SilentlyContinue | Select-Object -First 1; if($p){$p.Source} else { python -m pymobiledevice3 version 2>&1 }");
        pmd=(pm.stdout()+" "+pm.stderr()).trim();
      } else {
        // 40k37e55i: Nur Linux-Unterlage fuer den bestehenden Windows-Referenzpfad.
        // Keine neue Discovery-Quelle: wir zeigen exakt die Tools, die derselbe
        // CoreDevice/RemotePairing-Pfad anschliessend verwendet.
        String py3=resolveTool("python3");
        if(py3==null || py3.isBlank()) py3=resolveTool("python");
        python=firstNonBlank(py3,"");
        Path runtime=findGamRoot().toAbsolutePath().normalize().resolve("tools").resolve("apple-coredevice").resolve("runtime");
        Path linuxPmd=resolveCoreDevicePmd3E55h(runtime);
        pmd=linuxPmd==null?"":linuxPmd.toString();
      }

      int major=-1;
      try { if(!productVersion.isBlank()) major=Integer.parseInt(productVersion.split("\\.")[0]); } catch(Exception ignored) {}
      boolean modernCoreDevice=(major>=17);
      boolean advertisesRemotePairing=!rpVersion.isBlank();
      String classification = classicNetworkVisible ? "CLASSIC_USBMUXD_NETWORK_VISIBLE"
        : (modernCoreDevice && advertisesRemotePairing ? "COREDEVICE_REMOTEPAIRING_CANDIDATE" : "NETWORK_PATH_UNRESOLVED");

      log.info("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN][COREDEVICE_PATH] udid={}, productType={}, productVersion={}, buildVersion={}, supportsRP={}, classicNetworkVisible={}, classification={}",
        traceUdid(udid),productType,productVersion,buildVersion,rpVersion,classicNetworkVisible,classification);
      log.info("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN][COREDEVICE_TOOLS] python={}, pymobiledevice3={}",
        limitTrace(python),limitTrace(pmd));
      appleTrace("E20 WLAN COREDEVICE PATH","udid="+traceUdid(udid)+", productType="+traceValue(productType)+
        ", productVersion="+traceValue(productVersion)+", buildVersion="+traceValue(buildVersion)+
        ", supportsRP="+traceValue(rpVersion)+", classicNetworkVisible="+classicNetworkVisible+", classification="+classification);
      appleTrace("E20 WLAN COREDEVICE TOOLS","python="+traceValue(limitTrace(python))+", pymobiledevice3="+traceValue(limitTrace(pmd)));

      // 40k37e25: Fuer aktuelle iOS/iPadOS-Geraete wird eine GAM-eigene, isolierte
      // CoreDevice-Runtime verwendet. Sie veraendert weder System-Python noch PATH.
      if("COREDEVICE_REMOTEPAIRING_CANDIDATE".equals(classification)) {
        diagnoseGamCoreDeviceRuntimeE20(udid);
      }
    } catch(Exception ex){
      log.warn("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN][COREDEVICE_PATH_ERROR] {}",ex.toString());
      appleTrace("E20 WLAN COREDEVICE PATH ERROR",traceValue(ex.toString()));
    }
  }

  /** 40k37e25: CoreDevice-Runtime. PowerShell wird nur noch fuer den einmaligen uv-Bootstrap
   * verwendet. uv und pymobiledevice3 laufen danach direkt als Java-Kindprozesse; stdout/stderr
   * werden parallel konsumiert, damit volle Pipes keinen Prozess-Deadlock mehr erzeugen. */
  private void diagnoseGamCoreDeviceRuntimeE20(String udid) {
    long now=System.currentTimeMillis();
    long previous=COREDEVICE_LAST_ATTEMPT_MS_E25.get();
    if(previous>0L && now-previous<COREDEVICE_DUPLICATE_SUPPRESS_MS_E25) {
      long remaining=Math.max(0L,COREDEVICE_DUPLICATE_SUPPRESS_MS_E25-(now-previous));
      log.info("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN][COREDEVICE_RUNTIME] skipped=true, reason=DUPLICATE_ATTEMPT_SUPPRESSED, remainingMs={}, udid={}",remaining,maskUdid(udid));
      appleTrace("E25 FIX1 COREDEVICE DUPLICATE SUPPRESSED","remainingMs="+remaining+", udid="+maskUdid(udid));
      return;
    }
    if(!COREDEVICE_BOOTSTRAP_ACTIVE_E20.compareAndSet(false,true)) {
      log.info("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN][COREDEVICE_RUNTIME] skipped=true, reason=PARALLEL_VARIANT_TEMPORARILY_DISABLED");
      appleTrace("E25 FIX1 COREDEVICE RUNTIME","skipped=true, reason=PARALLEL_VARIANT_TEMPORARILY_DISABLED");
      return;
    }
    // Zeitpunkt erst nach erfolgreichem Guard setzen: parallele Varianten duerfen das Fenster nicht verlaengern.
    COREDEVICE_LAST_ATTEMPT_MS_E25.set(now);
    try {
      boolean windows=System.getProperty("os.name","").toLowerCase(Locale.ROOT).contains("win");
      Path root=findGamRoot().toAbsolutePath().normalize();
      Path core=root.resolve("tools").resolve("apple-coredevice").toAbsolutePath().normalize();
      Path runtime=core.resolve("runtime");
      Path binDir=runtime.resolve("bin");
      Path pmdExe=windows ? binDir.resolve("pymobiledevice3.exe") : resolveCoreDevicePmd3E55h(runtime);
      if(pmdExe==null || !Files.isRegularFile(pmdExe)) {
        log.info("[DISCOVERY][40k37e55i][APPLE_MOBILE][WLAN][COREDEVICE_RUNTIME] ready=false, reason=PMD3_NOT_FOUND, os={}",System.getProperty("os.name",""));
        appleTrace("E55I WLAN COREDEVICE RUNTIME","ready=false, reason=PMD3_NOT_FOUND");

        // 40k37e56c-fix13: Genau dieser Pfad wird von Apple-Geraete pruefen / pairingTest
        // nachweislich erreicht. Unter Linux darf PMD3_NOT_FOUND deshalb nicht mehr nur
        // diagnostisch enden: hier wird der in fix12 eingefuehrte distributionsbreite
        // CoreDevice-Unterbau direkt hergestellt und danach im SELBEN Lauf erneut
        // aufgeloest. Der Apple-Oberbau/Collector bleibt unveraendert.
        if(!windows && System.getProperty("os.name","").toLowerCase(Locale.ROOT).contains("linux")) {
          appleTrace("FIX13 LINUX COREDEVICE BOOTSTRAP","event=START, reason=PMD3_NOT_FOUND");
          ToolchainInstallResult bootstrap=linuxToolchainPackageManagerE55(now);
          appleTrace("FIX13 LINUX COREDEVICE BOOTSTRAP",
            "event=RESULT, success="+bootstrap.success()+", state="+bootstrap.state()+", message="+limitTrace(bootstrap.message()));
          log.info("[DISCOVERY][40k37e56c-fix13][APPLE_MOBILE][WLAN][COREDEVICE_RUNTIME] event=LINUX_BOOTSTRAP_RESULT success={} state={} message={}",
            bootstrap.success(),bootstrap.state(),limitTrace(bootstrap.message()));
          pmdExe=resolveCoreDevicePmd3E55h(runtime);
          boolean readyAfterBootstrap=pmdExe!=null && Files.isRegularFile(pmdExe);
          appleTrace("FIX13 LINUX COREDEVICE BOOTSTRAP",
            "event=RESOLVE_AFTER_BOOTSTRAP, ready="+readyAfterBootstrap+", executable="+traceValue(pmdExe==null?null:pmdExe.toString()));
          if(!readyAfterBootstrap) {
            log.warn("[DISCOVERY][40k37e56c-fix13][APPLE_MOBILE][WLAN][COREDEVICE_RUNTIME] ready=false reason=PMD3_BOOTSTRAP_FAILED state={}",bootstrap.state());
            return;
          }
          log.info("[DISCOVERY][40k37e56c-fix13][APPLE_MOBILE][WLAN][COREDEVICE_RUNTIME] ready=true event=PMD3_READY_AFTER_BOOTSTRAP executable={}",pmdExe);
          appleTrace("FIX13 LINUX COREDEVICE BOOTSTRAP","event=PMD3_READY, executable="+traceValue(pmdExe.toString()));
        } else {
          return;
        }
      }
      Path uvExe=runtime.resolve("uv").resolve("uv.exe");
      Path marker=runtime.resolve("pymobiledevice3-11.16.3.ready");
      Files.createDirectories(runtime);

      // 40k37e55i: Unter Linux wird keine Windows-Runtime gebootstrapped. Ein bereits
      // vorhandenes pymobiledevice3 wird nur als Plattform-Unterlage an denselben
      // CoreDevice-WLAN-Pfad uebergeben. Windows bleibt unveraendert.
      if(windows && !Files.isRegularFile(uvExe)) {
        Path script=core.resolve("bootstrap-windows.ps1");
        if(!script.startsWith(root.resolve("tools").toAbsolutePath().normalize()) || !Files.isRegularFile(script)) {
          log.warn("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN][COREDEVICE_RUNTIME] ready=false, reason=UV_BOOTSTRAP_SCRIPT_MISSING, script={}",script);
          return;
        }
        CommandResult bootstrap=runProcessE20(45, Map.of(), "powershell.exe","-NoProfile","-NonInteractive","-ExecutionPolicy","Bypass",
          "-File",script.toString(),"-GamRoot",root.toString(),"-Udid",udid,"-UvOnly");
        log.info("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN][COREDEVICE_UV_BOOTSTRAP] ok={}, exit={}, data={}",
          bootstrap.ok(),bootstrap.exitCode(),limitTrace((bootstrap.stdout()+"\n"+bootstrap.stderr()).trim()));
        if(!bootstrap.ok() || !Files.isRegularFile(uvExe)) return;
      }

      Map<String,String> env=new LinkedHashMap<>();
      env.put("UV_CACHE_DIR",runtime.resolve("cache").toString());
      env.put("UV_PYTHON_INSTALL_DIR",runtime.resolve("python").toString());
      env.put("UV_TOOL_DIR",runtime.resolve("tools").toString());
      env.put("UV_TOOL_BIN_DIR",binDir.toString());
      env.put("UV_NO_MODIFY_PATH","1");
      env.put("PYTHONNOUSERSITE","1");
      env.put("PYMOBILEDEVICE3_USERSPACE_TCP_RELAY","1");
      if(udid!=null && !udid.isBlank()) env.put("PYMOBILEDEVICE3_UDID",udid);
      Files.createDirectories(binDir);

      if(windows) {
        CommandResult uvVersion=runProcessE20(15,env,uvExe.toString(),"--version");
        log.info("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN][COREDEVICE_UV] ok={}, exit={}, data={}",uvVersion.ok(),uvVersion.exitCode(),limitTrace((uvVersion.stdout()+" "+uvVersion.stderr()).trim()));
        if(!uvVersion.ok()) return;
      } else {
        CommandResult pmdVersion=runProcessE20(15,env,pmdExe.toString(),"version");
        log.info("[DISCOVERY][40k37e55i][APPLE_MOBILE][WLAN][COREDEVICE_PMD3_LINUX] ok={}, exit={}, executable={}, data={}",pmdVersion.ok(),pmdVersion.exitCode(),pmdExe,limitTrace((pmdVersion.stdout()+" "+pmdVersion.stderr()).trim()));
        appleTrace("E55I WLAN COREDEVICE PMD3","ok="+pmdVersion.ok()+", exit="+pmdVersion.exitCode()+", executable="+traceValue(pmdExe.toString()));
        if(!pmdVersion.ok()) return;
      }

      if(windows && (!Files.isRegularFile(pmdExe) || !Files.isRegularFile(marker))) {
        long t0=System.currentTimeMillis();
        CommandResult install=runProcessE20(180,env,uvExe.toString(),"tool","install","--force","--python","3.13","pymobiledevice3==11.16.3");
        log.info("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN][COREDEVICE_UV_INSTALL] ok={}, exit={}, durationMs={}, data={}",
          install.ok(),install.exitCode(),System.currentTimeMillis()-t0,limitTrace((install.stdout()+"\n"+install.stderr()).trim()));
        if(!install.ok() || !Files.isRegularFile(pmdExe)) {
          log.warn("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN][COREDEVICE_RUNTIME] ready=false, reason=PMD3_INSTALL_FAILED, exe={}",pmdExe);
          return;
        }
        Files.writeString(marker,"pymobiledevice3=11.16.3\npython=3.13\nuv=0.12.13\n",StandardCharsets.US_ASCII,
          StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING,StandardOpenOption.WRITE);
      }

      // 40k37e33-fix8: Zwei direkte pymobiledevice3-CLI-Kandidaten gegen die tatsaechlich
      // installierte 11.16.3 pruefen. Erst Help/Existenz feststellen, dann nur vorhandene
      // Befehle ausfuehren. Eine IPv4 wird nur bei eindeutigem Apple-/UDID-Bezug uebernommen.
      probeDirectPymobiledeviceIpE33Fix7(env,pmdExe,udid);

      // 40k37e53-fix1: `remote browse` ist bei normalen iPads haeufig leer, obwohl der
      // funktionierende RemotePairing/Userspace-RSD-Weg bereits existiert. Bonjour kann in
      // diesem Fall trotzdem den physischen RFC1918-Endpunkt des iPads liefern. Nur wenn die
      // Ausgabe gleichzeitig die EXAKTE Apple-UDID enthaelt, darf diese Adresse als echte
      // LAN-IP des Apple-Treffers in die allgemeine Merge-Engine gelangen.
      CommandResult mergeBonjourRp=runProcessE20(20,env,pmdExe.toString(),"--no-color","bonjour","remotepairing");
      String mergeBonjourRpData=(mergeBonjourRp.stdout()+"\n"+mergeBonjourRp.stderr()).trim();
      cacheUdidBoundPrivateLanIpE53Fix1(mergeBonjourRpData,udid,"BONJOUR_REMOTEPAIRING");

      CommandResult browse=runProcessE20(30,env,pmdExe.toString(),"--no-color","remote","browse");
      String data=(browse.stdout()+"\n"+browse.stderr()).trim();
      log.info("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN][COREDEVICE_REMOTE_BROWSE] ok={}, exit={}",browse.ok(),browse.exitCode());
      appleTrace("E24 WLAN COREDEVICE REMOTE BROWSE","ok="+browse.ok()+", exit="+browse.exitCode()+", data="+traceValue(limitTrace(data)));
      cacheCoreDeviceWifiIpE33(data, udid);


      // Falls das RemotePairing bereits aus einem frueheren Lauf vorhanden ist,
      // muss e25 ohne erneuten USB-Bootstrap direkt zum WLAN-Datenzugriff gelangen.
      String compactBrowse=data.replaceAll("\\s+","");
      // 40k37e56c-fix36: Aktuelle pymobiledevice3-Versionen liefern `remote browse`
      // auf macOS als JSON-Array der RemotePairing-Peers und nicht als historisches
      // {"usb": [...], "wifi": [...]}-Objekt. fix35 hat PMD3/RemotePairing wieder
      // funktionsfaehig gemacht; der erfolgreiche Peer (authenticated + available)
      // wurde hier bislang nur wegen des alten JSON-Formattests verworfen. Ein solcher
      // Peer ist noch NICHT allein ein WLAN-Treffer: er oeffnet ausschliesslich den
      // bestehenden USERSPACE_RSD-Probe. Erst dessen semantischer Erfolg selektiert
      // das Geraet und erzeugt spaeter den regulaeren NETWORK-Treffer.
      boolean browseWifiVisible=browse.ok() && (
        (compactBrowse.contains("\"wifi\":[") && !compactBrowse.contains("\"wifi\":[]"))
        || remoteBrowseHasAuthenticatedAvailablePeerFix36(data)
      );
      appleTrace("FIX36 REMOTE BROWSE CLASSIFY","ok="+browse.ok()+", authenticatedAvailable="+
        remoteBrowseHasAuthenticatedAvailablePeerFix36(data)+", wlanCandidate="+browseWifiVisible+", udid="+traceUdid(udid));
      if(browseWifiVisible && udid!=null && !udid.isBlank()) {
        if(tryCoreDeviceRsdViaWifiTunnelE25(env,pmdExe,udid,"DIRECT_REMOTE_BROWSE_FIX36")) return;
      }

      // 40k37e25: e23 hat gezeigt, dass `remote pair` den normalen iPadOS-26.6-
      // Kandidaten nicht findet (Bonjour-Browse der CLI bleibt leer). Deshalb wird jetzt
      // der von pymobiledevice3 vorgesehene einmalige Bootstrap ueber die bereits
      // vertrauenswuerdige USB-Lockdown-Verbindung versucht: `lockdown remotepairing --pair`.
      // Apple-Trust wird nicht umgangen; ohne vorhandene USB-Vertrauensbeziehung darf der
      // Befehl fehlschlagen. Danach wird sofort erneut `remote browse` ausgefuehrt.
      boolean browseEmpty=data.replaceAll("\\s+","").contains("\"usb\":[]")
        && data.replaceAll("\\s+","").contains("\"wifi\":[]");
      if(browse.ok() && browseEmpty) {
        if(udid!=null && !udid.isBlank()) {
          CommandResult usbPair=runProcessE20(75,env,pmdExe.toString(),"--no-color","lockdown","remotepairing","--pair");
          String usbPairData=(usbPair.stdout()+"\n"+usbPair.stderr()).trim();
          String usbPairLower=usbPairData.toLowerCase(Locale.ROOT);
          cacheRemotePairingBasisE27(usbPairData,udid,"USB_PAIR");
          boolean usbPairSemanticOk=usbPair.ok() && !usbPairLower.contains("error ")
            && !usbPairLower.contains("traceback") && !usbPairLower.contains("no device");
          log.info("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][USB_WLAN][COREDEVICE_REMOTEPAIRING] processOk={}, semanticOk={}, exit={}, udid={}",
            usbPair.ok(),usbPairSemanticOk,usbPair.exitCode(),maskUdid(udid));
          if(usbPairSemanticOk) {
            // fix2: Der erste Browse liegt VOR dem Bootstrap und ist bei einem frischen Backend
            // typischerweise noch leer. Nach erfolgreichem Pairing deshalb erneut browsen und
            // die echte IPv4 nur bei exakt passender Apple-UDID uebernehmen.
            CommandResult postPairBrowse=runProcessE20(30,env,pmdExe.toString(),"--no-color","remote","browse");
            String postPairBrowseData=(postPairBrowse.stdout()+"\n"+postPairBrowse.stderr()).trim();
            log.info("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN][COREDEVICE_REMOTE_BROWSE_AFTER_PAIR] ok={}, exit={}",
              postPairBrowse.ok(),postPairBrowse.exitCode());
            cacheCoreDeviceWifiIpE33(postPairBrowseData,udid);
            if(tryCoreDeviceRsdViaWifiTunnelE25(env,pmdExe,udid,"AFTER_PAIR")) return;
          }
        } else {
          log.info("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][USB_WLAN][COREDEVICE_REMOTEPAIRING] skipped=true, reason=NO_USB_UDID");
        }

      }

    } catch(Exception ex) {
      log.warn("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN][COREDEVICE_RUNTIME_ERROR] {}",ex.toString());
      appleTrace("E24 WLAN COREDEVICE RUNTIME ERROR",traceValue(ex.toString()));
    } finally {
      COREDEVICE_BOOTSTRAP_ACTIVE_E20.set(false);
    }
  }

  // 40k37e33: pymobiledevice3 11.16.3 kann auf Windows/iOS 17.4+ einen
  // RSD-Tunnel komplett im Userspace aufbauen. Das ist fuer GAM der bevorzugte Pfad:
  // kein TUN/TAP-Adapter, kein separater privilegierter Tunnelprozess und damit keine
  // Abhaengigkeit von der unter Windows fehlgeschlagenen Admin-Erkennung von start-tunnel.
  // Als read-only Realzugriff wird der LaunchServices-Installationsdatensatz der Apple-
  // Einstellungen-App abgefragt. Dieser Befehl benoetigt RSD und beweist damit mehr als
  // remote browse/pairing: echte CoreDevice-Daten wurden vom iPad gelesen.
  /** 40k37e33-fix8: Direkte, read-only CLI-Pruefung fuer die beiden dokumentations-/
   * suchseitig genannten IP-Pfade. Keine Fremdquelle, kein ARP, keine FRITZ!Box. */
  private void probeDirectPymobiledeviceIpE33Fix7(Map<String,String> env, Path pmdExe, String udid) {
    try {
      CommandResult lockdownHelp=runProcessE20(20,env,pmdExe.toString(),"--no-color","lockdown","-h");
      String lockdownHelpData=(lockdownHelp.stdout()+"\n"+lockdownHelp.stderr()).trim();
      boolean hasGetIp=lockdownHelp.ok() && lockdownHelpData.toLowerCase(Locale.ROOT).contains("get-ip");
      log.trace("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN][PMD3_LOCKDOWN_HELP] ok={}, exit={}, hasGetIp={}",lockdownHelp.ok(),lockdownHelp.exitCode(),hasGetIp);

      if(hasGetIp) {
        CommandResult getIpHelp=runProcessE20(20,env,pmdExe.toString(),"--no-color","lockdown","get-ip","-h");
        log.trace("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN][PMD3_LOCKDOWN_GET_IP_HELP] ok={}, exit={}, data={}",
          getIpHelp.ok(),getIpHelp.exitCode(),limitTrace((getIpHelp.stdout()+"\n"+getIpHelp.stderr()).trim()));
        CommandResult getIp=runProcessE20(30,env,pmdExe.toString(),"--no-color","lockdown","get-ip");
        String getIpData=(getIp.stdout()+"\n"+getIp.stderr()).trim();
        log.trace("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN][PMD3_LOCKDOWN_GET_IP] ok={}, exit={}, udid={}, data={}",
          getIp.ok(),getIp.exitCode(),maskUdid(udid),limitTrace(getIpData));
        if(getIp.ok()) {
          java.util.regex.Matcher m=java.util.regex.Pattern.compile("(?<![0-9.])((?:\\d{1,3}\\.){3}\\d{1,3})(?![0-9.])").matcher(getIpData);
          while(m.find()) {
            String candidate=m.group(1);
            if(isPrivateLanIpv4E33Fix5(candidate)) {
              COREDEVICE_CONFIRMED_WIFI_IP_E33.set(candidate);
              log.info("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN][IP_CONFIRMED] udid={}, ip={}, origin=PMD3_LOCKDOWN_GET_IP",maskUdid(udid),candidate);
              break;
            }
          }
        }
      }

      CommandResult usbmuxHelp=runProcessE20(20,env,pmdExe.toString(),"--no-color","usbmux","-h");
      String usbmuxHelpData=(usbmuxHelp.stdout()+"\n"+usbmuxHelp.stderr()).trim();
      boolean hasList=usbmuxHelp.ok() && usbmuxHelpData.toLowerCase(Locale.ROOT).contains("list");
      log.trace("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN][PMD3_USBMUX_HELP] ok={}, exit={}, hasList={}",usbmuxHelp.ok(),usbmuxHelp.exitCode(),hasList);
      if(hasList) {
        CommandResult listHelp=runProcessE20(20,env,pmdExe.toString(),"--no-color","usbmux","list","-h");
        log.trace("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN][PMD3_USBMUX_LIST_HELP] ok={}, exit={}, data={}",
          listHelp.ok(),listHelp.exitCode(),limitTrace((listHelp.stdout()+"\n"+listHelp.stderr()).trim()));
        CommandResult list=runProcessE20(30,env,pmdExe.toString(),"--no-color","usbmux","list");
        String listData=(list.stdout()+"\n"+list.stderr()).trim();
        log.trace("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN][PMD3_USBMUX_LIST] ok={}, exit={}, udid={}, data={}",
          list.ok(),list.exitCode(),maskUdid(udid),limitTrace(listData));

        // 40k37e53-fix5: Keine zusaetzliche IP-/MAC-Ermittlung mehr. Apple und
        // Netzwerkquellen bleiben bis zu einer bestaetigten Zusammenfuehrung strikt getrennt.
        // Vorhandene Quellwerte werden ausschliesslich an ihrer jeweiligen Ursprungsquelle gespeichert.
        // Nur uebernehmen, wenn die Ausgabe die exakte Apple-UDID UND eine private IPv4 enthaelt.
        if(list.ok() && udid!=null && !udid.isBlank() && listData.contains(udid)) {
          java.util.regex.Matcher m=java.util.regex.Pattern.compile("(?<![0-9.])((?:\\d{1,3}\\.){3}\\d{1,3})(?![0-9.])").matcher(listData);
          while(m.find()) {
            String candidate=m.group(1);
            if(isPrivateLanIpv4E33Fix5(candidate)) {
              COREDEVICE_CONFIRMED_WIFI_IP_E33.set(candidate);
              log.info("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN][IP_CONFIRMED] udid={}, ip={}, origin=PMD3_USBMUX_LIST",maskUdid(udid),candidate);
              break;
            }
          }
        }
      }
    } catch(Exception ex) {
      log.warn("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN][PMD3_DIRECT_IP_ERROR] udid={}, error={}",maskUdid(udid),ex.toString());
    }
  }

  /** 40k37e33-fix8: Diagnostiziert die vom funktionierenden Apple-CoreDevice/USERSPACE_RSD-
   * Pfad selbst gelieferten Endpoint-Metadaten. Bewusst keine Netzwerk-Fremdquelle. Der Helfer
   * gibt nur nicht-sensitive Endpoint-/peer_info-Felder aus; Zertifikate/Pairing-Secrets werden
   * nicht protokolliert. Eine IP wird erst dann produktiv uebernommen, wenn der Apple-Ursprung
   * im Test eindeutig belegt ist. */
  private void probeCoreDeviceIpFromAppleE33Fix3(Map<String,String> env, Path pmdExe, String udid, String source) {
    try {
      Path root=findGamRoot().toAbsolutePath().normalize();
      Path helper=root.resolve("tools").resolve("apple-coredevice").resolve("gam-coredevice-ip-diagnose.py").normalize();
      Path runtime=root.resolve("tools").resolve("apple-coredevice").resolve("runtime").normalize();
      Path python=runtime.resolve("tools").resolve("pymobiledevice3").resolve("Scripts").resolve("python.exe");
      if(!Files.isRegularFile(helper)) {
        log.trace("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN][COREDEVICE_IP_DIAG] skipped=true, reason=HELPER_MISSING, file={}",helper);
        return;
      }
      if(!Files.isRegularFile(python)) {
        // uv kann je nach Tool-Layout den Interpreter in einem anders benannten Tool-Verzeichnis
        // ablegen. Nur innerhalb des GAM-eigenen Runtime-Baums suchen, niemals System-Python nutzen.
        try(java.util.stream.Stream<Path> walk=Files.walk(runtime.resolve("tools"),5)) {
          python=walk.filter(Files::isRegularFile)
            .filter(x->x.getFileName().toString().equalsIgnoreCase("python.exe"))
            .filter(x->x.toString().toLowerCase(Locale.ROOT).contains("pymobiledevice3"))
            .findFirst().orElse(python);
        }
      }
      if(!Files.isRegularFile(python)) {
        log.trace("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN][COREDEVICE_IP_DIAG] skipped=true, reason=GAM_PMD3_PYTHON_MISSING, expected={}",python);
        return;
      }
      CommandResult r=runProcessE20(60,env,python.toString(),helper.toString(),udid);
      String data=(r.stdout()+"\n"+r.stderr()).trim();
      log.trace("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN][COREDEVICE_IP_DIAG] processOk={}, exit={}, udid={}, source={}, data={}",
        r.ok(),r.exitCode(),maskUdid(udid),source,limitTrace(data));
      // fix4: Die gefilterten Apple/CoreDevice-Netzwerkfelder einzeln loggen. So bleibt jede
      // Fundstelle sichtbar, auch wenn die kompakte Diagnose-JSON-Zeile vom Logger begrenzt wird.
      // fix5: Physische Socket-Gegenstellen, die pymobiledevice3 beim Aufbau genau dieses
      // Apple-Userspace-RSD-Tunnels verwendet. Nur eine echte RFC1918-IPv4-Adresse darf
      // produktiv als WLAN-IP uebernommen werden; localhost, fdxx:: und virtuelle Tunnel-
      // Endpunkte bleiben reine Diagnosewerte.
      for(String line : r.stdout().split("\\R")) {
        if(line==null || !line.startsWith("GAM_PHYSICAL_PEER\t")) continue;
        String[] parts=line.split("\t",4);
        if(parts.length<4) continue;
        String host=parts[2].trim();
        log.info("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN][COREDEVICE_PHYSICAL_PEER] udid={}, source={}, api={}, host={}, port={}",
          maskUdid(udid),source,limitTrace(parts[1]),limitTrace(host),limitTrace(parts[3]));
        if(isPrivateLanIpv4E33Fix5(host)) {
          COREDEVICE_CONFIRMED_WIFI_IP_E33.set(host);
          log.info("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN][IP_CONFIRMED] udid={}, ip={}, origin=APPLE_COREDEVICE_PHYSICAL_SOCKET",maskUdid(udid),host);
        }
      }
      // 40k37e53-fix3: Der Userspace-RSD-Prozess spricht lokal mit einem Relay. Der
      // Diagnosehelfer bindet dessen localhost-Port an den Relay-PID und liefert nur
      // private IPv4-Gegenstellen desselben Prozesses. Das ist die physische Apple-WLAN-
      // Verbindung und darf daher der exakten UDID als LAN-IP zugeordnet werden.
      for(String line : r.stdout().split("\\R")) {
        if(line==null || !line.startsWith("GAM_BOUND_LAN_PEER\t")) continue;
        String[] parts=line.split("\t",4);
        if(parts.length<4) continue;
        String host=parts[2].trim();
        log.info("[DISCOVERY][40k37e53-fix7][APPLE_MOBILE][WLAN][RELAY_LAN_PEER] udid={}, source={}, pid={}, host={}, endpoint={}",
          maskUdid(udid),source,limitTrace(parts[1]),limitTrace(host),limitTrace(parts[3]));
        if(isPrivateLanIpv4E33Fix5(host)) {
          COREDEVICE_CONFIRMED_WIFI_IP_E33.set(host);
          log.info("[DISCOVERY][40k37e53-fix3][APPLE_MOBILE][WLAN][IP_CONFIRMED] udid={}, ip={}, origin=USERSPACE_RSD_RELAY_CHAIN",maskUdid(udid),host);
        }
      }
      for(String line : r.stdout().split("\\R")) {
        if(line==null || !line.startsWith("GAM_NET_CANDIDATE\t")) continue;
        String[] parts=line.split("\t",4);
        if(parts.length<4) continue;
        log.trace("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN][COREDEVICE_NET_FIELD] udid={}, source={}, origin={}, path={}, value={}",
          maskUdid(udid),source,parts[1],limitTrace(parts[2]),limitTrace(parts[3]));
      }
      appleTrace("E33 FIX6 APPLE COREDEVICE IP DIAG","processOk="+r.ok()+", exit="+r.exitCode()+", udid="+maskUdid(udid)+", source="+source+", data="+traceValue(limitTrace(data)));
    } catch(Exception ex) {
      log.warn("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN][COREDEVICE_IP_DIAG_ERROR] udid={}, source={}, error={}",maskUdid(udid),source,ex.toString());
    }
  }

  private boolean isPrivateLanIpv4E33Fix5(String value) {
    if(value==null || !value.matches("(?:\\d{1,3}\\.){3}\\d{1,3}")) return false;
    try {
      String[] p=value.split("\\.");
      int a=Integer.parseInt(p[0]), b=Integer.parseInt(p[1]);
      for(String x:p) if(Integer.parseInt(x)<0 || Integer.parseInt(x)>255) return false;
      return a==10 || (a==172 && b>=16 && b<=31) || (a==192 && b==168);
    } catch(Exception ignored) { return false; }
  }

  private boolean tryCoreDeviceUserspaceRsdE25(Map<String,String> env, Path pmdExe, String udid, String source) {
    try {
      Map<String,String> userspaceEnv=new HashMap<>();
      if(env!=null) userspaceEnv.putAll(env);
      userspaceEnv.put("PYMOBILEDEVICE3_USERSPACE","1");
      userspaceEnv.put("PYMOBILEDEVICE3_DEFAULT_FALLBACK","userspace");
      // 40k37e51: --userspace muss die WLAN-/RemotePairing-Auswahl selbst treffen.
      // PYMobileDevice3_UDID zwingt 11.16.3 sonst in create_using_usbmux(serial=...) und
      // macht einen korrekten WLAN-only-Lauf faelschlich von USB abhaengig.
      userspaceEnv.remove("PYMOBILEDEVICE3_UDID");

      // 40k37e53-fix6: Keine weitere Discovery. Waehren genau der bereits bewaehrte
      // Userspace-RSD-Probe laeuft, beobachten wir ausschliesslich dessen eigenen
      // Prozessbaum und merken eine von ihm tatsaechlich verwendete private IPv4-
      // Gegenstelle. So wird die physische LAN-IP an der Quelle gespeichert, an der
      // GAM sie fuer die Apple-WLAN-Verbindung bereits benutzt.
      java.util.concurrent.atomic.AtomicReference<String> transportPeerE53Fix6=new java.util.concurrent.atomic.AtomicReference<>();
      CommandResult probe=runProcessE53Fix6CapturePeer(75,userspaceEnv,transportPeerE53Fix6,pmdExe.toString(),"--no-color","apps","install-record","com.apple.Preferences","--userspace");
      String data=(probe.stdout()+"\n"+probe.stderr()).trim();
      String lower=data.toLowerCase(Locale.ROOT);
      boolean semanticOk=probe.ok() && !data.isBlank()
        && !lower.contains("traceback") && !lower.contains("no device")
        && !lower.contains("error: ") && !lower.contains("no such option")
        && !lower.contains("requires admin privileges")
        && (lower.contains("com.apple.preferences") || lower.contains("bundle") || lower.contains("identifier") || lower.contains("install"));
      log.info("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN][COREDEVICE_USERSPACE_RSD_PROBE] processOk={}, semanticOk={}, exit={}, udid={}, source={}",
        probe.ok(),semanticOk,probe.exitCode(),maskUdid(udid),source);
      appleTrace("E25 FIX6 WLAN COREDEVICE USERSPACE RSD PROBE",
        "processOk="+probe.ok()+", semanticOk="+semanticOk+", exit="+probe.exitCode()+", udid="+maskUdid(udid)+", source="+source+", data="+traceValue(limitTrace(data)));
      if(semanticOk) {
        String transportPeer=blankToNull(transportPeerE53Fix6.get());
        if(isPrivateLanIpv4E33Fix5(transportPeer)) {
          COREDEVICE_CONFIRMED_WIFI_IP_E33.set(transportPeer);
          log.info("[DISCOVERY][40k37e53-fix6][APPLE_MOBILE][WLAN][CONNECTION_ENDPOINT_CAPTURED] udid={}, lanIp={}, origin=EXISTING_USERSPACE_RSD_PROCESS",maskUdid(udid),transportPeer);
        } else {
          log.info("[DISCOVERY][40k37e53-fix6][APPLE_MOBILE][WLAN][CONNECTION_ENDPOINT_CAPTURED] udid={}, lanIp=null, origin=EXISTING_USERSPACE_RSD_PROCESS",maskUdid(udid));
        }
        // 40k37e33-fix8: Der produktive USERSPACE_RSD-Zugriff ist bewiesen. Jetzt wird in
        // einem separaten, read-only Python-Helfer genau derselbe Apple-CoreDevice-Pfad
        // geoeffnet und ausschliesslich dessen Endpoint-/peer_info-Metadaten nach einer vom
        // iPad/CoreDevice selbst gelieferten Netzadresse untersucht. Keine FRITZ!Box-,
        // ARP-/Neighbor- oder sonstige Fremdquelle wird verwendet.
        probeCoreDeviceIpFromAppleE33Fix3(userspaceEnv, pmdExe, udid, source);
        // Bei DIRECT (bereits vorhandenes RemotePairing) kann der aktuelle Lauf noch keinen
        // Basis-Snapshot im Speicher haben. Solange USB ebenfalls vorhanden ist, lesen wir den
        // normalen RemotePairing-Handshake deshalb einmal ohne --pair nach. Das veraendert nichts.
        if(COREDEVICE_REMOTEPAIRING_BASIS_E27.get().isBlank()) {
          CommandResult rpInfo=runProcessE20(30,userspaceEnv,pmdExe.toString(),"--no-color","lockdown","remotepairing");
          String rpData=(rpInfo.stdout()+"\n"+rpInfo.stderr()).trim();
          log.info("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][USB_WLAN][COREDEVICE_REMOTEPAIRING_BASIS_READ] ok={}, exit={}, udid={}, source={}, data={}",rpInfo.ok(),rpInfo.exitCode(),maskUdid(udid),source,limitTrace(rpData));
          if(rpInfo.ok()) cacheRemotePairingBasisE27(rpData,udid,source+"_READ");
        }
        boolean basisOk=probeCoreDeviceBasisInventoryE27(udid,source);
        boolean extendedOk=probeCoreDeviceCapabilityInventoryE26(userspaceEnv,pmdExe,udid,source);
        compareUsbWlanParityE51Fix4(udid,source);
        COREDEVICE_SELECTED_UDID_E25.set(udid);
        log.info("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN][COREDEVICE_TRANSPORT_SELECTED] udid={}, source={}, transport=USERSPACE_RSD, basisInventoryOk={}, extendedInventoryOk={}, action=STOP_ALTERNATIVE_APPLE_PATHS",maskUdid(udid),source,basisOk,extendedOk);
        return true;
      }
      return false;
    } catch(Exception ex) {
      log.warn("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN][COREDEVICE_USERSPACE_RSD_ERROR] udid={}, source={}, error={}",maskUdid(udid),source,ex.toString());
      return false;
    }
  }

  /** 40k37e34: WLAN-Capability- und Datendiagnose fuer ein normales, nicht
   * jailbreaktes iOS/iPadOS-Geraet ohne Developer Mode. Jeder produktive Sammler wird
   * erst ausgefuehrt, wenn die Hilfe des EXAKTEN Unterbefehls --userspace bestaetigt.
   * Damit unterscheiden die Logs sauber OK / UNSUPPORTED / FAILED und bewahren jede
   * erfolgreiche Rohantwort als JSON/Text-Snapshot fuer die anschliessende Feldanalyse. */
  private boolean probeCoreDeviceCapabilityInventoryE26(Map<String,String> env, Path pmdExe, String udid, String source) {
    boolean any=false;
    any |= probeCoreDeviceCapabilityCommandE26(env,pmdExe,udid,source,"APPS",90,
      new String[]{"apps","list"}, new String[]{"apps","list","--userspace"}, "apple-mobile-coredevice-e26-apps.json");
    any |= probeCoreDeviceCapabilityCommandE26(env,pmdExe,udid,source,"BATTERY",60,
      new String[]{"diagnostics","battery","single"}, new String[]{"diagnostics","battery","single","--userspace"}, "apple-mobile-coredevice-e26-battery.json");
    any |= probeCoreDeviceCapabilityCommandE26(env,pmdExe,udid,source,"SYSTEM_STORAGE",60,
      new String[]{"diagnostics","info"}, new String[]{"diagnostics","info","--userspace"}, "apple-mobile-coredevice-e26-system-storage.json");

    // RemotePairing-Basis ist bereits sicher verfuegbar und kann weitere normale
    // DeviceProperties enthalten (z.B. Locale/Zeitzone/Modellnummer). Vollstaendig
    // sichern, aber Pairing-Secrets werden hier nicht erzeugt oder abgefragt.
    String basis=COREDEVICE_REMOTEPAIRING_BASIS_E27.get();
    boolean basisOk=basis!=null&&!basis.isBlank();
    if(basisOk) writeCoreDeviceSnapshotE28("apple-mobile-coredevice-e26-device-properties.json",basis,"COREDEVICE_E26_DEVICE_PROPERTIES_SNAPSHOT");
    log.info("[DISCOVERY][40k37e34][APPLE_MOBILE][WLAN][COREDEVICE_WLAN_CAPABILITY] BASIS={}, APPS/BATTERY/SYSTEM_STORAGE siehe Einzelmarker, udid={}, source={}, developerModeRequired=false",
      basisOk?"OK":"UNAVAILABLE",maskUdid(udid),source);
    log.info("[DISCOVERY][40k37e34][APPLE_MOBILE][WLAN][COREDEVICE_WLAN_CAPABILITY_SUMMARY] anyExtendedOk={}, basisOk={}, udid={}, source={}",any,basisOk,maskUdid(udid),source);
    return any||basisOk;
  }

  private boolean probeCoreDeviceCapabilityCommandE26(Map<String,String> env, Path pmdExe, String udid, String source,
      String capability, int timeoutSeconds, String[] helpPath, String[] runPath, String snapshotName) {
    try {
      java.util.List<String> helpArgs=new java.util.ArrayList<>();
      helpArgs.add(pmdExe.toString()); helpArgs.add("--no-color"); helpArgs.addAll(java.util.Arrays.asList(helpPath)); helpArgs.add("-h");
      CommandResult help=runProcessE20(25,env,helpArgs.toArray(String[]::new));
      String helpData=(help.stdout()+"\n"+help.stderr()).trim();
      boolean userspaceSupported=help.ok()&&helpData.contains("--userspace");
      if(!userspaceSupported) {
        log.info("[DISCOVERY][40k37e34][APPLE_MOBILE][WLAN][COREDEVICE_WLAN_CAPABILITY] capability={}, status=UNSUPPORTED, reason=HELP_HAS_NO_USERSPACE, exit={}, udid={}, source={}",
          capability,help.exitCode(),maskUdid(udid),source);
        return false;
      }
      java.util.List<String> args=new java.util.ArrayList<>();
      args.add(pmdExe.toString()); args.add("--no-color"); args.addAll(java.util.Arrays.asList(runPath));
      CommandResult result=runProcessE20(timeoutSeconds,env,args.toArray(String[]::new));
      String data=(result.stdout()+"\n"+result.stderr()).trim();
      boolean semanticOk=semanticReadOnlyJsonE28(result,data);
      String status=semanticOk?"OK":"FAILED";
      log.info("[DISCOVERY][40k37e34][APPLE_MOBILE][WLAN][COREDEVICE_WLAN_CAPABILITY] capability={}, status={}, processOk={}, exit={}, udid={}, source={}",
        capability,status,result.ok(),result.exitCode(),maskUdid(udid),source);
      log.trace("[DISCOVERY][40k37e54-fix24][APPLE_MOBILE][WLAN][COREDEVICE_WLAN_CAPABILITY_DATA] capability={}, udid={}, data={}",capability,maskUdid(udid),limitTrace(data));
      if(semanticOk) writeCoreDeviceSnapshotE28(snapshotName,data,"COREDEVICE_E26_"+capability+"_SNAPSHOT");
      return semanticOk;
    } catch(Exception ex) {
      log.warn("[DISCOVERY][40k37e34][APPLE_MOBILE][WLAN][COREDEVICE_WLAN_CAPABILITY] capability={}, status=FAILED, udid={}, source={}, error={}",
        capability,maskUdid(udid),source,ex.toString());
      return false;
    }
  }

  // 40k37e33: Erster produktiver Ausbau hinter dem bereits bewiesenen USERSPACE_RSD-Transport.
  // Die Befehle werden nur ausgefuehrt, wenn die jeweilige pymobiledevice3-Hilfe den
  // Userspace-Schalter fuer genau diesen Unterbefehl bestaetigt. So wird keine CLI-Syntax
  // geraten. Fehler einzelner Sammler sind nicht fatal; der funktionierende Transport bleibt aktiv.
  /** 40k37e33: Produktive, read-only CoreDevice-Sammler.
   * Die GAM-eigene Runtime ist auf pymobiledevice3 11.16.3 gepinnt; deshalb werden die
   * bereits verifizierten --userspace-Befehle direkt ausgefuehrt. Ein einzelner Sammlerfehler
   * ist nicht fatal und beeinflusst den bestaetigten Transport nicht. */
  private boolean probeCoreDeviceExtendedInventoryE28(Map<String,String> env, Path pmdExe, String udid, String source) {
    boolean any=false;
    try {
      CommandResult apps=runProcessE20(90,env,pmdExe.toString(),"--no-color","apps","list","--userspace");
      String appsData=(apps.stdout()+"\n"+apps.stderr()).trim();
      boolean appsOk=semanticReadOnlyJsonE28(apps,appsData);
      log.info("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN][COREDEVICE_APPS_INVENTORY] semanticOk={}, exit={}, udid={}, source={}",appsOk,apps.exitCode(),maskUdid(udid),source);
      if(appsOk){ writeCoreDeviceSnapshotE28("apple-mobile-coredevice-apps.json",appsData,"COREDEVICE_APPS_SNAPSHOT"); any=true; }

      CommandResult battery=runProcessE20(60,env,pmdExe.toString(),"--no-color","diagnostics","battery","single","--userspace");
      String batteryData=(battery.stdout()+"\n"+battery.stderr()).trim();
      boolean batteryOk=semanticReadOnlyJsonE28(battery,batteryData);
      log.info("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN][COREDEVICE_BATTERY_INVENTORY] semanticOk={}, exit={}, udid={}, source={}",batteryOk,battery.exitCode(),maskUdid(udid),source);
      if(batteryOk){ writeCoreDeviceSnapshotE28("apple-mobile-coredevice-battery.json",batteryData,"COREDEVICE_BATTERY_SNAPSHOT"); any=true; }

      CommandResult info=runProcessE20(60,env,pmdExe.toString(),"--no-color","diagnostics","info","--userspace");
      String infoData=(info.stdout()+"\n"+info.stderr()).trim();
      boolean infoOk=semanticReadOnlyJsonE28(info,infoData);
      log.info("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN][COREDEVICE_SYSTEM_INVENTORY] semanticOk={}, exit={}, udid={}, source={}",infoOk,info.exitCode(),maskUdid(udid),source);
      if(infoOk){ writeCoreDeviceSnapshotE28("apple-mobile-coredevice-system.json",infoData,"COREDEVICE_SYSTEM_SNAPSHOT"); any=true; }

      log.info("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN][COREDEVICE_EXTENDED_INVENTORY] semanticOk={}, apps={}, battery={}, system={}, udid={}, source={}",any,appsOk,batteryOk,infoOk,maskUdid(udid),source);
      return any;
    } catch(Exception ex) {
      log.warn("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN][COREDEVICE_EXTENDED_INVENTORY_ERROR] udid={}, source={}, error={}",maskUdid(udid),source,ex.toString());
      return any;
    }
  }

  private boolean semanticReadOnlyJsonE28(CommandResult r, String data) {
    if(r==null || !r.ok() || data==null || data.isBlank()) return false;
    String lower=data.toLowerCase(Locale.ROOT);
    if(lower.contains("traceback") || lower.contains("no device") || lower.contains("error: ") || lower.contains("no such option") || lower.contains("requires admin privileges")) return false;
    try { new ObjectMapper().readTree(data); return true; } catch(Exception ignored) { return data.startsWith("[") || data.startsWith("{"); }
  }

  /** 40k37e51-fix5: Erfasst die USB-Referenz direkt im physischen USB-Discovery-Zweig. */
  private void persistFullUsbReferenceE51Fix4(AppleDevice device) {
    if(device==null || device.udid()==null || device.udid().isBlank()) return;
    String udid=device.udid();
    try {
      log.info("[DISCOVERY][40k37e54][APPLE_MOBILE][PARITY][USB_CAPTURE] udid={}, action=START_FULL_REFERENCE",maskUdid(udid));
      InventoryProbeResult basis=probeInventory(udid);
      ExtendedInventoryProbeResult extended=probeExtendedInventory(udid);
      SoftwareProbeResult software=probeSoftware(udid);
      if(basis!=null && basis.success()) {
        writeUsbParityReferenceE51Fix4(udid,basis,extended,software);
      } else {
        log.warn("[DISCOVERY][40k37e54][APPLE_MOBILE][PARITY][USB_CAPTURE] udid={}, result=FAILED, reason=BASIS_NOT_AVAILABLE",maskUdid(udid));
      }
    } catch(Exception ex) {
      log.warn("[DISCOVERY][40k37e51-fix5][APPLE_MOBILE][PARITY][USB_CAPTURE_ERROR] udid={}, error={}",maskUdid(udid),ex.toString());
    }
  }

  private String paritySafeUdidE51Fix4(String udid) {
    return udid==null?"unknown":udid.replaceAll("[^A-Za-z0-9._-]","_");
  }

  private Path usbReferencePathE51Fix4(String udid) {
    return findGamRoot().resolve("logs").resolve("apple-mobile-usb-reference-"+paritySafeUdidE51Fix4(udid)+".json");
  }

  private Path wlanReferencePathE51Fix4(String udid) {
    return findGamRoot().resolve("logs").resolve("apple-mobile-wlan-reference-"+paritySafeUdidE51Fix4(udid)+".json");
  }

  private boolean hasUsbReferenceE51Fix4(String udid) {
    try { return Files.isRegularFile(usbReferencePathE51Fix4(udid)); } catch(Exception ex) { return false; }
  }

  private void writeUsbParityReferenceE51Fix4(String udid, InventoryProbeResult basis, ExtendedInventoryProbeResult extended, SoftwareProbeResult software) throws Exception {
    LinkedHashMap<String,Object> root=new LinkedHashMap<>();
    root.put("generatedAt",OffsetDateTime.now().toString()); root.put("transport","USB"); root.put("udid",udid);
    root.put("default",basis==null?Map.of():basis.defaultFields()); root.put("domains",basis==null?Map.of():basis.domains());
    root.put("extended",extended==null?Map.of():extended.sections());
    List<Map<String,String>> apps=new ArrayList<>();
    if(software!=null && software.items()!=null) for(SoftwareItem x:software.items()) {
      LinkedHashMap<String,String> a=new LinkedHashMap<>(); a.put("scope",firstNonBlank(x.scopeType(),"")); a.put("name",firstNonBlank(x.name(),""));
      a.put("version",firstNonBlank(x.version(),"")); a.put("bundleId",firstNonBlank(x.bundleId(),"")); apps.add(a);
    }
    root.put("apps",apps);
    Path file=usbReferencePathE51Fix4(udid); Files.createDirectories(file.getParent()); writeJsonAtomicE3(file,root);
    // Alias fuer Diagnose/Kompatibilitaet, ohne die UDID-spezifische Referenz zu ersetzen.
    writeJsonAtomicE3(file.getParent().resolve("apple-mobile-usb-reference-latest.json"),root);
    log.info("[DISCOVERY][40k37e54][APPLE_MOBILE][PARITY][USB_REFERENCE_SAVED] udid={}, defaultFields={}, domains={}, extendedSections={}, apps={}, file={}",maskUdid(udid),basis==null?0:basis.defaultFields().size(),basis==null?0:basis.domains().size(),extended==null?0:extended.sections().size(),apps.size(),file.toAbsolutePath());
  }

  private void compareUsbWlanParityE51Fix4(String udid,String source) {
    try {
      Path usbFile=usbReferencePathE51Fix4(udid);
      if(!Files.isRegularFile(usbFile)) {
        log.info("[DISCOVERY][40k37e54][APPLE_MOBILE][PARITY] state=NO_USB_REFERENCE, udid={}, source={}",maskUdid(udid),source); return;
      }
      Path dir=findGamRoot().resolve("logs"); ObjectMapper om=new ObjectMapper(); JsonNode usb=om.readTree(Files.readString(usbFile,StandardCharsets.UTF_8));
      Set<String> usbBasis=new TreeSet<>(),usbExtended=new TreeSet<>(),usbApps=new TreeSet<>();
      collectJsonLeafNamesE51Fix3(usb.path("default"),usbBasis); collectJsonLeafNamesE51Fix3(usb.path("domains"),usbBasis); collectJsonLeafNamesE51Fix3(usb.path("extended"),usbExtended);
      if(usb.path("apps").isArray()) for(JsonNode a:usb.path("apps")){String b=a.path("bundleId").asText("");if(!b.isBlank())usbApps.add(b.toLowerCase(Locale.ROOT));}
      Set<String> wlanBasis=readJsonLeafNamesE51Fix3(dir.resolve("apple-mobile-coredevice-e26-device-properties.json"));
      if(wlanBasis.isEmpty()) wlanBasis=readJsonLeafNamesE51Fix3(dir.resolve("apple-mobile-coredevice-e27-basis.json"));
      Set<String> wlanApps=readBundleIdsE51Fix3(dir.resolve("apple-mobile-coredevice-e26-apps.json")); if(wlanApps.isEmpty()) wlanApps=readBundleIdsE51Fix3(dir.resolve("apple-mobile-coredevice-apps.json"));
      Set<String> wlanExtended=new TreeSet<>(); wlanExtended.addAll(readJsonLeafNamesE51Fix3(dir.resolve("apple-mobile-coredevice-e26-battery.json"))); wlanExtended.addAll(readJsonLeafNamesE51Fix3(dir.resolve("apple-mobile-coredevice-e26-system-storage.json")));
      if(wlanExtended.isEmpty()){wlanExtended.addAll(readJsonLeafNamesE51Fix3(dir.resolve("apple-mobile-coredevice-battery.json")));wlanExtended.addAll(readJsonLeafNamesE51Fix3(dir.resolve("apple-mobile-coredevice-system.json")));}
      LinkedHashMap<String,Object> wlanRaw=new LinkedHashMap<>(); wlanRaw.put("generatedAt",OffsetDateTime.now().toString()); wlanRaw.put("transport","WLAN_USERSPACE_RSD"); wlanRaw.put("udid",udid); wlanRaw.put("source",source);
      wlanRaw.put("basisKeys",wlanBasis); wlanRaw.put("appBundleIds",wlanApps); wlanRaw.put("extendedKeys",wlanExtended); Path wlanFile=wlanReferencePathE51Fix4(udid); writeJsonAtomicE3(wlanFile,wlanRaw); writeJsonAtomicE3(dir.resolve("apple-mobile-wlan-reference-latest.json"),wlanRaw);
      logParityCategoryE51Fix5("BASIS",usbBasis,wlanBasis,udid,source); logParityCategoryE51Fix5("APPS",usbApps,wlanApps,udid,source); logParityCategoryE51Fix5("BATTERY_SYSTEM_STORAGE",usbExtended,wlanExtended,udid,source);
      LinkedHashMap<String,Object> report=new LinkedHashMap<>(); report.put("generatedAt",OffsetDateTime.now().toString());report.put("udid",udid);report.put("source",source);report.put("usbReference",usbFile.toString());report.put("wlanReference",wlanFile.toString());
      report.put("basis",parityMapE51Fix5(usbBasis,wlanBasis));report.put("apps",parityMapE51Fix5(usbApps,wlanApps));report.put("batterySystemStorage",parityMapE51Fix5(usbExtended,wlanExtended));
      LinkedHashMap<String,Object> directions=new LinkedHashMap<>();
      directions.put("usbOnly",directionMapE51Fix5(usbBasis,wlanBasis,usbApps,wlanApps,usbExtended,wlanExtended,true));
      directions.put("wlanOnly",directionMapE51Fix5(usbBasis,wlanBasis,usbApps,wlanApps,usbExtended,wlanExtended,false));
      report.put("directionalDifferences",directions);
      Path candidates=dir.resolve("apple-mobile-usb-wlan-parity-candidates-"+paritySafeUdidE51Fix4(udid)+".json");writeJsonAtomicE3(candidates,directions);writeJsonAtomicE3(dir.resolve("apple-mobile-usb-wlan-parity-candidates-latest.json"),directions);
      Path out=dir.resolve("apple-mobile-usb-wlan-parity-"+paritySafeUdidE51Fix4(udid)+".json");writeJsonAtomicE3(out,report);writeJsonAtomicE3(dir.resolve("apple-mobile-usb-wlan-parity-latest.json"),report);
      log.info("[DISCOVERY][40k37e54][APPLE_MOBILE][PARITY][DIRECTIONAL] udid={}, USB_ONLY={}, WLAN_ONLY={}, candidates={}",maskUdid(udid),countDirectionalE51Fix5(directions,"usbOnly"),countDirectionalE51Fix5(directions,"wlanOnly"),candidates.toAbsolutePath());
      int usbOnly=countDirectionalE51Fix5(directions,"usbOnly"), wlanOnly=countDirectionalE51Fix5(directions,"wlanOnly");
      log.info("[DISCOVERY][40k37e54][APPLE_MOBILE][PARITY][SUMMARY] udid={}, semanticParity={}, usbOnly={}, wlanOnly={}, note=TRANSPORT_SPECIFIC_DIFFERENCES_REMAIN_VISIBLE",maskUdid(udid),(usbOnly==0&&wlanOnly==0),usbOnly,wlanOnly);
      log.info("[DISCOVERY][40k37e54][APPLE_MOBILE][PARITY][REPORT] udid={}, usbReference={}, wlanReference={}, report={}",maskUdid(udid),usbFile.toAbsolutePath(),wlanFile.toAbsolutePath(),out.toAbsolutePath());
    } catch(Exception ex) { log.warn("[DISCOVERY][40k37e54][APPLE_MOBILE][PARITY][ERROR] udid={}, error={}",maskUdid(udid),ex.toString()); }
  }

  /** 40k37e51-fix3: Persistiert den USB-Datenumfang desselben Apple-Geraets als reine
   * Vergleichsreferenz. Pairing-Secrets werden weder gelesen noch gespeichert. */
  private void writeUsbParityReferenceE51Fix3(String udid, InventoryProbeResult basis,
      ExtendedInventoryProbeResult extended, SoftwareProbeResult software) {
    try {
      LinkedHashMap<String,Object> root=new LinkedHashMap<>();
      root.put("generatedAt",OffsetDateTime.now().toString());
      root.put("udid",udid);
      root.put("default",basis==null?Map.of():basis.defaultFields());
      root.put("domains",basis==null?Map.of():basis.domains());
      root.put("extended",extended==null?Map.of():extended.sections());
      List<Map<String,String>> apps=new ArrayList<>();
      if(software!=null && software.items()!=null) for(SoftwareItem x:software.items()) {
        LinkedHashMap<String,String> a=new LinkedHashMap<>();
        a.put("scope",firstNonBlank(x.scopeType(),"")); a.put("name",firstNonBlank(x.name(),""));
        a.put("version",firstNonBlank(x.version(),"")); a.put("bundleId",firstNonBlank(x.bundleId(),""));
        apps.add(a);
      }
      root.put("apps",apps);
      Path dir=findGamRoot().resolve("logs"); Files.createDirectories(dir);
      Path file=dir.resolve("apple-mobile-usb-parity-reference-e51fix3.json");
      writeJsonAtomicE3(file,root);
      log.info("[DISCOVERY][40k37e51-fix3][APPLE_MOBILE][PARITY][USB_REFERENCE] udid={}, defaultFields={}, domains={}, extendedSections={}, apps={}, file={}",
        maskUdid(udid),basis==null?0:basis.defaultFields().size(),basis==null?0:basis.domains().size(),
        extended==null?0:extended.sections().size(),apps.size(),file.toAbsolutePath());
    } catch(Exception ex) {
      log.warn("[DISCOVERY][40k37e51-fix3][APPLE_MOBILE][PARITY][USB_REFERENCE_ERROR] udid={}, error={}",maskUdid(udid),ex.toString());
    }
  }

  /** Vergleicht nach einem erfolgreichen WLAN-Lauf den aktuell erreichbaren RSD-Datenumfang
   * mit der letzten USB-Referenz desselben UDID. Fehlende Felder werden nur diagnostiziert;
   * es werden niemals Werte erfunden oder aus anderen Quellen substituiert. */
  private void compareUsbWlanParityE51Fix3(String udid,String source) {
    try {
      Path dir=findGamRoot().resolve("logs");
      Path ref=dir.resolve("apple-mobile-usb-parity-reference-e51fix3.json");
      if(!Files.isRegularFile(ref)) {
        log.info("[DISCOVERY][40k37e51-fix3][APPLE_MOBILE][PARITY] state=NO_USB_REFERENCE, udid={}, source={}, hint=EINMAL_USB_INVENTARISIEREN_DANN_WLAN_TESTEN",maskUdid(udid),source);
        return;
      }
      ObjectMapper om=new ObjectMapper(); JsonNode usb=om.readTree(Files.readString(ref,StandardCharsets.UTF_8));
      String refUdid=usb.path("udid").asText("");
      if(!refUdid.equalsIgnoreCase(udid)) {
        log.info("[DISCOVERY][40k37e51-fix3][APPLE_MOBILE][PARITY] state=USB_REFERENCE_UDID_MISMATCH, expected={}, reference={}",maskUdid(udid),maskUdid(refUdid)); return;
      }
      Set<String> usbBasis=new TreeSet<>(), usbExtended=new TreeSet<>(), usbApps=new TreeSet<>();
      collectJsonLeafNamesE51Fix3(usb.path("default"),usbBasis); collectJsonLeafNamesE51Fix3(usb.path("domains"),usbBasis);
      collectJsonLeafNamesE51Fix3(usb.path("extended"),usbExtended);
      if(usb.path("apps").isArray()) for(JsonNode a:usb.path("apps")){ String b=a.path("bundleId").asText(""); if(!b.isBlank()) usbApps.add(b.toLowerCase(Locale.ROOT)); }
      Set<String> wlanBasis=readJsonLeafNamesE51Fix3(dir.resolve("apple-mobile-coredevice-e26-device-properties.json"));
      if(wlanBasis.isEmpty()) wlanBasis=readJsonLeafNamesE51Fix3(dir.resolve("apple-mobile-coredevice-e27-basis.json"));
      Set<String> wlanApps=readBundleIdsE51Fix3(dir.resolve("apple-mobile-coredevice-e26-apps.json"));
      if(wlanApps.isEmpty()) wlanApps=readBundleIdsE51Fix3(dir.resolve("apple-mobile-coredevice-apps.json"));
      Set<String> wlanExtended=new TreeSet<>();
      wlanExtended.addAll(readJsonLeafNamesE51Fix3(dir.resolve("apple-mobile-coredevice-e26-battery.json")));
      wlanExtended.addAll(readJsonLeafNamesE51Fix3(dir.resolve("apple-mobile-coredevice-e26-system-storage.json")));
      if(wlanExtended.isEmpty()){ wlanExtended.addAll(readJsonLeafNamesE51Fix3(dir.resolve("apple-mobile-coredevice-battery.json"))); wlanExtended.addAll(readJsonLeafNamesE51Fix3(dir.resolve("apple-mobile-coredevice-system.json"))); }
      logParityCategoryE51Fix3("BASIS",usbBasis,wlanBasis,udid,source);
      logParityCategoryE51Fix3("APPS",usbApps,wlanApps,udid,source);
      logParityCategoryE51Fix3("BATTERY_SYSTEM_STORAGE",usbExtended,wlanExtended,udid,source);
      LinkedHashMap<String,Object> report=new LinkedHashMap<>(); report.put("generatedAt",OffsetDateTime.now().toString()); report.put("udid",udid); report.put("source",source);
      report.put("basis",parityMapE51Fix3(usbBasis,wlanBasis)); report.put("apps",parityMapE51Fix3(usbApps,wlanApps)); report.put("batterySystemStorage",parityMapE51Fix3(usbExtended,wlanExtended));
      Path out=dir.resolve("apple-mobile-usb-wlan-parity-e51fix3.json"); writeJsonAtomicE3(out,report);
      log.info("[DISCOVERY][40k37e51-fix3][APPLE_MOBILE][PARITY][REPORT] udid={}, file={}",maskUdid(udid),out.toAbsolutePath());
    } catch(Exception ex) { log.warn("[DISCOVERY][40k37e51-fix3][APPLE_MOBILE][PARITY][ERROR] udid={}, error={}",maskUdid(udid),ex.toString()); }
  }

  private void collectJsonLeafNamesE51Fix3(JsonNode n,Set<String> out){
    if(n==null||n.isMissingNode()||n.isNull()) return;
    if(n.isObject()){ n.fields().forEachRemaining(e->{ if(e.getValue().isValueNode()) out.add(e.getKey().toLowerCase(Locale.ROOT)); else collectJsonLeafNamesE51Fix3(e.getValue(),out); }); }
    else if(n.isArray()) for(JsonNode x:n) collectJsonLeafNamesE51Fix3(x,out);
  }
  private Set<String> readJsonLeafNamesE51Fix3(Path p){ try{ if(!Files.isRegularFile(p)) return new TreeSet<>(); Set<String>s=new TreeSet<>(); collectJsonLeafNamesE51Fix3(new ObjectMapper().readTree(Files.readString(p,StandardCharsets.UTF_8)),s); return s;}catch(Exception e){return new TreeSet<>();} }
  private Set<String> readBundleIdsE51Fix3(Path p){
    Set<String> out=new TreeSet<>(); try{ if(!Files.isRegularFile(p)) return out; JsonNode n=new ObjectMapper().readTree(Files.readString(p,StandardCharsets.UTF_8)); collectBundleIdsE51Fix3(n,out);}catch(Exception ignored){} return out;
  }
  private void collectBundleIdsE51Fix3(JsonNode n,Set<String> out){ if(n==null)return; if(n.isObject()){ n.fields().forEachRemaining(e->{ String k=e.getKey().toLowerCase(Locale.ROOT); if((k.equals("cfbundleidentifier")||k.equals("bundleidentifier")||k.equals("bundleid")||k.equals("identifier"))&&e.getValue().isTextual()){String v=e.getValue().asText().trim();if(v.contains("."))out.add(v.toLowerCase(Locale.ROOT));} collectBundleIdsE51Fix3(e.getValue(),out);}); } else if(n.isArray()) for(JsonNode x:n) collectBundleIdsE51Fix3(x,out); }
  private void logParityCategoryE51Fix5(String category,Set<String> usb,Set<String> wlan,String udid,String source){Set<String>uo=new TreeSet<>(usb);uo.removeAll(wlan);Set<String>wo=new TreeSet<>(wlan);wo.removeAll(usb);log.info("[DISCOVERY][40k37e54][APPLE_MOBILE][PARITY][{}] udid={}, source={}, USB_PRESENT={}, WLAN_PRESENT={}, USB_ONLY={}, WLAN_ONLY={}",category,maskUdid(udid),source,usb.size(),wlan.size(),uo.size(),wo.size());log.trace("[DISCOVERY][40k37e54-fix24][APPLE_MOBILE][PARITY][{}_DETAIL] udid={}, missingInWlan={}, missingInUsb={}",category,maskUdid(udid),limitTrace(String.join(",",uo)),limitTrace(String.join(",",wo)));}

  /** 40k37e51-fix5: Paritaet ist strikt bidirektional: USB-only UND WLAN-only. */
  private Map<String,Object> parityMapE51Fix5(Set<String> usb,Set<String> wlan){
    Set<String> usbOnly=new TreeSet<>(usb);usbOnly.removeAll(wlan);
    Set<String> wlanOnly=new TreeSet<>(wlan);wlanOnly.removeAll(usb);
    Set<String> common=new TreeSet<>(usb);common.retainAll(wlan);
    LinkedHashMap<String,Object>m=new LinkedHashMap<>();
    m.put("usbCount",usb.size());m.put("wlanCount",wlan.size());m.put("commonCount",common.size());
    m.put("missingInWlan",usbOnly);m.put("missingInUsb",wlanOnly);
    m.put("usbOnly",usbOnly);m.put("wlanOnly",wlanOnly);m.put("common",common);return m;
  }
  private Map<String,Object> directionMapE51Fix5(Set<String> ub,Set<String> wb,Set<String> ua,Set<String> wa,Set<String> ue,Set<String> we,boolean usbOnly){
    LinkedHashMap<String,Object> out=new LinkedHashMap<>();
    out.put("basis",directionSetE51Fix5(ub,wb,usbOnly));out.put("apps",directionSetE51Fix5(ua,wa,usbOnly));out.put("batterySystemStorage",directionSetE51Fix5(ue,we,usbOnly));return out;
  }
  private Set<String> directionSetE51Fix5(Set<String> usb,Set<String> wlan,boolean usbOnly){Set<String>x=new TreeSet<>(usbOnly?usb:wlan);x.removeAll(usbOnly?wlan:usb);return x;}
  private int countDirectionalE51Fix5(Map<String,Object> directions,String key){Object o=directions.get(key);if(!(o instanceof Map<?,?> m))return 0;int n=0;for(Object v:m.values())if(v instanceof Collection<?> c)n+=c.size();return n;}

  private Map<String,Object> parityMapE51Fix3(Set<String> usb,Set<String> wlan){ Set<String> missing=new TreeSet<>(usb);missing.removeAll(wlan);Set<String> extra=new TreeSet<>(wlan);extra.removeAll(usb);LinkedHashMap<String,Object>m=new LinkedHashMap<>();m.put("usbCount",usb.size());m.put("wlanCount",wlan.size());m.put("missingInWlan",missing);m.put("wlanExtra",extra);return m;}
  private void logParityCategoryE51Fix3(String category,Set<String> usb,Set<String> wlan,String udid,String source){ Set<String> missing=new TreeSet<>(usb);missing.removeAll(wlan);Set<String> extra=new TreeSet<>(wlan);extra.removeAll(usb);log.info("[DISCOVERY][40k37e51-fix3][APPLE_MOBILE][PARITY][{}] udid={}, source={}, USB_PRESENT={}, WLAN_PRESENT={}, WLAN_MISSING={}, WLAN_EXTRA={}",category,maskUdid(udid),source,usb.size(),wlan.size(),missing.size(),extra.size()); }

  private void writeCoreDeviceSnapshotE28(String filename, String data, String marker) {
    try {
      Path logDir=findGamRoot().resolve("logs");
      Files.createDirectories(logDir);
      Path snapshot=logDir.resolve(filename);
      Files.writeString(snapshot,data,StandardCharsets.UTF_8,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING,StandardOpenOption.WRITE);
      log.info("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN][{}] file={}",marker,snapshot.toAbsolutePath());
    } catch(Exception ex) {
      log.warn("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN][COREDEVICE_SNAPSHOT_ERROR] file={}, error={}",filename,ex.toString());
    }
  }

  // 40k37e33: Basisinventarisierung ohne Developer-Dienste. Die Daten stammen direkt
  // aus peerDeviceInfo.deviceKVSData des normalen RemotePairing-Handshakes. Damit sind
  // weder Developer Mode noch DeveloperDiskImage/PersonalizedImage Voraussetzung.
  private void cacheRemotePairingBasisE27(String raw, String udid, String source) {
    if(raw==null || raw.isBlank()) return;
    try {
      JsonNode root=new ObjectMapper().readTree(raw);
      JsonNode peer=root.path("peerDeviceInfo");
      JsonNode kvs=peer.path("deviceKVSData");
      JsonNode basis=kvs.has("NULL") ? kvs.path("NULL") : kvs;
      if(basis==null || basis.isMissingNode() || !basis.isObject()) return;
      String json=new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(basis);
      COREDEVICE_REMOTEPAIRING_BASIS_E27.set(json);
      log.info("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN][COREDEVICE_REMOTEPAIRING_BASIS_CAPTURED] udid={}, source={}, deviceName={}, deviceClass={}, productVersion={}, buildVersion={}, hardwareModel={}, cpuArchitecture={}",
        maskUdid(udid),source,jsonText(basis,"DeviceName"),jsonText(basis,"DeviceClass"),
        firstNonBlank(jsonText(basis,"HumanReadableProductVersionString"),jsonText(basis,"ProductVersion")),
        jsonText(basis,"BuildVersion"),firstNonBlank(jsonText(basis,"HardwareModel"),jsonText(basis,"HWModelStr")),jsonText(basis,"CPUArchitecture"));
    } catch(Exception ex) {
      log.warn("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN][COREDEVICE_REMOTEPAIRING_BASIS_CAPTURE_ERROR] udid={}, source={}, error={}",maskUdid(udid),source,ex.toString());
    }
  }

  private boolean probeCoreDeviceBasisInventoryE27(String udid, String source) {
    String data=COREDEVICE_REMOTEPAIRING_BASIS_E27.get();
    if(data==null || data.isBlank()) {
      log.info("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN][COREDEVICE_BASIS_INVENTORY] semanticOk=false, udid={}, source={}, reason=NO_REMOTEPAIRING_BASIS_CACHE",maskUdid(udid),source);
      return false;
    }
    try {
      JsonNode basis=new ObjectMapper().readTree(data);
      String deviceName=jsonText(basis,"DeviceName");
      String deviceClass=jsonText(basis,"DeviceClass");
      String productVersion=firstNonBlank(jsonText(basis,"HumanReadableProductVersionString"),jsonText(basis,"ProductVersion"));
      String buildVersion=jsonText(basis,"BuildVersion");
      String hardwareModel=firstNonBlank(jsonText(basis,"HardwareModel"),jsonText(basis,"HWModelStr"));
      String cpu=jsonText(basis,"CPUArchitecture");
      boolean semanticOk=!deviceClass.isBlank() && (!productVersion.isBlank() || !buildVersion.isBlank() || !hardwareModel.isBlank());
      log.info("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN][COREDEVICE_BASIS_INVENTORY] semanticOk={}, udid={}, source={}, deviceName={}, deviceClass={}, productVersion={}, buildVersion={}, hardwareModel={}, cpuArchitecture={}, developerModeRequired=false",
        semanticOk,maskUdid(udid),source,deviceName,deviceClass,productVersion,buildVersion,hardwareModel,cpu);
      appleTrace("E27 WLAN COREDEVICE BASIS INVENTORY","semanticOk="+semanticOk+", udid="+maskUdid(udid)+", source="+source+", deviceName="+traceValue(deviceName)+", deviceClass="+traceValue(deviceClass)+", productVersion="+traceValue(productVersion)+", buildVersion="+traceValue(buildVersion)+", hardwareModel="+traceValue(hardwareModel)+", cpu="+traceValue(cpu));
      if(semanticOk) {
        try {
          Path logDir=findGamRoot().resolve("logs");
          Files.createDirectories(logDir);
          Path snapshot=logDir.resolve("apple-mobile-coredevice-e27-basis.json");
          Files.writeString(snapshot,data,StandardCharsets.UTF_8,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING,StandardOpenOption.WRITE);
          log.info("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN][COREDEVICE_BASIS_SNAPSHOT] file={}",snapshot.toAbsolutePath());
        } catch(Exception snapshotError) {
          log.warn("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN][COREDEVICE_BASIS_SNAPSHOT_ERROR] {}",snapshotError.toString());
        }
      }
      return semanticOk;
    } catch(Exception ex) {
      log.warn("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN][COREDEVICE_BASIS_INVENTORY_ERROR] udid={}, source={}, error={}",maskUdid(udid),source,ex.toString());
      return false;
    }
  }

  // 40k37e25-fix2: rsd-info akzeptiert in pymobiledevice3 11.16.3 keine --udid-Option.
  // Deshalb wird zuerst ein echter, lang laufender WLAN-Tunnel fuer die bekannte UDID
  // gestartet. --script-mode liefert den RSD-Endpunkt; solange der Tunnelprozess lebt,
  // wird exakt dieser Endpunkt mit `remote rsd-info --rsd HOST PORT` abgefragt.
  private boolean tryCoreDeviceRsdViaWifiTunnelE25(Map<String,String> env, Path pmdExe, String udid, String source) {
    // fix4: zuerst der aktuelle, nicht privilegierte pymobiledevice3-Userspace-RSD-Pfad.
    // Der klassische start-tunnel-Pfad bleibt nur als Diagnose/Fallback erhalten.
    if(tryCoreDeviceUserspaceRsdE25(env,pmdExe,udid,source)) return true;
    Process tunnel=null;
    ExecutorService io=Executors.newFixedThreadPool(2);
    try {
      ProcessBuilder pb=new ProcessBuilder(pmdExe.toString(),"--no-color","remote","start-tunnel","-t","wifi","--udid",udid,"--script-mode");
      pb.redirectErrorStream(false);
      if(env!=null) pb.environment().putAll(env);
      tunnel=pb.start();
      final Process tp=tunnel;
      BufferedReader stdout=new BufferedReader(new InputStreamReader(tp.getInputStream(),StandardCharsets.UTF_8));
      BufferedReader stderr=new BufferedReader(new InputStreamReader(tp.getErrorStream(),StandardCharsets.UTF_8));
      Future<String> outLine=io.submit(stdout::readLine);
      Future<String> errLine=io.submit(stderr::readLine);
      log.info("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN][COREDEVICE_WIFI_TUNNEL_START_FALLBACK] udid={}, pid={}, source={}",maskUdid(udid),tp.pid(),source);

      String firstOut="";
      String firstErr="";
      long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(30);
      while(System.nanoTime()<deadline && firstOut.isBlank() && tp.isAlive()) {
        try { String line=outLine.get(500,TimeUnit.MILLISECONDS); if(line!=null) firstOut=line.trim(); }
        catch(TimeoutException ignore) { }
        if(firstOut.isBlank() && errLine.isDone()) {
          try { String line=errLine.get(); if(line!=null) firstErr=line.trim(); } catch(Exception ignore) { }
        }
      }
      if(firstOut.isBlank() && outLine.isDone()) {
        try { String line=outLine.get(); if(line!=null) firstOut=line.trim(); } catch(Exception ignore) { }
      }
      if(firstErr.isBlank() && errLine.isDone()) {
        try { String line=errLine.get(); if(line!=null) firstErr=line.trim(); } catch(Exception ignore) { }
      }

      String[] endpoint=parseRsdEndpointE25(firstOut);
      log.info("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN][COREDEVICE_WIFI_TUNNEL_ENDPOINT_FALLBACK] udid={}, alive={}, stdout={}, stderr={}, host={}, port={}",
        maskUdid(udid),tp.isAlive(),limitTrace(firstOut),limitTrace(firstErr),endpoint==null?"":endpoint[0],endpoint==null?"":endpoint[1]);
      appleTrace("E25 FIX2 WLAN COREDEVICE TUNNEL ENDPOINT","udid="+maskUdid(udid)+", alive="+tp.isAlive()+", stdout="+traceValue(limitTrace(firstOut))+", stderr="+traceValue(limitTrace(firstErr)));
      if(endpoint==null || !tp.isAlive()) return false;

      CommandResult rsdInfo=runProcessE20(60,env,pmdExe.toString(),"--no-color","remote","rsd-info","--rsd",endpoint[0],endpoint[1]);
      String rsdData=(rsdInfo.stdout()+"\n"+rsdInfo.stderr()).trim();
      String lower=rsdData.toLowerCase(Locale.ROOT);
      boolean semanticOk=rsdInfo.ok() && !rsdData.isBlank()
        && !lower.contains("traceback") && !lower.contains("no device") && !lower.contains("error: ")
        && !lower.contains("no such option");
      log.info("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN][COREDEVICE_RSD_INFO_FALLBACK] processOk={}, semanticOk={}, exit={}, udid={}, host={}, port={}, source={}, data={}",
        rsdInfo.ok(),semanticOk,rsdInfo.exitCode(),maskUdid(udid),endpoint[0],endpoint[1],source,limitTrace(rsdData));
      appleTrace("E25 FIX2 WLAN COREDEVICE RSD INFO","processOk="+rsdInfo.ok()+", semanticOk="+semanticOk+", exit="+rsdInfo.exitCode()+", udid="+maskUdid(udid)+", host="+endpoint[0]+", port="+endpoint[1]+", data="+traceValue(limitTrace(rsdData)));
      if(semanticOk) {
        COREDEVICE_SELECTED_UDID_E25.set(udid);
        log.info("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN][COREDEVICE_TRANSPORT_SELECTED] udid={}, source={}, action=STOP_ALTERNATIVE_APPLE_PATHS",maskUdid(udid),source);
        return true;
      }
      return false;
    } catch(Exception ex) {
      log.warn("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN][COREDEVICE_WIFI_TUNNEL_ERROR_FALLBACK] udid={}, source={}, error={}",maskUdid(udid),source,ex.toString());
      return false;
    } finally {
      if(tunnel!=null) {
        long pid=tunnel.pid();
        if(tunnel.isAlive()) {
          tunnel.destroy();
          try { if(!tunnel.waitFor(3,TimeUnit.SECONDS)) tunnel.destroyForcibly(); } catch(InterruptedException ie) { Thread.currentThread().interrupt(); tunnel.destroyForcibly(); }
        }
        log.info("[DISCOVERY][40k37e33-fix8][APPLE_MOBILE][WLAN][COREDEVICE_WIFI_TUNNEL_STOP_FALLBACK] pid={}, alive={}",pid,tunnel.isAlive());
      }
      io.shutdownNow();
    }
  }

  private static String[] parseRsdEndpointE25(String line) {
    if(line==null || line.isBlank()) return null;
    String s=line.trim();
    try {
      JsonNode n=new ObjectMapper().readTree(s);
      String host=firstNonBlank(n.path("address").asText(""),n.path("host").asText(""),n.path("hostname").asText(""));
      int port=n.path("port").asInt(-1);
      if(!host.isBlank() && port>0 && port<=65535) return new String[]{host,Integer.toString(port)};
    } catch(Exception ignore) { }
    java.util.regex.Matcher m=java.util.regex.Pattern.compile("(?:\\\"(?:address|host|hostname)\\\"\\s*:\\s*\\\"([^\\\"]+)\\\".*?\\\"port\\\"\\s*:\\s*(\\d{1,5}))|(?:^|\\s)(\\[[^]]+]|[^\\s:]+)\\s+(\\d{1,5})(?:\\s|$)").matcher(s);
    if(m.find()) {
      String host=m.group(1)!=null?m.group(1):m.group(3);
      String port=m.group(2)!=null?m.group(2):m.group(4);
      if(host!=null && host.startsWith("[") && host.endsWith("]")) host=host.substring(1,host.length()-1);
      try { int p=Integer.parseInt(port); if(p>0 && p<=65535) return new String[]{host,port}; } catch(Exception ignore) { }
    }
    return null;
  }

  /** 40k37e53-fix6: Fuehrt den bestehenden Apple-Userspace-Befehl unveraendert aus und
   * beobachtet parallel nur dessen eigenen Windows-Prozessbaum. Eine ESTABLISHED-RFC1918-
   * Gegenstelle dieses Prozessbaums ist keine neue Erkennungsquelle, sondern die physische
   * Verbindung, die der bereits laufende Apple-Transport selbst verwendet. */
  private CommandResult runProcessE53Fix6CapturePeer(int timeoutSeconds, Map<String,String> environment,
      java.util.concurrent.atomic.AtomicReference<String> peerOut, String... command) {
    Process p=null;
    ExecutorService io=Executors.newFixedThreadPool(2);
    java.util.concurrent.atomic.AtomicBoolean stop=new java.util.concurrent.atomic.AtomicBoolean(false);
    Thread watcher=null;
    try {
      ProcessBuilder pb=new ProcessBuilder(command).redirectErrorStream(false);
      if(environment!=null) pb.environment().putAll(environment);
      p=pb.start();
      final Process proc=p;
      final long rootPid=p.pid();
      watcher=new Thread(() -> {
        while(!stop.get() && proc.isAlive() && peerOut.get()==null) {
          try {
            java.util.Set<Long> pids=new java.util.HashSet<>();
            pids.add(rootPid);
            proc.toHandle().descendants().forEach(h->pids.add(h.pid()));
            Process np=new ProcessBuilder("netstat","-ano","-p","tcp").redirectErrorStream(true).start();
            String ns=readAll(np.getInputStream());
            np.waitFor(2,TimeUnit.SECONDS);
            for(String raw:ns.split("\\R")) {
              String line=raw.trim();
              if(!line.toUpperCase(Locale.ROOT).contains("ESTABLISHED")) continue;
              String[] f=line.split("\\s+");
              if(f.length<5) continue;
              long pid;
              try { pid=Long.parseLong(f[f.length-1]); } catch(Exception ignored) { continue; }
              if(!pids.contains(pid)) continue;
              String remote=f[2];
              int colon=remote.lastIndexOf(':');
              String host=colon>0?remote.substring(0,colon):remote;
              if(host.startsWith("[") && host.endsWith("]")) host=host.substring(1,host.length()-1);
              if(isPrivateLanIpv4E33Fix5(host)) {
                peerOut.compareAndSet(null,host);
                log.info("[DISCOVERY][40k37e53-fix6][APPLE_MOBILE][WLAN][TRANSPORT_SOCKET_PEER] rootPid={}, pid={}, lanIp={}, source=EXISTING_USERSPACE_RSD_PROCESS",rootPid,pid,host);
                break;
              }
            }
            if(peerOut.get()==null) Thread.sleep(100);
          } catch(InterruptedException ie) { Thread.currentThread().interrupt(); break; }
          catch(Exception ignored) { try { Thread.sleep(150); } catch(InterruptedException ie) { Thread.currentThread().interrupt(); break; } }
        }
      },"gam-apple-peer-capture");
      watcher.setDaemon(true);
      watcher.start();
      Future<String> out=io.submit(() -> readAll(proc.getInputStream()));
      Future<String> err=io.submit(() -> readAll(proc.getErrorStream()));
      boolean done=p.waitFor(timeoutSeconds,TimeUnit.SECONDS);
      stop.set(true);
      if(watcher!=null) watcher.interrupt();
      if(!done) {
        p.destroy();
        if(!p.waitFor(2,TimeUnit.SECONDS)) p.destroyForcibly();
        p.waitFor(3,TimeUnit.SECONDS);
        return new CommandResult(false,getFutureTextE20(out,2),getFutureTextE20(err,2)+"\\nTimeout",Integer.MIN_VALUE);
      }
      int exit=p.exitValue();
      return new CommandResult(exit==0,getFutureTextE20(out,5),getFutureTextE20(err,5),exit);
    } catch(Exception ex) {
      if(p!=null && p.isAlive()) p.destroyForcibly();
      return new CommandResult(false,"",ex.getMessage()==null?ex.getClass().getSimpleName():ex.getMessage(),-1);
    } finally {
      stop.set(true);
      if(watcher!=null) watcher.interrupt();
      io.shutdownNow();
    }
  }

  private static CommandResult runProcessE20(int timeoutSeconds, Map<String,String> environment, String... command) {
    Process p=null;
    ExecutorService io=Executors.newFixedThreadPool(2);
    try {
      ProcessBuilder pb=new ProcessBuilder(command).redirectErrorStream(false);
      if(environment!=null) pb.environment().putAll(environment);
      p=pb.start();
      final Process proc=p;
      Future<String> out=io.submit(() -> readAll(proc.getInputStream()));
      Future<String> err=io.submit(() -> readAll(proc.getErrorStream()));
      boolean done=p.waitFor(timeoutSeconds,TimeUnit.SECONDS);
      if(!done) {
        p.destroy();
        if(!p.waitFor(2,TimeUnit.SECONDS)) p.destroyForcibly();
        p.waitFor(3,TimeUnit.SECONDS);
        String stdout=getFutureTextE20(out,2);
        String stderr=getFutureTextE20(err,2);
        return new CommandResult(false,stdout,stderr.isBlank()?"Timeout":stderr+"\nTimeout",Integer.MIN_VALUE);
      }
      int exit=p.exitValue();
      String stdout=getFutureTextE20(out,5);
      String stderr=getFutureTextE20(err,5);
      return new CommandResult(exit==0,stdout,stderr,exit);
    } catch(Exception ex) {
      if(p!=null && p.isAlive()) p.destroyForcibly();
      return new CommandResult(false,"",ex.getMessage()==null?ex.getClass().getSimpleName():ex.getMessage(),-1);
    } finally {
      io.shutdownNow();
    }
  }

  private static String getFutureTextE20(Future<String> future,int seconds) {
    try { return future.get(seconds,TimeUnit.SECONDS); }
    catch(Exception ex) { future.cancel(true); return ""; }
  }

  /** 40k37e25: gezielte, rein lesende Diagnose des bereits gepairten Apple-Geraets. */
  private List<String> parseUdidLines(String stdout){
    if(stdout==null||stdout.isBlank()) return List.of();
    List<String> out=new ArrayList<>();
    for(String raw:stdout.split("\\R")){
      String v=raw.trim();
      if(v.isBlank()) continue;
      int space=v.indexOf(' '); if(space>0) v=v.substring(0,space).trim();
      if(!v.isBlank()) out.add(v);
    }
    return List.copyOf(out);
  }

  private boolean windowsUsbHintPresent() {
    return !readWindowsUsbDevices().isEmpty();
  }

  /**
   * 40k37a1: liest tatsächlich vorhandene Apple-Mobile-PnP-Geräte unter Windows.
   * DEVPKEY_Device_BusReportedDeviceDesc ist wichtig, weil der Treibername häufig
   * nur "Apple Mobile Device USB ..." lautet, der Bus aber "iPad"/"iPhone"
   * meldet. Ausgabe bewusst tab-separiert, damit keine JSON-Bibliothek nötig ist.
   */
  private List<WindowsUsbDevice> readWindowsUsbDevices() {
    if (!System.getProperty("os.name","").toLowerCase(Locale.ROOT).contains("win")) return List.of();

    // 40k37a5: Basiserkennung ist unverhandelbar. Die a4-Variante hat fuer jedes
    // Apple-Seed erneut Properties aller PnP-Geraete abgefragt und konnte dadurch
    // in den Timeout laufen. Jetzt wird jeder Apple-PnP-Knoten genau einmal gelesen,
    // anschliessend in Java per ContainerId/USB-Basiskennung gruppiert und angereichert.
    String ps = "$ErrorActionPreference='SilentlyContinue'; "
      + "$seed=Get-PnpDevice -PresentOnly | Where-Object { $_.InstanceId -match '(?i)^USB\\\\VID_05AC&' }; "
      + "foreach($x in $seed){ "
      + "$cid=[string](Get-PnpDeviceProperty -InstanceId $x.InstanceId -KeyName 'DEVPKEY_Device_ContainerId').Data; "
      + "$parent=[string](Get-PnpDeviceProperty -InstanceId $x.InstanceId -KeyName 'DEVPKEY_Device_Parent').Data; "
      + "$bus=[string](Get-PnpDeviceProperty -InstanceId $x.InstanceId -KeyName 'DEVPKEY_Device_BusReportedDeviceDesc').Data; "
      + "$parts=New-Object System.Collections.Generic.List[string]; "
      + "$props=Get-PnpDeviceProperty -InstanceId $x.InstanceId; foreach($pr in $props){ $v=[string]$pr.Data; if(-not [string]::IsNullOrWhiteSpace($v)){ $parts.Add(([string]$pr.KeyName+'='+$v)) } }; "
      + "$parts.Add(('FriendlyName='+[string]$x.FriendlyName)); $parts.Add(('BusReported='+$bus)); $parts.Add(('InstanceId='+$x.InstanceId)); "
      + "$evidence=($parts -join ' | '); $pt=''; $m=[regex]::Match($evidence,'(?i)\\b(iPad|iPhone)\\d{1,2},\\d{1,2}\\b'); if($m.Success){$pt=$m.Value}; "
      + "$name=[string]$x.FriendlyName; if([string]::IsNullOrWhiteSpace($name)){$name=$bus}; "
      + "$fields=@($x.InstanceId,$name,$bus,$x.Manufacturer,$x.Status,$cid,$parent,$pt,$evidence) | ForEach-Object { ([string]$_ -replace '[\\t\\r\\n]',' ') }; [Console]::WriteLine(($fields -join [char]9)) }";
    CommandResult r = run(14, "powershell.exe", "-NoProfile", "-NonInteractive", "-Command", ps);

    // Sicherheitsnetz: Detailanreicherung darf nie die physische Erkennung kosten.
    // Falls die Property-Abfrage scheitert oder in einen Timeout laeuft, wird sofort
    // auf die bereits in a2/a3 bewaehrte, sehr einfache PnP-Abfrage zurueckgefallen.
    if (!r.ok() || r.stdout().isBlank()) {
      String basic = "$ErrorActionPreference='SilentlyContinue'; "
        + "$d=Get-PnpDevice -PresentOnly | Where-Object { $_.InstanceId -match '(?i)^USB\\\\VID_05AC&' }; "
        + "foreach($x in $d){ $bus=[string](Get-PnpDeviceProperty -InstanceId $x.InstanceId -KeyName 'DEVPKEY_Device_BusReportedDeviceDesc').Data; "
        + "$name=[string]$x.FriendlyName; if([string]::IsNullOrWhiteSpace($name)){$name=$bus}; "
        + "$fields=@($x.InstanceId,$name,$bus,$x.Manufacturer,$x.Status,'','','','fallback-basic-pnp') | ForEach-Object { ([string]$_ -replace '[\\t\\r\\n]',' ') }; [Console]::WriteLine(($fields -join [char]9)) }";
      r = run(12, "powershell.exe", "-NoProfile", "-NonInteractive", "-Command", basic);
    }
    if (!r.ok() || r.stdout().isBlank()) return List.of();

    // Alle Interfaces desselben physischen Apple-Geraets sammeln. Dabei gewinnt ein
    // expliziter iPad/iPhone-Knoten vor PTP oder USB Multiplexor. ProductType/Evidence
    // werden aus allen Geschwistern zusammengetragen, soweit Windows sie liefert.
    record Candidate(String instanceId,String friendly,String bus,String manufacturer,String status,
                     String containerId,String parentId,String productType,String rawEvidence) {}
    List<Candidate> candidates = new ArrayList<>();
    for (String line : r.stdout().split("\\R")) {
      if (line == null || line.isBlank()) continue;
      String[] f = line.split("\\t", -1);
      if (f.length < 1 || f[0].isBlank()) continue;
      String instanceId=f[0].trim();
      // 40k37a6/a7 hard safety boundary: nur echte Apple-USB-Knoten.
      if (!instanceId.toUpperCase(Locale.ROOT).startsWith("USB\\VID_05AC&")) continue;
      candidates.add(new Candidate(instanceId,
        f.length>1?f[1].trim():"", f.length>2?f[2].trim():"", f.length>3?f[3].trim():"",
        f.length>4?f[4].trim():"", f.length>5?f[5].trim():"", f.length>6?f[6].trim():"",
        f.length>7?f[7].trim():"", f.length>8?f[8].trim():""));
    }

    // 40k37a7: Windows zeigt ein physisches iPad als Basis-PnP-Knoten plus
    // MI_00/MI_01-Interfaces. ContainerId ist nicht auf jedem Treiberpfad stabil
    // verfuegbar. Deshalb bilden wir die physische Identitaet zusaetzlich ueber
    // die echte Parent-Beziehung ab. Ein Interface wird nur mit einem Kandidaten
    // vereinigt, wenn sein DEVPKEY_Device_Parent exakt dessen InstanceId ist.
    // Dadurch werden zwei gleichzeitig angeschlossene iPads gleichen Typs NICHT
    // allein wegen gleicher VID/PID zusammengelegt.
    int n=candidates.size();
    int[] parent=new int[n];
    for(int i=0;i<n;i++) parent[i]=i;
    java.util.function.IntUnaryOperator root = new java.util.function.IntUnaryOperator(){
      public int applyAsInt(int x){ while(parent[x]!=x){ parent[x]=parent[parent[x]]; x=parent[x]; } return x; }
    };
    java.util.function.BiConsumer<Integer,Integer> unite=(a,b)->{
      int ra=root.applyAsInt(a), rb=root.applyAsInt(b); if(ra!=rb) parent[rb]=ra;
    };
    for(int i=0;i<n;i++) for(int j=i+1;j<n;j++) {
      Candidate a=candidates.get(i), b=candidates.get(j);
      boolean sameContainer=!a.containerId().isBlank() && a.containerId().equalsIgnoreCase(b.containerId());
      boolean parentLink=(!a.parentId().isBlank() && a.parentId().equalsIgnoreCase(b.instanceId()))
        || (!b.parentId().isBlank() && b.parentId().equalsIgnoreCase(a.instanceId()));
      if(sameContainer || parentLink) unite.accept(i,j);
    }

    // 40k37a8: Auf dem aktuellen Windows-Apple-Treiberpfad koennen ContainerId/
    // Parent-Beziehungen fuer Basisgeraet und MI_00/MI_01 fehlen, obwohl alle Knoten
    // eindeutig dieselbe Apple VID/PID tragen. VID/PID allein ist KEINE globale
    // Geraeteidentitaet (zwei gleiche iPads haetten denselben Wert). Darum greift
    // dieser Fallback nur, wenn fuer eine VID/PID im aktuellen Scan exakt EIN
    // physischer Nicht-MI-Basisknoten vorhanden ist. Dann duerfen dessen MI_xx-
    // Interfaces sicher diesem einen Basisgeraet zugeschlagen werden. Gibt es zwei
    // Basisknoten gleicher VID/PID, bleibt die Zuordnung unangetastet.
    LinkedHashMap<String,List<Integer>> byVidPid=new LinkedHashMap<>();
    for(int i=0;i<n;i++) {
      String key=appleVidPidKey(candidates.get(i).instanceId());
      if(!key.isBlank()) byVidPid.computeIfAbsent(key,k->new ArrayList<>()).add(i);
    }
    for(List<Integer> idxs:byVidPid.values()) {
      List<Integer> bases=new ArrayList<>();
      for(Integer idx:idxs) if(!isAppleUsbInterface(candidates.get(idx).instanceId())) bases.add(idx);
      if(bases.size()!=1) continue;
      int base=bases.get(0);
      for(Integer idx:idxs) {
        if(idx!=base && isAppleUsbInterface(candidates.get(idx).instanceId())) unite.accept(base,idx);
      }
    }
    LinkedHashMap<Integer,List<Candidate>> groups = new LinkedHashMap<>();
    for(int i=0;i<n;i++) groups.computeIfAbsent(root.applyAsInt(i),k->new ArrayList<>()).add(candidates.get(i));

    // 40k37a9 Postcondition: Ein MI_xx-Knoten darf niemals als eigene physische
    // Apple-Gruppe emittiert werden, wenn fuer dieselbe VID/PID exakt ein
    // Nicht-MI-Basisknoten im selben Scan existiert. Dieser zweite, bewusst
    // redundante Schutz sitzt direkt vor der Emission und verhindert, dass eine
    // kuenftige Aenderung an Container-/Parent-Gruppierung wieder 2x iPad +
    // Multiplexor nach aussen gelangen laesst.
    LinkedHashMap<String,List<Candidate>> uniqueBasesByVidPid = new LinkedHashMap<>();
    for (Candidate c : candidates) {
      if (isAppleUsbInterface(c.instanceId())) continue;
      String key = appleVidPidKey(c.instanceId());
      if (!key.isBlank()) uniqueBasesByVidPid.computeIfAbsent(key,k->new ArrayList<>()).add(c);
    }
    if (!groups.isEmpty()) {
      LinkedHashMap<Integer,List<Candidate>> collapsed = new LinkedHashMap<>();
      Set<Integer> consumed = new HashSet<>();
      for (Map.Entry<Integer,List<Candidate>> entry : groups.entrySet()) {
        if (consumed.contains(entry.getKey())) continue;
        List<Candidate> merged = new ArrayList<>(entry.getValue());
        Candidate anchor = merged.stream().filter(c -> !isAppleUsbInterface(c.instanceId())).findFirst().orElse(null);
        if (anchor != null) {
          String key = appleVidPidKey(anchor.instanceId());
          if (!key.isBlank() && uniqueBasesByVidPid.getOrDefault(key,List.of()).size() == 1) {
            for (Map.Entry<Integer,List<Candidate>> other : groups.entrySet()) {
              if (other.getKey().equals(entry.getKey()) || consumed.contains(other.getKey())) continue;
              boolean onlyInterfaces = !other.getValue().isEmpty() && other.getValue().stream().allMatch(c -> isAppleUsbInterface(c.instanceId()));
              boolean sameVidPid = other.getValue().stream().allMatch(c -> key.equalsIgnoreCase(appleVidPidKey(c.instanceId())));
              if (onlyInterfaces && sameVidPid) {
                merged.addAll(other.getValue());
                consumed.add(other.getKey());
              }
            }
          }
        }
        collapsed.put(entry.getKey(), merged);
        consumed.add(entry.getKey());
      }
      groups = collapsed;
    }

    List<WindowsUsbDevice> out = new ArrayList<>();
    for (List<Candidate> group : groups.values()) {
      Candidate best=group.get(0);
      String productType="";
      StringBuilder allEvidence=new StringBuilder();
      for (Candidate c : group) {
        if (!c.productType().isBlank()) productType=c.productType();
        if (!c.rawEvidence().isBlank()) {
          if (allEvidence.length()>0) allEvidence.append(" | ");
          allEvidence.append(c.rawEvidence());
        }
        String ev=(c.friendly()+" "+c.bus()).toLowerCase(Locale.ROOT);
        String bev=(best.friendly()+" "+best.bus()).toLowerCase(Locale.ROOT);
        int score=(ev.contains("ipad")||ev.contains("iphone")?100:0)
          + (!c.instanceId().toUpperCase(Locale.ROOT).matches(".*&MI_[0-9A-F]{2}.*")?40:0)
          + (ev.contains("ptp")?-20:0) + (ev.contains("multiplex")?-30:0);
        int bestScore=(bev.contains("ipad")||bev.contains("iphone")?100:0)
          + (!best.instanceId().toUpperCase(Locale.ROOT).matches(".*&MI_[0-9A-F]{2}.*")?40:0)
          + (bev.contains("ptp")?-20:0) + (bev.contains("multiplex")?-30:0);
        if (score>bestScore) best=c;
      }
      if (productType.isBlank()) {
        java.util.regex.Matcher pm=java.util.regex.Pattern.compile("(?i)\\b(?:iPad|iPhone)\\d{1,2},\\d{1,2}\\b").matcher(allEvidence.toString());
        if(pm.find()) productType=pm.group();
      }
      StringBuilder interfaceIds=new StringBuilder();
      for (Candidate c : group) {
        if (interfaceIds.length()>0) interfaceIds.append(", ");
        interfaceIds.append(c.instanceId());
      }
      if (allEvidence.length()>0) allEvidence.append(" | ");
      allEvidence.append("AppleUsbNodes=").append(interfaceIds);
      String combined=(best.friendly()+" "+best.bus()+" "+allEvidence).toLowerCase(Locale.ROOT);
      String platform=productType.toLowerCase(Locale.ROOT).startsWith("ipad") || combined.contains("ipad") ? "iPadOS"
        : productType.toLowerCase(Locale.ROOT).startsWith("iphone") || combined.contains("iphone") ? "iOS" : "iOS/iPadOS";
      String modelDisplayName=appleModelDisplayName(productType);
      String name=firstNonBlank(modelDisplayName,
        (combined.contains("ipad") ? "iPad" : combined.contains("iphone") ? "iPhone" : ""),
        best.bus(), best.friendly(), "Apple-Mobilgeraet");
      out.add(new WindowsUsbDevice(best.instanceId(), name, platform, firstNonBlank(best.manufacturer(),"Apple"), best.status(),
        best.containerId(), best.parentId(), productType, modelDisplayName, compactWindowsEvidence(allEvidence.toString())));
    }
    return out;
  }

  private static String physicalAppleUsbKey(String instanceId) {
    if (instanceId == null) return "";
    // Entfernt nur die Interface-Komponente MI_00/MI_01; VID/PID/Serienanteil bleiben.
    return instanceId.replaceAll("(?i)&MI_[0-9A-F]{2}", "");
  }

  private static boolean isAppleUsbInterface(String instanceId) {
    if (instanceId == null || instanceId.isBlank()) return false;
    String upper = instanceId.toUpperCase(Locale.ROOT);
    // 40k37a9: kein regex auf dem kompletten Windows-InstanceId-Praefix.
    // Windows liefert z.B. USB\VID_05AC&PID_12AB&MI_00\... und
    // USB\VID_05AC&PID_12AB&MI_01\.... Fuer die Composite-Interface-
    // Erkennung ist allein der explizite &MI_xx-Bestandteil entscheidend.
    return java.util.regex.Pattern.compile("&MI_[0-9A-F]{2}(?:\\\\|$)").matcher(upper).find();
  }

  private static String appleVidPidKey(String instanceId) {
    if (instanceId == null || instanceId.isBlank()) return "";
    String upper = instanceId.toUpperCase(Locale.ROOT);
    String prefix = "USB\\VID_05AC&PID_";
    if (!upper.startsWith(prefix) || upper.length() < prefix.length() + 4) return "";
    String pid = upper.substring(prefix.length(), prefix.length() + 4);
    for (int i = 0; i < pid.length(); i++) {
      char c = pid.charAt(i);
      if (!((c >= '0' && c <= '9') || (c >= 'A' && c <= 'F'))) return "";
    }
    return "VID_05AC&PID_" + pid;
  }

  private static String compactWindowsEvidence(String raw) {
    if (raw == null || raw.isBlank()) return "";
    LinkedHashSet<String> useful = new LinkedHashSet<>();
    java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?i)\\b(?:iPad|iPhone)\\d{1,2},\\d{1,2}\\b|Apple iPad|Apple iPhone|\\biPad\\b|\\biPhone\\b").matcher(raw);
    while (m.find() && useful.size() < 8) useful.add(m.group());
    return String.join(", ", useful);
  }


  /** d6: Eine liegengebliebene d5-EXE gilt nicht allein durch ihre Existenz als installiert. */
  private boolean ideviceInstallerUsable(){
    String tool=resolveTool("ideviceinstaller");
    if(tool==null) return false;
    CommandResult smoke=run(8,tool,"--help");
    appleTrace("E4 SOFTWARE EXISTING SMOKE","tool="+traceValue(tool)+", ok="+smoke.ok()+", stderr="+traceValue(limitTrace(smoke.stderr())));
    return smoke.ok();
  }

  /** 40k37e25: vollstaendige, isolierte ideviceinstaller-UCRT64-Runtime aus MSYS2.
   *
   * Anders als d5 kopiert d8 keine DLLs mehr aus der jrjr/c48-Toolchain. Stattdessen wird die
   * offizielle UCRT64-Repository-Datenbank gelesen, die Runtime-Abhaengigkeitskette rekursiv
   * aufgeloest, jedes Paket gegen seinen SHA-256 aus der Repository-Datenbank geprueft und nur
   * der gemeinsame ucrt64/bin-Laufzeitbestand in den GAM-Unterordner uebernommen.
   */
  private ToolchainInstallResult installIdeviceInstallerSupplement(){
    long started=System.currentTimeMillis();
    if(ideviceInstallerUsable()) return new ToolchainInstallResult(true,"ALREADY_INSTALLED","ideviceinstaller ist bereits vorhanden und startfaehig.",OffsetDateTime.now().toString(),MSYS2_IDEVICEINSTALLER_PINNED_VERSION,firstNonBlank(resolveTool("ideviceinstaller")),false,System.currentTimeMillis()-started);
    if(!System.getProperty("os.name","").toLowerCase(Locale.ROOT).contains("win")) return new ToolchainInstallResult(false,"UNSUPPORTED","Automatische ideviceinstaller-Ergaenzung ist derzeit nur fuer Windows vorgesehen.",OffsetDateTime.now().toString(),MSYS2_IDEVICEINSTALLER_PINNED_VERSION,"",false,System.currentTimeMillis()-started);
    Path dbFile=null, dbStage=null, runtimeStage=null;
    try{
      Path root=findGamRoot().toAbsolutePath().normalize();
      Path target=root.resolve("tools").resolve("libimobiledevice").resolve("ideviceinstaller").toAbsolutePath().normalize();
      Files.createDirectories(target.getParent());
      dbFile=Files.createTempFile("gam-msys2-ucrt64-", ".db");
      dbStage=Files.createTempDirectory("gam-msys2-ucrt64-db-");
      runtimeStage=Files.createTempDirectory("gam-msys2-ucrt64-runtime-");
      HttpClient client=HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(java.time.Duration.ofSeconds(20)).build();
      Path packageCache=root.resolve("tools").resolve("libimobiledevice").resolve("ideviceinstaller-cache").resolve("msys2-ucrt64");
      Files.createDirectories(packageCache);

      // Repository-DB: online bevorzugt mit Mirror-Failover. Falls wirklich alle Mirrors
      // ausfallen, darf eine zuvor erfolgreich geladene DB als Offline-Fallback dienen.
      Path cachedDb=packageCache.resolve("ucrt64.db");
      try{
        downloadMsys2WithFailover(client,"ucrt64.db",dbFile,60);
        Files.copy(dbFile,cachedDb,StandardCopyOption.REPLACE_EXISTING);
        appleTrace("E4 SOFTWARE MSYS2 DB","source=online, cache="+traceValue(cachedDb.toString()));
      }catch(Exception onlineDbError){
        if(!Files.isRegularFile(cachedDb)) throw onlineDbError;
        Files.copy(cachedDb,dbFile,StandardCopyOption.REPLACE_EXISTING);
        appleTrace("E4 SOFTWARE MSYS2 DB","source=cache, reason="+traceValue(onlineDbError.getMessage()));
      }
      CommandResult dbExtract=run(60,"tar.exe","-xf",dbFile.toString(),"-C",dbStage.toString());
      if(!dbExtract.ok()) throw new IllegalStateException("MSYS2-UCRT64-Repository-Datenbank konnte nicht entpackt werden: "+firstNonBlank(dbExtract.stderr(),dbExtract.stdout()));
      Map<String,Msys2Package> repo=readMsys2RepoDb(dbStage);
      Msys2Package rootPkg=repo.get(MSYS2_IDEVICEINSTALLER_PACKAGE);
      if(rootPkg==null) throw new IllegalStateException("ideviceinstaller fehlt in der MSYS2-UCRT64-Repository-Datenbank.");
      if(!MSYS2_IDEVICEINSTALLER_PINNED_VERSION.equals(rootPkg.version())) throw new SecurityException("Unerwartete ideviceinstaller-Version im MSYS2-Repository: "+rootPkg.version()+" (erwartet "+MSYS2_IDEVICEINSTALLER_PINNED_VERSION+")");
      if(!MSYS2_IDEVICEINSTALLER_UCRT_SHA256.equalsIgnoreCase(rootPkg.sha256())) throw new SecurityException("Der gepinnte ideviceinstaller-SHA-256 stimmt nicht mit der MSYS2-Repository-Datenbank ueberein.");

      List<Msys2Package> closure=resolveMsys2RuntimeClosure(repo,MSYS2_IDEVICEINSTALLER_PACKAGE);
      appleTrace("E4 SOFTWARE MSYS2 RESOLVE","packages="+closure.size()+", names="+traceValue(closure.stream().map(Msys2Package::name).collect(java.util.stream.Collectors.joining(","))));
      for(Msys2Package pkg:closure){
        Path archive=Files.createTempFile("gam-msys2-pkg-", ".pkg.tar.zst");
        Path cachedArchive=packageCache.resolve(pkg.filename());
        boolean cacheHit=false;
        try{
          if(Files.isRegularFile(cachedArchive)){
            String cachedSha=sha256(cachedArchive);
            if(pkg.sha256()!=null && pkg.sha256().equalsIgnoreCase(cachedSha)){
              Files.copy(cachedArchive,archive,StandardCopyOption.REPLACE_EXISTING);
              cacheHit=true;
              appleTrace("E4 SOFTWARE MSYS2 CACHE HIT","name="+pkg.name()+", version="+pkg.version()+", bytes="+Files.size(cachedArchive));
            }else{
              appleTrace("E4 SOFTWARE MSYS2 CACHE INVALID","name="+pkg.name()+", expected="+traceValue(pkg.sha256())+", actual="+traceValue(cachedSha));
              try{Files.deleteIfExists(cachedArchive);}catch(Exception ignored){}
            }
          }
          if(!cacheHit){
            downloadMsys2WithFailover(client,pkg.filename(),archive,120);
            String downloadedSha=sha256(archive);
            if(pkg.sha256()==null||pkg.sha256().isBlank()||!pkg.sha256().equalsIgnoreCase(downloadedSha)) throw new SecurityException("SHA-256-Pruefung fehlgeschlagen fuer "+pkg.name()+" "+pkg.version());
            Files.copy(archive,cachedArchive,StandardCopyOption.REPLACE_EXISTING);
            appleTrace("E4 SOFTWARE MSYS2 CACHE STORE","name="+pkg.name()+", version="+pkg.version()+", bytes="+Files.size(archive));
          }
          String actual=sha256(archive);
          if(pkg.sha256()==null||pkg.sha256().isBlank()||!pkg.sha256().equalsIgnoreCase(actual)) throw new SecurityException("SHA-256-Pruefung fehlgeschlagen fuer "+pkg.name()+" "+pkg.version());
          CommandResult extract=run(60,"tar.exe","-xf",archive.toString(),"-C",runtimeStage.toString());
          if(!extract.ok()) throw new IllegalStateException("MSYS2-Paket konnte nicht entpackt werden: "+pkg.name()+" - "+firstNonBlank(extract.stderr(),extract.stdout()));
          appleTrace("E4 SOFTWARE MSYS2 PACKAGE","name="+pkg.name()+", version="+pkg.version()+", sha256Verified=true, cacheHit="+cacheHit+", bytes="+Files.size(archive));
        } finally { try{Files.deleteIfExists(archive);}catch(Exception ignored){} }
      }

      Path bin=runtimeStage.resolve("ucrt64").resolve("bin");
      Path exe=bin.resolve("ideviceinstaller.exe");
      if(!Files.isRegularFile(exe)) throw new IllegalStateException("Die aufgeloeste MSYS2-Runtime enthaelt ideviceinstaller.exe nicht.");
      Path fresh=target.resolveSibling("ideviceinstaller.d8-new");
      deleteRecursively(fresh); Files.createDirectories(fresh);
      int copied=0;
      try(var st=Files.list(bin)){
        for(Path f:st.filter(Files::isRegularFile).toList()){
          String n=f.getFileName().toString().toLowerCase(Locale.ROOT);
          if(n.endsWith(".dll")||n.equals("ideviceinstaller.exe")){ Files.copy(f,fresh.resolve(f.getFileName()),StandardCopyOption.REPLACE_EXISTING); copied++; }
        }
      }
      CommandResult smoke=run(15,fresh.resolve("ideviceinstaller.exe").toString(),"--help");
      appleTrace("E4 SOFTWARE MSYS2 SMOKE","ok="+smoke.ok()+", exitCode="+formatExitCode(smoke.exitCode())+", files="+copied+", stdout="+traceValue(limitTrace(smoke.stdout()))+", stderr="+traceValue(limitTrace(smoke.stderr())));
      if(!smoke.ok()) throw new IllegalStateException("Die vollstaendige MSYS2-UCRT64-Runtime wurde geladen, ideviceinstaller startet aber weiterhin nicht (ExitCode "+formatExitCode(smoke.exitCode())+").");

      deleteRecursively(target);
      try{ Files.move(fresh,target,StandardCopyOption.ATOMIC_MOVE); }
      catch(Exception moveFailure){ Files.move(fresh,target,StandardCopyOption.REPLACE_EXISTING); }
      return new ToolchainInstallResult(true,"INSTALLED","MSYS2 UCRT64 ideviceinstaller "+rootPkg.version()+" samt "+closure.size()+" Runtime-Paketen wurde SHA-256-geprueft und GAM-lokal isoliert installiert.",OffsetDateTime.now().toString(),"MSYS2-"+rootPkg.version(),target.toString(),true,System.currentTimeMillis()-started);
    }catch(Exception e){
      return new ToolchainInstallResult(false,"INSTALL_FAILED",e.getMessage()==null?e.getClass().getSimpleName():e.getMessage(),OffsetDateTime.now().toString(),"MSYS2-"+MSYS2_IDEVICEINSTALLER_PINNED_VERSION,"",false,System.currentTimeMillis()-started);
    } finally {
      try{if(dbFile!=null)Files.deleteIfExists(dbFile);}catch(Exception ignored){}
      try{if(dbStage!=null)deleteRecursively(dbStage);}catch(Exception ignored){}
      try{if(runtimeStage!=null)deleteRecursively(runtimeStage);}catch(Exception ignored){}
    }
  }

  private record Msys2Package(String name,String version,String filename,String sha256,List<String> depends,List<String> provides){}

  /** d8: Ein Repository-Objekt ueber mehrere von MSYS2 gelistete UCRT64-Mirrors laden.
   * Bei HTTP 5xx, 4xx, Timeout oder Transportfehler wird automatisch der naechste Mirror
   * probiert. SHA-256 bleibt Aufgabe des Aufrufers, weil nur die Repo-DB selbst keinen
   * paketbezogenen Hash besitzt. */
  private void downloadMsys2WithFailover(HttpClient client,String relativePath,Path target,int timeoutSeconds) throws Exception{
    List<String> errors=new ArrayList<>();
    for(String base:MSYS2_UCRT_REPO_BASES){
      String url=base+relativePath;
      try{
        Files.deleteIfExists(target);
        downloadTo(client,url,target,timeoutSeconds);
        appleTrace("E4 SOFTWARE MSYS2 MIRROR","file="+traceValue(relativePath)+", result=ok, base="+traceValue(base));
        return;
      }catch(Exception e){
        errors.add(base+" -> "+firstNonBlank(e.getMessage(),e.getClass().getSimpleName()));
        appleTrace("E4 SOFTWARE MSYS2 MIRROR","file="+traceValue(relativePath)+", result=fail, base="+traceValue(base)+", error="+traceValue(firstNonBlank(e.getMessage(),e.getClass().getSimpleName())));
      }
    }
    throw new IllegalStateException("Alle MSYS2-UCRT64-Mirrors fehlgeschlagen fuer "+relativePath+": "+String.join(" | ",errors));
  }

  private static void downloadTo(HttpClient client,String url,Path target,int timeoutSeconds) throws Exception{
    HttpRequest request=HttpRequest.newBuilder(URI.create(url)).header("User-Agent","GAM-Standalone/2.1.0").timeout(java.time.Duration.ofSeconds(timeoutSeconds)).GET().build();
    HttpResponse<Path> dl=client.send(request,HttpResponse.BodyHandlers.ofFile(target));
    if(dl.statusCode()/100!=2) throw new IllegalStateException("Download fehlgeschlagen (HTTP "+dl.statusCode()+"): "+url);
  }

  /** d8: Repo-DB inklusive PROVIDES einlesen. Virtuelle Pakete haben kein eigenes Archiv. */
  private static Map<String,Msys2Package> readMsys2RepoDb(Path root) throws Exception{
    LinkedHashMap<String,Msys2Package> out=new LinkedHashMap<>();
    try(var walk=Files.walk(root,4)){
      for(Path desc:walk.filter(p->Files.isRegularFile(p)&&"desc".equals(p.getFileName().toString())).toList()){
        List<String> lines=Files.readAllLines(desc,StandardCharsets.UTF_8);
        Map<String,List<String>> fields=new LinkedHashMap<>(); String current=null;
        for(String line:lines){
          if(line.startsWith("%")&&line.endsWith("%")&&line.length()>2){ current=line.substring(1,line.length()-1); fields.putIfAbsent(current,new ArrayList<>()); }
          else if(current!=null&&!line.isBlank()) fields.get(current).add(line.trim());
        }
        String name=first(fields,"NAME"), version=first(fields,"VERSION"), filename=first(fields,"FILENAME"), hash=first(fields,"SHA256SUM");
        if(name!=null&&version!=null&&filename!=null&&hash!=null){
          out.put(name,new Msys2Package(name,version,filename,hash,
            List.copyOf(fields.getOrDefault("DEPENDS",List.of())),
            List.copyOf(fields.getOrDefault("PROVIDES",List.of()))));
        }
      }
    }
    return out;
  }

  private static String first(Map<String,List<String>> fields,String key){ List<String> v=fields.get(key); return v==null||v.isEmpty()?null:v.get(0); }

  /** d7: echte Pakete zuerst, ansonsten virtuelle Abhaengigkeiten ueber PROVIDES.
   * Bei mehreren Providern wird aus Sicherheitsgruenden nicht geraten. */
  private List<Msys2Package> resolveMsys2RuntimeClosure(Map<String,Msys2Package> repo,String rootName){
    LinkedHashMap<String,List<Msys2Package>> providers=new LinkedHashMap<>();
    for(Msys2Package pkg:repo.values()){
      for(String rawProvide:pkg.provides()){
        String virtualName=normalizeMsys2Dependency(rawProvide);
        if(virtualName!=null) providers.computeIfAbsent(virtualName,k->new ArrayList<>()).add(pkg);
      }
    }
    LinkedHashMap<String,Msys2Package> resolved=new LinkedHashMap<>();
    java.util.ArrayDeque<String> queue=new java.util.ArrayDeque<>(); queue.add(rootName);
    while(!queue.isEmpty()){
      String requested=queue.removeFirst();
      Msys2Package pkg=repo.get(requested);
      if(pkg==null){
        List<Msys2Package> candidates=providers.getOrDefault(requested,List.of());
        if(candidates.size()==1){
          pkg=candidates.get(0);
          appleTrace("E4 SOFTWARE MSYS2 PROVIDER","virtual="+requested+", provider="+pkg.name()+", version="+pkg.version());
        } else if(candidates.isEmpty()){
          throw new IllegalStateException("MSYS2-Runtime-Abhaengigkeit nicht gefunden: "+requested);
        } else {
          String names=candidates.stream().map(Msys2Package::name).sorted().collect(java.util.stream.Collectors.joining(","));
          throw new IllegalStateException("MSYS2-Runtime-Abhaengigkeit hat mehrere Provider und wird nicht geraten: "+requested+" -> "+names);
        }
      }
      if(resolved.containsKey(pkg.name())) continue;
      resolved.put(pkg.name(),pkg);
      for(String raw:pkg.depends()){
        String dep=normalizeMsys2Dependency(raw);
        if(dep!=null&&dep.startsWith("mingw-w64-ucrt-x86_64-")&&!resolved.containsKey(dep)) queue.addLast(dep);
      }
    }
    return List.copyOf(resolved.values());
  }

  private static String normalizeMsys2Dependency(String raw){
    if(raw==null) return null; String x=raw.trim(); if(x.isEmpty()) return null;
    int cut=x.length(); for(char c:new char[]{'<','>','='}){ int i=x.indexOf(c); if(i>=0&&i<cut) cut=i; }
    x=x.substring(0,cut).trim(); return x.isEmpty()?null:x;
  }

  private static Path findDirectoryContaining(Path root,String fileName) throws Exception{
    try(var walk=Files.walk(root,6)){ for(Path p:walk.filter(Files::isDirectory).toList()) if(Files.isRegularFile(p.resolve(fileName))) return p; }
    return null;
  }

  /** 40k37c3: lädt die gepinnte x64-Windows-Distribution aus dem bekannten GitHub-Release,
   * prüft den von GitHub gelieferten SHA-256-Digest (sofern vorhanden), entpackt ausschließlich
   * innerhalb von <GAM>/tools/libimobiledevice und validiert die drei von GAM benötigten Werkzeuge. */
  public ToolchainInstallResult installToolchain() {
    long started=System.currentTimeMillis();
    String hostOs=System.getProperty("os.name","").toLowerCase(Locale.ROOT);
    if(hostOs.contains("mac")) return macOsToolchainE56(started);
    if(!hostOs.contains("win")) return linuxToolchainPackageManagerE55(started);
    try {
      Path gamRoot=findGamRoot().toAbsolutePath().normalize();
      Path target=gamRoot.resolve("tools").resolve("libimobiledevice").toAbsolutePath().normalize();
      Path forbiddenBackend=gamRoot.resolve("backend").toAbsolutePath().normalize();
      if(target.startsWith(forbiddenBackend))
        throw new IllegalStateException("Interner Schutz: berechnetes Toolchain-Ziel liegt unter backend und wurde blockiert: "+target);
      if(!target.getParent().equals(gamRoot.resolve("tools").toAbsolutePath().normalize()))
        throw new IllegalStateException("Interner Schutz: unerwartetes Toolchain-Ziel: "+target);
      log.info("Apple-Mobile-Toolchain 40k37c47: GAM-Root={}, Installationsziel={}",gamRoot,target);
      Files.createDirectories(target);
      HttpClient client=HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(java.time.Duration.ofSeconds(20)).build();
      HttpRequest metaReq=HttpRequest.newBuilder(URI.create(TOOLCHAIN_RELEASE_API)).header("Accept","application/vnd.github+json").header("User-Agent","GAM-Standalone/2.1.0").timeout(java.time.Duration.ofSeconds(30)).GET().build();
      HttpResponse<String> meta=client.send(metaReq,HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      if(meta.statusCode()/100!=2) throw new IllegalStateException("GitHub-Release-Metadaten nicht erreichbar (HTTP "+meta.statusCode()+")");
      JsonNode root=new ObjectMapper().readTree(meta.body());
      JsonNode selected=null;
      List<String> assetNames=new ArrayList<>();
      for(JsonNode a:root.path("assets")) {
        String originalName=a.path("name").asText("");
        String n=originalName.toLowerCase(Locale.ROOT);
        if(!originalName.isBlank()) assetNames.add(originalName);
        // 40k37c5: jrjr benennt die 64-Bit-Windows-Suite mit "w64".
        // c4 kannte x64/win64/mingw64, aber nicht dieses reale Namensschema.
        if(n.endsWith(".zip") && (n.contains("w64")||n.contains("x86_64")||n.contains("x64")||n.contains("mingw64")||n.contains("win64"))) { selected=a; break; }
      }
      if(selected==null) throw new IllegalStateException("Im gepinnten Release wurde kein Windows-64-Bit-ZIP gefunden. Gefundene Assets: "+(assetNames.isEmpty()?"<keine>":String.join(", ",assetNames)));
      log.info("Apple-Mobile-Toolchain 40k37c47: verwende Release-Asset {}",selected.path("name").asText("<unbekannt>"));
      String url=selected.path("browser_download_url").asText("");
      if(url.isBlank()||!url.startsWith("https://github.com/jrjr/libimobiledevice-windows/releases/download/")) throw new IllegalStateException("Unerwartete Download-Adresse im Release.");
      String digest=selected.path("digest").asText("");
      Path zip=Files.createTempFile("gam-libimobiledevice-", ".zip");
      try {
        HttpRequest dlReq=HttpRequest.newBuilder(URI.create(url)).header("User-Agent","GAM-Standalone/2.1.0").timeout(java.time.Duration.ofMinutes(2)).GET().build();
        HttpResponse<Path> dl=client.send(dlReq,HttpResponse.BodyHandlers.ofFile(zip));
        if(dl.statusCode()/100!=2) throw new IllegalStateException("Toolchain-Download fehlgeschlagen (HTTP "+dl.statusCode()+")");
        boolean digestVerified=false;
        if(digest!=null && digest.toLowerCase(Locale.ROOT).startsWith("sha256:")) {
          String expected=digest.substring(7).trim().toLowerCase(Locale.ROOT);
          String actual=sha256(zip);
          if(!expected.equals(actual)) throw new SecurityException("SHA-256-Prüfung der Toolchain fehlgeschlagen.");
          digestVerified=true;
        }
        Path stage=Files.createTempDirectory(target.getParent(),"libimobiledevice-stage-");
        try {
          unzipSafely(zip,stage);
          Path source=findToolDirectory(stage);
          if(source==null) throw new IllegalStateException("Die heruntergeladene Distribution enthält idevice_id.exe, idevicepair.exe und ideviceinfo.exe nicht gemeinsam.");
          // Ziel erst nach erfolgreicher Validierung ersetzen; README darf bestehen bleiben.
          try(var stream=Files.list(target)){ for(Path x:stream.toList()) deleteRecursively(x); }
          try(var stream=Files.list(source)){ for(Path x:stream.toList()) Files.move(x,target.resolve(x.getFileName()),StandardCopyOption.REPLACE_EXISTING); }
        } finally { deleteRecursively(stage); }
        boolean ok=Files.isRegularFile(target.resolve("idevice_id.exe"))&&Files.isRegularFile(target.resolve("idevicepair.exe"))&&Files.isRegularFile(target.resolve("ideviceinfo.exe"));
        if(!ok) throw new IllegalStateException("Toolchain wurde entpackt, aber die Pflichtwerkzeuge fehlen im Zielverzeichnis.");
        return new ToolchainInstallResult(true,"INSTALLED","libimobiledevice wurde GAM-lokal installiert und validiert. Kein PATH und keine UAC-Installation erforderlich.",OffsetDateTime.now().toString(),TOOLCHAIN_TAG,target.toString(),digestVerified,System.currentTimeMillis()-started);
      } finally { Files.deleteIfExists(zip); }
    } catch(Exception e) {
      log.warn("Apple-Mobile-Toolchain-Installation fehlgeschlagen",e);
      return new ToolchainInstallResult(false,"INSTALL_FAILED","Installation fehlgeschlagen: "+(e.getMessage()==null?e.getClass().getSimpleName():e.getMessage()),OffsetDateTime.now().toString(),TOOLCHAIN_TAG,"",false,System.currentTimeMillis()-started);
    }
  }


  /** 40k37e56: Vorbereiteter macOS-Unterbau fuer denselben Apple-Mobile-Pfad, der unter
   * Windows und Linux bereits praktisch validiert ist. Der Realtest auf macOS steht noch aus.
   * libimobiledevice/ideviceinstaller werden bevorzugt aus Homebrew verwendet; pymobiledevice3
   * 11.16.3 liegt isoliert unter tools/apple-coredevice/runtime/macos-venv. */
  private ToolchainInstallResult macOsToolchainE56(long started) {
    try {
      String arch=System.getProperty("os.arch","unknown");
      Path root=findGamRoot().toAbsolutePath().normalize();
      Path runtime=root.resolve("tools").resolve("apple-coredevice").resolve("runtime");
      Path venv=runtime.resolve("macos-venv");
      Path pmd=venv.resolve("bin").resolve("pymobiledevice3");
      List<String> missing=new ArrayList<>();
      // fix14: Fuer Pairing/CoreDevice sind nur die Lockdown-Basiswerkzeuge zwingend.
      // Diagnose/App-Werkzeuge werden nicht zum Anlass fuer wiederholte Root-Abfragen.
      for(String tool:List.of("idevice_id","ideviceinfo","idevicepair"))
        if(resolveTool(tool)==null) missing.add(tool);
      boolean pmdReady=Files.isRegularFile(pmd) || resolveTool("pymobiledevice3")!=null;
      if(missing.isEmpty() && pmdReady) {
        log.info("[DISCOVERY][40k37e56][APPLE_MOBILE][MACOS_TOOLCHAIN] result=READY arch={} pmd3={}",arch,Files.isRegularFile(pmd)?pmd:"PATH");
        return new ToolchainInstallResult(true,"MACOS_READY",
          "macOS-Apple-Basis und pymobiledevice3 sind vorhanden. Der vorbereitete CoreDevice/RemotePairing-/USERSPACE_RSD-Pfad kann real getestet werden.",
          OffsetDateTime.now().toString(),"40k37e56-macos-ready",arch,false,System.currentTimeMillis()-started);
      }
      String brew=resolveTool("brew");
      if(brew==null || brew.isBlank()) {
        return new ToolchainInstallResult(false,"HOMEBREW_REQUIRED",
          "macOS-Vorbereitung 40k37e56: Homebrew wurde nicht gefunden. Benoetigt werden libimobiledevice, ideviceinstaller, Python 3 und die GAM-eigene pymobiledevice3-Runtime. Architektur: "+arch+".",
          OffsetDateTime.now().toString(),"40k37e56-macos-prepared",arch,false,System.currentTimeMillis()-started);
      }
      if(!missing.isEmpty()) {
        log.info("[DISCOVERY][40k37e56][APPLE_MOBILE][MACOS_TOOLCHAIN] action=BREW_INSTALL arch={} missing={}",arch,missing);
        CommandResult br=run(900,brew,"install","libimobiledevice","ideviceinstaller","python@3.13");
        if(!br.ok()) return new ToolchainInstallResult(false,"BREW_INSTALL_FAILED",
          "Homebrew konnte die macOS-Apple-Basis nicht vollstaendig installieren: "+firstNonBlank(br.stderr(),br.stdout(),"keine Detailmeldung").replaceAll("\s+"," ").trim(),
          OffsetDateTime.now().toString(),"40k37e56-macos-brew",arch,false,System.currentTimeMillis()-started);
      }
      Files.createDirectories(runtime);
      String py=resolveTool("python3");
      if(py==null || py.isBlank()) {
        // Homebrew-Pfade explizit pruefen, falls die gestartete GAM-Shell ihren PATH noch nicht aktualisiert hat.
        for(String candidate:List.of("/opt/homebrew/bin/python3","/usr/local/bin/python3"))
          if(Files.isRegularFile(Path.of(candidate))){ py=candidate; break; }
      }
      if(py==null || py.isBlank()) return new ToolchainInstallResult(false,"PYTHON3_MISSING",
        "Die macOS-Apple-Basis ist vorbereitet, aber python3 wurde noch nicht gefunden. GAM nach der Homebrew-Installation neu starten.",
        OffsetDateTime.now().toString(),"40k37e56-macos-python",arch,false,System.currentTimeMillis()-started);
      if(!Files.isRegularFile(venv.resolve("bin").resolve("python3"))) {
        CommandResult vr=run(180,py,"-m","venv",venv.toString());
        if(!vr.ok()) return new ToolchainInstallResult(false,"PMD3_VENV_FAILED",
          "Die GAM-eigene macOS-Python-Umgebung konnte nicht angelegt werden: "+firstNonBlank(vr.stderr(),vr.stdout(),"keine Detailmeldung"),
          OffsetDateTime.now().toString(),"40k37e56-macos-venv",arch,false,System.currentTimeMillis()-started);
      }
      Path vp=venv.resolve("bin").resolve("python3");
      CommandResult pi=run(900,vp.toString(),"-m","pip","install","--disable-pip-version-check","pymobiledevice3==11.16.3");
      if(!pi.ok() || !Files.isRegularFile(pmd)) return new ToolchainInstallResult(false,"PMD3_INSTALL_FAILED",
        "pymobiledevice3 11.16.3 konnte in der GAM-eigenen macOS-Runtime nicht installiert werden: "+firstNonBlank(pi.stderr(),pi.stdout(),"keine Detailmeldung").replaceAll("\s+"," ").trim(),
        OffsetDateTime.now().toString(),"40k37e56-macos-pmd3",arch,false,System.currentTimeMillis()-started);
      log.info("[DISCOVERY][40k37e56][APPLE_MOBILE][MACOS_TOOLCHAIN] result=PREPARED arch={} pmd3={} realTest=pending",arch,pmd);
      return new ToolchainInstallResult(true,"MACOS_PREPARED_REALTEST_PENDING",
        "macOS-Apple-Basis und pymobiledevice3 11.16.3 sind vorbereitet. Intel und Apple Silicon werden ueber die jeweilige native Homebrew-/Python-Architektur unterstuetzt; der reale iPad-Test steht noch aus.",
        OffsetDateTime.now().toString(),"40k37e56-macos-prepared",pmd.toString(),false,System.currentTimeMillis()-started);
    } catch(Exception e) {
      return new ToolchainInstallResult(false,"MACOS_PREPARE_FAILED","macOS-Apple-Vorbereitung fehlgeschlagen: "+firstNonBlank(e.getMessage(),e.toString()),OffsetDateTime.now().toString(),"40k37e56-macos","",false,System.currentTimeMillis()-started);
    }
  }

  /** 40k37e55: Linux besitzt absichtlich keine portable Windows-artige EXE-Toolchain.
   * Die systemnahen Pairing-/usbmuxd-Komponenten bleiben Eigentum des Paketmanagers.
   * Dieser erste Linux-Build erkennt Distribution/Paketmanager und liefert den passenden,
   * explizit vom Benutzer auszufuehrenden Installationsweg. GAM veraendert das System hier
   * noch nicht selbst und umgeht weder sudo/polkit noch die Paketdatenbank. */
  /** 40k37e56c-fix12: gemeinsamer Linux-Unterbau fuer Apple/CoreDevice.
   * Die Distribution entscheidet nur ueber die systemnahen Pakete. pymobiledevice3 liegt
   * distributionsunabhaengig in derselben GAM-eigenen linux-venv. Dadurch funktionieren
   * Debian/Ubuntu, Arch/CachyOS, Fedora/RHEL, openSUSE und Alpine ueber denselben Oberbau. */
  private ToolchainInstallResult linuxToolchainPackageManagerE55(long started) {
    try {
      String os=System.getProperty("os.name","").toLowerCase(Locale.ROOT);
      if(!os.contains("linux"))
        return new ToolchainInstallResult(false,"UNSUPPORTED","Die Apple-Mobile-Toolchain wird auf dieser Plattform noch nicht automatisch bereitgestellt.",OffsetDateTime.now().toString(),"40k37e56c-fix12","",false,System.currentTimeMillis()-started);

      String manager="";
      for(String candidate:List.of("pacman","apt","dnf","zypper","yum","apk")) {
        CommandResult r=run(4,"sh","-lc","command -v "+candidate+" 2>/dev/null");
        if(r.ok()&&!r.stdout().trim().isBlank()){ manager=candidate; break; }
      }
      String distro="linux";
      CommandResult dr=run(4,"sh","-lc",". /etc/os-release 2>/dev/null; printf '%s' \"${PRETTY_NAME:-${ID:-Linux}}\"");
      if(dr.ok()&&!dr.stdout().trim().isBlank()) distro=dr.stdout().trim();

      List<String> missing=new ArrayList<>();
      for(String tool:List.of("idevice_id","ideviceinfo","idevicepair","idevicediagnostics","ideviceinstaller"))
        if(resolveTool(tool)==null) missing.add(tool);
      boolean usbmuxd=run(4,"sh","-lc","command -v usbmuxd >/dev/null 2>&1").ok();

      Path gamRoot=findGamRoot().toAbsolutePath().normalize();
      Path runtime=gamRoot.resolve("tools").resolve("apple-coredevice").resolve("runtime");
      Path venv=runtime.resolve("linux-venv");
      Path pmd=venv.resolve("bin").resolve("pymobiledevice3");
      Path resolved=resolveCoreDevicePmd3E55h(runtime);
      boolean pmdReady=resolved!=null && Files.exists(resolved) && !Files.isDirectory(resolved);

      log.info("[DISCOVERY][40k37e56c-fix14][APPLE_MOBILE][LINUX_BOOTSTRAP] event=START distro={} manager={} baseMissing={} usbmuxd={} python3={} gamVenv={} expectedPmd3={} resolvedPmd3={} pmdReady={}",
        distro,firstNonBlank(manager,"unknown"),missing,usbmuxd,resolveTool("python3"),venv,pmd,resolved,pmdReady);

      // fix14: PMD3 ist eine GAM-lokale Python-Runtime und benoetigt keine Root-Rechte.
      // Sie wird deshalb IMMER zuerst eingerichtet/repariert. Fehlende optionale Apple-CLI-
      // Werkzeuge (z.B. ideviceinstaller/diagnostics) duerfen keine wiederholte pkexec-Abfrage
      // im Pairing ausloesen. Systempakete werden erst danach und nur fuer wirklich fehlende
      // Lockdown-Basis betrachtet.
      if(!pmdReady) {
        appleTrace("FIX14 PMD3 PERSISTENT RUNTIME","event=ENSURE_START, venv="+traceValue(venv.toString())+", expected="+traceValue(pmd.toString()));
        ToolchainInstallResult pr=ensureLinuxPmd3RuntimeFix12(started,distro,manager,runtime,venv,pmd);
        appleTrace("FIX14 PMD3 PERSISTENT RUNTIME","event=ENSURE_RESULT, success="+pr.success()+", state="+traceValue(pr.state())+", executable="+traceValue(pr.targetDirectory()));
        if(!pr.success()) return pr;
        resolved=resolveCoreDevicePmd3E55h(runtime);
        pmdReady=resolved!=null && Files.exists(resolved) && !Files.isDirectory(resolved);
        if(!pmdReady) return new ToolchainInstallResult(false,"PMD3_RESOLVE_AFTER_INSTALL_FAILED","pymobiledevice3 wurde installiert, der persistente GAM-Pfad ist danach aber nicht aufloesbar: "+pmd,OffsetDateTime.now().toString(),"40k37e56c-fix14-linux-pmd3",manager,false,System.currentTimeMillis()-started);
        appleTrace("FIX14 PMD3 PERSISTENT RUNTIME","event=READY, executable="+traceValue(resolved.toString()));
      }

      // Wichtigster fix12-Fall: libimobiledevice ist bereits komplett vorhanden (wie auf
      // CachyOS im isolierten Test), aber die CoreDevice-Python-Runtime fehlt. Dann NICHT
      // erneut Systempakete installieren, sondern direkt die GAM-venv herstellen.
      if(missing.isEmpty() && usbmuxd && !pmdReady) {
        ToolchainInstallResult r=ensureLinuxPmd3RuntimeFix12(started,distro,manager,runtime,venv,pmd);
        if(!r.success()) return r;
        resolved=resolveCoreDevicePmd3E55h(runtime);
        pmdReady=resolved!=null && Files.exists(resolved) && !Files.isDirectory(resolved);
      }
      if(missing.isEmpty() && usbmuxd && pmdReady) {
        log.info("[DISCOVERY][40k37e56c-fix12][APPLE_MOBILE][LINUX_BOOTSTRAP] event=READY distro={} manager={} pmd3={}",distro,firstNonBlank(manager,"unknown"),resolved);
        return new ToolchainInstallResult(true,"SYSTEM_PACKAGES_AND_PMD3_READY",
          "Linux-Apple-Basis und GAM-CoreDevice-Runtime sind bereit ("+distro+"). pymobiledevice3 11.16.3 wird isoliert aus der GAM-Runtime bzw. einem vorhandenen kompatiblen Pfad verwendet.",
          OffsetDateTime.now().toString(),"40k37e56c-fix12-linux-ready",resolved.toString(),false,System.currentTimeMillis()-started);
      }

      // Fehlende systemnahe Basis breit nach Paketmanager abdecken. Autorisierung bleibt
      // sichtbar ueber polkit/pkexec; GAM selbst wird nicht als root gestartet.
      boolean pkexec=run(4,"sh","-lc","command -v pkexec >/dev/null 2>&1").ok();
      if(!pkexec || manager.isBlank()) {
        return new ToolchainInstallResult(false,"PACKAGE_MANAGER_REQUIRED",
          "Linux-Apple-Basis unvollstaendig. Fehlend: "+String.join(", ",missing)+(usbmuxd?"":"; usbmuxd")+". Paketmanager="+firstNonBlank(manager,"unbekannt")+", pkexec="+pkexec+".",
          OffsetDateTime.now().toString(),"40k37e56c-fix12-linux-system",manager,false,System.currentTimeMillis()-started);
      }

      List<String> cmd=new ArrayList<>();
      cmd.add("pkexec");
      switch(manager) {
        case "pacman" -> { Collections.addAll(cmd,"pacman","-S","--needed","--noconfirm","libimobiledevice","usbmuxd","ideviceinstaller","python","python-pip"); }
        case "apt" -> { Collections.addAll(cmd,"apt-get","install","-y","libimobiledevice-utils","usbmuxd","ideviceinstaller","python3-venv","python3-pip"); }
        case "dnf" -> { Collections.addAll(cmd,"dnf","install","-y","libimobiledevice","usbmuxd","ideviceinstaller","python3","python3-pip"); }
        case "yum" -> { Collections.addAll(cmd,"yum","install","-y","libimobiledevice","usbmuxd","ideviceinstaller","python3","python3-pip"); }
        case "zypper" -> { Collections.addAll(cmd,"zypper","--non-interactive","install","libimobiledevice-utils","usbmuxd","ideviceinstaller","python3","python3-pip"); }
        case "apk" -> { Collections.addAll(cmd,"apk","add","libimobiledevice","usbmuxd","ideviceinstaller","python3","py3-pip"); }
        default -> { }
      }
      if(cmd.size()==1) return new ToolchainInstallResult(false,"PACKAGE_MANAGER_REQUIRED","Paketmanager wird erkannt, aber noch nicht automatisch unterstuetzt: "+manager,OffsetDateTime.now().toString(),"40k37e56c-fix12-linux-system",manager,false,System.currentTimeMillis()-started);

      log.info("[DISCOVERY][40k37e56c-fix12][APPLE_MOBILE][LINUX_BOOTSTRAP] event=PACKAGE_MANAGER_START distro={} manager={} missing={}",distro,manager,missing);
      CommandResult install=run(900,cmd.toArray(String[]::new));
      if(!install.ok()) {
        String err=firstNonBlank(install.stderr(),install.stdout(),"Paketinstallation fehlgeschlagen.");
        if(err.length()>700) err=err.substring(0,700)+"…";
        return new ToolchainInstallResult(false,"PACKAGE_MANAGER_INSTALL_FAILED","Linux-Apple-Basis konnte ueber "+manager+" nicht vollstaendig installiert werden: "+err.replaceAll("\\s+"," ").trim(),OffsetDateTime.now().toString(),"40k37e56c-fix12-linux-system",manager,false,System.currentTimeMillis()-started);
      }

      List<String> remain=new ArrayList<>();
      for(String tool:List.of("idevice_id","ideviceinfo","idevicepair")) if(resolveTool(tool)==null) remain.add(tool);
      boolean usbmuxdNow=run(4,"sh","-lc","command -v usbmuxd >/dev/null 2>&1").ok();
      if(!remain.isEmpty() || !usbmuxdNow) return new ToolchainInstallResult(false,"PACKAGE_MANAGER_PARTIAL","Paketinstallation abgeschlossen, aber weiterhin fehlend: "+remain+(usbmuxdNow?"":"; usbmuxd"),OffsetDateTime.now().toString(),"40k37e56c-fix12-linux-system",manager,false,System.currentTimeMillis()-started);

      ToolchainInstallResult pmdResult=ensureLinuxPmd3RuntimeFix12(started,distro,manager,runtime,venv,pmd);
      if(!pmdResult.success()) return pmdResult;
      Path finalPmd=resolveCoreDevicePmd3E55h(runtime);
      if(finalPmd==null || !Files.exists(finalPmd) || Files.isDirectory(finalPmd)) return new ToolchainInstallResult(false,"PMD3_VALIDATE_FAILED","pymobiledevice3 wurde vorbereitet, ist fuer GAM aber noch nicht aufloesbar.",OffsetDateTime.now().toString(),"40k37e56c-fix12-linux-pmd3",manager,false,System.currentTimeMillis()-started);
      return new ToolchainInstallResult(true,"SYSTEM_PACKAGES_AND_PMD3_READY","Linux-Apple-Basis und pymobiledevice3 11.16.3 sind bereit ("+distro+").",OffsetDateTime.now().toString(),"40k37e56c-fix12-linux-ready",finalPmd.toString(),false,System.currentTimeMillis()-started);
    } catch(Exception e) {
      return new ToolchainInstallResult(false,"LINUX_BOOTSTRAP_FAILED","Linux-Apple-Unterbau fehlgeschlagen: "+firstNonBlank(e.getMessage(),e.toString()),OffsetDateTime.now().toString(),"40k37e56c-fix12-linux","",false,System.currentTimeMillis()-started);
    }
  }

  private ToolchainInstallResult ensureLinuxPmd3RuntimeFix12(long started,String distro,String manager,Path runtime,Path venv,Path pmd) {
    try {
      Files.createDirectories(runtime);
      String python=resolveTool("python3");
      if(python==null || python.isBlank()) python=resolveTool("python");
      if(python==null || python.isBlank()) return new ToolchainInstallResult(false,"PYTHON3_MISSING","Python 3 fehlt; die GAM-eigene CoreDevice-Runtime kann nicht angelegt werden.",OffsetDateTime.now().toString(),"40k37e56c-fix12-linux-pmd3",manager,false,System.currentTimeMillis()-started);

      Path vpy=venv.resolve("bin").resolve("python3");
      if(!Files.isRegularFile(vpy)) vpy=venv.resolve("bin").resolve("python");
      if(!Files.isRegularFile(vpy)) {
        log.info("[DISCOVERY][40k37e56c-fix12][APPLE_MOBILE][LINUX_BOOTSTRAP] event=VENV_CREATE distro={} python={} venv={}",distro,python,venv);
        CommandResult vr=run(180,python,"-m","venv",venv.toString());
        if(!vr.ok()) return new ToolchainInstallResult(false,"PMD3_VENV_FAILED","GAM-linux-venv konnte nicht angelegt werden: "+firstNonBlank(vr.stderr(),vr.stdout(),"unbekannter Fehler"),OffsetDateTime.now().toString(),"40k37e56c-fix12-linux-pmd3",manager,false,System.currentTimeMillis()-started);
        vpy=venv.resolve("bin").resolve("python3");
        if(!Files.isRegularFile(vpy)) vpy=venv.resolve("bin").resolve("python");
      }
      if(!Files.isRegularFile(vpy)) return new ToolchainInstallResult(false,"PMD3_VENV_FAILED","GAM-linux-venv besitzt keinen Python-Interpreter: "+venv,OffsetDateTime.now().toString(),"40k37e56c-fix12-linux-pmd3",manager,false,System.currentTimeMillis()-started);

      CommandResult pipCheck=run(30,vpy.toString(),"-m","pip","--version");
      if(!pipCheck.ok()) {
        CommandResult ensure=run(120,vpy.toString(),"-m","ensurepip","--upgrade");
        if(!ensure.ok()) return new ToolchainInstallResult(false,"PIP_MISSING","pip fehlt in der GAM-linux-venv: "+firstNonBlank(ensure.stderr(),ensure.stdout(),"ensurepip nicht verfuegbar"),OffsetDateTime.now().toString(),"40k37e56c-fix12-linux-pmd3",manager,false,System.currentTimeMillis()-started);
      }
      log.info("[DISCOVERY][40k37e56c-fix12][APPLE_MOBILE][LINUX_BOOTSTRAP] event=PMD3_INSTALL distro={} venv={} version=11.16.3",distro,venv);
      CommandResult pi=run(900,vpy.toString(),"-m","pip","install","--disable-pip-version-check","pymobiledevice3==11.16.3");
      if(!pi.ok() || !Files.isRegularFile(pmd)) {
        String err=firstNonBlank(pi.stderr(),pi.stdout(),"pymobiledevice3 wurde nach pip-Installation nicht gefunden.");
        if(err.length()>700) err=err.substring(0,700)+"…";
        return new ToolchainInstallResult(false,"PMD3_INSTALL_FAILED","pymobiledevice3 11.16.3 konnte nicht in der GAM-linux-venv installiert werden: "+err.replaceAll("\\s+"," ").trim(),OffsetDateTime.now().toString(),"40k37e56c-fix12-linux-pmd3",manager,false,System.currentTimeMillis()-started);
      }
      CommandResult verify=run(30,pmd.toString(),"-h");
      if(!verify.ok()) return new ToolchainInstallResult(false,"PMD3_VALIDATE_FAILED","pymobiledevice3 ist vorhanden, der Funktionsaufruf schlug aber fehl: "+firstNonBlank(verify.stderr(),verify.stdout(),"keine Detailmeldung"),OffsetDateTime.now().toString(),"40k37e56c-fix15-linux-pmd3",manager,false,System.currentTimeMillis()-started);
      appleTrace("FIX15 PMD3 VALIDATION", "event=READY, executable="+pmd+", probe=-h");
      log.info("[DISCOVERY][40k37e56c-fix12][APPLE_MOBILE][LINUX_BOOTSTRAP] event=PMD3_READY distro={} manager={} executable={} version={}",distro,firstNonBlank(manager,"unknown"),pmd,verify.stdout().replaceAll("\\s+"," ").trim());
      return new ToolchainInstallResult(true,"PMD3_READY","pymobiledevice3 11.16.3 ist in der GAM-eigenen Linux-Runtime bereit.",OffsetDateTime.now().toString(),"40k37e56c-fix12-linux-pmd3",pmd.toString(),false,System.currentTimeMillis()-started);
    } catch(Exception e) {
      return new ToolchainInstallResult(false,"PMD3_BOOTSTRAP_FAILED","GAM-CoreDevice-Runtime konnte nicht vorbereitet werden: "+firstNonBlank(e.getMessage(),e.toString()),OffsetDateTime.now().toString(),"40k37e56c-fix12-linux-pmd3",manager,false,System.currentTimeMillis()-started);
    }
  }

  /** 40k37c45: bestimmt den Standalone-Root bevorzugt aus einem expliziten Laufzeitanker.
   * Die Startskripte setzen GAM_ROOT auf den echten Standalone-Ordner. Damit hängt die
   * Toolchain-Installation nicht mehr davon ab, welches Arbeitsverzeichnis Maven bzw.
   * Spring Boot dem Backend-JVM-Prozess gibt. Jeder explizite Wert wird strikt validiert. */
  private Path findGamRoot() {
    // 1. Expliziter, von den GAM-Startskripten gesetzter Root.
    for(String explicit : List.of(
        System.getProperty("gam.root", ""),
        System.getenv().getOrDefault("GAM_ROOT", ""))) {
      if(explicit==null || explicit.isBlank()) continue;
      try {
        Path p=Path.of(explicit).toAbsolutePath().normalize();
        if(isGamRoot(p)) return p;
        throw new IllegalStateException("Expliziter GAM_ROOT ist ungültig (backend/frontend fehlen): "+p);
      } catch(IllegalStateException e) { throw e; }
      catch(Exception e) { throw new IllegalStateException("Expliziter GAM_ROOT ist nicht verwendbar: "+explicit,e); }
    }

    // 2. Kompatibilitätsfallback für Starts ohne aktualisierte Skripte.
    try {
      Path cwd = Path.of(System.getProperty("user.dir",".")).toAbsolutePath().normalize();
      if(isGamRoot(cwd)) return cwd;
      Path parent = cwd.getParent();
      if(parent!=null && "backend".equalsIgnoreCase(String.valueOf(cwd.getFileName())) && isGamRoot(parent)) return parent;
    } catch(Exception ignored) {}

    // 3. Letzter lesender Fallback über den tatsächlichen Code-Standort.
    try {
      URI location = AppleMobileDiscoveryService.class.getProtectionDomain().getCodeSource().getLocation().toURI();
      Path code = Path.of(location).toAbsolutePath().normalize();
      Path p = Files.isRegularFile(code) ? code.getParent() : code;
      for(int i=0; p!=null && i<12; i++, p=p.getParent()) {
        if(isGamRoot(p)) return p;
        if("backend".equalsIgnoreCase(String.valueOf(p.getFileName())) && p.getParent()!=null && isGamRoot(p.getParent())) return p.getParent();
      }
    } catch(Exception ignored) {}

    throw new IllegalStateException("GAM-Root konnte nicht eindeutig bestimmt werden. Erwartet wird der Standalone-Ordner mit backend und frontend; backend/tools ist niemals ein zulässiges Ziel.");
  }

  private static boolean isGamRoot(Path p) {
    return p!=null && Files.isDirectory(p.resolve("backend")) && Files.isDirectory(p.resolve("frontend"));
  }
  private static Path findToolDirectory(Path root) throws Exception {
    try(var walk=Files.walk(root,5)) {
      for(Path p:walk.filter(Files::isDirectory).toList()) if(Files.isRegularFile(p.resolve("idevice_id.exe"))&&Files.isRegularFile(p.resolve("idevicepair.exe"))&&Files.isRegularFile(p.resolve("ideviceinfo.exe"))) return p;
    }
    return null;
  }
  private static void unzipSafely(Path zip,Path target) throws Exception {
    try(ZipInputStream zin=new ZipInputStream(Files.newInputStream(zip))) { ZipEntry e; byte[] buf=new byte[65536];
      while((e=zin.getNextEntry())!=null) { Path out=target.resolve(e.getName()).normalize(); if(!out.startsWith(target)) throw new SecurityException("Unsicherer ZIP-Pfad erkannt.");
        if(e.isDirectory()) Files.createDirectories(out); else { Files.createDirectories(out.getParent()); try(var os=Files.newOutputStream(out)){ int n; while((n=zin.read(buf))>0) os.write(buf,0,n); } }
      }
    }
  }
  private static String sha256(Path file) throws Exception { MessageDigest md=MessageDigest.getInstance("SHA-256"); try(var in=Files.newInputStream(file)){byte[] b=new byte[65536];int n;while((n=in.read(b))>0)md.update(b,0,n);} StringBuilder s=new StringBuilder();for(byte x:md.digest())s.append(String.format("%02x",x));return s.toString(); }
  private static void deleteRecursively(Path p) throws Exception { if(p==null||!Files.exists(p))return; if(Files.isDirectory(p)){try(var st=Files.list(p)){for(Path c:st.toList())deleteRecursively(c);}} Files.deleteIfExists(p); }

  /** 40k37c45: GAM-eigene Apple-Toolchain wird nur noch aus dem kanonischen
   * <GAM>/tools/libimobiledevice geladen. Ein historischer
   * <GAM>/backend/tools/libimobiledevice-Pfad wird bewusst nicht verwendet. */
  private boolean commandExists(String command) { return resolveTool(command) != null; }

  private String resolveTool(String command) {
    String os = System.getProperty("os.name","").toLowerCase(Locale.ROOT);
    if (!os.contains("win")) {
      CommandResult r=run(6,"sh","-lc","command -v '"+command.replace("'","")+"'");
      return r.ok()&&!r.stdout().trim().isBlank()?r.stdout().trim().split("\\R")[0]:null;
    }
    String exe=command.toLowerCase(Locale.ROOT).endsWith(".exe")?command:command+".exe";

    // 1. Ausschließlich die kanonische portable GAM-Toolchain.
    try {
      Path root=findGamRoot();
      for(String rel : List.of("tools\\libimobiledevice", "tools\\libimobiledevice\\bin", "tools\\libimobiledevice\\ideviceinstaller", "tools\\libimobiledevice\\ideviceinstaller\\bin")) {
        Path x=root.resolve(rel).resolve(exe);
        if(Files.isRegularFile(x)) return x.toAbsolutePath().normalize().toString();
      }
      Path legacy=root.resolve("backend").resolve("tools").resolve("libimobiledevice").resolve(exe);
      Path legacyBin=root.resolve("backend").resolve("tools").resolve("libimobiledevice").resolve("bin").resolve(exe);
      if(Files.isRegularFile(legacy)||Files.isRegularFile(legacyBin))
        log.warn("Apple-Mobile-Toolchain 40k37c47: Legacy-/Fehlpfad unter backend/tools/libimobiledevice erkannt und bewusst ignoriert. Erwartet wird {}",root.resolve("tools").resolve("libimobiledevice"));
    } catch(Exception e) {
      log.debug("Apple-Mobile-Toolchain: GAM-Root nicht auflösbar: {}",e.getMessage());
    }

    // 2. PATH-Kompatibilität bleibt erhalten, aber niemals für Dateien innerhalb
    //    des GAM-Backend-Baums. Damit kann ein alter backend/tools-Bestand auch dann
    //    nicht wieder als "systemweit" erscheinen, wenn ein Startskript oder die
    //    Windows-Umgebung ihn in PATH aufgenommen hat.
    CommandResult where=run(6,"cmd.exe","/c","where",exe);
    if(where.ok()&&!where.stdout().trim().isBlank()) {
      Path root; try { root=findGamRoot(); } catch(Exception ignored) { root=null; }
      Path backendRoot = root==null ? null : root.resolve("backend").toAbsolutePath().normalize();
      Path canonicalRoot = root==null ? null : root.resolve("tools").resolve("libimobiledevice").toAbsolutePath().normalize();
      for(String line : where.stdout().trim().split("\\R")) {
        try {
          Path candidate=Path.of(line.trim()).toAbsolutePath().normalize();
          if(root!=null) {
            // GAM-eigene Werkzeuge sind ausschließlich aus dem kanonischen Root erlaubt.
            if(candidate.startsWith(backendRoot)) {
              log.warn("Apple-Mobile-Toolchain 40k37c47: PATH-Treffer innerhalb des GAM-Backends wird ignoriert: {}",candidate);
              continue;
            }
            // Falls PATH auf die kanonische Toolchain zeigt, ist das natürlich gültig.
            if(candidate.startsWith(canonicalRoot) && Files.isRegularFile(candidate)) return candidate.toString();
            // Andere Dateien irgendwo innerhalb des GAM-Roots werden ebenfalls nicht als
            // vermeintlich systemweite Installation akzeptiert.
            if(candidate.startsWith(root.toAbsolutePath().normalize())) continue;
          }
          if(Files.isRegularFile(candidate)) return candidate.toString();
        } catch(Exception ignored) {}
      }
    }

    // 3. Nur echte externe Installationspfade; keine cwd-/backend-Entwicklerpfade mehr.
    String pf=System.getenv("ProgramFiles"), pf86=System.getenv("ProgramFiles(x86)");
    List<String> dirs=new ArrayList<>(List.of(
      "C:\\msys64\\mingw64\\bin", "C:\\msys64\\ucrt64\\bin",
      System.getProperty("user.home","")+"\\scoop\\apps\\libimobiledevice\\current\\bin"));
    if(pf!=null) dirs.add(pf+"\\libimobiledevice");
    if(pf86!=null) dirs.add(pf86+"\\libimobiledevice");
    for(String d:dirs){ Path x=Path.of(d,exe); if(Files.isRegularFile(x)) return x.toAbsolutePath().normalize().toString(); }
    return null;
  }

  /** 40k37c47: detaillierter Trace fuer die bisher im Depot-Trace unsichtbare
   * Pairing-/Lockdown-Kette. Exitcode ist im bestehenden CommandResult nicht separat
   * vorhanden; ok/stdout/stderr bilden denselben fachlichen Befund ab. */
  private CommandResult runToolTraced(int timeoutSeconds,String event,String command,String... args){
    long performanceStarted=System.currentTimeMillis();
    String tool=resolveTool(command);
    appleTrace(event+" START", "tool="+traceValue(tool)+", args="+traceArgs(args)+", timeoutSec="+timeoutSeconds);
    if(tool==null){
      CommandResult r=new CommandResult(false,"",command+" nicht gefunden");
      appleTrace(event+" RESULT", "ok=false, stderr="+traceValue(r.stderr()));
      performanceTool(event,command,timeoutSeconds,performanceStarted,r);
      return r;
    }
    String[] all=new String[args.length+1]; all[0]=tool; System.arraycopy(args,0,all,1,args.length);
    CommandResult r=run(timeoutSeconds,all);
    appleTrace(event+" RESULT", "ok="+r.ok()+", stdout="+traceValue(limitTrace(r.stdout()))+", stderr="+traceValue(limitTrace(r.stderr())));
    performanceTool(event,command,timeoutSeconds,performanceStarted,r);
    return r;
  }

  /** fix31: plattformgerechte, rein lokale Support-Prüfung. Auf macOS darf niemals
   * windowsAppleDevicesInstalled() den Zustand des Einrichtungsbuttons bestimmen. */
  private boolean appleSupportReadyFix31(){
    String os=System.getProperty("os.name","").toLowerCase(Locale.ROOT);
    boolean tools=commandExists("idevice_id") && commandExists("ideviceinfo") && commandExists("idevicepair");
    if(os.contains("mac")){
      if(!tools) return false;
      try {
        Path rt=findGamRoot().toAbsolutePath().normalize().resolve("tools").resolve("apple-coredevice").resolve("runtime");
        Path pmd=resolveCoreDevicePmd3E55h(rt);
        return pmd!=null && Files.isRegularFile(pmd);
      } catch(Exception ignored){ return false; }
    }
    if(os.contains("linux")) return tools && run(4,"sh","-lc","command -v usbmuxd >/dev/null 2>&1").ok();
    return windowsAppleDevicesInstalled();
  }

  private void tracePairingEnvironment(String context){
    try {
      boolean pnp=!readWindowsUsbDevices().isEmpty();
      boolean supportReady=appleSupportReadyFix31();
      appleTrace("PAIR ENV", "context="+context+", os="+traceValue(System.getProperty("os.name"))+", pnpAppleUsb="+pnp+", appleDevicesInstalled="+supportReady);
      String id=resolveTool("idevice_id"), pair=resolveTool("idevicepair"), info=resolveTool("ideviceinfo");
      appleTrace("PAIR TOOL PATHS", "context="+context+", idevice_id="+traceValue(id)+", idevicepair="+traceValue(pair)+", ideviceinfo="+traceValue(info));
      if(System.getProperty("os.name","").toLowerCase(Locale.ROOT).contains("win")){
        // 40k37c47: keine verschachtelte PowerShell-Stringinterpolation mehr. Die c45/c46-
        // Variante konnte auf Windows als Syntaxfehler enden. Der -f-Operator erzeugt
        // dieselbe Diagnoseausgabe ohne Parser-Mehrdeutigkeit.
        CommandResult svc=run(8,"powershell.exe","-NoProfile","-NonInteractive","-Command",
          "$ErrorActionPreference='SilentlyContinue'; Get-Service | Where-Object { $_.Name -match 'Apple|Mobile|Bonjour' -or $_.DisplayName -match 'Apple|Mobile Device|Bonjour' } | ForEach-Object { '{0}|{1}|{2}|{3}' -f $_.Name,$_.Status,$_.StartType,$_.DisplayName }");
        appleTrace("PAIR WINDOWS SERVICES", "context="+context+", ok="+svc.ok()+", data="+traceValue(limitTrace(svc.stdout()))+", stderr="+traceValue(limitTrace(svc.stderr())));
        CommandResult proc=run(8,"powershell.exe","-NoProfile","-NonInteractive","-Command",
          "$ErrorActionPreference='SilentlyContinue'; Get-Process | Where-Object { $_.ProcessName -match 'Apple|MobileDevice|usbmux' } | Select-Object -ExpandProperty ProcessName -Unique");
        appleTrace("PAIR WINDOWS PROCESSES", "context="+context+", ok="+proc.ok()+", data="+traceValue(limitTrace(proc.stdout())));
      }
    } catch(Exception e){ appleTrace("PAIR ENV FAILED", "context="+context+", error="+e.getClass().getSimpleName()+":"+traceValue(e.getMessage())); }
  }

  private static String traceArgs(String... args){
    if(args==null||args.length==0)return "[]";
    List<String> out=new ArrayList<>();
    for(int i=0;i<args.length;i++){
      String a=args[i];
      if(i>0 && "-u".equals(args[i-1])) out.add("<udid:"+traceUdid(a)+">"); else out.add(a==null?"":a);
    }
    return out.toString();
  }
  private static String traceUdid(String udid){
    String u=blankToNull(udid); if(u==null)return "<none>";
    return u.length()<=8?"<set>":u.substring(0,4)+"..."+u.substring(u.length()-4);
  }
  private static String limitTrace(String s){
    if(s==null)return ""; String x=s.replace("\r","").trim(); return x.length()>1800?x.substring(0,1800)+"…":x;
  }

  private CommandResult runTool(int timeoutSeconds,String command,String... args){
    String tool=resolveTool(command); if(tool==null) return new CommandResult(false,"",command+" nicht gefunden");
    String[] all=new String[args.length+1]; all[0]=tool; System.arraycopy(args,0,all,1,args.length); return run(timeoutSeconds,all);
  }

  /** Produktkennung -> fachlich lesbarer Modellname. Keine Heuristik: nur bekannte
   * Apple ProductTypes werden aufgeloest; unbekannte bleiben transparent erhalten. */
  private static String appleModelDisplayName(String productType){
    if(productType==null) return "";
    return switch(productType.trim()){
      case "iPad11,6" -> "iPad 8. Generation (Wi-Fi)";
      case "iPad11,7" -> "iPad 8. Generation (Wi-Fi + Cellular)";
      case "iPad13,1" -> "iPad 9. Generation (Wi-Fi)";
      case "iPad13,2" -> "iPad 9. Generation (Wi-Fi + Cellular)";
      case "iPad7,11" -> "iPad 7. Generation (Wi-Fi)";
      case "iPad7,12" -> "iPad 7. Generation (Wi-Fi + Cellular)";
      default -> productType.toLowerCase(Locale.ROOT).startsWith("ipad") ? "iPad ("+productType+")"
        : productType.toLowerCase(Locale.ROOT).startsWith("iphone") ? "iPhone ("+productType+")" : "";
    };
  }

  private static Map<String,String> parseInfo(String text) {
    LinkedHashMap<String,String> out = new LinkedHashMap<>();
    for (String line : text.split("\\R")) {
      int p = line.indexOf(':'); if (p <= 0) continue;
      String k=line.substring(0,p).trim(), v=line.substring(p+1).trim();
      if (!k.isBlank() && !v.isBlank()) out.putIfAbsent(k,v);
    }
    return out;
  }

  private static CommandResult run(int timeoutSeconds, String... command) {
    try {
      Process p = new ProcessBuilder(command).redirectErrorStream(false).start();
      boolean done = p.waitFor(timeoutSeconds, TimeUnit.SECONDS);
      if (!done) { p.destroyForcibly(); return new CommandResult(false,"","Timeout",Integer.MIN_VALUE); }
      String stdout = readAll(p.getInputStream());
      String stderr = readAll(p.getErrorStream());
      int exit=p.exitValue();
      return new CommandResult(exit==0, stdout, stderr,exit);
    } catch (Exception e) { return new CommandResult(false,"",e.getMessage()==null?e.getClass().getSimpleName():e.getMessage(),-1); }
  }
  private static String readAll(java.io.InputStream in) throws Exception {
    StringBuilder b=new StringBuilder();
    try(BufferedReader r=new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))){String line;while((line=r.readLine())!=null)b.append(line).append('\n');}
    return b.toString();
  }
  private record CommandResult(boolean ok,String stdout,String stderr,int exitCode){
    private CommandResult(boolean ok,String stdout,String stderr){ this(ok,stdout,stderr,ok?0:-1); }
  }
  private static String formatExitCode(int code){
    if(code==Integer.MIN_VALUE) return "TIMEOUT";
    return code+"/0x"+String.format(Locale.ROOT,"%08X",code);
  }
  private static String detail(String label,String value){return value==null||value.isBlank()?"":" · "+label+": "+value.replaceAll("\\s+"," ").trim();}
  private static String blankToNull(String s){return s==null||s.isBlank()?null:s.trim();}
  private static String str(Object o){return o==null?null:blankToNull(String.valueOf(o));}
  private static String firstNonBlank(String... values){for(String v:values)if(v!=null&&!v.isBlank())return v.trim();return "";}
  private static void diagnostic(Consumer<DeviceDiscoveryService.DiscoveryDiagnosticEvent> c,String session,String phase,String status,String message,int count){
    if(c!=null)c.accept(new DeviceDiscoveryService.DiscoveryDiagnosticEvent(session,OffsetDateTime.now().toString(),phase,status,message,count));
  }
  public record SoftwareItem(String scopeType,String name,String version,String bundleId,String installPath,String source) {}
  public record SoftwareProbeResult(boolean success,String state,String message,List<SoftwareItem> items,int userCount,int systemCount,long durationMs) {}

}
