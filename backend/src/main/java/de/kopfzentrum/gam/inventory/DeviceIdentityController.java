package de.kopfzentrum.gam.inventory;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 40k33b5: REST-Endpunkte für Geräteidentität, Quellenübersicht und
 * Identitätsverwaltung. Identitätsschlüssel werden bewusst NICHT als Pfad-
 * Parameter übergeben, sondern als Query-Parameter (Lesezugriffe) bzw. im
 * Request-Body (Schreibzugriffe) - siehe bereits bestehende "40k31.1"-Regel in
 * InventoryWriteController: zusammengesetzte Identitätsschlüssel mit
 * Doppelpunkten wurden dort in der Vergangenheit durch URL-/Firewall-
 * Normalisierung als 403 abgewiesen, wenn sie Teil des Pfads waren.
 */
@RestController
@RequestMapping("/api/inventory/discovery/identity")
public class DeviceIdentityController {
  private final DeviceIdentityService service;
  public DeviceIdentityController(DeviceIdentityService service) { this.service = service; }

  @GetMapping("/overview")
  public List<DeviceIdentityService.IdentityRow> overview() {
    return service.overview();
  }

  @GetMapping("/detail")
  public DeviceIdentityService.IdentityRow detail(@RequestParam String identityKey) {
    return service.detail(identityKey);
  }

  @GetMapping("/history")
  public List<DeviceIdentityService.HistoryEntry> history(@RequestParam String identityKey) {
    return service.history(identityKey);
  }

  @PostMapping("/reassess")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public DeviceIdentityService.ReassessResult reassess(@RequestBody Map<String,String> request) {
    return service.reassess(requireKey(request));
  }

  @PostMapping("/alias")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public Map<String,Object> addAlias(@RequestBody Map<String,String> request) {
    String alias = request.get("alias");
    if (alias == null || alias.isBlank()) throw new IllegalArgumentException("Aliasname fehlt.");
    return service.addAlias(requireKey(request), alias);
  }

  @PutMapping("/alias")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public Map<String,Object> renameAlias(@RequestBody Map<String,String> request) {
    String oldAlias = request.get("oldAlias"), newAlias = request.get("newAlias");
    if (oldAlias == null || newAlias == null) throw new IllegalArgumentException("Alter und neuer Aliasname werden benötigt.");
    return service.renameAlias(requireKey(request), oldAlias, newAlias);
  }

  @PostMapping("/alias/remove")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public Map<String,Object> removeAlias(@RequestBody Map<String,String> request) {
    String alias = request.get("alias");
    if (alias == null || alias.isBlank()) throw new IllegalArgumentException("Aliasname fehlt.");
    return service.removeAlias(requireKey(request), alias);
  }

  @PutMapping("/main-name")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public Map<String,Object> setMainName(@RequestBody Map<String,String> request) {
    String name = request.get("name");
    if (name == null || name.isBlank()) throw new IllegalArgumentException("Neuer Hauptname fehlt.");
    return service.setMainName(requireKey(request), name);
  }

  @GetMapping("/integrity")
  public DeviceIdentityService.IntegrityResult integrity(@RequestParam String identityKey) {
    return service.checkIntegrity(identityKey);
  }

  @GetMapping("/split-candidates")
  public List<DeviceIdentityService.SplitCandidate> splitCandidates(@RequestParam String identityKey) {
    return service.splitCandidates(identityKey);
  }

  @PostMapping("/split")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public Map<String,Object> split(@RequestBody Map<String,String> request, java.security.Principal principal) {
    String ref = request.get("ref");
    if (ref == null || ref.isBlank()) throw new IllegalArgumentException("Auswahl für die Wiederauftrennung fehlt.");
    return service.split(requireKey(request), ref, principal == null ? null : principal.getName());
  }

  @PostMapping("/linux/inventory")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public DeviceIdentityService.LinuxActionResult runLinuxInventory(@RequestBody Map<String,String> request) {
    return service.runLinuxInventory(requireKey(request));
  }

  @PostMapping("/macos/inventory")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public DeviceIdentityService.LinuxActionResult runMacOsInventory(@RequestBody Map<String,String> request) { return service.runMacOsInventory(requireKey(request)); }

  @PostMapping("/macos/ssh-test")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public MacOsInventoryService.TestResult testMacOsSsh(@RequestBody Map<String,String> request) { return service.testMacOsSsh(requireKey(request)); }

  @PostMapping("/linux/ssh-test")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public LinuxNetworkDiscoveryService.SshTestResult testLinuxSsh(@RequestBody Map<String,String> request) {
    return service.testLinuxSsh(requireKey(request));
  }

