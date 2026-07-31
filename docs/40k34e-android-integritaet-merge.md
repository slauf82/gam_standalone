# Schritt 40k34e – Android: Integrität und Merge

## Einordnung in den Plattform-Workflow

40k34e integriert Android vollständig in die **eine** zentrale
Geräteidentitäts-, Integritäts- und Merge-Architektur, die bereits für
Windows und Linux verwendet wird. Es gibt **keine** zweite Android-Identität,
**kein** separates Android-Merge-System, **keine** parallele
Integritätsprüfung - jede Erweiterung erfolgt additiv in den bereits
bestehenden zentralen Klassen.

## Zuerst durchgeführte Bestandsaufnahme

Wie im Auftrag verlangt, wurde vor jeder Neuentwicklung geprüft, was bereits
existiert:

- **Zentrale Konflikt-/Konfidenzbewertung:** `DeviceIdentityConfidenceEngine`
  (seit 40k31c) - vergleicht MAC, Seriennummer, Name, IP, Hersteller, Typ und
  liefert Score, Entscheidung (`AUTO_MERGE`/`POSSIBLE_DUPLICATE`/`DISTINCT`)
  sowie eine `conflicts`-Liste.
- **Zentrale Schweregrad-Klassifizierung:** `DiscoveryRegistrationRepository.
  conflictSeverity()`/`highestConflictSeverity()`/`criticalConflictMessage()`
  (seit 40k33b10) - ordnet Konflikttexte in 🔴 KRITISCH / 🟠 HOCH /
  🟡 PRÜFHINWEIS ein und wird bereits identisch von Merge-Kandidatenliste,
  Merge-Vorschau **und** der Integritätsprüfung verwendet.
- **Zentraler Feldvergleich:** `DiscoveryRegistrationRepository.
  compareIdentityFields()` (seit 40k33b10, um SSH-Hostkey/Build-Fingerprint
  bereits in 40k34b erweitert) - EINE gemeinsame Vergleichstabelle für Merge
  und Integrität.
- **Zentrale Kandidatenermittlung/Vorschau/Bestätigung:**
  `DeviceMergeService.findCandidates()`/`preview()`/`confirm()`.
- **Zentrale Integritätsprüfung:** `DeviceIdentityService.checkIntegrity()`.
- **Zentrale Merge-Bestätigungssperre (40k33b10):** Checkbox „Konflikt
  bewusst geprüft“, deaktivierter Button bei kritischer Schutzregel.

**Ergebnis:** Alle geforderten Mechanismen existierten bereits zentral. Für
40k34e war deshalb **keine neue Klasse** erforderlich - ausschließlich
gezielte, additive Erweiterungen der oben genannten, bereits bestehenden
Dateien.

## Wiederverwendete Infrastruktur (keine Duplizierung)

| Anforderung aus dem Auftrag | Bereits vorhandene, wiederverwendete Stelle |
|---|---|
| Severity-Klassifizierung | `conflictSeverity()`/`highestConflictSeverity()` |
| Konflikttexte/Score | `DeviceIdentityConfidenceEngine.assess()` |
| Feldvergleich in Vorschau/Integrität | `compareIdentityFields()` |
| Merge-Kandidaten | `DeviceMergeService.findCandidates()` |
| Merge-Vorschau/-Bestätigung | `DeviceMergeService.preview()`/`confirm()` |
| Merge-Bestätigungssperre | bestehende Checkbox/Button-Logik aus 40k33b10 |
| Integritätsprüfung/-anzeige | `DeviceIdentityService.checkIntegrity()` |
| App-/Laufhistorie | `AppInventoryRepository` (40k34c) |

## Starke, unterstützende und schwache Identitätsmerkmale

**Stark** (dürfen automatisch zusammenführen):
- Seriennummer, sofern über eine als „hardwarenah“ erkannte Quelle gemeldet.
  `DeviceIdentityConfidenceEngine.isHardwareSerial()` wurde um das
  Schlüsselwort **„adb“** ergänzt - eine über ADB gemeldete Seriennummer
  (`ro.serialno`) erhält damit dieselbe hohe Gewichtung wie eine per
  SSH/WMI/WinRM/Proxmox/BIOS gemeldete Seriennummer, statt der schwächeren
  generischen Geräte-ID-Gewichtung.
- MAC-Adresse (bereits zentral vorhanden, unverändert).
- Bereits bestätigte frühere Zuordnung (Alias-/Merge-Historie, bereits
  zentral vorhanden).

