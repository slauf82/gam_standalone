package de.kopfzentrum.gam.link;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class InventoryWarehouseRepository {
  private final JdbcTemplate jdbc;
  private final NamedParameterJdbcTemplate named;

  public InventoryWarehouseRepository(JdbcTemplate jdbc, NamedParameterJdbcTemplate named) {
    this.jdbc = jdbc;
    this.named = named;
  }

  @PostConstruct
  public void ensureJournalTable() {
    jdbc.execute("""
      CREATE TABLE IF NOT EXISTS `gam_materialbewegung` (
        `ID` BIGINT NOT NULL AUTO_INCREMENT,
        `CREATED_AT` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        `GERAET_ID` INT NULL,
        `MATERIAL_ID` INT NOT NULL,
        `DELTA` INT NOT NULL,
        `BESTAND_ALT` INT NULL,
        `BESTAND_NEU` INT NULL,
        `GRUND` VARCHAR(255) NULL,
        `USERNAME` VARCHAR(100) NULL,
        PRIMARY KEY (`ID`),
        KEY `IDX_gam_materialbewegung_material` (`MATERIAL_ID`),
        KEY `IDX_gam_materialbewegung_geraet` (`GERAET_ID`)
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
      """);
  }

  public List<DeviceMaterialLink> links(Integer deviceId, Integer materialId) {
    MapSqlParameterSource p = new MapSqlParameterSource();
    StringBuilder sql = new StringBuilder("""
      SELECT gv.`ID` AS link_id, g.`ID` AS device_id, g.`Name` AS device_name,
             v.`ID` AS material_id, v.`Name` AS material_name, v.`Eigenschaften` AS material_type,
             v.`Anzahl` AS stock, v.`EMail_Hersteller` AS manufacturer_email
      FROM `geräte_neu_vmaterial` gv
      JOIN `geräte_neu` g ON g.`ID` = gv.`GERÄTEID`
      JOIN `verbrauchsmaterial` v ON v.`ID` = gv.`VMID`
      WHERE 1=1
      """);
    if (deviceId != null) { sql.append(" AND gv.`GERÄTEID` = :deviceId"); p.addValue("deviceId", deviceId); }
    if (materialId != null) { sql.append(" AND gv.`VMID` = :materialId"); p.addValue("materialId", materialId); }
    sql.append(" ORDER BY g.`Name`, v.`Name`");
    return named.query(sql.toString(), p, (rs, row) -> mapLink(rs));
  }

  @Transactional
  public DeviceMaterialLink addLink(Integer deviceId, Integer materialId) {
    if (deviceId == null || materialId == null) throw new IllegalArgumentException("Gerät und Material müssen gesetzt sein.");
    Integer existing = jdbc.queryForObject("SELECT COUNT(*) FROM `geräte_neu_vmaterial` WHERE `GERÄTEID`=? AND `VMID`=?", Integer.class, deviceId, materialId);
    if (existing == null || existing == 0) {
      jdbc.update("INSERT INTO `geräte_neu_vmaterial` (`GERÄTEID`,`VMID`) VALUES (?,?)", deviceId, materialId);
    }
    return links(deviceId, materialId).stream().findFirst().orElseThrow();
  }

  @Transactional
  public void removeLink(Integer linkId) {
    if (linkId == null) throw new IllegalArgumentException("Link-ID fehlt.");
    jdbc.update("DELETE FROM `geräte_neu_vmaterial` WHERE `ID`=?", linkId);
  }

  @Transactional
  public MaterialBookingResult book(MaterialBookingRequest request) {
    if (request.materialId() == null) throw new IllegalArgumentException("Material-ID fehlt.");
    int requested = request.quantity() == null ? 1 : Math.abs(request.quantity());
    String direction = request.direction() == null ? "OUT" : request.direction().trim().toUpperCase();
    int delta = ("IN".equals(direction) || "PLUS".equals(direction) || "ZUGANG".equals(direction)) ? requested : -requested;

    MaterialRow row = jdbc.queryForObject("SELECT `ID`,`Name`,COALESCE(`Anzahl`,0) AS stock FROM `verbrauchsmaterial` WHERE `ID`=? FOR UPDATE", (rs, n) -> new MaterialRow(rs.getInt("ID"), rs.getString("Name"), rs.getInt("stock")), request.materialId());
    int next = Math.max(0, row.stock() + delta);
    int effectiveDelta = next - row.stock();
    jdbc.update("UPDATE `verbrauchsmaterial` SET `Anzahl`=? WHERE `ID`=?", next, request.materialId());
    String deviceName = null;
    if (request.deviceId() != null) {
      addLink(request.deviceId(), request.materialId());
      deviceName = jdbc.query("SELECT `Name` FROM `geräte_neu` WHERE `ID`=?", rs -> rs.next() ? rs.getString(1) : null, request.deviceId());
    }
    jdbc.update("""
      INSERT INTO `gam_materialbewegung` (`GERAET_ID`,`MATERIAL_ID`,`DELTA`,`BESTAND_ALT`,`BESTAND_NEU`,`GRUND`,`USERNAME`)
      VALUES (?,?,?,?,?,?,?)
      """, request.deviceId(), request.materialId(), effectiveDelta, row.stock(), next, request.reason(), request.username());
    return new MaterialBookingResult(request.materialId(), row.name(), row.stock(), effectiveDelta, next, request.deviceId(), deviceName, request.reason());
  }

  public List<MaterialMovement> movements(Integer deviceId, Integer materialId, int limit) {
    MapSqlParameterSource p = new MapSqlParameterSource().addValue("limit", Math.min(Math.max(limit <= 0 ? 100 : limit, 1), 500));
    StringBuilder sql = new StringBuilder("""
      SELECT m.`ID`, m.`CREATED_AT`, m.`GERAET_ID`, g.`Name` AS device_name, m.`MATERIAL_ID`, v.`Name` AS material_name,
             m.`DELTA`, m.`BESTAND_ALT`, m.`BESTAND_NEU`, m.`GRUND`, m.`USERNAME`
      FROM `gam_materialbewegung` m
      LEFT JOIN `geräte_neu` g ON g.`ID` = m.`GERAET_ID`
      LEFT JOIN `verbrauchsmaterial` v ON v.`ID` = m.`MATERIAL_ID`
      WHERE 1=1
      """);
    if (deviceId != null) { sql.append(" AND m.`GERAET_ID`=:deviceId"); p.addValue("deviceId", deviceId); }
    if (materialId != null) { sql.append(" AND m.`MATERIAL_ID`=:materialId"); p.addValue("materialId", materialId); }
    sql.append(" ORDER BY m.`CREATED_AT` DESC, m.`ID` DESC LIMIT :limit");
    return named.query(sql.toString(), p, (rs, row) -> mapMovement(rs));
  }

  private DeviceMaterialLink mapLink(ResultSet rs) throws SQLException {
    return new DeviceMaterialLink(rs.getInt("link_id"), rs.getInt("device_id"), rs.getString("device_name"), rs.getInt("material_id"), rs.getString("material_name"), rs.getString("material_type"), getInteger(rs, "stock"), rs.getString("manufacturer_email"));
  }

  private MaterialMovement mapMovement(ResultSet rs) throws SQLException {
    return new MaterialMovement(rs.getLong("ID"), rs.getTimestamp("CREATED_AT").toLocalDateTime(), getInteger(rs, "GERAET_ID"), rs.getString("device_name"), rs.getInt("MATERIAL_ID"), rs.getString("material_name"), rs.getInt("DELTA"), getInteger(rs, "BESTAND_ALT"), getInteger(rs, "BESTAND_NEU"), rs.getString("GRUND"), rs.getString("USERNAME"));
  }

  private static Integer getInteger(ResultSet rs, String col) throws SQLException { int v = rs.getInt(col); return rs.wasNull() ? null : v; }
  private record MaterialRow(Integer id, String name, Integer stock) {}
}
