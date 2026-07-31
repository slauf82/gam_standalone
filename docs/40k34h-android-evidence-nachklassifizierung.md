# Schritt 40k34h – Android Evidence-Modell und Nachklassifizierung

## Ziel und Problem

Android-Geräte wurden nach dem Netzwerkscan häufig fälschlich als „Access
Point" (oder generisches „Netzwerkgerät") klassifiziert, da Smartphones im
WLAN zunächst wie normale Netzwerkgeräte erscheinen. Es fehlte eine
Nachklassifizierung, die bereits vorhandene, aber bisher ungenutzte Hinweise
(MAC-Hersteller, Hostname, bereits gespeicherte ADB-Daten) zu einer
belastbaren Einstufung zusammenführt.

## Architektur

**Keine neue Discovery-Quelle.** Es werden ausschließlich bereits
gesammelte Daten ausgewertet: der Hostname/Name und Protokolltext aus dem
laufenden Suchlauf, die MAC-Adresse, sowie - falls vorhanden - eine bereits
gespeicherte ADB-Verbindung bzw. bereits über ADB erfasste Android-Version/
Build-Fingerprint derselben Identität. Kein zusätzlicher Netzwerk- oder
ADB-Zugriff während der Nachklassifizierung.

### Neue, generische Bausteine

- **`DeviceEvidenceEngine`** (neu) - plattformunabhängiges Evidence-Modell:
  vier Prioritätsstufen (`SEHR_HOCH`/`HOCH`/`MITTEL`/`NIEDRIG`, siehe
  Auftrag), eine `Evidence`-Struktur (Stufe + Begründung) und eine
  `classify()`-Methode, die aus gesammelter Evidence eine Klassifizierung mit
  Konfidenzlabel ableitet - **oder eben keine**, wenn zu wenig Evidence
  vorhanden ist. Diese Klasse kennt nichts Android-Spezifisches und kann
  unverändert für spätere Plattformen (iPhone, Windows, Linux, macOS, NAS,
  Drucker, Router, IoT) wiederverwendet werden - wie im Auftrag gefordert.
- **`AndroidDeviceClassifier.classifyWithEvidence(...)`** (neue Methode in
  der bereits bestehenden Klasse, **keine neue Klasse**) - der erste
  Anwendungsfall des Evidence-Modells. Nutzt intern **dieselbe**
  Kategorie-Regelwerk-Methode (`guessCategory()`) wie die bereits bestehende
  `classify()`-Methode (durch Refactoring in eine gemeinsame private
  Hilfsmethode extrahiert) - keine Code-Duplikate, keine zweite
  Klassifizierungslogik.
- **`DiscoveryRegistrationRepository.applyEvidenceReclassification(...)`**
  (neue Methode) - schreibt die neue Kategorie **automatisch**, respektiert
  aber eine bereits bestehende manuelle Zuordnung (`manual_device_type`)
  und setzt selbst **keine** manuelle Sperre, damit spätere, noch
  spezifischere automatische Einstufungen weiterhin möglich bleiben. Nutzt
  dieselbe Änderungshistorie (`gam_discovery_device_type_history`) wie die
  bereits bestehende manuelle Umkategorisierung - keine zweite Tabelle,
  keine neue Migration.
- **Neuer Schritt in `DeviceDiscoveryService.scanStreaming()`** (zwischen
  Reverse-DNS und Konsolidierung) - iteriert über die in diesem Suchlauf
  gesehenen Geräte, sammelt Evidence je Gerät und wendet bei ausreichender
  Konfidenz die Nachklassifizierung an.

## Evidence-Prioritäten (wie im Auftrag)

| Stufe | Beispiel | Gewicht |
|---|---|---|
| Sehr hoch | bereits gespeicherte ADB-Verbindung, bereits über ADB erfasster Build-Fingerprint/Android-Version | 100 (allein ausreichend) |
| Hoch | bekannter Smartphone-Hersteller anhand MAC-OUI-Präfix; konkreter Hostname-/Protokoll-Treffer (z.B. „Galaxy", „Pixel") | 60 |
| Mittel | (aktuell nicht separat verwendet - siehe Einschränkungen) | 25 |
| Niedrig | allgemeiner „android"-Hinweis ohne konkrete Zuordnung | 10 |

