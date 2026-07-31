package de.kopfzentrum.gam.inventory;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 40k34t: Verbindet den (bereits vor der gesamten Discovery-/Android-Architektur
 * bestehenden) Gerätebestand ({@code InventoryDevice}, Tabellen {@code geräte}/
 * {@code geräte_neu}) mit der kanonischen Discovery-Geräteidentität
 * ({@code identityKey}, {@link DiscoveryRegistrationRepository}).
 *
 * Diese Klasse erzeugt AUSDRÜCKLICH KEINE zweite Geräteidentität - sie enthält
 * ausschließlich die Verbindung (Fremdschlüssel-ähnliche Zuordnung) zwischen
 * einem bereits bestehenden Inventar-Datensatz und einem bereits bestehenden
 * Identitätsschlüssel. Beide Seiten bleiben unverändert in ihren jeweils
 * eigenen, bereits bestehenden Tabellen.
 *
 * Zwei Wege, wie eine Verknüpfung entsteht:
 * 1. VERLÄSSLICH, beim Verschieben eines Geräts von "Registrierte Geräte" in
 *    den Gerätebestand ({@code InventoryWriteController.moveRegisteredToInventory()}):
 *    die Verknüpfung wird SOFORT hergestellt, BEVOR die ursprüngliche Discovery-
 *    Identität gelöscht wird - zu diesem Zeitpunkt ist die Zuordnung zu 100%
 *    eindeutig, keine Ratelogik nötig.
 * 2. BEST-EFFORT-Nachverknüpfung ({@link #linkExistingUnlinkedDevices()}) für
 *    bereits VOR 40k34t verschobene Bestandsgeräte, deren ursprüngliche
 *    Discovery-Identität zum Zeitpunkt des Verschiebens bereits gelöscht wurde
 *    (das war das bisherige Verhalten) - hier kann nur noch anhand einer
 *    aktuell noch existierenden, übereinstimmenden Discovery-Identität
 *    (Seriennummer, danach IP+Name) nachverknüpft werden. Eindeutigkeit hat
 *    Vorrang: mehrdeutige Treffer werden NICHT verknüpft, sondern als
 *    mehrdeutig protokolliert.
 */
@Repository
public class InventoryIdentityLinkRepository {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(InventoryIdentityLinkRepository.class);
  private final JdbcTemplate jdbc;

  public InventoryIdentityLinkRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
    jdbc.execute("""
      CREATE TABLE IF NOT EXISTS gam_inventory_identity_links (
        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        source VARCHAR(32) NOT NULL,
        inventory_device_id INT NOT NULL,
        identity_key VARCHAR(255) NOT NULL,
        matched_by VARCHAR(64) NOT NULL,
        matched_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        ambiguous BOOLEAN NOT NULL DEFAULT FALSE,
        UNIQUE KEY uq_gam_inventory_identity_link (source, inventory_device_id)
      )
      """);
  }

  public Optional<String> findIdentityKey(String source, int deviceId) {
    List<String> rows = jdbc.query(
      "SELECT identity_key FROM gam_inventory_identity_links WHERE source=? AND inventory_device_id=? AND ambiguous=FALSE",
      (rs, n) -> rs.getString(1), source, deviceId);
    return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
  }

  /**
   * Stellt eine eindeutige Verknüpfung her oder aktualisiert eine bestehende.
   * Wird insbesondere direkt beim Verschieben eines Geräts in den Gerätebestand
   * aufgerufen - zu diesem Zeitpunkt ist die Zuordnung sicher, keine Mehrdeutigkeit
   * möglich.
   */
  public void link(String source, int deviceId, String identityKey, String matchedBy) {
    int updated = jdbc.update(
      "UPDATE gam_inventory_identity_links SET identity_key=?, matched_by=?, matched_at=CURRENT_TIMESTAMP, ambiguous=FALSE WHERE source=? AND inventory_device_id=?",
      identityKey, matchedBy, source, deviceId);
    if (updated == 0) {
      jdbc.update("INSERT INTO gam_inventory_identity_links(source,inventory_device_id,identity_key,matched_by) VALUES (?,?,?,?)",
        source, deviceId, identityKey, matchedBy);
    }
    log.info("[INVENTORY-LINK] Verknüpfung hergestellt: source={} deviceId={} identityKey={} (Merkmal: {})", source, deviceId, identityKey, matchedBy);
  }

  private void markAmbiguous(String source, int deviceId, String reason) {
    log.info("[INVENTORY-LINK] Mehrdeutige Zuordnung übersprungen: source={} deviceId={} Grund={}", source, deviceId, reason);
  }

  /**
   * 40k34t: Best-Effort-Nachverknüpfung für Bestandsgeräte, die bereits VOR
   * diesem Schritt in den Gerätebestand verschoben wurden (ihre ursprüngliche
   * Discovery-Identität wurde beim Verschieben bisher immer gelöscht - siehe
   * Dokumentation). Kann nur Geräte verknüpfen, für die AKTUELL noch eine
   * übereinstimmende, eindeutige Discovery-Identität existiert (z.B. weil ein
   * späterer Suchlauf dasselbe physische Gerät erneut gefunden und registriert
   * hat). Reine lokale Datenbankabfragen, kein Remote-Aufruf.
   *
   * Zuordnungsreihenfolge (nur bei tatsächlich vorhandenen, stabilen Merkmalen):
   * 1. Seriennummer (exakt, eindeutig)
   * 2. IP-Adresse UND Name gemeinsam (exakt, eindeutig) - IP allein reicht
   *    nicht, wie im Auftrag gefordert.
   * Name allein wird NIE als Zuordnungsmerkmal verwendet.
   */
  public LinkSummary linkExistingUnlinkedDevices(List<InventoryDevice> inventoryDevices) {
    int linked = 0, ambiguous = 0, unresolved = 0, alreadyLinked = 0;
    for (InventoryDevice device : inventoryDevices) {
      String source = device.source();
      int id = device.id();
      if (findIdentityKey(source, id).isPresent()) { alreadyLinked++; continue; }
      String serial = clean(device.serialNumber());
      String ip = clean(device.ip());
      String name = clean(device.name());

      if (serial != null) {
        List<String> bySerial = jdbc.query("SELECT identity_key FROM gam_discovery_registered_devices WHERE serial_number=?",
          (rs, n) -> rs.getString(1), serial);
        if (bySerial.size() == 1) { link(source, id, bySerial.get(0), "Seriennummer"); linked++; continue; }
        if (bySerial.size() > 1) { markAmbiguous(source, id, "mehrere Discovery-Identitäten mit derselben Seriennummer"); ambiguous++; continue; }
      }
      if (ip != null && name != null) {
        List<String> byIpAndName = jdbc.query(
          "SELECT identity_key FROM gam_discovery_registered_devices WHERE address=? AND name=?",
          (rs, n) -> rs.getString(1), ip, name);
        if (byIpAndName.size() == 1) { link(source, id, byIpAndName.get(0), "IP-Adresse + Name"); linked++; continue; }
        if (byIpAndName.size() > 1) { markAmbiguous(source, id, "mehrere Discovery-Identitäten mit derselben IP-Adresse und demselben Namen"); ambiguous++; continue; }
      }
      unresolved++;
      log.debug("[INVENTORY-LINK] Keine Zuordnung möglich: source={} deviceId={} (keine übereinstimmende Seriennummer, keine übereinstimmende IP+Name-Kombination)", source, id);
    }
    log.info("[INVENTORY-LINK] Nachverknüpfung abgeschlossen: {} neu verknüpft, {} bereits verknüpft, {} mehrdeutig übersprungen, {} ungeklärt",
      linked, alreadyLinked, ambiguous, unresolved);
    return new LinkSummary(linked, alreadyLinked, ambiguous, unresolved);
  }

  public record LinkSummary(int linked, int alreadyLinked, int ambiguous, int unresolved) {}

  private static String clean(String v) { return v == null || v.isBlank() ? null : v.trim(); }
}
