package de.kopfzentrum.gam.inventory;

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
      """, r.name(), r.type(), r.serialNumber(), r.ip(), r.location());
    Integer id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
    if (r.branchId() != null) {
      jdbc.update("INSERT INTO `filiale_geräte_neu` (`rfiliale_ID`,`geräte_neu_id`) VALUES (?,?)", r.branchId(), id);
    }
    return repository.detail("new", id);
  }

  @PutMapping("/devices/new/{id}")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public InventoryDeviceDetail updateNewDevice(@PathVariable Integer id, @RequestBody InventoryDeviceUpdateRequest r) {
    jdbc.update("""
      UPDATE `geräte_neu` SET `Name`=?, `Typ`=?, `Seriennummer`=?, `IP`=?, `Standort`=? WHERE `ID`=?
      """, r.name(), r.type(), r.serialNumber(), r.ip(), r.location(), id);
    if (r.branchId() != null) {
      Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM `filiale_geräte_neu` WHERE `geräte_neu_id`=?", Integer.class, id);
      if (count != null && count > 0) jdbc.update("UPDATE `filiale_geräte_neu` SET `rfiliale_ID`=? WHERE `geräte_neu_id`=?", r.branchId(), id);
      else jdbc.update("INSERT INTO `filiale_geräte_neu` (`rfiliale_ID`,`geräte_neu_id`) VALUES (?,?)", r.branchId(), id);
    }
    return repository.detail("new", id);
  }

  @PutMapping("/devices/legacy/{id}")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public InventoryDeviceDetail updateLegacyDevice(@PathVariable Integer id, @RequestBody InventoryDeviceUpdateRequest r) {
    Boolean outOfService = r.active() == null ? null : !r.active();
    jdbc.update("""
      UPDATE `geräte` SET `GeräteName`=?, `GeräteTyp`=?, `Seriennummer`=?, `Inventarnummer`=?, `Hersteller`=?,
        `INNERBETRIEBLICHER_STANDORT`=?, `Filiale`=?, `MEDGERÄTE`=?, `ELEKGERÄTE`=?, `INVENTAR`=?, `AUSSERBETRIEB`=?, `IMEINSATZ`=?, `Anschaffungsdatum`=?, `BEMERKUNG`=?
      WHERE `GERÄTE_ID`=?
      """, r.name(), r.type(), r.serialNumber(), r.inventoryNumber(), r.manufacturer(), r.location(), r.branchId(), r.medicalDevice(), r.electricalDevice(), r.inventory(), outOfService, r.inUse(), r.acquisitionDate(), r.note(), id);
    return repository.detail("legacy", id);
  }
}
