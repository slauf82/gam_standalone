package de.kopfzentrum.gam.inventory;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 40k33b4: Eigenständige, kleine Ablage für die manuelle Gerätezusammenführung.
 * Bewusst getrennt von DiscoveryRegistrationRepository (welches weiterhin
 * ausschließlich die Identitätstabelle selbst sowie die eigentliche
 * mergeDevices()-Transaktion verantwortet), damit die bereits bestehende,
 * sicherheitsrelevante Identitätslogik dort so wenig wie möglich angefasst wird.
 *
 * - gam_discovery_merge_ignored: abgelehnte/ignorierte Zusammenführungsvorschläge.
 *   Ein Vorschlag wird anhand einer Signatur (Hash der erkannten Übereinstimmungs-
 *   merkmale) erneut vorgeschlagen, sobald sich die zugrunde liegenden Merkmale
 *   wesentlich ändern (Section 12).
 * - gam_discovery_merge_log: technisches Audit-Protokoll abgeschlossener
 *   Zusammenführungen (Section 11) - keine vollständige Rückgängig-Funktion,
 *   siehe Dokumentation im mitgelieferten docs/-Text zur Begründung.
 */
@Repository
public class DeviceMergeRepository {
  private final JdbcTemplate jdbc;
  public DeviceMergeRepository(JdbcTemplate jdbc){ this.jdbc = jdbc; }

  @PostConstruct void init(){
    jdbc.execute("""
      CREATE TABLE IF NOT EXISTS gam_discovery_merge_ignored (
        pair_key VARCHAR(600) NOT NULL,
        key_a VARCHAR(255) NOT NULL,
        key_b VARCHAR(255) NOT NULL,
        signature VARCHAR(64) NOT NULL,
        ignored_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        ignored_by VARCHAR(255) NULL,
        PRIMARY KEY(pair_key)
      )
      """);
    jdbc.execute("""
      CREATE TABLE IF NOT EXISTS gam_discovery_merge_log (
        id BIGINT NOT NULL AUTO_INCREMENT,
        target_identity_key VARCHAR(255) NOT NULL,
        merged_identity_keys TEXT NOT NULL,
        summary TEXT NULL,
        performed_by VARCHAR(255) NULL,
        performed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        PRIMARY KEY(id)
      )
      """);
    // 40k33b7: rein additive Erweiterung derselben Tabelle - kein zweites Merge-Log.
    // "action" unterscheidet MERGE- von SPLIT-Einträgen (Wiederauftrennung).
    // "source_snapshot" sichert die wichtigsten Felder der beim Merge gelöschten
    // Quell-Datensätze als JSON, damit eine spätere Wiederauftrennung möglichst
    // verlustfrei rekonstruieren kann, was vor der Zusammenführung bekannt war.
    addLogColumn("action", "VARCHAR(20) NOT NULL DEFAULT 'MERGE'");
    addLogColumn("source_snapshot", "MEDIUMTEXT NULL");
  }

  private void addLogColumn(String name, String definition){
    Integer count = jdbc.queryForObject("""
      SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'gam_discovery_merge_log' AND column_name = ?
      """, Integer.class, name);
    if (count != null && count == 0) {
      jdbc.execute("ALTER TABLE gam_discovery_merge_log ADD COLUMN `" + name + "` " + definition);
    }
  }

  private static String pairKey(String a, String b){
    // Symmetrisch: Reihenfolge der zwei Schluessel darf keine Rolle spielen.
    return a.compareTo(b) <= 0 ? a + "|" + b : b + "|" + a;
  }

  /** Signatur aus den erkannten Übereinstimmungsmerkmalen (sortiert, damit die Reihenfolge egal ist). */
  public static String signatureOf(List<String> matchedSignals){
    List<String> sorted = new ArrayList<>(matchedSignals == null ? List.of() : matchedSignals);
    java.util.Collections.sort(sorted);
    String joined = String.join("|", sorted);
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] digest = md.digest(joined.getBytes(StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder();
      for (byte b : digest) sb.append(String.format("%02x", b));
      return sb.toString();
    } catch (Exception e) {
      return Integer.toHexString(joined.hashCode());
    }
  }

