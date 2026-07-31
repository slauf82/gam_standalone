# Schritt 40k34j – Android-Realgeräteklassifizierung verbessern

## Ursache der bisherigen Fehlklassifizierung (analysiert, nicht vermutet)

Die tatsächlich vorhandenen Regeln in `AndroidDeviceClassifier` wurden
gegen die sechs Referenz-Hostnamen geprüft. Ergebnis:

1. **Bindestrich statt Leerzeichen (Hauptursache):** Die bestehenden
   Kategorie-Muster prüften auf `"galaxy a"`, `"galaxy s"`, `"galaxy z"`,
   `"pixel "`, `"poco "` usw. - jeweils mit **Leerzeichen**. Reale
   DHCP-Hostnamen trennen Hersteller und Modell aber praktisch immer mit
   **Bindestrichen** (`Galaxy-A56`, nicht `Galaxy A56`). Dadurch griff
   keines dieser Muster bei `Egon-Galaxy-A56-5G`, `Galaxy-A02s` oder
   `Conni-Galaxy-A56-5G`, obwohl der Herstellername eindeutig im Hostnamen
   steht.
2. **„Doogee" fehlte vollständig** - weder in `guessCategory()` noch in
   `manufacturerOf()` war dieser Hersteller hinterlegt, wodurch
   `Sebastian-Doogee-S96-Pro` keinerlei Evidence erzeugte.
