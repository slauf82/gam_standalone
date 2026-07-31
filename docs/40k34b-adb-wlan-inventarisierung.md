# Schritt 40k34b – ADB-Unterstützung und Android-Inventarisierung über WLAN

## Architektur

ADB ist eine **optionale Tiefeninventarisierungsquelle** für Android -
architektonisch gleichrangig zu SSH bei Linux (`LinuxNetworkDiscoveryService`).
Es wurde **keine** neue Geräteverwaltung, **keine** zweite Geräteidentität,
**keine** Android-spezifische Datenbank, **keine** zweite Merge-/
Integritätslogik und **keine** zweite Discovery-Architektur geschaffen.

- `AndroidAdbService` (neu) übernimmt exakt die Rolle, die
  `LinuxNetworkDiscoveryService` für SSH bereits hat: Werkzeug-Erkennung,
  Statusermittlung, Verbindungsaufbau, strukturierte, lesende
  Shell-Inventarisierung.
- `AndroidInventoryParser` (neu) übernimmt die Rolle, die die
  `remoteScript()`-Auswertung bei Linux hat: reine Textauswertung bereits
  abgerufener Ausgaben.
- Die Ergebnisse werden - wie bei Linux und Windows - über die bereits
  bestehende `recordDiscoveryHit()` in **dieselbe** Geräteidentität
  geschrieben. Es entsteht dabei nie ein zweites Gerät.
- Merge, Integritätsprüfung, Reportkategorisierung und die
  Schutzstufen-Darstellung aus 40k33b10 wurden **nicht verändert** - ADB
  liefert lediglich zusätzliche Werte in dieselben, bereits bestehenden
  Felder (`hardwareAddress`, `serialNumber`, `discovery_protocol`).

## Prüfung vorhandener Architektur für externe Werkzeuge (vor der Umsetzung)

Es existiert bereits ein etabliertes Muster für externe Kommandozeilen-
werkzeuge (`LinuxNetworkDiscoveryService` für `ssh`): konfigurierbarer Pfad
über Spring-`Environment`/Umgebungsvariable, sonst Suche im `PATH`,
`ProcessBuilder` mit Timeout, kein automatischer Download. `AndroidAdbService`
folgt exakt demselben Muster für `adb`:

1. `gam.discovery.android.adb-path` / `GAM_ANDROID_ADB_PATH` (konfigurierter Pfad)
2. `adb` aus dem `PATH`

Kein mitgeliefertes ADB-Binary wurde ergänzt: Das Projekt besitzt keine
allgemeine, sicher abgesicherte Downloadarchitektur für externe Werkzeuge
(bestätigt durch dieselbe Prüfung, die bereits für SSH galt) - deshalb wurde,
wie im Auftrag gefordert, keine neue Downloadlogik eingeführt.

## Unterstützte ADB-Modi

1. **Klassisches ADB over TCP/IP** - `adb connect HOST:PORT` (Vorschlagsport
   5555, editierbar).
2. **Modernes Wireless Debugging (Android 11+)** - zweistufig:
   - `adb pair HOST:PAIRING_PORT PAIRING_CODE` (6-stelliger Code)
   - anschließend `adb connect HOST:ADB_PORT` mit einem **eigenständigen**,
     vom Pairing-Port unabhängigen Verbindungsport (Oberfläche verlangt
     dafür ausdrücklich zwei getrennte Eingaben, wie im Auftrag gefordert).
3. **Bereits verbundene Geräte** - `adb devices -l`, inkl. Unterscheidung
   `device`/`unauthorized`/`offline`/unbekannt.
4. **Begrenzte automatische Wiederverbindung** - `reconnectKnownAndroidDevices()`
   verbindet gezielt zu den in der Geräteidentität gespeicherten, bereits
   bekannten ADB-Zielen, maximal 5 Versuche pro Aufruf, kein Dauerpolling.

## Einrichtung unter Android 11 und neuer (Wireless Debugging)

1. Auf dem Android-Gerät: Einstellungen → Über das Telefon → siebenmal auf
   die Build-Nummer tippen (Entwickleroptionen aktivieren).
2. Entwickleroptionen → **Wireless-Debugging** aktivieren.
3. „Mit Pairing-Code koppeln“ antippen - IP-Adresse, Pairing-Port und
   6-stelliger Code werden angezeigt.
4. In GAM: Dialog „Android-Gerät koppeln“ öffnen, IP-Adresse, Pairing-Port
   und Pairing-Code eintragen, „Gerät koppeln“ klicken.
5. Nach erfolgreichem Pairing zeigt Android einen **separaten** ADB-
   Verbindungs-Port unter „Wireless-Debugging“ an (nicht der Pairing-Port!).
   Diesen im Feld „ADB-Verbindungs-Port“ eintragen und „Verbinden“ klicken.

