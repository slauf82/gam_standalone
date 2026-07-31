package de.kopfzentrum.gam.inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * 40k34a: Android-Geräteklassifizierung ausschließlich auf Basis bereits
 * vorhandener Discovery-Informationen (mDNS-Diensttyp, mDNS-TXT-Eintrag,
 * Hostname/Anzeigename, bereits bekannter Hersteller). Reine Textauswertung,
 * kein Netzwerkzugriff, keine eigene Discovery - analog zur bereits
 * bestehenden Auswertung von SNMP-sysDescr (40k33b9) und Linux-Hinweisen
 * (40k33b3). Liefert nur eine Einstufung, wenn ein konkreter, tatsächlich
 * vorhandener Hinweis gefunden wird - keine unsicheren Vermutungen.
 *
 * 40k34h: zusätzlich eine evidenzbasierte Nachklassifizierung
 * (classifyWithEvidence()), die mehrere schwächere Hinweise (MAC-Hersteller,
 * Hostname) zu einer Klassifizierung zusammenführt und dabei ausdrücklich
 * auch eine bereits bestehende, falsche Einstufung (z.B. "Access Point")
 * überschreiben darf. Nutzt DAS GLEICHE Kategorie-Regelwerk wie die
 * bestehende classify()-Methode - keine zweite, parallele Logik.
 *
 * 40k34j: Feinabgleich anhand realer Testgeräte. Ursache der bisherigen
 * Fehlklassifizierung (siehe docs/40k34j-android-realgeraete.md): reale
 * DHCP-Hostnamen trennen Hersteller/Modell mit Bindestrichen
 * ("Galaxy-A56"), die bisherigen Muster prüften aber auf ein Leerzeichen
 * ("galaxy a") und griffen deshalb nie. Zusätzlich fehlten mehrere real
 * vorkommende Hersteller (u.a. Doogee) vollständig, und ein im Hostnamen
 * genannter Hersteller ohne begleitendes "smartphone"/"tablet"-Schlüsselwort
 * lieferte bisher gar keine Evidence. Alle drei Lücken wurden hier behoben -
 * ausschließlich in dieser Klasse, DeviceEvidenceEngine bleibt unverändert.
 */
public final class AndroidDeviceClassifier {
  private AndroidDeviceClassifier() {}

  public record Classification(String category, String platform, String roles, String manufacturer, List<String> reasons) {}

  private record CategoryGuess(String category, String roles, String reason) {}

  /**
   * @param name bereits bekannter Anzeigename/Hostname
   * @param signalText bereits bekannter Diensttyp/Protokolltext (z.B. mDNS-Diensttyp, TXT-Eintrag)
   * @param knownManufacturer bereits von einer anderen Quelle ermittelter Hersteller, falls vorhanden
   */
  public static Optional<Classification> classify(String name, String signalText, String knownManufacturer) {
    String hay = (nullToEmpty(name) + " " + nullToEmpty(signalText)).toLowerCase(Locale.ROOT);
    CategoryGuess guess = guessCategory(hay);
    if (guess == null) return Optional.empty();
    String manufacturer = knownManufacturer != null && !knownManufacturer.isBlank() ? knownManufacturer : manufacturerOf(hay);
    return Optional.of(new Classification(guess.category(), "Android", guess.roles(), manufacturer, List.of(guess.reason())));
  }

