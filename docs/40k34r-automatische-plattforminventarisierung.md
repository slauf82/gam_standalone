# Schritt 40k34r – Automatische Plattforminventarisierung und vollständiger Inventarisierungsworkflow

## Warum die automatische Inventarisierung bisher nicht stattfand (Analyse)

Der Discovery-Workflow (`DeviceDiscoveryService.scanStreaming()`) endete bisher
nach der Konsolidierung/Nachklassifizierung (40k34h–n) - er schrieb Discovery-
Grunddaten und ggf. eine korrigierte Kategorie in die Geräteidentität, rief
aber **an keiner Stelle** den in 40k34p eingeführten Plattform-Dispatcher
(`DeviceIdentityService.runPlatformInventory()`) auf. Die Plattforminventarisierung
existierte bereits vollständig (Android, Linux, Windows lokal/remote), war
aber ausschließlich über einen **manuellen** Klick im Geräteidentitäts-Dialog
erreichbar - es gab keinen Code-Pfad, der sie nach einer Discovery automatisch
ausgelöst hätte. `DeviceDiscoveryService` und `DeviceIdentityService` waren
zwei vollständig unverbundene Komponenten in dieser Hinsicht.

## Erweiterte Klassen (keine Parallelarchitektur)

| Klasse | Änderung |
|---|---|
| `DeviceDiscoveryService` | neue Abhängigkeit `DeviceIdentityService`; ein neuer Aufruf `deviceIdentityService.autoTriggerPlatformInventoryIfEligible(identityKey)` in der bereits bestehenden Nachklassifizierungs-Schleife (40k34h) |
| `DeviceIdentityService` | neue Methode `autoTriggerPlatformInventoryIfEligible()`; Extraktion der bereits bestehenden Plattform-Ermittlungslogik aus `runAllKnownPlatforms()` (40k34p) in eine gemeinsame private Hilfsmethode `applicablePlatforms()`; neue Zulässigkeitsprüfung in `runPlatformInventory()` (verhindert Doppelläufe) |
| `PlatformInventoryRepository` | zwei neue, additive Abfragen: `isRunning()`, `recentlyRun()` - dieselbe, bereits bestehende Tabelle aus 40k34p, keine neue Statusverwaltung |
| `PlatformInventorySection` (Frontend) | Button umbenannt in „▶ Neu inventarisieren" (identische Funktion wie bisher, kein zweiter Button); neues, sich selbst begrenzendes Nachfragen, solange eine Plattform „läuft" |

**Keine neue API, kein neuer Endpunkt** - der automatische Trigger nutzt
ausschließlich die bereits in 40k34p bestehende `runPlatformInventory()`-
Methode, genau wie die manuelle Aktion.

## Automatischer Workflow

```text
Discovery erkennt/bestätigt ein bereits registriertes Gerät (im bereits
  bestehenden Nachklassifizierungs-Durchlauf, 40k34h)
↓
DeviceDiscoveryService ruft deviceIdentityService.
  autoTriggerPlatformInventoryIfEligible(identityKey) auf
↓
DeviceIdentityService ermittelt (bereits bestehende Logik aus
  runAllKnownPlatforms(), jetzt als applicablePlatforms() extrahiert),
  welche Plattform(en) zum Gerät passen
↓
Für jede passende Plattform: Prüfung "läuft bereits?" und "Cooldown
  aktiv?" (siehe unten)
↓
Falls beides negativ: asynchroner Start über einen eigenen Executor
  (Java-Virtual-Threads) - Discovery wartet NICHT auf das Ergebnis
↓
runPlatformInventory() (bereits bestehend, 40k34p/q) läuft wie beim
  manuellen Klick - inkl. Historie, Status, recordDiscoveryHit()
```

**Discovery bleibt schnell:** `autoTriggerPlatformInventoryIfEligible()`
selbst kehrt sofort zurück (die Zulässigkeitsprüfungen sind reine, schnelle
Datenbankabfragen); die eigentliche Inventarisierung läuft in einem separaten
virtuellen Thread (`Executors.newVirtualThreadPerTaskExecutor()`) - der
Suchlauf selbst wartet an keiner Stelle auf deren Abschluss.

## Unterstützte Plattformen (dieselbe Logik wie bei der manuellen „Alle
Plattformen"-Aktion)

Die Ermittlung, welche Plattform zu einem Gerät passt, war bereits in
40k34p für `runAllKnownPlatforms()` vorhanden (ADB-Verbindung/Plattformfeld/
Protokolltext) - diese Logik wurde **nicht dupliziert**, sondern in
`applicablePlatforms()` extrahiert und von **beiden** Stellen
(`runAllKnownPlatforms()` und dem neuen automatischen Trigger) gemeinsam
genutzt.

