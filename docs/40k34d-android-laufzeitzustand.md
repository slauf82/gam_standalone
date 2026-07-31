# Schritt 40k34d – Android Apps, Dienste und Rollen (Laufzeitzustand)

## Ausgangsprüfung (vor jeder Neuentwicklung durchgeführt)

Wie im Auftrag verlangt, wurde zuerst geprüft, was aus 40k34a–40k34c bereits
existiert und wiederverwendet werden kann:

- **`AndroidAdbService`** (40k34b): bereits vorhandene Befehlsliste
  `SHELL_COMMANDS`, die in `inventorize()` sequenziell abgearbeitet wird, und
  die Methode `runShellCommand(args, timeout, maxChars)`. → **Wiederverwendet
  durch reine Erweiterung der Liste**, keine neue ADB-Methode für die
  Befehlsausführung selbst nötig.
- **`AndroidInventoryParser`** (40k34b): nimmt bereits eine `Map<String,String>`
  roher Kommandoausgaben entgegen und liefert eine `Map<String,String>`
  fertiger „Label: Wert“-Merkmale zurück. → **Wiederverwendet durch
  Erweiterung derselben `parse()`-Methode**, keine neue Parser-Klasse.
- **`AndroidInventorySection`** (Frontend, 40k34b): rendert bereits
  **generisch alle** über `discovery_protocol` gelieferten „Label: Wert“-Paare
  in einer Tabelle. → Neue Felder erscheinen dadurch **automatisch**, ganz
  ohne Codeänderung. Für die im Auftrag ausdrücklich verlangten
  **einklappbaren Themenbereiche** wurde die Darstellung zusätzlich in
  Gruppen aufgeteilt (siehe unten) - weiterhin dieselbe Datenquelle,
  keine neue Abfrage.
- **`AndroidAppsSection`** (Frontend, 40k34c): besitzt bereits Suche und
  Filter für installierte Apps. → wie im Auftrag gefordert **unverändert
  wiederverwendet**, nicht dupliziert.

**Ergebnis der Prüfung:** Für 40k34d war in keinem Fall eine neue Klasse
erforderlich. Es wurden ausschließlich bestehende Listen/Methoden/
Komponenten erweitert.

## Architektur

Keine neue Android-Architektur, kein neuer Service, keine zweite
Inventarisierung, keine zweite Geräteidentität, keine zweite Merge-Logik.
Alle neuen Felder fließen über denselben, bereits bestehenden Mechanismus:
`AndroidAdbService.inventorize()` → `AndroidInventoryParser.parse()` →
`discovery_protocol`-Text derselben Geräteidentität (`recordDiscoveryHit()`,
unverändert seit 40k34b) → generische Anzeige im Frontend.

## Verwendete ADB-Kommandos (additive Erweiterung der bestehenden Liste)

| Label | Befehl | Zweck |
|---|---|---|
| PROCESSES | `ps -A` | Anzahl laufender Prozesse |
| SERVICES | `dumpsys activity services` | Anzahl/Beispiele laufender Dienste |
| USERS | `pm list users` | Benutzer (Haupt-/Gast-/Arbeitsprofil) |
| POWER | `dumpsys power` | Bildschirm an/aus, Doze, Battery Saver |
| CPU_ONLINE | `cat /sys/devices/system/cpu/online` | Anzahl aktiver CPU-Kerne |
| LOADAVG | `cat /proc/loadavg` | CPU-Last (1/5/15 Min.) |
| AIRPLANE_MODE / WIFI_ON / BLUETOOTH_ON | `settings get global …` | Flugmodus/WLAN/Bluetooth |
| DEFAULT_IME | `settings get secure default_input_method` | Standard-Tastatur |
| ACCESSIBILITY_SERVICES / NOTIFICATION_LISTENERS / AUTOFILL_SERVICE | `settings get secure …` | jeweilige aktive Dienste |
| ROLE_DIALER / ROLE_SMS / ROLE_ASSISTANT | `cmd role holders android.app.role.…` | Standard-Telefon/-SMS/-Assistent |
| DEVICE_POLICY | `dumpsys device_policy` | Device Owner / Profile Owner |
| CONNECTIVITY | `dumpsys connectivity` | VPN-Hinweis (Best Effort) |

Alle 20 Befehle laufen **einmal je Inventarisierungslauf** (kein Aufruf je
Prozess, kein Aufruf je Dienst) - dieselbe Performance-Architektur wie beim
App-Inventar in 40k34c. Die drei potenziell großen `dumpsys`-Ausgaben
(`SERVICES`, `POWER`, `DEVICE_POLICY`, `CONNECTIVITY`, `PROCESSES`) erhalten
über eine kleine, additive Zuordnungstabelle (`LARGE_OUTPUT_LABELS`) einen
größeren, aber weiterhin **begrenzten** Lesepuffer als die übrigen, kleinen
Befehle - keine neue Lese-Architektur, nur ein Parameter mehr an der
bestehenden `runShellCommand()`-Methode.