**Unterstützend** (verstärken, entscheiden aber nicht allein):
- Build-Fingerprint (neu ausgewertet, siehe unten), Hersteller, Modell,
  Produktname, Android-Version - fließen wie bei jeder anderen Plattform nur
  über die bestehenden Gewichtungen (`manufacturerWeight`, `typeWeight`,
  Namensvergleich) ein.

**Schwach** (dürfen niemals allein einen automatischen Merge auslösen):
IP-Adresse, Gerätename, installierte Apps, laufende Prozesse, Dienste,
Rollen, Akkustand, Bildschirmzustand, Netzwerkstatus. Für all diese gilt
unverändert die bereits bestehende zentrale Regel „IP-Adresse allein mergt
nie“ (`ipNeverMergesAlone`) - Apps/Prozesse/Dienste/Rollen waren und sind an
**keiner** Stelle Eingabe der Konfidenzberechnung.

## Neue Android-spezifische Integritätsregeln

### Kritisch (🔴)
- **Bereits vorhanden, jetzt für Android nutzbar:** abweichende
  Seriennummer bei gemeinsamer ADB-Kennung (durch die `isHardwareSerial()`-
  Erweiterung erkennt die zentrale Engine dies jetzt zuverlässig als
  kritischen Konflikt, exakt wie bei einer widersprüchlichen SSH-Seriennummer
  bei Linux).
- **Neu:** Zwei unterschiedliche Geräteidentitäten sind aktuell mit
  derselben ADB-Verbindung (Host+Port) verknüpft - ein technisch
  unplausibler 1:n-Zustand (`DiscoveryRegistrationRepository.
  otherIdentitiesWithSameAdbConnection()`, reine Datenbankabfrage).

### Hoch (🟠)
- **Neu:** Abweichender Build-Fingerprint trotz bereits übereinstimmender
  starker Identität (MAC oder Seriennummer) - erweitert direkt
  `DeviceIdentityConfidenceEngine.assess()`, blockiert einen automatischen
  Merge über eine eigenständige, additive Score-Deckelung.
- **Neu:** ADB-Verbindung ist einem Datensatz zugeordnet, dessen Plattform
  eindeutig nicht Android ist.
- **Neu:** Ein vollständiger App-Inventarlauf meldet 0 Pakete, obwohl ein
  früherer vollständiger Lauf desselben Geräts Pakete enthielt (Vergleich
  ausschließlich anhand der bereits gespeicherten Lauf-Historie aus
  40k34c, `AppInventoryRepository.recentRuns()` - kein neuer ADB-Aufruf).
- **Neu:** Ein App-Inventarlauf ist als vollständig markiert, obwohl
  grundlegende Systemdaten (Android-Version) im Geräteprotokoll vollständig
  fehlen.

### Informativ (🟡)
Alle im Auftrag genannten informativen Fälle (ADB nicht verfügbar, noch
nicht per ADB inventarisiert, teilweiser/abgebrochener Lauf, unbekannte
Android-Version, fehlende Rollen, fehlender Anzeigename usw.) waren
**bereits durch die bestehenden 40k34a–d-Statusfelder abgedeckt**
(„Inventarisierungsstatus“, „App-Inventarisierungsstatus“, neutrale
ADB-Statusanzeige) und mussten nicht dupliziert werden.

## Vollständigkeit und Aktualität

Unverändert seit 40k34c gilt: `AppInventoryRepository.markRemoved()` wird
**ausschließlich** nach einem als `VOLLSTAENDIG` bewerteten Lauf aufgerufen;
ein teilweiser oder fehlgeschlagener Lauf löscht keine Bestandsdaten. 40k34e
ergänzt darauf aufbauend die Kontrollregel „vollständiger Lauf ohne Pakete
trotz vorheriger Bestände“ (siehe oben) - genau der im Auftrag beschriebene
Fall einer möglichen Fehlmeldung, die **nicht** unbesehen als tatsächliche
Änderung interpretiert werden darf. „Nicht ermittelbar“ (Feld fehlt im
Protokoll) und „nicht vorhanden“ (Feld existiert mit explizitem Wert) waren
bereits durch die bestehende `putIfPresent()`-Logik der Android-Parser
(40k34b–d: nur tatsächlich gelieferte Werte werden geschrieben) sauber
getrennt - hierfür war keine Änderung nötig.

## Merge-Kandidaten und -Ausschlüsse

