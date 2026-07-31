# Schritt 40k34c – Android-App- und Paket-Inventarisierung

## Architektur

Diese Erweiterung fügt **keine** neue Android-Architektur hinzu. Sie erweitert
ausschließlich die bereits bestehende, lesende Android-/ADB-Inventarisierung
(40k34a/40k34b) um Apps und Pakete als **untergeordnete Inventardaten** eines
Android-Geräts - keine App erscheint jemals als eigenständiges Gerät in der
allgemeinen Geräteverwaltung.

- `AndroidAdbService` (bestehend, **nicht** dupliziert) wurde um Methoden für
  Paketlisten/`dumpsys package`/Standardrollen-Auflösung erweitert - dieselbe
  ProcessBuilder-/Timeout-/Argumentlisten-Architektur wie seit 40k34b.
- `AndroidAppInventoryParser` (neu) übernimmt für Apps dieselbe Rolle wie
  `AndroidInventoryParser` für das Systeminventar: reine Textauswertung einer
  bereits abgerufenen Ausgabe.
- `AppInventoryRepository` (neu, bewusst generisch benannt statt
  "Android...") verwaltet zwei zusätzliche, additive Tabellen nach der im
  Auftrag selbst vorgeschlagenen Struktur Gerät → Inventarlauf →
  App-Datensätze.
- `DeviceIdentityService.runAndroidAppInventory()` orchestriert den Ablauf -
  reine Zusammenführung bestehender Bausteine, keine neue Merge-/
  Identitätslogik.

## Prüfung eines wiederverwendbaren Softwareinventars (vor der Umsetzung)

Es existiert bereits `DeviceSoftware` (`name`, `workplaceId`) für das formale
Inventar-Modul. Diese Struktur ist an einen **Workplace** (bereits ins
Inventar übernommenes Gerät) gebunden, nicht an eine Discovery-Identität, und
besitzt keines der für Android benötigten Felder (Version, Zeitstempel,
Installationsquelle, Flags). Eine Erweiterung um rund 20 zusätzliche Spalten
hätte die bestehende, einfache Struktur für einen andersartigen Zweck
umgewidmet. Stattdessen wurde die im Auftrag selbst angebotene Alternative
gewählt: zwei neue, **bewusst generisch benannte** Tabellen (nicht
"android_"-präfigiert), die konzeptionell auch für ein künftiges Windows-/
Linux-Softwareinventar wiederverwendbar wären, auch wenn sie aktuell
ausschließlich von der Android-Inventarisierung befüllt werden.

## Datenmodell

```
Android-Gerät (bestehende Geräteidentität, identity_key)
    │
    ├── gam_discovery_app_inventory_runs   (Inventarlauf-Verlauf/Status)
    │
    └── gam_discovery_installed_apps       (aktueller App-/Paketstand)
```

**`gam_discovery_installed_apps`** - EIN Datensatz je (Gerät, Paket), additiv
aktualisiert bei jedem Lauf:
`identity_key, package_name, display_name, version_name, version_code,
install_time, update_time, installer_package, is_system, is_updated_system,
is_enabled, is_debuggable, is_test_only, min_sdk, target_sdk, roles,
first_seen_at, last_seen_at, removed_at`

**`gam_discovery_app_inventory_runs`** - Verlaufstabelle je Lauf:
`identity_key, started_at, finished_at, status, app_count, user_app_count,
system_app_count, changes_summary, message`

Beide Tabellen werden über dasselbe migrationssichere
`information_schema`-Muster wie alle bisherigen additiven Erweiterungen
angelegt (`CREATE TABLE IF NOT EXISTS`). Keine bestehende Migration wurde
verändert.

## Verwendete ADB-/Package-Manager-Quellen

Wenige Sammelabfragen statt eines Prozesses je App (Performance-Anforderung):

| Aufruf | Zweck | Häufigkeit |
|---|---|---|
| `pm list packages -3` | Benutzer-Apps | 1× je Lauf |
| `pm list packages -s` | System-Apps | 1× je Lauf |
| `pm list packages -d` | deaktivierte Pakete | 1× je Lauf |
| `pm list packages -e` | aktivierte Pakete | 1× je Lauf |
| `dumpsys package` (ohne Argument) | Version, Zeitstempel, Installer, Flags, SDK-Werte für **alle** Pakete in einem Aufruf | 1× je Lauf |
| `cmd package resolve-activity -a android.intent.action.MAIN -c android.intent.category.HOME` | aktueller Standard-Launcher | 1× je Lauf |
| `cmd package resolve-activity -a android.intent.action.VIEW -d http://example.org` | aktueller Standardbrowser | 1× je Lauf |

