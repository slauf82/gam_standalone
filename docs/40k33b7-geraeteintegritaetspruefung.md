# Schritt 40k33b7 – Geräteintegritätsprüfung und Wiederauftrennung

## Ziel

Nicht jede Zusammenführung bleibt dauerhaft korrekt - neue Discoverydaten
können bestehende Zusammenführungen infrage stellen. 40k33b7 ergänzt deshalb
eine kontinuierliche, ausschließlich informative Integritätsprüfung sowie ein
geführtes Werkzeug zur manuellen Wiederauftrennung. **Es wird nie automatisch
etwas aufgetrennt** - die Entscheidung bleibt immer beim Benutzer.

## Geänderte/neue Dateien

Backend:
- `backend/.../inventory/DeviceMergeRepository.java` – bestehendes Merge-Log
  additiv um `action` (MERGE/SPLIT) und `source_snapshot` (JSON) erweitert;
  neue Methoden `logSplit()`, `sourceSnapshotsForTarget()`.
- `backend/.../inventory/DeviceMergeService.java` – `confirm()` sichert vor
  dem eigentlichen Merge einen JSON-Schnappschuss der gleich zu löschenden
  Quell-Datensätze. Die Merge-Entscheidungslogik selbst wurde NICHT verändert.
- `backend/.../inventory/DiscoveryRegistrationRepository.java` – neue
  öffentliche Hilfsmethoden `detailValues()` (Wiederverwendung der bestehenden
  Protokoll-Parsing-Logik) und `resolveKey()` (Wiederverwendung der
  bestehenden Schlüsselberechnung aus `register()`).
- `backend/.../inventory/LinuxNetworkDiscoveryService.java` – erfasst jetzt
  zusätzlich den SSH-Hostkey-Fingerabdruck; neue öffentliche Methode
  `currentHostKeyFingerprint()` für den erneuten Live-Abgleich.
- `backend/.../inventory/DeviceIdentityService.java` – neue Methoden
  `checkIntegrity()`, `splitCandidates()`, `split()`.
- `backend/.../inventory/DeviceIdentityController.java` – neue Endpunkte
  `/integrity`, `/split-candidates`, `/split`.

Frontend:
- `frontend/src/api/client.ts` – neue Typen/Funktionen für Integritäts-
  prüfung und Wiederauftrennung.
- `frontend/src/main.tsx` – neue Komponenten `IntegritySection` (einklapp-
  barer Bereich im Dialog „Geräteidentität“) und `DeviceSplitDialog`
  (geführte Wiederauftrennung), neuer Button „Wiederauftrennung“.
- `frontend/src/style.css` – Hervorhebung widersprüchlicher Tabellenzeilen.

Dokumentation:
- `docs/40k33b7-geraeteintegritaetspruefung.md` (diese Datei).

## Architekturentscheidungen

- **Keine zweite Identitäts-/Mergeverwaltung:** Die Integritätsprüfung nutzt
  ausschließlich die bereits vorhandene Kandidaten-/Konflikterkennung
  (`DeviceMergeService.findCandidates()`, dieselbe Engine, die auch während
  eines Suchlaufs über automatische Zusammenführungen entscheidet). Es wurde
  keine zweite Bewertungslogik gebaut - lediglich eine neue Fragestellung an
  dieselbe Engine gerichtet: "gibt es aktuell einen harten Konflikt (🔴) mit
  einem anderen bereits registrierten Gerät?"
- **Keine neue Datenbank:** Die einzige Migration ist eine additive
  Erweiterung des bereits in 40k33b4 eingeführten Merge-Logs (zwei neue
  Spalten). Keine neue Tabelle.
- **SSH-Hostkey als zusätzliches, optionales Signal:** Nur bei Linux-Geräten
  mit konfiguriertem gemeinsamem SSH-Zugang (unverändert seit 40k33b3/b6a).
  Ein geänderter Hostkey ist ein sehr starkes technisches Indiz für ein
  anderes physisches Gerät und zählt daher allein bereits als ein Konflikt.
