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
  // 40k37e55e: ausfuehrliche Kandidatenbewertung getrennt vom normalen Backend-Log.
  private static final Logger mergeDebug = LoggerFactory.getLogger("GAM_MERGE_DIAGNOSTICS");
  private static final Map<String,Long> MERGE_LOG_DEDUP = new java.util.concurrent.ConcurrentHashMap<>();
  private static final long MERGE_LOG_DEDUP_MS = 30_000L;
  private static boolean shouldLogMergeDiagnostic(String signature) {
    long now=System.currentTimeMillis();
    Long previous=MERGE_LOG_DEDUP.put(signature,now);
    if(MERGE_LOG_DEDUP.size()>4096) MERGE_LOG_DEDUP.entrySet().removeIf(e -> now-e.getValue()>MERGE_LOG_DEDUP_MS);
    return previous==null || now-previous>MERGE_LOG_DEDUP_MS;
  }
  /** Für sehr große Bestände wird die paarweise Prüfung begrenzt (siehe docs/ - bekannte Einschränkung). */
  private static final int MAX_CANDIDATE_ROWS = 500;

  private final DiscoveryRegistrationRepository registrations;
  private final DeviceMergeRepository merges;
  private final DeviceIdentityMergeSettingsRepository mergeSettings;
  private final AppInventoryRepository appInventory;
  private final PlatformInventoryRepository platformInventory;
  private final ObjectMapper objectMapper;
  private final DeviceIdentityConfidenceEngine identityEngine = new DeviceIdentityConfidenceEngine();

  public DeviceMergeService(DiscoveryRegistrationRepository registrations, DeviceMergeRepository merges,
                            DeviceIdentityMergeSettingsRepository mergeSettings, AppInventoryRepository appInventory,
                            PlatformInventoryRepository platformInventory, ObjectMapper objectMapper) {
    this.registrations = registrations;
    this.merges = merges;
    this.mergeSettings = mergeSettings;
    this.appInventory = appInventory;
    this.platformInventory = platformInventory;
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
    Set<String> inventoried = platformInventory.successfullyInventoriedIdentityKeys();

    List<Candidate> result = new ArrayList<>();
    for (int i = 0; i < scoped.size(); i++) {
      for (int j = i + 1; j < scoped.size(); j++) {
        Map<String,Object> a = scoped.get(i), b = scoped.get(j);
        String keyA = String.valueOf(a.get("identityKey")), keyB = String.valueOf(b.get("identityKey"));
        // fix43: Nur ZWEI erfolgreich inventarisierte Geraete duerfen in
        // automatische Merge-Vorschlaege gelangen; reine Router-/ARP-Treffer nie.
        if (!inventoried.contains(keyA) || !inventoried.contains(keyB)) continue;
        DiscoveredDevice da = toDiscoveredDevice(a), db = toDiscoveredDevice(b);
        if (incompatibleRegisteredPlatforms(a,b)) continue;
        // Namen, Kategorie, IP und gemeinsame Quellen sind KEIN Identitaetsbeweis.
        if (!hasMatchingStrongIdentity(a,b)) continue;
        var assessment = identityEngine.assess(da, db, settings);
        if (assessment.decision() == DeviceIdentityConfidenceEngine.Decision.DISTINCT) continue;
        // Harte Widersprueche duerfen nicht als angeblich plausible Kandidaten erscheinen.
        if (assessment.conflicts().stream().anyMatch(DeviceMergeService::isHardIdentityConflict)) continue;
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
          String diagSignature="candidate|"+keyA+"|"+keyB+"|"+criticality+"|"+conflicts+"|"+assessment.score()+"|"+assessment.confidencePercent();
          if(shouldLogMergeDiagnostic(diagSignature)) mergeDebug.debug("Merge-Kandidat {} <-> {}: Schutzstufe {} ausgelöst durch {} (Score {}, Konfidenz {}%)",
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


  /** fix43: Nur belastbare, exakt uebereinstimmende Identitaetsmerkmale.
   * MAC alleine gilt hier nur bei zwei inventarisierten Datensaetzen.
   * UUIDs koennen unterschiedliche Netzwerkadapter desselben Hosts verbinden.
   */
  static boolean hasMatchingStrongIdentity(Map<String,Object> a, Map<String,Object> b) {
    if(a==null || b==null) return false;
    String aSerial=strongValue(a.get("serialNumber")), bSerial=strongValue(b.get("serialNumber"));
    if(equalPresent(aSerial,bSerial)) return true;
    String aMac=macValue(a.get("hardwareAddress")), bMac=macValue(b.get("hardwareAddress"));
    if(equalPresent(aMac,bMac)) return true;
    Map<String,String> ad=DiscoveryRegistrationRepository.detailValues(str(a.get("protocol")));
    Map<String,String> bd=DiscoveryRegistrationRepository.detailValues(str(b.get("protocol")));
    for(String key:List.of("Hardware-UUID","System-UUID","Provisioning-UDID","SSH-Hostkey","Build-Fingerprint")) {
      if(equalPresent(strongValue(ad.get(key)),strongValue(bd.get(key)))) return true;
    }
    return false;
  }
  private static String strongValue(Object o) {
    if(o==null) return "";
    String value=String.valueOf(o).trim().toLowerCase(Locale.ROOT);
    if(value.isBlank() || Set.of("unknown","unbekannt","none","null","n/a","-","00000000-0000-0000-0000-000000000000").contains(value)) return "";
    return value;
  }
  private static String macValue(Object o) {
    String v=strongValue(o).replace(":","").replace("-","").replace(".","");
    if(!v.matches("[0-9a-f]{12}") || v.equals("000000000000") || v.equals("ffffffffffff")) return "";
    return v;
  }
  private static boolean equalPresent(String a,String b) {return !a.isEmpty() && a.equals(b);}
  private static boolean isHardIdentityConflict(String conflict) {
    String c=conflict.toLowerCase(Locale.ROOT);
    return c.contains("abweichende seriennummer") || c.contains("build-fingerprint") || c.contains("ssh-hostkey");
  }

  public MergePreviewResult preview(String targetKey, List<String> sourceKeys, Map<String,Object> overrides) {
    // 40k36q18: Eine VORSCHAU muss auch bei unterschiedlichen MAC-Adressen möglich sein.
    // Die tatsächliche Schreiboperation bleibt weiterhin gesperrt, bis der Benutzer
    // den Konflikt ausdrücklich bestätigt. planMerge braucht für die reine Vorschau
    // daher intern die MAC-Freigabe, ohne diese als Benutzerbestätigung zu werten.
    Map<String,Object> previewOverrides=new LinkedHashMap<>();
    if(overrides!=null) previewOverrides.putAll(overrides);
    previewOverrides.put("confirmMacConflict",true);
    rejectCrossPlatformMerge(targetKey, sourceKeys);
    var plan = registrations.planMerge(targetKey, sourceKeys, previewOverrides);
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
    if (criticality != null) {
      String diagSignature="preview|"+targetKey+"|"+sourceKeys+"|"+criticality+"|"+allConflicts;
      if(shouldLogMergeDiagnostic(diagSignature)) mergeDebug.debug("Merge-Vorschau {} <- {}: Schutzstufe {} ausgelöst durch {}", targetKey, sourceKeys, criticality, allConflicts);
    }

    return new MergePreviewResult(plan.targetKey(), plan.sourceKeys(), plan.name(), plan.manualName(),
      plan.deviceType(), plan.manualDeviceType(), plan.address(), plan.mac(), plan.serialNumber(),
      plan.manufacturer(), plan.protocol(), plan.status(), plan.detectionCount(), plan.lastScanHits(),
      plan.knownMacs(), plan.knownMacs().size() > 1 && !confirmed, criticality, criticalMessage, comparison,
      plan.knownMacs().size() > 1 || DiscoveryRegistrationRepository.requiresManualConfirmation(criticality));
  }

  public Map<String,Object> confirm(String targetKey, List<String> sourceKeys, Map<String,Object> overrides, String actor) {
    Map<String,Object> confirmedOverrides=new LinkedHashMap<>();
    if(overrides!=null) confirmedOverrides.putAll(overrides);
    boolean acknowledgedCritical = Boolean.TRUE.equals(confirmedOverrides.get("confirmMacConflict"))
      || Boolean.TRUE.equals(confirmedOverrides.get("confirmCriticalConflict"));
    // 40k36q18: Eine ausdrückliche Bestätigung des kritischen/HOCH-Konflikts gilt
    // bei einem Multi-Interface-Merge zugleich als bewusste Bestätigung der
    // unterschiedlichen MAC-Adressen. Ohne diese Bestätigung bleibt planMerge gesperrt.
    if(Boolean.TRUE.equals(confirmedOverrides.get("confirmCriticalConflict")))
      confirmedOverrides.put("confirmMacConflict",true);
    rejectCrossPlatformMerge(targetKey, sourceKeys);
    var plan = registrations.planMerge(targetKey, sourceKeys, confirmedOverrides);
    // 40k37e56c-fix16: confirm(...) wird ausschließlich nach der ausdrücklichen
    // Benutzerbestätigung ausgeführt. Der bisher geloggte Override-Wert konnte false sein,
    // obwohl der Benutzer den angebotenen Merge bewusst bestätigt hatte. Das war nur eine
    // falsche Logausgabe; Merge-/Konfliktlogik und confirmedOverrides bleiben unverändert.
    log.debug("Zusammenführung von {} in {} wird bestätigt (kritischer Konflikt bewusst bestätigt: true)",
      sourceKeys, targetKey);
    String snapshotJson = snapshotOf(plan.sourceRows());
    Map<String,Object> merged = registrations.mergeDevices(targetKey, sourceKeys, confirmedOverrides, actor);
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

  private void rejectCrossPlatformMerge(String targetKey, List<String> sourceKeys) {
    Map<String,Object> target=registrations.find(targetKey);
    if (sourceKeys==null) return;
    for (String sourceKey:sourceKeys) {
      if (incompatibleRegisteredPlatforms(target,registrations.find(sourceKey)))
        throw new IllegalArgumentException("Zusammenführung abgelehnt: Android und iOS/iPadOS sind unterschiedliche Plattformen.");
    }
  }

  private static boolean incompatibleRegisteredPlatforms(Map<String,Object> a,Map<String,Object> b) {
    if (DeviceIdentityConfidenceEngine.incompatibleMobilePlatforms(toDiscoveredDevice(a),toDiscoveredDevice(b))) return true;
    String x=preferredMobileFamily(a),y=preferredMobileFamily(b);
    String inferredA=inferredMobileFamily(a),inferredB=inferredMobileFamily(b);
    // Eindeutige Plattformeinstellung darf einen neutralen Netzwerktreffer nicht sperren.
    if (!x.isEmpty() && !inferredB.isEmpty() && !x.equals(inferredB)) return true;
    if (!y.isEmpty() && !inferredA.isEmpty() && !y.equals(inferredA)) return true;
    return !x.isEmpty() && !y.isEmpty() && !x.equals(y);
  }
  private static String preferredMobileFamily(Map<String,Object> row) {
    String p=str(row.get("preferredPlatform"));
    if(p==null) return "";
    if(p.equalsIgnoreCase("Android")) return "android";
    if(p.equalsIgnoreCase("iOS") || p.equalsIgnoreCase("iPadOS")) return "apple-mobile";
    return "";
  }
  private static String inferredMobileFamily(Map<String,Object> row) {
    return DeviceIdentityConfidenceEngine.mobileFamily(toDiscoveredDevice(row));
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