3. **Ein im Hostnamen erkannter Hersteller ohne begleitendes
   „phone"/„tablet"-Schlüsselwort lieferte bisher gar keine Evidence.**
   `guessCategory()` verlangte für eine Smartphone-Einstufung immer eine
   Kombination aus Herstellername UND einem Kategorie-Wort (oder eine
   spezifische Modellreihe wie „galaxy a"). Ein reiner Herstellername wie
   „Doogee" allein wurde nirgends als eigenständiger Hinweis gewertet.
4. **Reine Modellnummern** (`A56-von-Egon`, `F107-Pro`) enthalten **keinen**
   erkennbaren Herstellernamen im Hostnamen selbst - hier ist die
   automatische Erkennung bewusst auf einen bereits von einer anderen
   Quelle bekannten Hersteller angewiesen (siehe „Bekannte Grenzen").

Alle vier Ursachen wurden anhand des tatsächlichen Codes verifiziert
(Python-Simulation der Java-Logik gegen die sechs Hostnamen, siehe
„Durchgeführte Prüfungen").

## Geänderte Android-Regeln

**Ausschließlich `AndroidDeviceClassifier` wurde geändert.**
`DeviceEvidenceEngine` ist unverändert (keine neue Priorität, keine neue
Schwellenwertlogik, kein Android-Wissen darin). `WindowsDeviceClassifier`
und `LinuxDeviceClassifier` sind unverändert.

1. **Leerzeichen-normalisierte Zusatzprüfung** (`haySpaced =
   hay.replace('-', ' ').replace('_', ' ')`) für die reinen
   Kategorie-Wortgruppen in `guessCategory()` - die mDNS-Diensttyp-Muster
   (`_googlecast`, `_androidtvremote`) bleiben unverändert auf dem
   Original-Text, da dort der Unterstrich Teil der Syntax ist.
2. **„Galaxy-Note"** als weiteres Smartphone-Muster ergänzt (war bisher
   nicht enthalten, obwohl im Auftrag explizit gelistet).
3. **Neuer, eigenständiger Hinweis**: Ein im Hostnamen erkannter
   Smartphone-Hersteller (`manufacturerOf(hay)`) zählt jetzt als **eigene
   HOCH-Evidence**, unabhängig davon, ob zusätzlich ein „phone"/„tablet"-
   Schlüsselwort vorhanden ist. Das behebt Fall 3 oben direkt.
4. **Neuer Hinweis**: Ein bereits von einer **anderen** Discoveryquelle
   bekannter Hersteller (`knownManufacturer`-Parameter, sofern ein
   bekannter Smartphone-/Tablet-Hersteller) zählt ebenfalls als HOCH-Evidenz
   - wichtig für reine Modellnummern-Hostnamen (siehe „Kombination mehrerer
   Hinweise").
5. **Neue, vorsichtige MITTEL-Evidenz** für ein generisches
   Modellnummern-Muster (`\b[a-z]\d{2,4}[a-z]?\b`, passt auf „A56", „A02s",
   „S96", „F107" usw.) - **niemals allein ausreichend** (25 von 60 nötigen
   Punkten), ausschließlich unterstützend in Kombination mit einem der
   obigen, stärkeren Hinweise. Genau wie im Auftrag gefordert.
6. **Erweiterte Herstellerliste** in `manufacturerOf()`: Doogee, Oppo,
   Vivo (mit Wortgrenze, um Fehltreffer wie „Vivobook" zu vermeiden),
   Fairphone, Nokia, Asus/Zenfone/ROG Phone ergänzt. „Nothing" wurde
   bewusst **nicht** als bloßes Wort geprüft (zu hohes Risiko für
   Fehltreffer, da „nothing" ein gängiges englisches Wort ist), sondern
   weiterhin nur als „nothing phone"/„nothing-phone".
7. **Kategorie-Fallback**: Liefert `guessCategory()` keine Kategorie, aber
   es liegt ein erkannter/bekannter Smartphone-Hersteller vor, wird als
   Zielkategorie `"📱 Smartphone"` verwendet (statt des generischen
   „Android-Gerät") - genau die im Auftrag geforderte Zielkategorie
   „Smartphones & Tablets" (über die bestehende Report-Kategorisierung).

## Neue Hostname-Muster (Zusammenfassung)

| Hersteller | Erkannte Muster |
|---|---|
| Samsung | `samsung`, `galaxy` (inkl. Galaxy-A/S/Z/Note über Leerzeichen-Normalisierung) |
| Google | `pixel`, `google`, `chromecast` |
| Doogee | `doogee` (**neu**) |
| Motorola | `motorola`, `moto` |
| Xiaomi | `xiaomi`, `redmi`, `poco`, `mi phone` |
| OnePlus | `oneplus` |
| Huawei | `huawei`, `mediapad`, `matepad` |
| Honor | `honor` |
| Oppo | `oppo` (**neu**) |
| Realme | `realme` |
| Vivo | `vivo` (Wortgrenze, **neu**) |
| Nothing | `nothing phone` (unverändert vorsichtig) |
| Fairphone | `fairphone` (**neu**) |
| Nokia | `nokia` (**neu**) |
| Asus | `zenfone`, `asus`, `rog phone` (**neu**) |

## Kombination mehrerer Android-Hinweise

Die sechs Referenzgeräte wurden gegen die neuen Regeln simuliert
(Python-Nachbau der Java-Logik, siehe „Durchgeführte Prüfungen"):

| Gerät | Ergebnis nur aus Hostname | Benötigte Evidence |
|---|---|---|
| Egon-Galaxy-A56-5G | ✅ „Sicher" (Smartphone) | Kategorie-Treffer (HOCH) + Hersteller im Hostnamen (HOCH) + Modellnummer (MITTEL) |
| Galaxy-A02s | ✅ „Sicher" (Smartphone) | wie oben |
| Sebastian-Doogee-S96-Pro | ✅ „Sicher" (Smartphone) | Hersteller im Hostnamen (HOCH) + Modellnummer (MITTEL) |
| Conni-Galaxy-A56-5G | ✅ „Sicher" (Smartphone) | wie Egon-Galaxy |
| A56-von-Egon | ⚠️ nur mit bereits bekanntem Hersteller | bekannter Hersteller (HOCH) + Modellnummer (MITTEL) |
| F107-Pro | ⚠️ nur mit bereits bekanntem Hersteller | bekannter Hersteller (HOCH) + Modellnummer (MITTEL) |

**Vier von sechs** Referenzgeräten werden bereits allein durch den
Hostnamen korrekt erkannt. Die verbleibenden zwei (`A56-von-Egon`,
`F107-Pro`) enthalten **keinerlei** Herstellerhinweis im Hostnamen selbst -
hier bleibt die Erkennung, wie im Auftrag selbst als Grenze beschrieben
(„Modellnummern... dürfen nicht isoliert automatisch als Smartphone
gelten"), auf einen bereits von einer **anderen** Discoveryquelle
bekannten Hersteller angewiesen (`knownManufacturer`-Parameter). Ist der
Hersteller dieser beiden Geräte in der echten Umgebung bereits über eine
andere Quelle (SNMP, Home Assistant, mDNS, vorherige ADB-Verbindung)
bekannt, werden auch sie automatisch erkannt - das wurde ebenfalls
simuliert und bestätigt (siehe „Durchgeführte Prüfungen").

## Access-Point-Korrektur

Unverändert seit 40k34h/i: `applyEvidenceReclassification()` überschreibt
eine automatische „Access Point"-Einstufung, sofern die neue Kategorie
nicht weniger spezifisch ist (beide Kategorien sind „spezifisch" im Sinne
der bestehenden `categorySpecificity()`-Rangfolge) und die Zuordnung nicht
manuell gesperrt ist. Keine Änderung an dieser Logik in 40k34j.

## Ausdrücklich NICHT umgesetzt

- Keine Änderung an `DeviceEvidenceEngine` (unverändert, wie gefordert).
- Keine Verschiebung von Android-Regeln in die generische Engine.
- Kein zusätzlicher ADB-Aufruf, kein zusätzlicher Netzwerkscan.
- Keine neue/erweiterte MAC-OUI-Datenbank, keine Online-/IEEE-Abfrage.
- Keine Änderung an `WindowsDeviceClassifier`/`LinuxDeviceClassifier`.
- Keine neue Merge- oder Integritätslogik.
- „Nothing" wurde bewusst **nicht** als bloßes Einzelwort ergänzt (siehe
  oben) - eine Erweiterung wäre unsicher gewesen.

## Bekannte Grenzen

- **`A56-von-Egon` und `F107-Pro` werden nicht allein durch den Hostnamen
  erkannt** (siehe oben) - dies ist eine bewusste, im Auftrag selbst
  vorgegebene Grenze („Modellnummern dürfen nicht isoliert automatisch als
  Smartphone gelten"), keine übersehene Lücke. Die Erkennung dieser beiden
  Geräte hängt in der Praxis davon ab, ob der jeweilige Hersteller bereits
  über eine andere, bereits vorhandene Discoveryquelle bekannt ist.
- Das Modellnummern-Muster (`\b[a-z]\d{2,4}[a-z]?\b`) ist bewusst generisch
  gehalten und könnte theoretisch auch bei anderen, nicht-Android-Geräten
  zufällig zutreffen (z.B. ein Drucker „M404"). Da dieses Muster **niemals
  allein** ausreicht (nur MITTEL, 25 von 60 nötigen Punkten), besteht dabei
  kein Risiko einer eigenständigen Fehlklassifizierung.
- „Vivo" wurde mit Wortgrenze abgesichert, bleibt aber ein potenziell
  mehrdeutiges Wortfragment in Kombination mit anderen Sprachen/Begriffen -
  auch hier greift der Schutz, dass ein einzelner HOCH-Treffer zwar für
  sich genommen die Schwelle erreicht (60 Punkte), aber niemals eine
  bereits spezifischere, andere Kategorie überschreibt.

## Durchgeführte statische Prüfungen

**Es konnte in dieser Sandbox weiterhin kein echter Maven-/Gradle-Build
durchgeführt werden** (kein `javac`, kein `mvn`, kein `gradle` verfügbar) -
dies wird ausdrücklich benannt. Stattdessen:

- Klammernbilanz der geänderten Datei (`AndroidDeviceClassifier.java`)
  geprüft (ausgeglichen).
- Automatisierter Abgleich aller `record Name(...)`-Deklarationen gegen
  alle `new Name(...)`-Konstruktor-Aufrufe im gesamten betroffenen
  Dateikreis (inkl. der unveränderten `DeviceEvidenceEngine`,
  `WindowsDeviceClassifier`, `LinuxDeviceClassifier`,
  `DiscoveryRegistrationRepository`, `DeviceDiscoveryService`) - keine
  Abweichungen gefunden.
- **Python-Nachbau der tatsächlichen Java-Logik** (`guessCategory()`,
  `manufacturerOf()`, `classifyWithEvidence()`, `DeviceEvidenceEngine.
  classify()`-Schwellenwertformel) und Simulation gegen alle sechs
  Referenz-Hostnamen sowie gegen die beiden Grenzfälle mit zusätzlich
  bereits bekanntem Hersteller - Ergebnis siehe Tabelle oben, exakt
  reproduzierbar.
- Manuelle Prüfung, dass `guessCategory()` für bereits eindeutig
  spezifische Kategorien (Android TV, Fire TV, Google TV, E-Book Reader,
  Kiosk/Display) unverändert VOR den neuen Smartphone-Regeln geprüft wird
  - keine Regressionsgefahr für bereits korrekt erkannte Nicht-Smartphone-
  Android-Geräte.
- Manuelle Prüfung auf Nullwerte in allen geänderten Codepfaden
  (`nullToEmpty()` konsequent verwendet, `knownManufacturer`-Parameter
  wird vor der Listenprüfung auf `null` geprüft).

```text
fachlich umgesetzt
statisch geprüft (inkl. Logik-Simulation gegen alle sechs Referenzgeräte)
nicht durch echten Build verifiziert
```

Der reale Build erfolgt anschließend lokal bei Sebastian.
