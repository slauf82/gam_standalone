package de.kopfzentrum.gam.inventory;

import java.util.List;
import java.util.Optional;

/**
 * 40k34h: Generisches, wiederverwendbares Evidence-Modell für die
 * Geräte-Nachklassifizierung. Sammelt gewichtete Hinweise (Evidence) aus
 * bereits vorhandenen Discovery-Daten und leitet daraus eine Klassifizierung
 * mit Konfidenzstufe ab - unabhängig von der Plattform.
 *
 * Keine neue Discovery-Quelle: diese Klasse liest keine Netzwerkdaten,
 * sondern bewertet ausschließlich bereits von anderen Quellen gelieferte
 * Hinweise (Hostname, MAC-Hersteller, Protokolltext, gespeicherte
 * ADB-Verbindung usw.).
 *
 * Android ist der erste Anwendungsfall (siehe AndroidDeviceClassifier).
 * Dieselbe Struktur kann später unverändert für weitere Plattformen
 * (iPhone, Windows, Linux, macOS, NAS, Drucker, Router, IoT) genutzt werden -
 * dafür ist in 40k34h keine Änderung an dieser Klasse nötig, nur ein
 * weiterer plattformspezifischer Aufrufer analog zu AndroidDeviceClassifier.
 */
public final class DeviceEvidenceEngine {
  private DeviceEvidenceEngine() {}

  /** Gewichtung je Hinweisstärke - entspricht den im Auftrag genannten Prioritätsstufen. */
  public enum Tier {
    SEHR_HOCH(100), HOCH(60), MITTEL(25), NIEDRIG(10);
    public final int weight;
    Tier(int weight) { this.weight = weight; }
  }

  public record Evidence(Tier tier, String description) {}

  public record Classification(String category, String platform, String roles, String manufacturer,
                               String confidenceLabel, List<Evidence> matched) {}

  /**
   * Bewertet die gesammelte Evidence und liefert nur dann eine Klassifizierung,
   * wenn genügend Gewicht erreicht wurde:
   * - EIN einzelner SEHR_HOCH-Hinweis reicht allein aus (z.B. eine bereits
   *   bestehende ADB-Verbindung mit gemeldeter Android-Version).
   * - Andernfalls ist eine Mindestsumme aus mehreren schwächeren Hinweisen
   *   nötig (z.B. bekannter Smartphone-MAC-Hersteller UND passender
   *   Hostname) - ein einzelner schwacher Hinweis reicht nicht.
   * Keine Vermutung ohne tatsächlich vorhandene Evidence.
   */
  public static Optional<Classification> classify(List<Evidence> evidence, String category, String platform,
                                                   String roles, String manufacturer) {
    if (evidence == null || evidence.isEmpty()) return Optional.empty();
    boolean hasSehrHoch = evidence.stream().anyMatch(e -> e.tier() == Tier.SEHR_HOCH);
    int score = evidence.stream().mapToInt(e -> e.tier().weight).sum();
    if (!hasSehrHoch && score < 60) return Optional.empty();
    String confidence = hasSehrHoch ? "Sehr sicher" : score >= 85 ? "Sicher" : "Wahrscheinlich";
    return Optional.of(new Classification(category, platform, roles, manufacturer, confidence, List.copyOf(evidence)));
  }
}
