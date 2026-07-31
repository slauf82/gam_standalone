# Schritt 40k33b5 – Geräteidentität, Quellenübersicht und Identitätsverwaltung

## Ausgangslage

40k33b4 hat eine stabile Geräteidentität geschaffen (IP-Merge-Brücke, manuelle
Zusammenführung, keine neuen Dubletten nach einem Merge). 40k33b5 macht diese
Identität für den Benutzer sichtbar und verwaltbar, ohne die bestehende
Discovery- oder Merge-Logik zu verändern.

## Geänderte/neue Dateien

Backend:
- `backend/.../inventory/DiscoveryRegistrationRepository.java` – Alias-
  Verwaltung erweitert (`removeAlias`, `renameAlias`, `aliasesWithTimestamps`),
  `typeHistoryFor()` (liest die bereits seit 40k33b1 bestehende
  Kategorie-Historie), `distinctSourceLabels()` (öffentlicher Wrapper um die
  bereits vorhandene `collectProtocol()`-Quellen-Erkennung).
- `backend/.../inventory/DeviceMergeRepository.java` – `mergeCountForTarget()`,
  `logEntriesTouching()` (liest das bestehende Merge-Audit-Log aus 40k33b4,
  gefiltert auf ein Gerät).
- `backend/.../inventory/DeviceIdentityService.java` (neu) – aggregiert
  Identität, Quellenübersicht, Aliase, Vertrauensbewertung, Identitätsverlauf
  und die neuen manuellen Aktionen. Keine eigene Datenhaltung, alles wird aus
  den bestehenden Repositories gelesen bzw. an sie delegiert.
- `backend/.../inventory/DeviceIdentityController.java` (neu) – REST-
  Endpunkte unter `/api/inventory/discovery/identity/...`.

Frontend:
- `frontend/src/api/client.ts` – neue Typen/Funktionen für Identitäts-
  Übersicht, -Detail, -Verlauf, Neubewertung und Aliasverwaltung.
- `frontend/src/main.tsx` – neue Komponente `DeviceIdentityDialog`, Spalte
  „Identität“ (Quellen-/Alias-/Merge-Anzahl, Vertrauensstufe) in der
  Geräteliste, Alias-Suche, sechs neue Filter.

Dokumentation:
- `docs/40k33b5-geraeteidentitaet-quellenverwaltung.md` (diese Datei).

## Backend im Detail

`DeviceIdentityService` baut bewusst KEINE zweite Identitätsverwaltung auf:

- Quellenliste: Wiederverwendung der bereits vorhandenen
  `collectProtocol()`-Logik (dieselbe, die auch `mergeDiscoveryProtocol()` beim
  Zusammenführen von Discovery-Treffern nutzt) - keine neue Quellen-Erkennung.
- Identitätsverlauf: kombiniert die bereits bestehende
  `gam_discovery_device_type_history`-Tabelle (seit 40k33b1), die
  Alias-Zeitstempel und das Merge-Audit-Log aus 40k33b4 zu einer
  chronologischen Liste. Keine neue Verlaufstabelle.
- Vertrauensbewertung: **wichtige bewusste Anpassung.** Die bestehende
  `DeviceIdentityConfidenceEngine` bewertet paarweise, ob zwei GETRENNTE
  Treffer zusammengehören. Nach einem erfolgreichen Merge gibt es aber nur
  noch EINEN Datensatz - einen Vergleichspartner gibt es nicht mehr. Die neue
  Bewertung in `DeviceIdentityService.assessConfidence()` bewertet deshalb,
  wie gut eine BEREITS bestehende Identität durch eindeutige Merkmale (MAC,
  Seriennummer, Hersteller) und mehrere unabhängige Quellen/Aliase abgesichert
  ist ("Sehr sicher"/"Sicher"/"Teilweise bestätigt"/"Unsicher" mit
  Begründungen) - keine neue, parallele Bewertungsengine, sondern eine
  andersartige, aber begründete Interpretation für den Einzelfall „ein Gerät“.
