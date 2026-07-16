package de.kopfzentrum.gam.system;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/setup")
public class FirstRunSetupController {
  @Value("${spring.datasource.url}") private String datasourceUrl;
  @Value("${spring.datasource.username:root}") private String datasourceUser;
  @Value("${spring.datasource.password:}") private String datasourcePassword;

  public record SetupRequest(String mode, String language, String adminUsername, String adminPassword,
                             String adminName, String adminEmail, String practiceName, String country, String timezone) {}

  @GetMapping("/status")
  public Map<String, Object> status() {
    Map<String, Object> result = new LinkedHashMap<>();
    boolean databaseExists = false;
    boolean initialized = false;
    long accounts = 0;
    String error = "";
    try (Connection connection = DriverManager.getConnection(datasourceUrl, datasourceUser, datasourcePassword);
         Statement statement = connection.createStatement()) {
      databaseExists = true;
      try (ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM accounts")) {
        if (rs.next()) accounts = rs.getLong(1);
        initialized = true;
      } catch (Exception ignored) {
        initialized = false;
      }
    } catch (Exception ex) {
      error = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
    }
    result.put("required", !initialized || accounts == 0);
    result.put("databaseExists", databaseExists);
    result.put("initialized", initialized);
    result.put("accountCount", accounts);
    result.put("database", databaseName());
    result.put("emptyDatabaseAvailable", locateSql("gam_v2_1_0_preview2_empty.sql") != null);
    result.put("demoDatabaseAvailable", locateSql("gam_demo_v2_1_0_preview2_anonymisiert.sql") != null);
    result.put("error", error);
    return result;
  }

  @PostMapping("/initialize")
  @ResponseStatus(HttpStatus.CREATED)
  public Map<String, Object> initialize(@RequestBody SetupRequest request) throws Exception {
    validate(request);
    Map<String, Object> current = status();
    if (Boolean.FALSE.equals(current.get("required"))) {
      throw new IllegalStateException("GAM ist bereits eingerichtet. Der Erststart-Assistent wurde aus Sicherheitsgründen beendet.");
    }

    String mode = normalize(request.mode(), "empty");
    String sqlFile = "demo".equals(mode)
        ? "gam_demo_v2_1_0_preview2_anonymisiert.sql"
        : "gam_v2_1_0_preview2_empty.sql";
    Path script = locateSql(sqlFile);
    if (script == null) throw new IllegalStateException("Die Datenbankvorlage " + sqlFile + " wurde nicht gefunden.");

    createDatabaseIfMissing();
    resetApplicationSchema();
    executeSqlScript(script);
    createAdministrator(request);
    applyPracticeDetails(request);

    return Map.of(
        "success", true,
        "mode", mode,
        "database", databaseName(),
        "username", request.adminUsername().trim(),
        "language", normalize(request.language(), "de"),
        "message", "GAM wurde erfolgreich eingerichtet."
    );
  }

  private void validate(SetupRequest request) {
    if (request == null) throw new IllegalArgumentException("Einrichtungsdaten fehlen.");
    if (blank(request.adminUsername())) throw new IllegalArgumentException("Bitte einen Administrator-Benutzernamen angeben.");
    if (request.adminUsername().trim().length() < 3) throw new IllegalArgumentException("Der Benutzername muss mindestens 3 Zeichen lang sein.");
    if (blank(request.adminPassword()) || request.adminPassword().length() < 8) throw new IllegalArgumentException("Das Administratorpasswort muss mindestens 8 Zeichen lang sein.");
    String mode = normalize(request.mode(), "empty");
    if (!List.of("empty", "demo").contains(mode)) throw new IllegalArgumentException("Unbekannte Datenbankauswahl.");
  }

  private void createDatabaseIfMissing() throws Exception {
    String serverUrl = serverJdbcUrl();
    String database = databaseName();
    try (Connection connection = DriverManager.getConnection(serverUrl, datasourceUser, datasourcePassword);
         Statement statement = connection.createStatement()) {
      statement.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + database.replace("`", "``") + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
    }
  }

  private void resetApplicationSchema() throws Exception {
    try (Connection connection = DriverManager.getConnection(datasourceUrl, datasourceUser, datasourcePassword);
         Statement statement = connection.createStatement()) {
      statement.execute("SET FOREIGN_KEY_CHECKS=0");
      List<String> tables = new ArrayList<>();
      try (ResultSet rs = statement.executeQuery("SELECT table_name FROM information_schema.tables WHERE table_schema='" + sql(databaseName()) + "'")) {
        while (rs.next()) tables.add(rs.getString(1));
      }
      for (String table : tables) {
        statement.execute("DROP TABLE IF EXISTS `" + table.replace("`", "``") + "`");
      }
      statement.execute("SET FOREIGN_KEY_CHECKS=1");
    }
  }

  private void executeSqlScript(Path script) throws Exception {
    String sql = Files.readString(script, StandardCharsets.UTF_8);
    List<String> statements = splitSql(sql);
    try (Connection connection = DriverManager.getConnection(datasourceUrl, datasourceUser, datasourcePassword);
         Statement statement = connection.createStatement()) {
      connection.setAutoCommit(false);
      try {
        for (String command : statements) {
          String trimmed = command.trim();
          if (!trimmed.isEmpty()) statement.execute(trimmed);
        }
        connection.commit();
      } catch (Exception ex) {
        connection.rollback();
        throw new IllegalStateException("Datenbankimport fehlgeschlagen: " + ex.getMessage(), ex);
      }
    }
  }