  /**
   * 40k34h: Evidenzbasierte Nachklassifizierung - EIN gemeinsames Regelwerk
   * (dieselbe guessCategory()/manufacturerOf()-Logik wie classify() oben),
   * ergänzt um zusätzliche, bereits vorhandene Hinweise, die für eine
   * Erstklassifizierung während des Netzwerkscans (classify() oben) noch
   * nicht zur Verfügung stehen: eine bereits gespeicherte ADB-Verbindung,
   * bereits über ADB erfasste Android-Kennwerte im Geräteprotokoll, ein
   * MAC-Adress-Hersteller-Präfix (OUI), sowie (seit 40k34j) ein im Hostnamen
   * erkannter Smartphone-Hersteller unabhängig davon, ob zusätzlich ein
   * Gerätetyp-Schlüsselwort ("phone"/"tablet") vorhanden ist, und ein
   * bereits von einer anderen Quelle bekannter Hersteller. Mehrere
   * schwächere Hinweise zusammen können eine Klassifizierung ergeben, auch
   * ohne ADB - siehe DeviceEvidenceEngine für die Gewichtung/Schwellenwerte.
   *
   * @param macAddress bereits bekannte MAC-Adresse, falls vorhanden
   * @param adbHost bereits gespeicherte ADB-Verbindung (adb_host), falls vorhanden
   * @param storedProtocol bereits gespeicherter discovery_protocol-Text der Identität, falls vorhanden
   */
  public static Optional<DeviceEvidenceEngine.Classification> classifyWithEvidence(
      String name, String signalText, String knownManufacturer, String macAddress, String adbHost, String storedProtocol) {
    String hay = (nullToEmpty(name) + " " + nullToEmpty(signalText)).toLowerCase(Locale.ROOT);
    List<DeviceEvidenceEngine.Evidence> evidence = new ArrayList<>();

    String protocolLower = nullToEmpty(storedProtocol).toLowerCase(Locale.ROOT);
    if (adbHost != null && !adbHost.isBlank()) {
      evidence.add(new DeviceEvidenceEngine.Evidence(DeviceEvidenceEngine.Tier.SEHR_HOCH, "ADB-Verbindung bereits bekannt (adb_host gesetzt)"));
    }
    if (protocolLower.contains("build-fingerprint:")) {
      evidence.add(new DeviceEvidenceEngine.Evidence(DeviceEvidenceEngine.Tier.SEHR_HOCH, "Android Build-Fingerprint bereits über ADB erfasst"));
    }
    if (protocolLower.contains("android-version:")) {
      evidence.add(new DeviceEvidenceEngine.Evidence(DeviceEvidenceEngine.Tier.SEHR_HOCH, "Android-Version bereits über ADB erfasst"));
    }

    String ouiManufacturer = manufacturerFromMacOui(macAddress);
    if (ouiManufacturer != null) {
      evidence.add(new DeviceEvidenceEngine.Evidence(DeviceEvidenceEngine.Tier.HOCH, "MAC-Adress-Hersteller (OUI) deutet auf " + ouiManufacturer + " hin"));
    }

    CategoryGuess guess = guessCategory(hay);
    if (guess != null) {
      boolean specific = !"Android-Gerät".equals(guess.category());
      evidence.add(new DeviceEvidenceEngine.Evidence(specific ? DeviceEvidenceEngine.Tier.HOCH : DeviceEvidenceEngine.Tier.NIEDRIG, guess.reason()));
    }

    // 40k34j: EIGENSTÄNDIGER Hinweis - ein im Hostnamen erkannter Smartphone-
    // Hersteller zählt als Evidence, AUCH WENN kein zusätzliches "phone"/"tablet"-
    // Schlüsselwort vorhanden ist (z.B. "Sebastian-Doogee-S96-Pro" enthält
    // keines der bisherigen Kategorie-Schlüsselwörter, aber eindeutig einen
    // Herstellernamen). Vorher lieferte ein solcher Fall gar keine Evidence.
    String hostnameManufacturer = manufacturerOf(hay);
    if (hostnameManufacturer != null) {
      evidence.add(new DeviceEvidenceEngine.Evidence(DeviceEvidenceEngine.Tier.HOCH, "Smartphone-Hersteller im Hostnamen erkannt: " + hostnameManufacturer));
    }

    // 40k34j: ein bereits von einer ANDEREN Quelle bekannter Hersteller (nicht aus
    // dem Hostnamen selbst) zählt ebenfalls als Hinweis, sofern es sich um einen
    // bekannten Smartphone-/Tablet-Hersteller handelt - z.B. wenn eine reine
    // Modellbezeichnung wie "A56" im Hostnamen steht, der Hersteller "Samsung"
    // aber bereits über eine andere Discoveryquelle bekannt ist.
    if (knownManufacturer != null && isKnownSmartphoneManufacturer(knownManufacturer)) {
      evidence.add(new DeviceEvidenceEngine.Evidence(DeviceEvidenceEngine.Tier.HOCH, "Bereits von anderer Quelle bekannter Smartphone-Hersteller: " + knownManufacturer));
    }

    // 40k34j: vorsichtige, NIEMALS allein ausreichende Evidence für ein reines
    // Modellnummern-Muster (z.B. "A56", "A02s", "S96-Pro", "F107-Pro") - trägt
    // nur zusammen mit einem der obigen, stärkeren Hinweise zur Schwelle bei.
    if (looksLikeModelNumber(hay)) {
      evidence.add(new DeviceEvidenceEngine.Evidence(DeviceEvidenceEngine.Tier.MITTEL, "Hostname enthält ein typisches Smartphone-Modellnummern-Muster"));
    }

    if (evidence.isEmpty()) return Optional.empty();

    String category = guess != null ? guess.category()
      : (hostnameManufacturer != null || (knownManufacturer != null && isKnownSmartphoneManufacturer(knownManufacturer))) ? "Smartphones & Tablets"
      : "Android-Gerät";
    String roles = guess != null ? guess.roles() : ("Smartphones & Tablets".equals(category) ? "Telefon · Multimedia" : null);
    String manufacturer = knownManufacturer != null && !knownManufacturer.isBlank() ? knownManufacturer
      : hostnameManufacturer != null ? hostnameManufacturer
      : ouiManufacturer != null ? ouiManufacturer : manufacturerOf(hay);

    return DeviceEvidenceEngine.classify(evidence, category, "Android", roles, manufacturer);
  }