- Unterkategorie/Plattform: werden aus der bereits im gesamten Projekt
  verwendeten Kategorie-Namenskonvention "Familie / Spezifisch" (z.B. "Energie
  / Wechselrichter") sowie aus dem vorhandenen `discovery_protocol`-Text
  abgeleitet. Keine neue Datenstruktur, keine Migration nötig.
- „Identität erneut prüfen“: ruft die bereits vorhandene
  `DeviceMergeService.findCandidates()` erneut auf und filtert auf das
  betreffende Gerät - keine zweite Kandidatenermittlung.
- „Discovery erneut ausführen“: Ein echter, gezielter Rescan NUR eines
  einzelnen Geräts existiert in der bestehenden Architektur nicht (der
  vorhandene Suchlauf ist ein vollständiger Netzwerk-Scan mit fester
  Reihenfolge der Quellen). Um "keine bestehende Discovery zu verändern"
  einzuhalten, verweist der Dialog stattdessen auf den bereits vorhandenen
  Suchlauf-Button ("Suche starten") - ein neuer Suchlauf ergänzt gemäß der
  bestehenden Merge-Logik ohnehin nur zusätzliche Quellen am selben
  Datensatz und erzeugt keine Dublette. Das ist eine bewusste, dokumentierte
  Einschränkung.

## Neue REST-Endpunkte

Identitätsschlüssel werden bewusst NICHT als Pfad-Parameter übergeben (Query-
Parameter bei Lesezugriffen, Body bei Schreibzugriffen) - dieselbe
Vorsichtsmaßnahme, die im Projekt bereits als "40k31.1"-Regel für
zusammengesetzte Identitätsschlüssel mit Sonderzeichen dokumentiert ist.

- `GET /api/inventory/discovery/identity/overview`
- `GET /api/inventory/discovery/identity/detail?identityKey=...`
- `GET /api/inventory/discovery/identity/history?identityKey=...`
- `POST /api/inventory/discovery/identity/reassess`
- `POST /api/inventory/discovery/identity/alias` (hinzufügen)
- `PUT /api/inventory/discovery/identity/alias` (umbenennen)
- `POST /api/inventory/discovery/identity/alias/remove`
- `PUT /api/inventory/discovery/identity/main-name`

## Frontend im Detail

- Neue Spalte „Identität“ in der Liste der registrierten Geräte: Quellen-/
  Alias-/Merge-Anzahl, Vertrauensstufe, Hinweis auf offene
  Zusammenführungskandidaten, Button „Identität“ öffnet den neuen Dialog.
- Neuer Dialog „Geräteidentität“: alle Identitätsfelder aus Abschnitt 2 des
  Auftrags, Quellenübersicht (Badges, Wiederverwendung der bereits
  bestehenden `discoverySourceBadges()`-Komponente), Aliasverwaltung
  (hinzufügen/umbenennen/entfernen/als Hauptname übernehmen),
  Vertrauensbewertung mit Begründung, Identitätsverlauf, „Identität erneut
  prüfen“.
- Suche: durchsucht jetzt zusätzlich die Aliasnamen (aus der Identitäts-
  Übersicht).
- Sechs neue Filter (Abschnitt 11): nur mehrere Quellen, nur mit Aliasen, nur
  manuell zusammengeführt, nur ungeprüft, nur mit Identitätskonflikt, nur
  hohe Sicherheit.
- Die bereits bestehende technische Detailanzeige
  (`registeredDetailSections`, "welche Discovery welche Information geliefert
  hat") sowie die Quellen-Badges (`discoverySourceBadges`) wurden
  unverändert wiederverwendet, nicht neu gebaut.

## Neue Datenbankmigrationen

Keine neuen Tabellen. Es werden ausschließlich bereits vorhandene Tabellen
(`gam_discovery_registered_devices`, `gam_discovery_device_aliases`,
`gam_discovery_device_type_history`, `gam_discovery_merge_log`) gelesen bzw.
punktuell geschrieben (Alias hinzufügen/entfernen/umbenennen nutzt die
bestehende `gam_discovery_device_aliases`-Tabelle aus 40k33b4). Bestehende
Daten und Zusammenführungen bleiben vollständig erhalten; keine Migration
erforderlich.

## Tests / durchgeführte Prüfungen in dieser Umgebung

Wie bereits in 40k33b3/40k33b4 hat diese Sandbox **kein JDK** (nur JRE, kein
Internetzugriff zum Nachinstallieren) und **kein funktionierendes
`npm install`** (Registry antwortet mit 403, `node_modules` fehlt). Ein
echter `mvn package`/`npm run build`-Lauf sowie das Ausführen automatisierter
Tests waren daher technisch nicht möglich. Stattdessen wurde geprüft:

- Klammern-/Parameterbilanz aller neuen/geänderten Java-Dateien (ausgeglichen).
- Kreuzabgleich aller neuen Methoden-/Record-Signaturen zwischen Aufrufer und
  Aufgerufenem.
- Isolierter TypeScript-Transpile-Lauf (`ts.transpileModule`, syntaxprüfend)
  über `client.ts` und die vollständige `main.tsx` - jeweils 0 Diagnosen.
  Dabei wurde ein Fehler beim Einfügen der neuen Komponente entdeckt und
  sofort behoben (eine Funktionssignatur wurde versehentlich beim Einfügen
  überschrieben) - der erneute Lauf danach war wieder fehlerfrei.
- Manuelle Nachverfolgung aller neuen Zustandsvariablen/Komponenten-
  referenzen (`identityDialogKey`, `identityOverview`, `identityFilter`,
  `DeviceIdentityDialog`) auf Konsistenz.

**Nicht durchgeführt** (nicht ausführbar in dieser Umgebung): echter
Maven-/Vite-Build mit vollständiger Typprüfung, Ausführung bestehender oder
neuer automatisierter Tests, Start gegen eine echte MariaDB-Instanz. Bitte vor
dem Produktiveinsatz in einer Umgebung mit Internetzugriff bzw. vorhandenem
JDK/`node_modules` nachholen.

## Bekannte Einschränkungen

- "Discovery erneut ausführen" löst keinen gezielten Rescan nur eines
  einzelnen Geräts aus (siehe Begründung oben), sondern verweist auf den
  bestehenden vollständigen Suchlauf.
- "Identitätskonflikt"-Filter zeigt Geräte mit mindestens einem aktuell
  offenen (nicht ignorierten) Zusammenführungsvorschlag - keine tiefergehende
  Widerspruchsanalyse einzelner Felder.
- Quellenübersicht zeigt eine gerätweite erste/letzte Erkennung und
  Gesamttrefferzahl; eine ECHTE Erkennung/letzte Erkennung/Trefferzahl PRO
  EINZELNER QUELLE existiert nicht, da die zugrunde liegenden Discovery-
  Dienste das bisher nicht separat speichern (nur der zusammengeführte
  `discovery_protocol`-Text). Eine Änderung daran hätte Eingriffe in die
  Discovery-Dienste selbst bedeutet, was der Auftrag ausdrücklich ausschließt.

## Manuelle Testanleitung

1. **Alias-Suche:** Im Gerätemanager unter „Registrierte Geräte“ nach einem
   bekannten Alias suchen (z.B. „Kostal-Wechselrichter“ bei einem bereits
   unter „Kostal-1“ geführten Gerät) - das Gerät muss gefunden werden.
2. **Quellen-/Alias-/Merge-Anzeige:** Spalte „Identität“ in der Tabelle
   prüfen; bei einem zusammengeführten Gerät müssen Quellenanzahl > 1 und
   Merge-Anzahl ≥ 1 sichtbar sein.
3. **Dialog „Geräteidentität“:** Button „Identität“ öffnen, alle Felder aus
   Abschnitt 2 des Auftrags müssen befüllt oder als „—“ erkennbar sein.
4. **Aliasverwaltung:** Alias hinzufügen, umbenennen, entfernen, „Als
   Hauptname übernehmen“ - jeweils muss sich die Liste sofort aktualisieren.
5. **Identitätsverlauf:** Nach einer Zusammenführung (40k33b4) muss ein
   MERGE-Eintrag erscheinen; nach einer Kategorieänderung ein
   KATEGORIE-Eintrag; nach dem Hinzufügen eines Alias ein ALIAS-Eintrag.
6. **Filter:** Jeden der sechs neuen Filter einzeln auswählen und prüfen, dass
   nur passende Geräte angezeigt werden.
7. **Neubewertung:** „Identität erneut prüfen“ auslösen; bei einem Gerät mit
   noch nicht zusammengeführter Dublette muss ein Hinweis auf einen offenen
   Kandidaten erscheinen.
8. **Keine neuen Dubletten:** Einen neuen Suchlauf starten; bereits
   identifizierte Geräte (inkl. der in 40k33b4 zusammengeführten) dürfen
   nicht erneut als separate Einträge auftauchen.