  static List<String> splitSql(String sql) {
    List<String> result = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    boolean single = false, dbl = false, backtick = false, lineComment = false, blockComment = false, escaped = false;
    for (int i = 0; i < sql.length(); i++) {
      char c = sql.charAt(i);
      char next = i + 1 < sql.length() ? sql.charAt(i + 1) : '\0';
      if (lineComment) {
        if (c == '\n') { lineComment = false; current.append(c); }
        continue;
      }
      if (blockComment) {
        if (c == '*' && next == '/') { blockComment = false; i++; }
        continue;
      }
      if (!single && !dbl && !backtick) {
        if (c == '-' && next == '-' && (i + 2 >= sql.length() || Character.isWhitespace(sql.charAt(i + 2)))) { lineComment = true; i++; continue; }
        if (c == '#') { lineComment = true; continue; }
        if (c == '/' && next == '*') { blockComment = true; i++; continue; }
      }
      if (c == '\\' && (single || dbl) && !escaped) { escaped = true; current.append(c); continue; }
      if (!escaped) {
        if (c == '\'' && !dbl && !backtick) single = !single;
        else if (c == '"' && !single && !backtick) dbl = !dbl;
        else if (c == '`' && !single && !dbl) backtick = !backtick;
      } else escaped = false;
      if (c == ';' && !single && !dbl && !backtick) {
        if (!current.toString().trim().isEmpty()) result.add(current.toString());
        current.setLength(0);
      } else current.append(c);
    }
    if (!current.toString().trim().isEmpty()) result.add(current.toString());
    return result;
  }

  private void createAdministrator(SetupRequest request) throws Exception {
    try (Connection connection = DriverManager.getConnection(datasourceUrl, datasourceUser, datasourcePassword);
         Statement statement = connection.createStatement()) {
      String username = sql(request.adminUsername().trim());
      statement.executeUpdate("DELETE FROM accounts WHERE username='" + username + "'");
      String password = sha256(request.adminPassword());
      String fullname = sql(blank(request.adminName()) ? request.adminUsername().trim() : request.adminName().trim());
      String email = sql(blank(request.adminEmail()) ? "" : request.adminEmail().trim());
      statement.executeUpdate("INSERT INTO accounts (username,password,fullname,role,email,secretkey) VALUES ('" + username + "','" + password + "','" + fullname + "','superadmin','" + email + "',NULL)");
    }
  }

  private void applyPracticeDetails(SetupRequest request) {
    if (blank(request.practiceName())) return;
    String practice = request.practiceName().trim();
    try (Connection connection = DriverManager.getConnection(datasourceUrl, datasourceUser, datasourcePassword);
         Statement statement = connection.createStatement()) {
      // Die historisch gewachsenen Dumps unterscheiden sich leicht. Daher nur vorhandene Spalten aktualisieren.
      if (columnExists(connection, "rechnungsgesellschaft", "GESELLSCHAFTSNAME")) {
        int changed = statement.executeUpdate("UPDATE rechnungsgesellschaft SET GESELLSCHAFTSNAME='" + sql(practice) + "' ORDER BY ID LIMIT 1");
        if (changed == 0) statement.executeUpdate("INSERT INTO rechnungsgesellschaft (GESELLSCHAFTSNAME) VALUES ('" + sql(practice) + "')");
      } else if (columnExists(connection, "gesellschaft", "NAME")) {
        statement.executeUpdate("UPDATE gesellschaft SET NAME='" + sql(practice) + "' ORDER BY ID LIMIT 1");
      }
    } catch (Exception ignored) {
      // Praxisangaben sind Komfortdaten; ein uneinheitlicher Legacy-Dump darf die Einrichtung nicht abbrechen.
    }
  }

  private boolean columnExists(Connection connection, String table, String column) {
    try (ResultSet rs = connection.getMetaData().getColumns(connection.getCatalog(), null, table, column)) { return rs.next(); }
    catch (Exception ex) { return false; }
  }

  private Path locateSql(String filename) {
    for (Path candidate : List.of(
        Paths.get("database", filename), Paths.get("..", "database", filename),
        Paths.get("sql", filename), Paths.get("..", "sql", filename))) {
      Path absolute = candidate.toAbsolutePath().normalize();
      if (Files.isRegularFile(absolute)) return absolute;
    }
    return null;
  }

  private String databaseName() {
    String clean = datasourceUrl;
    int query = clean.indexOf('?'); if (query >= 0) clean = clean.substring(0, query);
    int slash = clean.lastIndexOf('/');
    return slash >= 0 ? clean.substring(slash + 1) : "kopfzentruminventardb";
  }

  private String serverJdbcUrl() {
    String clean = datasourceUrl;
    int query = clean.indexOf('?'); String suffix = query >= 0 ? clean.substring(query) : "";
    if (query >= 0) clean = clean.substring(0, query);
    int slash = clean.lastIndexOf('/');
    return (slash >= 0 ? clean.substring(0, slash + 1) : clean + "/") + suffix;
  }

  private String sha256(String value) throws Exception {
    return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
  }
  private String sql(String value) { return value == null ? "" : value.replace("\\", "\\\\").replace("'", "''"); }
  private String normalize(String value, String fallback) { return blank(value) ? fallback : value.trim().toLowerCase(Locale.ROOT); }
  private boolean blank(String value) { return value == null || value.isBlank(); }
}
