package de.kopfzentrum.gam.inventory;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 40k34m: Zentrale, plattformübergreifende Inventarisierungs-Statusverwaltung.
 *
 * Vor der Einführung existierte nur eine Android-App-spezifische Lauf-Historie
 * ({@link AppInventoryRepository}, 40k34c). Für Windows/Linux/macOS/iOS gab es
 * keine vergleichbare, generische Statusverwaltung - Linux-Nachinventarisierung
 * bestand bisher lediglich aus einer Cache-Invalidierung
 * ({@code DeviceIdentityService.refreshLinuxCache()}), die erst beim nächsten
 * ohnehin stattfindenden Suchlauf wirksam wurde, kein sofortiger, gezielter Lauf.
 *
 * Diese Klasse ist bewusst als EINE generische Tabelle für ALLE Plattformen
 * angelegt (nicht als fünf einzelne Plattform-Tabellen) - "Automatisch",
 * "Windows", "Linux", "Android", "macOS", "iOS" sind einfach Werte der Spalte
 * {@code platform}, keine eigene Struktur je Plattform. Weitere Plattformen
 * lassen sich dadurch ergänzen, ohne diese Klasse oder ihr Schema zu ändern.
 */
@Repository
public class PlatformInventoryRepository {
  private final JdbcTemplate jdbc;

  public PlatformInventoryRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
    jdbc.execute("""
      CREATE TABLE IF NOT EXISTS gam_discovery_platform_inventory_runs (
        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        identity_key VARCHAR(255) NOT NULL,
        platform VARCHAR(64) NOT NULL,
        started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        finished_at TIMESTAMP NULL,
        status VARCHAR(32) NOT NULL,
        message VARCHAR(1000) NULL,
        triggered_by VARCHAR(255) NULL,
        INDEX idx_platform_runs_identity (identity_key),
        INDEX idx_platform_runs_identity_platform (identity_key, platform)
      )
      """);
  }

  public long startRun(String identityKey, String platform, String triggeredBy) {
    jdbc.update("INSERT INTO gam_discovery_platform_inventory_runs(identity_key,platform,status,triggered_by) VALUES (?,?,'LAEUFT',?)",
      identityKey, platform, triggeredBy);
    return jdbc.queryForObject("SELECT MAX(id) FROM gam_discovery_platform_inventory_runs WHERE identity_key=? AND platform=?",
      Long.class, identityKey, platform);
  }

  public void finishRun(long runId, String status, String message) {
    jdbc.update("UPDATE gam_discovery_platform_inventory_runs SET finished_at=CURRENT_TIMESTAMP, status=?, message=? WHERE id=?",
      status, message, runId);
  }

  /** Letzter (abgeschlossener oder laufender) Lauf je Plattform für dieses Gerät. */
  public List<Map<String,Object>> latestRunsByPlatform(String identityKey) {
    return jdbc.queryForList("""
      SELECT r.platform, r.started_at AS startedAt, r.finished_at AS finishedAt, r.status, r.message,
             TIMESTAMPDIFF(SECOND, r.started_at, COALESCE(r.finished_at, CURRENT_TIMESTAMP)) AS durationSeconds
      FROM gam_discovery_platform_inventory_runs r
      INNER JOIN (
        SELECT platform, MAX(id) AS maxId FROM gam_discovery_platform_inventory_runs WHERE identity_key=? GROUP BY platform
      ) latest ON latest.platform=r.platform AND latest.maxId=r.id
      WHERE r.identity_key=?
      """, identityKey, identityKey);
  }

  public List<Map<String,Object>> history(String identityKey, String platform, int limit) {
    int safeLimit = Math.max(1, Math.min(50, limit));
    return jdbc.queryForList("""
      SELECT platform, started_at AS startedAt, finished_at AS finishedAt, status, message, triggered_by AS triggeredBy
      FROM gam_discovery_platform_inventory_runs WHERE identity_key=? AND platform=? ORDER BY started_at DESC LIMIT ?
      """, identityKey, platform, safeLimit);
  }

  /** 40k34r: Läuft für dieses Gerät/diese Plattform bereits eine Inventarisierung? Verhindert Doppelläufe. */
  public boolean isRunning(String identityKey, String platform) {
    Integer count = jdbc.queryForObject(
      "SELECT COUNT(*) FROM gam_discovery_platform_inventory_runs WHERE identity_key=? AND platform=? AND status='LAEUFT'",
      Integer.class, identityKey, platform);
    return count != null && count > 0;
  }

  /**
   * 40k34r: Cooldown für die AUTOMATISCHE Nachinventarisierung - verhindert, dass
   * dasselbe Gerät bei jedem Suchlauf erneut inventarisiert wird. Gilt
   * ausdrücklich NICHT für die manuelle "Neu inventarisieren"-Aktion (siehe
   * DeviceIdentityService.runPlatformInventory() - dort nicht geprüft).
   */
  public boolean recentlyRun(String identityKey, String platform, int cooldownMinutes) {
    Integer count = jdbc.queryForObject(
      "SELECT COUNT(*) FROM gam_discovery_platform_inventory_runs WHERE identity_key=? AND platform=? AND started_at > DATE_SUB(CURRENT_TIMESTAMP, INTERVAL ? MINUTE)",
      Integer.class, identityKey, platform, cooldownMinutes);
    return count != null && count > 0;
  }
}