**Schwelle:** Ein einzelner SEHR_HOCH-Hinweis reicht allein aus. Ohne einen
solchen ist eine Mindestsumme von 60 Gewichtungspunkten nötig - ein
einzelner HOCH-Hinweis (z.B. nur MAC-OUI, ohne passenden Hostname) reicht
also bereits aus, ein einzelner NIEDRIG-Hinweis allein nicht.

## MAC-OUI-Zuordnung - ausdrücklich unvollständig

Es wurde eine **sehr kleine**, in dieser Sitzung **nicht gegen die aktuelle
IEEE-OUI-Datenbank verifizierte** Beispielliste weniger MAC-Adress-Präfixe
zu Samsung/Google/Xiaomi/OnePlus/Motorola/Huawei ergänzt
(`AndroidDeviceClassifier.KNOWN_SMARTPHONE_OUI_PREFIXES`). **Diese Liste ist
bewusst nicht vollständig** und sollte vor einem Produktiveinsatz gegen eine
aktuelle, vollständige OUI-Quelle geprüft und erweitert werden - siehe
„Bekannte Einschränkungen". Ein MAC-Präfix, der nicht in dieser kleinen
Liste enthalten ist, liefert schlicht kein OUI-Evidence (kein Fehler, keine
Vermutung) - genau wie im Auftrag gefordert: „Ehrlichkeit vor
Vollständigkeit".

## Klassifizierung und Überschreiben von „Access Point"

`applyEvidenceReclassification()` schreibt die neue Kategorie direkt, sofern
sie sich von der aktuellen unterscheidet und die Identität **nicht** bereits
manuell zugeordnet wurde. Dadurch kann eine bisherige „Access Point"-
Einstufung durch „📱 Smartphone"/„📱 Tablet"/„Android-Gerät" ersetzt werden,
sobald ausreichend Android-Evidence vorliegt - die Netzwerkerkennung selbst
(Discovery-Quelle) bleibt davon unberührt, nur die Geräteklasse ändert sich.

## Darstellung