Da 40k34e ausschließlich die bereits zentrale Engine erweitert, werden
Android-Geräte automatisch mit derselben Logik wie jede andere Plattform in
`findCandidates()` bewertet - keine gesonderte Android-Kandidatenliste. Die
im Auftrag beispielhaft beschriebene Gewichtung (sehr stark/stark
unterstützend/schwach) entspricht der bereits bestehenden
Gewichtungsstruktur (`macWeight`, `hardwareSerialWeight`, `hostnameWeight`,
`ipWeight`, `manufacturerWeight`, `typeWeight`) - für Android wurde
ausschließlich die Zuordnung der ADB-Seriennummer zur „hardwarenahen“
(stärksten) Gewichtungsstufe ergänzt.

**Blockiert werden** (automatisch, über die zentrale Engine/Klassifizierung):
unterschiedliche starke Identitäten (Seriennummer/MAC), gleichzeitig
abweichende Build-Fingerprints trotz gemeinsamer starker Identität,
gleichzeitig erreichbare unterschiedliche IP-Adressen (bereits zentral seit
40k33b6a) sowie - neu erkennbar - zwei Identitäten mit derselben aktiven
ADB-Verbindung.

## Merge-Vorschau

Da `MergePreviewResult.fieldComparison` und `Candidate.fieldComparison`
bereits dieselbe `compareIdentityFields()`-Methode verwenden, die in 40k34e
um **Android-Version**, **ADB-Verbindung** (Host:Port) und **letzter
ADB-Kontakt** erweitert wurde, erscheinen diese Angaben **automatisch, ohne
jede Frontend-Änderung** in der bestehenden Merge-Vorschau- und
Kandidatentabelle - dieselbe generische Zeilendarstellung
(`{f.label}`/`{f.valueA}`/`{f.valueB}`) rendert jede neue Zeile bereits.

**Bewusst nicht umgesetzt:** die Anzahl installierter Apps und der Zeitpunkt
des letzten vollständigen App-Laufs wurden **nicht** zusätzlich in die
Vorschau-Tabelle aufgenommen. Das hätte eine zusätzliche Datenbankabfrage
aus `DeviceMergeService` heraus erfordert (dort bislang keine
`AppInventoryRepository`-Abhängigkeit für Anzeigezwecke) und war angesichts
des Zeitrahmens dieses Schritts nicht mit der gebotenen Sorgfalt umsetzbar.
Das entspricht der ausdrücklichen Vorgabe „nicht die komplette App-Liste
laden“ - hier wurde bewusst konservativ **gar keine** App-Zahl ergänzt,
statt eine unsicher getestete Abfrage einzubauen. Dies wird als bekannte
Lücke benannt, nicht verschwiegen.

## Datenübernahme beim Merge / App-Inventar beim Merge

`AppInventoryRepository.reassignToTarget()` (neu) wird am Ende von
`DeviceMergeService.confirm()` für jedes Quellgerät aufgerufen:

- **Inventarläufe** (`gam_discovery_app_inventory_runs`) werden unverändert
  auf die Zielidentität umgehängt - Zeitstempel, Status und Zusammenfassung
  jedes einzelnen Laufs bleiben erhalten, die Herkunft jedes Laufs bleibt
  damit nachvollziehbar.
- **Aktuelle Paketdatensätze** (`gam_discovery_installed_apps`): existiert
  für ein Paket noch kein Datensatz am Zielgerät, wird der Datensatz einfach
  umgehängt. Existiert bereits einer, bleibt der **zuletzt gesehene**
  (neuere) Stand erhalten, der ältere wird verworfen - es entsteht dabei
  weder ein doppelter Primärschlüssel noch ein stiller Informationsverlust
  (der verworfene Datensatz beschrieb ohnehin denselben Paketstand, nur zu
  einem früheren Zeitpunkt).
- Bei **zeitgleich widersprüchlichen vollständigen Läufen** (identischer
  Zeitstempel, unterschiedlicher Inhalt) wurde **keine** automatische
  Konflikterkennung ergänzt - dieser sehr seltene Grenzfall wird als
  bekannte Einschränkung benannt (siehe unten), statt eine unsichere
  Sonderregel einzuführen.

## Laufzeitdaten beim Merge

Prozesse, Dienste, Rollen und Gerätezustand (40k34d) werden ausschließlich
im `discovery_protocol`-Text derselben Identität geführt (kein separates
Zeitreihen-Modell) - beim Merge übernimmt die bereits bestehende
`mergeDevices()`-Logik unverändert den zuletzt bekannten Stand des
Zielgeräts bzw. der neuesten Quelle. Es wurde **keine** neue
Historienstruktur für Laufzeitdaten eingeführt, da eine solche bislang auch
für Windows/Linux nicht existiert - konsistent mit „keine Android-
Sonderarchitektur“.