  private static CategoryGuess guessCategory(String hay) {
    // 40k34j: reale DHCP-Hostnamen trennen Hersteller/Modell fast immer mit
    // Bindestrichen oder Unterstrichen ("Galaxy-A56"), nicht mit Leerzeichen.
    // Für die reinen Kategorie-Wortgruppen (nicht für mDNS-Diensttypen wie
    // "_googlecast", die den Unterstrich als Teil der Syntax benötigen) wird
    // deshalb zusätzlich eine leerzeichen-normalisierte Fassung geprüft.
    String haySpaced = hay.replace('-', ' ').replace('_', ' ');

    if (hay.contains("amzn-wplay") || hay.contains("fire tv") || hay.contains("firetv")) {
      return new CategoryGuess("📺 Fire TV", "Streaming · Fernseher", "Hinweis auf Amazon Fire TV (amzn-wplay/Name)");
    }
    if (hay.contains("shield")) {
      return new CategoryGuess("🎮 Nvidia Shield", "Streaming · Multimedia", "Namenshinweis auf Nvidia Shield");
    }
    if (hay.contains("_androidtvremote") || hay.contains("android tv remote")) {
      return new CategoryGuess("📺 Google TV", "Streaming · Fernseher", "mDNS-Diensttyp _androidtvremote(2)._tcp erkannt");
    }
    if (hay.contains("_googlecast") || hay.contains("chromecast") || hay.contains("google cast")) {
      return new CategoryGuess("📺 Android TV", "Streaming · Multimedia", "mDNS-Diensttyp _googlecast._tcp bzw. Chromecast-Hinweis erkannt");
    }
    if (hay.contains("kindle") || hay.contains("e-book") || hay.contains("ebook-reader") || hay.contains("ereader")) {
      return new CategoryGuess("📖 E-Book Reader", "Multimedia", "Namenshinweis auf E-Book-Reader");
    }
    if (hay.contains("kiosk") || hay.contains("signage") || hay.contains("panel")) {
      return new CategoryGuess("🖥 Android Display", "Digital Signage · Bedienpanel", "Namenshinweis auf Kiosk/Digital-Signage/Bedienpanel");
    }
    if (haySpaced.contains("galaxy tab") || haySpaced.contains("mediapad") || haySpaced.contains("matepad") || hay.contains("tablet")) {
      return new CategoryGuess("Smartphones & Tablets", "Tablet · Multimedia", "Namenshinweis auf Tablet");
    }
    if (haySpaced.contains("galaxy s") || haySpaced.contains("galaxy a") || haySpaced.contains("galaxy z") || haySpaced.contains("galaxy note")
      || haySpaced.contains("pixel ") || haySpaced.contains("redmi") || haySpaced.contains("poco ") || haySpaced.contains("mi phone")
      || hay.contains("smartphone")) {
      return new CategoryGuess("Smartphones & Tablets", "Telefon · Multimedia", "Namenshinweis auf Smartphone-Modell");
    }
    if (hay.contains("android")) {
      return new CategoryGuess("Android-Gerät", null, "Allgemeiner Android-Hinweis erkannt, keine spezifischere Zuordnung möglich");
    }
    return null;
  }