## Cooldown

Ein neuer, additiver Datenbank-Check
(`PlatformInventoryRepository.recentlyRun()`) verhindert, dass dasselbe
Gerät bei jedem Suchlauf erneut automatisch inventarisiert wird - Standard
**30 Minuten** seit dem letzten Lauf **derselben Plattform**. Der Cooldown
gilt **ausdrücklich nur für die automatische Auslösung** - die manuelle
„Neu inventarisieren"-Aktion bleibt jederzeit ohne Wartezeit möglich (der
Cooldown-Check wird dort nicht aufgerufen).

## Schutz vor Doppelinventarisierung

Ein zweiter, additiver Check (`isRunning()`) verhindert unabhängig vom
Cooldown, dass für dasselbe Gerät und dieselbe Plattform zwei
Inventarisierungen gleichzeitig laufen - dieser Schutz wurde direkt in
`runPlatformInventory()` selbst eingebaut und gilt daher **sowohl** für die
automatische **als auch** die manuelle Auslösung einheitlich.

## Manuelle Neuinventarisierung (UI)

Der bereits in 40k34p vorhandene Button in `PlatformInventorySection`
(„Alle bekannten Plattformen erneut inventarisieren") wurde in
**„▶ Neu inventarisieren (alle bekannten Plattformen)"** umbenannt - **kein
zweiter, separater Button wurde ergänzt**, da der bestehende Button bereits
exakt die im Auftrag geforderte Funktion erfüllt: er startet ausschließlich
die Plattforminventarisierung erneut, keine neue Discovery. Das
„Optional"-Angebot aus dem Auftrag („Nur Plattform inventarisieren" /
„Discovery erneut starten", „falls bereits sinnvoll getrennt vorhanden")
war bereits vorhanden: die einzelnen „Inventarisieren"-Buttons je
Plattformzeile entsprechen „Nur Plattform inventarisieren", und die
allgemeine Netzwerksuche bleibt die bereits bestehende, unabhängige
Discovery-Aktion.

## Oberflächenaktualisierung nach Abschluss

Da die automatische Inventarisierung asynchron im Hintergrund läuft (die
HTTP-Antwort des Suchlaufs kehrt bereits zurück, bevor der Hintergrundlauf
fertig ist), wurde `PlatformInventorySection` um ein **kurzes, sich selbst
begrenzendes** Nachfragen ergänzt: Solange für dieses Gerät irgendeine
Plattform als „läuft" gemeldet ist, wird der Status alle 4 Sekunden neu
geladen; sobald keine Plattform mehr läuft, stoppt dieses Nachfragen sich
selbst. Kein dauerhaftes Polling, keine neue Polling-Architektur - nur ein
begrenzter, bedingter Mechanismus für genau diesen Anwendungsfall.

## Gerätedetails

Es wurde geprüft, ob nach erfolgreicher Inventarisierung tatsächlich mehr
als Discovery-Grunddaten sichtbar sind: Ja - `AndroidInventorySection`/
`AndroidAppsSection` (40k34b/c/d) und `LinuxInventorySection`/
`LazyLinuxSection` (40k33b6a/b) zeigen bereits **alle** über
`recordDiscoveryHit()` gespeicherten „· Label: Wert"-Paare generisch an
(keine künstliche Beschränkung auf Discovery-Felder). Für Windows gilt seit
40k34q dasselbe: sowohl die lokale als auch die neue Remote-Inventarisierung
schreiben ihre Detailfelder (Edition, Version, Build, Architektur, BIOS,
CPU, RAM, Laufwerke, Netzwerkkarten, installierte Software, angemeldete
Benutzer) in dasselbe `discovery_protocol`-Textfeld - sie werden bereits
über die bestehende, generische Protokoll-Detailanzeige im
Geräteidentitäts-Dialog sichtbar. **Es musste hierfür keine neue
Anzeige-Komponente ergänzt werden.**

## Merge

Unverändert: `recordDiscoveryHit()` (seit 40k33b) übernimmt neue
Inventardaten weiterhin über dieselbe COALESCE-basierte Konsolidierung in
**dieselbe** Geräteidentität - keine Dubletten, keine zweite Identität. Die
automatische Inventarisierung nutzt exakt denselben Schreibpfad wie die
manuelle.

## Logging

