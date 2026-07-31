package de.kopfzentrum.gam.inventory;

import java.util.*;

/**
 * Schritt 40k31c: quellenuebergreifende, gewichtete Identitaetsbewertung.
 * Starke Merkmale duerfen automatisch zusammenfuehren; schwache Merkmale
 * dienen nur als zusaetzliche Evidenz und verhindern IP-basierte Fehl-Merges.
 */
public final class DeviceIdentityConfidenceEngine {
  public static final Settings DEFAULT_SETTINGS = new Settings(95,60,85,95,90,80,50,5,10,15,5,10,true,true,true);

  public record Settings(int autoMergeThreshold, int possibleDuplicateThreshold, int macWeight,
                         int hardwareSerialWeight, int snmpSerialWeight, int deviceIdWeight,
                         int hostnameWeight, int ipWeight, int manufacturerWeight, int typeWeight,
                         int twoSourceBonus, int threeSourceBonus, boolean automaticMergeEnabled,
                         boolean hardConflictsBlockMerge, boolean ipNeverMergesAlone) {}

  public enum Decision { AUTO_MERGE, POSSIBLE_DUPLICATE, DISTINCT }

  public record Assessment(int score, int confidencePercent, Decision decision,
                           List<String> matchedSignals, List<String> conflicts) {}

  public Assessment assess(DiscoveredDevice left, DiscoveredDevice right) { return assess(left, right, DEFAULT_SETTINGS); }