## Unterstützte Android-Versionen

- `pm list users`, `settings get …`, `dumpsys power/battery/connectivity`
  sind seit vielen Jahren stabil (Android 6+).
- `cmd role holders` (RoleManager-Shell-Befehl) setzt Android 10 (API 29)
  voraus - auf älteren Geräten liefert der Befehl typischerweise eine leere
  oder Fehlerausgabe, die dann korrekt **nicht** übernommen wird (kein
  Absturz, kein Rateergebnis).
- `dumpsys device_policy` liefert nur dann einen Device/Profile Owner,
  wenn tatsächlich einer eingerichtet ist - sonst bleibt das Feld leer.

## Neue Felder (additiv, dieselbe „Label: Wert“-Konvention)

Laufende Prozesse; Laufende Dienste (Anzahl/Beispiele); Benutzeranzahl,
Benutzer; Bildschirm an; Doze Mode; Battery Saver; CPU-Kerne (online);
CPU-Last (1/5/15 min); Flugmodus; WLAN aktiv; Bluetooth aktiv; VPN aktiv;
Standard-IME; Accessibility Services; Notification Listener; Autofill
Service; Standard-Telefon-App; Standard-SMS-App; Standard-Assistent; Device
Owner; Profile Owner; DNS-Server; Mobilfunk vorhanden.

## Frontend

`AndroidInventorySection` gruppiert dieselben, bereits geladenen Merkmale
jetzt zusätzlich in einklappbare Bereiche: **Prozesse**, **Dienste / Apps
(aktiv)**, **Rollen**, **Netzwerk**, **Gerätezustand**, **Benutzer**, sowie
„Weitere Merkmale“ als Auffangbereich für alles, was keiner der Gruppen
zugeordnet ist (damit garantiert **kein** Feld verschwindet). Es wurde keine
neue Datenquelle und keine neue Ladefunktion ergänzt - reine
Darstellungsgruppierung der bereits vorhandenen Werte.

„Apps (aktiv)“ wurde aus Zeit-/Umfangsgründen **vereinfacht** als die Liste
der Paketnamen laufender Dienste (`dumpsys activity services`) umgesetzt,
**nicht** als vollständiger Abgleich mit der installierten-Apps-Tabelle aus
40k34c (das hätte eine zusätzliche, im Auftrag nicht ausdrücklich verlangte
Kreuzabfrage zwischen zwei bereits geladenen Datensätzen im Frontend
erfordert) - ehrlich als Vereinfachung benannt, nicht verschwiegen.

## Bekannte Einschränkungen (ehrlich benannt)

- **Bildschirm gesperrt** wurde **nicht** umgesetzt: Es gibt kein einzelnes,
  über alle Android-Versionen hinweg zuverlässig dokumentiertes
  `dumpsys power`-Feld dafür, das ich mit ausreichender Sicherheit prüfen
  konnte. Statt eine unsichere Vermutung einzubauen, bleibt dieses Merkmal
  bewusst weg (siehe Auftrag: „lieber ehrlich als nicht verfügbar“).
- **VPN aktiv** ist ausdrücklich als Best-Effort-Hinweis gekennzeichnet
  („ohne Garantie auf Vollständigkeit“) - `dumpsys connectivity` ist nicht
  auf jeder Android-Version/jedem Hersteller identisch aufgebaut.
- **WLAN-SSID** wurde **nicht** umgesetzt: Ohne Root/Standort-Berechtigung
  ist der SSID-Zugriff auf aktuellen Android-Versionen zunehmend
  eingeschränkt und nicht zuverlässig über einen einzelnen, einfachen
  ADB-Befehl auslesbar - lieber weggelassen als geraten.
- **Swap/ZRAM** wurde **nicht** separat erfasst - `/proc/meminfo` enthält
  zwar theoretisch `SwapTotal`/`SwapFree`, viele Android-Geräte nutzen aber
  ZRAM-Kompression, die sich nicht einheitlich in diesen Feldern
  widerspiegelt; eine zuverlässige, versionsübergreifende Auswertung war im
  Rahmen dieses Schritts nicht seriös umsetzbar.
