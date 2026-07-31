package de.kopfzentrum.gam.inventory;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 40k34c: App-/Paketinventar als Unterdaten EINER Geräteidentität - bewusst
 * generisch benannt (nicht "android_"-präfigiert), damit dieselbe Struktur
 * später auch für Windows-/Linux-Softwareinventar wiederverwendbar wäre
 * (siehe Auftrag: plattformübergreifend wiederverwendbare Struktur bevorzugt).
 * Aktuell wird sie ausschließlich von der Android-Inventarisierung befüllt.
 *
 * Zwei Tabellen, keine "Android-Sonderdatenbank":
 * - gam_discovery_installed_apps: aktueller Stand, EIN Datensatz je (Gerät, Paket).
 * - gam_discovery_app_inventory_runs: Verlauf/Status der einzelnen Inventarläufe,
 *   Grundlage für Änderungserkennung und Vollständigkeitsanzeige.
 */
@Repository
public class AppInventoryRepository {
  private final JdbcTemplate jdbc;
  public AppInventoryRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

  @PostConstruct void init() {
    jdbc.execute("""
      CREATE TABLE IF NOT EXISTS gam_discovery_installed_apps (
        identity_key VARCHAR(255) NOT NULL,
        package_name VARCHAR(255) NOT NULL,
        android_user_id INT NOT NULL DEFAULT 0,
        android_user_name VARCHAR(255) NULL,
        display_name VARCHAR(255) NULL,
        version_name VARCHAR(100) NULL,
        version_code BIGINT NULL,
        install_time TIMESTAMP NULL,
        update_time TIMESTAMP NULL,
        installer_package VARCHAR(255) NULL,
        is_system BOOLEAN NOT NULL DEFAULT FALSE,
        is_updated_system BOOLEAN NOT NULL DEFAULT FALSE,
        is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
        is_debuggable BOOLEAN NOT NULL DEFAULT FALSE,
        is_test_only BOOLEAN NOT NULL DEFAULT FALSE,
        min_sdk INT NULL,
        target_sdk INT NULL,
        roles VARCHAR(255) NULL,
        first_seen_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        last_seen_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        removed_at TIMESTAMP NULL,
        PRIMARY KEY(identity_key, package_name, android_user_id)
      )
      """);
    try { jdbc.execute("ALTER TABLE gam_discovery_installed_apps ADD COLUMN IF NOT EXISTS android_user_id INT NOT NULL DEFAULT 0 AFTER package_name"); } catch (Exception ignored) {}
    try { jdbc.execute("ALTER TABLE gam_discovery_installed_apps ADD COLUMN IF NOT EXISTS android_user_name VARCHAR(255) NULL AFTER android_user_id"); } catch (Exception ignored) {}
    try {
      Integer hasUserPk = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='gam_discovery_installed_apps' AND CONSTRAINT_NAME='PRIMARY' AND COLUMN_NAME='android_user_id'", Integer.class);
      if (hasUserPk == null || hasUserPk == 0) jdbc.execute("ALTER TABLE gam_discovery_installed_apps DROP PRIMARY KEY, ADD PRIMARY KEY(identity_key, package_name, android_user_id)");
    } catch (Exception ignored) {}
    jdbc.execute("""
      CREATE TABLE IF NOT EXISTS gam_discovery_app_inventory_runs (
        id BIGINT NOT NULL AUTO_INCREMENT,
        identity_key VARCHAR(255) NOT NULL,
        started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        finished_at TIMESTAMP NULL,
        status VARCHAR(40) NOT NULL,
        app_count INT NOT NULL DEFAULT 0,
        user_app_count INT NOT NULL DEFAULT 0,
        system_app_count INT NOT NULL DEFAULT 0,
        changes_summary VARCHAR(500) NULL,
        message VARCHAR(500) NULL,
        PRIMARY KEY(id),
        INDEX idx_app_runs_identity(identity_key, started_at)
      )
      """);
  }

  public record InstalledApp(
    String packageName, Integer androidUserId, String androidUserName, String displayName, String versionName, Long versionCode,
    Object installTime, Object updateTime, String installerPackage, boolean system,
    boolean updatedSystem, boolean enabled, boolean debuggable, boolean testOnly,
    Integer minSdk, Integer targetSdk, String roles) {}

  public List<Map<String,Object>> currentApps(String identityKey) {
    return jdbc.queryForList("""
      SELECT package_name AS packageName, android_user_id AS androidUserId, android_user_name AS androidUserName, display_name AS displayName, version_name AS versionName,
             version_code AS versionCode, install_time AS installTime, update_time AS updateTime,
             installer_package AS installerPackage, is_system AS system, is_updated_system AS updatedSystem,
             is_enabled AS enabled, is_debuggable AS debuggable, is_test_only AS testOnly,
             min_sdk AS minSdk, target_sdk AS targetSdk, roles, first_seen_at AS firstSeenAt,
             last_seen_at AS lastSeenAt, removed_at AS removedAt
      FROM gam_discovery_installed_apps WHERE identity_key=? ORDER BY android_user_id ASC, package_name ASC
      """, identityKey);
  }

