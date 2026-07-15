package de.kopfzentrum.gam.masterdata;

import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/gam/admin/masterdata")
@PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN','ADMINISTRATOR')")
public class MasterDataAdminController {
  private final JdbcTemplate jdbc;
  private final NamedParameterJdbcTemplate named;
  private final Map<String, MasterDataCatalog> catalogs;

  public MasterDataAdminController(JdbcTemplate jdbc, NamedParameterJdbcTemplate named) {
    this.jdbc = jdbc;
    this.named = named;
    this.catalogs = buildCatalogs();
    ensureInvoiceTextAssignmentTable();
    ensureInvoiceTextFallbackRows();
    ensureInvoiceLogoTable();
    ensureInvoiceCompanyLogoColumn();
    ensureMarketingMasterDataTables();
  }

  @GetMapping("/catalogs")
  public Collection<MasterDataCatalog> catalogs() {
    return catalogs.values();
  }

  @GetMapping("/{key}")
  public MasterDataRows rows(@PathVariable String key,
                             @RequestParam(defaultValue = "") String q,
                             @RequestParam(defaultValue = "150") int limit) {
    MasterDataCatalog c = catalog(key);
    if (isInvoiceTextFallbackCatalog(c)) ensureInvoiceTextFallbackRows();
    if (isInvoiceLogoCatalog(c)) ensureInvoiceLogoTable();
    int safeLimit = Math.min(Math.max(limit, 1), 500);
    MapSqlParameterSource params = new MapSqlParameterSource().addValue("limit", safeLimit);
    StringBuilder sql = new StringBuilder("SELECT ")
      .append(columnList(c.primaryKey(), c.fields()))
      .append(" FROM ").append(table(c.tableName()))
      .append(" WHERE 1=1 ");
    if (q != null && !q.isBlank() && !c.searchFields().isEmpty()) {
      params.addValue("q", "%" + q.trim() + "%");
      sql.append(" AND (");
      for (int i = 0; i < c.searchFields().size(); i++) {
        if (i > 0) sql.append(" OR ");
        sql.append("CAST(").append(col(c.searchFields().get(i))).append(" AS CHAR) LIKE :q");
      }
      sql.append(") ");
    }
    sql.append(" ORDER BY ").append(col(c.primaryKey())).append(isInvoiceTextFallbackCatalog(c) ? " ASC" : " DESC").append(" LIMIT :limit");
    return new MasterDataRows(c, named.query(sql.toString(), params, new ColumnMapRowMapper()));
  }

  @PostMapping("/{key}")
  public Map<String, Object> create(@PathVariable String key, @RequestBody Map<String, Object> payload) {
    MasterDataCatalog c = catalog(key);
    Map<String, Object> values = writableValues(c, payload);
    if (values.isEmpty()) throw new IllegalArgumentException("Keine speicherbaren Felder übergeben.");
    String cols = values.keySet().stream().map(this::col).collect(Collectors.joining(", "));
    String params = values.keySet().stream().map(k -> ":" + paramName(k)).collect(Collectors.joining(", "));
    MapSqlParameterSource p = toParams(values);
    named.update("INSERT INTO " + table(c.tableName()) + " (" + cols + ") VALUES (" + params + ")", p);
    Integer id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
    return find(c, id);
  }

  @PutMapping("/{key}/{id}")
  public Map<String, Object> update(@PathVariable String key, @PathVariable String id, @RequestBody Map<String, Object> payload) {
    MasterDataCatalog c = catalog(key);
    if (isProtectedInvoiceTextFallback(c, id)) {
      throw new IllegalArgumentException("Systemstandard ID 0 darf nicht geändert werden.");
    }
    if (isProtectedInvoiceLogoFallback(c, id)) {
      throw new IllegalArgumentException("Systemlogo ID 0 darf nicht geändert werden.");
    }
    Map<String, Object> values = writableValues(c, payload);
    if (values.isEmpty()) return find(c, id);
    String set = values.keySet().stream().map(k -> col(k) + " = :" + paramName(k)).collect(Collectors.joining(", "));
    MapSqlParameterSource p = toParams(values).addValue("id", id);
    named.update("UPDATE " + table(c.tableName()) + " SET " + set + " WHERE " + col(c.primaryKey()) + " = :id", p);
    return find(c, id);
  }

  // Schritt 39k: Die alte 39d-Rechnungstext-Set-Oberfläche bleibt bewusst entfernt.
  // Fachlich richtig sind vier getrennte Textbaustein-Kataloge plus Textzuordnung je Gesellschaft.

