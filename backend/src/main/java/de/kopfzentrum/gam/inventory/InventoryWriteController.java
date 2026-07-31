package de.kopfzentrum.gam.inventory;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryWriteController {
  private final JdbcTemplate jdbc;
  private final InventoryRepository repository;
  private final DiscoveryRegistrationRepository registrations;
  private final InventoryIdentityLinkRepository identityLinks;

  public InventoryWriteController(JdbcTemplate jdbc, InventoryRepository repository, DiscoveryRegistrationRepository registrations, InventoryIdentityLinkRepository identityLinks) {
    this.jdbc = jdbc;
    this.repository = repository;
    this.registrations = registrations;
    this.identityLinks = identityLinks;
  }

  @PostMapping("/devices/new")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public InventoryDeviceDetail createNewDevice(@RequestBody InventoryDeviceUpdateRequest r) {
    String ip = clean(r.ip());
    String serial = clean(r.serialNumber());
    java.util.List<Integer> existing = jdbc.query("""
      SELECT `ID` FROM `geräte_neu`
      WHERE (? IS NOT NULL AND LOWER(COALESCE(`IP`,''))=LOWER(?))
         OR (? IS NOT NULL AND LOWER(COALESCE(`Seriennummer`,''))=LOWER(?))
      ORDER BY `ID` LIMIT 1
      """, (rs,rowNum)->rs.getInt(1), ip, ip, serial, serial);
    if(!existing.isEmpty()) return repository.detail("new", existing.get(0));
    jdbc.update("""
      INSERT INTO `geräte_neu` (`Name`,`Typ`,`Seriennummer`,`IP`,`Standort`)
      VALUES (?,?,?,?,?)
      """, clean(r.name()), clean(r.type()), clean(r.serialNumber()), clean(r.ip()), clean(r.location()));
    Integer id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
    upsertNewAssignment(id, r.branchId(), null);
    return repository.detail("new", id);
  }

  @PutMapping("/devices/new/{id}")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public InventoryDeviceDetail updateNewDevice(@PathVariable Integer id, @RequestBody InventoryDeviceUpdateRequest r) {
    jdbc.update("""
      UPDATE `geräte_neu`
      SET `Name`=?, `Typ`=?, `Seriennummer`=?, `IP`=?, `Standort`=?
      WHERE `ID`=?
      """, clean(r.name()), clean(r.type()), clean(r.serialNumber()), clean(r.ip()), clean(r.location()), id);
    if (r.branchId() != null) upsertNewAssignment(id, r.branchId(), null);
    return repository.detail("new", id);
  }

  @DeleteMapping("/devices/new/{id}")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public java.util.Map<String, Object> deleteNewDevice(@PathVariable Integer id) {
    jdbc.update("DELETE FROM `geräte_neu_vmaterial` WHERE `GERÄTEID`=?", id);
    jdbc.update("DELETE FROM `filiale_geräte_neu` WHERE `geräte_neu_id`=?", id);
    int deleted = jdbc.update("DELETE FROM `geräte_neu` WHERE `ID`=?", id);
    return java.util.Map.of("deleted", deleted, "id", id);
  }


  @GetMapping("/discovery/registered")
  public java.util.List<java.util.Map<String,Object>> registeredDiscoveryDevices() {
    return registrations.findAll();
  }

  @PostMapping("/discovery/register")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public java.util.Map<String,Object> registerDiscoveredDevice(@RequestBody java.util.Map<String,String> request){
    int changed=registrations.register(request.get("address"),request.get("hardwareAddress"),request.get("serialNumber"),request.get("name"),request.get("type"),request.get("manufacturer"),request.get("protocol"),request.get("status"));
    return java.util.Map.of("registered",changed);
  }

  @PostMapping("/discovery/register-all")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  @Transactional
  public java.util.Map<String,Object> registerAllDiscoveredDevices(@RequestBody java.util.List<java.util.Map<String,String>> devices){
    int changed=0;
    for(java.util.Map<String,String> request:devices){
      registrations.register(request.get("address"),request.get("hardwareAddress"),request.get("serialNumber"),request.get("name"),request.get("type"),request.get("manufacturer"),request.get("protocol"),request.get("status"));
      changed++;
    }
    return java.util.Map.of("registered", changed, "requested", devices.size(), "registeredTotal", registrations.count());
  }

  @PutMapping("/discovery/registered/device-type")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public java.util.Map<String,Object> updateRegisteredDeviceType(@RequestBody java.util.Map<String,String> request, java.security.Principal principal) {
    String identityKey=clean(request.get("identityKey"));
    String deviceType=clean(request.get("deviceType"));
    if(identityKey==null) throw new IllegalArgumentException("Identitätsschlüssel fehlt.");
    return registrations.updateDeviceType(identityKey, deviceType, principal==null?null:principal.getName());
  }

  @PutMapping("/discovery/registered/device-name")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public java.util.Map<String,Object> updateRegisteredDeviceName(@RequestBody java.util.Map<String,String> request) {
    String identityKey=clean(request.get("identityKey"));
    String name=clean(request.get("name"));
    if(identityKey==null) throw new IllegalArgumentException("Identitätsschlüssel fehlt.");
    return registrations.updateDeviceName(identityKey, name);
  }

  @PostMapping("/discovery/registered/{identityKey}/inventory")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public InventoryDeviceDetail moveRegisteredToInventory(@PathVariable String identityKey) {
    java.util.Map<String,Object> registered = registrations.find(identityKey);
    InventoryDeviceUpdateRequest request = new InventoryDeviceUpdateRequest(
      stringValue(registered.get("name")), stringValue(registered.get("deviceType")),
      firstNonBlank(stringValue(registered.get("serialNumber")), stringValue(registered.get("hardwareAddress"))),
      null, stringValue(registered.get("manufacturer")), stringValue(registered.get("address")),
      "Automatisch erkannt", null, false, false, true, true, true, null,
      "Aus registrierten Geräten in den Gerätebestand übernommen"
    );
    InventoryDeviceDetail detail = createNewDevice(request);
    // 40k34t: Verknüpfung zur kanonischen Discovery-Identität JETZT herstellen -
    // zu diesem Zeitpunkt ist die Zuordnung zu 100% sicher (kein Ratespiel), da wir
    // sowohl die Quelle (identityKey) als auch das gerade erzeugte Zielgerät kennen.
    // Vorher ging diese Verbindung durch das anschließende deregisterByKey()
    // unwiderruflich verloren - das war die eigentliche Ursache dafür, dass
    // Plattforminventarisierung/-status im Gerätebestand strukturell nicht
    // erscheinen konnten (siehe docs/40k34t-...).
    identityLinks.link(detail.device().source(), detail.device().id(), identityKey, "Verschieben in den Gerätebestand");
    registrations.deregisterByKey(identityKey);
    return detail;
  }

  /**
   * 40k34t: Best-Effort-Nachverknüpfung bereits vorhandener Gerätebestand-Einträge
   * mit einer aktuell noch existierenden, eindeutigen Discovery-Identität (siehe
   * InventoryIdentityLinkRepository für die genaue Zuordnungsreihenfolge). Rein
   * lokale Datenbankabfragen, kein Remote-Aufruf, idempotent (bereits verknüpfte
   * Geräte werden übersprungen).
   */
  @PostMapping("/devices/link-existing")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public InventoryIdentityLinkRepository.LinkSummary linkExistingDevices() {
    List<InventoryDevice> all = repository.search(new InventorySearchCriteria(null, null, null, null, 10000, 0));
    return identityLinks.linkExistingUnlinkedDevices(all);
  }

  // 40k34t: BEWUSST KEIN zweiter Plattforminventarisierungs-Endpunkt hier - sobald
  // ein Gerätebestand-Eintrag über InventoryDeviceDetail.identityKey verknüpft ist,
  // ruft das Frontend direkt den bereits bestehenden, generischen Endpunkt
  // POST /api/inventory/discovery/identity/platform-inventory (DeviceIdentityController,
  // seit 40k34p unverändert) auf - derselbe Dispatcher, dieselbe API, unabhängig
  // davon, ob die Aktion aus dem Geräteidentitäts-Dialog oder aus dem Gerätebestand
  // ausgelöst wird.

  @DeleteMapping("/discovery/registered/{identityKey}")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public java.util.Map<String,Object> revokeRegistration(@PathVariable String identityKey) {
    return java.util.Map.of("registrationRevoked", registrations.deregisterByKey(identityKey), "identityKey", identityKey);
  }

  /**
   * 40k31.1: Body-basierte Variante, damit zusammengesetzte Identitaetsschluessel
   * mit Doppelpunkten, Schraegstrichen oder weiteren Sonderzeichen nicht mehr
   * durch URL-/Firewall-Normalisierung als 403 abgewiesen werden.
   */
  @PostMapping("/discovery/registered/revoke")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public java.util.Map<String,Object> revokeRegistrationByBody(@RequestBody java.util.Map<String,String> request) {
    String identityKey = clean(request.get("identityKey"));
    if (identityKey == null) throw new IllegalArgumentException("Identitaetsschluessel fehlt.");
    return java.util.Map.of("registrationRevoked", registrations.deregisterByKey(identityKey), "identityKey", identityKey);
  }


  @DeleteMapping("/discovery/registered")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  @Transactional
  public java.util.Map<String,Object> revokeAllRegistrations() {
    int removed = registrations.deregisterAll();
    return java.util.Map.of("registrationsRevoked", removed);
  }

  @PostMapping("/devices/empty-to-registered")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  @Transactional
  public java.util.Map<String,Object> emptyEntireInventoryToRegistered() {
    java.util.List<java.util.Map<String,Object>> modern = jdbc.queryForList("SELECT `ID` AS id, `Name` AS name, `Typ` AS type, `Seriennummer` AS serialNumber, `IP` AS ip FROM `geräte_neu`");
    java.util.List<java.util.Map<String,Object>> legacy = jdbc.queryForList("SELECT `GERÄTE_ID` AS id, `GeräteName` AS name, `GeräteTyp` AS type, `Seriennummer` AS serialNumber, `Hersteller` AS manufacturer, CASE WHEN COALESCE(`AUSSERBETRIEB`,0)=0 THEN 'ONLINE' ELSE 'OFFLINE' END AS status FROM `geräte`");

    for (java.util.Map<String,Object> row : modern) {
      registrations.register(stringValue(row.get("ip")), null, stringValue(row.get("serialNumber")),
        stringValue(row.get("name")), stringValue(row.get("type")), null,
        "Gerätebestand GAM 2.0", "REGISTRIERT");
    }
    for (java.util.Map<String,Object> row : legacy) {
      registrations.register(null, null, stringValue(row.get("serialNumber")),
        stringValue(row.get("name")), stringValue(row.get("type")), stringValue(row.get("manufacturer")),
        "Gerätebestand GAM 1.0", stringValue(row.get("status")));
    }

    jdbc.update("DELETE FROM `geräte_neu_vmaterial`");
    jdbc.update("DELETE FROM `filiale_geräte_neu`");
    int modernRemoved = jdbc.update("DELETE FROM `geräte_neu`");
    int legacyRemoved = jdbc.update("DELETE FROM `geräte`");
    return java.util.Map.of("modernMoved", modernRemoved, "legacyMoved", legacyRemoved, "registeredTotal", registrations.count());
  }

  @PostMapping("/devices/new/{id}/remove-from-inventory")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public java.util.Map<String,Object> removeNewDeviceFromInventory(@PathVariable Integer id) {
    InventoryDeviceDetail detail = repository.detail("new", id);
    InventoryDevice device = detail.device();
    registrations.register(device.ip(), null, device.serialNumber(), device.name(), device.type(), device.manufacturer(), "Gerätebestand", device.active() ? "ONLINE" : "OFFLINE");
    jdbc.update("DELETE FROM `geräte_neu_vmaterial` WHERE `GERÄTEID`=?", id);
    jdbc.update("DELETE FROM `filiale_geräte_neu` WHERE `geräte_neu_id`=?", id);
    int deleted = jdbc.update("DELETE FROM `geräte_neu` WHERE `ID`=?", id);
    return java.util.Map.of("removedFromInventory", deleted, "id", id);
  }

  @PostMapping("/discovery/deregister")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public java.util.Map<String, Object> deregisterDiscoveredDevice(@RequestBody java.util.Map<String, String> request) {
    String ip = clean(request.get("address"));
    String name = clean(request.get("name"));
    String mac = clean(request.get("hardwareAddress"));
    String serial = clean(request.get("serialNumber"));
    int registryDeleted = registrations.deregister(ip, mac, serial, name);
    java.util.List<Integer> ids = jdbc.query("""
      SELECT `ID` FROM `geräte_neu`
      WHERE (? IS NOT NULL AND LOWER(COALESCE(`IP`,''))=LOWER(?))
         OR (? IS NOT NULL AND LOWER(COALESCE(`Name`,''))=LOWER(?))
      """, (rs, rowNum) -> rs.getInt(1), ip, ip, name, name);
    int deleted = 0;
    for (Integer id : ids) {
      jdbc.update("DELETE FROM `geräte_neu_vmaterial` WHERE `GERÄTEID`=?", id);
      jdbc.update("DELETE FROM `filiale_geräte_neu` WHERE `geräte_neu_id`=?", id);
      deleted += jdbc.update("DELETE FROM `geräte_neu` WHERE `ID`=?", id);
    }
    return java.util.Map.of("deregistered", deleted, "registrationDeleted", registryDeleted, "address", ip == null ? "" : ip, "name", name == null ? "" : name);
  }

  @PutMapping("/devices/legacy/{id}")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public InventoryDeviceDetail updateLegacyDevice(@PathVariable Integer id, @RequestBody InventoryDeviceUpdateRequest r) {
    Boolean outOfService = r.active() == null ? null : !r.active();
    jdbc.update("""
      UPDATE `geräte`
      SET `GeräteName`=?, `GeräteTyp`=?, `Seriennummer`=?, `Inventarnummer`=?, `Hersteller`=?,
          `INNERBETRIEBLICHER_STANDORT`=?, `Filiale`=?, `MEDGERÄTE`=?, `ELEKGERÄTE`=?, `INVENTAR`=?,
          `AUSSERBETRIEB`=?, `IMEINSATZ`=?, `Anschaffungsdatum`=?, `BEMERKUNG`=?
      WHERE `GERÄTE_ID`=?
      """, clean(r.name()), clean(r.type()), clean(r.serialNumber()), clean(r.inventoryNumber()), clean(r.manufacturer()),
      clean(r.location()), r.branchId(), r.medicalDevice(), r.electricalDevice(), r.inventory(), outOfService, r.inUse(), emptyToNull(r.acquisitionDate()), clean(r.note()), id);
    return repository.detail("legacy", id);
  }

  @PatchMapping("/devices/legacy/{id}/active")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public InventoryDeviceDetail setLegacyActive(@PathVariable Integer id, @RequestParam boolean active) {
    jdbc.update("UPDATE `geräte` SET `AUSSERBETRIEB`=? WHERE `GERÄTE_ID`=?", !active, id);
    return repository.detail("legacy", id);
  }

  @PostMapping("/devices/new/{id}/assignments")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public InventoryDeviceDetail setNewDeviceAssignment(@PathVariable Integer id, @RequestBody DeviceAssignmentRequest request) {
    upsertNewAssignment(id, request.branchId(), request.companyId());
    return repository.detail("new", id);
  }

  @PostMapping("/devices/new/{id}/materials")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public InventoryDeviceDetail addMaterial(@PathVariable Integer id, @RequestBody DeviceConsumableRequest request) {
    if (request.materialId() == null) throw new IllegalArgumentException("Material-ID fehlt.");
    Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM `geräte_neu_vmaterial` WHERE `GERÄTEID`=? AND `VMID`=?", Integer.class, id, request.materialId());
    if (count == null || count == 0) jdbc.update("INSERT INTO `geräte_neu_vmaterial` (`GERÄTEID`,`VMID`) VALUES (?,?)", id, request.materialId());
    return repository.detail("new", id);
  }

  @DeleteMapping("/devices/new/{id}/materials/{linkId}")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public InventoryDeviceDetail removeMaterial(@PathVariable Integer id, @PathVariable Integer linkId) {
    jdbc.update("DELETE FROM `geräte_neu_vmaterial` WHERE `ID`=? AND `GERÄTEID`=?", linkId, id);
    return repository.detail("new", id);
  }

  private void upsertNewAssignment(Integer deviceId, Integer branchId, Integer companyId) {
    if (deviceId == null || (branchId == null && companyId == null)) return;
    Integer existing = null;
    try {
      existing = jdbc.queryForObject("SELECT `ID` FROM `filiale_geräte_neu` WHERE `geräte_neu_id`=? ORDER BY `ID` LIMIT 1", Integer.class, deviceId);
    } catch (EmptyResultDataAccessException ignored) {}
    if (existing == null) {
      jdbc.update("INSERT INTO `filiale_geräte_neu` (`rfiliale_ID`,`rgesellschafts_ID`,`geräte_neu_id`) VALUES (?,?,?)", branchId, companyId, deviceId);
    } else {
      if (branchId != null && companyId != null) jdbc.update("UPDATE `filiale_geräte_neu` SET `rfiliale_ID`=?, `rgesellschafts_ID`=? WHERE `ID`=?", branchId, companyId, existing);
      else if (branchId != null) jdbc.update("UPDATE `filiale_geräte_neu` SET `rfiliale_ID`=? WHERE `ID`=?", branchId, existing);
      else jdbc.update("UPDATE `filiale_geräte_neu` SET `rgesellschafts_ID`=? WHERE `ID`=?", companyId, existing);
    }
  }

  private static String stringValue(Object value) {
    return value == null ? null : clean(String.valueOf(value));
  }

  private static String firstNonBlank(String first, String second) {
    return first != null ? first : second;
  }

  private static String clean(String value) {
    if (value == null) return null;
    String trimmed = value.replace("\r", " ").replace("\n", " ").trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private static String emptyToNull(String value) {
    String cleaned = clean(value);
    return cleaned == null ? null : cleaned;
  }
}
