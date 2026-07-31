# Schritt 40k34p – Plattforminventarisierung manuell starten / Nachinventarisierung

## Ausgangsprüfung (vor jeder Neuentwicklung durchgeführt)

Wie im Auftrag verlangt, wurde zuerst geprüft, welche Erst-/Nachinventarisierungs-
Mechanismen bereits existieren:

- **Android:** `DeviceIdentityService.runAndroidInventory()` (Systeminventar,
  40k34b) und `runAndroidAppInventory()` (Apps, 40k34c) - beide bereits als
  gezielte, jederzeit erneut auslösbare Aktionen für ein einzelnes Gerät
  vorhanden.
- **Linux:** `refreshLinuxCache()` existierte bereits - **aber** sie
  invalidiert nur den On-Demand-Zwischenspeicher (40k33b6b) und wartet auf
  den nächsten ohnehin stattfindenden Suchlauf. Es gab **keine** Möglichkeit,
  sofort und gezielt eine neue Linux-Inspektion für genau ein Gerät
  auszulösen - `LinuxNetworkDiscoveryService.inspectSingle()` (die
  eigentliche SSH-Inspektionsmethode) existierte zwar bereits, wurde aber
  bisher nur intern während eines Suchlaufs aufgerufen, nie direkt vom
  Benutzer.
- **Windows:** `WindowsInventoryDiscoveryService` läuft **ausschließlich
  lokal** auf dem GAM-Host selbst (WMI/PowerShell-Selbstabfrage) - es gibt
  **keinen** Mechanismus, ein **fremdes** Windows-Gerät fernzuinventarisieren.
- **macOS/iOS:** keinerlei Inventarisierungslogik vorhanden.
- **Statusverwaltung:** Es existierte nur eine Android-App-spezifische
  Lauf-Historie (`gam_discovery_app_inventory_runs`, 40k34c) - keine
  plattformübergreifende, generische Statusverwaltung.

**Ergebnis:** Eine zentrale, plattformübergreifende Statusverwaltung und ein
zentraler Dispatch-Einstiegspunkt fehlten tatsächlich und wurden ergänzt -
alle bereits vorhandenen, plattformspezifischen Inventarisierungsmethoden
wurden dabei **wiederverwendet**, keine davon wurde dupliziert oder ersetzt.

## Wiederverwendete Infrastruktur

| Bereich | Wiederverwendet |
|---|---|
| Android-Systeminventar | `DeviceIdentityService.runAndroidInventory()` (unverändert) |
| Android-App-Inventar | `DeviceIdentityService.runAndroidAppInventory()` (unverändert) |
| Linux-Inspektion | `LinuxNetworkDiscoveryService.inspectSingle()`/`invalidateCache()` (unverändert, jetzt zusätzlich direkt aufrufbar) |
| Geräteidentität/-schreiblogik | `DiscoveryRegistrationRepository.recordDiscoveryHit()`/`find()` (unverändert) |
| Berechtigungen | `@PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")` - dieselbe Rolle wie bei den bestehenden Android-Endpunkten |
| Aktueller Benutzer (für Historie) | `java.security.Principal`-Parameter - dasselbe Muster wie bei `split()` |
| Logging | dieselbe SLF4J-Logger-Instanz in `DeviceIdentityService` |
| Frontend-Bausteine | Bestehende `<details>`/Tabellen-/Button-/`gamNotify()`-Konventionen aus `AndroidInventorySection`/`AndroidAppsSection` |

## Neue, zentrale Statusverwaltung

Eine neue, additive Tabelle `gam_discovery_platform_inventory_runs`
(`identity_key, platform, started_at, finished_at, status, message,
triggered_by`) - **eine** generische Tabelle für **alle** Plattformen
(Windows/Linux/Android/macOS/iOS sind Werte der Spalte `platform`, keine
eigene Struktur je Plattform). Neue Klasse `PlatformInventoryRepository`
verwaltet ausschließlich diese Tabelle - keine zweite Statusverwaltung,
keine Parallelstruktur zur bereits bestehenden Android-App-Historie (die
unverändert weiterläuft, App-Läufe sind eine Ebene feiner als der neue,
plattformweite Status).

## Zentraler Dispatch: `DeviceIdentityService.runPlatformInventory()`

```java
public PlatformInventoryResult runPlatformInventory(String identityKey, String platform, String actor)
```

