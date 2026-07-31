# Schritt 40k34i – Evidence-Modell für Windows und Linux nachrüsten

## Ausgangsprüfung (vor jeder Neuentwicklung durchgeführt)

Wie im Auftrag verlangt, wurde zuerst geprüft, welche Windows-/Linux-
Klassifizierungslogik bereits existiert:

- **Windows:** Es gab **keine eigenständige Windows-Klassifizierungsklasse**.
  Die Erkennung geschieht implizit innerhalb von
  `WindowsInventoryDiscoveryService` (lokale WMI-/PowerShell-Abfragen),
  registriert direkt und fest als Kategorie `"Windows-PC"` - die einzige im
  Projekt tatsächlich verwendete Windows-Kategorie. Es existiert **keine**
  separate „Windows-Server"/„Laptop"/„Workstation"-Kategorie im Code.
- **Linux:** Es gibt bereits eine sehr differenzierte **Unterkategorie**-
  Entscheidung in `LinuxNetworkDiscoveryService.classify()` (Server/Client/
  Container-Host/Virtualisierung/Einplatinencomputer/…) - diese läuft aber
  erst, **nachdem** Linux durch einen bestehenden Trigger (Hostname,
  SSH+Cockpit, SNMP-Hinweis) bereits bestätigt wurde. Es gab **keine**
  eigenständige „Ist das überhaupt Linux?"-Klassifizierungsklasse für davor
  liegende, noch generisch eingestufte Geräte.

