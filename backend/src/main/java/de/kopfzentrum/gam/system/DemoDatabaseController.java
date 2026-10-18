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
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
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
  public static final String DEMO_FILE = "gam-2.1.0-preview3-leere-db-zu-beispieldatenbank-40k33a11.sql";

  @Value("${spring.datasource.url}") private String datasourceUrl;
  @Value("${spring.datasource.username:root}") private String datasourceUser;
  @Value("${spring.datasource.password:}") private String datasourcePassword;

  private record ImportProgress(boolean running,int percent,String phase,String message,int current,int total,boolean success,String error) {}
  private final AtomicReference<ImportProgress> importProgress = new AtomicReference<>(
    new ImportProgress(false,0,"BEREIT","Noch kein Import gestartet.",0,0,false,null));

  @GetMapping("/progress")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public ImportProgress progress() { return importProgress.get(); }

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
    if(importProgress.get().running()) throw new IllegalStateException("Ein Beispieldatenimport läuft bereits.");
    Path script = locateSql();
    if (script == null) throw new IllegalStateException("Die Beispieldatendatei " + DEMO_FILE + " wurde nicht gefunden.");
    Path runtime = locateSql("gam_v2_1_0_preview3_runtime_schema.sql");
    if (runtime == null) throw new IllegalStateException("Das aktuelle GAM-Runtime-Schema wurde nicht gefunden.");

    String runtimeSql = Files.readString(runtime, StandardCharsets.UTF_8);
    String dataSql = Files.readString(script, StandardCharsets.UTF_8);
    List<String> runtimeCommands = FirstRunSetupController.splitSql(runtimeSql).stream().map(String::trim).filter(x->!x.isEmpty()&&!isConnectionControlStatement(x)).toList();
    List<String> dataCommands = FirstRunSetupController.splitSql(dataSql).stream().map(String::trim).filter(x->!x.isEmpty()&&!isConnectionControlStatement(x)).toList();
    int total = runtimeCommands.size() + dataCommands.size() + 2;
    int done = 0;
    importProgress.set(new ImportProgress(true,1,"START","Import wird vorbereitet …",0,total,false,null));
    try {
      importProgress.set(progressOf(++done,total,"RUNTIME_SCHEMA","Runtime-Schema wird geprüft und ergänzt …",false,null));
      executeSchemaCommands(runtimeCommands,done,total);
      done += runtimeCommands.size();

      importProgress.set(progressOf(done,total,"BEISPIELDATEN","Beispieldaten werden importiert …",false,null));
      try (Connection connection = DriverManager.getConnection(datasourceUrl, datasourceUser, datasourcePassword);
           Statement statement = connection.createStatement()) {
        connection.setAutoCommit(false);
        int commandNumber = 0;
        String currentCommand = null;
        try {
          for (String trimmed : dataCommands) {
            currentCommand = trimmed; commandNumber++;
            statement.execute(trimmed);
            done++;
            importProgress.set(progressOf(done,total,"BEISPIELDATEN","SQL-Anweisung " + commandNumber + "/" + dataCommands.size() + " verarbeitet",false,null));
          }
          importProgress.set(progressOf(++done,total,"ABSCHLUSS","Transaktion wird abgeschlossen …",false,null));
          if (!connection.isClosed()) connection.commit();
        } catch (Exception ex) {
          rollbackSafely(connection, ex);
          String preview = sqlPreview(currentCommand);
          LOG.error("Beispieldatenimport fehlgeschlagen bei SQL-Anweisung {}: {}", commandNumber, preview, ex);
          throw new IllegalStateException("Beispieldatenimport fehlgeschlagen bei Anweisung " + commandNumber
            + (preview.isBlank() ? "" : " (" + preview + ")") + ": " + ex.getMessage(), ex);
        }
      }
      importProgress.set(new ImportProgress(false,100,"FERTIG","Beispieldaten wurden erfolgreich importiert.",total,total,true,null));
      return Map.of("success", true, "file", DEMO_FILE, "message", "Die anonymisierten Beispieldaten wurden erfolgreich importiert.");
    } catch (Exception ex) {
      ImportProgress last=importProgress.get();
      importProgress.set(new ImportProgress(false,last.percent(),"FEHLER","Import fehlgeschlagen.",last.current(),last.total(),false,ex.getMessage()));
      throw ex;
    }
  }

  private ImportProgress progressOf(int current,int total,String phase,String message,boolean success,String error) {
    int percent = total<=0?0:Math.max(1,Math.min(99,(int)Math.floor((current*100.0)/total)));
    return new ImportProgress(true,percent,phase,message,current,total,success,error);
  }

  private void executeSchemaCommands(List<String> commands,int baseDone,int total) throws Exception {
    try (Connection connection = DriverManager.getConnection(datasourceUrl, datasourceUser, datasourcePassword);
         Statement statement = connection.createStatement()) {
      int commandNumber=0;
      for (String trimmed : commands) {
        commandNumber++;
        try {
          statement.execute(trimmed);
        } catch (SQLException ex) {
          int code=ex.getErrorCode();
          if (code!=1060 && code!=1061 && code!=1068)
            throw new IllegalStateException("Runtime-Schema fehlgeschlagen bei Anweisung " + commandNumber + " (MariaDB " + code + "): " + ex.getMessage(), ex);
        }
        importProgress.set(progressOf(baseDone+commandNumber,total,"RUNTIME_SCHEMA","Runtime-Schema " + commandNumber + "/" + commands.size() + " geprüft",false,null));
      }
    }
  }

  static boolean isConnectionControlStatement(String sql) {
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

  private Path locateSql() { return locateSql(DEMO_FILE); }

  private Path locateSql(String filename) {
    Path userDir = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
    for (Path candidate : new Path[]{
        userDir.resolve("database").resolve(filename),
        userDir.resolve("../database").resolve(filename).normalize(),
        userDir.resolve(filename)}) {
      if (Files.isRegularFile(candidate)) return candidate;
    }
    return null;
  }

  private void executeSchema(Path script) throws Exception {
    String sql = Files.readString(script, StandardCharsets.UTF_8);
    try (Connection connection = DriverManager.getConnection(datasourceUrl, datasourceUser, datasourcePassword);
         Statement statement = connection.createStatement()) {
      int commandNumber=0;
      for (String command : FirstRunSetupController.splitSql(sql)) {
        String trimmed = command.trim();
        if (trimmed.isEmpty() || isConnectionControlStatement(trimmed)) continue;
        commandNumber++;
        try {
          statement.execute(trimmed);
        } catch (SQLException ex) {
          // 40k36q28: Das Runtime-Schema ist additiv und wird absichtlich auch auf
          // bereits laufenden Datenbanken angewendet. MariaDB meldet bei wiederholten
          // ALTERs u.a. "Multiple primary key defined" / doppelte Index-/Spaltennamen.
          // Diese bekannten Idempotenzfälle sind kein Fehler; andere SQL-Fehler bleiben sichtbar.
          int code=ex.getErrorCode();
          if (code==1060 || code==1061 || code==1068) {
            LOG.debug("Runtime-Schema: bereits vorhandene Struktur bei Anweisung {} ignoriert (MariaDB {}): {}",
              commandNumber, code, ex.getMessage());
            continue;
          }
          throw new IllegalStateException("Runtime-Schema fehlgeschlagen bei Anweisung " + commandNumber
            + " (MariaDB " + code + "): " + ex.getMessage(), ex);
        }
      }
    }
  }
}
