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
    int orphaned = recoverOrphanedRunningRuns();
    if (orphaned > 0) {
      System.out.println("[PLATFORM-INVENTORY] " + orphaned + " verwaiste LAEUFT-Inventarisierung(en) beim Backend-Start als ABGEBROCHEN markiert.");
    }
  }

  /**
   * 40k36q22: Nach einem Backend-Neustart existieren keine Worker-Threads aus dem
   * vorherigen Prozess mehr. Alle zu diesem Zeitpunkt noch als LAEUFT gespeicherten
   * Läufe sind daher verwaist und werden als sauber historisierter Abbruch markiert.
   */
  public int recoverOrphanedRunningRuns() {
    return jdbc.update("""
      UPDATE gam_discovery_platform_inventory_runs
      SET finished_at=CURRENT_TIMESTAMP,
          status='ABGEBROCHEN',
          message=COALESCE(NULLIF(message,''),'Inventarisierung durch Backend-Neustart oder unerwartetes Ende unterbrochen.')
      WHERE status='LAEUFT'
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

  /** 40k36t5: Falls eine verifizierte Inventarisierung die kanonische Identität
   * korrigiert, folgt auch der bereits gestartete Plattformlauf dieser Identität. */
  public void moveRunToIdentity(long runId, String identityKey) {
    if (identityKey == null || identityKey.isBlank()) return;
    jdbc.update("UPDATE gam_discovery_platform_inventory_runs SET identity_key=? WHERE id=?", identityKey, runId);
  }

  public boolean isCancelled(long runId) {
    String status=jdbc.query("SELECT status FROM gam_discovery_platform_inventory_runs WHERE id=?",
      rs->rs.next()?rs.getString(1):null,runId);
    return "ABGEBROCHEN".equalsIgnoreCase(status);
  }


  /** fix43: Nur wirklich erfolgreich abgeschlossene Plattform-Inventarisierungen
   * zaehlen als Voraussetzung fuer AUTOMATISCHE Merge-Vorschlaege.
   * Discovery, Ping, Routerfunde oder fehlgeschlagene/lauefende Laeufe genuegen nicht.
   */
  public java.util.Set<String> successfullyInventoriedIdentityKeys() {
    return new java.util.HashSet<>(jdbc.queryForList("""
      SELECT DISTINCT identity_key FROM gam_discovery_platform_inventory_runs
      WHERE finished_at IS NOT NULL AND status='ERFOLGREICH'
      """, String.class));
  }

  /** Letzter (abgeschlossener oder laufender) Lauf je Plattform für dieses Gerät. */
  public List<Map<String,Object>> latestRunsByPlatform(String identityKey) {
    return jdbc.queryForList("""
      SELECT r.platform, r.started_at AS startedAt, r.finished_at AS finishedAt, r.status, r.message,
             r.triggered_by AS triggeredBy,
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

  /** 40k36q18: markiert laufende Inventarisierungen als abgebrochen. */
  public int cancelRunning(String identityKey, String platform, String message) {
    if (identityKey == null || identityKey.isBlank()) return 0;
    if (platform == null || platform.isBlank()) {
      return jdbc.update("""
        UPDATE gam_discovery_platform_inventory_runs
        SET finished_at=CURRENT_TIMESTAMP,status='ABGEBROCHEN',message=?
        WHERE identity_key=? AND status='LAEUFT'
        """, message, identityKey);
    }
    return jdbc.update("""
      UPDATE gam_discovery_platform_inventory_runs
      SET finished_at=CURRENT_TIMESTAMP,status='ABGEBROCHEN',message=?
      WHERE identity_key=? AND platform=? AND status='LAEUFT'
      """, message, identityKey, platform);
  }

  public int cancelAllRunning(String message) {
    return jdbc.update("""
      UPDATE gam_discovery_platform_inventory_runs
      SET finished_at=CURRENT_TIMESTAMP,status='ABGEBROCHEN',message=?
      WHERE status='LAEUFT'
      """, message);
  }

  /** 40k36q10: Globaler Status aller Plattforminventarisierungen für die überall sichtbare Statusleiste. */
  public Map<String,Object> globalStatus() {
    // 40k36q22 Sicherheitsnetz: Ein extrem alter LAEUFT-Datensatz darf die globale
    // Statusleiste nicht unbegrenzt aktiv halten. Normale lange Läufe bleiben möglich;
    // erst nach 12 Stunden wird ein offensichtlicher Altzustand bereinigt.
    jdbc.update("""
      UPDATE gam_discovery_platform_inventory_runs
      SET finished_at=CURRENT_TIMESTAMP,status='ABGEBROCHEN',
          message='Inventarisierung wegen veraltetem Laufstatus automatisch als abgebrochen markiert.'
      WHERE status='LAEUFT' AND started_at < TIMESTAMPADD(HOUR,-12,CURRENT_TIMESTAMP)
      """);
    Map<String,Object> result = new java.util.LinkedHashMap<>();
    List<Map<String,Object>> active = jdbc.queryForList("""
      SELECT r.id, r.identity_key AS identityKey, r.platform, r.started_at AS startedAt,
             r.triggered_by AS triggeredBy, COALESCE(d.name,r.identity_key) AS deviceName,
             TIMESTAMPDIFF(SECOND,r.started_at,CURRENT_TIMESTAMP) AS durationSeconds
      FROM gam_discovery_platform_inventory_runs r
      LEFT JOIN gam_discovery_registered_devices d ON d.identity_key=r.identity_key
      WHERE r.status='LAEUFT'
      ORDER BY r.started_at ASC
      """);
    result.put("active", !active.isEmpty());
    result.put("activeCount", active.size());
    result.put("activeRuns", active);
    if (!active.isEmpty()) {
      String trigger = String.valueOf(active.get(0).getOrDefault("triggeredBy", ""));
      result.put("triggeredBy", trigger);
      result.put("origin", originLabel(trigger));
      result.put("startedAt", active.get(0).get("startedAt"));
      Integer completedRecent = jdbc.queryForObject("""
        SELECT COUNT(*) FROM gam_discovery_platform_inventory_runs
        WHERE status<>'LAEUFT' AND COALESCE(triggered_by,'')=COALESCE(?, '')
          AND started_at >= TIMESTAMPADD(MINUTE,-15,CURRENT_TIMESTAMP)
        """, Integer.class, trigger);
      result.put("completedRecent", completedRecent == null ? 0 : completedRecent);
    } else {
      List<Map<String,Object>> latest = jdbc.queryForList("""
        SELECT r.platform, r.finished_at AS finishedAt, r.status, r.message, r.triggered_by AS triggeredBy,
               COALESCE(d.name,r.identity_key) AS deviceName
        FROM gam_discovery_platform_inventory_runs r
        LEFT JOIN gam_discovery_registered_devices d ON d.identity_key=r.identity_key
        WHERE r.finished_at IS NOT NULL ORDER BY r.finished_at DESC LIMIT 1
        """);
      if (!latest.isEmpty()) {
        Map<String,Object> l=latest.get(0);
        result.put("lastFinishedAt", l.get("finishedAt"));
        result.put("lastStatus", l.get("status"));
        result.put("lastMessage", l.get("message"));
        result.put("lastDeviceName", l.get("deviceName"));
        result.put("origin", originLabel(String.valueOf(l.getOrDefault("triggeredBy", ""))));
      }
    }
    return result;
  }

  private static String originLabel(String trigger) {
    String t=trigger==null?"":trigger.toLowerCase();
    if(t.contains("scheduler")) return "Automatischer Scheduler-Lauf";
    if(t.contains("start")||t.contains("startup")) return "Startinventarisierung";
    if(t.contains("discovery")) return "Discovery-Inventarisierung";
    if(t.contains("automatisch")) return "Automatische Inventarisierung";
    if(t.isBlank()) return "Inventarisierung";
    return "Manuelle Inventarisierung";
  }

}
