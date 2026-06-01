package de.kopfzentrum.gam.inventory;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class InventoryRepository {
  private final JdbcTemplate jdbc;
  private final NamedParameterJdbcTemplate named;

  public InventoryRepository(JdbcTemplate jdbc, NamedParameterJdbcTemplate named) {
    this.jdbc = jdbc;
    this.named = named;
  }

  public List<InventoryDevice> search(InventorySearchCriteria criteria) {
    String source = criteria.source() == null ? "all" : criteria.source().trim().toLowerCase();
    if ("legacy".equals(source)) return searchLegacy(criteria);
    if ("new".equals(source) || "geraete_neu".equals(source) || "geräte_neu".equals(source)) return searchNew(criteria);
    List<InventoryDevice> legacy = searchLegacy(criteria);
    List<InventoryDevice> modern = searchNew(criteria);
    return java.util.stream.Stream.concat(legacy.stream(), modern.stream())
      .limit(normalizeLimit(criteria.limit()))
      .toList();
  }

  public InventoryDeviceDetail detail(String source, Integer id) {
    if (id == null) throw new IllegalArgumentException("Geräte-ID fehlt.");
    String normalized = source == null ? "legacy" : source.trim().toLowerCase();
    InventoryDevice device = ("new".equals(normalized) || "geraete_neu".equals(normalized) || "geräte_neu".equals(normalized))
      ? findNew(id) : findLegacy(id);
    return new InventoryDeviceDetail(device, assignmentsForNewDevice(normalized, id), consumablesForNewDevice(normalized, id), softwareForDevice(normalized, id));
  }

  public InventoryStats stats() {
    long legacy = count("SELECT COUNT(*) FROM `geräte`");
    long modern = count("SELECT COUNT(*) FROM `geräte_neu`");
    long assignments = count("SELECT COUNT(*) FROM `filiale_geräte_neu`");
    long medical = count("SELECT COUNT(*) FROM `geräte` WHERE COALESCE(`MEDGERÄTE`,0) <> 0");
    long electrical = count("SELECT COUNT(*) FROM `geräte` WHERE COALESCE(`ELEKGERÄTE`,0) <> 0");
    long out = count("SELECT COUNT(*) FROM `geräte` WHERE COALESCE(`AUSSERBETRIEB`,0) <> 0");
    return new InventoryStats(legacy, modern, assignments, medical, electrical, out);
  }

  private List<InventoryDevice> searchLegacy(InventorySearchCriteria c) {
    int limit = normalizeLimit(c.limit());
    int offset = Math.max(c.offset(), 0);
    MapSqlParameterSource p = new MapSqlParameterSource().addValue("limit", limit).addValue("offset", offset);
    StringBuilder sql = new StringBuilder("""
      SELECT g.`GERÄTE_ID`, g.`GeräteName`, g.`GeräteTyp`, g.`Seriennummer`, g.`Inventarnummer`, g.`Hersteller`,
             g.`INNERBETRIEBLICHER_STANDORT`, g.`Filiale`, f.`FILIALEKUERZEL`, f.`FILIALENAME`,
             g.`MEDGERÄTE`, g.`ELEKGERÄTE`, g.`INVENTAR`, g.`AUSSERBETRIEB`, g.`IMEINSATZ`,
             g.`Anschaffungsdatum`, g.`BEMERKUNG`
      FROM `geräte` g
      LEFT JOIN `filiale` f ON f.`FILIALE_ID` = g.`Filiale`
      WHERE 1=1
      """);
    addCommonFilters(sql, p, c, "g.`GeräteName`", "g.`GeräteTyp`", "g.`Seriennummer`", "g.`Inventarnummer`", "g.`Hersteller`", "g.`INNERBETRIEBLICHER_STANDORT`", "f.`FILIALENAME`", "g.`Filiale`", "g.`AUSSERBETRIEB`");
    sql.append(" ORDER BY g.`GERÄTE_ID` DESC LIMIT :limit OFFSET :offset");
    return named.query(sql.toString(), p, (rs, row) -> mapLegacy(rs));
  }

  private List<InventoryDevice> searchNew(InventorySearchCriteria c) {
    int limit = normalizeLimit(c.limit());
    int offset = Math.max(c.offset(), 0);
    MapSqlParameterSource p = new MapSqlParameterSource().addValue("limit", limit).addValue("offset", offset);
    StringBuilder sql = new StringBuilder("""
      SELECT gn.`ID`, gn.`Name`, gn.`Typ`, gn.`Seriennummer`, gn.`IP`, gn.`Standort`,
             fg.`rfiliale_ID`, f.`FILIALEKUERZEL`, f.`FILIALENAME`
      FROM `geräte_neu` gn
      LEFT JOIN `filiale_geräte_neu` fg ON fg.`geräte_neu_id` = gn.`ID`
      LEFT JOIN `filiale` f ON f.`FILIALE_ID` = fg.`rfiliale_ID`
      WHERE 1=1
      """);
    if (c.q() != null && !c.q().isBlank()) {
      sql.append(" AND (gn.`Name` LIKE :q OR gn.`Typ` LIKE :q OR gn.`Seriennummer` LIKE :q OR gn.`IP` LIKE :q OR gn.`Standort` LIKE :q OR f.`FILIALENAME` LIKE :q) ");
      p.addValue("q", "%" + c.q().trim() + "%");
    }
    if (c.branchId() != null) { sql.append(" AND fg.`rfiliale_ID` = :branchId "); p.addValue("branchId", c.branchId()); }
    sql.append(" GROUP BY gn.`ID` ORDER BY gn.`ID` DESC LIMIT :limit OFFSET :offset");
    return named.query(sql.toString(), p, (rs, row) -> mapNew(rs));
  }

  private void addCommonFilters(StringBuilder sql, MapSqlParameterSource p, InventorySearchCriteria c,
                                String name, String type, String serial, String inventoryNo, String manufacturer, String location, String branchName, String branchIdCol, String outOfServiceCol) {
    if (c.q() != null && !c.q().isBlank()) {
      sql.append(" AND (").append(name).append(" LIKE :q OR ").append(type).append(" LIKE :q OR ").append(serial).append(" LIKE :q OR ")
        .append(inventoryNo).append(" LIKE :q OR ").append(manufacturer).append(" LIKE :q OR ").append(location).append(" LIKE :q OR ").append(branchName).append(" LIKE :q) ");
      p.addValue("q", "%" + c.q().trim() + "%");
    }
    if (c.branchId() != null) { sql.append(" AND ").append(branchIdCol).append(" = :branchId "); p.addValue("branchId", c.branchId()); }
    if (Boolean.TRUE.equals(c.activeOnly())) { sql.append(" AND COALESCE(").append(outOfServiceCol).append(",0) = 0 "); }
  }

  private InventoryDevice findLegacy(Integer id) {
    return jdbc.queryForObject("""
      SELECT g.`GERÄTE_ID`, g.`GeräteName`, g.`GeräteTyp`, g.`Seriennummer`, g.`Inventarnummer`, g.`Hersteller`,
             g.`INNERBETRIEBLICHER_STANDORT`, g.`Filiale`, f.`FILIALEKUERZEL`, f.`FILIALENAME`,
             g.`MEDGERÄTE`, g.`ELEKGERÄTE`, g.`INVENTAR`, g.`AUSSERBETRIEB`, g.`IMEINSATZ`,
             g.`Anschaffungsdatum`, g.`BEMERKUNG`
      FROM `geräte` g LEFT JOIN `filiale` f ON f.`FILIALE_ID` = g.`Filiale`
      WHERE g.`GERÄTE_ID` = ?
      """, (rs, row) -> mapLegacy(rs), id);
  }

  private InventoryDevice findNew(Integer id) {
    return jdbc.queryForObject("""
      SELECT gn.`ID`, gn.`Name`, gn.`Typ`, gn.`Seriennummer`, gn.`IP`, gn.`Standort`,
             fg.`rfiliale_ID`, f.`FILIALEKUERZEL`, f.`FILIALENAME`
      FROM `geräte_neu` gn
      LEFT JOIN `filiale_geräte_neu` fg ON fg.`geräte_neu_id` = gn.`ID`
      LEFT JOIN `filiale` f ON f.`FILIALE_ID` = fg.`rfiliale_ID`
      WHERE gn.`ID` = ?
      GROUP BY gn.`ID`
      """, (rs, row) -> mapNew(rs), id);
  }

  private List<DeviceAssignment> assignmentsForNewDevice(String source, Integer id) {
    if (!("new".equals(source) || "geraete_neu".equals(source) || "geräte_neu".equals(source))) return List.of();
    return jdbc.query("""
      SELECT fg.`ID`, fg.`rfiliale_ID`, f.`FILIALEKUERZEL`, f.`FILIALENAME`, fg.`rgesellschafts_ID`, rg.`gesellschaftsname`
      FROM `filiale_geräte_neu` fg
      LEFT JOIN `filiale` f ON f.`FILIALE_ID` = fg.`rfiliale_ID`
      LEFT JOIN `rechnungsgesellschaft` rg ON rg.`id` = fg.`rgesellschafts_ID`
      WHERE fg.`geräte_neu_id` = ?
      ORDER BY fg.`ID` ASC
      """, (rs, row) -> new DeviceAssignment(getInt(rs,"ID"), getInt(rs,"rfiliale_ID"), rs.getString("FILIALEKUERZEL"), rs.getString("FILIALENAME"), getInt(rs,"rgesellschafts_ID"), rs.getString("gesellschaftsname")), id);
  }

  private List<DeviceConsumable> consumablesForNewDevice(String source, Integer id) {
    if (!("new".equals(source) || "geraete_neu".equals(source) || "geräte_neu".equals(source))) return List.of();
    return jdbc.query("""
      SELECT gv.`ID`, gv.`VMID`, v.`Name`, v.`Eigenschaften`, v.`Anzahl`, v.`EMail_Hersteller`
      FROM `geräte_neu_vmaterial` gv
      LEFT JOIN `verbrauchsmaterial` v ON v.`ID` = gv.`VMID`
      WHERE gv.`rgeräte_neu_id` = ?
      ORDER BY v.`Name` ASC
      """, (rs, row) -> new DeviceConsumable(getInt(rs,"ID"), getInt(rs,"VMID"), rs.getString("Name"), rs.getString("Eigenschaften"), getInt(rs,"Anzahl"), rs.getString("EMail_Hersteller")), id);
  }

  private List<DeviceSoftware> softwareForDevice(String source, Integer id) {
    // Im alten Datenmodell hängt Software primär an Arbeitsplätzen. Die direkte Geräteverknüpfung wird später nachgezogen.
    return List.of();
  }

  private InventoryDevice mapLegacy(ResultSet rs) throws SQLException {
    Boolean out = getBool(rs, "AUSSERBETRIEB");
    return new InventoryDevice(
      getInt(rs,"GERÄTE_ID"), "legacy", rs.getString("GeräteName"), rs.getString("GeräteTyp"), rs.getString("Seriennummer"),
      rs.getString("Inventarnummer"), rs.getString("Hersteller"), null, rs.getString("INNERBETRIEBLICHER_STANDORT"),
      getInt(rs,"Filiale"), rs.getString("FILIALEKUERZEL"), rs.getString("FILIALENAME"), getBool(rs,"MEDGERÄTE"), getBool(rs,"ELEKGERÄTE"),
      getBool(rs,"INVENTAR"), out == null ? null : !out, getBool(rs,"IMEINSATZ"), rs.getString("Anschaffungsdatum"), rs.getString("BEMERKUNG"));
  }

  private InventoryDevice mapNew(ResultSet rs) throws SQLException {
    return new InventoryDevice(getInt(rs,"ID"), "new", rs.getString("Name"), rs.getString("Typ"), rs.getString("Seriennummer"), null,
      null, rs.getString("IP"), rs.getString("Standort"), getInt(rs,"rfiliale_ID"), rs.getString("FILIALEKUERZEL"), rs.getString("FILIALENAME"),
      null, null, null, true, null, null, null);
  }

  private long count(String sql) {
    Long c = jdbc.queryForObject(sql, Long.class);
    return c == null ? 0L : c;
  }

  private int normalizeLimit(int requested) { return Math.min(Math.max(requested <= 0 ? 100 : requested, 1), 500); }
  private static Integer getInt(ResultSet rs, String col) throws SQLException { int v = rs.getInt(col); return rs.wasNull() ? null : v; }
  private static Boolean getBool(ResultSet rs, String col) throws SQLException { boolean v = rs.getBoolean(col); return rs.wasNull() ? null : v; }
}