  public boolean isIgnored(String keyA, String keyB, String currentSignature){
    List<String> rows = jdbc.query("SELECT signature FROM gam_discovery_merge_ignored WHERE pair_key=?",
      (rs, n) -> rs.getString(1), pairKey(keyA, keyB));
    if (rows.isEmpty()) return false;
    // Section 12: Aendern sich die Merkmale wesentlich (andere Signatur), gilt der
    // Vorschlag nicht mehr als abgelehnt und darf erneut erscheinen.
    return rows.get(0).equals(currentSignature);
  }

  public void ignore(String keyA, String keyB, String signature, String actor){
    jdbc.update("""
      INSERT INTO gam_discovery_merge_ignored(pair_key,key_a,key_b,signature,ignored_by)
      VALUES (?,?,?,?,?)
      ON DUPLICATE KEY UPDATE signature=VALUES(signature), ignored_at=CURRENT_TIMESTAMP, ignored_by=VALUES(ignored_by)
      """, pairKey(keyA, keyB), keyA, keyB, signature, actor);
  }

  public void logMerge(String targetKey, List<String> mergedKeys, String summary, String sourceSnapshotJson, String actor){
    jdbc.update("""
      INSERT INTO gam_discovery_merge_log(target_identity_key,merged_identity_keys,summary,action,source_snapshot,performed_by)
      VALUES (?,?,?,'MERGE',?,?)
      """, targetKey, String.join(", ", mergedKeys), summary, sourceSnapshotJson, actor);
  }

  /** 40k33b7: Protokolliert eine manuelle Wiederauftrennung im selben Audit-Log. */
  public void logSplit(String originIdentityKey, String newIdentityKey, String summary, String actor){
    jdbc.update("""
      INSERT INTO gam_discovery_merge_log(target_identity_key,merged_identity_keys,summary,action,performed_by)
      VALUES (?,?,?,'SPLIT',?)
      """, originIdentityKey, newIdentityKey, summary, actor);
  }

  /**
   * 40k33b7: Liefert die gesicherten Quell-Schnappschüsse (JSON) aller Zusammen-
   * führungen, bei denen diese Identität das Zielgerät war - Grundlage für die
   * Wiederauftrennung. Ältere, vor 40k33b7 durchgeführte Zusammenführungen haben
   * keinen Schnappschuss (source_snapshot ist dann NULL) und werden übersprungen.
   */
  public List<String> sourceSnapshotsForTarget(String identityKey){
    return jdbc.query("""
      SELECT source_snapshot FROM gam_discovery_merge_log
      WHERE target_identity_key=? AND action='MERGE' AND source_snapshot IS NOT NULL
      ORDER BY performed_at ASC
      """, (rs,row)->rs.getString(1), identityKey);
  }

  public List<Map<String,Object>> recentLog(int limit){
    int safeLimit = Math.max(1, Math.min(500, limit));
    return jdbc.queryForList("""
      SELECT id, target_identity_key AS targetIdentityKey, merged_identity_keys AS mergedIdentityKeys,
             summary, action, performed_by AS performedBy, performed_at AS performedAt
      FROM gam_discovery_merge_log ORDER BY performed_at DESC LIMIT ?
      """, safeLimit);
  }

  /**
   * 40k33b5: Anzahl der Zusammenführungen, bei denen diese Identität das Zielgerät war.
   * Wiederverwendung des bestehenden Merge-Audit-Logs aus 40k33b4 - keine neue Zählung.
   */
  public int mergeCountForTarget(String identityKey){
    Integer count = jdbc.queryForObject(
      "SELECT COUNT(*) FROM gam_discovery_merge_log WHERE target_identity_key=?", Integer.class, identityKey);
    return count == null ? 0 : count;
  }

  /**
   * 40k33b5: Alle Merge-Log-Einträge, die diese Identität betreffen - entweder als
   * Zielgerät oder als (inzwischen gelöschter) Quell-Datensatz einer früheren
   * Zusammenführung. Grundlage für den neuen Identitätsverlauf.
   */
  public List<Map<String,Object>> logEntriesTouching(String identityKey){
    return jdbc.queryForList("""
      SELECT id, target_identity_key AS targetIdentityKey, merged_identity_keys AS mergedIdentityKeys,
             summary, action, performed_by AS performedBy, performed_at AS performedAt
      FROM gam_discovery_merge_log
      WHERE target_identity_key=? OR merged_identity_keys LIKE ?
      ORDER BY performed_at DESC
      """, identityKey, "%" + identityKey + "%");
  }
}
