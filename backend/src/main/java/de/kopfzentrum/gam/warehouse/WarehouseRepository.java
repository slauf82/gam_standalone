package de.kopfzentrum.gam.warehouse;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Stream;

@Repository
public class WarehouseRepository {
  private final JdbcTemplate jdbc;
  private final NamedParameterJdbcTemplate named;

  public WarehouseRepository(JdbcTemplate jdbc, NamedParameterJdbcTemplate named) {
    this.jdbc = jdbc;
    this.named = named;
  }

  public List<WarehouseItem> search(WarehouseSearchCriteria criteria) {
    String kind = criteria.kind() == null ? "all" : criteria.kind().trim().toLowerCase();
    if ("lager".equals(kind) || "storage".equals(kind)) return searchStorage(criteria);
    if ("verbrauch".equals(kind) || "verbrauchsmaterial".equals(kind) || "consumable".equals(kind)) return searchConsumables(criteria);
    return Stream.concat(searchStorage(criteria).stream(), searchConsumables(criteria).stream())
      .limit(normalizeLimit(criteria.limit()))
      .toList();
  }

  public WarehouseItem detail(String kind, Integer id) {
    if (id == null) throw new IllegalArgumentException("Lager-/Material-ID fehlt.");
    String normalized = kind == null ? "lager" : kind.trim().toLowerCase();
    if ("verbrauch".equals(normalized) || "verbrauchsmaterial".equals(normalized) || "consumable".equals(normalized)) return findConsumable(id);
    return findStorage(id);
  }

  public WarehouseItem moveStock(String kind, Integer id, Double delta) {
    if (delta == null) throw new IllegalArgumentException("Mengenänderung fehlt.");
    WarehouseItem current = detail(kind, id);
    double next = (current.quantity() == null ? 0.0 : current.quantity()) + delta;
    return updateStock(kind, id, next);
  }



  public WarehouseItem create(String kind, WarehouseItemRequest request) {
    String normalized = normalizeKind(kind == null || kind.isBlank() ? request.kind() : kind);
    if ("verbrauchsmaterial".equals(normalized)) {
      String name = text(request.name(), request.description());
      if (name == null || name.isBlank()) throw new IllegalArgumentException("Materialname fehlt.");
      jdbc.update("INSERT INTO `verbrauchsmaterial` (`Name`,`Eigenschaften`,`Anzahl`,`EMail_Hersteller`) VALUES (?,?,?,?)",
        name, blankToNull(request.properties()), request.quantity() == null ? null : request.quantity().intValue(), emailOrDefault(request.manufacturerEmail()));
      Integer id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
      return findConsumable(id);
    }
    String description = text(request.description(), request.name());
    if (description == null || description.isBlank()) throw new IllegalArgumentException("Beschreibung fehlt.");
    jdbc.update("INSERT INTO `lager` (`BESCHREIBUNG`,`LAGERORT`,`MENGE`) VALUES (?,?,?)",
      description, blankToNull(request.location()), request.quantity());
    Integer id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
    return findStorage(id);
  }

  public WarehouseItem update(String kind, Integer id, WarehouseItemRequest request) {
    if (id == null) throw new IllegalArgumentException("Lager-/Material-ID fehlt.");
    String normalized = normalizeKind(kind == null || kind.isBlank() ? request.kind() : kind);
    if ("verbrauchsmaterial".equals(normalized)) {
      String name = text(request.name(), request.description());
      if (name == null || name.isBlank()) throw new IllegalArgumentException("Materialname fehlt.");
      jdbc.update("UPDATE `verbrauchsmaterial` SET `Name`=?, `Eigenschaften`=?, `Anzahl`=?, `EMail_Hersteller`=? WHERE `ID`=?",
        name, blankToNull(request.properties()), request.quantity() == null ? null : request.quantity().intValue(), emailOrDefault(request.manufacturerEmail()), id);
      return findConsumable(id);
    }
    String description = text(request.description(), request.name());
    if (description == null || description.isBlank()) throw new IllegalArgumentException("Beschreibung fehlt.");
    jdbc.update("UPDATE `lager` SET `BESCHREIBUNG`=?, `LAGERORT`=?, `MENGE`=? WHERE `CODE`=?",
      description, blankToNull(request.location()), request.quantity(), id);
    return findStorage(id);
  }