Macht **6 Prozessaufrufe je Lauf**, unabhängig davon, ob ein Gerät 20 oder
500 Apps installiert hat - erfüllt ausdrücklich die Performance-Vorgabe
("wenige Sammelabfragen" statt "hunderte gleichzeitig laufende Prozesse").

`dumpsys package` kann bei vielen installierten Apps sehr groß werden -
deshalb wird die Ausgabe mit einem erhöhten, aber weiterhin **begrenzten**
Lesepuffer (2 MB) abgerufen und ausschließlich blockweise/regelbasiert
geparst; die vollständige Rohausgabe wird **nirgends dauerhaft gespeichert**.

## Erfasste App-Felder

| Bereich | Felder |
|---|---|
| Identität | Paketname, Version Name, Version Code/Long Version Code |
| Installation | Installationszeitpunkt, letzter Aktualisierungszeitpunkt, Installer-Paket |
| Status | System-App, Benutzer-App, aktualisierte System-App, aktiviert/deaktiviert |
| Technik | minSdk, targetSdk, debuggable, testOnly |
| Rollen | Launcher, Standardbrowser (siehe unten) |

**Nicht erfasst** (ehrlich benannt, siehe „Bekannte Einschränkungen“):
Anzeigename/Label (siehe unten), APK-Pfad/Split-APK-Anzahl, UID/sharedUserId,
compileSdk, native ABI-Hinweise je App, suspendiert/verborgen/archiviert,
Instant-App-Flag, Berechtigungszahlen.

### Anzeigename (App-Label)

Ein zuverlässiges App-Label ist ohne APK-Analyse (`aapt`) oder eine
zusätzliche Bibliothek nicht robust auslesbar. Da der Auftrag ausdrücklich
**keine neue APK-Analysebibliothek** und **keinen APK-Download** erlaubt,
wird - wie im Auftrag ausdrücklich als zulässiger Weg beschrieben - der
Paketname weiterhin angezeigt und der Anzeigename klar als „nicht verfügbar“
gekennzeichnet, statt einen Namen zu erfinden.

## Benutzer-App/System-App-Abgrenzung

Eine App kann gleichzeitig mehrere Merkmale tragen (z.B. System-App +
aktualisierte System-App + deaktiviert) - es wird deshalb NICHT nur ein
einzelner Typ, sondern ein Satz unabhängiger Flags gespeichert
(`is_system`, `is_updated_system`, `is_enabled`, …), wie im Auftrag
gefordert.

## App-Rollen

Umgesetzt wurden ausschließlich Rollen, die sich aus **klaren technischen
Merkmalen** ohne zusätzlichen, teuren Aufwand ergeben:

- **Launcher** - über `cmd package resolve-activity` mit
  `ACTION_MAIN`/`CATEGORY_HOME` (die tatsächliche System-Intent-Auflösung,
  keine Vermutung).
- **Standardbrowser** - über `cmd package resolve-activity` mit
  `ACTION_VIEW` auf eine HTTP-URL.

**Bewusst nicht umgesetzt** (ehrlich benannt): WebView-Anbieter, VPN-App,
Device-Admin/-Owner/Profile-Owner, Kiosk-/Lock-Task-Erkennung, Tastatur/IME,
Standard-Telefon-/SMS-/Assistent-App. Jede dieser Rollen würde einen
zusätzlichen, teils separaten `dumpsys`-Aufruf je Rolle erfordern
(`dumpsys webviewupdate`, `dumpsys device_policy`, `dumpsys input_method`
usw.) - das hätte die Anzahl der Sammelabfragen deutlich erhöht und wurde
angesichts der Performance-Vorgabe und des Zeitrahmens dieses Schritts
zurückgestellt, statt unzuverlässig aus Paketnamen zu raten (was der Auftrag
ausdrücklich verbietet: „Keine spekulative Sicherheitsbewertung“, „Rollen
dürfen nur aus klaren technischen Merkmalen … abgeleitet werden“).