## Vertrauenswürdigkeit der Quellen

Es existiert bereits eine implizite Quellgewichtung über
`serialWeight()`/`isHardwareSerial()` (hardwarenahe Quellen wie
SSH/WMI/Proxmox/BIOS/**ADB** > SNMP > generische Geräte-ID) sowie über die
`categorySpecificity()`-Rangfolge bei der Kategorievergabe. Es wurde
**keine** zusätzliche, parallele Android-Quellrangliste eingeführt - die
bestehende Struktur wurde lediglich um „adb“ ergänzt (siehe oben).

## Integritätsbewertung und Prozentanzeige

Die Statusstufen „Integrität hoch“/„Bitte überprüfen“/„Integritätswarnung“
sowie die Kritikalitäts-Einordnung (🔴/🟠/🟡) aus 40k33b10 wurden
**unverändert** wiederverwendet - lediglich die Anzahl möglicher Konflikte,
die in diese Berechnung einfließen, wurde um die neuen Android-Prüfungen
ergänzt. Ein kritischer Konflikt (z.B. widersprüchliche starke Identität)
führt weiterhin zu „Integritätswarnung“ und der bereits bestehenden roten
Schutzregel-Darstellung, unabhängig davon, wie viele optionale Android-
Felder zusätzlich fehlen.

## Datenschutz

Es wurden keine neuen App-Inhalte, Prozessinhalte oder personenbezogenen
Daten in die Integritäts-/Merge-Prüfung aufgenommen - ausschließlich bereits
gespeicherte technische Metadaten (Seriennummer, Build-Fingerprint,
Android-Version, ADB-Verbindungsdaten, Paketanzahl aus der Lauf-Historie).

## Performance-Grundsatz

Alle neuen Prüfungen arbeiten **ausschließlich auf bereits gespeicherten
Daten** - kein einziger neuer ADB-Aufruf wurde für Integrität, Kandidaten-
ermittlung, Vorschau oder Merge-Bestätigung ergänzt. Die einzige
verbleibende Live-Abfrage in `checkIntegrity()` ist die bereits seit
40k33b7 bestehende SSH-Hostkey-Prüfung für Linux - unverändert, nicht Teil
von 40k34e.

## Logging und Audit

Bestehende Logging-Infrastruktur wiederverwendet:
`DeviceMergeService`/`DeviceIdentityService` protokollieren wie bisher Start/
Ende von Merge-Vorgängen (`log.debug`) und der neuen Android-Inventarläufe
(`log.info`, seit 40k34c). Die neuen Integritätsregeln fließen in die
bereits bestehende `reasons`/`rawConflicts`-Liste ein und werden über die
vorhandene Rückgabe des Integritäts-Endpunkts sichtbar - es wurde kein
zusätzlicher, separater Log-Kanal für Android-Integrität eingeführt.

## Datenbankmigration

Ausschließlich eine additive Erweiterung der bereits bestehenden Nutzung
vorhandener Spalten/Tabellen - **keine neue Tabelle, keine neue Spalte**.
`AppInventoryRepository.reassignToTarget()` nutzt die seit 40k34c
bestehenden Tabellen `gam_discovery_app_inventory_runs`/
`gam_discovery_installed_apps` unverändert; `otherIdentitiesWithSameAdbConnection()`
nutzt die seit 40k34b bestehenden Spalten `adb_host`/`adb_port`.

## Bekannte Einschränkungen (ehrlich benannt)

- **Keine App-Anzahl in der Merge-Vorschau** (siehe oben) - bewusst
  zurückgestellt statt einer unsicher getesteten Erweiterung.
- **Kein automatischer Zeitgleich-Konflikt bei App-Läufen** - der im
  Auftrag genannte Fall „zeitgleiche widersprüchliche vollständige Läufe“
  wird nicht gesondert erkannt.
- **Keine Erkennung** für „bestätigte Benutzerentscheidung würde
  widersprochen“ über die bereits bestehende Alias-/Merge-Historie hinaus -
  es wurde keine neue Bestätigungs-Datenstruktur ergänzt.
- **1:n-ADB-Erkennung nur in der Integritätsprüfung**, nicht zusätzlich als
  eigener Ausschlussgrund innerhalb `findCandidates()` verdrahtet - der
  Zustand ist ohnehin nur durch einen Datenfehler erreichbar, nicht durch
  normale Bedienung.

## Tests / durchgeführte Prüfungen in dieser Umgebung

Kein JDK, kein funktionierendes `npm install` in dieser Sandbox - **kein
echter Build und keine echten Geräte-/Merge-Tests möglich.** Dies wird
hiermit ausdrücklich benannt. Stattdessen geprüft:

- Klammernbilanz aller fünf geänderten Backend-Dateien (ausgeglichen).
- Kreuzabgleich der neuen Methodensignaturen (`reassignToTarget()`,
  `otherIdentitiesWithSameAdbConnection()`) zwischen Aufrufer und
  Definition.
- Manuelle Durchsicht der erweiterten `DeviceIdentityConfidenceEngine.
  assess()`-Logik: die neue Build-Fingerprint-Prüfung wurde bewusst als
  **isolierte, additive** Score-Deckelung ergänzt, ohne die bestehende
  Deckelungsformel für MAC-/Serien-/IP-Konflikte zu verändern (Regressions-
  Vermeidung).
- Bestätigt: `compareIdentityFields()` wird unverändert von Merge-
  Kandidatenliste, Merge-Vorschau **und** Integritätsprüfung genutzt - die
  neuen Zeilen (Android-Version, ADB-Verbindung, letzter ADB-Kontakt)
  erscheinen dadurch konsistent an allen drei Stellen.
- Bestätigt: kein manueller `new DeviceMergeService(...)`-Aufruf existiert
  im Projekt, der durch den geänderten Konstruktor (zusätzlicher
  `AppInventoryRepository`-Parameter) brechen würde - Spring übernimmt die
  Injektion.

**Nicht durchgeführt** (nicht möglich in dieser Sandbox): echter
Maven-Build, jeglicher der in Abschnitt 18 des Auftrags genannten
Identitäts-/Integritäts-/Merge-/UI-Testfälle gegen eine echte Datenbank oder
echte Android-Geräte. Bitte vor dem Produktiveinsatz insbesondere folgende
Szenarien nachholen: zwei Android-Geräte mit identischer ADB-Seriennummer
zusammenführen (muss kritisch blockiert werden, bis bestätigt), ein Gerät
mit abweichendem Build-Fingerprint trotz gleicher Seriennummer, sowie einen
tatsächlichen Merge zweier Android-Geräte mit vorhandener App-Historie
(Läufe/Pakete müssen korrekt und ohne Duplikate im Zielgerät erscheinen).

## Testanleitung

1. Zwei Android-Datensätze mit identischer, über ADB gemeldeter
   Seriennummer als Kandidat anzeigen lassen: muss ❗ KRITISCH zeigen
   (dieselbe Darstellung wie bei einem SSH-Seriennummern-Konflikt bei
   Linux).
2. Zwei Datensätze mit gleicher Seriennummer, aber unterschiedlichem
   Build-Fingerprint: muss 🟠 HOCH zeigen, automatischer Merge blockiert.
3. Ein Datensatz mit `adb_host`/`adb_port` gesetzt, aber Plattform
   ungleich „Android“: Integritätsprüfung muss den neuen HOCH-Konflikt
   zeigen.
4. Zwei Datensätze mit identischem `adb_host`/`adb_port`: Integritätsprüfung
   beider Geräte muss den neuen KRITISCH-Konflikt zeigen.
5. Ein Android-Gerät mit mehreren vollständigen App-Läufen, bei dem der
   neueste vollständige Lauf 0 Pakete liefert: HOCH-Konflikt muss
   erscheinen.
6. Merge zweier Android-Geräte mit App-Historie durchführen: Ziel-Identität
   muss anschließend alle Läufe beider Geräte sowie den jeweils neuesten
   Stand je Paket enthalten, keine doppelten Paketzeilen.
7. Regressionstest: bestehende Windows-/Linux-Integritäts- und
   Merge-Szenarien aus 40k33b7/b10 unverändert funktionsfähig.

## Abgrenzung zu 40k34f

40k34e umfasst ausschließlich Integrität, Konflikterkennung,
Qualitätsbewertung, Merge-Kandidaten, -Vorschau, -Schutz, -Ausführung und
Auditierung. **Keine** neuen Android-Reports, Dashboards, PDF-Berichte,
CSV-/Excel-Exporte oder Managementübersichten - diese folgen vollständig in
40k34f.