**Ergebnis:** Da in beiden Fällen keine geeignete, eigenständige
Classifier-Struktur vorhanden war (nur in größere Inventarisierungs-Services
eingebettete Logik), wurden - wie im Auftrag als Rückfalloption vorgesehen
(„Nur neue Klassen anlegen, wenn im bestehenden Projekt keine geeignete
Struktur vorhanden ist") - zwei neue, schlanke Klassen nach dem Vorbild von
`AndroidDeviceClassifier` angelegt: `WindowsDeviceClassifier` und
`LinuxDeviceClassifier`. Beide enthalten **ausschließlich** die neue
Evidence-Auswertungsmethode `classifyWithEvidence(...)` - sie ersetzen und
verändern die bestehende, bereits funktionierende Erst-Klassifizierung in
`WindowsInventoryDiscoveryService`/`LinuxNetworkDiscoveryService` **nicht**.

## Geänderte und neue Klassen

| Klasse | Änderung |
|---|---|
| `WindowsDeviceClassifier` | **neu** - `classifyWithEvidence(currentCategory, protocol, hostname)` |
| `LinuxDeviceClassifier` | **neu** - `classifyWithEvidence(currentCategory, protocol, hostname)` |
| `DeviceEvidenceEngine` | **unverändert** - dient jetzt als gemeinsame Grundlage für drei Plattformen (Android, Windows, Linux), enthält weiterhin kein Plattform-Sonderwissen |
| `AndroidDeviceClassifier` | **unverändert** (siehe unten zur einzigen generischen Korrektur) |
| `DiscoveryRegistrationRepository.applyEvidenceReclassification()` | **korrigiert** - neue Spezifitäts-Schutzregel (siehe unten), gilt für alle Plattformen gleichermaßen |
| `DeviceDiscoveryService` | Nachklassifizierungs-Schritt aus 40k34h generalisiert: wertet jetzt Android-, Windows- **und** Linux-Evidence im selben Durchlauf aus, mit Konfliktbehandlung |

## Die eine generische Korrektur (betrifft auch Android)

Bei der Umsetzung fiel auf: `applyEvidenceReclassification()` (40k34h)
prüfte bisher nur, ob sich die neue Kategorie von der alten unterscheidet -
nicht aber, ob sie **spezifischer oder weniger spezifisch** ist. Für Windows/
Linux ist das Risiko einer ungewollten **Herabstufung** viel konkreter als
bei Android (z.B. ein bereits als „Server / Linux-System" erkanntes Gerät
dürfte niemals durch die generische Bestätigung „Computer / Linux"
zurückgestuft werden). Es wurde daher eine kleine, generische Korrektur
ergänzt: `applyEvidenceReclassification()` nutzt jetzt zusätzlich die
bereits bestehende `categorySpecificity()`-Rangfolge (dieselbe, die auch bei
der Discovery-Konsolidierung verwendet wird) als Schutzregel - eine
automatische Nachklassifizierung darf eine bereits spezifischere Kategorie
niemals durch eine weniger spezifische ersetzen. Diese Korrektur gilt
**für alle Plattformen einschließlich Android**, wie im Auftrag ausdrücklich
als Ausnahme erlaubt („Android bleibt unverändert, außer falls eine kleine
generische Korrektur zwingend nötig ist").

## Windows-Evidence (nur bereits vorhandene Daten)

| Stufe | Bereits vorhandenes Signal |
|---|---|
| Sehr hoch | Protokolltext beginnt bereits mit „Windows-Inventarisierung" (erfolgreiche WMI-/PowerShell-Abfrage lief bereits); bereits erfasstes „Edition"-Detailfeld nennt Windows 10/11/Server |
| Hoch | bereits vorhandener SMB-/NetBIOS-Hinweis; bereits erfasstes „Domäne/Arbeitsgruppe"-Detailfeld; bereits erfasstes „Windows-Rollen/Features"-Detailfeld |
| Mittel | Windows-typisches Hostnamen-Muster (`desktop-`, `win-`, `pc-`, `-pc`) |
| Niedrig | (aktuell nicht separat verwendet - ein einzelner unspezifischer Port allein wurde bewusst nicht als Evidence codiert, siehe Auftrag: „Ein einzelner Port darf Windows nicht eindeutig bestätigen") |

Zielkategorie: ausschließlich `"Windows-PC"` (die einzige vorhandene).

## Linux-Evidence (nur bereits vorhandene Daten)

| Stufe | Bereits vorhandenes Signal |
|---|---|
| Sehr hoch | Protokolltext enthält bereits „Linux bestätigt: ja" (bestehende Linux-Nachprüfung lief bereits erfolgreich); bereits erfasste „Distributions-ID"/„Kernel"-Felder; SNMP-Systembeschreibung nennt bereits wörtlich „Linux" (seit 40k33b9 gespeichert) |
| Hoch | SSH erreichbar **zusammen mit** einem weiteren Linux-Hinweis (Paketmanager apt/dnf/yum/zypper/pacman/apk bereits erkannt, systemd/Init-System-Feld vorhanden, oder Protokolltext nennt bereits „linux"); bereits erkannter Paketmanager allein |
| Mittel | Linux-typisches Hostnamen-Muster (`linux-`, „ubuntu", „debian", „raspberrypi") |
| Niedrig | SSH allein erreichbar, ohne jeden weiteren Hinweis |

**Wichtig, wie im Auftrag gefordert:** SSH allein erreicht nur die niedrige
Stufe und reicht für sich genommen **nie** für eine Nachklassifizierung aus
(die Schwelle in `DeviceEvidenceEngine` verlangt entweder einen SEHR_HOCH-
Treffer oder mindestens 60 Gewichtungspunkte - ein einzelner NIEDRIG-Treffer
mit 10 Punkten reicht nicht).

Zielkategorie bei ausreichender, aber nicht subkategorie-spezifischer
Evidence: die bereits bestehende, konservative Standardkategorie
`"Computer / Linux"` aus `LinuxNetworkDiscoveryService` - keine neue
Kategorie erfunden.

## Konfliktbehandlung zwischen Plattformen

Der generalisierte Nachklassifizierungs-Schritt in
`DeviceDiscoveryService` wertet für jedes bereits registrierte, in diesem
Suchlauf gesehene Gerät **alle drei** Classifier aus (Android, Windows,
Linux). Liefert **mehr als einer** von ihnen ein Ergebnis für dasselbe
Gerät (z.B. SSH **und** SMB gleichzeitig erreichbar), wird **keine**
automatische Entscheidung getroffen - der Fall wird protokolliert
(`log.info(...)` mit allen drei Ergebnissen) und im Diagnose-Ereignis
`EVIDENCE_RECLASSIFICATION` als „widersprüchlicher Fall" mitgezählt, aber
die bestehende Kategorie bleibt unverändert. Es wurde **keine** neue Merge-
oder Integritätsarchitektur dafür gebaut - die Konfliktprüfung ist eine
einfache Zähl-Bedingung innerhalb desselben, bereits bestehenden
Nachklassifizierungs-Durchlaufs.

## Schutz manueller Zuordnungen

Unverändert seit 40k34h: `applyEvidenceReclassification()` schreibt nur,
wenn `manual_device_type=FALSE` (SQL-`WHERE`-Bedingung), und setzt selbst
niemals ein manuelles Flag - eine manuell zugeordnete Kategorie bleibt
also für **alle drei** Plattformen vollständig geschützt.

## Einordnung in die Discovery-Pipeline

Keine zweite, parallele Nachklassifizierungs-Pipeline. Der bereits in
40k34h eingeführte Schritt (zwischen Reverse-DNS-Auflösung und
Konsolidierung) wurde direkt erweitert, nicht dupliziert. Kein
zusätzlicher Netzwerkscan, kein zusätzlicher SSH-/WMI-/WinRM-/ADB-Aufruf -
alle drei Classifier werten ausschließlich bereits im Suchlauf gesammelte
`DiscoveredDevice`-Daten und bereits gespeicherte Identitätsfelder aus.

## UI

**Keine Frontend-Änderungen.** Wie bei Android (40k34h) läuft die neue
Kategorie über dasselbe, bereits generisch angezeigte `device_type`-Feld
und dieselbe, bereits bestehende Änderungshistorie
(`gam_discovery_device_type_history`) - beides zeigt Windows-/Linux-
Nachklassifizierungen automatisch identisch zu Android an.

## Nicht Bestandteil (wie im Auftrag)

Keine neuen Scanner, keine neuen Netzwerkprotokolle, keine neue
Inventarisierung, keine neuen Reports, keine Merge-Änderungen, keine
Softwareverteilung/Paketverwaltung, keine macOS-/iOS-Erkennung, keine
große Erweiterung der MAC-OUI-Listen (unverändert seit 40k34h), keine neue
UI, keine neue Datenbankmigration.

## Rückwärtskompatibilität

- Ein bereits als „Server / Linux-System" (oder jede andere rang-2-
  Kategorie) erkanntes Gerät kann durch die neue Spezifitäts-Schutzregel
  **nicht mehr** versehentlich auf eine generischere Kategorie
  zurückgestuft werden - weder durch Windows-, Linux- noch Android-Evidence.
- Ein bereits als „Windows-PC" registriertes Gerät bleibt „Windows-PC"
  (keine neue Kategorie, keine Änderung der bestehenden
  `WindowsInventoryDiscoveryService`-Logik).
- Manuelle Zuordnungen bleiben vollständig geschützt (unverändert).
- Ein einzelnes schwaches Merkmal (SSH allein, ein einzelner Port) löst
  weiterhin **keine** Umklassifizierung aus.

## Bekannte Grenzen

- Die MITTEL-Stufe wird bei Windows/Linux etwas gröber behandelt als im
  Auftrag beispielhaft beschrieben (z.B. „typische Windows-Portkombination"
  wurde nicht als eigene Evidence-Regel umgesetzt, da im Projekt aktuell
  keine generische, bereits gespeicherte Portkombinations-Auswertung
  existiert, die ohne zusätzliche Abfrage nutzbar wäre) - lieber
  ausgelassen als eine neue, ungeprüfte Heuristik einzuführen.
  „Hersteller-/modellbezogene Hinweise" für Windows wurden aus demselben
  Grund nicht ergänzt.
- Ein bereits bestehendes, geringfügiges Detail aus 40k34h/i: An mehreren
  Stellen wird `String.valueOf(map.getOrDefault("protocol", ""))`
  verwendet; falls der Schlüssel `"protocol"` in der Datenbank-Zeile mit
  dem Wert `NULL` (statt fehlendem Schlüssel) vorliegt, liefert
  `getOrDefault` diesen `null`-Wert unverändert zurück, und
  `String.valueOf(null)` erzeugt dann die Zeichenkette `"null"` statt einer
  leeren Zeichenkette. Das ist **kein neues Problem dieses Schritts**
  (identisches Verhalten bereits seit 40k34h), führt aber zu keinem
  Fehlverhalten, da die Zeichenkette „null" keines der verwendeten
  Schlüsselwort-Muster auslöst - wird hier der Vollständigkeit halber
  benannt, nicht verschwiegen.
- Keine Erkennung von macOS/NAS/anderen Unix-Systemen, die ebenfalls SSH
  anbieten könnten und fälschlich als „schwaches Linux-Indiz" auffallen
  könnten - dies wird durch die bewusst hohe Schwelle (SSH allein reicht
  nicht) bereits entschärft, aber nicht vollständig ausgeschlossen.

## Durchgeführte statische Prüfungen

**Es konnte in dieser Sandbox weiterhin kein echter Maven-/Gradle-Build
durchgeführt werden** (kein `javac`, kein `mvn`, kein `gradle` verfügbar) -
dies wird hiermit ausdrücklich und ehrlich benannt. Stattdessen:

- Klammernbilanz aller sechs geänderten/neuen Backend-Dateien geprüft
  (ausgeglichen).
- Automatisierter Abgleich aller `record Name(...)`-Deklarationen gegen
  alle `new Name(...)`-Konstruktor-Aufrufe im gesamten Package - diesmal
  mit einer **verbesserten** Prüflogik, die mehrere gleichnamige Records in
  unterschiedlichen Klassenkontexten korrekt toleriert (Lehre aus 40k34g/h):
  ein Aufruf gilt als korrekt, wenn er zu **mindestens einer** der
  bekannten Deklarationen mit demselben Namen passt. Ergebnis: keine echten
  Abweichungen gefunden.
- Kreuzabgleich aller neuen Methodensignaturen zwischen Aufrufer
  (`DeviceDiscoveryService`) und den drei Classifiern.
- Manuelle Durchsicht auf Nullwerte in allen neuen Codepfaden (siehe
  „Bekannte Grenzen" für die eine gefundene, unkritische Vorbedingung).
- Manuelle Prüfung, dass kein neuer Netzwerk-/SSH-/WMI-/ADB-Aufruf
  ergänzt wurde (alle drei neuen `classifyWithEvidence()`-Methoden nehmen
  ausschließlich bereits übergebene Strings entgegen).
- Manuelle Prüfung der Idempotenz: `applyEvidenceReclassification()` bricht
  sofort ab, wenn die neue Kategorie der alten entspricht oder weniger
  spezifisch ist - ein wiederholter Suchlauf mit unveränderten Daten
  schreibt daher nichts erneut.

```text
fachlich umgesetzt
statisch geprüft
nicht durch echten Build verifiziert
```

Der reale Build erfolgt anschließend lokal bei Sebastian.

## Testanleitung (noch nicht durchgeführt, als Checkliste gedacht)

1. Ein Gerät mit bereits erfolgreicher Windows-WMI-Inventarisierung, aber
   generischer Ausgangskategorie: muss auf „Windows-PC" wechseln.
2. Ein Gerät mit bereits vorhandener SNMP-Systembeschreibung „Linux …":
   muss auf „Computer / Linux" wechseln, sofern noch generisch eingestuft.
3. Ein Gerät, das nur SSH anbietet, sonst keine Linux-Hinweise: darf
   **nicht** umklassifiziert werden.
4. Ein Gerät mit sowohl SSH als auch SMB erreichbar: darf **nicht**
   automatisch entschieden werden; Konflikt muss geloggt werden.
5. Ein manuell als „Router" zugeordnetes Gerät mit schwachen Linux-
   Hinweisen: manuelle Zuordnung muss unverändert bleiben.
6. Ein bereits als „Server / Linux-System" erkanntes Gerät: darf durch
   generische Linux-Bestätigung **nicht** auf „Computer / Linux"
   zurückgestuft werden.
7. Zweiter, unveränderter Suchlauf desselben Geräts: darf keine erneute
   Änderungshistorie-Zeile erzeugen (Idempotenz).