## Installationsquelle

Die **technische** Installer-Paketkennung (`installerPackageName` aus
`dumpsys package`) ist die Primärinformation und wird unverändert gespeichert
und angezeigt. Es wurde **keine** automatische Vertrauens-/Gefahrenbewertung
ergänzt - Sideloading (kein Installer-Paket bekannt) wird als „unbekannt“
angezeigt, nicht als Warnung oder Risiko.

## Vollständigkeit eines Inventarlaufs

Jeder Lauf erhält einen Status: `VOLLSTAENDIG`, `TEILWEISE`,
`FEHLGESCHLAGEN`, `NICHT_ERREICHBAR`, `NICHT_AUTORISIERT`. Die Schwelle für
„vollständig“ ist nachvollziehbar: Ein Lauf gilt als vollständig, wenn für
**mehr als die Hälfte** der über `pm list packages` gefundenen Pakete auch
ein zugehöriger `dumpsys package`-Block ausgewertet werden konnte. **Nur bei
einem vollständigen Lauf** werden nicht mehr gesehene Pakete als entfernt
markiert (`removed_at` gesetzt) - eine Teilinventarisierung aktualisiert
ausschließlich die gesehenen Datensätze und löscht/entfernt nichts.

## Änderungserkennung

Bei jedem Lauf wird verglichen, welche zuvor aktiven Pakete jetzt fehlen
(„entfernt“, nur bei vollständigem Lauf wirksam), welche neu hinzukamen und
welche mit vorhandenen Detaildaten aktualisiert wurden. Das Ergebnis wird als
kompakte Zusammenfassung („3 neu, 1 entfernt, 2 aktualisiert“) im Lauf-
Verlauf gespeichert und in der Oberfläche angezeigt.

## Datenschutz

Es werden ausschließlich die im Auftrag explizit aufgeführten technischen
Paketfelder erfasst. **Nicht erfasst:** App-Inhalte, Nutzungsdauer, zuletzt
geöffnete App, Browserverlauf, Nachrichten, Kontakte, Konten, App-
Datenbanken, Benutzerdokumente, Benachrichtigungen, Zwischenablage,
Zugangsdaten. Es wird keine Usage-Stats-Berechtigung angefordert, kein
Accessibility-Dienst verwendet, kein `run-as`, kein Backup.

## Sicherheitsgrenzen

- Alle neuen Befehle laufen über dieselbe, bereits bestehende
  `ProcessBuilder`-Argumentlisten-Architektur aus 40k34b - keine
  Shell-Verkettung, keine neue Prozessausführungsschicht.
- **Paketnamen werden validiert** (`AndroidAdbService.isValidPackageName()`),
  bevor sie überhaupt in einen Aufruf gelangen könnten - relevant für eine
  spätere gezielte Detailabfrage; in diesem Schritt werden Paketnamen
  ausschließlich aus der bereits validierten `pm list packages`-Ausgabe
  übernommen, nie aus freiem Benutzertext.
- Ausschließlich lesende Befehle - kein Start/Stopp, keine (De-)Installation,
  keine Berechtigungsänderung, kein App-Datenzugriff.
- Die Pairing-Code-Behandlung aus 40k34b wurde nicht verändert.

## Performance-Konzept

Sechs feste Sammelabfragen je Lauf (siehe Tabelle oben), keine Parallelität
nötig, da alles sequenziell und mit eigenem Timeout läuft. Kein Poll-Loop pro
App. Ein Lauf für ein Gerät mit mehreren hundert Paketen bleibt dadurch in
derselben Größenordnung wie ein Gerät mit wenigen Paketen (der einzige
Unterschied ist die Größe der EINEN `dumpsys package`-Antwort, nicht die
Anzahl der Prozesse).

## Fehlerbehandlung

Konsistent mit 40k34b in vier Klassen:
- **Neutral:** noch keine Inventarisierung, Gerät nicht verbunden.
- **Warnung:** Teilinventarisierung, einzelne Paketdetails nicht lesbar.
- **Autorisierung:** `unauthorized` mit Klartext-Hinweis, kein Programmfehler.
- **Fehler:** ADB-Prozess konnte nicht gestartet werden, Paketliste leer.

