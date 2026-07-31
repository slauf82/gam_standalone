package de.kopfzentrum.gam.inventory;

import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 40k33b4: Ermittelt Zusammenführungskandidaten unter bereits registrierten
 * Discovery-Geräten und orchestriert Vorschau, Bestätigung und Ablehnung.
 *
 * Wichtig: Für die Kandidatenbewertung wird bewusst dieselbe, bereits
 * vorhandene DeviceIdentityConfidenceEngine verwendet, die auch während eines
 * laufenden Suchlaufs live entscheidet, ob zwei Treffer automatisch
 * zusammengeführt werden. Es gibt also KEINE zweite, parallele Bewertungslogik -
 * nur eine zusätzliche Ansicht auf bereits persistierte, historische Datensätze
 * (die z.B. vor dem 40k33b4-Fix der IP-Merge-Brücke als zwei getrennte
 * Datensätze gespeichert wurden).
 */
@Service
public class DeviceMergeService {
  private static final Logger log = LoggerFactory.getLogger(DeviceMergeService.class);
  /** Für sehr große Bestände wird die paarweise Prüfung begrenzt (siehe docs/ - bekannte Einschränkung). */
  private static final int MAX_CANDIDATE_ROWS = 500;

  private final DiscoveryRegistrationRepository registrations;
  private final DeviceMergeRepository merges;
  private final DeviceIdentityMergeSettingsRepository mergeSettings;
  private final AppInventoryRepository appInventory;
  private final ObjectMapper objectMapper;
  private final DeviceIdentityConfidenceEngine identityEngine = new DeviceIdentityConfidenceEngine();

  public DeviceMergeService(DiscoveryRegistrationRepository registrations, DeviceMergeRepository merges,
                            DeviceIdentityMergeSettingsRepository mergeSettings, AppInventoryRepository appInventory,
                            ObjectMapper objectMapper) {
    this.registrations = registrations;
    this.merges = merges;
    this.mergeSettings = mergeSettings;
    this.appInventory = appInventory;
    this.objectMapper = objectMapper;
  }

  public record Candidate(String keyA, String nameA, String typeA, String addressA, String macA,
                          String keyB, String nameB, String typeB, String addressB, String macB,
                          int score, int confidencePercent, String decision, String warningLevel,
                          List<String> matchedSignals, List<String> conflicts, String criticalityLevel,
                          String criticalMessage, List<DiscoveryRegistrationRepository.FieldComparisonRow> fieldComparison,
                          boolean manualConfirmationRequired) {}

  public record MergePreviewResult(String targetKey, List<String> sourceKeys, String name, boolean manualName,
                                   String deviceType, boolean manualDeviceType, String address, String mac,
                                   String serialNumber, String manufacturer, String protocol, String status,
                                   long detectionCount, int lastScanHits, List<String> macConflicts,
                                   boolean macConflictConfirmationRequired, String criticalityLevel,
                                   String criticalMessage, List<DiscoveryRegistrationRepository.FieldComparisonRow> fieldComparison,
                                   boolean manualConfirmationRequired) {}