## Klassisches ADB over TCP/IP (ältere Geräte)

Erfordert einmalig eine USB-Verbindung zur Aktivierung
(`adb tcpip 5555` extern vom Benutzer ausgeführt, GAM selbst führt diesen
Befehl nicht aus, da er ein bereits per USB autorisiertes Gerät voraussetzt
und außerhalb des in diesem Schritt beschriebenen, rein netzwerkbasierten
Funktionsumfangs liegt). Danach im Dialog „ADB-Verbindung herstellen“
IP-Adresse und Port (Vorschlag 5555) eintragen.

## Sicherheitshinweise

- **Kein Root, kein `su`, kein `adb root`.** Alle Inventarisierungsbefehle
  sind fest vordefiniert und ausschließlich lesend.
- **Keine Shell-Verkettung.** Jeder ADB-Aufruf erfolgt über
  `ProcessBuilder` mit einer Argumentliste (`List<String>`), niemals über
  eine zusammengesetzte Shell-Zeile (kein `sh -c`, kein `cmd /c`).
- **Validierung vor jedem Aufruf:** Host/Hostname und Port werden geprüft,
  bevor sie überhaupt in einen `ProcessBuilder`-Aufruf gelangen.
- **Pairing-Code:** wird nur für den einen Kopplungsaufruf verwendet,
  niemals protokolliert, niemals in eine Fehlermeldung eingebettet, niemals
  in der Datenbank gespeichert.
- **stdout/stderr getrennt:** Shell-Befehle laufen ohne
  `redirectErrorStream` - Fehlerausgaben des Geräts vermischen sich nicht
  mit den erfassten Nutzdaten.
- **Timeouts und Begrenzung:** jeder Prozess hat ein festes Timeout
  (5–15 s je nach Aktion), die gelesene Ausgabe ist auf maximal 40.000
  Zeichen begrenzt (Schutz vor sehr großen, ungefilterten Ausgaben).
- **Kein freies Kommando:** Es gibt keinen Endpunkt, der ein vom Benutzer
  frei eingebbares ADB-Kommando ausführt.

## Inventarisierte Felder

Erfasst über `adb -s <SERIAL> shell getprop` (Großteil der Felder),
ergänzt um `uname -a`, `cat /proc/meminfo`, `df`, `wm size`, `wm density`,
`dumpsys battery`, `uptime`, `getenforce`, `ip -4 -o addr show scope global`
- jeder Befehl einzeln mit eigenem Timeout, ein fehlender/fehlgeschlagener
Befehl bricht die übrigen nicht ab.

| Bereich | Felder |
|---|---|
| Gerät | Hersteller, Marke, Modell, Produktname, Gerätename, Gerätecodename, Hardwarebezeichnung, Seriennummer (nur falls vom Gerät freigegeben) |
| Android | Android-Version, API-Level, Buildnummer, Build-ID, Build-Fingerprint, Sicherheits-Patch-Level, Build-Typ, Build-Tags |
| Hardware | CPU-Architektur, unterstützte ABIs, Hardwareplattform, SoC-Bezeichnung (falls verfügbar), Kernel-Version, Kernel-Architektur |
| Speicher | Speicher belegt/verfügbar/gesamt (`/data`) |
| Arbeitsspeicher | gesamter RAM, verfügbarer RAM |
| Display | Auflösung, Dichte (DPI) |
| Akku | Ladezustand, Akkustatus, Ladeart, Gesundheitsstatus, Temperatur, Spannung |
| Netzwerk | IP-Adresse (aus vorhandener globaler IPv4-Adresse) |
| System | Uptime, Zeitzone, Sprache/Locale, Geräteverschlüsselungsstatus, SELinux-Status, Debug-/Entwicklerstatus |

**Nicht erfasst** (bewusst, siehe Auftrag): Bildwiederholrate (auf den
meisten Geräten ohne Root nicht zuverlässig auslesbar), App-Daten,
Benutzerdateien, WLAN-Zugangsdaten.

## Zuordnung zur bestehenden Geräteidentität

Ein über ADB verbundenes Gerät wird über dieselbe `recordDiscoveryHit()`-
Schreiblogik in die Identität geschrieben, die auch der Benutzer bereits im
Dialog „Geräteidentität“ geöffnet hat (`identityKey` wird beim Verbinden
optional mitgegeben und dabei mit `adb_host`/`adb_port` verknüpft) - es
entsteht dabei kein neues Gerät.