Neue Log-Einträge (`[AUTO-INVENTORY]`-Präfix) für: Start (Plattform, Gerät,
Grund „unmittelbar nach Discovery als passend erkannt"), Überspringen
(bereits aktiv/Cooldown, jeweils mit Begründung), Abschluss (Ergebnis,
Dauer in ms), Fehlschlag. Die bereits bestehende
`[PLATFORM-INVENTORY]`-Protokollierung (40k34p, Benutzer/Gerät/Plattform/
Dauer/Ergebnis) bleibt unverändert und läuft für automatische Aufrufe
identisch mit, lediglich mit dem Aktor-Text „automatisch (nach Discovery)"
statt einem Principal-Namen.

## Performance

Die Zulässigkeitsprüfung selbst besteht aus zwei einfachen, indizierten
`COUNT(*)`-Abfragen (kein zusätzlicher Remote-Aufruf) - erst wenn beide
grün sind, erfolgt der tatsächliche, bereits bestehende Remote-Aufruf
(SSH/ADB/WinRM/lokale PowerShell). Läuft eine Plattform bereits oder greift
der Cooldown, wird **kein einziger** zusätzlicher Remote-Aufruf ausgelöst.

## Nicht Bestandteil / bewusst NICHT umgesetzt

- **Keine „Wartet"-Statusstufe** ergänzt: Da der automatische Trigger über
  virtuelle Threads praktisch verzögerungsfrei in den „Läuft"-Status
  übergeht, wurde eine eigene Zwischenstufe als nicht sinnvoll unterscheidbar
  eingestuft und bewusst weggelassen, statt eine kaum beobachtbare
  Zusatzstufe vorzutäuschen.
- **Kein separater „Discovery erneut starten"-Button** im Identitäts-Dialog
  ergänzt (siehe 40k34m: eine gezielte Einzelgerät-Re-Discovery existiert
  weiterhin nicht separat) - die allgemeine Netzwerksuche bleibt der Weg.
- Kein Cooldown-Wert konfigurierbar über die Oberfläche - aktuell fest auf
  30 Minuten im Code (`AUTO_INVENTORY_COOLDOWN_MINUTES`), keine neue
  Einstellungs-UI ergänzt.

## Bekannte Einschränkungen

- **Kein echter Build und kein Test mit echter Hardware/echten
  Suchläufen möglich** in dieser Sandbox (kein `javac`/`mvn`, keine echten
  Android-/Linux-/Windows-Zielgeräte erreichbar).
- Das Zusammenspiel „viele gleichzeitig neu gefundene Geräte lösen viele
  parallele automatische Inventarisierungen aus" wurde nicht gegen eine
  echte Umgebung mit vielen Geräten getestet - der virtuelle-Thread-Ansatz
  sollte dafür geeignet sein (sehr geringes Speicher-Overhead je Thread),
  wurde aber nicht unter Last verifiziert.
- Der Cooldown wird **je Plattform und Gerät**, nicht global begrenzt - bei
  sehr vielen Geräten könnten theoretisch viele automatische Läufe
  gleichzeitig anlaufen, wenn ein Suchlauf viele neue/passende Geräte auf
  einmal findet. Es wurde keine zusätzliche globale Drosselung ergänzt.

## Durchgeführte statische Prüfungen

**Es konnte in dieser Sandbox weiterhin kein echter Build durchgeführt
werden.** Stattdessen:

- Klammernbilanz aller fünf geänderten Backend-Dateien - ausgeglichen.
- Automatisierter Record-Konstruktor-Argumentzahl-Abgleich - keine
  Abweichungen.
- Manuelle Prüfung auf zirkuläre Bean-Abhängigkeiten: `DeviceIdentityService`
  referenziert `DeviceDiscoveryService` nur als reinen Typnamen in einem
  Javadoc-Kommentar (keine echte Spring-Abhängigkeit) - die neue
  Injektion von `DeviceIdentityService` in `DeviceDiscoveryService` ist
  damit unbedenklich.
- Bestätigt: kein manueller `new DeviceDiscoveryService(...)`-Aufruf im
  Projekt, der durch den geänderten Konstruktor brechen würde.
- Isolierter TypeScript-Transpile-Lauf über die vollständige `main.tsx` -
  0 Diagnosen.

```text
fachlich umgesetzt
statisch geprüft
nicht durch echten Build oder echte Suchlauf-/Hardwaretests verifiziert
```

Der reale Build- und Funktionstest erfolgt anschließend lokal bei
Sebastian - insbesondere sollte ein vollständiger Suchlauf mit mehreren
bereits registrierten Android-/Linux-/Windows-Geräten beobachtet werden,
um zu bestätigen, dass die automatische Inventarisierung tatsächlich
anläuft, den Suchlauf nicht verzögert, und die Statusanzeige sich nach
Abschluss von selbst aktualisiert.