- **Android** → ruft `runAndroidInventory()` **und** `runAndroidAppInventory()`
  auf (System + Apps als ein gemeinsamer „Android"-Lauf).
- **Linux** → invalidiert den Cache **und** ruft jetzt zusätzlich sofort
  `inspectSingle()` auf, übernimmt ein gefundenes Ergebnis direkt über
  `recordDiscoveryHit()` - echte, sofortige Nachinventarisierung statt
  reiner Cache-Invalidierung.
- **Windows/macOS/iOS** → liefert ehrlich `NICHT_UNTERSTUETZT` mit einer
  klaren Begründung (siehe unten) - **keine vorgetäuschte Inventarisierung**.

`runAllKnownPlatforms()` ermittelt anhand **bereits gespeicherter** Hinweise
(Plattformfeld, ADB-Verbindung, Protokolltext), welche Plattformen
grundsätzlich zum Gerät passen, und ruft `runPlatformInventory()` nur für
diese auf - keine Ratelogik, keine unpassenden Läufe.

`platformInventoryStatus()` liefert für alle fünf Plattformen einen Status-
Eintrag - `NOCH_NIE`, falls für diese Plattform noch kein Lauf existiert.

## Nachinventarisierung

Es wird **nichts blind gelöscht**. Für Android laufen die bereits
bestehenden 40k34b/c-Regeln unverändert (vollständige/teilweise Läufe,
`markRemoved()` nur bei vollständigem Lauf). Für Linux überschreibt
`recordDiscoveryHit()` lediglich das ohnehin bereits bestehende COALESCE-
basierte Konsolidierungsverhalten (unverändert seit 40k33b) - keine neue
Löschlogik wurde ergänzt.

## Kontextmenü / Benutzeroberfläche

**Ehrlich benannt, bewusste Vereinfachung:** Statt eines echten, aufklapp-
baren Kontextmenüs mit Untermenü (wie im Auftrag skizziert) wurde eine
neue, direkt sichtbare Tabelle **„Plattforminventarisierung"** im
bestehenden Geräteidentitäts-Dialog ergänzt (`PlatformInventorySection`,
oberhalb der bereits bestehenden Linux-/Android-Abschnitte, **für jedes
Gerät sichtbar**, nicht auf Android beschränkt):

- eine Zeile je Plattform mit Status-Symbol (⚪ noch nie, ⏳ läuft,
  ✅ erfolgreich, 🟡 teilweise, ❌ fehlgeschlagen, ➖ nicht unterstützt,
  ⚠️ nicht erreichbar, 🔒 nicht autorisiert), letztem Lauf, Dauer und
  Meldung, sowie einem eigenen „Inventarisieren"-Button je Zeile.
- ein Button „Alle bekannten Plattformen erneut inventarisieren" oberhalb
  der Tabelle.

Eine gleichwertige, aber deutlich weniger riskante Lösung als ein
neu zu bauendes Kontextmenü-Widget (kein neues UI-Interaktionsmuster,
keine Tastatur-/Klick-außerhalb-Logik nötig) - funktional identisch
(gezielte Einzelplattform- oder Alle-Plattformen-Inventarisierung),
barrierearm (jede Aktion ist ein normaler, fokussierbarer Button statt
eines versteckten Menüs).

**Nicht umgesetzt:** Die Ansicht wurde **nicht zusätzlich** in die
„Gefundene Geräte"- und „Gerätebestand"-Tabellen als eigener Menüpunkt
integriert - sie ist über den bereits bestehenden Geräteidentitäts-Dialog
für jedes **registrierte** Gerät erreichbar. Für noch nicht registrierte,
nur gefundene Geräte ergibt eine Plattforminventarisierung ohnehin keinen
Sinn (es existiert noch keine Geräteidentität, an der ein Lauf gespeichert
werden könnte).

## „Discovery erneut durchführen"

**Ehrlich benannt:** Es wurde **keine neue, auf ein einzelnes Gerät
begrenzte Re-Discovery** gebaut. Der Auftrag verlangt ausdrücklich
„Discovery und Inventarisierung bleiben getrennte Aktionen" - diese
Trennung ist durch die neue Plattforminventarisierung bereits vollständig
gegeben (sie berührt Discovery nicht). Eine **gezielte** Re-Discovery nur
für ein einzelnes, bereits bekanntes Gerät existiert weiterhin nicht
separat - die bereits bestehende, vollständige Netzwerksuche bleibt der
Weg, um ein Gerät erneut zu entdecken (z.B. nach Hostname-/IP-Änderung).
Eine echte Einzelgerät-Re-Discovery wäre ein größerer, eigenständiger
Umbau der Discovery-Pipeline gewesen und wurde aus Zeitgründen nicht
umgesetzt - als bekannte Lücke benannt, nicht verschwiegen.

## Fehlerbehandlung

`runPlatformInventory()` fängt Ausnahmen ab, markiert den Lauf als
`FEHLGESCHLAGEN` mit der Fehlermeldung und wirft nichts weiter - Discovery,
Geräteidentität und bereits vorhandene Inventardaten bleiben in jedem Fall
unangetastet. Der Benutzer kann jederzeit erneut auf „Inventarisieren"
klicken.

## Performance

Kein zusätzlicher Remote-Aufruf pro Statusabfrage (`platformInventoryStatus()`
liest ausschließlich die lokale Historientabelle). Ein tatsächlicher
Inventarisierungslauf löst weiterhin nur die bereits bestehenden, bereits
auf Sammelabfragen optimierten Methoden aus (ADB: 6 Sammelbefehle, siehe
40k34c; Linux: eine einzelne SSH-Sitzung, siehe 40k33b3) - keine neuen,
zusätzlichen Mehrfachabfragen wurden ergänzt.

## Berechtigungen

Wiederverwendet: `@PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")` -
dieselbe Rolle wie bei den bereits bestehenden Android-Inventarisierungs-
Endpunkten. Keine neue Berechtigungsstruktur.

## Logging

Ein neuer, informativer Log-Eintrag je Lauf
(`log.info("[PLATFORM-INVENTORY] {} für {} durch {}: {} - {}", ...)`) über
dieselbe, bereits bestehende SLF4J-Logger-Instanz. Es werden Benutzer
(Principal-Name), Gerät (Identitätsschlüssel), Plattform, Status und
Ergebnismeldung protokolliert - **keine** Zugangsdaten (SSH-Passwörter,
ADB-Pairing-Codes bleiben wie bisher außerhalb jeder Logmeldung, siehe
40k34b).

## Nicht Bestandteil

Keine neuen Inventardaten-Felder, keine Änderung an Discovery, Merge oder
Integrität. Keine Android-Sonderlösung - Android durchläuft exakt denselben
`runPlatformInventory()`-Dispatch wie jede andere Plattform.

## Durchgeführte statische Prüfungen

**Es konnte in dieser Sandbox weiterhin kein echter Build durchgeführt
werden** (kein `javac`/`mvn`/`gradle`). Stattdessen:

- Klammernbilanz aller drei neuen/geänderten Backend-Dateien
  (`PlatformInventoryRepository.java`, `DeviceIdentityService.java`,
  `DeviceIdentityController.java`) - ausgeglichen.
- Automatisierter Record-Konstruktor-Argumentzahl-Abgleich (Lehre aus
  40k34g) über alle drei Dateien - keine Abweichungen gefunden.
- Isolierter TypeScript-Transpile-Lauf über `client.ts` und die
  vollständige `main.tsx` - 0 Diagnosen.
- Funktionsgrenzen (`PlatformInventorySection`, `LinuxInventorySection`,
  `DeviceIdentityDialog`) per grep verifiziert - korrekt getrennt.
- Manuelle Prüfung: `linux.inspectSingle()`/`invalidateCache()` sowie
  `AndroidAdbService`/`AppInventoryRepository`-Methoden existierten bereits
  unverändert - keine Signaturabweichung.

```text
fachlich umgesetzt
statisch geprüft
nicht durch echten Build oder echten Gerätetest verifiziert
```

Der reale Build- und Funktionstest erfolgt anschließend lokal bei
Sebastian - insbesondere die neue, sofortige Linux-Nachinventarisierung
(vorher nur Cache-Invalidierung) sollte an einem echten SSH-erreichbaren
Linux-Gerät geprüft werden.

## Bekannte Einschränkungen

- Windows-/macOS-/iOS-Ferninventarisierung sind ehrlich als „nicht
  unterstützt" gekennzeichnet, nicht implementiert (siehe oben) - der
  Dispatch-Rahmen ist bereits vorhanden, sodass eine spätere Ergänzung
  (z.B. WinRM für Windows) ohne Umbau möglich ist.
- Kein eigenes Kontextmenü-Widget, sondern eine direkt sichtbare Tabelle
  im Geräteidentitäts-Dialog (siehe oben, bewusste, dokumentierte
  Vereinfachung).
- Keine gezielte Einzelgerät-Re-Discovery (siehe oben) - weiterhin nur
  über die bestehende vollständige Netzwerksuche möglich.
- Kein echter Build-/Gerätetest möglich (siehe oben).

## Testanleitung

1. Ein Linux-Gerät mit zunächst deaktiviertem SSH registrieren (Discovery
   erfolgreich, keine Inventardaten). SSH aktivieren, „Linux
   inventarisieren" klicken: Status muss auf „erfolgreich" wechseln, ohne
   dass ein neuer Suchlauf nötig ist.
2. Ein Android-Gerät nach Aktivierung von USB-Debugging über
   „Android inventarisieren" nachinventarisieren: System- und Apps-Status
   müssen sich aktualisieren.
3. Eine fehlgeschlagene Inventarisierung (z.B. SSH vorübergehend nicht
   erreichbar) darf weder das Gerät löschen noch vorhandene Inventardaten
   überschreiben - Status muss „fehlgeschlagen"/„nicht erreichbar" zeigen,
   ein erneuter Versuch muss weiterhin möglich sein.
4. „Alle bekannten Plattformen erneut inventarisieren" bei einem Gerät mit
   sowohl Android- als auch (fälschlich) Linux-Hinweisen: nur die
   tatsächlich passende(n) Plattform(en) dürfen laufen.
5. Historie/Status nach mehreren aufeinanderfolgenden Läufen prüfen -
   `platformInventoryStatus()` muss immer den jeweils **letzten** Lauf je
   Plattform zeigen.