  public List<Candidate> findCandidates() {
    List<Map<String,Object>> rows = registrations.findAll();
    boolean truncated = rows.size() > MAX_CANDIDATE_ROWS;
    List<Map<String,Object>> scoped = truncated ? rows.subList(0, MAX_CANDIDATE_ROWS) : rows;
    var settings = mergeSettings.load();

    List<Candidate> result = new ArrayList<>();
    for (int i = 0; i < scoped.size(); i++) {
      for (int j = i + 1; j < scoped.size(); j++) {
        Map<String,Object> a = scoped.get(i), b = scoped.get(j);
        String keyA = String.valueOf(a.get("identityKey")), keyB = String.valueOf(b.get("identityKey"));
        DiscoveredDevice da = toDiscoveredDevice(a), db = toDiscoveredDevice(b);
        var assessment = identityEngine.assess(da, db, settings);
        if (assessment.decision() == DeviceIdentityConfidenceEngine.Decision.DISTINCT) continue;
        String signature = DeviceMergeRepository.signatureOf(assessment.matchedSignals());
        if (merges.isIgnored(keyA, keyB, signature)) continue;
        // 40k33b10: Schutzstufe und tabellarische Gegenüberstellung - reine Einordnung/Aufbereitung
        // der bereits von der Engine gelieferten Konflikte, keine zweite Bewertung.
        List<String> conflicts = assessment.conflicts();
        String criticality = DiscoveryRegistrationRepository.highestConflictSeverity(conflicts);
        String criticalMessage = DiscoveryRegistrationRepository.requiresManualConfirmation(criticality) ? DiscoveryRegistrationRepository.criticalConflictMessage(conflicts) : null;
        List<DiscoveryRegistrationRepository.FieldComparisonRow> comparison =
          DiscoveryRegistrationRepository.requiresManualConfirmation(criticality) ? DiscoveryRegistrationRepository.compareIdentityFields(a, b) : List.of();
        if (criticality != null) {
          log.debug("Merge-Kandidat {} <-> {}: Schutzstufe {} ausgelöst durch {} (Score {}, Konfidenz {}%)",
            keyA, keyB, criticality, conflicts, assessment.score(), assessment.confidencePercent());
        }
        result.add(new Candidate(keyA, str(a.get("name")), str(a.get("deviceType")), str(a.get("address")), str(a.get("hardwareAddress")),
          keyB, str(b.get("name")), str(b.get("deviceType")), str(b.get("address")), str(b.get("hardwareAddress")),
          assessment.score(), assessment.confidencePercent(), assessment.decision().name(),
          warningLevel(assessment), assessment.matchedSignals(), conflicts, criticality, criticalMessage, comparison,
          DiscoveryRegistrationRepository.requiresManualConfirmation(criticality)));
      }
    }
    result.sort(Comparator.comparingInt(Candidate::score).reversed());
    return result;
  }


  public MergePreviewResult preview(String targetKey, List<String> sourceKeys, Map<String,Object> overrides) {
    var plan = registrations.planMerge(targetKey, sourceKeys, overrides);
    boolean confirmed = overrides != null && Boolean.TRUE.equals(overrides.get("confirmMacConflict"));

    // 40k33b10: dieselbe Konflikterkennung wie bei der Kandidatenermittlung, hier zusätzlich
    // für die freie Geräteauswahl angewendet (die nicht zwangsläufig über einen bereits
    // gelisteten Kandidaten läuft). Keine zweite Bewertungslogik - derselbe Aufruf derselben Engine.
    var settings = mergeSettings.load();
    List<String> allConflicts = new ArrayList<>();
    List<DiscoveryRegistrationRepository.FieldComparisonRow> comparison = new ArrayList<>();
    for (Map<String,Object> sourceRow : plan.sourceRows()) {
      var assessment = identityEngine.assess(toDiscoveredDevice(plan.targetRow()), toDiscoveredDevice(sourceRow), settings);
      allConflicts.addAll(assessment.conflicts());
      if (!assessment.conflicts().isEmpty()) comparison.addAll(DiscoveryRegistrationRepository.compareIdentityFields(plan.targetRow(), sourceRow));
    }
    String criticality = DiscoveryRegistrationRepository.highestConflictSeverity(allConflicts);
    String criticalMessage = DiscoveryRegistrationRepository.requiresManualConfirmation(criticality) ? DiscoveryRegistrationRepository.criticalConflictMessage(allConflicts) : null;
    if (criticality != null) log.debug("Merge-Vorschau {} <- {}: Schutzstufe {} ausgelöst durch {}", targetKey, sourceKeys, criticality, allConflicts);

    return new MergePreviewResult(plan.targetKey(), plan.sourceKeys(), plan.name(), plan.manualName(),
      plan.deviceType(), plan.manualDeviceType(), plan.address(), plan.mac(), plan.serialNumber(),
      plan.manufacturer(), plan.protocol(), plan.status(), plan.detectionCount(), plan.lastScanHits(),
      plan.knownMacs(), plan.knownMacs().size() > 1 && !confirmed, criticality, criticalMessage, comparison,
      plan.knownMacs().size() > 1 || DiscoveryRegistrationRepository.requiresManualConfirmation(criticality));
  }

