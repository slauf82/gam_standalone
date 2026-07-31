# Schritt 40k33b4 – Discovery-Chaining und kontrollierte Gerätezusammenführung

## Ausgangsproblem

In 40k33b3 wurden Treffer verschiedener Discovery-Quellen für dasselbe physische
Gerät teilweise nicht zusammengeführt, z. B.:

- `Kostal-Wechselrichter-1` (mDNS, keine MAC) und `Kostal-1` (Windows/FRITZ!Box,
  MAC `F8:36:9B:A5:23:3E`) – beide `192.168.2.170`.
- `Kostal-Wechselrichter-2` / `Kostal-2` – analog, `192.168.2.172`.
- `BYD401908-00279` (Linux-Netzwerkerkennung, Kategorie „Dateiserver / Linux“)
  und `BYD-Garage` (Windows/FRITZ!Box, MAC `00:14:97:2B:FE:73`) – beide
  `192.168.2.51`.

## Ursachenanalyse (vor der Umsetzung durchgeführt)

- Discovery-Treffer werden pro Suchlauf zunächst live in einer In-Memory-Map
  zusammengeführt (`DeviceDiscoveryService.emitIfNew()` →
  `findIdentityMatch()` → `DeviceIdentityConfidenceEngine.assess()`), bevor sie
  persistiert werden (`gam_discovery_registered_devices`,
  `DiscoveryRegistrationRepository`).
- Die Confidence-Engine vergab für eine übereinstimmende IP-Adresse bisher nur
  ein sehr geringes Gewicht (`ipWeight=5`) und verhinderte über
  `ipNeverMergesAlone` ausdrücklich, dass eine IP allein jemals zu einem
  automatischen Merge führt. Das ist grundsätzlich richtig (DHCP kann IPs neu
  vergeben), verhinderte aber auch den Fall, dass ein Treffer OHNE MAC
  (mDNS/SSDP) mit einem Treffer MIT MAC (ARP/Windows/FRITZ!Box) für dieselbe
  IP zusammengeführt wird, obwohl kein Widerspruch vorliegt.
- Da die beiden Treffer nie zusammengeführt wurden, landeten sie unter
  unterschiedlichen `identity_key`-Werten (einmal `mac:...`, einmal `ip:...`)
  als zwei getrennte Datensätze in der Datenbank.
- Kategorie-Priorität: Es gab **zwei** leicht unterschiedliche, unvollständige
  Implementierungen von „bevorzuge die spezifischere Kategorie“ – eine in
  `DeviceDiscoveryService` (nur „Netzwerkgerät“/„Home Assistant Gerät“ als
  generisch erkannt) und eine ältere, nur auf das Wort „netzwerk“ prüfende
  Variante in `DiscoveryRegistrationRepository.consolidateSemanticDuplicates()`.
  Beide waren zusätzlich reihenfolgeabhängig: eine zuerst gespeicherte
  generische Linux-Plattformkategorie („Dateiserver / Linux“) konnte eine
  später erkannte spezifische Kategorie („Energie / Wechselrichter“) fälschlich
  überdauern.
- Es gab keinen Schutz gegen das Überschreiben eines manuell gesetzten Namens
  (nur die Gerätekategorie war über `manual_device_type` geschützt).

## Geänderte/neue Dateien

Backend:
- `backend/.../inventory/DeviceIdentityConfidenceEngine.java` – IP-Merge-Brücke
  ergänzt.
- `backend/.../inventory/DiscoveryRegistrationRepository.java` – `manual_name`-
  Spalte/Schutz, Alias-Tabelle, gemeinsame `preferSpecificType()`/
  `categorySpecificity()`, `planMerge()`/`mergeDevices()`.
- `backend/.../inventory/DeviceMergeRepository.java` (neu) – ignorierte
  Vorschläge, Merge-Audit-Log.
- `backend/.../inventory/DeviceMergeService.java` (neu) – Kandidatensuche,
  Vorschau, Bestätigung, Ablehnung.