Optionale Felder, die bei einzelnen Apps fehlen, führen **nicht** dazu, dass
der gesamte Lauf als fehlgeschlagen markiert wird - nur ein vollständiger
Ausfall der Paketliste selbst zählt als `FEHLGESCHLAGEN`.

## Logging

`DeviceIdentityService` protokolliert (Info-Ebene): Start/Ende der
App-Inventarisierung, interne Gerätekennung (`identityKey`), Anzahl
gefundener/Benutzer-/System-Apps, Laufstatus, Dauer, Änderungszusammenfassung.
**Nicht protokolliert:** vollständige `dumpsys`-Ausgaben, App-Inhalte,
Pairing-Code, unnötige vollständige Seriennummern (nutzt weiterhin
`shortSerial()` aus 40k34b für alle serial-bezogenen Meldungen).

## Neue REST-Endpunkte

- `GET /api/inventory/discovery/identity/android/apps?identityKey=...`
- `GET /api/inventory/discovery/identity/android/apps/runs?identityKey=...&limit=...`
- `POST /api/inventory/discovery/identity/android/apps/inventory`

## Oberfläche

Neuer, **eigenständiger** Unterbereich „Installierte Apps“ innerhalb des
bestehenden Android-Inventarisierungsbereichs (nicht ersetzt, nur erweitert),
lazy geladen. Kopfbereich mit Gesamt-/Benutzer-/System-/deaktiviert-Zählung
und letztem vollständigem Lauf. Tabelle mit App/Paketname/Version/Typ/Status/
Installationsquelle/installiert/aktualisiert/Rollen, aufklappbarer
Detailzeile (Version Code, SDKs, debuggable/testOnly, zuletzt gesehen).
Filter (Alle/Benutzer/System/Deaktiviert/Aktualisierte System-Apps/Launcher/
Browser), Suche über Name/Paket/Version/Quelle/Rolle, Sortierung nach
Name/Paket/Version/Installations-/Aktualisierungsdatum/Typ.

## Erkennungs- vs. Inventarisierungsquellen

Die bestehende „Verwendete Quellen“-Anzeige aus 40k33b9 (Erkennungsquellen:
mDNS/SSDP/SNMP/Home Assistant/Netzwerkscan) bleibt unverändert. Für Android
wird zusätzlich sichtbar, dass ADB (mit Package Manager als der innerhalb von
ADB verwendeten Informationsquelle) eine **Inventarisierungsquelle** ist -
über dasselbe, bereits bestehende `discovery_protocol`-Textfeld
(„Apps und Pakete: …“), keine zweite Quellenarchitektur.

## Inventarisierungsgrad

Der bestehende Android-Inventarisierungsgrad bleibt erhalten; „Apps und
Pakete“ wird als **eine zusätzliche Merkmalsgruppe** ergänzt (Vorhandensein
einer erfolgreichen App-Inventarisierung, nicht die Anzahl einzelner Apps) -
eine große Paketanzahl dominiert damit nicht den Prozentwert, wie im Auftrag
gefordert.

## Merge und Identität

App-Daten wurden **nicht** als Merge-Signal verwendet oder in die
Konflikterkennung eingebunden - weder Paketlisten noch Launcher/Browser
fließen in `DeviceIdentityConfidenceEngine` ein. Die Merge-Schutzregeln aus
40k33b10 bleiben vollständig unverändert.

## Tests / durchgeführte Prüfungen in dieser Umgebung

Kein JDK, kein funktionierendes `npm install` in dieser Sandbox - **kein
echter Build und keine echten Gerätetests möglich.** Dies wird hiermit
ausdrücklich benannt. Stattdessen geprüft:

- Klammernbilanz aller fünf neuen/geänderten Backend-Dateien (ausgeglichen;
  ein Zwischenfehler durch zwei überlappende Bearbeitungsschritte wurde
  während der Entwicklung entdeckt und korrigiert, siehe Commit-Verlauf
  dieser Sitzung).
- Kreuzabgleich aller neuen Methodensignaturen zwischen Controller und
  Service sowie zwischen Service und Repository/AndroidAdbService.
- Isolierter TypeScript-Transpile-Lauf über `client.ts` und die vollständige
  `main.tsx` - 0 Diagnosen.
