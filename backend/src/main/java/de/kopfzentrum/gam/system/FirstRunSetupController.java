package de.kopfzentrum.gam.system;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PushbackReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/setup")
public class FirstRunSetupController {
  private static final Logger log = LoggerFactory.getLogger(FirstRunSetupController.class);
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
        initialized = coreTablesPresent(connection);
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
    result.put("emptyDatabaseAvailable", locateSql("gam_v2_1_0_preview3_empty.sql") != null
        && locateSql("gam_v2_1_0_preview3_runtime_schema.sql") != null);
    result.put("demoDatabaseAvailable", locateSql(DemoDatabaseController.DEMO_FILE) != null
        && locateSql("gam_v2_1_0_preview3_empty.sql") != null
        && locateSql("gam_v2_1_0_preview3_runtime_schema.sql") != null);
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
    Path baseScript = locateSql("gam_v2_1_0_preview3_empty.sql");
    if (baseScript == null) throw new IllegalStateException("Die leere GAM-Datenbankvorlage wurde nicht gefunden.");

    log.info("[Setup 1/8] Datenbank sicherstellen");
    createDatabaseIfMissing();
    log.info("[Setup 2/8] Anwendungsschema zurücksetzen");
    resetApplicationSchema();
    log.info("[Setup 3/8] Basisschema importieren");
    executeSqlScript(baseScript);
    log.info("[Setup 4/8] Runtime-Schema anwenden");
    ensureRuntimeSchema();
    log.info("[Setup 5/8] Datenbankschema verifizieren");
    verifyBaseSchema(baseScript);
    verifyRuntimeSchema();
    verifyCriticalRuntimeColumns();
    log.info("[Setup 6/8] Optionale Beispieldaten verarbeiten");
    if ("demo".equals(mode)) {
      Path demoData = locateSql(DemoDatabaseController.DEMO_FILE);
      if (demoData == null) throw new IllegalStateException("Die Beispieldatendatei " + DemoDatabaseController.DEMO_FILE + " wurde nicht gefunden.");
      executeDataSqlScript(demoData);
    }
    log.info("[Setup 7/8] Administrator und Praxisdaten anlegen");
    createAdministrator(request);
    verifyAdministrator(request.adminUsername().trim());
    applyPracticeDetails(request);
    log.info("[Setup 8/8] Einrichtung erfolgreich abgeschlossen");

