package de.kopfzentrum.gam.system;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Schritt 40k33a11: nachträglicher, administrativer Import der gepflegten Beispieldaten. */
@RestController
@RequestMapping("/api/database/demo")
public class DemoDatabaseController {
  private static final Logger LOG = LoggerFactory.getLogger(DemoDatabaseController.class);
  private static final String DEMO_FILE = "gam-2.1.0-preview3-leere-db-zu-beispieldatenbank-40k33a11.sql";

  @Value("${spring.datasource.url}") private String datasourceUrl;
  @Value("${spring.datasource.username:root}") private String datasourceUser;
  @Value("${spring.datasource.password:}") private String datasourcePassword;

  @GetMapping("/status")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public Map<String,Object> status() throws Exception {
    Path file = locateSql();
    Map<String,Object> result = new LinkedHashMap<>();
    result.put("available", file != null);
    result.put("file", DEMO_FILE);
    result.put("accountCount", count("accounts"));
    result.put("patientCount", count("adressen"));
    result.put("invoiceCount", count("rechnung"));
    result.put("message", file == null ? "Die gepflegte Beispieldatendatei wurde nicht gefunden." : "Beispieldaten können importiert werden.");
    return result;
  }

  @PostMapping("/import")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public Map<String,Object> importDemo() throws Exception {
    Path script = locateSql();
    if (script == null) throw new IllegalStateException("Die Beispieldatendatei " + DEMO_FILE + " wurde nicht gefunden.");
    String sql = Files.readString(script, StandardCharsets.UTF_8);
    try (Connection connection = DriverManager.getConnection(datasourceUrl, datasourceUser, datasourcePassword);
         Statement statement = connection.createStatement()) {
      connection.setAutoCommit(false);
      int commandNumber = 0;
      String currentCommand = null;
      try {
        for (String command : FirstRunSetupController.splitSql(sql)) {
          String trimmed = command.trim();
          if (trimmed.isEmpty() || isConnectionControlStatement(trimmed)) continue;
          currentCommand = trimmed;
          commandNumber++;
          statement.execute(trimmed);
        }
        if (!connection.isClosed()) connection.commit();
      } catch (Exception ex) {
        rollbackSafely(connection, ex);
        String preview = sqlPreview(currentCommand);
        LOG.error("Beispieldatenimport fehlgeschlagen bei SQL-Anweisung {}: {}", commandNumber, preview, ex);
        throw new IllegalStateException("Beispieldatenimport fehlgeschlagen bei Anweisung " + commandNumber
          + (preview.isBlank() ? "" : " (" + preview + ")") + ": " + ex.getMessage(), ex);
      }
    }
    return Map.of("success", true, "file", DEMO_FILE, "message", "Die anonymisierten Beispieldaten wurden erfolgreich importiert.");
  }

  private static boolean isConnectionControlStatement(String sql) {
    String upper = sql.stripLeading().toUpperCase(java.util.Locale.ROOT);
    return upper.startsWith("USE ")
      || upper.equals("START TRANSACTION")
      || upper.equals("BEGIN")
      || upper.equals("COMMIT")
      || upper.equals("ROLLBACK")
      || upper.startsWith("LOCK TABLES")
      || upper.startsWith("UNLOCK TABLES");
  }

  private static void rollbackSafely(Connection connection, Exception original) {
    if (connection == null) return;
    try {
      if (!connection.isClosed()) connection.rollback();
    } catch (SQLException rollbackError) {
      original.addSuppressed(rollbackError);
      LOG.warn("Rollback des Beispieldatenimports war nicht mehr möglich: {}", rollbackError.getMessage());
    }
  }

  private static String sqlPreview(String sql) {
    if (sql == null) return "";
    String oneLine = sql.replaceAll("\\s+", " ").trim();
    return oneLine.length() <= 180 ? oneLine : oneLine.substring(0, 177) + "...";
  }

  private long count(String table) {
    try (Connection connection = DriverManager.getConnection(datasourceUrl, datasourceUser, datasourcePassword);
         Statement statement = connection.createStatement();
         ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM `" + table.replace("`", "``") + "`")) {
      return rs.next() ? rs.getLong(1) : 0L;
    } catch (Exception ignored) { return 0L; }
  }

  private Path locateSql() {
    Path userDir = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
    for (Path candidate : new Path[]{
        userDir.resolve("database").resolve(DEMO_FILE),
        userDir.resolve("../database").resolve(DEMO_FILE).normalize(),
        userDir.resolve(DEMO_FILE)}) {
      if (Files.isRegularFile(candidate)) return candidate;
    }
    return null;
  }
}
