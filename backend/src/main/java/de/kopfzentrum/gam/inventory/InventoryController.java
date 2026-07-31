package de.kopfzentrum.gam.inventory;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import org.springframework.http.MediaType;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
  private static final Logger log = LoggerFactory.getLogger(InventoryController.class);
  private final InventoryRepository repository;
  private final DeviceDiscoveryService discoveryService;
  private final BuiltinDiscoverySettingsRepository builtinDiscoverySettings;
  private final DiscoverySessionService discoverySessions;
  private final DeviceIdentityMergeSettingsRepository mergeSettings;
  private final ObjectMapper objectMapper;
  private final WinRmSettingsRepository winRmSettings;
  private final LinuxSshSettingsRepository linuxSshSettings;
  private final WindowsRemoteInventoryService windowsRemote;

  public InventoryController(InventoryRepository repository, DeviceDiscoveryService discoveryService, BuiltinDiscoverySettingsRepository builtinDiscoverySettings, DiscoverySessionService discoverySessions, DeviceIdentityMergeSettingsRepository mergeSettings, ObjectMapper objectMapper, WinRmSettingsRepository winRmSettings, WindowsRemoteInventoryService windowsRemote, LinuxSshSettingsRepository linuxSshSettings) {
    this.repository = repository;
    this.discoveryService = discoveryService;
    this.builtinDiscoverySettings = builtinDiscoverySettings;
    this.discoverySessions = discoverySessions;
    this.mergeSettings = mergeSettings;
    this.objectMapper = objectMapper;
    this.winRmSettings = winRmSettings; this.linuxSshSettings = linuxSshSettings;
    this.windowsRemote = windowsRemote;
  }

  @GetMapping("/devices")
  public List<InventoryDevice> devices(
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "all") String source,
      @RequestParam(required = false) Integer branchId,
      @RequestParam(defaultValue = "false") boolean activeOnly,
      @RequestParam(defaultValue = "100") int limit,
      @RequestParam(defaultValue = "0") int offset) {
    return repository.search(new InventorySearchCriteria(q, source, branchId, activeOnly, limit, offset));
  }

  @GetMapping("/devices/{source}/{id}")
  public InventoryDeviceDetail device(@PathVariable String source, @PathVariable Integer id) {
    return repository.detail(source, id);
  }

  @GetMapping("/branches")
  public List<InventoryBranchOption> branches() {
    return repository.branches();
  }

  @GetMapping("/companies")
  public List<InventoryCompanyOption> companies() {
    return repository.companies();
  }

  @GetMapping("/materials")
  public List<InventoryMaterialOption> materials(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "200") int limit) {
    return repository.materials(q, limit);
  }

  @GetMapping("/discovery/capabilities")
  public java.util.Map<String,Object> discoveryCapabilities() {
    return discoveryService.capabilities();
  }

  @GetMapping("/discovery/builtin-sources")
  public java.util.Map<String,Boolean> builtinDiscoverySources() { return builtinDiscoverySettings.load(); }

  @PutMapping("/discovery/builtin-sources")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public java.util.Map<String,Boolean> saveBuiltinDiscoverySources(@RequestBody java.util.Map<String,Boolean> values, java.security.Principal principal) {
    return builtinDiscoverySettings.save(values, principal == null ? "system" : principal.getName());
  }


  @GetMapping("/discovery/winrm")
  public WinRmSettingsRepository.View winRmSettings() { return winRmSettings.view(); }

  @PutMapping("/discovery/winrm")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public WinRmSettingsRepository.View saveWinRmSettings(@RequestBody WinRmSettingsRepository.Update update, java.security.Principal principal) {
    return winRmSettings.save(update, principal == null ? "system" : principal.getName());
  }

  @PostMapping("/discovery/winrm/test")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public WindowsRemoteInventoryService.WinRmTestResult testWinRm(@RequestBody java.util.Map<String,Object> request) {
    String host = request == null ? "" : String.valueOf(request.getOrDefault("host", "")).trim();
    if (host.isBlank()) return new WindowsRemoteInventoryService.WinRmTestResult(winRmSettings.load().enabled(), false, "Bitte eine IP-Adresse oder einen Hostnamen angeben.", OffsetDateTime.now().toString());
    return windowsRemote.testConnection(host);
  }


  @GetMapping("/discovery/ssh")
  public LinuxSshSettingsRepository.View linuxSshSettings(){ return linuxSshSettings.view(); }

  @PutMapping("/discovery/ssh")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public LinuxSshSettingsRepository.View saveLinuxSshSettings(@RequestBody LinuxSshSettingsRepository.Update update, java.security.Principal principal){
    return linuxSshSettings.save(update, principal==null?"system":principal.getName());
  }

  @GetMapping("/discovery/merge-settings")
  public DeviceIdentityConfidenceEngine.Settings mergeSettings(){ return mergeSettings.load(); }

  @PutMapping("/discovery/merge-settings")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public DeviceIdentityConfidenceEngine.Settings saveMergeSettings(@RequestBody java.util.Map<String,Object> values, java.security.Principal principal){ return mergeSettings.save(values, principal==null?"system":principal.getName()); }

  @PostMapping("/discovery/merge-settings/reset")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public DeviceIdentityConfidenceEngine.Settings resetMergeSettings(java.security.Principal principal){ return mergeSettings.reset(principal==null?"system":principal.getName()); }

  /**
   * 40k30i: Der alte synchrone Discovery-Endpunkt ist bewusst deaktiviert.
   * Eine Discovery darf ausschließlich über Session + SSE gestartet werden,
   * damit kein versteckter erster Scan vor dem Live-Scan läuft.
   */
  @PostMapping("/discovery/scan")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public ResponseEntity<java.util.Map<String,Object>> legacyDiscoveryDisabled(jakarta.servlet.http.HttpServletRequest request) {
    log.error("[DISCOVERY-TRACE] LEGACY_ENDPOINT_CALLED method={} uri={} remote={} userAgent={} thread={}", request.getMethod(), request.getRequestURI(), request.getRemoteAddr(), request.getHeader("User-Agent"), Thread.currentThread().getName());
    return ResponseEntity.status(org.springframework.http.HttpStatus.GONE).body(java.util.Map.of(
      "error", "LEGACY_DISCOVERY_DISABLED",
      "message", "Bitte den Live-Discovery-Session-Endpunkt verwenden."
    ));
  }


  @PostMapping("/discovery/sessions")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public java.util.Map<String,Object> startDiscoverySession(jakarta.servlet.http.HttpServletRequest request) {
    log.warn("[DISCOVERY-TRACE] SESSION_ENDPOINT_CALLED method={} uri={} remote={} clientTrace={} userAgent={} thread={}", request.getMethod(), request.getRequestURI(), request.getRemoteAddr(), request.getHeader("X-GAM-Discovery-Trace"), request.getHeader("User-Agent"), Thread.currentThread().getName());
    return discoverySessions.start();
  }

  /**
   * 40k30n: Live-Ereignisse per Long-Polling. Dieser Weg ist absichtlich nicht
   * streaming-basiert und bleibt daher auch hinter puffenden Servlet-Filtern,
   * Virenscannern und Reverse-Proxies unmittelbar sichtbar.
   */
  @PostMapping("/discovery/sessions/{sessionId}/cancel")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public java.util.Map<String,Object> cancelDiscoverySession(@PathVariable String sessionId) {
    return discoverySessions.cancel(sessionId);
  }

  @GetMapping(value = "/discovery/sessions/{sessionId}/events-poll", produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public java.util.Map<String,Object> discoverySessionEventsPoll(@PathVariable String sessionId,
      @RequestParam(name = "after", defaultValue = "0") long after,
      jakarta.servlet.http.HttpServletRequest request) {
    log.warn("[DISCOVERY-TRACE] POLL_ENDPOINT_CALLED sessionId={} after={} remote={} clientTrace={} thread={}",
      sessionId, after, request.getRemoteAddr(), request.getHeader("X-GAM-Discovery-Trace"), Thread.currentThread().getName());
    return discoverySessions.poll(sessionId, Math.max(0, after));
  }

  @GetMapping(value = "/discovery/sessions/{sessionId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public ResponseEntity<StreamingResponseBody> discoverySessionEvents(@PathVariable String sessionId, jakarta.servlet.http.HttpServletRequest request) {
    log.warn("[DISCOVERY-TRACE] SSE_ENDPOINT_CALLED sessionId={} method={} uri={} remote={} clientTrace={} lastEventId={} userAgent={} thread={}", sessionId, request.getMethod(), request.getRequestURI(), request.getRemoteAddr(), request.getHeader("X-GAM-Discovery-Trace"), request.getHeader("Last-Event-ID"), request.getHeader("User-Agent"), Thread.currentThread().getName());
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.TEXT_EVENT_STREAM);
    headers.set("Cache-Control", "no-cache, no-store, no-transform");
    headers.set("Pragma", "no-cache");
    headers.set("X-Accel-Buffering", "no");
    headers.set("Connection", "keep-alive");
    headers.set("Content-Encoding", "identity");
    StreamingResponseBody body = output -> discoverySessions.stream(sessionId, output);
    return ResponseEntity.ok().headers(headers).body(body);
  }

  /**
   * 40k30k: Der direkte POST-Streaming-Pfad ist deaktiviert. Einige Servlet-/Security-
   * Filter pufferten diesen Request bis zum Ende. Die einzige aktive Pipeline ist jetzt:
   * Session anlegen -> SSE-Kanal öffnen -> genau einen Worker starten.
   */
  @PostMapping(value = "/discovery/scan-stream", produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public ResponseEntity<java.util.Map<String,Object>> directStreamDisabled(jakarta.servlet.http.HttpServletRequest request) {
    log.error("[DISCOVERY-TRACE] DIRECT_STREAM_ENDPOINT_CALLED method={} uri={} remote={} clientTrace={} userAgent={} thread={}", request.getMethod(), request.getRequestURI(), request.getRemoteAddr(), request.getHeader("X-GAM-Discovery-Trace"), request.getHeader("User-Agent"), Thread.currentThread().getName());
    return ResponseEntity.status(org.springframework.http.HttpStatus.GONE).body(java.util.Map.of(
      "error", "DIRECT_STREAM_DISABLED",
      "message", "Bitte die Discovery-Session mit SSE-Ereigniskanal verwenden."
    ));
  }

  private void writeSseUnchecked(OutputStream output, String event, Object data) {
    try { writeSse(output, event, data); }
    catch (IOException failure) { throw new java.io.UncheckedIOException(failure); }
  }

  private void writeSse(OutputStream output, String event, Object data) throws IOException {
    String json = objectMapper.writeValueAsString(data);
    String block = "event: " + event + "\n" + "data: " + json + "\n\n";
    output.write(block.getBytes(StandardCharsets.UTF_8));
    output.flush();
  }

  private void writeSseComment(OutputStream output, String comment) throws IOException {
    output.write((": " + comment + "\n\n").getBytes(StandardCharsets.UTF_8));
    output.flush();
  }

  @GetMapping("/stats")
  public InventoryStats stats() {
    return repository.stats();
  }
}