    return Map.of(
        "success", true,
        "mode", mode,
        "database", databaseName(),
        "username", request.adminUsername().trim(),
        "language", normalize(request.language(), "de"),
        "message", "GAM wurde erfolgreich eingerichtet."
    );
  }


  private boolean coreTablesPresent(Connection connection) {
    for (String table : List.of("accounts", "news", "rechnungsgesellschaft", "gam_settings")) {
      try (var ps = connection.prepareStatement(
          "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=? AND table_name=?")) {
        ps.setString(1, databaseName());
        ps.setString(2, table);
        try (ResultSet rs = ps.executeQuery()) {
          if (!rs.next() || rs.getInt(1) != 1) return false;
        }
      } catch (Exception ex) {
        return false;
      }
    }
    return true;
  }

  private void ensureRuntimeSchema() throws Exception {
    Path runtime = locateSql("gam_v2_1_0_preview3_runtime_schema.sql");
    if (runtime == null) throw new IllegalStateException("Das aktuelle GAM-Runtime-Schema wurde nicht gefunden.");
    executeRuntimeSchemaScript(runtime);
  }

  private void verifyRuntimeSchema() throws Exception {
    Path runtime = locateSql("gam_v2_1_0_preview3_runtime_schema.sql");
    if (runtime == null) throw new IllegalStateException("Das aktuelle GAM-Runtime-Schema wurde nicht gefunden.");
    verifySchemaTables(runtime);
  }

  private void verifySchemaTables(Path script) throws Exception {
    String sqlText = Files.readString(script, StandardCharsets.UTF_8);
    var matcher = java.util.regex.Pattern.compile("(?im)^\\s*CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?`?([A-Za-z0-9_]+)`?").matcher(sqlText);
    List<String> expected = new ArrayList<>();
    while (matcher.find()) expected.add(matcher.group(1));
    List<String> missing = new ArrayList<>();
    try (Connection connection = DriverManager.getConnection(datasourceUrl, datasourceUser, datasourcePassword)) {
      for (String table : expected) {
        try (var ps = connection.prepareStatement("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=? AND table_name=?")) {
          ps.setString(1, databaseName());
          ps.setString(2, table);
          try (ResultSet rs = ps.executeQuery()) { if (!rs.next() || rs.getInt(1) != 1) missing.add(table); }
        }
      }
    }
    if (!missing.isEmpty()) throw new IllegalStateException("Runtime-Schema unvollständig. Fehlende Tabellen: " + String.join(", ", missing));
  }

  private void verifyCriticalRuntimeColumns() throws Exception {
    Map<String,List<String>> expected = Map.of(
        "gam_linux_ssh_settings", List.of("id","enabled","username","auth_mode","password","key_path","port","updated_at","updated_by"),
        "gam_winrm_settings", List.of("id","enabled","username","password","port","use_https","updated_at","updated_by"),
        "gam_winrm_trusted_targets", List.of("host","created_at","created_by"),
        "gam_os_access_profile", List.of("id","type","name","host","enabled","username","auth_mode","password","key_path","port","use_https","pairing_port","pairing_code","identity_key","updated_at","updated_by"),
        "gam_discovery_registered_devices", List.of("identity_key","name","device_type","address","hardware_address","serial_number","manufacturer","discovery_protocol","pending_discovery_protocol","device_status","first_seen_at","last_seen_at","registered_at","detection_count","last_scan_hits","manual_device_type","manual_name","adb_host","adb_port","adb_last_connected_at")
    );
    try (Connection connection = DriverManager.getConnection(datasourceUrl, datasourceUser, datasourcePassword)) {
      for (var entry : expected.entrySet()) for (String column : entry.getValue()) {
        if (!columnExists(connection, entry.getKey(), column))
          throw new IllegalStateException("Runtime-Schema unvollständig: Spalte " + entry.getKey() + "." + column + " fehlt.");
      }
    }
  }

  private void executeDataSqlScript(Path script) throws Exception {
    final int commitInterval = 25;
    final int maxStatementChars = 8 * 1024 * 1024;
    final long fileSize = Files.size(script);
    log.info("[Setup 6a/8] Streaming-Beispieldatenimport gestartet: {} MB; SQL wird ohne Vorab-Zerlegung verarbeitet",
        Math.max(1L, fileSize / (1024L * 1024L)));

    try (Connection connection = DriverManager.getConnection(datasourceUrl, datasourceUser, datasourcePassword);
         Statement statement = connection.createStatement();
         PushbackReader reader = new PushbackReader(new BufferedReader(Files.newBufferedReader(script, StandardCharsets.UTF_8), 64 * 1024), 2)) {
      connection.setAutoCommit(false);
      int commandNumber = 0;
      int sinceCommit = 0;
      long importStarted = System.nanoTime();
      long charsRead = 0L;
      String currentCommand = "";

      StringBuilder current = new StringBuilder(64 * 1024);
      boolean single = false, dbl = false, backtick = false, lineComment = false, blockComment = false, escaped = false;
      try {
        int raw;
        while ((raw = reader.read()) != -1) {
          charsRead++;
          char c = (char) raw;

          if (lineComment) {
            if (c == '\n') {
              lineComment = false;
              current.append(c);
            }
            continue;
          }

          if (blockComment) {
            if (c == '*') {
              int peek = reader.read();
              if (peek != -1) {
                charsRead++;
                if ((char) peek == '/') {
                  blockComment = false;
                } else {
                  reader.unread(peek);
                  charsRead--;
                }
              }
            }
            continue;
          }

          if (!single && !dbl && !backtick) {
            if (c == '-') {
              int peek = reader.read();
              if (peek != -1) {
                charsRead++;
                if ((char) peek == '-') {
                  int third = reader.read();
                  if (third != -1) {
                    charsRead++;
                    if (Character.isWhitespace((char) third)) {
                      lineComment = true;
                      continue;
                    }
                    reader.unread(third);
                    charsRead--;
                  }
                }
                reader.unread(peek);
                charsRead--;
              }
            } else if (c == '#') {
              lineComment = true;
              continue;
            } else if (c == '/') {
              int peek = reader.read();
              if (peek != -1) {
                charsRead++;
                if ((char) peek == '*') {
                  blockComment = true;
                  continue;
                }
                reader.unread(peek);
                charsRead--;
              }
            }
          }

          if (c == '\\' && (single || dbl) && !escaped) {
            escaped = true;
            current.append(c);
            ensureDemoStatementSize(current, maxStatementChars, commandNumber + 1, charsRead);
            continue;
          }
          if (!escaped) {
            if (c == '\'' && !dbl && !backtick) single = !single;
            else if (c == '"' && !single && !backtick) dbl = !dbl;
            else if (c == '`' && !single && !dbl) backtick = !backtick;
          } else {
            escaped = false;
          }

          if (c == ';' && !single && !dbl && !backtick) {
            String trimmed = current.toString().trim();
            current.setLength(0);
            if (trimmed.isEmpty() || DemoDatabaseController.isConnectionControlStatement(trimmed)) continue;

            commandNumber++;
            currentCommand = trimmed;
            long commandStarted = System.nanoTime();
            statement.execute(trimmed);
            long commandMillis = (System.nanoTime() - commandStarted) / 1_000_000L;
            sinceCommit++;

            if (commandMillis >= 2_000L) {
              log.info("[Setup 6b/8] Langsame Demo-SQL-Anweisung {} abgeschlossen ({} ms): {}",
                  commandNumber, commandMillis, sqlPreview(trimmed));
            } else if (commandNumber == 1 || commandNumber % 10 == 0) {
              log.info("[Setup 6b/8] Beispieldatenimport: {} SQL-Anweisungen verarbeitet",
                  commandNumber);
            }

            if (sinceCommit >= commitInterval) {
              connection.commit();
              sinceCommit = 0;
              log.debug("[Setup 6c/8] Zwischen-Commit nach SQL-Anweisung {}", commandNumber);
            }
            currentCommand = "";
          } else {
            current.append(c);
            ensureDemoStatementSize(current, maxStatementChars, commandNumber + 1, charsRead);
          }
        }

        String trailing = current.toString().trim();
        if (!trailing.isEmpty() && !DemoDatabaseController.isConnectionControlStatement(trailing)) {
          commandNumber++;
          currentCommand = trailing;
          long commandStarted = System.nanoTime();
          statement.execute(trailing);
          long commandMillis = (System.nanoTime() - commandStarted) / 1_000_000L;
          sinceCommit++;
          if (commandMillis >= 2_000L) {
            log.info("[Setup 6b/8] Langsame abschließende Demo-SQL-Anweisung {} abgeschlossen ({} ms): {}",
                commandNumber, commandMillis, sqlPreview(trailing));
          }
          currentCommand = "";
        }

        connection.commit();
        long totalMillis = (System.nanoTime() - importStarted) / 1_000_000L;
        log.info("[Setup 6d/8] Streaming-Beispieldatenimport abgeschlossen und Transaktion beendet ({} SQL-Anweisungen, {} ms)",
            commandNumber, totalMillis);
      } catch (Exception ex) {
        try { connection.rollback(); } catch (Exception ignored) {}
        throw new IllegalStateException("Beispieldatenimport fehlgeschlagen bei Anweisung " + commandNumber
            + " (" + sqlPreview(currentCommand) + "): " + ex.getMessage(), ex);
      } finally {
        try {
          if (!connection.getAutoCommit()) connection.setAutoCommit(true);
        } catch (Exception ignored) {}
      }
    }
  }

  private void ensureDemoStatementSize(StringBuilder current, int maxStatementChars, int nextCommandNumber, long charsRead) {
    if (current.length() <= maxStatementChars) return;
    throw new IllegalStateException(
        "Beispieldatenimport abgebrochen: SQL-Anweisung " + nextCommandNumber
            + " überschreitet die Sicherheitsgrenze von " + (maxStatementChars / (1024 * 1024))
            + " MiB (gelesene Zeichen: " + charsRead + "). Der SQL-Dump ist vermutlich beschädigt oder unvollständig.");
  }

  private String sqlPreview(String sql) {
    if (sql == null) return "";
    String oneLine = sql.replaceAll("\\s+", " ").trim();
    return oneLine.length() <= 180 ? oneLine : oneLine.substring(0, 177) + "...";
  }

  private void verifyBaseSchema(Path script) throws Exception {
    String sqlText = Files.readString(script, StandardCharsets.UTF_8);
    var matcher = java.util.regex.Pattern.compile("(?im)^CREATE TABLE `([^`]+)`").matcher(sqlText);
    List<String> expected = new ArrayList<>();
    while (matcher.find()) expected.add(matcher.group(1));
    List<String> missing = new ArrayList<>();
    try (Connection connection = DriverManager.getConnection(datasourceUrl, datasourceUser, datasourcePassword)) {
      for (String table : expected) {
        try (var ps = connection.prepareStatement(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=? AND table_name=?")) {
          ps.setString(1, databaseName());
          ps.setString(2, table);
          try (ResultSet rs = ps.executeQuery()) {
            if (!rs.next() || rs.getInt(1) != 1) missing.add(table);
          }
        }
      }
    }
    if (!missing.isEmpty()) {
      throw new IllegalStateException("Datenbankimport unvollständig. Fehlende Tabellen: " + String.join(", ", missing));
    }
  }

  private void verifyAdministrator(String username) throws Exception {
    try (Connection connection = DriverManager.getConnection(datasourceUrl, datasourceUser, datasourcePassword);
         var ps = connection.prepareStatement("SELECT COUNT(*) FROM accounts WHERE username=? AND role='superadmin'")) {
      ps.setString(1, username);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next() || rs.getInt(1) != 1) {
          throw new IllegalStateException("Administrator wurde nicht vollständig angelegt.");
        }
      }
    }
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
    closeOtherApplicationConnections();
    try (Connection connection = DriverManager.getConnection(datasourceUrl, datasourceUser, datasourcePassword);
         Statement statement = connection.createStatement()) {
      statement.execute("SET SESSION lock_wait_timeout=10");
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

  private void closeOtherApplicationConnections() throws Exception {
    String db = databaseName();
    try (Connection connection = DriverManager.getConnection(serverJdbcUrl(), datasourceUser, datasourcePassword);
         Statement statement = connection.createStatement()) {
      long ownId = -1;
      try (ResultSet rs = statement.executeQuery("SELECT CONNECTION_ID()")) { if (rs.next()) ownId = rs.getLong(1); }
      List<Long> ids = new ArrayList<>();
      try (var ps = connection.prepareStatement("SELECT ID FROM information_schema.PROCESSLIST WHERE DB=? AND USER=? AND ID<>?")) {
        ps.setString(1, db); ps.setString(2, datasourceUser); ps.setLong(3, ownId);
        try (ResultSet rs = ps.executeQuery()) { while (rs.next()) ids.add(rs.getLong(1)); }
      }
      for (Long id : ids) {
        try { statement.execute("KILL CONNECTION " + id); log.info("[Setup] Alte GAM-DB-Verbindung {} beendet, um Metadata-Locks zu lösen", id); }
        catch (SQLException ex) { log.debug("[Setup] DB-Verbindung {} konnte nicht beendet werden: {}", id, ex.getMessage()); }
      }
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

  /**
   * Führt ausschließlich das additive Runtime-Schema idempotent aus.
   *
   * <p>40k36t14: Der Erststart-Assistent verwendet damit dieselbe Schutzlogik wie der
   * bereits gehärtete Demo-Datenbankpfad aus 40k36q28. Wiederholte additive ALTERs
   * dürfen bei bereits vorhandenen Spalten, Indizes oder Primärschlüsseln nicht die
   * komplette Erstinitialisierung abbrechen. Andere SQL-Fehler bleiben weiterhin hart.
   */
  private void executeRuntimeSchemaScript(Path script) throws Exception {
    String sql = Files.readString(script, StandardCharsets.UTF_8);
    List<String> statements = splitSql(sql);
    try (Connection connection = DriverManager.getConnection(datasourceUrl, datasourceUser, datasourcePassword);
         Statement statement = connection.createStatement()) {
      statement.execute("SET SESSION lock_wait_timeout=10");
      int commandNumber = 0;
      for (String command : statements) {
        String trimmed = command.trim();
        if (trimmed.isEmpty() || DemoDatabaseController.isConnectionControlStatement(trimmed)) continue;
        commandNumber++;
        if (isRedundantPrimaryKey(connection, trimmed)) {
          log.debug("[Setup Runtime] PRIMARY KEY bereits vorhanden, Anweisung {} übersprungen", commandNumber);
          continue;
        }
        try {
          statement.execute(trimmed);
        } catch (SQLException ex) {
          int code = ex.getErrorCode();
          // MariaDB: 1060 = duplicate column, 1061 = duplicate key/index,
          // 1068 = multiple primary key. Diese Fälle sind bei einem additiven
          // Runtime-Schema erwartbare Idempotenzfälle.
          if (code == 1060 || code == 1061 || code == 1068) continue;
          throw new IllegalStateException(
              "Runtime-Schema fehlgeschlagen bei Anweisung " + commandNumber
                  + " (MariaDB " + code + "): " + ex.getMessage(), ex);
        }
      }
    }
  }

  private boolean isRedundantPrimaryKey(Connection connection, String sqlStatement) {
    var m = java.util.regex.Pattern.compile("(?is)^\\s*ALTER\\s+TABLE\\s+`?([A-Za-z0-9_]+)`?.*?ADD\\s+PRIMARY\\s+KEY").matcher(sqlStatement);
    if (!m.find()) return false;
    String table = m.group(1);
    try (var ps = connection.prepareStatement("SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema=? AND table_name=? AND constraint_type='PRIMARY KEY'")) {
      ps.setString(1, databaseName()); ps.setString(2, table);
      try (ResultSet rs = ps.executeQuery()) { return rs.next() && rs.getInt(1) > 0; }
    } catch (SQLException ex) { return false; }
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