  /** 40k34c: aktiv geführte (nicht als entfernt markierte) Pakete für diese Identität. */
  public List<String> activePackageNames(String identityKey) {
    return jdbc.query("SELECT CONCAT(android_user_id, ':', package_name) FROM gam_discovery_installed_apps WHERE identity_key=? AND removed_at IS NULL",
      (rs, n) -> rs.getString(1), identityKey);
  }

  /**
   * 40k34e: Übernimmt beim Zusammenführen zweier Android-Datensätze die App-
   * Inventarläufe und aktuellen Paketdatensätze des Quellgeräts in das Zielgerät.
   * Läufe werden unverändert umgehängt (eigener Zeitstempel/Status bleibt erhalten -
   * die Herkunft jedes Laufs bleibt damit nachvollziehbar). Existiert für ein Paket
   * bereits ein Datensatz am Zielgerät, bleibt der zuletzt gesehene (neuere) Stand
   * erhalten und der ältere wird verworfen - es entsteht dabei kein doppelter
   * Primärschlüssel und kein stiller Verlust (der verworfene Datensatz beschrieb
   * ohnehin denselben Paketstand, nur älter).
   */
  public void reassignToTarget(String sourceKey, String targetKey) {
    if (sourceKey.equals(targetKey)) return;
    jdbc.update("UPDATE gam_discovery_app_inventory_runs SET identity_key=? WHERE identity_key=?", targetKey, sourceKey);

    List<Map<String,Object>> sourceApps = jdbc.queryForList(
      "SELECT package_name, android_user_id, last_seen_at FROM gam_discovery_installed_apps WHERE identity_key=?", sourceKey);
    for (Map<String,Object> app : sourceApps) {
      String pkg = (String) app.get("package_name");
      Number userId = (Number) app.get("android_user_id");
      List<Map<String,Object>> existing = jdbc.queryForList(
        "SELECT last_seen_at FROM gam_discovery_installed_apps WHERE identity_key=? AND package_name=? AND android_user_id=?", targetKey, pkg, userId);
      if (existing.isEmpty()) {
        jdbc.update("UPDATE gam_discovery_installed_apps SET identity_key=? WHERE identity_key=? AND package_name=? AND android_user_id=?",
          targetKey, sourceKey, pkg, userId);
      } else {
        Object targetSeen = existing.get(0).get("last_seen_at");
        Object sourceSeen = app.get("last_seen_at");
        boolean sourceNewer = sourceSeen != null && (targetSeen == null || sourceSeen.toString().compareTo(targetSeen.toString()) > 0);
        if (sourceNewer) {
          jdbc.update("DELETE FROM gam_discovery_installed_apps WHERE identity_key=? AND package_name=? AND android_user_id=?", targetKey, pkg, userId);
          jdbc.update("UPDATE gam_discovery_installed_apps SET identity_key=? WHERE identity_key=? AND package_name=? AND android_user_id=?",
            targetKey, sourceKey, pkg, userId);
        } else {
          jdbc.update("DELETE FROM gam_discovery_installed_apps WHERE identity_key=? AND package_name=? AND android_user_id=?", sourceKey, pkg, userId);
        }
      }
    }
  }

  public void upsertApp(String identityKey, InstalledApp app) {
    jdbc.update("""
      INSERT INTO gam_discovery_installed_apps
        (identity_key, package_name, android_user_id, android_user_name, display_name, version_name, version_code, install_time, update_time,
         installer_package, is_system, is_updated_system, is_enabled, is_debuggable, is_test_only,
         min_sdk, target_sdk, roles, first_seen_at, last_seen_at, removed_at)
      VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,NULL)
      ON DUPLICATE KEY UPDATE
        android_user_name=COALESCE(VALUES(android_user_name),android_user_name), display_name=COALESCE(VALUES(display_name),display_name),
        version_name=VALUES(version_name), version_code=VALUES(version_code),
        install_time=COALESCE(VALUES(install_time),install_time), update_time=VALUES(update_time),
        installer_package=COALESCE(VALUES(installer_package),installer_package),
        is_system=VALUES(is_system), is_updated_system=VALUES(is_updated_system), is_enabled=VALUES(is_enabled),
        is_debuggable=VALUES(is_debuggable), is_test_only=VALUES(is_test_only),
        min_sdk=VALUES(min_sdk), target_sdk=VALUES(target_sdk), roles=VALUES(roles),
        last_seen_at=CURRENT_TIMESTAMP, removed_at=NULL
      """, identityKey, app.packageName(), app.androidUserId(), app.androidUserName(), app.displayName(), app.versionName(), app.versionCode(),
      app.installTime(), app.updateTime(), app.installerPackage(), app.system(), app.updatedSystem(),
      app.enabled(), app.debuggable(), app.testOnly(), app.minSdk(), app.targetSdk(), app.roles());
  }

