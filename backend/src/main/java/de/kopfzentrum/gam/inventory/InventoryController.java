package de.kopfzentrum.gam.inventory;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
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
  private final WinRmTrustedTargetRepository winRmTrustedTargets;
  private final LinuxSshSettingsRepository linuxSshSettings;
  private final WindowsRemoteInventoryService windowsRemote;
  private final WindowsInventoryDiscoveryService windowsInventoryDiscovery;
  private final OsAccessProfileRepository osAccessProfiles;
  private final EmbeddedSshClientService embeddedSsh;
  private final AndroidAdbService androidAdb;
  private final DeviceIdentityService deviceIdentityService;
  private final StartupInventorySettingsRepository startupInventorySettings;
  private final AppleMobileDiscoveryService appleMobileDiscovery;

  public InventoryController(InventoryRepository repository, DeviceDiscoveryService discoveryService, BuiltinDiscoverySettingsRepository builtinDiscoverySettings, DiscoverySessionService discoverySessions, DeviceIdentityMergeSettingsRepository mergeSettings, ObjectMapper objectMapper, WinRmSettingsRepository winRmSettings, WinRmTrustedTargetRepository winRmTrustedTargets, WindowsRemoteInventoryService windowsRemote, WindowsInventoryDiscoveryService windowsInventoryDiscovery, LinuxSshSettingsRepository linuxSshSettings, OsAccessProfileRepository osAccessProfiles, EmbeddedSshClientService embeddedSsh, AndroidAdbService androidAdb, DeviceIdentityService deviceIdentityService, StartupInventorySettingsRepository startupInventorySettings, AppleMobileDiscoveryService appleMobileDiscovery) {
    this.repository = repository;
    this.discoveryService = discoveryService;
    this.builtinDiscoverySettings = builtinDiscoverySettings;
    this.discoverySessions = discoverySessions;
    this.mergeSettings = mergeSettings;
    this.objectMapper = objectMapper;
    this.winRmSettings = winRmSettings; this.winRmTrustedTargets=winRmTrustedTargets; this.linuxSshSettings = linuxSshSettings;
    this.windowsRemote = windowsRemote;
    this.windowsInventoryDiscovery = windowsInventoryDiscovery;
    this.osAccessProfiles = osAccessProfiles; this.embeddedSsh = embeddedSsh; this.androidAdb = androidAdb; this.deviceIdentityService=deviceIdentityService; this.startupInventorySettings=startupInventorySettings; this.appleMobileDiscovery=appleMobileDiscovery;
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

  @GetMapping("/startup-inventory-settings")
  public StartupInventorySettingsRepository.Settings startupInventorySettings(){
    return startupInventorySettings.load();
  }

  @PutMapping("/startup-inventory-settings")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public StartupInventorySettingsRepository.Settings saveStartupInventorySettings(@RequestBody java.util.Map<String,Object> body, java.security.Principal principal){
    boolean enabled=body==null||!body.containsKey("enabled")||Boolean.TRUE.equals(body.get("enabled"));
    return startupInventorySettings.save(enabled,principal==null?"system":principal.getName());
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


  @GetMapping("/discovery/winrm/trusted-targets")
  public java.util.List<WinRmTrustedTargetRepository.TrustedTarget> winRmTrustedTargets(){ return winRmTrustedTargets.list(); }

  @PostMapping("/discovery/winrm/trusted-targets")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public java.util.Map<String,Object> trustWinRmTarget(@RequestBody java.util.Map<String,Object> request, java.security.Principal principal){
    String host=request==null?"":String.valueOf(request.getOrDefault("host","")).trim();
    var row=winRmTrustedTargets.trust(host,principal==null?"system":principal.getName());
    return java.util.Map.of("trusted",true,"host",row.host());
  }

  @DeleteMapping("/discovery/winrm/trusted-targets")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public java.util.Map<String,Object> revokeWinRmTarget(@RequestParam String host){
    winRmTrustedTargets.revoke(host);
    return java.util.Map.of("trusted",false,"host",host);
  }


  @GetMapping("/discovery/apple-mobile/status")
  public AppleMobileDiscoveryService.Status appleMobileStatus() { return appleMobileDiscovery.status(); }

  @PostMapping("/discovery/apple-mobile/test")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public AppleMobileDiscoveryService.TestResult testAppleMobile() {
    log.warn("FIX33 LEGACY TEST ROUTE INTERCEPT | endpoint=/discovery/apple-mobile/test | action=PAIRING_ONLY");
    // fix33: selbst ein noch vorhandener/alter Frontend-Aufruf auf /test darf nicht mehr
    // in den historischen Diagnosepfad laufen. Der bekannte /test-Endpunkt wird hier
    // bewusst auf denselben Pairing-only Servicepfad geroutet.
    return appleMobileDiscovery.testPairingConnectionFix33();
  }

  @GetMapping("/discovery/apple-mobile/admin-tunnel")
  public AppleMobileDiscoveryService.AdminTunnelStatus appleAdminTunnelStatus() { return appleMobileDiscovery.adminTunnelStatus(); }

  @PostMapping("/discovery/apple-mobile/admin-tunnel")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public AppleMobileDiscoveryService.AdminTunnelStatus startAppleAdminTunnel() { return appleMobileDiscovery.startAdminTunnel(); }

  @DeleteMapping("/discovery/apple-mobile/admin-tunnel")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public AppleMobileDiscoveryService.AdminTunnelStatus stopAppleAdminTunnel() { return appleMobileDiscovery.stopAdminTunnel(); }

  @PostMapping("/discovery/apple-mobile/toolchain/install")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public AppleMobileDiscoveryService.ToolchainInstallResult installAppleMobileToolchain() { return appleMobileDiscovery.installToolchain(); }

  @PostMapping("/discovery/apple-mobile/apple-devices/install")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public AppleMobileDiscoveryService.AppleDevicesInstallResult installAppleDevices() { return appleMobileDiscovery.installAppleDevices(); }

  @GetMapping("/discovery/apple-mobile/apple-devices/depot")
  public AppleMobileDiscoveryService.AppleOfflineDepotStatus appleOfflineDepotStatus() { return appleMobileDiscovery.appleOfflineDepotStatus(); }

  @PutMapping("/discovery/apple-mobile/apple-devices/depot")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public AppleMobileDiscoveryService.AppleOfflineDepotStatus configureAppleOfflineDepot(@RequestBody java.util.Map<String,Object> request) {
    return appleMobileDiscovery.configureAppleOfflineDepot(String.valueOf(request.getOrDefault("directory","")));
  }

  @PostMapping("/discovery/apple-mobile/apple-devices/depot/prepare")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public AppleMobileDiscoveryService.AppleOfflineDepotResult prepareAppleOfflineDepot() { return appleMobileDiscovery.prepareAppleOfflineDepot(); }

  @PostMapping("/discovery/apple-mobile/apple-devices/depot/update-check")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public AppleMobileDiscoveryService.AppleDepotUpdateStatus checkAppleOfflineDepotUpdates() { return appleMobileDiscovery.checkAppleOfflineDepotUpdates(); }

  @PostMapping("/discovery/apple-mobile/apple-devices/depot/update")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public AppleMobileDiscoveryService.AppleOfflineDepotResult updateAppleOfflineDepot() { return appleMobileDiscovery.updateAppleOfflineDepot(); }

  @PostMapping("/discovery/apple-mobile/apple-devices/depot/install")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public AppleMobileDiscoveryService.AppleOfflineDepotResult installAppleFromOfflineDepot() { return appleMobileDiscovery.installAppleFromOfflineDepot(); }

  @PostMapping("/discovery/apple-mobile/inventory-probe")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public AppleMobileDiscoveryService.InventoryProbeResult probeAppleMobileInventory(@RequestBody(required=false) java.util.Map<String,Object> request) {
    String udid=request==null?null:String.valueOf(request.getOrDefault("udid","")).trim();
    return appleMobileDiscovery.probeInventory(udid);
  }

  @PostMapping("/discovery/apple-mobile/extended-inventory-probe")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public AppleMobileDiscoveryService.ExtendedInventoryProbeResult probeAppleMobileExtendedInventory(@RequestBody(required=false) java.util.Map<String,Object> request) {
    String udid=request==null?null:String.valueOf(request.getOrDefault("udid","")).trim();
    return appleMobileDiscovery.probeExtendedInventory(udid);
  }

  @PostMapping("/discovery/apple-mobile/pair")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public AppleMobileDiscoveryService.PairingResult pairAppleMobile(@RequestBody(required=false) java.util.Map<String,Object> request, @RequestParam(required=false, defaultValue="") String gamTrace) {
    log.warn("FIX32 CONTROLLER ACTIVE | endpoint=/discovery/apple-mobile/pair | gamTrace={} | codeSource={}", gamTrace, InventoryController.class.getProtectionDomain().getCodeSource().getLocation());
    String udid=request==null?null:String.valueOf(request.getOrDefault("udid","")).trim();
    log.warn("FIX32 PAIR ROUTE | sequence=USB_LOCKDOWN_THEN_REMOTEPAIRING | pairingOnly=true");
    log.warn("[DISCOVERY][40k37e56c-fix31][APPLE_MOBILE][PAIR_ENDPOINT] event=ENTER endpoint=/discovery/apple-mobile/pair requestedUdidPresent={} thread={}", udid!=null && !udid.isBlank(), Thread.currentThread().getName());
    AppleMobileDiscoveryService.PairingResult result=appleMobileDiscovery.pair(udid);
    log.warn("[DISCOVERY][40k37e56c-fix31][APPLE_MOBILE][PAIR_ENDPOINT] event=EXIT endpoint=/discovery/apple-mobile/pair success={} state={} durationMs={} transport={}", result.success(), result.state(), result.durationMs(), result.success()?"USB + Netzwerk":"USB");
    return result;
  }

  @GetMapping("/discovery/apple-mobile/pair-wifi/status")
  public AppleMobileDiscoveryService.PairingResult appleMobileWifiPairingStatus() {
    return appleMobileDiscovery.wifiPairingStatus();
  }

  @PostMapping("/discovery/apple-mobile/pair-wifi")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public AppleMobileDiscoveryService.PairingResult pairAppleMobileWifi(@RequestBody(required=false) java.util.Map<String,Object> request) {
    String udid=request==null?null:String.valueOf(request.getOrDefault("udid","")).trim();
    log.warn("[DISCOVERY][40k37e56c-fix11][APPLE_MOBILE][PAIR_ENDPOINT] event=ENTER endpoint=/discovery/apple-mobile/pair-wifi requestedUdidPresent={} thread={}", udid!=null && !udid.isBlank(), Thread.currentThread().getName());
    AppleMobileDiscoveryService.PairingResult result=appleMobileDiscovery.pairWifi(udid);
    log.warn("[DISCOVERY][40k37e56c-fix11][APPLE_MOBILE][PAIR_ENDPOINT] event=EXIT endpoint=/discovery/apple-mobile/pair-wifi success={} state={} durationMs={}", result.success(), result.state(), result.durationMs());
    return result;
  }

  @PostMapping("/discovery/apple-mobile/pair-host")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public AppleMobileDiscoveryService.PairingResult pairAppleMobileHostWifiIos27() {
    return appleMobileDiscovery.pairHostWifiIos27();
  }

  @PostMapping("/discovery/apple-mobile/pairing/validate")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public AppleMobileDiscoveryService.PairingResult validateAppleMobilePairing(@RequestBody(required=false) java.util.Map<String,Object> request) {
    String udid=request==null?null:String.valueOf(request.getOrDefault("udid","")).trim();
    return appleMobileDiscovery.validatePairing(udid);
  }

  @PostMapping("/discovery/windows-local/test")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public WindowsInventoryDiscoveryService.LocalWindowsTestResult testWindowsLocal() {
    return windowsInventoryDiscovery.testLocalConnection();
  }

  @PostMapping("/discovery/winrm/test")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public WindowsRemoteInventoryService.WinRmTestResult testWinRm(@RequestBody java.util.Map<String,Object> request) {
    String host = request == null ? "" : String.valueOf(request.getOrDefault("host", "")).trim();
    if (host.isBlank()) return new WindowsRemoteInventoryService.WinRmTestResult(winRmSettings.load().enabled(), false, "Bitte eine IP-Adresse oder einen Hostnamen angeben.", OffsetDateTime.now().toString());
    return windowsRemote.testConnection(host);
  }


  // 40k36q3: Mehrfach-Betriebssystemzugaenge. Die alten Einzelendpunkte bleiben als Fallback kompatibel.
  @GetMapping("/discovery/os-access-profiles")
  public java.util.List<OsAccessProfileRepository.Profile> osAccessProfiles(){ return osAccessProfiles.list(); }

  @PutMapping("/discovery/os-access-profiles")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public OsAccessProfileRepository.Profile saveOsAccessProfile(@RequestBody OsAccessProfileRepository.Update update, java.security.Principal principal){
    return osAccessProfiles.save(update, principal==null?"system":principal.getName());
  }

  @DeleteMapping("/discovery/os-access-profiles/{id}")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public java.util.Map<String,Object> deleteOsAccessProfile(@PathVariable long id){ osAccessProfiles.delete(id); return java.util.Map.of("deleted",true,"id",id); }

  @PostMapping("/discovery/os-access-profiles/{id}/test")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public java.util.Map<String,Object> testOsAccessProfile(@PathVariable long id){
    var p=osAccessProfiles.find(id);
    if(p==null) return java.util.Map.of("success",false,"message","Betriebssystemzugang wurde nicht gefunden.");
    long started=System.currentTimeMillis();
    if("SSH".equalsIgnoreCase(p.type())){
      var r=embeddedSsh.testDetailed(p.host(),p.port(),p.username(),p.authMode(),p.password(),p.keyPath());
      String inventoryMessage=r.commandOk()?deviceIdentityService.autoInventoryVerifiedSshHost(p.host(),p.name()):"";
      return java.util.Map.of("success",r.commandOk(),"message",r.message()+inventoryMessage,"durationMs",r.durationMs(),"handshakeOk",r.handshakeOk(),"authOk",r.authOk(),"autoInventory",r.commandOk());
    }
    if("WINRM".equalsIgnoreCase(p.type())){
      var r=windowsRemote.testConnection(p.host(),p.enabled(),p.username(),p.password(),p.port(),p.https());
      var result=new java.util.LinkedHashMap<String,Object>();
      result.put("success",r.reachable());
      result.put("message",r.message());
      result.put("durationMs",System.currentTimeMillis()-started);
      result.put("tcpReachable",r.tcpReachable());
      result.put("wsmanAttempted",r.wsmanAttempted());
      result.put("endpoint",r.endpoint());
      result.put("usernameFormat",r.usernameFormat());
      result.put("requestedAuthentication",r.requestedAuthentication());
      result.put("authenticationUsed",r.authenticationUsed());
      result.put("legacyFallbackUsed",r.legacyFallbackUsed());
      result.put("clientStack",r.clientStack());
      result.put("trustedTarget",r.trustedTarget());
      result.put("trustRequired",r.trustRequired());
      result.put("errorType",r.errorType());
      result.put("errorDetail",r.errorDetail());
      return result;
    }
    if("ADB".equalsIgnoreCase(p.type())){
      var r=androidAdb.connect(p.host(),p.port());
      return java.util.Map.of("success",r.success(),"message",r.message(),"durationMs",System.currentTimeMillis()-started);
    }
    return java.util.Map.of("success",false,"message","Unbekannter Zugangstyp: "+p.type());
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