Die vom Gerät gemeldete Seriennummer wird - genau wie bei jeder anderen
Discoveryquelle - über die bereits bestehende `serialNumber`-Spalte
geschrieben (nur wenn dort noch kein Wert steht, siehe bestehendes
`COALESCE`-Verhalten in `recordDiscoveryHit()`). Dadurch greift die
**bereits vorhandene** Konflikterkennung (`DeviceIdentityConfidenceEngine`,
seit 40k33b10 mit sichtbarer Schutzstufen-Einordnung) automatisch auch für
ADB-Seriennummern-Widersprüche - **keine zweite Android-Konfliktlogik**.

Der Build-Fingerprint wird zusätzlich als informatives Feld in die bereits
bestehende Feld-Gegenüberstellung (`compareIdentityFields()`, seit 40k33b10
für Merge-Dialog und Integritätsprüfung gemeinsam genutzt) aufgenommen - rein
zur Anzeige, ohne eine neue harte Konfliktregel zu erzeugen.

## Statusmeldungen (neutral / Warnung / Fehler / Autorisierung)

- **Neutral:** „ADB ist nicht eingerichtet. Die Android-Erkennung ohne ADB
  bleibt verfügbar.“ - erscheint, wenn `adb` nicht gefunden wurde. Kein
  Fehler, keine rote Darstellung.
- **Warnung:** Gerät offline, erneutes Pairing erforderlich, unvollständige
  Inventarisierung.
- **Fehler:** ADB-Prozess konnte nicht gestartet werden, Zeitüberschreitung.
- **Autorisierung:** „Bitte bestätigen Sie die ADB-Autorisierung auf dem
  Android-Gerät.“ - erscheint bei `unauthorized`-Geräten, ausdrücklich nicht
  als technischer Fehler dargestellt.

## Inventarisierungsgrad und Quellenübersicht

Wiederverwendung derselben "· Label: Value"-Konvention und desselben
`discovery_protocol`-Mechanismus wie bei Linux (40k33b9). Die bereits
bestehende Quellenübersicht (`Verwendete Quellen`, `Inventarisierungsgrad`)
zeigt jetzt automatisch auch „ADB“ als Quelle, sobald eine erfolgreiche
ADB-Inventarisierung stattgefunden hat (da diese denselben
`discovery_protocol`-Text ergänzt, den auch `distinctSourceLabels()`
bereits auswertet).

## Neue REST-Endpunkte

- `GET /api/inventory/discovery/identity/android/adb-status`
- `POST /api/inventory/discovery/identity/android/pair`
- `POST /api/inventory/discovery/identity/android/connect`
- `POST /api/inventory/discovery/identity/android/disconnect`
- `POST /api/inventory/discovery/identity/android/inventory`
- `POST /api/inventory/discovery/identity/android/reconnect-known`

## Neue Datenbankmigration

Rein additiv, auf der bereits bestehenden Tabelle
`gam_discovery_registered_devices` (dasselbe `information_schema`-geprüfte
Migrationsmuster wie bei allen bisherigen additiven Spalten):

- `adb_host VARCHAR(255) NULL`
- `adb_port INT NULL`
- `adb_last_connected_at TIMESTAMP NULL`

**Nicht gespeichert:** Pairing-Code, temporäre Autorisierungstoken,
vollständige/ungefilterte Shell-Ausgaben.

## Logging

`AndroidAdbService` protokolliert (Info-Ebene, keine übermäßigen
Debug-Ausgaben): gefundenen ADB-Pfad und Version, Start/Ende jeder
Inventarisierung samt gekürzter Geräte-Kennung (`shortSerial()` - niemals
die volle Seriennummer), Verbindungsart (Netzwerk erkennbar an
`host:port`-Format der Seriennummer), Status je Verbindungsversuch,
erfolgreich ausgewertete Merkmalsanzahl, Dauer. **Niemals protokolliert:**
Pairing-Code, vollständige Shell-Ausgaben.

## Bekannte Einschränkungen

- Es gibt keine Möglichkeit, `adb tcpip 5555` selbst über USB anzustoßen -
  das erfordert eine bereits per USB autorisierte Verbindung und liegt
  außerhalb des in diesem Schritt beschriebenen, netzwerkbasierten
  Funktionsumfangs (siehe Abgrenzung im Auftrag: kein dauerhaftes
  USB-Kabel).
- Bildwiederholrate wird nicht erfasst (ohne Root auf den meisten Geräten
  nicht zuverlässig auslesbar).
- Die automatische Wiederverbindung läuft nur, wenn explizit ausgelöst
  (Endpunkt `android/reconnect-known`) - sie ist NICHT automatisch in den
  regulären Netzwerk-Suchlauf (`DeviceDiscoveryService.scanStreaming()`)
  integriert, um die bestehende Discovery-Pipeline nicht zusätzlich zu
  verändern und das Risiko von Regressionen dort zu vermeiden.