  public void delete(String kind, Integer id) {
    if (id == null) throw new IllegalArgumentException("Lager-/Material-ID fehlt.");
    String normalized = normalizeKind(kind);
    if ("verbrauchsmaterial".equals(normalized)) {
      jdbc.update("DELETE FROM `geräte_neu_vmaterial` WHERE `VMID`=?", id);
      jdbc.update("DELETE FROM `verbrauchsmaterial` WHERE `ID`=?", id);
      return;
    }
    jdbc.update("DELETE FROM `lager` WHERE `CODE`=?", id);
  }
  public WarehouseStats stats() {
    long storageCount = count("SELECT COUNT(*) FROM `lager`");
    long consumableCount = count("SELECT COUNT(*) FROM `verbrauchsmaterial`");
    long withStock = count("SELECT COUNT(*) FROM `verbrauchsmaterial` WHERE COALESCE(`Anzahl`,0) > 0");
    long withoutStock = count("SELECT COUNT(*) FROM `verbrauchsmaterial` WHERE COALESCE(`Anzahl`,0) <= 0");
    long links = count("SELECT COUNT(*) FROM `geräte_neu_vmaterial`");
    Double storageQty = jdbc.queryForObject("SELECT COALESCE(SUM(`MENGE`),0) FROM `lager`", Double.class);
    Double consumableQty = jdbc.queryForObject("SELECT COALESCE(SUM(`Anzahl`),0) FROM `verbrauchsmaterial`", Double.class);
    return new WarehouseStats(storageCount, consumableCount, withStock, withoutStock, links, storageQty == null ? 0 : storageQty, consumableQty == null ? 0 : consumableQty);
  }

  public WarehouseItem updateStock(String kind, Integer id, Double quantity) {
    if (quantity == null) throw new IllegalArgumentException("Menge fehlt.");
    String normalized = normalizeKind(kind);
    if ("verbrauchsmaterial".equals(normalized)) {
      jdbc.update("UPDATE `verbrauchsmaterial` SET `Anzahl` = ? WHERE `ID` = ?", quantity.intValue(), id);
      return findConsumable(id);
    }
    jdbc.update("UPDATE `lager` SET `MENGE` = ? WHERE `CODE` = ?", quantity, id);
    return findStorage(id);
  }

  private List<WarehouseItem> searchStorage(WarehouseSearchCriteria c) {
    int limit = normalizeLimit(c.limit());
    int offset = Math.max(c.offset(), 0);
    MapSqlParameterSource p = new MapSqlParameterSource().addValue("limit", limit).addValue("offset", offset);
    StringBuilder sql = new StringBuilder("""
      SELECT l.`CODE`, l.`BESCHREIBUNG`, l.`LAGERORT`, l.`MENGE`
      FROM `lager` l
      WHERE 1=1
      """);
    if (c.q() != null && !c.q().isBlank()) {
      sql.append(" AND (l.`BESCHREIBUNG` LIKE :q OR l.`LAGERORT` LIKE :q) ");
      p.addValue("q", "%" + c.q().trim() + "%");
    }
    if (Boolean.TRUE.equals(c.onlyWithStock())) sql.append(" AND COALESCE(l.`MENGE`,0) > 0 ");
    sql.append(" ORDER BY l.`BESCHREIBUNG` ASC LIMIT :limit OFFSET :offset");
    return named.query(sql.toString(), p, (rs, row) -> mapStorage(rs));
  }