- `backend/.../inventory/DeviceMergeController.java` (neu) – REST-Endpunkte.
- `backend/.../inventory/DeviceDiscoveryService.java` – Discovery-Chaining-
  Protokollierung (Hostname-/Fingerabdruck-Hinweise anderer Quellen werden an
  die Linux-Nachprüfung weitergereicht), gemeinsame `preferSpecificType()`.
- `backend/.../security/GlobalApiExceptionHandler.java` – `IllegalStateException`
  → verständliche 409-Antwort (für den MAC-Konflikt-Schutz).

Frontend:
- `frontend/src/api/client.ts` – neue Typen/Funktionen für die
  Gerätezusammenführung, `manualName` an `RegisteredDiscoveryDevice` ergänzt.
- `frontend/src/main.tsx` – neue Komponente `DeviceMergeDialog` sowie Button
  „Gerätezusammenführung prüfen“ im Gerätemanager.

Dokumentation:
- `docs/40k33b4-discovery-chaining-geraetezusammenfuehrung.md` (diese Datei).

## Neue Backend-Funktionen

- `DiscoveryRegistrationRepository.hasManualDeviceType(...)` (bereits 40k33b3).
- `DiscoveryRegistrationRepository.addAlias()`/`aliasesFor()` – Aliasverwaltung.
- `DiscoveryRegistrationRepository.planMerge()` – reine Berechnung des
  Merge-Ergebnisses (keine Schreibung), Grundlage für Vorschau UND Ausführung.
- `DiscoveryRegistrationRepository.mergeDevices()` – transaktionale
  Ausführung.
- `DeviceMergeService.findCandidates()/preview()/confirm()/ignore()/auditLog()`.
- REST: `GET/POST /api/inventory/discovery/merge/candidates|preview|confirm|ignore|log`.

## Verwendete Merge-Regeln (Priorität)

1. Manuell gepflegter Name/Kategorie (an Ziel ODER Quelle) hat immer Vorrang
   vor automatisch erkannten Werten.
2. Spezifische Kategorie vor allgemeiner Kategorie
   (`categorySpecificity()`: 0 = generischer Platzhalter wie „Netzwerkgerät“,
   1 = erkannte Linux-Plattform ohne eigene Geräteklasse, 2 = konkrete
   Geräteklasse) – unabhängig von der Erkennungsreihenfolge.
3. IP-Adresse ist eine starke Merge-Brücke, wenn genau eine Seite eine bekannte
   MAC-Adresse hat und kein Widerspruch vorliegt.
4. Zwei unterschiedliche bekannte MAC-Adressen verhindern die automatische wie
   die manuelle Zusammenführung, außer der Benutzer bestätigt dies in der
   Vorschau ausdrücklich (`confirmMacConflict`).
5. Trefferzähler werden aufsummiert (jede Quelle zählte eigene Rohtreffer),
   nicht überschrieben; `first_seen_at`/`last_seen_at` werden über alle
   beteiligten Datensätze als MIN/MAX übernommen.
6. Namen zusammengeführter/umbenannter Datensätze bleiben als Alias erhalten
   und werden bei künftigen Treffern wieder derselben Identität zugeordnet.

## Schutzregeln bei IP-/MAC-Konflikten

- Identische IP + eine bekannte MAC + keine bekannte MAC auf der anderen Seite
  + kein sonstiger Konflikt → automatische Merge-Brücke.
- Identische IP + zwei unterschiedliche bekannte MACs → **keine** automatische
  Zusammenführung; auch die manuelle Zusammenführung wird ohne ausdrückliche
  Bestätigung blockiert (`IllegalStateException` → HTTP 409).
- Eine IP überschreibt niemals eine abweichende bekannte Hardware-Identität.

## Discovery-Chaining (Trigger-Kette)

