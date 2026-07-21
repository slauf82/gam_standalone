package de.kopfzentrum.gam.inventory;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DeviceDiscoveryService {
  private static final Logger log = LoggerFactory.getLogger(DeviceDiscoveryService.class);
  private static final AtomicLong SCAN_SEQUENCE = new AtomicLong();
  private static final Pattern IPV4 = Pattern.compile("(?<![0-9])((?:[0-9]{1,3}\\.){3}[0-9]{1,3})(?![0-9])");
  private static final Pattern MAC = Pattern.compile("(?i)([0-9a-f]{2}(?:[:-][0-9a-f]{2}){5})");
  private static final int HOST_TIMEOUT_MS = 300;
  private final DeviceIdentityConfidenceEngine identityEngine = new DeviceIdentityConfidenceEngine();
  private final DeviceIdentityMergeSettingsRepository mergeSettings;
  private final JdbcTemplate jdbc;
  private final FritzBoxDeviceSource fritzBoxDeviceSource;
  private final HomeAssistantDeviceSource homeAssistantDeviceSource;
  private final TuyaDeviceSource tuyaDeviceSource;
  private final BuiltinDiscoverySettingsRepository builtinSettings;
  private final AdditionalDiscoveryService additionalDiscovery;
  private final SnmpDiscoveryService snmpDiscovery;
  private final DiscoveryRegistrationRepository registrations;
  private Map<String,FritzBoxDeviceSource.FritzDevice> fritzByIp = Map.of();
  private Map<String,FritzBoxDeviceSource.FritzDevice> fritzByMac = Map.of();
  private Map<String,HomeAssistantDeviceSource.HaDevice> haByIp = Map.of();
  private Map<String,HomeAssistantDeviceSource.HaDevice> haByMac = Map.of();
  private Map<String,TuyaDeviceSource.TuyaDevice> tuyaByIp = Map.of();
  private Map<String,TuyaDeviceSource.TuyaDevice> tuyaByMac = Map.of();

  public DeviceDiscoveryService(JdbcTemplate jdbc, FritzBoxDeviceSource fritzBoxDeviceSource, HomeAssistantDeviceSource homeAssistantDeviceSource, TuyaDeviceSource tuyaDeviceSource, BuiltinDiscoverySettingsRepository builtinSettings, AdditionalDiscoveryService additionalDiscovery, SnmpDiscoveryService snmpDiscovery, DiscoveryRegistrationRepository registrations, DeviceIdentityMergeSettingsRepository mergeSettings) { this.jdbc = jdbc; this.fritzBoxDeviceSource = fritzBoxDeviceSource; this.homeAssistantDeviceSource = homeAssistantDeviceSource; this.tuyaDeviceSource = tuyaDeviceSource; this.builtinSettings = builtinSettings; this.additionalDiscovery = additionalDiscovery; this.snmpDiscovery = snmpDiscovery; this.registrations = registrations; this.mergeSettings = mergeSettings; }

  private static String discoveryCallerSummary() {
    return StackWalker.getInstance().walk(stream -> stream
      .filter(frame -> !frame.getClassName().startsWith("java.") && !frame.getClassName().startsWith("org.springframework") && !frame.getClassName().equals(DeviceDiscoveryService.class.getName()))
      .limit(8).map(frame -> frame.getClassName()+"#"+frame.getMethodName()+":"+frame.getLineNumber())
      .reduce((a,b) -> a+" <- "+b).orElse("unknown"));
  }

  public Map<String,Object> capabilities() {
    LinkedHashMap<String,Object> result = new LinkedHashMap<>();
    result.put("platform", platform());
    result.put("network", true);
    result.put("powershell", isWindows() && (commandExists("powershell.exe") || commandExists("pwsh.exe")));
    result.put("arp", commandExists(isWindows() ? "arp.exe" : "arp"));
    result.put("ipNeighbor", isLinux() && commandExists("ip"));
    result.put("activeSubnetScan", true);
    result.put("ssdp", true);
    result.put("mdns", true);
    result.put("mdnsMode", "Java UDP Multicast (integriert), externe Werkzeuge nur als Fallback");
    result.put("wsDiscoveryOnvif", true);
    result.put("netbios", System.getProperty("os.name","").toLowerCase().contains("win"));
    result.put("dhcpLeases", true);
    result.put("usbLocal", true);
    result.put("bluetoothLocal", true);
    result.put("dockerLocal", true);
    result.put("reverseDns", true);
    result.put("fritzBoxTr064", true);
    result.put("homeAssistantRestApi", true);
    result.put("tuyaCloudApi", true);
    result.put("snmp", true);
    result.put("snmpMode", "Integrierte SNMP-v1-Systemabfrage (sysDescr, sysObjectID, sysName)");
    result.put("usb", false);
    result.put("bluetooth", false);
    result.put("builtinSources", builtinSettings.load());
    result.put("identityMerge", "40k31c1 Confidence Engine: Gewichtungen und Schwellwerte vollständig einstellbar");
    result.put("note", "40k31c nutzt eine gewichtete Merge-Engine; identische IP-Adressen allein werden nicht mehr automatisch zusammengeführt.");
    return result;
  }

  public List<DiscoveredDevice> scan() {
    log.warn("[DISCOVERY-TRACE] LEGACY_SCAN_METHOD_ENTER thread={} caller={}", Thread.currentThread().getName(), discoveryCallerSummary());
    List<DiscoveredDevice> result = new ArrayList<>();
    scanStreaming(result::add, (progress, phase) -> {});
    return result;
  }

  public void scanStreaming(Consumer<DiscoveredDevice> deviceConsumer, BiConsumer<Integer,String> progressConsumer) {
    scanStreaming(deviceConsumer, progressConsumer, event -> {});
  }

  public void scanStreaming(Consumer<DiscoveredDevice> deviceConsumer, BiConsumer<Integer,String> progressConsumer,
                            Consumer<DiscoveryDiagnosticEvent> diagnosticConsumer) {
    long scanNo = SCAN_SEQUENCE.incrementAndGet();
    long scanStartedNanos = System.nanoTime();
    log.warn("[DISCOVERY-TRACE] ENGINE_SCAN_START scanNo={} thread={} caller={}", scanNo, Thread.currentThread().getName(), discoveryCallerSummary());
    registrations.beginDiscoveryRun();
    LinkedHashMap<String,DiscoveredDevice> seen = new LinkedHashMap<>();
    String now = OffsetDateTime.now().toString();

    String sessionId = UUID.randomUUID().toString();
    diagnostic(diagnosticConsumer, sessionId, "DISCOVERY", "STARTED", "Discovery gestartet", seen.size());
    try {
      progressConsumer.accept(2, "Discovery wird initialisiert (" + platform() + ")");

      List<InterfaceNetwork> networks;
      if (builtinSettings.enabled("LOCAL_ADAPTERS")) {
        diagnostic(diagnosticConsumer, sessionId, "LOCAL_ADAPTERS", "RUNNING", "Lokale Netzwerkadapter werden gelesen", seen.size());
        networks = addLocalInterfaces(seen, now, deviceConsumer);
        diagnostic(diagnosticConsumer, sessionId, "LOCAL_ADAPTERS", "COMPLETED", networks.size() + " lokale IPv4-Netze erkannt", seen.size());
      } else { networks = List.of(); diagnostic(diagnosticConsumer, sessionId, "LOCAL_ADAPTERS", "SKIPPED", "Quelle ist deaktiviert", seen.size()); }

      progressConsumer.accept(4, "FRITZ!Box-Gerätenamen werden optional geladen");
      diagnostic(diagnosticConsumer, sessionId, "FRITZBOX", "RUNNING", "TR-064-Geräteliste wird geprüft", seen.size());
      var fritzResult = fritzBoxDeviceSource.load();
      installFritzIndex(fritzResult.devices());
      diagnostic(diagnosticConsumer, sessionId, "FRITZBOX", fritzResult.status(), fritzResult.message(), seen.size());

      progressConsumer.accept(5, "Home-Assistant-Geräteinformationen werden geladen");
      diagnostic(diagnosticConsumer, sessionId, "HOME_ASSISTANT", "RUNNING", "Home-Assistant-REST-API wird geprüft", seen.size());
      var haResult = homeAssistantDeviceSource.load();
      installHomeAssistantIndex(haResult.devices());
      emitHomeAssistantOnlyDevices(haResult.devices(), seen, now, deviceConsumer);
      diagnostic(diagnosticConsumer, sessionId, "HOME_ASSISTANT", haResult.status(), haResult.message(), seen.size());

      progressConsumer.accept(6, "Smart-Life-/Tuya-Geräteinformationen werden geladen");
      diagnostic(diagnosticConsumer, sessionId, "TUYA", "RUNNING", "Tuya Cloud API wird geprüft", seen.size());
      var tuyaResult = tuyaDeviceSource.load();
      installTuyaIndex(tuyaResult.devices());
      emitTuyaOnlyDevices(tuyaResult.devices(), seen, now, deviceConsumer);
      diagnostic(diagnosticConsumer, sessionId, "TUYA", tuyaResult.status(), tuyaResult.message(), seen.size());

      progressConsumer.accept(7, "Soforttreffer aus der lokalen Nachbartabelle werden geladen");
      if (builtinSettings.enabled("NEIGHBOR")) {
        diagnostic(diagnosticConsumer, sessionId, "NEIGHBOR_INITIAL", "RUNNING", "Erste ARP-/Neighbor-Auswertung läuft", seen.size());
        readNeighborSources(seen, now, deviceConsumer, progressConsumer, 6, 20, false);
        diagnostic(diagnosticConsumer, sessionId, "NEIGHBOR_INITIAL", "COMPLETED", "Erste ARP-/Neighbor-Auswertung abgeschlossen", seen.size());
      } else diagnostic(diagnosticConsumer, sessionId, "NEIGHBOR_INITIAL", "SKIPPED", "Quelle ist deaktiviert", seen.size());

      progressConsumer.accept(22, "Vertiefte lokale IPv4-Suche aktualisiert die Nachbartabelle");
      if (!builtinSettings.enabled("ACTIVE_SCAN")) {
        diagnostic(diagnosticConsumer, sessionId, "ACTIVE_SCAN", "SKIPPED", "Quelle ist deaktiviert", seen.size());
      } else if (networks.isEmpty()) {
        diagnostic(diagnosticConsumer, sessionId, "ACTIVE_SCAN", "SKIPPED", "Kein geeignetes lokales IPv4-Netz gefunden", seen.size());
      } else {
        diagnostic(diagnosticConsumer, sessionId, "ACTIVE_SCAN", "RUNNING", "Aktiver Subnetzscan läuft", seen.size());
        activeSubnetScan(networks, seen, now, deviceConsumer, progressConsumer);
        diagnostic(diagnosticConsumer, sessionId, "ACTIVE_SCAN", "COMPLETED", "Aktiver Subnetzscan abgeschlossen", seen.size());
      }

      progressConsumer.accept(78, "Nachbartabellen werden nach dem Netzscan erneut gelesen");
      if (builtinSettings.enabled("NEIGHBOR")) {
        diagnostic(diagnosticConsumer, sessionId, "NEIGHBOR_REFRESH", "RUNNING", "Zweite ARP-/Neighbor-Auswertung läuft", seen.size());
        readNeighborSources(seen, now, deviceConsumer, progressConsumer, 78, 88, false);
        diagnostic(diagnosticConsumer, sessionId, "NEIGHBOR_REFRESH", "COMPLETED", "Zweite ARP-/Neighbor-Auswertung abgeschlossen", seen.size());
      } else diagnostic(diagnosticConsumer, sessionId, "NEIGHBOR_REFRESH", "SKIPPED", "Quelle ist deaktiviert", seen.size());

      progressConsumer.accept(90, "SSDP-/UPnP-Geräte werden gesucht");
      if (builtinSettings.enabled("SSDP")) {
        diagnostic(diagnosticConsumer, sessionId, "SSDP", "RUNNING", "SSDP-/UPnP-Suche läuft", seen.size());
        discoverSsdp(seen, now, deviceConsumer);
        diagnostic(diagnosticConsumer, sessionId, "SSDP", "COMPLETED", "SSDP-/UPnP-Suche abgeschlossen", seen.size());
      } else diagnostic(diagnosticConsumer, sessionId, "SSDP", "SKIPPED", "Quelle ist deaktiviert", seen.size());

      progressConsumer.accept(94, "mDNS-/Bonjour-Geräte werden gesucht");
      if (builtinSettings.enabled("MDNS")) {
        diagnostic(diagnosticConsumer, sessionId, "MDNS", "RUNNING", "mDNS-/Bonjour-Suche laeuft", seen.size());
        String mdnsResult = discoverMdns(seen, now, deviceConsumer);
        String mdnsStatus = mdnsResult.startsWith("COMPLETED") ? "COMPLETED" : "FAILED";
        diagnostic(diagnosticConsumer, sessionId, "MDNS", mdnsStatus, mdnsMessage(mdnsResult), seen.size());
      } else diagnostic(diagnosticConsumer, sessionId, "MDNS", "SKIPPED", "Quelle ist deaktiviert", seen.size());

      progressConsumer.accept(95, "Weitere Discovery-Quellen werden ausgewertet");
      additionalDiscovery.scan(builtinSettings.load(), now, d -> emitIfNew(seen, d, deviceConsumer), diagnosticConsumer, sessionId, seen.size());

      progressConsumer.accept(96, "SNMP-Systeminformationen werden abgefragt");
      if (builtinSettings.enabled("SNMP")) {
        diagnostic(diagnosticConsumer, sessionId, "SNMP", "RUNNING", "SNMP-v1-Identitätsabfrage läuft", seen.size());
        List<String> snmpTargets = seen.values().stream().map(DiscoveredDevice::address).filter(Objects::nonNull).distinct().toList();
        SnmpDiscoveryService.Result snmpResult = snmpDiscovery.scan(snmpTargets, snmp -> {
          DiscoveredDevice device = new DiscoveredDevice(
            "snmp:" + snmp.address(), snmp.name(), snmp.type(), snmp.address(), null,
            "SNMP v1", "ERKANNT", snmp.manufacturer(), snmp.objectId(), now,
            isRegistered(snmp.address(), null, snmp.name())
          );
          emitIfNew(seen, device, deviceConsumer);
        });
        diagnostic(diagnosticConsumer, sessionId, "SNMP", "COMPLETED", snmpResult.message(), seen.size());
      } else diagnostic(diagnosticConsumer, sessionId, "SNMP", "SKIPPED", "Quelle ist deaktiviert", seen.size());

      progressConsumer.accept(97, "DNS-Namen werden aufgelöst");
      if (builtinSettings.enabled("DNS_NAMES")) {
        diagnostic(diagnosticConsumer, sessionId, "DNS_NAMES", "RUNNING", "Reverse-DNS-Namensauflösung läuft", seen.size());
        enrichReverseDns(seen, deviceConsumer);
        diagnostic(diagnosticConsumer, sessionId, "DNS_NAMES", "COMPLETED", "Reverse-DNS-Namensauflösung abgeschlossen", seen.size());
      } else diagnostic(diagnosticConsumer, sessionId, "DNS_NAMES", "SKIPPED", "Quelle ist deaktiviert", seen.size());

      progressConsumer.accept(98, "Gerätenamen und Registrierungsstatus werden konsolidiert");
      diagnostic(diagnosticConsumer, sessionId, "FINALIZE", "RUNNING", "Ergebnisse werden konsolidiert", seen.size());
      diagnostic(diagnosticConsumer, sessionId, "FINALIZE", "COMPLETED", "Ergebnisse konsolidiert", seen.size());
      progressConsumer.accept(100, "Gerätesuche abgeschlossen");
      diagnostic(diagnosticConsumer, sessionId, "DISCOVERY", "COMPLETED", "Globaler Abschluss erreicht", seen.size());
      log.warn("[DISCOVERY-TRACE] ENGINE_SCAN_COMPLETE scanNo={} engineSessionId={} uniqueDevices={} durationMs={} thread={}", scanNo, sessionId, seen.size(), (System.nanoTime()-scanStartedNanos)/1_000_000L, Thread.currentThread().getName());
    } catch (RuntimeException ex) {
      log.error("[DISCOVERY-TRACE] ENGINE_SCAN_FAILED scanNo={} engineSessionId={} uniqueDevices={} durationMs={} thread={}", scanNo, sessionId, seen.size(), (System.nanoTime()-scanStartedNanos)/1_000_000L, Thread.currentThread().getName(), ex);
      diagnostic(diagnosticConsumer, sessionId, "DISCOVERY", "FAILED", ex.getClass().getSimpleName() + ": " + Objects.toString(ex.getMessage(), "ohne Meldung"), seen.size());
      throw ex;
    }
  }

  private void diagnostic(Consumer<DiscoveryDiagnosticEvent> consumer, String sessionId, String phase, String status, String message, int deviceCount) {
    DiscoveryDiagnosticEvent event = new DiscoveryDiagnosticEvent(sessionId, OffsetDateTime.now().toString(), phase, status, message, deviceCount);
    log.info("[DISCOVERY] session={} phase={} status={} devices={} message={}", sessionId, phase, status, deviceCount, message);
    consumer.accept(event);
  }

  public record DiscoveryDiagnosticEvent(String sessionId, String timestamp, String phase, String status, String message, int deviceCount) {}

  private List<InterfaceNetwork> addLocalInterfaces(Map<String,DiscoveredDevice> result, String now,
                                                     Consumer<DiscoveredDevice> consumer) {
    List<InterfaceNetwork> networks = new ArrayList<>();
    try {
      Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
      if (interfaces == null) return networks;
      while (interfaces.hasMoreElements()) {
        NetworkInterface ni = interfaces.nextElement();
        if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;
        String mac = formatMac(ni.getHardwareAddress());
        for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
          if (!(ia.getAddress() instanceof Inet4Address address)) continue;
          String ip = address.getHostAddress();
          short prefix = ia.getNetworkPrefixLength();
          if (prefix >= 16 && prefix <= 30) networks.add(new InterfaceNetwork(ip, prefix));
          String name = cleanHost(address.getHostName(), ni.getDisplayName());
          DiscoveredDevice device = new DiscoveredDevice("network:" + ip, name, "Computer / Netzwerkadapter",
            ip, mac, "LOCAL", "ONLINE", null, null, now, isRegistered(ip, mac, name));
          emitIfNew(result, device, consumer);
        }
      }
    } catch (Exception ignored) { }
    return networks;
  }

  private void readNeighborSources(Map<String,DiscoveredDevice> result, String now,
                                   Consumer<DiscoveredDevice> consumer,
                                   BiConsumer<Integer,String> progressConsumer,
                                   int progressStart, int progressEnd, boolean resolveNames) {
    List<CommandSource> sources = new ArrayList<>();
    if (isWindows()) {
      // arp -a liefert unter Windows die schnellsten vollständigen Soforttreffer.
      sources.add(new CommandSource("Windows ARP", List.of("cmd.exe", "/c", "arp -a")));
      String ps = commandExists("powershell.exe") ? "powershell.exe" : (commandExists("pwsh.exe") ? "pwsh.exe" : null);
      if (ps != null) {
        sources.add(new CommandSource("PowerShell Get-NetNeighbor", List.of(ps, "-NoProfile", "-NonInteractive", "-Command",
          "Get-NetNeighbor -AddressFamily IPv4 -ErrorAction SilentlyContinue | Where-Object {$_.State -ne 'Unreachable' -and $_.State -ne 'Incomplete'} | ForEach-Object { \"$($_.IPAddress) $($_.LinkLayerAddress)\" }")));
      }
    } else if (isLinux()) {
      sources.add(new CommandSource("Linux ip neigh", List.of("sh", "-c", "ip -4 neigh show 2>/dev/null")));
      sources.add(new CommandSource("Linux ARP", List.of("sh", "-c", "arp -an 2>/dev/null")));
    } else if (isMac()) {
      sources.add(new CommandSource("macOS ARP", List.of("sh", "-c", "arp -an 2>/dev/null")));
    } else {
      sources.add(new CommandSource("ARP", List.of("sh", "-c", "arp -an 2>/dev/null || ip neigh 2>/dev/null")));
    }

    int completed = 0;
    for (CommandSource source : sources) {
      progressConsumer.accept(progressStart + ((progressEnd - progressStart) * completed / Math.max(1, sources.size())),
        source.label() + " wird ausgewertet");
      runNeighborCommand(source.command(), source.label(), result, now, consumer, resolveNames);
      completed++;
    }
  }

  private void runNeighborCommand(List<String> command, String protocol,
                                  Map<String,DiscoveredDevice> result, String now,
                                  Consumer<DiscoveredDevice> consumer, boolean resolveNames) {
    Process process = null;
    ExecutorService outputReader = Executors.newSingleThreadExecutor(r -> {
      Thread thread = new Thread(r, "gam-discovery-command-reader");
      thread.setDaemon(true);
      return thread;
    });
    try {
      process = new ProcessBuilder(command).redirectErrorStream(true).start();
      Process runningProcess = process;
      Charset charset = isWindows() ? Charset.defaultCharset() : StandardCharsets.UTF_8;
      Future<?> readerTask = outputReader.submit(() -> {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(runningProcess.getInputStream(), charset))) {
          String line;
          while ((line = reader.readLine()) != null) {
            processNeighborLine(line, protocol, result, now, consumer, resolveNames);
          }
        } catch (Exception ex) {
          if (runningProcess.isAlive()) throw new CompletionException(ex);
        }
      });

      boolean completed = process.waitFor(8, TimeUnit.SECONDS);
      if (!completed) {
        log.warn("[DISCOVERY] Neighbor-Befehl {} überschritt 8 Sekunden und wird beendet", protocol);
        process.destroy();
        if (!process.waitFor(500, TimeUnit.MILLISECONDS)) process.destroyForcibly();
      }
      try {
        readerTask.get(1200, TimeUnit.MILLISECONDS);
      } catch (TimeoutException timeout) {
        readerTask.cancel(true);
        log.warn("[DISCOVERY] Ausgabe von {} konnte nach Prozessende nicht vollständig gelesen werden", protocol);
      }
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      if (process != null) process.destroyForcibly();
    } catch (Exception failure) {
      log.warn("[DISCOVERY] Neighbor-Quelle {} fehlgeschlagen: {}", protocol, failure.toString());
      if (process != null) process.destroyForcibly();
    } finally {
      outputReader.shutdownNow();
    }
  }

  private void processNeighborLine(String line, String protocol,
                                   Map<String,DiscoveredDevice> result, String now,
                                   Consumer<DiscoveredDevice> consumer, boolean resolveNames) {
    Matcher ipMatcher = IPV4.matcher(line);
    if (!ipMatcher.find()) return;
    String ip = ipMatcher.group(1);
    if (!validHostIp(ip)) return;
    Matcher macMatcher = MAC.matcher(line);
    String mac = macMatcher.find() ? normalizeMac(macMatcher.group(1)) : null;
    FritzBoxDeviceSource.FritzDevice fritz = findFritz(ip, mac);
    HomeAssistantDeviceSource.HaDevice ha = findHomeAssistant(ip, mac);
    TuyaDeviceSource.TuyaDevice tuya = findTuya(ip, mac);
    String name = tuya != null && !tuya.name().isBlank() ? tuya.name() : (ha != null && !ha.name().isBlank() ? ha.name() : (fritz != null && fritz.name() != null && !fritz.name().isBlank() ? fritz.name() : (resolveNames ? reverseName(ip) : "Netzwerkgerät " + ip)));
    String sourceProtocol = mergeSources(protocol, fritz == null ? null : "FRITZ!Box TR-064");
    sourceProtocol = mergeSources(sourceProtocol, ha == null ? null : "Home Assistant REST API");
    sourceProtocol = mergeSources(sourceProtocol, tuya == null ? null : "Tuya Cloud API");
    String status = (tuya != null && tuya.online()) || (ha != null && ha.online()) || (fritz != null && fritz.active()) ? "ONLINE" : "ERKANNT";
    String type = tuya != null && !tuya.type().startsWith("Smart Life / Tuya") ? tuya.type() : (ha != null ? ha.type() : (tuya != null ? tuya.type() : classifyFritzFallback(fritz, name)));
    String manufacturer = tuya != null && !tuya.manufacturer().isBlank() ? tuya.manufacturer() : (ha == null || ha.manufacturer().isBlank() ? null : ha.manufacturer());
    DiscoveredDevice device = new DiscoveredDevice("network:" + ip, name, type, ip, mac,
      sourceProtocol, status, manufacturer, null, now, isRegistered(ip, mac, name));
    emitIfNew(result, device, consumer);
  }

  private void activeSubnetScan(List<InterfaceNetwork> networks,
                                Map<String,DiscoveredDevice> result, String now,
                                Consumer<DiscoveredDevice> consumer,
                                BiConsumer<Integer,String> progressConsumer) {
    LinkedHashSet<String> candidates = new LinkedHashSet<>();
    for (InterfaceNetwork network : networks) candidates.addAll(network.hostsLimitedTo24());
    if (candidates.isEmpty()) return;

    ExecutorService pool = Executors.newFixedThreadPool(Math.min(32, Math.max(8, Runtime.getRuntime().availableProcessors() * 2)));
    CompletionService<String> completion = new ExecutorCompletionService<>(pool);
    for (String ip : candidates) completion.submit(() -> isReachable(ip) ? ip : null);

    int done = 0;
    try {
      while (done < candidates.size()) {
        Future<String> future = completion.poll(2, TimeUnit.SECONDS);
        if (future == null) continue;
        done++;
        future.get();
        // 40k9: Ein positiver Ping/Reachability-Test ist noch kein sicher bestätigtes Gerät.
        // Der aktive Scan dient ausschließlich dazu, ARP-/Neighbor-Caches zu aktualisieren.
        // Sichtbar wird ein neuer Treffer erst in der anschließenden Nachbartabellen-Auswertung
        // (MAC/Neighbor bestätigt) oder über SSDP/UPnP. So entstehen keine 200+ False Positives.
        int progress = 22 + (int)Math.round(53.0 * done / candidates.size());
        progressConsumer.accept(Math.min(75, progress), "Netzwerk wird geprüft: " + done + " / " + candidates.size() + " Adressen; bestätigte Treffer folgen aus der Nachbartabelle");
      }
    } catch (Exception ignored) {
    } finally {
      pool.shutdownNow();
    }
  }

  private boolean isReachable(String ip) {
    try {
      InetAddress address = InetAddress.getByName(ip);
      if (address.isReachable(HOST_TIMEOUT_MS)) return true;
      Process process;
      if (isWindows()) process = new ProcessBuilder("ping", "-n", "1", "-w", String.valueOf(HOST_TIMEOUT_MS), ip).start();
      else process = new ProcessBuilder("ping", "-c", "1", "-W", "1", ip).start();
      return process.waitFor(2, TimeUnit.SECONDS) && process.exitValue() == 0;
    } catch (Exception ignored) { return false; }
  }

  private void discoverSsdp(Map<String,DiscoveredDevice> result, String now, Consumer<DiscoveredDevice> consumer) {
    byte[] request = ("M-SEARCH * HTTP/1.1\r\n" +
      "HOST: 239.255.255.250:1900\r\n" +
      "MAN: \"ssdp:discover\"\r\n" +
      "MX: 2\r\n" +
      "ST: ssdp:all\r\n\r\n").getBytes(StandardCharsets.US_ASCII);
    try (DatagramSocket socket = new DatagramSocket()) {
      socket.setSoTimeout(600);
      socket.send(new DatagramPacket(request, request.length, InetAddress.getByName("239.255.255.250"), 1900));
      long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
      while (System.nanoTime() < until) {
        try {
          byte[] buf = new byte[8192];
          DatagramPacket packet = new DatagramPacket(buf, buf.length);
          socket.receive(packet);
          String ip = packet.getAddress().getHostAddress();
          String response = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
          String name = header(response, "SERVER");
          if (name == null || name.isBlank()) name = reverseName(ip);
          DiscoveredDevice device = new DiscoveredDevice("network:" + ip, name, "UPnP-/Netzwerkgerät", ip, null,
            "SSDP / UPnP", "ONLINE", null, null, now, isRegistered(ip, null, name));
          emitIfNew(result, device, consumer);
        } catch (SocketTimeoutException ignored) { }
      }
    } catch (Exception ignored) { }
  }


  private void installFritzIndex(List<FritzBoxDeviceSource.FritzDevice> devices) {
    Map<String,FritzBoxDeviceSource.FritzDevice> byIp = new HashMap<>();
    Map<String,FritzBoxDeviceSource.FritzDevice> byMac = new HashMap<>();
    Set<String> ambiguousIps = new HashSet<>();
    for (var device : devices) {
      if (device.ip() != null && !device.ip().isBlank()) {
        if (byIp.containsKey(device.ip()) && byIp.get(device.ip()).sourceId() != device.sourceId()) ambiguousIps.add(device.ip());
        else byIp.put(device.ip(), device);
      }
      if (device.mac() != null && !device.mac().isBlank()) byMac.putIfAbsent(normalizeMac(device.mac()), device);
    }
    ambiguousIps.forEach(byIp::remove);
    fritzByIp = Map.copyOf(byIp);
    fritzByMac = Map.copyOf(byMac);
  }

  private FritzBoxDeviceSource.FritzDevice findFritz(String ip, String mac) {
    if (mac != null && !mac.isBlank()) {
      var byMac = fritzByMac.get(normalizeMac(mac));
      if (byMac != null) return byMac;
    }
    return ip == null ? null : fritzByIp.get(ip);
  }


  private void installHomeAssistantIndex(List<HomeAssistantDeviceSource.HaDevice> devices) {
    Map<String,HomeAssistantDeviceSource.HaDevice> byIp=new HashMap<>();
    Map<String,HomeAssistantDeviceSource.HaDevice> byMac=new HashMap<>();
    Set<String> ambiguousIps=new HashSet<>();
    for(var d:devices){
      if(d.ip()!=null&&!d.ip().isBlank()){if(byIp.containsKey(d.ip())&&byIp.get(d.ip()).sourceId()!=d.sourceId())ambiguousIps.add(d.ip());else byIp.put(d.ip(),d);}
      if(d.mac()!=null&&!d.mac().isBlank())byMac.putIfAbsent(normalizeMac(d.mac()),d);
    }
    ambiguousIps.forEach(byIp::remove);haByIp=Map.copyOf(byIp);haByMac=Map.copyOf(byMac);
  }

  private HomeAssistantDeviceSource.HaDevice findHomeAssistant(String ip,String mac){
    if(mac!=null&&!mac.isBlank()){var d=haByMac.get(normalizeMac(mac));if(d!=null)return d;}return ip==null?null:haByIp.get(ip);
  }

  private void emitHomeAssistantOnlyDevices(List<HomeAssistantDeviceSource.HaDevice> devices,Map<String,DiscoveredDevice> seen,String now,Consumer<DiscoveredDevice> consumer){
    for(var d:devices){
      String address=!d.ip().isBlank()?d.ip():d.entityId();
      String id=!d.mac().isBlank()?"network:"+d.mac():(!d.ip().isBlank()?"network:"+d.ip():"homeassistant:"+d.sourceId()+":"+d.entityId());
      DiscoveredDevice device=new DiscoveredDevice(id,d.name(),d.type(),address,d.mac().isBlank()?null:d.mac(),"Home Assistant REST API · "+d.sourceName(),d.online()?"ONLINE":"ERKANNT",d.manufacturer().isBlank()?null:d.manufacturer(),null,now,isRegistered(d.ip(),d.mac(),d.name()));
      emitIfNew(seen,device,consumer);
    }
  }


  private void installTuyaIndex(List<TuyaDeviceSource.TuyaDevice> devices){
    Map<String,TuyaDeviceSource.TuyaDevice> byIp=new HashMap<>();Map<String,TuyaDeviceSource.TuyaDevice> byMac=new HashMap<>();Set<String> ambiguousIps=new HashSet<>();
    for(var d:devices){if(d.ip()!=null&&!d.ip().isBlank()){if(byIp.containsKey(d.ip())&&byIp.get(d.ip()).sourceId()!=d.sourceId())ambiguousIps.add(d.ip());else byIp.put(d.ip(),d);}if(d.mac()!=null&&!d.mac().isBlank())byMac.putIfAbsent(normalizeMac(d.mac()),d);}
    ambiguousIps.forEach(byIp::remove);tuyaByIp=Map.copyOf(byIp);tuyaByMac=Map.copyOf(byMac);
  }

  private TuyaDeviceSource.TuyaDevice findTuya(String ip,String mac){if(mac!=null&&!mac.isBlank()){var d=tuyaByMac.get(normalizeMac(mac));if(d!=null)return d;}return ip==null?null:tuyaByIp.get(ip);}

  private void emitTuyaOnlyDevices(List<TuyaDeviceSource.TuyaDevice> devices,Map<String,DiscoveredDevice> seen,String now,Consumer<DiscoveredDevice> consumer){
    for(var d:devices){String address=!d.ip().isBlank()?d.ip():(!d.uuid().isBlank()?d.uuid():d.deviceId());String id=!d.mac().isBlank()?"network:"+d.mac():(!d.ip().isBlank()?"network:"+d.ip():"tuya:"+d.sourceId()+":"+d.deviceId());DiscoveredDevice device=new DiscoveredDevice(id,d.name(),d.type(),address,d.mac().isBlank()?null:d.mac(),"Tuya Cloud API · "+d.sourceName(),d.online()?"ONLINE":"ERKANNT",d.manufacturer().isBlank()?null:d.manufacturer(),d.deviceId(),now,isRegistered(d.ip(),d.mac(),d.name()));emitIfNew(seen,device,consumer);}
  }

  private static String classifyFritzFallback(FritzBoxDeviceSource.FritzDevice fritz,String name){
    String type=HomeAssistantDeviceSource.classify("","",name,"","");
    if(!"Home Assistant Gerät".equals(type))return type;
    return "Netzwerkgerät";
  }
  private static String header(String response, String key) {
    for (String line : response.split("\\r?\\n")) {
      int colon = line.indexOf(':');
      if (colon > 0 && line.substring(0, colon).trim().equalsIgnoreCase(key)) return line.substring(colon + 1).trim();
    }
    return null;
  }

  private void emitIfNew(Map<String,DiscoveredDevice> result, DiscoveredDevice device, Consumer<DiscoveredDevice> consumer) {
    String matchingKey = findIdentityMatch(result, device);
    DiscoveredDevice emitted;
    String storageKey;
    if (matchingKey == null) {
      emitted = applyAutomaticRegistration(device);
      storageKey = emitted.id();
    } else {
      DiscoveredDevice previous = result.get(matchingKey);
      emitted = applyAutomaticRegistration(mergeIdentity(previous, device));
      storageKey = matchingKey;
    }
    result.put(storageKey, emitted);
    // Jeder Treffer und jede Identitaetsergaenzung wird unmittelbar gestreamt.
    // Bei aktivierter Automatik ist der Datensatz bereits in diesem Ereignis registriert,
    // bleibt aber bis zur ausdruecklichen Inventaruebernahme ausserhalb der Geraeteliste.
    consumer.accept(emitted);
  }

  public int registeredCount() { return registrations.count(); }

  private DiscoveredDevice applyAutomaticRegistration(DiscoveredDevice device) {
    boolean persistHit = device.alreadyRegistered() || builtinSettings.enabled("AUTO_REGISTER");
    if (!persistHit) return device;
    try {
      // Jeder Roh-Treffer wird gezaehlt. So bleibt sichtbar, wenn dieselbe Identitaet
      // etwa durch ARP, Fritz!Box, mDNS und Home Assistant mehrfach gefunden wurde.
      registrations.recordDiscoveryHit(device.address(), device.hardwareAddress(), device.serialNumber(), device.name(),
        device.type(), device.manufacturer(), device.protocol(), device.status());
      return new DiscoveredDevice(device.id(), device.name(), device.type(), device.address(),
        device.hardwareAddress(), device.protocol(), device.status(), device.manufacturer(),
        device.serialNumber(), device.lastSeen(), true);
    } catch (RuntimeException registrationFailure) {
      log.warn("[DISCOVERY] Automatische Registrierung/Trefferzaehlung fuer {} fehlgeschlagen: {}", device.id(), registrationFailure.toString());
      return device;
    }
  }

  private String findIdentityMatch(Map<String,DiscoveredDevice> result, DiscoveredDevice candidate) {
    String bestKey = null;
    DeviceIdentityConfidenceEngine.Assessment best = null;
    for (var entry : result.entrySet()) {
      var assessment = identityEngine.assess(entry.getValue(), candidate, mergeSettings.load());
      if (assessment.decision() == DeviceIdentityConfidenceEngine.Decision.AUTO_MERGE
          && (best == null || assessment.score() > best.score())) {
        bestKey = entry.getKey();
        best = assessment;
      }
    }
    if (best != null) {
      log.debug("[DISCOVERY-MERGE] {} -> {} score={} confidence={} signals={}",
        candidate.id(), bestKey, best.score(), best.confidencePercent(), best.matchedSignals());
    }
    return bestKey;
  }

  private static DiscoveredDevice mergeIdentity(DiscoveredDevice current, DiscoveredDevice incoming) {
    String name = preferSpecificName(current.name(), incoming.name());
    String type = preferSpecificType(current.type(), incoming.type());
    String address = firstNonBlank(current.address(), incoming.address());
    String mac = firstNonBlank(current.hardwareAddress(), incoming.hardwareAddress());
    String protocol = mergeSources(current.protocol(), incoming.protocol());
    String status = "ONLINE".equalsIgnoreCase(current.status()) || "ONLINE".equalsIgnoreCase(incoming.status()) ? "ONLINE" : firstNonBlank(incoming.status(), current.status());
    String manufacturer = firstNonBlank(current.manufacturer(), incoming.manufacturer());
    String serial = firstNonBlank(current.serialNumber(), incoming.serialNumber());
    String lastSeen = firstNonBlank(incoming.lastSeen(), current.lastSeen());
    return new DiscoveredDevice(current.id(), name, type, address, mac, protocol, status,
        manufacturer, serial, lastSeen, current.alreadyRegistered() || incoming.alreadyRegistered());
  }

  private static String mergeSources(String a, String b) {
    LinkedHashSet<String> values = new LinkedHashSet<>();
    for (String source : (nullToEmpty(a) + " · " + nullToEmpty(b)).split("[,·]")) {
      String cleaned = source.trim();
      if (!cleaned.isBlank()) values.add(cleaned);
    }
    return String.join(" · ", values);
  }

  private static String preferSpecificName(String a, String b) {
    boolean aGeneric = isGenericName(a);
    boolean bGeneric = isGenericName(b);
    if (aGeneric && !bGeneric) return b;
    return firstNonBlank(a, b);
  }

  private static String preferSpecificType(String a, String b) {
    if (a == null || a.isBlank() || "Netzwerkgerät".equalsIgnoreCase(a) || "Home Assistant Gerät".equalsIgnoreCase(a)) return firstNonBlank(b, a);
    return a;
  }

  private static boolean isGenericName(String value) {
    if (value == null || value.isBlank()) return true;
    String v = value.toLowerCase(Locale.ROOT);
    return v.startsWith("netzwerkgerät ") || v.startsWith("mdns-gerät ") || v.equals("unbekanntes gerät");
  }

  private static boolean compatibleType(String a, String b) {
    if (a == null || b == null || a.isBlank() || b.isBlank()) return true;
    if (a.equalsIgnoreCase(b)) return true;
    return a.toLowerCase(Locale.ROOT).contains("netzwerk") || b.toLowerCase(Locale.ROOT).contains("netzwerk");
  }

  private static String normalizeMac(String value) {
    return value == null ? "" : value.replaceAll("[^0-9A-Fa-f]", "").toUpperCase(Locale.ROOT);
  }

  private static String normalizeIp(String value) {
    if (value == null) return "";
    String v = value.trim().toLowerCase(Locale.ROOT);
    return v.matches("(?:\\d{1,3}\\.){3}\\d{1,3}") ? v : "";
  }

  private static String normalizeIdentity(String value) {
    if (value == null) return "";
    String v = value.trim().toLowerCase(Locale.ROOT);
    // Kurze, generische Modell-/Serienangaben nicht als alleinige Identität verwenden.
    return v.length() >= 6 ? v : "";
  }

  private static String normalizeName(String value) {
    if (isGenericName(value)) return "";
    return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
  }

  private static boolean sameNonBlank(String a, String b) {
    return a != null && b != null && !a.isBlank() && a.equals(b);
  }

  private static String firstNonBlank(String preferred, String fallback) {
    return preferred != null && !preferred.isBlank() ? preferred : fallback;
  }

  private boolean isRegistered(String ip, String mac, String name) {
    try {
      Integer count = jdbc.queryForObject("""
        SELECT (SELECT COUNT(*) FROM `geräte_neu` WHERE LOWER(COALESCE(`IP`,''))=LOWER(?) OR LOWER(COALESCE(`Name`,''))=LOWER(?))
             + (SELECT COUNT(*) FROM `geräte` WHERE LOWER(COALESCE(`GeräteName`,''))=LOWER(?))
        """, Integer.class, nullToEmpty(ip), nullToEmpty(name), nullToEmpty(name));
      return (count != null && count > 0) || registrations.isRegistered(ip, mac, null, name);
    } catch (Exception ignored) { return registrations.isRegistered(ip, mac, null, name); }
  }

  private static boolean commandExists(String command) {
    try {
      Process p = isWindows() ? new ProcessBuilder("cmd.exe", "/c", "where " + command).start()
                              : new ProcessBuilder("sh", "-c", "command -v " + command).start();
      return p.waitFor(2, TimeUnit.SECONDS) && p.exitValue() == 0;
    } catch (Exception ignored) { return false; }
  }

  private static boolean validHostIp(String ip) {
    try {
      InetAddress a = InetAddress.getByName(ip);
      return a instanceof Inet4Address && !a.isAnyLocalAddress() && !a.isMulticastAddress() && !ip.endsWith(".255");
    } catch (Exception ignored) { return false; }
  }

  private static String reverseName(String ip) {
    try {
      String host = InetAddress.getByName(ip).getCanonicalHostName();
      return host.equals(ip) ? "Netzwerkgerät " + ip : host;
    } catch (Exception ignored) { return "Netzwerkgerät " + ip; }
  }

  private static String cleanHost(String host, String fallback) {
    if (host == null || host.isBlank()) return fallback == null ? "Lokaler Computer" : fallback;
    return host;
  }

  private static String formatMac(byte[] bytes) {
    if (bytes == null || bytes.length == 0) return null;
    StringJoiner joiner = new StringJoiner(":");
    for (byte b : bytes) joiner.add(String.format("%02X", b));
    return joiner.toString();
  }

  private static String nullToEmpty(String value) { return value == null ? "" : value; }
  private static String platform() { return isWindows() ? "Windows" : isMac() ? "macOS" : isLinux() ? "Linux" : System.getProperty("os.name", "Unbekannt"); }
  private static boolean isWindows() { return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win"); }
  private static boolean isMac() { return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac"); }
  private static boolean isLinux() { return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("linux"); }

  private record CommandSource(String label, List<String> command) { }

  private record InterfaceNetwork(String address, short prefix) {
    Set<String> hostsLimitedTo24() {
      LinkedHashSet<String> hosts = new LinkedHashSet<>();
      String[] parts = address.split("\\.");
      if (parts.length != 4) return hosts;
      // Für Sicherheit und Laufzeit höchstens das lokale /24 scannen, auch wenn das Interface größer konfiguriert ist.
      String base = parts[0] + "." + parts[1] + "." + parts[2] + ".";
      for (int i = 1; i <= 254; i++) {
        String candidate = base + i;
        if (!candidate.equals(address)) hosts.add(candidate);
      }
      return hosts;
    }
  }
  private String discoverMdns(Map<String,DiscoveredDevice> result, String now, Consumer<DiscoveredDevice> consumer) {
    try {
      NativeMdnsDiscovery.Result nativeResult = new NativeMdnsDiscovery().discover(3200);
      for (NativeMdnsDiscovery.Host host : nativeResult.hosts()) {
        String ip = host.address();
        if (!validHostIp(ip)) continue;
        String name = cleanHost(host.displayName(), cleanHost(host.hostName(), "mDNS-Gerät " + ip));
        DiscoveredDevice device = new DiscoveredDevice(
          "mdns:" + ip + ":" + normalizeIdentity(host.serviceType()),
          name,
          "Netzwerkgerät",
          ip,
          null,
          "mDNS / Bonjour (integriert)",
          "ERKANNT",
          null,
          null,
          now,
          isRegistered(ip, null, name)
        );
        emitIfNew(result, device, consumer);
      }
      log.info("[DISCOVERY] Integrierte mDNS-Suche erfolgreich: packets={} records={} services={} hosts={}",
        nativeResult.packets(), nativeResult.records(), nativeResult.serviceTypes().size(), nativeResult.hosts().size());
      return "COMPLETED_NATIVE";
    } catch (Exception nativeError) {
      log.warn("[DISCOVERY] Integrierte mDNS-Suche fehlgeschlagen, externer Fallback wird geprüft: {}", nativeError.toString());
      return discoverMdnsExternalFallback(result, now, consumer);
    }
  }

  private String discoverMdnsExternalFallback(Map<String,DiscoveredDevice> result, String now, Consumer<DiscoveredDevice> consumer) {
    List<String> command;
    if (isLinux() && commandExists("avahi-browse")) command = List.of("sh", "-c", "avahi-browse -atrp 2>/dev/null | head -n 300");
    else if ((isMac() && commandExists("dns-sd")) || (isWindows() && commandExists("dns-sd.exe"))) {
      String executable = isWindows() ? "dns-sd.exe" : "dns-sd";
      command = List.of(executable, "-B", "_services._dns-sd._udp", "local");
    } else return "FAILED_NATIVE_NO_FALLBACK";
    Process process = null;
    try {
      process = new ProcessBuilder(command).redirectErrorStream(true).start();
      long until = System.currentTimeMillis() + 2500;
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
        while (System.currentTimeMillis() < until) {
          if (!reader.ready()) { Thread.sleep(40); continue; }
          String line = reader.readLine(); if (line == null) break;
          Matcher ipMatcher = IPV4.matcher(line);
          if (!ipMatcher.find()) continue;
          String ip = ipMatcher.group(1); if (!validHostIp(ip)) continue;
          String[] parts = line.split(";");
          String name = parts.length > 6 && !parts[6].isBlank() ? parts[6].trim() : reverseName(ip);
          Matcher macMatcher = MAC.matcher(line); String mac = macMatcher.find() ? normalizeMac(macMatcher.group(1)) : null;
          DiscoveredDevice d = new DiscoveredDevice("mdns:"+ip, cleanHost(name,"mDNS-Gerät "+ip), "Netzwerkgerät", ip, mac, "mDNS / Bonjour (Fallback)", "ERKANNT", null, null, now, isRegistered(ip,mac,name));
          emitIfNew(result,d,consumer);
        }
      }
      return "COMPLETED_FALLBACK";
    } catch (Exception ex) {
      log.warn("[DISCOVERY] Externe mDNS-Fallback-Suche fehlgeschlagen: {}", ex.toString());
      return "FAILED";
    } finally { if (process != null) process.destroyForcibly(); }
  }

  private static String mdnsMessage(String status) {
    return switch (status) {
      case "COMPLETED_NATIVE" -> "mDNS-/Bonjour-Suche mit integrierter Java-Multicast-Engine abgeschlossen";
      case "COMPLETED_FALLBACK" -> "mDNS-/Bonjour-Suche über externes Fallback-Werkzeug abgeschlossen";
      case "FAILED_NATIVE_NO_FALLBACK" -> "Integrierte mDNS-Suche fehlgeschlagen; kein externes Fallback-Werkzeug verfügbar";
      default -> "mDNS-/Bonjour-Suche fehlgeschlagen";
    };
  }

  private void enrichReverseDns(Map<String,DiscoveredDevice> result, Consumer<DiscoveredDevice> consumer) {
    List<Map.Entry<String,DiscoveredDevice>> snapshot = new ArrayList<>(result.entrySet());
    ExecutorService pool = Executors.newFixedThreadPool(Math.min(8, Math.max(1, snapshot.size())));
    try {
      List<Future<?>> jobs = new ArrayList<>();
      for (var entry : snapshot) jobs.add(pool.submit(() -> {
        DiscoveredDevice d = entry.getValue();
        if (d.address() == null || !validHostIp(d.address())) return;
        if (d.name() != null && !d.name().startsWith("Netzwerkgerät ") && !d.name().equals(d.address())) return;
        String resolved = reverseName(d.address());
        if (resolved == null || resolved.isBlank() || resolved.equals(d.address())) return;
        DiscoveredDevice updated = new DiscoveredDevice(d.id(), resolved, d.type(), d.address(), d.hardwareAddress(), appendProtocol(d.protocol(), "Reverse DNS"), d.status(), d.manufacturer(), d.serialNumber(), d.lastSeen(), d.alreadyRegistered());
        synchronized (result) { result.put(entry.getKey(), updated); }
        consumer.accept(updated);
      }));
      for (Future<?> job : jobs) try { job.get(900, TimeUnit.MILLISECONDS); } catch (Exception ignored) { job.cancel(true); }
    } finally { pool.shutdownNow(); }
  }

  private static String appendProtocol(String current, String addition) {
    if (current == null || current.isBlank()) return addition;
    return current.contains(addition) ? current : current + ", " + addition;
  }

}