  private List<WarehouseItem> searchConsumables(WarehouseSearchCriteria c) {
    int limit = normalizeLimit(c.limit());
    int offset = Math.max(c.offset(), 0);
    MapSqlParameterSource p = new MapSqlParameterSource().addValue("limit", limit).addValue("offset", offset);
    StringBuilder sql = new StringBuilder("""
      SELECT v.`ID`, v.`Name`, v.`Eigenschaften`, v.`Anzahl`, v.`EMail_Hersteller`, COUNT(gv.`ID`) AS linked_devices
      FROM `verbrauchsmaterial` v
      LEFT JOIN `geräte_neu_vmaterial` gv ON gv.`VMID` = v.`ID`
      WHERE 1=1
      """);
    if (c.q() != null && !c.q().isBlank()) {
      sql.append(" AND (v.`Name` LIKE :q OR v.`Eigenschaften` LIKE :q OR v.`EMail_Hersteller` LIKE :q) ");
      p.addValue("q", "%" + c.q().trim() + "%");
    }
    if (Boolean.TRUE.equals(c.onlyWithStock())) sql.append(" AND COALESCE(v.`Anzahl`,0) > 0 ");
    sql.append(" GROUP BY v.`ID` ORDER BY v.`Name` ASC LIMIT :limit OFFSET :offset");
    return named.query(sql.toString(), p, (rs, row) -> mapConsumable(rs));
  }

  private WarehouseItem findStorage(Integer id) {
    return jdbc.queryForObject("SELECT `CODE`, `BESCHREIBUNG`, `LAGERORT`, `MENGE` FROM `lager` WHERE `CODE` = ?", (rs, row) -> mapStorage(rs), id);
  }

  private WarehouseItem findConsumable(Integer id) {
    return jdbc.queryForObject("""
      SELECT v.`ID`, v.`Name`, v.`Eigenschaften`, v.`Anzahl`, v.`EMail_Hersteller`, COUNT(gv.`ID`) AS linked_devices
      FROM `verbrauchsmaterial` v
      LEFT JOIN `geräte_neu_vmaterial` gv ON gv.`VMID` = v.`ID`
      WHERE v.`ID` = ?
      GROUP BY v.`ID`
      """, (rs, row) -> mapConsumable(rs), id);
  }

  private WarehouseItem mapStorage(ResultSet rs) throws SQLException {
    return new WarehouseItem(getInt(rs, "CODE"), "lager", rs.getString("BESCHREIBUNG"), rs.getString("BESCHREIBUNG"), rs.getString("LAGERORT"), getDouble(rs,"MENGE"), null, null, 0L);
  }

  private WarehouseItem mapConsumable(ResultSet rs) throws SQLException {
    return new WarehouseItem(getInt(rs, "ID"), "verbrauchsmaterial", rs.getString("Name"), rs.getString("Name"), null, getDouble(rs,"Anzahl"), rs.getString("Eigenschaften"), rs.getString("EMail_Hersteller"), getLong(rs,"linked_devices"));
  }

  private String normalizeKind(String kind) {
    String normalized = kind == null ? "lager" : kind.trim().toLowerCase();
    if ("verbrauch".equals(normalized) || "verbrauchsmaterial".equals(normalized) || "consumable".equals(normalized) || "material".equals(normalized)) return "verbrauchsmaterial";
    return "lager";
  }

  private static String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
  private static String text(String primary, String fallback) { return primary != null && !primary.isBlank() ? primary.trim() : blankToNull(fallback); }
  private static String emailOrDefault(String value) { return value == null || value.isBlank() ? "kopfzentrum@prosoft-krippner.com" : value.trim(); }
  private long count(String sql) { Long c = jdbc.queryForObject(sql, Long.class); return c == null ? 0 : c; }
  private int normalizeLimit(int requested) { return Math.min(Math.max(requested <= 0 ? 100 : requested, 1), 500); }
  private static Integer getInt(ResultSet rs, String col) throws SQLException { int v = rs.getInt(col); return rs.wasNull() ? null : v; }
  private static Long getLong(ResultSet rs, String col) throws SQLException { long v = rs.getLong(col); return rs.wasNull() ? null : v; }
  private static Double getDouble(ResultSet rs, String col) throws SQLException { double v = rs.getDouble(col); return rs.wasNull() ? null : v; }
}