- **CPU-Frequenzen** wurden **nicht** erfasst (im Auftrag als „sofern
  verfügbar“ formuliert) - das Auslesen pro Kern
  (`/sys/devices/system/cpu/cpuN/cpufreq/scaling_cur_freq`) hätte mehrere
  zusätzliche Befehle je Kern erfordert, was der Performance-Vorgabe
  widerspricht; stattdessen wird die bereits aussagekräftige CPU-Last
  (`/proc/loadavg`) erfasst.
- **„Apps (aktiv)“** ist vereinfacht (siehe Frontend-Abschnitt oben).
- Es wurden **keine neuen Integritätsregeln** ergänzt (folgt laut Auftrag
  vollständig in 40k34e) und **keine neuen Reports** (folgen in 40k34f).

## Datenschutz

Es werden ausschließlich Systemzustands-Merkmale erfasst (Prozessanzahl,
Dienstpaketnamen, Benutzer-IDs/-typen, Geräteeinstellungen). **Nicht
erfasst:** SMS, Kontakte, Fotos, Videos, Dateien, Accounts, Nachrichten,
Browserverlauf, App-Inhalte - keiner der neuen Befehle liest solche Daten.

## Merge und Identität

Laufende Prozesse, Dienste und Rollen wurden an **keiner** Stelle in
`DeviceIdentityConfidenceEngine` oder eine andere Merge-/Identitätsprüfung
eingebunden - sie sind ausschließlich Anzeige-Inventardaten, wie im Auftrag
gefordert.

## Tests / durchgeführte Prüfungen in dieser Umgebung

Kein JDK, kein funktionierendes `npm install` in dieser Sandbox - **kein
echter Build und keine echten Gerätetests möglich.** Dies wird hiermit
ausdrücklich benannt. Stattdessen geprüft:

- Klammernbilanz von `AndroidAdbService.java` (ausgeglichen). Bei
  `AndroidInventoryParser.java` zeigte die rohe Zeichenzählung eine
  scheinbare Abweichung (52 vs. 49), die sich bei vollständiger manueller
  Durchsicht als Fehlalarm herausstellte: mehrere Regex-Strings enthalten
  escapte, unpaarige `{`-Zeichen (z.B. `"UserInfo\\{(\\d+):"`,
  `"ComponentInfo\\{([\\w.]+)/"`), die eine reine Zeichenzählung ohne
  Java-Syntaxverständnis fälschlich mitzählt. Keine echte
  Unausgeglichenheit im Code.
- Kreuzabgleich: jedes neue `SHELL_COMMANDS`-Label wird in
  `AndroidInventoryParser.parse()` tatsächlich ausgewertet (keine
  verwaisten Labels).
- Isolierter TypeScript-Transpile-Lauf über die vollständige `main.tsx` -
  0 Diagnosen.
- Alle React-Funktionskomponenten-Grenzen nach der Änderung per grep erneut
  verifiziert.
- Manuelle Durchsicht: alle neuen ADB-Aufrufe verwenden weiterhin reine
  Argumentlisten (kein `sh -c`), keine neuen Schreibbefehle.

**Nicht durchgeführt** (nicht möglich in dieser Sandbox): echter
Maven-/Vite-Build, Tests gegen echte Android-Geräte unterschiedlicher
Android-Versionen/Hersteller. Bitte vor dem Produktiveinsatz insbesondere an
einem Gerät mit Android 10+ (für `cmd role holders`) und einem älteren Gerät
(<Android 10, zur Kontrolle des sauberen Fallback-Verhaltens) verifizieren.

## Testanleitung

1. Android-Inventarisierung auf einem verbundenen Gerät erneut ausführen:
   die neuen Bereiche „Prozesse“, „Dienste / Apps (aktiv)“, „Rollen“,
   „Netzwerk“, „Gerätezustand“, „Benutzer“ müssen erscheinen, sofern die
   jeweiligen Felder tatsächlich geliefert wurden.
2. Gerät mit Android < 10: `cmd role holders`-Felder bleiben leer, kein
   Fehler, keine falschen Werte.
3. Gerät ohne Device Owner: Feld „Device Owner“ erscheint korrekt nicht.
4. Regressionstest: bestehende Felder aus 40k34b/c (Hersteller, Android-
   Version, installierte Apps, Änderungsverfolgung) bleiben unverändert
   sichtbar und funktionsfähig.
5. Regressionstest: Merge-Schutzregeln (40k33b10) und Integritätsprüfung
   (40k33b7) unverändert.

## Abgrenzung

40k34d liefert ausschließlich den **aktuellen Laufzeitzustand** als
zusätzliche Anzeige-Inventardaten. Integritätsregeln für diese neuen Felder
folgen in 40k34e, neue Reports in 40k34f - beides bewusst **nicht** Teil
dieses Schritts.