  /**
   * Herstellererkennung nach demselben, bereits im Projekt etablierten Muster
   * wie SnmpDiscoveryService.manufacturer() - ein einfacher Schlüsselwort-Abgleich,
   * keine neue Architektur. 40k34j: um real vorkommende, bisher fehlende
   * Hersteller ergänzt (u.a. Doogee, Oppo, Vivo, Nothing, Fairphone, Nokia,
   * Asus) - jeweils exakt die im Auftrag genannten, eindeutigen Marken.
   */
  private static String manufacturerOf(String hay) {
    if (hay.contains("samsung") || hay.contains("galaxy")) return "Samsung";
    if (hay.contains("pixel") || hay.contains("chromecast") || hay.contains("google")) return "Google";
    if (hay.contains("sony") || hay.contains("bravia")) return "Sony";
    if (hay.contains("xiaomi") || hay.contains("redmi") || hay.contains("poco") || hay.contains("mi phone")) return "Xiaomi";
    if (hay.contains("lenovo")) return "Lenovo";
    if (hay.contains("huawei") || hay.contains("mediapad") || hay.contains("matepad")) return "Huawei";
    if (hay.contains("amazon") || hay.contains("fire tv") || hay.contains("firetv") || hay.contains("kindle") || hay.contains("amzn")) return "Amazon";
    if (hay.contains("nvidia") || hay.contains("shield")) return "Nvidia";
    if (hay.contains("oneplus")) return "OnePlus";
    if (hay.contains("motorola") || hay.contains("moto")) return "Motorola";
    if (hay.contains("honor")) return "Honor";
    if (hay.contains("realme")) return "Realme";
    if (hay.contains("nothing phone") || hay.replace('-', ' ').contains("nothing phone")) return "Nothing";
    if (hay.contains("doogee")) return "Doogee";
    if (hay.contains("oppo")) return "Oppo";
    if (java.util.regex.Pattern.compile("\\bvivo\\b").matcher(hay).find()) return "Vivo";
    if (hay.contains("fairphone")) return "Fairphone";
    if (hay.contains("nokia")) return "Nokia";
    if (hay.contains("zenfone") || hay.contains("asus") || hay.contains("rog phone") || hay.contains(" rog")) return "Asus";
    return null;
  }

  /** 40k34j: bereits bekannte, hier zusätzlich als eigenständige Evidence nutzbare Markenliste. */
  private static boolean isKnownSmartphoneManufacturer(String manufacturer) {
    if (manufacturer == null || manufacturer.isBlank()) return false;
    String m = manufacturer.trim().toLowerCase(Locale.ROOT);
    return List.of("samsung","google","xiaomi","redmi","poco","oneplus","motorola","honor","realme","nothing",
      "doogee","oppo","vivo","fairphone","nokia","asus","huawei","sony").contains(m);
  }

  /**
   * 40k34j: vorsichtiges, generisches Muster für Smartphone-Modellnummern
   * (z.B. "A56", "A02s", "S96", "F107"), optional mit Zusatz wie "Pro"/"Plus"/
   * "Ultra"/"Lite"/"Max"/"5G". Wird NIE allein für eine Klassifizierung
   * verwendet (siehe classifyWithEvidence: nur als MITTEL-Priorität), sondern
   * ausschließlich als unterstützender Hinweis in Kombination mit einem
   * bereits bekannten oder im Hostnamen erkannten Hersteller.
   */
  private static boolean looksLikeModelNumber(String hay) {
    return java.util.regex.Pattern.compile("\\b[a-z]\\d{2,4}[a-z]?\\b").matcher(hay).find();
  }

  /**
   * 40k34h: Sehr kleine, ausdrücklich NICHT vollständige und nicht gegen die
   * aktuelle IEEE-OUI-Datenbank verifizierte Beispielzuordnung einiger
   * öffentlich bekannter MAC-Adress-Präfixe zu Smartphone-Herstellern. Dient
   * als zusätzlicher, unterstützender Hinweis (Priorität HOCH) - niemals
   * alleinige Grundlage einer Klassifizierung (siehe DeviceEvidenceEngine:
   * ein einzelner HOCH-Hinweis reicht ohne weitere Evidence nicht aus).
   * Diese Liste sollte vor einem Produktiveinsatz gegen eine aktuelle,
   * vollständige OUI-Quelle erweitert/geprüft werden - siehe Dokumentation.
   */
  private static final java.util.Map<String,String> KNOWN_SMARTPHONE_OUI_PREFIXES = java.util.Map.ofEntries(
    java.util.Map.entry("8425DB", "Samsung"), java.util.Map.entry("A0B4A5", "Samsung"), java.util.Map.entry("5C0A5B", "Samsung"),
    java.util.Map.entry("F8DAE9", "Google"), java.util.Map.entry("3C5AB4", "Google"),
    java.util.Map.entry("34CE00", "Xiaomi"), java.util.Map.entry("64CC2E", "Xiaomi"),
    java.util.Map.entry("A4C138", "OnePlus"),
    java.util.Map.entry("40B0FA", "Motorola"),
    java.util.Map.entry("FC8F90", "Huawei")
  );

  private static String manufacturerFromMacOui(String macAddress) {
    if (macAddress == null) return null;
    String normalized = macAddress.replaceAll("[^0-9A-Fa-f]", "").toUpperCase(Locale.ROOT);
    if (normalized.length() < 6) return null;
    return KNOWN_SMARTPHONE_OUI_PREFIXES.get(normalized.substring(0, 6));
  }

  private static String nullToEmpty(String value) { return value == null ? "" : value; }
}