  public Map<String,Object> confirm(String targetKey, List<String> sourceKeys, Map<String,Object> overrides, String actor) {
    var plan = registrations.planMerge(targetKey, sourceKeys, overrides);
    // planMerge() wirft bereits bei ungeloestem MAC-Konflikt; hier nur noch die eigentliche Schreibung.
    boolean acknowledgedCritical = overrides != null && (Boolean.TRUE.equals(overrides.get("confirmMacConflict"))
      || Boolean.TRUE.equals(overrides.get("confirmCriticalConflict")));
    log.debug("Zusammenführung von {} in {} wird bestätigt (kritischer Konflikt bewusst bestätigt: {})",
      sourceKeys, targetKey, acknowledgedCritical);
    String snapshotJson = snapshotOf(plan.sourceRows());
    Map<String,Object> merged = registrations.mergeDevices(targetKey, sourceKeys, overrides, actor);
    // 40k34e: App-Inventarläufe und aktuelle Paketdatensätze (40k34c) sind kein
    // Identitätsmerkmal, sollen aber beim Merge nicht verloren gehen - Übernahme in
    // dieselbe, bereits bestehende Merge-Bestätigung, kein separater Android-Merge-Pfad.
    for (String sourceKey : sourceKeys) appInventory.reassignToTarget(sourceKey, targetKey);
    merges.logMerge(targetKey, sourceKeys,
      "Zusammengeführt: " + plan.sourceRows().size() + " Quelle(n) in " + targetKey
        + " · Name: " + plan.name() + " · Kategorie: " + plan.deviceType()
        + (acknowledgedCritical ? " · kritischer Konflikt vom Benutzer bewusst bestätigt" : ""),
      snapshotJson, actor);
    log.debug("Zusammenführung von {} in {} abgeschlossen", sourceKeys, targetKey);
    return merged;
  }

  /**
   * 40k33b7: Sichert die wichtigsten Felder der (gleich zu löschenden) Quell-
   * Datensätze als JSON, damit eine spätere Wiederauftrennung möglichst
   * verlustfrei rekonstruieren kann, was vor der Zusammenführung bekannt war.
   */
  private String snapshotOf(List<Map<String,Object>> sourceRows) {
    List<Map<String,Object>> snapshot = new ArrayList<>();
    for (Map<String,Object> row : sourceRows) {
      Map<String,Object> entry = new LinkedHashMap<>();
      for (String field : List.of("identityKey","name","deviceType","address","hardwareAddress","serialNumber","manufacturer","protocol","status")) {
        entry.put(field, row.get(field));
      }
      snapshot.add(entry);
    }
    try { return objectMapper.writeValueAsString(snapshot); }
    catch (Exception e) { return null; }
  }

  public void ignore(String keyA, String keyB, String actor) {
    Map<String,Object> a = registrations.find(keyA), b = registrations.find(keyB);
    DiscoveredDevice da = toDiscoveredDevice(a), db = toDiscoveredDevice(b);
    var assessment = identityEngine.assess(da, db, mergeSettings.load());
    merges.ignore(keyA, keyB, DeviceMergeRepository.signatureOf(assessment.matchedSignals()), actor);
  }

  public List<Map<String,Object>> auditLog(int limit) {
    return merges.recentLog(limit);
  }

  /**
   * 40k33b6a: Reine Anzeige-Klassifikation ("Warnstufe") aus der bereits
   * vorhandenen Entscheidung/Konfliktliste - ändert nichts an Schwellenwerten
   * oder daran, ob automatisch zusammengeführt werden darf.
   * 🔴 Hoher Konflikt: es liegt mindestens ein harter Konflikt vor (z.B.
   *    abweichende MAC/Seriennummer/IP bei gleichzeitig erreichbaren Geräten) -
   *    die Discovery führt solche Kandidaten nie automatisch zusammen.
   * 🟢 Sehr sicher: automatische Zusammenführung würde greifen, keine Konflikte.
   * 🟡 Bitte prüfen: möglicher Kandidat ohne harten Konflikt, aber (noch) nicht
   *    sicher genug für eine automatische Zusammenführung.
   */
  private static String warningLevel(DeviceIdentityConfidenceEngine.Assessment assessment) {
    if (!assessment.conflicts().isEmpty()) return "RED";
    if (assessment.decision() == DeviceIdentityConfidenceEngine.Decision.AUTO_MERGE) return "GREEN";
    return "YELLOW";
  }

  private static DiscoveredDevice toDiscoveredDevice(Map<String,Object> row) {
    return new DiscoveredDevice(
      str(row.get("identityKey")), str(row.get("name")), str(row.get("deviceType")), str(row.get("address")),
      str(row.get("hardwareAddress")), str(row.get("protocol")), str(row.get("status")),
      str(row.get("manufacturer")), str(row.get("serialNumber")), str(row.get("lastSeenAt")), true);
  }

  private static String str(Object value) { return value == null ? null : String.valueOf(value); }
}