  @PostMapping("/linux/refresh-cache")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public Map<String,Object> refreshLinuxCache(@RequestBody Map<String,String> request) {
    service.refreshLinuxCache(requireKey(request));
    return Map.of("refreshed", true);
  }

  @GetMapping("/android/adb-status")
  public AndroidAdbService.AdbStatus androidAdbStatus() {
    return service.androidAdbStatus();
  }

  @PostMapping("/android/pair")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public AndroidAdbService.ActionResult pairAndroidDevice(@RequestBody Map<String,Object> request) {
    String host = String.valueOf(request.get("host"));
    int pairingPort = toInt(request.get("pairingPort"));
    String code = String.valueOf(request.get("pairingCode"));
    return service.pairAndroidDevice(host, pairingPort, code);
  }

  @PostMapping("/android/connect")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public AndroidAdbService.ActionResult connectAndroidDevice(@RequestBody Map<String,Object> request) {
    String host = String.valueOf(request.get("host"));
    int port = toInt(request.get("port"));
    Object identityKey = request.get("identityKey");
    return service.connectAndroidDevice(identityKey == null ? null : String.valueOf(identityKey), host, port);
  }

  @PostMapping("/android/disconnect")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public AndroidAdbService.ActionResult disconnectAndroidDevice(@RequestBody Map<String,Object> request) {
    String host = String.valueOf(request.get("host"));
    int port = toInt(request.get("port"));
    return service.disconnectAndroidDevice(host, port);
  }

  @PostMapping("/android/inventory")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public DeviceIdentityService.LinuxActionResult runAndroidInventory(@RequestBody Map<String,String> request) {
    return service.runAndroidInventory(requireKey(request));
  }

  @PostMapping("/android/reconnect-known")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public Map<String,Object> reconnectKnownAndroidDevices() {
    return Map.of("results", service.reconnectKnownAndroidDevices());
  }

  @GetMapping("/android/apps")
  public List<Map<String,Object>> androidInstalledApps(@RequestParam String identityKey) {
    return service.androidInstalledApps(identityKey);
  }

  @GetMapping("/android/apps/runs")
  public List<Map<String,Object>> androidAppRunHistory(@RequestParam String identityKey, @RequestParam(defaultValue = "10") int limit) {
    return service.androidAppRunHistory(identityKey, limit);
  }

  @GetMapping("/android/apps/global-summary")
  public Map<String,Object> androidAppsGlobalSummary() {
    return service.androidAppsGlobalSummary();
  }

  @PostMapping("/android/apps/inventory")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public DeviceIdentityService.AppInventoryRunResult runAndroidAppInventory(@RequestBody Map<String,String> request) {
    return service.runAndroidAppInventory(requireKey(request));
  }

  // 40k34m: Plattforminventarisierung - eine zentrale, plattformübergreifende
  // Erweiterung der bestehenden Inventarisierungs-Endpunkte (dieselbe Berechtigung,
  // dasselbe Principal-Muster wie split() oben).
  @PostMapping("/platform-inventory")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public DeviceIdentityService.PlatformInventoryResult runPlatformInventory(@RequestBody Map<String,String> request, java.security.Principal principal) {
    String platform = request.get("platform");
    if (platform == null || platform.isBlank()) throw new IllegalArgumentException("Plattform fehlt.");
    return service.runPlatformInventory(requireKey(request), platform, principal == null ? null : principal.getName());
  }

  @PostMapping("/platform-inventory/all")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public List<DeviceIdentityService.PlatformInventoryResult> runAllKnownPlatforms(@RequestBody Map<String,String> request, java.security.Principal principal) {
    return service.runAllKnownPlatforms(requireKey(request), principal == null ? null : principal.getName());
  }

  @GetMapping("/platform-inventory/status")
  public List<Map<String,Object>> platformInventoryStatus(@RequestParam String identityKey) {
    return service.platformInventoryStatus(identityKey);
  }

  private static int toInt(Object value) {
    if (value == null) throw new IllegalArgumentException("Port fehlt.");
    try { return Integer.parseInt(String.valueOf(value).trim()); }
    catch (NumberFormatException e) { throw new IllegalArgumentException("Ungültiger Port."); }
  }

  private static String requireKey(Map<String,String> request) {
    String key = request.get("identityKey");
    if (key == null || key.isBlank()) throw new IllegalArgumentException("Identitätsschlüssel fehlt.");
    return key;
  }
}