`DeviceDiscoveryService` reicht bereits von anderen Quellen gelieferte Hinweise
(Name, Typ, Protokoll aus mDNS/SSDP/SNMP/ARP/Windows/FRITZ!Box/Home
Assistant/Tuya) gezielt an die Linux-Nachprüfung weiter
(`LinuxNetworkDiscoveryService`, seit 40k33b3). Der Suchlauf protokolliert
dies jetzt als eigene Diagnosephase `TRIGGER_CHAIN` (Anzahl Ziele, davon wie
viele einen plausiblen Linux-Hinweis mitbringen). Schleifenschutz: jede IP wird
pro Suchlauf genau einmal dedupliziert (keine Rekursion), ein fehlgeschlagenes
Einzelziel bricht den restlichen Suchlauf nicht ab (bestehendes
try/catch pro Ziel in `LinuxNetworkDiscoveryService.scan()`).

**Bekannte Einschränkung:** Eine vollständig generische Trigger-Engine über
alle im Auftrag genannten Kategorien (ONVIF, SNMP, Home Assistant,
Energie-/Wechselrichter-Hinweise) hätte bedeutet, für diese Kategorien
komplett neue aktive Prüfmodule zu entwickeln, die es im Projekt heute noch
nicht gibt (nur für Linux existiert ein solches gezieltes
Nachprüfmodul). Das wäre keine „sinnvolle Erweiterung der vorhandenen
Architektur“ mehr, sondern der Bau mehrerer neuer Discovery-Module – das würde
den Rahmen dieses Schritts sprengen und wurde bewusst nicht umgesetzt. Die
Chaining-Protokollierung und der Hinweis-Übergabemechanismus sind aber generisch
gehalten, sodass ein künftiges, dediziertes Prüfmodul (z. B. für ONVIF) ohne
Architekturänderung angeschlossen werden könnte.

## Neue Datenbankmigrationen

Alle über das bestehende Migrationsmuster (`addColumn()` mit
`information_schema`-Prüfung bzw. `CREATE TABLE IF NOT EXISTS`) umgesetzt,
funktionieren also sowohl mit leerer als auch mit bestehender Datenbank:

- `gam_discovery_registered_devices.manual_name BOOLEAN NOT NULL DEFAULT FALSE`
- `gam_discovery_device_aliases` (identity_key, alias_name, alias_semantic,
  created_at)
- `gam_discovery_merge_ignored` (pair_key, key_a, key_b, signature, ignored_at,
  ignored_by)
- `gam_discovery_merge_log` (id, target_identity_key, merged_identity_keys,
  summary, performed_by, performed_at)

## Rückgängig machen von Zusammenführungen

Es wurde geprüft, ob eine sichere „Rückgängig“-Funktion innerhalb dieses
Schritts sinnvoll möglich ist. Da eine Zusammenführung Zähler aufsummiert,
Aliase zusammenlegt und Quell-Datensätze endgültig löscht, wäre eine
Rückgängig-Funktion nur eine unsichere Rekonstruktion (keine echte Umkehrung).
Entsprechend der Vorgabe „keine unsichere Pseudo-Rückgängig-Funktion“ wurde
stattdessen umgesetzt:

- Zusammenführung erfolgt ausschließlich nach Vorschau und expliziter
  Bestätigung.
- Vollständige Transaktion (`@Transactional` auf `mergeDevices()`).
- Verständliche Fehlermeldungen bei Abbruch (u. a. MAC-Konflikt → 409).
- Technisches Audit-Protokoll (`gam_discovery_merge_log`,
  `GET /api/inventory/discovery/merge/log`).
- Keine Teilspeicherung (Transaktion schlägt vollständig fehl oder vollständig
  durch).

## Durchgeführte Prüfungen in dieser Umgebung

Diese Sandbox hat **kein JDK** (nur JRE, kein `javac`, kein Internetzugriff zum
Nachinstallieren) und **kein funktionierendes `npm install`** (Registry
antwortet mit `403 Forbidden`, `node_modules` fehlt vollständig und konnte
nicht installiert werden). Ein echter `mvn package`/`npm run build`-Lauf war
daher technisch nicht möglich. Stattdessen wurde geprüft:

- Klammern-/Parameterbilanz aller neuen/geänderten Java-Dateien (automatisiert
  ausgezählt, jeweils ausgeglichen).