  public Assessment assess(DiscoveredDevice left, DiscoveredDevice right, Settings settings) {
    if (settings == null) settings = DEFAULT_SETTINGS;
    if (left == null || right == null) return new Assessment(0, 0, Decision.DISTINCT, List.of(), List.of());

    int score = 0;
    List<String> matches = new ArrayList<>();
    List<String> conflicts = new ArrayList<>();

    String leftMac = normalizeMac(left.hardwareAddress());
    String rightMac = normalizeMac(right.hardwareAddress());
    if (same(leftMac, rightMac)) { score += settings.macWeight(); matches.add("MAC-Adresse"); }
    else if (both(leftMac, rightMac)) conflicts.add("abweichende MAC-Adresse");

    String leftSerial = normalizeStrong(left.serialNumber());
    String rightSerial = normalizeStrong(right.serialNumber());
    if (same(leftSerial, rightSerial)) { score += serialWeight(left, right, settings); matches.add("Seriennummer/Geräte-ID"); }
    else if (both(leftSerial, rightSerial) && isHardwareSerial(left, right)) conflicts.add("abweichende Seriennummer");

    // 40k34e: Android-Ergänzung derselben Engine (keine zweite Bewertungslogik) - trotz
    // bereits übereinstimmender starker Identität (MAC oder Seriennummer, z.B. per ADB
    // ermittelt) widerspricht ein unterschiedlicher Build-Fingerprint dieser Identität und
    // wird als eigenständiger, hoch eingestufter Konflikt gemeldet (siehe conflictSeverity()
    // in DiscoveryRegistrationRepository).
    boolean strongIdentityMatch = same(leftMac, rightMac) || same(leftSerial, rightSerial);
    if (strongIdentityMatch) {
      String leftFingerprint = normalizeText(DiscoveryRegistrationRepository.detailValues(left.protocol()).get("Build-Fingerprint"));
      String rightFingerprint = normalizeText(DiscoveryRegistrationRepository.detailValues(right.protocol()).get("Build-Fingerprint"));
      if (both(leftFingerprint, rightFingerprint) && !leftFingerprint.equals(rightFingerprint)) {
        conflicts.add("abweichender Build-Fingerprint trotz gleicher starker Identität");
      }
    }

    String leftName = normalizeName(left.name());
    String rightName = normalizeName(right.name());
    if (same(leftName, rightName)) {
      boolean semanticIdentity = isDistinctiveSemanticName(leftName)
        && independentSources(left.protocol(), right.protocol());
      int nameScore = semanticIdentity
        ? Math.max(90, settings.hostnameWeight())
        : (compatibleType(left.type(), right.type()) ? settings.hostnameWeight() : Math.max(1, settings.hostnameWeight()/2));
      score += nameScore;
      matches.add(semanticIdentity ? "Semantische Geräteidentität" : "Hostname/Gerätename");
    }

    String leftIp = normalizeIp(left.address());
    String rightIp = normalizeIp(right.address());
    if (same(leftIp, rightIp)) { score += settings.ipWeight(); matches.add("IP-Adresse"); }
    // 40k33b6a: unterschiedliche, jeweils gültige IP-Adressen bei gleichzeitig als
    // erreichbar gemeldeten Geräten sind ein eigenständiger, deutlich sichtbarer
    // Konflikt (z.B. Geräte-Umzug ins falsche Segment) - rein zusätzliche
    // Erkennung, keine Änderung der bestehenden Schwellenwerte/Gewichtung.
    else if (both(leftIp, rightIp) && isReachable(left.status()) && isReachable(right.status())) {
      conflicts.add("abweichende IP-Adresse bei gleichzeitig erreichbaren Geräten");
    }

    String leftManufacturer = normalizeText(left.manufacturer());
    String rightManufacturer = normalizeText(right.manufacturer());
    if (same(leftManufacturer, rightManufacturer)) { score += settings.manufacturerWeight(); matches.add("Hersteller"); }

    String leftType = normalizeText(left.type());
    String rightType = normalizeText(right.type());
    if (same(leftType, rightType)) { score += settings.typeWeight(); matches.add("Gerätetyp"); }

    int sourceBonus = sourceAgreementBonus(left.protocol(), right.protocol(), settings);
    if (sourceBonus > 0 && !matches.isEmpty()) { score += sourceBonus; matches.add("mehrere unabhängige Quellen"); }

    // Harte Konflikte begrenzen die Automatik. Eine identische IP allein darf nie mergen.
    if (settings.hardConflictsBlockMerge() && !conflicts.isEmpty() && !same(leftMac, rightMac) && !same(leftSerial, rightSerial)) score = Math.min(score, settings.autoMergeThreshold()-1);
    if (settings.ipNeverMergesAlone() && matches.size() == 1 && matches.contains("IP-Adresse")) score = Math.min(settings.ipWeight(), settings.possibleDuplicateThreshold()-1);
    // 40k34e: der Build-Fingerprint-Konflikt muss einen automatischen Merge in jedem Fall
    // verhindern, auch wenn die vorstehende Bedingung (mac/seriell weichen ab) hier nicht
    // zutrifft - eigenständige, additive Deckelung.
    if (settings.hardConflictsBlockMerge() && conflicts.stream().anyMatch(c -> c.contains("Build-Fingerprint"))) {
      score = Math.min(score, settings.autoMergeThreshold()-1);
    }

    // 40k33b4: IP-Merge-Bruecke. Wenn ein Treffer eine bekannte MAC-Adresse hat, der andere
    // Treffer fuer dieselbe IP noch KEINE MAC-Adresse besitzt, keine widersprechende bekannte
    // MAC existiert und auch sonst kein Konflikt vorliegt (z.B. abweichende Seriennummer), ist
    // die gemeinsame IP ein starkes Identitaetsmerkmal - typischerweise derselbe physische
    // Wechselrichter/Server, einmal per mDNS ohne MAC und einmal per ARP/Windows mit MAC erkannt.
    // Zwei GEGENSAETZLICHE bekannte MAC-Adressen loesen diese Bruecke ausdruecklich NICHT aus.
    boolean exactlyOneKnownMac = leftMac.isEmpty() != rightMac.isEmpty();
    boolean ipBridgeEligible = same(leftIp, rightIp) && exactlyOneKnownMac && conflicts.isEmpty();
    if (ipBridgeEligible) {
      score = Math.max(score, settings.autoMergeThreshold());
      if (!matches.contains("IP-Adresse")) matches.add("IP-Adresse");
      matches.add("IP-Merge-Brücke (bekannte MAC ergänzt fehlende MAC ohne Widerspruch)");
    }

    int confidence = Math.min(100, Math.max(0, (int)Math.round(score / 1.6)));
    Decision decision = settings.automaticMergeEnabled() && score >= settings.autoMergeThreshold() ? Decision.AUTO_MERGE
      : score >= settings.possibleDuplicateThreshold() ? Decision.POSSIBLE_DUPLICATE : Decision.DISTINCT;
    return new Assessment(score, confidence, decision, List.copyOf(matches), List.copyOf(conflicts));
  }