- **Keine automatische Entscheidung:** Weder die Integritätsprüfung noch die
  Wiederauftrennung verändern von sich aus etwas. Die Prüfung liefert
  ausschließlich Status, Begründungen und eine tabellarische
  Gegenüberstellung; die Wiederauftrennung erfordert in jedem Fall eine
  ausdrückliche Bestätigung durch den Benutzer (inkl. JavaScript-
  Sicherheitsabfrage vor der endgültigen Ausführung).
- **Lazy Loading, konsistent mit 40k33b6b:** Die Integritätsprüfung wird
  ebenfalls erst beim Öffnen des einklappbaren Bereichs ausgeführt, nicht
  automatisch beim Öffnen des Dialogs.

## Integritätsprüfung im Detail

`DeviceIdentityService.checkIntegrity(identityKey)` prüft:

1. Steht diese Identität aktuell einem anderen, bereits registrierten Gerät
   als harter Konflikt (`warningLevel === 'RED'`, aus der bereits
   bestehenden Kandidatenermittlung) gegenüber? Für jeden gefundenen
   Konflikt wird eine vollständige tabellarische Gegenüberstellung gebaut:
   IP-Adresse, MAC-Adresse, Seriennummer, Hostname/Name,
   Betriebssystem/Kategorie, Hersteller/Hardware, Discovery-Quellen, Rollen,
   Zeitpunkt der letzten Erkennung.
2. Bei Linux-Geräten mit gespeichertem SSH-Hostkey und konfiguriertem
   SSH-Zugang: Abgleich mit dem aktuell abgerufenen Hostkey-Fingerabdruck.

**Warnstufen:**
- 🟢 Integrität hoch: keine Konflikte gefunden.
- 🟡 Bitte überprüfen: genau ein Konflikt gefunden.
- 🔴 Integritätswarnung: zwei oder mehr Konflikte gefunden.

## Wiederauftrennung im Detail

`DeviceIdentityService.splitCandidates(identityKey)` ermittelt, was
abgespalten werden könnte:
- jeder bekannte **Alias** (Details wie MAC/IP nicht mehr rekonstruierbar,
  da vor der Wiederauftrennung nur der Name bekannt ist),
- jeder **Quell-Schnappschuss** aus dem Merge-Log, sofern ab 40k33b7 mit
  vollständigen Merkmalen gesichert (`fullSnapshot: true`).

`DeviceIdentityService.split(identityKey, ref, actor)`:
- legt über die bereits vorhandene `register()`-Methode ein **neues,
  eigenständiges Gerät** an (dieselbe Identitätsschlüssel-Berechnung wie bei
  jedem regulären Discovery-Treffer),
- entfernt bei einer alias-basierten Auswahl nur den entsprechenden Alias
  vom Ursprungsgerät,
- protokolliert die Aktion im bestehenden Merge-Log (`action='SPLIT'`).

**Wichtig:** Das Ursprungsgerät bleibt in jedem Fall vollständig erhalten -
die Zusammenführung selbst wird nicht rückgängig gemacht (das wäre bei
bereits aufsummierten Zählern/vereinigten Aliasen nicht sicher verlustfrei
möglich), sondern es entsteht zusätzlich ein neues, separates Gerät. Es
gehen dabei keine Informationen verloren.

## Neue REST-Endpunkte

- `GET /api/inventory/discovery/identity/integrity?identityKey=...`
- `GET /api/inventory/discovery/identity/split-candidates?identityKey=...`
- `POST /api/inventory/discovery/identity/split`

## Neue Datenbankmigration

Additive Erweiterung der bestehenden Tabelle `gam_discovery_merge_log`
(migrationssicher über denselben `information_schema`-geprüften Ansatz wie
alle bisherigen Migrationen in diesem Projekt):
- `action VARCHAR(20) NOT NULL DEFAULT 'MERGE'`
- `source_snapshot MEDIUMTEXT NULL`

Bestehende Daten bleiben vollständig erhalten; bereits vorhandene
Zusammenführungen (vor 40k33b7) haben lediglich keinen Schnappschuss
(`source_snapshot IS NULL`) und werden bei der Wiederauftrennung entsprechend
mit einem Hinweis versehen.

## Tests / durchgeführte Prüfungen in dieser Umgebung

Wie in den vorangegangenen Schritten: kein JDK, kein funktionierendes
`npm install` in dieser Sandbox - kein echter Build möglich. Stattdessen
geprüft:

- Klammern-/Parameterbilanz aller sechs geänderten/neuen Backend-Dateien
  (ausgeglichen).
- Kreuzabgleich aller neuen Methoden-/Record-Signaturen zwischen Aufrufer und
  Aufgerufenem (grep-basiert), inklusive Gegenprüfung gegen den TATSÄCHLICH
  hochgeladenen Stand (nicht gegen eigene Erinnerung - dabei wurden mehrere
  Abweichungen zur eigenen Erinnerung an frühere Schritte festgestellt und
  korrekt berücksichtigt, z.B. abweichende Feldnamen in `Candidate`).
- Die erweiterte SSH-Remote-Skript-Zeichenkette wurde erneut extrahiert und
  mit `bash -n` geprüft (fehlerfrei), ebenso das neue eigenständige
  Hostkey-Abgleichs-Kommando.
- Isolierter TypeScript-Transpile-Lauf über `client.ts` und die vollständige
  `main.tsx` nach jeder größeren Änderung - durchgehend 0 Diagnosen.
- Verwendete CSS-Klassen wurden gegen die tatsächlich vorhandenen Definitionen
  geprüft; zwei ursprünglich angenommene, tatsächlich nicht existierende
  Klassennamen wurden korrigiert (auf bereits vorhandene `note ok`/`note
  warn`/`note danger`-Kombinationen umgestellt, eine neue, kleine CSS-Regel
  für hervorgehobene Konfliktzeilen ergänzt).

**Nicht durchgeführt:** echter Maven-/Vite-Build, automatisierte Tests, Test
gegen ein echtes Linux-System mit SSH-Hostkey-Wechsel. Bitte vor dem
Produktiveinsatz nachholen.

## Bekannte Einschränkungen

- „Zwei unterschiedliche IP-Adressen gleichzeitig erreichbar“ wird nicht
  durch eine Live-Erreichbarkeitsprüfung beider IP-Adressen zum
  Prüfzeitpunkt erkannt (das würde einen zusätzlichen Netzwerkscan während
  einer reinen Leseanfrage bedeuten) - stattdessen zählt jeder von der
  bestehenden Engine erkannte harte Konflikt (MAC/Seriennummer differieren)
  einheitlich als Konflikt.
- Für Zusammenführungen vor 40k33b7 existiert kein Quell-Schnappschuss; die
  Wiederauftrennung kann dort nur auf Basis der noch bekannten Aliase
  (Name) erfolgen, nicht mit vollständigen Merkmalen.
- Die Wiederauftrennung ist bewusst additiv (neues Gerät entsteht zusätzlich)
  statt einer echten Rückabwicklung der ursprünglichen Zusammenführung - eine
  vollständige Umkehrung wäre bei bereits aufsummierten Zählern und
  vereinigten Aliasnamen nicht mehr sicher verlustfrei möglich.

## Manuelle Testanleitung

1. Bei einem bereits zusammengeführten Gerät im Dialog „Geräteidentität“ den
   Bereich „Integritätsprüfung“ öffnen: Status, Begründungen und Zeitpunkt
   müssen erscheinen.
2. Zwei Geräte mit unterschiedlichen bekannten MAC-Adressen registrieren
   (bzw. einen bestehenden 🔴-Kandidaten aus der Gerätezusammenführung
   nutzen): die Integritätsprüfung des betroffenen Geräts muss 🔴
   „Integritätswarnung“ mit tabellarischer Gegenüberstellung zeigen.
3. Bei einem Linux-Gerät mit konfiguriertem SSH-Zugang: nach einem
   Neustart mit neu generierten Hostkeys (z.B. Testumgebung) muss die
   Integritätsprüfung den Hostkey-Wechsel als Konflikt melden.
4. Button „Wiederauftrennung“ öffnen: bei einem Gerät mit Aliasen müssen
   diese zur Auswahl stehen; nach Bestätigung muss ein neues, eigenständiges
   Gerät in der Geräteliste erscheinen, während das Ursprungsgerät
   unverändert mit allen übrigen Informationen bestehen bleibt.
5. Regressionstest: eine bereits bestehende Zusammenführung (40k33b4) sowie
   die erweiterte Linux-Analyse (40k33b6b) müssen unverändert weiter
   funktionieren.