- SoC-Bezeichnung ist nur auf einem Teil der Geräte über `getprop`
  auslesbar (herstellerabhängig) - bleibt sonst leer, wird nicht geraten.

## Tests / durchgeführte Prüfungen in dieser Umgebung

Kein JDK, kein funktionierendes `npm install` in dieser Sandbox - kein
echter Build und keine echten ADB-Tests gegen ein reales Gerät möglich.
**Dies wird hiermit ausdrücklich benannt.** Stattdessen geprüft:

- Klammernbilanz aller fünf neuen/geänderten Backend-Dateien (ausgeglichen).
- Kreuzabgleich aller neuen Methodensignaturen zwischen Controller und
  Service.
- Isolierter TypeScript-Transpile-Lauf über `client.ts` und die vollständige
  `main.tsx` - 0 Diagnosen.
- Alle neuen React-Funktionskomponenten-Grenzen nach der Einfügung per grep
  verifiziert.
- Manuelle Prüfung, dass sämtliche `ProcessBuilder`-Aufrufe in
  `AndroidAdbService` ausschließlich Argumentlisten verwenden (kein
  `sh -c`/`cmd /c`, keine zusammengesetzten Shell-Strings) und dass
  Host/Port vor jedem Aufruf validiert werden.
- Manuelle Durchsicht: Pairing-Code wird an keiner Stelle protokolliert,
  gespeichert oder in eine Fehlermeldung eingebettet.

**Nicht durchgeführt** (nicht möglich in dieser Sandbox): echter
Maven-/Vite-Build, jegliche der 17 im Auftrag genannten Testfälle gegen
echte Android-Geräte oder eine echte ADB-Installation. Bitte vor dem
Produktiveinsatz mit mindestens einem echten Android-Gerät (idealerweise
mit modernem Wireless Debugging) verifizieren.

## Testanleitung (manuell, an echten Geräten nachzuholen)

1. **ADB nicht installiert:** Bereich „Android-Inventarisierung“ öffnen -
   neutrale Meldung „ADB ist nicht eingerichtet…“ muss erscheinen, keine
   rote Fehlerdarstellung.
2. **ADB installiert, kein Gerät:** „ADB prüfen“ klicken - Status mit
   Version/Pfad, 0 verbundene Geräte.
3. **Pairing (Wireless Debugging):** Dialog „Android-Gerät koppeln“
   durchlaufen, danach mit separatem ADB-Port verbinden.
4. **Klassisches TCP/IP:** Dialog „ADB verbinden“ mit Port 5555.
5. **Falscher/abgelaufener Pairing-Code:** verständliche Fehlermeldung,
   kein Rohfehler.
6. **`unauthorized`:** Hinweistext zur Bestätigung auf dem Gerät, kein
   Programmfehler.
7. **`offline`:** als Warnung, nicht als Fehler dargestellt.
8. **Mehrere gleichzeitig verbundene Geräte:** jede Inventarisierung muss
   über die korrekte Seriennummer adressiert sein - keine Datenvermischung.
9. **Bereits über mDNS erkanntes Gerät wird durch ADB angereichert:**
   Identität verbinden, Inventarisierung starten - Felder erscheinen in
   **derselben** Geräteidentität, kein zweites Gerät entsteht.
10. **Kritischer Identitätskonflikt (unterschiedliche Seriennummern):** die
    bereits bestehende 🔴-Darstellung aus 40k33b10 muss unverändert
    erscheinen, inkl. Bestätigungspflicht vor dem Zusammenführen.
11. **Trennen und erneut verbinden:** „ADB trennen“, danach erneut über
    „ADB verbinden“ verbinden - keine globale Trennung aller Geräte.
12. **Neustart von GAM mit bekannten ADB-Zielen:** `android/reconnect-known`
    aufrufen - begrenzte Anzahl Versuche, nachvollziehbares Ergebnis je
    Gerät.

## Abgrenzung zu späteren Android-Schritten

40k34b umfasst ADB-Erkennung/-Status, ADB über WLAN (klassisch und
Wireless Debugging mit Pairing), Connect/Disconnect, begrenzte automatische
Wiederverbindung, eine erste echte, lesende Android-Systeminventarisierung,
Integration in die bestehende Geräteidentität, Inventarisierungsgrad und
Quellenanzeige.

**Nicht Teil von 40k34b:** vollständige App-Inventarisierung,
App-Berechtigungsanalyse, App-Nutzungsstatistiken, Dateiübertragung,
Screenshots, Fernsteuerung, Installation/Deinstallation von Apps,
Android-MDM, Root-Funktionen, Änderung von Systemeinstellungen, frei
ausführbare ADB-Kommandos.
