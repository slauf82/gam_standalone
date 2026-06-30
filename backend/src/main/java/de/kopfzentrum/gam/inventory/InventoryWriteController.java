package de.kopfzentrum.gam.inventory;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
public class InventoryWriteController {
  private final JdbcTemplate jdbc;
  private final InventoryRepository repository;

  public InventoryWriteController(JdbcTemplate jdbc, InventoryRepository repository) {
    this.jdbc = jdbc;
    this.repository = repository;
  }

  @PostMapping("/devices/new")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public InventoryDeviceDetail createNewDevice(@RequestBody InventoryDeviceUpdateRequest r) {
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