Es wurde **keine Sonderlogik im Frontend** ergänzt. Da die neue Kategorie
über dasselbe `device_type`-Feld geschrieben wird, das im gesamten Frontend
bereits generisch angezeigt wird (Gerätetabelle, Reports, Identitätsdialog),
erscheint „📱 Smartphone" o.ä. automatisch überall dort, wo bisher „Access
Point" stand - ohne jede Codeänderung im Frontend. Auch die
Änderungshistorie im Identitätsdialog („Kategorie-Änderungshistorie")
zeigt die neue automatische Nachklassifizierung automatisch mit an, da sie
dieselbe, bereits bestehende `gam_discovery_device_type_history`-Tabelle
nutzt.

## Nicht Bestandteil (wie im Auftrag)

Keine neuen Scanner, keine neuen Netzwerkprotokolle, keine neuen Reports,
keine Merge-Änderungen, keine Änderungen an der Inventarisierung, keine
neuen Discovery-Quellen. Ausschließlich eine Verbesserung der
Geräteklassifizierung.

## Bekannte Einschränkungen

- **MITTEL-Priorität (DHCP-Hostname-Muster) nicht als eigene Evidence-Stufe
  verdrahtet:** Die bestehende `guessCategory()`-Methode erkennt bereits
  viele der im Auftrag genannten Hostname-Muster (Galaxy, Pixel, Xiaomi,
  Redmi, Moto, Tablet, Smartphone), stuft einen konkreten Treffer aber als
  HOCH statt als eigene MITTEL-Stufe ein - eine feinere Unterscheidung
  zwischen „DHCP-Hostname" und anderweitigem Hostname-Text war mit den
  bestehenden Daten (Hostname und andere Textquellen werden bereits zu
  einem gemeinsamen „hay"-Text zusammengeführt) nicht sauber trennbar, ohne
  die bestehende `classify()`-Methode zu verändern.
- **MAC-OUI-Liste ist absichtlich klein und unverifiziert** (siehe oben) -
  deckt nur einen kleinen Bruchteil der im Auftrag genannten Hersteller ab
  (Sony, Honor, Oppo, Vivo, Nokia, Fairphone, Nothing, Asus, Realme derzeit
  **nicht** in der OUI-Liste, nur über Hostname-Text erkennbar).
- **Die Nachklassifizierung läuft nur für Geräte, die in der aktuellen
  Registrierungstabelle bereits einen Datensatz haben** (per
  `resolveKey()` auflösbar) - ein Gerät, das in genau diesem Suchlauf zum
  allerersten Mal gesehen wird, durchläuft die normale
  Erstklassifizierung, nicht diese Nachklassifizierung (das ist so
  beabsichtigt: die Nachklassifizierung ergänzt bereits registrierte
  Geräte um zusätzliche, seither hinzugekommene Evidence).
- **Kein echter Build möglich** (siehe unten).

## Build-/Codeprüfung

**Es konnte in dieser Sandbox kein echter Maven-/Gradle-Build durchgeführt
werden** - das wird hiermit ausdrücklich und ehrlich benannt (kein `javac`,
kein `mvn`, kein `gradle` verfügbar, nur ein nacktes JRE). Stattdessen
wurden folgende statische Prüfungen durchgeführt:

- Klammernbilanz aller vier geänderten/neuen Backend-Dateien (ausgeglichen).
- Ein automatisiertes Skript, das alle `record Name(...)`-Deklarationen und
  alle `new Name(...)`-Konstruktor-Aufrufe extrahiert und die
  Argumentanzahl vergleicht (genau die Prüfung, die den in 40k34g
  gemeldeten echten Kompilierfehler nachträglich bestätigt hätte). Für die
  in diesem Schritt neuen/geänderten Dateien: keine echten Abweichungen
  gefunden. Zwei zunächst gemeldete „Treffer" erwiesen sich als Fehlalarm
  des Skripts selbst (zwei unterschiedliche Klassen mit demselben
  Record-Namen „Classification" - `DeviceEvidenceEngine.Classification`
  und `AndroidDeviceClassifier.Classification` - wurden ohne Klassenkontext
  gegeneinander verglichen; beide Konstruktor-Aufrufe passen tatsächlich
  exakt zu ihrer jeweils eigenen Deklaration).
- Manuelle Durchsicht aller neuen Methodensignaturen zwischen Aufrufer und
  Definition (`classifyWithEvidence()`, `applyEvidenceReclassification()`).

**Nicht durchgeführt:** echtes Kompilieren, echte Ausführung gegen eine
Datenbank oder ein echtes Android-Gerät. Bitte vor dem Produktiveinsatz
einen echten `mvn compile`/Build durchführen.

## Testanleitung (noch nicht durchgeführt, als Checkliste gedacht)

1. Ein Android-Smartphone im WLAN scannen, das zuvor als „Access Point"
   registriert wurde (bekannter MAC-Hersteller + passender Hostname):
   Kategorie muss nach dem nächsten Suchlauf auf „📱 Smartphone" wechseln.
2. Dasselbe Gerät nach einer bereits erfolgten ADB-Verbindung (40k34b)
   erneut scannen: Kategorie muss (sofern noch nicht spezifisch) auf
   „Android-Gerät" oder spezifischer wechseln, mit Konfidenz „Sehr sicher".
3. Ein Gerät, dessen Kategorie bereits manuell gesetzt wurde
   (`manual_device_type=TRUE`): darf **nicht** automatisch überschrieben
   werden.
4. Ein Gerät ohne jegliche Android-Evidence (z.B. ein echter Router):
   Kategorie darf sich nicht ändern.
5. Änderungshistorie im Identitätsdialog öffnen: automatische
   Nachklassifizierung muss dort mit Begründung erscheinen.