- Alle neuen React-Funktionskomponenten-Grenzen nach der Einfügung per grep
  verifiziert.
- Manuelle Durchsicht: alle neuen ADB-Aufrufe verwenden Argumentlisten (kein
  `sh -c`), Paketnamen werden vor Verwendung validiert.

**Nicht durchgeführt** (nicht möglich in dieser Sandbox): echter
Maven-/Vite-Build, jegliche der 25 im Auftrag genannten Testfälle gegen echte
Android-Geräte. Bitte vor dem Produktiveinsatz mit mindestens einem Gerät mit
vielen installierten Apps verifizieren (Performance/`dumpsys`-Parsing).

## Testanleitung (an echten Geräten nachzuholen)

1. Gerät mit wenigen Apps: vollständiger Lauf, alle Felder plausibel gefüllt.
2. Gerät mit vielen hundert Paketen: Lauf bleibt in vertretbarer Zeit (6
   Prozessaufrufe, nicht Hunderte).
3. Benutzer-/System-Apps erscheinen mit korrekten, unabhängigen Flags.
4. Eine deaktivierte App zeigt „deaktiviert“, keine „Fehler“-Darstellung.
5. Eine aktualisierte System-App zeigt beide Flags gleichzeitig.
6. Eine App ohne lesbares dumpsys-Detail erscheint trotzdem in der Liste
   (Paketname + Basis-Flags aus `pm list packages`), Lauf wird ggf. als
   „teilweise“ markiert, nicht als Fehler.
7. Zweiter Lauf nach Installation einer neuen App: „1 neu“ erscheint in der
   Änderungszusammenfassung.
8. Zweiter Lauf nach Deinstallation: „1 entfernt“ - nur wenn der zweite Lauf
   als vollständig bewertet wurde.
9. Ein absichtlich abgebrochener/unvollständiger Lauf darf **keine** Apps als
   entfernt markieren.
10. Zwei gleichzeitig verbundene Android-Geräte: App-Listen dürfen sich nicht
    vermischen (Zuordnung über `identityKey`/`serial`).
11. Regressionstest: bestehende Systeminventarisierung (40k34b), Merge-
    Schutzregeln (40k33b10), Integritätsprüfung (40k33b7) bleiben
    unverändert nutzbar.

## Bekannte Einschränkungen

- Kein App-Label/Anzeigename ohne APK-Analyse (siehe oben) - Paketname
  bleibt Primäranzeige.
- Keine APK-/Split-APK-Pfadinformationen, kein UID/sharedUserId, kein
  compileSdk, keine nativen-ABI-Hinweise je App.
- Rollen beschränkt auf Launcher/Standardbrowser (siehe „App-Rollen“) -
  WebView/VPN/Device-Admin/IME/Kiosk bewusst zurückgestellt.
- Keine Berechtigungszahlen erfasst (im Auftrag als optional beschrieben,
  aus Zeit-/Umfangsgründen in diesem Schritt nicht umgesetzt).
- Die „Vollständig“-Schwelle (mehr als die Hälfte der Pakete mit
  Detaildaten) ist eine nachvollziehbare, aber bewusst einfache Heuristik -
  keine herstellerspezifische Feinkalibrierung.

## Abgrenzung zu späteren Android-Schritten

40k34c umfasst: installierte Apps/Pakete, Benutzer-/System-Apps, Versionen,
Zeitstempel, Installationsquelle, Aktivierungsstatus, zentrale Rollen
(Launcher/Browser), Tabelle/Detailansicht/Filter/Suche/Sortierung,
vollständige/teilweise Läufe, Änderungserkennung, Integration in die
bestehende Android-Inventarisierung.

**Nicht Teil von 40k34c:** App-Installation/-Deinstallation, App-Start/
-Stopp, App-Daten löschen, Berechtigungen ändern, vollständige
Berechtigungsanalyse, Malware-Erkennung, Risikoscore, Datenschutzbewertung
einzelner Apps, Nutzungsstatistiken, App-Inhalte, APK-Download/-Analyse,
Screenshots, Bildschirmspiegelung, Fernsteuerung, MDM, Kiosk-Konfiguration
ändern, Device-Owner-Einrichtung, Root-Funktionen.