  @PostMapping("/invoice-logos/upload")
  public Map<String, Object> uploadInvoiceLogo(@RequestParam("file") MultipartFile file) throws IOException {
    if (file == null || file.isEmpty()) throw new IllegalArgumentException("Keine Logo-Datei übergeben.");
    String original = Optional.ofNullable(file.getOriginalFilename()).orElse("logo");
    String lower = original.toLowerCase(Locale.ROOT);
    if (!(lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".gif") || lower.endsWith(".webp"))) {
      throw new IllegalArgumentException("Bitte eine Bilddatei hochladen: PNG, JPG, GIF oder WEBP.");
    }
    String ext = lower.contains(".") ? lower.substring(lower.lastIndexOf('.')) : ".png";
    String safe = original.replaceAll("[^A-Za-z0-9._-]", "_");
    String name = System.currentTimeMillis() + "_" + safe;
    if (!name.toLowerCase(Locale.ROOT).endsWith(ext)) name += ext;
    byte[] bytes = file.getBytes();
    List<Path> targets = new ArrayList<>();
    targets.add(Path.of("src/main/resources/static/images/uploads", name));
    targets.add(Path.of("backend/src/main/resources/static/images/uploads", name));
    targets.add(Path.of("frontend/public/images/uploads", name));
    targets.add(Path.of("../frontend/public/images/uploads", name));
    Path primary = null;
    for (Path target : targets) {
      try {
        Path normalized = target.normalize();
        Files.createDirectories(normalized.getParent());
        Files.write(normalized, bytes);
        if (primary == null) primary = normalized;
      } catch (Exception ignored) { }
    }
    if (primary == null) throw new IOException("Logo konnte nicht gespeichert werden.");
    String url = "/images/uploads/" + name;
    return Map.of("url", url, "filename", name, "previewUrl", url);
  }


  @DeleteMapping("/{key}/{id}")
  public Map<String, Object> delete(@PathVariable String key, @PathVariable String id) {
    MasterDataCatalog c = catalog(key);
    if (isProtectedInvoiceTextFallback(c, id)) {
      throw new IllegalArgumentException("Systemstandard ID 0 darf nicht gelöscht werden.");
    }
    if (isProtectedInvoiceLogoFallback(c, id)) {
      throw new IllegalArgumentException("Systemlogo ID 0 darf nicht gelöscht werden.");
    }
    if (isInvoiceLogoCatalog(c)) {
      Integer used = jdbc.queryForObject("SELECT COUNT(*) FROM `rechnungsgesellschaft` WHERE `LOGO_ID` = ?", Integer.class, id);
      if (used != null && used > 0) {
        throw new IllegalArgumentException("Dieses Logo wird noch von " + used + " Gesellschaft(en) verwendet und kann nicht gelöscht werden.");
      }
    }
    Map<String, Object> existing = find(c, id);
    named.update("DELETE FROM " + table(c.tableName()) + " WHERE " + col(c.primaryKey()) + " = :id", new MapSqlParameterSource().addValue("id", id));
    existing.put("deleted", true);
    return existing;
  }

  private Map<String, Object> find(MasterDataCatalog c, Object id) {
    return jdbc.queryForObject("SELECT " + columnList(c.primaryKey(), c.fields()) + " FROM " + table(c.tableName()) + " WHERE " + col(c.primaryKey()) + " = ?", new ColumnMapRowMapper(), id);
  }

  private MasterDataCatalog catalog(String key) {
    MasterDataCatalog c = catalogs.get(key);
    if (c == null) throw new IllegalArgumentException("Unbekannter Adminbereich: " + key);
    return c;
  }

  private Map<String, Object> writableValues(MasterDataCatalog c, Map<String, Object> payload) {
    Map<String, Object> values = new LinkedHashMap<>();
    if (payload.containsKey(c.primaryKey()) && isManualPrimaryKey(c)) {
      Object pk = payload.get(c.primaryKey());
      if (pk instanceof String s && s.isBlank()) pk = null;
      if (pk != null) values.put(c.primaryKey(), pk);
    }
    for (String f : c.fields()) {
      if (!payload.containsKey(f)) continue;
      Object v = payload.get(f);
      if (v instanceof String s && s.isBlank()) v = null;
      values.put(f, v);
    }
    return values;
  }

  private boolean isManualPrimaryKey(MasterDataCatalog c) {
    return Set.of("lager", "kassenbuchoben").contains(c.tableName())
      || Set.of("CODE", "MANDANTENNUMMER").contains(c.primaryKey());
  }

  private MapSqlParameterSource toParams(Map<String, Object> values) {
    MapSqlParameterSource p = new MapSqlParameterSource();
    values.forEach((k, v) -> p.addValue(paramName(k), v));
    return p;
  }

  private String columnList(String pk, List<String> fields) {
    List<String> all = new ArrayList<>();
    all.add(pk);
    all.addAll(fields);
    return all.stream().distinct().map(this::col).collect(Collectors.joining(", "));
  }

  private String table(String name) { return "`" + name.replace("`", "") + "`"; }
  private String col(String name) { return "`" + name.replace("`", "") + "`"; }
  private String paramName(String name) { return name.replaceAll("[^A-Za-z0-9_]", "_"); }

  private void ensureInvoiceTextCompanyColumns() {
    ensureInvoiceTextAssignmentTable();
  }

  private void ensureInvoiceTextAssignmentTable() {
    try {
      jdbc.execute("CREATE TABLE IF NOT EXISTS `rechnungstext_gesellschaft_zuordnung` (" +
        "`ID` int NOT NULL AUTO_INCREMENT," +
        "`RGESELLSCHAFTS_ID` int(50) NOT NULL," +
        "`ANREDE_ID` int(50) DEFAULT NULL," +
        "`RECHNUNGSTEXT_ID` int(50) DEFAULT NULL," +
        "`RECHTLICHER_HINWEIS_ID` int(50) DEFAULT NULL," +
        "`GRUSSFORMEL_ID` int(50) DEFAULT NULL," +
        "PRIMARY KEY (`ID`)," +
        "UNIQUE KEY `uk_rechnungstext_gesellschaft` (`RGESELLSCHAFTS_ID`)" +
        ")");
    } catch (Exception ignored) { }
  }

  private void ensureInvoiceTextFallbackRows() {
    try {
      jdbc.execute("CREATE TABLE IF NOT EXISTS `rechnungsanrede` (`ID` int NOT NULL AUTO_INCREMENT, `TEXT` text, PRIMARY KEY (`ID`))");
      jdbc.execute("CREATE TABLE IF NOT EXISTS `rechnungstext` (`ID` int NOT NULL AUTO_INCREMENT, `TEXT` text, PRIMARY KEY (`ID`))");
      jdbc.execute("CREATE TABLE IF NOT EXISTS `rechnungsrechtlicherhinweis` (`ID` int NOT NULL AUTO_INCREMENT, `TEXT` text, PRIMARY KEY (`ID`))");
      jdbc.execute("CREATE TABLE IF NOT EXISTS `rechnungsgrussformel` (`ID` int NOT NULL AUTO_INCREMENT, `TEXT` text, PRIMARY KEY (`ID`))");
      // Schritt 39i: ID 0 ist fachlicher Systemstandard.
      // MariaDB/MySQL benötigen NO_AUTO_VALUE_ON_ZERO, damit 0 nicht als neuer Auto-Increment-Wert interpretiert wird.
      jdbc.execute("SET SESSION sql_mode = CONCAT_WS(',', @@sql_mode, 'NO_AUTO_VALUE_ON_ZERO')");
      jdbc.update("INSERT IGNORE INTO `rechnungsanrede` (`ID`, `TEXT`) VALUES (0, ?)", "Sehr geehrte Damen und Herren,");
      jdbc.update("INSERT IGNORE INTO `rechnungstext` (`ID`, `TEXT`) VALUES (0, ?)", "Wir erlauben uns folgende Leistungen in Rechnung zu stellen.");
      jdbc.update("INSERT IGNORE INTO `rechnungsrechtlicherhinweis` (`ID`, `TEXT`) VALUES (0, ?)", "Bitte begleichen Sie den Rechnungsbetrag innerhalb der angegebenen Frist.");
      jdbc.update("INSERT IGNORE INTO `rechnungsgrussformel` (`ID`, `TEXT`) VALUES (0, ?)", "Mit freundlichen Grüßen");
    } catch (Exception ignored) { }
  }

  private boolean isInvoiceTextFallbackCatalog(MasterDataCatalog c) {
    return Set.of("rechnungsanrede", "rechnungstext", "rechnungsrechtlicherhinweis", "rechnungsgrussformel").contains(c.tableName());
  }

  private boolean isProtectedInvoiceTextFallback(MasterDataCatalog c, String id) {
    return isInvoiceTextFallbackCatalog(c) && "0".equals(String.valueOf(id));
  }

  private boolean isInvoiceLogoCatalog(MasterDataCatalog c) {
    return "rechnungslogo".equals(c.tableName());
  }

  private boolean isProtectedInvoiceLogoFallback(MasterDataCatalog c, String id) {
    return isInvoiceLogoCatalog(c) && "0".equals(String.valueOf(id));
  }

  private void ensureInvoiceLogoTable() {
    try {
      jdbc.execute("CREATE TABLE IF NOT EXISTS `rechnungslogo` (`ID` int NOT NULL AUTO_INCREMENT, `NAME` varchar(255) DEFAULT NULL, `URL` varchar(1024) DEFAULT NULL, PRIMARY KEY (`ID`))");
      if (!columnExists("rechnungslogo", "NAME")) {
        jdbc.execute("ALTER TABLE `rechnungslogo` ADD COLUMN `NAME` varchar(255) DEFAULT NULL");
      }
      jdbc.execute("SET SESSION sql_mode = CONCAT_WS(',', @@sql_mode, 'NO_AUTO_VALUE_ON_ZERO')");
      jdbc.update("INSERT IGNORE INTO `rechnungslogo` (`ID`, `NAME`, `URL`) VALUES (0, ?, ?)", "Systemstandard", "KOPFZENTRUM_LOGO.png");
      try { jdbc.update("UPDATE `rechnungslogo` SET `NAME` = COALESCE(NULLIF(`NAME`, ''), 'Systemstandard') WHERE `ID` = 0"); } catch (Exception ignored) { }
    } catch (Exception ignored) { }
  }

  private void ensureInvoiceCompanyLogoColumn() {
    try {
      if (!columnExists("rechnungsgesellschaft", "LOGO_ID")) {
        jdbc.execute("ALTER TABLE `rechnungsgesellschaft` ADD COLUMN `LOGO_ID` int(50) DEFAULT NULL");
      }
    } catch (Exception ignored) { }
  }


  private void ensureMarketingMasterDataTables() {
    jdbc.execute("""
      CREATE TABLE IF NOT EXISTS gam_warehouse_type (
        id BIGINT NOT NULL AUTO_INCREMENT, name VARCHAR(255) NOT NULL, description TEXT NULL,
        active BOOLEAN NOT NULL DEFAULT TRUE, sort_order INT NOT NULL DEFAULT 100, PRIMARY KEY (id), UNIQUE KEY uk_warehouse_type_name (name)
      )
      """);
    jdbc.execute("""
      CREATE TABLE IF NOT EXISTS gam_warehouse_location (
        id BIGINT NOT NULL AUTO_INCREMENT, name VARCHAR(255) NOT NULL, code VARCHAR(80) NULL, warehouse_type_id BIGINT NULL,
        branch_id BIGINT NULL, address VARCHAR(500) NULL, active BOOLEAN NOT NULL DEFAULT TRUE, note TEXT NULL, PRIMARY KEY (id), UNIQUE KEY uk_warehouse_location_name (name)
      )
      """);
    jdbc.execute("""
      CREATE TABLE IF NOT EXISTS gam_marketing_action_type (
        id BIGINT NOT NULL AUTO_INCREMENT, name VARCHAR(255) NOT NULL, description TEXT NULL,
        active BOOLEAN NOT NULL DEFAULT TRUE, sort_order INT NOT NULL DEFAULT 100, PRIMARY KEY (id), UNIQUE KEY uk_marketing_action_type_name (name)
      )
      """);
    jdbc.execute("""
      CREATE TABLE IF NOT EXISTS gam_marketing_material_type (
        id BIGINT NOT NULL AUTO_INCREMENT, name VARCHAR(255) NOT NULL, description TEXT NULL,
        active BOOLEAN NOT NULL DEFAULT TRUE, sort_order INT NOT NULL DEFAULT 100, PRIMARY KEY (id), UNIQUE KEY uk_marketing_material_type_name (name)
      )
      """);
    jdbc.update("INSERT IGNORE INTO gam_warehouse_type (name, description, sort_order) VALUES ('Hauptlager', 'Zentrales Hauptlager', 10)");
    jdbc.update("INSERT IGNORE INTO gam_warehouse_location (name, code, warehouse_type_id, active) SELECT 'Hauptlager', 'HL', id, TRUE FROM gam_warehouse_type WHERE name = 'Hauptlager' LIMIT 1");
    jdbc.update("INSERT IGNORE INTO gam_marketing_action_type (name, description, sort_order) VALUES ('Materialverteilung', 'Marketingmaterial an Filialen verteilen', 10)");
    jdbc.update("INSERT IGNORE INTO gam_marketing_action_type (name, description, sort_order) VALUES ('Kampagne', 'Zeitlich begrenzte Marketingkampagne', 20)");
    jdbc.update("INSERT IGNORE INTO gam_marketing_material_type (name, description, sort_order) VALUES ('Flyer', 'Flyer und Faltblätter', 10)");
    jdbc.update("INSERT IGNORE INTO gam_marketing_material_type (name, description, sort_order) VALUES ('Plakat', 'Plakate und Aushänge', 20)");
    jdbc.update("INSERT IGNORE INTO gam_marketing_material_type (name, description, sort_order) VALUES ('Werbeartikel', 'Sonstige Werbe- und Streuartikel', 30)");
  }

  private boolean columnExists(String tableName, String columnName) {
    try {
      Integer count = jdbc.queryForObject("""
        SELECT COUNT(*)
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND UPPER(TABLE_NAME) = UPPER(?)
          AND UPPER(COLUMN_NAME) = UPPER(?)
        """, Integer.class, tableName, columnName);
      return count != null && count > 0;
    } catch (Exception ignored) {
      return false;
    }
  }

  private Object normalizeBlank(Object v) {
    if (v instanceof String s && s.isBlank()) return null;
    return v;
  }

  private static Map<String, MasterDataCatalog> buildCatalogs() {
    List<MasterDataCatalog> list = List.of(
      new MasterDataCatalog("warehouse-types", "Lagerarten", "Marketing & Logistik", "gam_warehouse_type", "id",
        List.of("name", "description", "active", "sort_order"), List.of("name", "description"), "Zentrale Lagerarten für Marketing- und Lagerworkflows."),
      new MasterDataCatalog("warehouse-locations", "Lager", "Marketing & Logistik", "gam_warehouse_location", "id",
        List.of("name", "code", "warehouse_type_id", "branch_id", "address", "active", "note"), List.of("name", "code", "address"), "Zentrale Lagerstammdaten. Im Marketingworkflow ist nur eine Auswahl möglich."),
      new MasterDataCatalog("marketing-action-types", "Marketing-Aktionsarten", "Marketing & Logistik", "gam_marketing_action_type", "id",
        List.of("name", "description", "active", "sort_order"), List.of("name", "description"), "Zulässige Aktionsarten für den Marketingworkflow."),
      new MasterDataCatalog("marketing-material-types", "Marketing-Materialarten", "Marketing & Logistik", "gam_marketing_material_type", "id",
        List.of("name", "description", "active", "sort_order"), List.of("name", "description"), "Zulässige Materialarten für den Marketingworkflow."),
      new MasterDataCatalog("branches", "Filialen / Standorte", "Stammdaten", "filiale", "FILIALE_ID",
        List.of("FILIALEKUERZEL", "FILIALENAME", "STRASSE", "PLZ", "ORT", "EMAIL", "RELEVANT", "KATEGORIE", "KOSTENSTELLE", "IMPORTORDNER", "EXPORTORDNER"),
        List.of("FILIALEKUERZEL", "FILIALENAME", "ORT", "KATEGORIE"), "Alt-GAM Filial- und Standortverwaltung ohne Rechnungsdetails."),
      new MasterDataCatalog("inventory-devices", "Geräte neu", "Inventar", "geräte_neu", "ID",
        List.of("Name", "Typ", "Seriennummer", "IP", "Standort"),
        List.of("Name", "Typ", "Seriennummer", "IP", "Standort"), "Geräteverwaltung aus GAM 1.0 für die moderne geräte_neu-Struktur."),
      new MasterDataCatalog("workplaces", "Arbeitsplätze", "Arbeitsplatzausstattung", "arbeitsplatz", "ID",
        List.of("FILIALE_ID", "ARBEITSPLATZ", "TELEFON", "DATUM_EINRICHTUNG", "DATUM_ANTRAGSTELLUNG", "DATUM_ANTRAGGENEHMIGT", "MITARBEITER", "FERTIGGESTELLT"),
        List.of("ARBEITSPLATZ", "MITARBEITER"), "Arbeitsplätze, Einrichtung und Arbeitsplatzstatus."),
      new MasterDataCatalog("personnel", "Personal", "Personaldaten", "personal", "ID",
        List.of("NAME", "VORNAME", "STATUS", "POSITION", "FILIALE_ID", "EMAIL", "TELEFON", "TELEFON2", "BEMERKUNG_PERSONAL",
          "EMAIL_PW_EXTERN", "EMAIL_PW_INTERN", "RECHNER_IP", "OFFICE_LIZENZ",
          "PLONE_BENUTZER", "PLONE_PW", "MICROSOFT_KONTO", "MICROSOFT_PW",
          "DIENSTHANDY", "VPN_TOKEN", "VPN_PIN", "QNAP_BENUTZER", "QNAP_PW",
          "DIENSTLAPTOP", "BEMERKUNG"),
        List.of("NAME", "VORNAME", "STATUS", "POSITION", "EMAIL", "RECHNER_IP", "PLONE_BENUTZER", "MICROSOFT_KONTO", "QNAP_BENUTZER", "BEMERKUNG_PERSONAL", "BEMERKUNG"), "Personalstammdaten mit vollständiger HR-/IT-Rollensicht."),
      new MasterDataCatalog("warehouse", "Lagerartikel", "Lager", "lager", "CODE",
        List.of("BESCHREIBUNG", "LAGERORT", "MENGE"),
        List.of("BESCHREIBUNG", "LAGERORT"), "Klassische Lagerartikel aus Alt-GAM."),
      new MasterDataCatalog("consumables", "Verbrauchsmaterial", "Lager", "verbrauchsmaterial", "ID",
        List.of("Name", "Eigenschaften", "Anzahl", "EMail_Hersteller"),
        List.of("Name", "Eigenschaften", "EMail_Hersteller"), "Verbrauchsmaterial inklusive Bestand und Herstellerkontakt."),
      new MasterDataCatalog("tasks", "Aufgaben", "Aufgabenverwaltung", "aufgaben", "ID",
        List.of("USERNAME", "TAGESDATUM", "KÜRZEL", "FILIALE_ID", "FACHBEREICH", "AUFGABE", "VERANTWORTLICHER", "PRIORITÄT", "STATUS", "FRIST", "ERLEDIGT", "BEMERKUNG"),
        List.of("USERNAME", "KÜRZEL", "FACHBEREICH", "AUFGABE", "VERANTWORTLICHER", "STATUS"), "Aufgabenverwaltung aus Alt-GAM."),
      new MasterDataCatalog("approvals", "Freigaben", "Freigabemanagement", "freigabe", "ID",
        List.of("DATUM", "EINTRAGENDER", "BESCHREIBUNG", "GESELLSCHAFT_ID", "FILIALE_ID", "STATUS", "BEMERKUNG"),
        List.of("EINTRAGENDER", "BESCHREIBUNG", "STATUS", "BEMERKUNG"), "Freigabe- und Entscheidungslisten."),
      new MasterDataCatalog("cashbook", "Kassenbuch", "Kassenbuch", "kassenbuch", "ID",
        List.of("DATUM", "GESCHÄFTSVORGANG", "STEUER", "EINNAHMEN", "AUSGABEN", "BESTAND", "GEGENKONTO", "MANDANTENNUMMER"),
        List.of("GESCHÄFTSVORGANG", "GEGENKONTO"), "Kassenbuch-Stammdatenerfassung außerhalb des Rechnungsmoduls."),
      new MasterDataCatalog("pricelist", "Preisliste", "Preisliste", "preisliste", "ID",
        List.of("ARTIKEL", "LIEFERANT", "LETZTER_EINKAUFSPREIS", "MONATLICHE_KOSTEN", "BEMERKUNG"),
        List.of("ARTIKEL", "LIEFERANT", "BEMERKUNG"), "Historische Preisliste und Lieferantenpreise."),
      new MasterDataCatalog("software-catalog", "Softwareauswahl", "Arbeitsplatzausstattung", "softwareauswahl", "ID",
        List.of("SOFTWARE"),
        List.of("SOFTWARE"), "Softwarekatalog für Arbeitsplätze."),
      new MasterDataCatalog("applications", "Anwendungen", "Benutzer/Rechte", "application", "ID",
        List.of("APPLICATION", "FG_SELECT"),
        List.of("APPLICATION", "FG_SELECT"), "Historischer Anwendungskatalog aus GAM 1.0."),
      new MasterDataCatalog("user-applications", "Benutzer-Anwendungen", "Benutzer/Rechte", "userapplication", "ID",
        List.of("USERNAME", "APPLICATION", "RGESELLSCHAFTS_ID", "ROLE"),
        List.of("USERNAME", "APPLICATION", "ROLE"), "Zuordnung von Benutzern zu Anwendungen, Rollen und Gesellschaften."),
      new MasterDataCatalog("menu-tree", "Menübaum", "Benutzer/Rechte", "thetree", "ID",
        List.of("NODE_NAME", "PARENT_ID", "APPLICATION_ID", "RGESELLSCHAFTS_ID"),
        List.of("NODE_NAME"), "Historische Menü-/Rechtebaumstruktur ohne MOH-Auswertung."),
      new MasterDataCatalog("themes", "Oberflächen-Themes", "Benutzer/Rechte", "themes", "ID",
        List.of("NAME", "THEMENAME"),
        List.of("NAME", "THEMENAME"), "Historische Theme-Auswahl aus GAM 1.0."),
      new MasterDataCatalog("companies-basic", "Gesellschaften einfach", "Stammdaten", "gesellschaft", "GESELLSCHAFTS_ID",
        List.of("GESELLSCHAFTSNAME"),
        List.of("GESELLSCHAFTSNAME"), "Einfache Gesellschaftsliste außerhalb der späteren Rechnungsadministration."),
      new MasterDataCatalog("old-devices", "Geräte alt", "Inventar", "geräte", "GERÄTE_ID",
        List.of("MEDGERÄTE", "ELEKGERÄTE", "INVENTAR", "GeräteName", "GeräteTyp", "Seriennummer", "Anschaffungsdatum", "Hersteller", "Inventarnummer", "INNERBETRIEBLICHER_STANDORT", "AUSSERBETRIEB", "IMEINSATZ", "Filiale", "BEMERKUNG", "URSPRUNGSFILIALE_ID", "BENANNTESTELLECE", "LIEFERANTENADRESSE"),
        List.of("GeräteName", "GeräteTyp", "Seriennummer", "Inventarnummer", "Hersteller"), "Altes Geräteverzeichnis aus GAM 1.0 zur vollständigen Abdeckung historischer Inventardaten."),
      new MasterDataCatalog("branch-devices", "Filiale-Geräte-Zuordnung", "Inventar", "filiale_geräte_neu", "ID",
        List.of("rfiliale_ID", "rgesellschafts_ID", "geräte_neu_id"),
        List.of("rfiliale_ID", "rgesellschafts_ID", "geräte_neu_id"), "Zuordnung neuer Geräte zu Filialen und Gesellschaften."),
      new MasterDataCatalog("device-consumable-links", "Gerät-Verbrauchsmaterial", "Inventar", "geräte_neu_vmaterial", "ID",
        List.of("GERÄTEID", "VMID"),
        List.of("GERÄTEID", "VMID"), "Historische Zuordnung von Geräten zu Verbrauchsmaterial."),
      new MasterDataCatalog("staff-shortcodes", "Mitarbeiterkürzel", "Personal", "kuerzel", "ID",
        List.of("KNAME", "KVORNAME", "KUERZEL", "ARZT", "MITARBEITER", "THERAPEUT"),
        List.of("KNAME", "KVORNAME", "KUERZEL"), "Kürzelverwaltung für Ärzte, Mitarbeiter und Therapeuten."),
      new MasterDataCatalog("name-accounts", "Namenskonten", "Kassenbuch", "namenskonto", "ID",
        List.of("konto", "nname"),
        List.of("konto", "nname"), "Zuordnung von Namen zu Konten/Gegenkonten."),
      new MasterDataCatalog("cashbook-header", "Kassenbuch-Kopfdaten", "Kassenbuch", "kassenbuchoben", "MANDANTENNUMMER",
        List.of("FIRMA"),
        List.of("MANDANTENNUMMER", "FIRMA"), "Kassenbuch-Kopfdaten je Mandant."),
      new MasterDataCatalog("folder-shares", "Ordnerfreigaben", "Arbeitsplatzausstattung", "ordnerfreigabe", "ID",
        List.of("ORDNERFREIGABE", "ARBEITSPLATZ_ID"),
        List.of("ORDNERFREIGABE", "ARBEITSPLATZ_ID"), "Zugeordnete Ordnerfreigaben je Arbeitsplatz."),
      new MasterDataCatalog("folder-share-catalog", "Ordnerfreigabe-Auswahl", "Arbeitsplatzausstattung", "ordnerfreigabeauswahl", "ID",
        List.of("ORDNERFREIGABE"),
        List.of("ORDNERFREIGABE"), "Katalog möglicher Ordnerfreigaben."),
      new MasterDataCatalog("software-installed", "Installierte Software", "Arbeitsplatzausstattung", "software", "ID",
        List.of("SOFTWARE", "ARBEITSPLATZ_ID"),
        List.of("SOFTWARE", "ARBEITSPLATZ_ID"), "Zugeordnete Software je Arbeitsplatz."),
      new MasterDataCatalog("news", "News / Hinweise", "Administration", "news", "ID",
        List.of("NEWS"),
        List.of("NEWS"), "Historische News- und Hinweisverwaltung."),
      new MasterDataCatalog("device-checks", "Geräteprüfungen", "Prüfungen", "kontrolle", "KONTROLL_ID",
        List.of("AUTORISIERTERPRÜFER", "GERÄTE", "DATUMLETZTEPRÜFUNG_STK", "INTERVALL_STK", "DATUMLETZTEPRÜFUNG_MTK", "INTERVALL_MTK", "DATUMLETZTEPRÜFUNG_BGV_A3", "INTERVALL_BGV_A3", "GEBRAUCHTANWEISUNG", "MEDIZINPRODUKTEBUCH", "INTERVALL", "AKTUELLE_PRÜFPLAKETTE", "WARTUNGSVERTRAG"),
        List.of("AUTORISIERTERPRÜFER", "INTERVALL"), "STK/MTK/BGV-A3 Kontroll- und Prüfdaten."),
      new MasterDataCatalog("commissioning", "Inbetriebnahmen", "Prüfungen", "inbetriebnahme", "INBETRIEBNAHME_ID",
        List.of("DATUMINBETRIEBNAHME", "DATUMFUNKTIONSKONTROLLE", "GERÄTE"),
        List.of("GERÄTE"), "Inbetriebnahme und Funktionskontrolle."),
      new MasterDataCatalog("instructions", "Einweisungen", "Prüfungen", "einweisung", "EINWEISUNGS_ID",
        List.of("DATUMERSTEINWEISUNG", "NAMEERSTEINGEWIESENER", "INBETRIEBNAHME_ID", "DATUMFOLGEEINWEISUNG", "NAMEFOLGEEINGEWIESENER"),
        List.of("NAMEERSTEINGEWIESENER", "NAMEFOLGEEINGEWIESENER"), "Erst- und Folgeeinweisungen."),
      new MasterDataCatalog("patient-addresses", "Patientenadressen", "Patientenakte", "adressen", "ID",
        List.of("PATIENTENNUMMER", "ANREDE", "TITEL", "VORNAME", "NAMENSZUSATZ", "NACHNAME", "STRASSE", "BUNDESLAND", "PLZ", "ORT", "LAND", "GEBDATUM", "VERSICHERTENNUMMER", "VERSICHERTENART"),
        List.of("PATIENTENNUMMER", "VORNAME", "NACHNAME", "PLZ", "ORT", "VERSICHERTENNUMMER"), "Patienten-/Adressstammdaten aus GAM 1.0 als eigenständiger Verwaltungsbereich außerhalb der Rechnungsadministration."),
      new MasterDataCatalog("text-replacements", "Textersetzungen", "Administration", "replacement", "ID",
        List.of("TEXT"),
        List.of("TEXT"), "Historische Textbaustein-/Ersetzungsverwaltung aus GAM 1.0."),
      new MasterDataCatalog("ui-translations-de", "Übersetzungen Deutsch", "Mehrsprachigkeit", "translation_german", "ID",
        List.of("TRANSLATED_TEXT", "TRANSLATE_DESCRIPTION"),
        List.of("TRANSLATED_TEXT", "TRANSLATE_DESCRIPTION"), "GAM-1.0-Übersetzungskatalog Deutsch."),
      new MasterDataCatalog("ui-translations-en", "Übersetzungen Englisch", "Mehrsprachigkeit", "translation_english", "ID",
        List.of("TRANSLATED_TEXT", "TRANSLATE_DESCRIPTION"),
        List.of("TRANSLATED_TEXT", "TRANSLATE_DESCRIPTION"), "GAM-1.0-Übersetzungskatalog Englisch."),
      new MasterDataCatalog("ui-translations-fr", "Übersetzungen Französisch", "Mehrsprachigkeit", "translation_french", "ID",
        List.of("TRANSLATED_TEXT", "TRANSLATE_DESCRIPTION"),
        List.of("TRANSLATED_TEXT", "TRANSLATE_DESCRIPTION"), "GAM-1.0-Übersetzungskatalog Französisch."),
      new MasterDataCatalog("ui-translations-uk", "Übersetzungen Ukrainisch", "Mehrsprachigkeit", "translation_ukrainian", "ID",
        List.of("TRANSLATED_TEXT", "TRANSLATE_DESCRIPTION"),
        List.of("TRANSLATED_TEXT", "TRANSLATE_DESCRIPTION"), "GAM-1.0-Übersetzungskatalog Ukrainisch."),
      new MasterDataCatalog("ui-translations-it", "Übersetzungen Italienisch", "Mehrsprachigkeit", "translation_italian", "ID",
        List.of("TRANSLATED_TEXT", "TRANSLATE_DESCRIPTION"),
        List.of("TRANSLATED_TEXT", "TRANSLATE_DESCRIPTION"), "Übersetzungskatalog Italienisch."),
      new MasterDataCatalog("ui-translations-sv", "Übersetzungen Schwedisch", "Mehrsprachigkeit", "translation_swedish", "ID",
        List.of("TRANSLATED_TEXT", "TRANSLATE_DESCRIPTION"),
        List.of("TRANSLATED_TEXT", "TRANSLATE_DESCRIPTION"), "Übersetzungskatalog Schwedisch."),
      new MasterDataCatalog("ui-translations-tr", "Übersetzungen Türkisch", "Mehrsprachigkeit", "translation_turkish", "ID",
        List.of("TRANSLATED_TEXT", "TRANSLATE_DESCRIPTION"),
        List.of("TRANSLATED_TEXT", "TRANSLATE_DESCRIPTION"), "Übersetzungskatalog Türkisch."),
      new MasterDataCatalog("ui-translations-ru", "Übersetzungen Russisch", "Mehrsprachigkeit", "translation_russian", "ID",
        List.of("TRANSLATED_TEXT", "TRANSLATE_DESCRIPTION"),
        List.of("TRANSLATED_TEXT", "TRANSLATE_DESCRIPTION"), "Übersetzungskatalog Russisch."),
      new MasterDataCatalog("ui-translations-es", "Übersetzungen Spanisch", "Mehrsprachigkeit", "translation_spanish", "ID",
        List.of("TRANSLATED_TEXT", "TRANSLATE_DESCRIPTION"),
        List.of("TRANSLATED_TEXT", "TRANSLATE_DESCRIPTION"), "Übersetzungskatalog Spanisch."),
      new MasterDataCatalog("ui-translations-pt", "Übersetzungen Portugiesisch", "Mehrsprachigkeit", "translation_portuguese", "ID",
        List.of("TRANSLATED_TEXT", "TRANSLATE_DESCRIPTION"),
        List.of("TRANSLATED_TEXT", "TRANSLATE_DESCRIPTION"), "Übersetzungskatalog Portugiesisch."),
      new MasterDataCatalog("ui-translations-nl", "Übersetzungen Niederländisch", "Mehrsprachigkeit", "translation_dutch", "ID",
        List.of("TRANSLATED_TEXT", "TRANSLATE_DESCRIPTION"),
        List.of("TRANSLATED_TEXT", "TRANSLATE_DESCRIPTION"), "Übersetzungskatalog Niederländisch."),
      new MasterDataCatalog("ui-translations-pl", "Übersetzungen Polnisch", "Mehrsprachigkeit", "translation_polish", "ID",
        List.of("TRANSLATED_TEXT", "TRANSLATE_DESCRIPTION"),
        List.of("TRANSLATED_TEXT", "TRANSLATE_DESCRIPTION"), "Übersetzungskatalog Polnisch."),
      new MasterDataCatalog("ui-translations-cs", "Übersetzungen Tschechisch", "Mehrsprachigkeit", "translation_czech", "ID",
        List.of("TRANSLATED_TEXT", "TRANSLATE_DESCRIPTION"),
        List.of("TRANSLATED_TEXT", "TRANSLATE_DESCRIPTION"), "Übersetzungskatalog Tschechisch."),
      new MasterDataCatalog("company-branch-links", "Gesellschaft-Filiale-Zuordnung", "Stammdaten", "rechnungsgesellschaft_filiale", "ID",
        List.of("RGESELLSCHAFTS_ID", "FILIALE_ID"),
        List.of("RGESELLSCHAFTS_ID", "FILIALE_ID"), "Zuordnung von Gesellschaften zu Filialen; Rechnungsdetails bleiben weiter im späteren Rechnungsadmin."),
      new MasterDataCatalog("payment-advice-lines", "Zahlungsavis-Positionen", "Zahlungsavis", "zahlungsavis", "ID",
        List.of("NUMMER", "MENGE", "PRODUKT_ID", "MWST", "PREIS2", "AUFTRAGGEBER", "DURCHFÜHRENDER", "FILIALE_ID", "RGESELLSCHAFTS_ID"),
        List.of("NUMMER", "AUFTRAGGEBER", "DURCHFÜHRENDER"), "Historische Zahlungsavis-Daten als eigener GAM-1.0-Verwaltungsbereich; Rechnungsstammdaten bleiben getrennt."),
      new MasterDataCatalog("invoice-companies", "Rechnungsgesellschaften", "Rechnungsadministration", "rechnungsgesellschaft", "id",
        List.of("gesellschaftskürzel", "gesellschaftsname", "gesellschaftsadresse", "post_straße_nummer", "post_plz_ort", "ustid", "register", "steuernummer", "gerichtsstand", "gesellschaftsführer", "ansprechpartner", "telefon", "fax", "email", "kontoinhaber", "iban", "bic", "neueskonto", "LOGO_ID"),
        List.of("gesellschaftskürzel", "gesellschaftsname", "post_plz_ort", "email", "iban", "LOGO_ID"), "Rechnungsrelevante Gesellschaftsdaten inklusive Bank-, Steuer-, Kontodaten und Logo-Zuordnung."),
      new MasterDataCatalog("invoice-products", "Rechnungsprodukte", "Rechnungsadministration", "rechnungsdaten", "rdaten_id",
        List.of("filiale_id", "code", "beschreibung", "abkürzung", "kategorie", "preis1", "preisneu", "preisalt", "preis_gueltigab", "mwst", "mwstalt", "mwst_gueltigab", "konto", "rgesellschafts_id", "auftraggeber", "durchführender", "gültig_bis", "gültig_ab"),
        List.of("code", "beschreibung", "kategorie", "preis1", "preisneu", "preis_gueltigab", "mwst", "mwst_gueltigab", "gültig_ab", "gültig_bis"), "Stichtagsfähige Rechnungsprodukte: Preis, MwSt und Angebotszeitraum werden über Datumsfelder vorbereitet."),
      new MasterDataCatalog("invoice-salutations", "Rechnungsanreden", "Rechnungsadministration", "rechnungsanrede", "ID",
        List.of("TEXT"), List.of("TEXT"), "Textbausteine für Rechnungsanreden."),
      new MasterDataCatalog("invoice-texts", "Rechnungstexte", "Rechnungsadministration", "rechnungstext", "ID",
        List.of("TEXT"), List.of("TEXT"), "Textbausteine für Rechnungstexte."),
      new MasterDataCatalog("invoice-legal-notes", "Rechtliche Hinweise", "Rechnungsadministration", "rechnungsrechtlicherhinweis", "ID",
        List.of("TEXT"), List.of("TEXT"), "Rechtliche Hinweise und Rechnungshinweise."),
      new MasterDataCatalog("invoice-greetings", "Grußformeln", "Rechnungsadministration", "rechnungsgrussformel", "ID",
        List.of("TEXT"), List.of("TEXT"), "Grußformeln für Rechnungen."),
      new MasterDataCatalog("invoice-text-assignments", "Textzuordnung je Gesellschaft", "Rechnungsadministration", "rechnungstext_gesellschaft_zuordnung", "ID",
        List.of("RGESELLSCHAFTS_ID", "ANREDE_ID", "RECHNUNGSTEXT_ID", "RECHTLICHER_HINWEIS_ID", "GRUSSFORMEL_ID"),
        List.of("RGESELLSCHAFTS_ID", "ANREDE_ID", "RECHNUNGSTEXT_ID", "RECHTLICHER_HINWEIS_ID", "GRUSSFORMEL_ID"),
        "Pro Gesellschaft wird ausgewählt, welche vorhandene Anrede, welcher Rechnungstext, welcher rechtliche Hinweis und welche Grußformel verwendet werden."),
      new MasterDataCatalog("invoice-logos", "Rechnungslogos", "Rechnungsadministration", "rechnungslogo", "ID",
        List.of("NAME", "URL"), List.of("NAME", "URL"), "Logo-Katalog für Rechnungsdokumente. Logos werden einmal angelegt und von Gesellschaften wiederverwendet.")
    );
    return list.stream().collect(Collectors.toMap(MasterDataCatalog::key, c -> c, (a, b) -> a, LinkedHashMap::new));
  }
}