  private static int serialWeight(DiscoveredDevice left, DiscoveredDevice right, Settings settings) {
    if (isHardwareSerial(left, right)) return settings.hardwareSerialWeight();
    String protocols = (safe(left.protocol()) + " " + safe(right.protocol())).toLowerCase(Locale.ROOT);
    if (protocols.contains("snmp")) return settings.snmpSerialWeight();
    if (protocols.contains("tuya") || protocols.contains("mqtt")) return settings.deviceIdWeight();
    return settings.deviceIdWeight();
  }

  private static boolean isHardwareSerial(DiscoveredDevice left, DiscoveredDevice right) {
    String p = (safe(left.protocol()) + " " + safe(right.protocol())).toLowerCase(Locale.ROOT);
    return p.contains("wmi") || p.contains("winrm") || p.contains("ssh") || p.contains("proxmox") || p.contains("bios") || p.contains("adb");
  }

  private static int sourceAgreementBonus(String a, String b, Settings settings) {
    Set<String> sources = new HashSet<>();
    collectSources(sources, a); collectSources(sources, b);
    return sources.size() >= 3 ? settings.threeSourceBonus() : sources.size() >= 2 ? settings.twoSourceBonus() : 0;
  }

  private static void collectSources(Set<String> target, String value) {
    if (value == null) return;
    for (String part : value.split("[,·]")) {
      String cleaned = normalizeText(part);
      if (!cleaned.isBlank()) target.add(cleaned);
    }
  }

  private static boolean compatibleType(String a, String b) {
    String left = normalizeText(a), right = normalizeText(b);
    return left.isBlank() || right.isBlank() || left.equals(right)
      || left.contains("netzwerk") || right.contains("netzwerk")
      || left.contains("gerät") || right.contains("gerät");
  }

  private static String normalizeMac(String value) {
    return value == null ? "" : value.replaceAll("[^0-9A-Fa-f]", "").toUpperCase(Locale.ROOT);
  }

  private static String normalizeIp(String value) {
    if (value == null) return "";
    String v = value.trim().toLowerCase(Locale.ROOT);
    return v.matches("(?:\\d{1,3}\\.){3}\\d{1,3}") ? v : "";
  }

  private static String normalizeStrong(String value) {
    String v = normalizeText(value).replaceAll("[^a-z0-9]", "");
    if (v.length() < 6 || Set.of("unknown", "default", "none", "null", "tobefilledbyoem").contains(v)) return "";
    return v;
  }

  private static String normalizeName(String value) {
    String v = normalizeText(value);
    int dot = v.indexOf('.');
    if (dot > 0 && dot < v.length() - 1 && v.substring(0, dot).matches("[a-z_]+")) v = v.substring(dot + 1);
    v = foldGerman(v).replaceAll("[^a-z0-9]", "");
    if (v.startsWith("netzwerkgeraet") || v.startsWith("mdnsgeraet") || v.equals("unbekanntesgeraet")) return "";
    return v;
  }

  private static boolean isDistinctiveSemanticName(String value) {
    return value != null && value.length() >= 8
      && !Set.of("smartdevice", "networkdevice", "unknowndevice", "unbekanntesgeraet", "geraet", "device").contains(value);
  }

  private static boolean independentSources(String a, String b) {
    Set<String> left = new HashSet<>(), right = new HashSet<>();
    collectSources(left, a); collectSources(right, b);
    if (left.isEmpty() || right.isEmpty()) return false;
    for (String source : left) if (right.contains(source)) return false;
    return true;
  }

  private static String foldGerman(String value) {
    return value.replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss");
  }

  private static String normalizeText(String value) {
    return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replaceAll("\s+", " ");
  }

  private static boolean same(String a, String b) { return both(a,b) && a.equals(b); }
  private static boolean both(String a, String b) { return a != null && b != null && !a.isBlank() && !b.isBlank(); }
  private static String safe(String value) { return value == null ? "" : value; }
  private static boolean isReachable(String status) { return status != null && status.trim().equalsIgnoreCase("ONLINE"); }
}