- Kreuzabgleich aller neuen Methoden-/Record-Signaturen zwischen Aufrufer und
  Aufgerufenem (grep-basiert).
- Isolierter TypeScript-Transpile-Lauf (`ts.transpileModule`, syntaxprüfend,
  ohne Typauflösung mangels installierter Projekt-Abhängigkeiten) über
  `client.ts` und die vollständige `main.tsx` – jeweils 0 Diagnosen.
- Manuelle Nachverfolgung der neuen Zustandsvariablen/Komponentenreferenzen im
  Frontend (`mergeDialogOpen`, `DeviceMergeDialog`) auf Konsistenz.

**Nicht durchgeführt** (nicht ausführbar in dieser Umgebung): echter
Maven-Build, echter Vite-/TypeScript-Build mit vollständiger Typprüfung,
Ausführung bestehender oder neuer automatisierter Tests, Start gegen eine
echte MariaDB-Instanz. Diese Schritte sollten vor dem Produktiveinsatz in einer
Umgebung mit Internetzugriff bzw. vorhandenem JDK/`node_modules` nachgeholt
werden.

## Manuelle Testanleitung

1. **Kostal-1 / Kostal-Wechselrichter-1 (bzw. -2):** Nach einem erneuten
   Suchlauf sollten beide Treffer für dieselbe IP direkt als EIN Eintrag mit
   MAC-Adresse und Kategorie „Energie / Wechselrichter“ erscheinen (IP-Merge-
   Brücke greift bereits live während des Suchlaufs). Bestehen aus einer
   früheren Version noch zwei getrennte Datensätze, über „Gerätezusammenführung
   prüfen“ → automatisch vorgeschlagener Kandidat → Vorschau → Bestätigen.
2. **BYD-Garage / BYD401908-00279:** Gleiches Vorgehen; in der Vorschau
   prüfen, dass die Kategorie NICHT auf „Dateiserver / Linux“ zurückfällt,
   sondern die zuvor bestehende Klassifikation erhalten bleibt.
3. **Home Assistant mit zusätzlicher Linux-Erkennung:** Kategorie „Smart-Home-
   Zentrale / Linux“ darf durch eine zusätzliche Linux-Netzwerktreffermeldung
   nicht verändert werden (bereits über `categorySpecificity()`
   sichergestellt).
4. **Zwei Geräte mit widersprüchlichen MAC-Adressen:** In der freien Auswahl
   beide markieren, „Vorschau für Auswahl“ – es muss eine deutliche Warnung
   samt Bestätigungs-Checkbox erscheinen; ohne Bestätigung bleibt „Zusammen-
   führung bestätigen“ deaktiviert.
5. **Freie Auswahl zweier beliebiger Geräte:** Im Dialog unter „Freie Auswahl“
   zwei Geräte per Checkbox markieren, Vorschau öffnen, Werte/Quellen prüfen,
   bestätigen; die Liste muss sich danach sofort aktualisieren und nur noch
   einen Eintrag zeigen.

## Bekannte Einschränkungen

- Kandidatensuche vergleicht bei sehr großen Bestände paarweise (O(n²)); ab
  500 registrierten Geräten wird die Prüfung auf die ersten 500 begrenzt.
- Keine echte Rückgängig-Funktion (siehe oben) – nur vollständiges,
  nachvollziehbares Audit-Protokoll.
- Generische Trigger-Engine für ONVIF/Energie-/Home-Assistant-spezifische
  aktive Nachprüfungen wurde nicht gebaut (siehe Abschnitt
  „Discovery-Chaining“) – nur die bereits vorhandene Linux-Nachprüfung wurde
  formal in die Chaining-Protokollierung eingebunden.
- Kein echter Build in dieser Sandbox durchgeführt (siehe oben) – vor dem
  Produktiveinsatz bitte `mvn -f backend/pom.xml package` und
  `npm --prefix frontend install && npm --prefix frontend run build` in einer
  Umgebung mit Internetzugriff ausführen.