  /**
   * 40k34c: Markiert Pakete als entfernt - wird NUR nach einem als vollständig
   * bewerteten Lauf aufgerufen (siehe DeviceIdentityService), niemals nach
   * einer Teilinventarisierung.
   */
  public void markRemoved(String identityKey, List<String> packageNamesStillPresent) {
    if (packageNamesStillPresent.isEmpty()) {
      jdbc.update("UPDATE gam_discovery_installed_apps SET removed_at=CURRENT_TIMESTAMP WHERE identity_key=? AND removed_at IS NULL", identityKey);
      return;
    }
    String placeholders = String.join(",", packageNamesStillPresent.stream().map(x -> "?").toList());
    Object[] args = new Object[packageNamesStillPresent.size() + 1];
    args[0] = identityKey;
    for (int i = 0; i < packageNamesStillPresent.size(); i++) args[i + 1] = packageNamesStillPresent.get(i);
    jdbc.update("UPDATE gam_discovery_installed_apps SET removed_at=CURRENT_TIMESTAMP " +
      "WHERE identity_key=? AND removed_at IS NULL AND CONCAT(android_user_id, ':', package_name) NOT IN (" + placeholders + ")", args);
  }

  public long startRun(String identityKey) {
    jdbc.update("INSERT INTO gam_discovery_app_inventory_runs(identity_key, status) VALUES (?, 'LAEUFT')", identityKey);
    Long id = jdbc.queryForObject("SELECT MAX(id) FROM gam_discovery_app_inventory_runs WHERE identity_key=?", Long.class, identityKey);
    return id == null ? -1 : id;
  }

  public void finishRun(long runId, String status, int appCount, int userAppCount, int systemAppCount,
                        String changesSummary, String message) {
    jdbc.update("""
      UPDATE gam_discovery_app_inventory_runs
      SET finished_at=CURRENT_TIMESTAMP, status=?, app_count=?, user_app_count=?, system_app_count=?,
          changes_summary=?, message=?
      WHERE id=?
      """, status, appCount, userAppCount, systemAppCount, changesSummary, message, runId);
  }

  public List<Map<String,Object>> recentRuns(String identityKey, int limit) {
    int safeLimit = Math.max(1, Math.min(50, limit));
    return jdbc.queryForList("""
      SELECT id, started_at AS startedAt, finished_at AS finishedAt, status, app_count AS appCount,
             user_app_count AS userAppCount, system_app_count AS systemAppCount,
             changes_summary AS changesSummary, message
      FROM gam_discovery_app_inventory_runs WHERE identity_key=? ORDER BY started_at DESC LIMIT ?
      """, identityKey, safeLimit);
  }

  /**
   * 40k34f: EINE aggregierte Abfrage über ALLE Android-Geräte hinweg für den
   * Report-Abschnitt "App-Auswertung" - keine Einzelabfrage je Gerät, keine
   * vollständige Appliste, nur Summenwerte (siehe Performance-Vorgabe).
   */
  public Map<String,Object> globalAppSummary() {
    Map<String,Object> summary = new java.util.LinkedHashMap<>();
    List<Map<String,Object>> totals = jdbc.queryForList("""
      SELECT COUNT(*) AS totalApps, COUNT(DISTINCT identity_key) AS deviceCount,
             SUM(CASE WHEN is_system=FALSE THEN 1 ELSE 0 END) AS userApps,
             SUM(CASE WHEN is_system=TRUE THEN 1 ELSE 0 END) AS systemApps,
             SUM(CASE WHEN is_enabled=FALSE THEN 1 ELSE 0 END) AS disabledApps,
             SUM(CASE WHEN is_updated_system=TRUE THEN 1 ELSE 0 END) AS updatedSystemApps
      FROM gam_discovery_installed_apps WHERE removed_at IS NULL
      """);
    summary.putAll(totals.isEmpty() ? Map.of() : totals.get(0));
    summary.put("installerDistribution", jdbc.queryForList("""
      SELECT COALESCE(installer_package,'unbekannt') AS installer, COUNT(*) AS count
      FROM gam_discovery_installed_apps WHERE removed_at IS NULL
      GROUP BY COALESCE(installer_package,'unbekannt') ORDER BY count DESC LIMIT 15
      """));
    return summary;
  }

  public Map<String,Object> lastCompleteRun(String identityKey) {
    List<Map<String,Object>> rows = jdbc.queryForList("""
      SELECT id, started_at AS startedAt, finished_at AS finishedAt, status, app_count AS appCount,
             user_app_count AS userAppCount, system_app_count AS systemAppCount,
             changes_summary AS changesSummary, message
      FROM gam_discovery_app_inventory_runs WHERE identity_key=? AND status='VOLLSTAENDIG'
      ORDER BY started_at DESC LIMIT 1
      """, identityKey);
    return rows.isEmpty() ? null : rows.get(0);
  }
}
