# Schritt 40k34v – Kompaktes, erweiterbares Aktionsmenü im Bereich „Registriert"

## Tatsächlich verwendeter Renderpfad (analysiert, nicht vermutet)

„Registriert" wird **innerhalb von `InventoryPage`** gerendert (nicht in
`DeviceManagerPanel` selbst, dessen eigene Geräteliste hier über
`hideDeviceList` ausgeblendet ist). Die tatsächliche Tabelle:
`registeredGroups.map(...) → <table className="compact-table">` mit den
Spalten `Name, Typ, Adresse, Seriennummer/MAC, Quelle, Treffer, Identität,
Registriert am, Aktionen`. Die „Aktionen"-Spalte enthielt bisher
**ausschließlich** zwei Buttons: „In Gerätebestand übernehmen" und
„Registrierung aufheben" - der Button „Identität" lag in einer
**separaten** Spalte. Keine Plattforminventarisierung war dort in
irgendeiner Form sichtbar - das bestätigt die Beobachtung aus dem Auftrag
exakt.

## Warum dort bisher keine neuen Aktionen sichtbar waren

40k34u hat `PlatformInventorySection` ausschließlich in
`DeviceIdentityDialog` ergänzt (erreichbar über den separaten
„Identität"-Button) - **nicht** direkt in der Aktionsspalte der
Registriert-Tabelle selbst. Das war keine falsche Komponente und keine
Bedingung, die etwas versteckt hätte, sondern schlicht: die Aktionsspalte
selbst wurde in 40k34u nicht angefasst.

## Umgesetzte Lösung: erweiterbare Menü-Registry statt Einzelbuttons

Statt einzelner, hart codierter Buttons wurde eine **Provider-Registry**
eingeführt (`REGISTERED_DEVICE_MENU_PROVIDERS`):

```ts
type DeviceMenuItem = {id, label, icon, group, onClick, disabled?, title?};
type DeviceMenuContext = {d, platform, busy, onOpenIdentity, runAll, onMoveToInventory, onRevoke};
type DeviceMenuProvider = (ctx: DeviceMenuContext) => DeviceMenuItem[];

const REGISTERED_DEVICE_MENU_PROVIDERS: DeviceMenuProvider[] = [
  generalDeviceMenuProvider,       // Details öffnen
  platformInventoryMenuProvider,   // Neu inventarisieren, Alle Plattformen, Details, Historie
  androidDeviceMenuProvider,       // nur sichtbar, wenn platform==='Android'
  windowsDeviceMenuProvider,       // nur sichtbar, wenn platform==='Windows'
  linuxDeviceMenuProvider,         // nur sichtbar, wenn platform==='Linux'
  inventoryDeviceMenuProvider,     // In Gerätebestand übernehmen, Registrierung aufheben
];
```

Ein künftiges Modul (Hardwareanalyse, Softwareinventar, Sicherheitsprüfung,
Compliance, Netzwerkanalyse, Virtualisierung, Smart Home, Reports)
ergänzt einfach einen weiteren Eintrag in dieser Liste - `RegisteredDeviceActionMenu`
selbst (Rendering, Öffnen/Schließen, Positionierung) bleibt unverändert.

## Antwort auf die acht Ergänzungspunkte

**1. Alle bestehenden Buttons vollständig übernommen, Aktionsspalte
vollständig ersetzt:** „Identität" → `open-identity`, „In Gerätebestand
übernehmen" → `move-to-inventory`, „Registrierung aufheben" → `revoke`.
Keine dieser drei Funktionen wurde entfernt oder verändert - nur in das
Menü verschoben. Die alte, separate „Identität"-Spalte enthält jetzt nur
noch die reinen Informationstexte (Quellenanzahl, Vertrauensbewertung),
der Button selbst ist im Menü.

**2. Dynamischer Aufbau je Plattform:** `clientPlatformOf(deviceType,
protocol)` wertet die ohnehin bereits geladenen Zeilendaten aus (keine
zusätzliche Abfrage) und blendet `androidDeviceMenuProvider`/
`windowsDeviceMenuProvider`/`linuxDeviceMenuProvider` nur ein, wenn die
jeweilige Plattform erkannt ist. **Ehrlich eingeschränkt umgesetzt:** von
den genannten Detaileinträgen (ADB, Apps, Akku / WinRM, Dienste,
Ereignisprotokoll / SSH, systemd, Pakete / ONVIF Snapshot-Stream-PTZ /
SNMP lesen) wurden nur die tatsächlich mit echten Daten hinterlegten
übernommen: **ADB-Verbindung**, **Installierte Apps** (Android),
**WinRM-/Systemdetails** (Windows), **SSH-/Systemdetails** (Linux) - alle
vier öffnen den bereits bestehenden Geräteidentitäts-Dialog mit den
jeweils passenden Abschnitten. **Akku, Dienste, Ereignisprotokoll,
systemd, Pakete, ONVIF (Snapshot/Stream/PTZ), SNMP-Lesen wurden bewusst
NICHT ergänzt** - für keinen dieser Punkte liefert ein bestehender
Inventarisierer aktuell echte Daten oder eine gezielt aufrufbare Funktion.
„Nicht verfügbare Aktionen möglichst ausblenden statt deaktivieren" wurde
damit durch **vollständiges Weglassen** umgesetzt - keine deaktivierten
Platzhalter-Einträge, keine erfundenen Funktionen.

**3. Intern erweiterbar:** siehe Registry oben - neue Module ergänzen
einen Provider, keine Änderung an `RegisteredDeviceActionMenu` nötig.

**4. Icons statt reinem Text:** Alle Menüeinträge tragen ein Icon. Die
allgemeinen/übergreifenden Einträge (Details, Neu inventarisieren, Alle
Plattformen, Inventarisierungsdetails, Historie, In Gerätebestand
übernehmen, Registrierung aufheben) nutzen echte Lucide-Icons (`Info`,
`RefreshCw`, `ListTodo`, `History`, `ArrowRightCircle`, `XCircle`) aus der
bereits im Projekt verwendeten Bibliothek `lucide-react` - **keine neue
UI-Bibliothek**. Die plattformspezifischen Einträge nutzen teils Emoji
(🖥️📦) als leichtgewichtige, sofort erkennbare Symbole, teils ebenfalls
`lucide-react` (`TerminalSquare` für ADB/SSH) - der Datentyp
`icon: React.ReactNode` erlaubt beides gleichberechtigt.

**5. Struktur für spätere Suchfunktion vorbereitet, nicht implementiert:**
Jeder `DeviceMenuItem` hat ein stabiles `id`, ein durchsuchbares `label`
und eine `group` - eine spätere Suchleiste im Menü könnte direkt über
`items.filter(i => i.label.toLowerCase().includes(query))` filtern, ohne
die Datenstruktur zu ändern. **Wie gefordert nicht implementiert.**

**6. Ausschließlich über identityKey:** Jede Aktion erhält als Kontext
`ctx.d` (das komplette `RegisteredDiscoveryDevice`-Objekt inkl.
`identityKey`) - `runAllKnownPlatforms(d.identityKey)`,
`moveRegisteredDeviceToInventory(d.identityKey)`,
`revokeRegisteredDevice(d.identityKey)`, `setIdentityDialogKey(d.identityKey)`
- nirgends wird über Zeilenindex, Position oder Anzeigename gehandelt.

**7./8. Langfristige Werkzeugmenü-Architektur statt Buttonsammlung:** Die
Registry-Struktur selbst *ist* die Antwort auf diesen Punkt - das Ziel war
nicht, in diesem Schritt alle denkbaren Aktionen zu ergänzen, sondern die
Architektur zu schaffen, in die jedes künftige technische Modul sich
einfügt. Das Menü in dieser Version enthält bewusst nur real
funktionierende Einträge - keine Scheinvielfalt.

## Menü-Darstellung und Bedienbarkeit

- Ein einzelner, schmaler Menübutton (Lucide-Icon `MoreVertical`) statt
  mehrerer Einzelbuttons - die Aktionen-Spaltenbreite bleibt konstant.
- **React-Portal** (`createPortal(..., document.body)`): das Dropdown wird
  außerhalb des überlaufenden `.scroll-table`-Containers gerendert -
  dadurch wird es **nicht** von der Tabelle abgeschnitten.
- Position wird beim Öffnen aus der tatsächlichen Position des Buttons
  berechnet (`getBoundingClientRect()`): öffnet nach unten, außer wenn der
  Platz darunter zu knapp ist (< 260px) und darüber mehr Platz ist - dann
  öffnet es nach oben. Horizontal rechtsbündig am Button, mit mindestens
  8px Rand zum Fensterrand - verhindert ein Herausragen bei schmalen
  Fenstern, ohne die Tabelle selbst zu verbreitern.
- `max-height: 70vh` mit `overflow-y: auto` - Scrollen bei vielen
  Einträgen statt Abschneiden.
- Schließt bei Klick außerhalb, bei Escape, sowie bei Scrollen/Resize (um
  eine dann falsch positionierte Fläche zu vermeiden).
- Schließt automatisch nach Auswahl einer Aktion.

## Verhalten während laufender Inventarisierung

`busy`-Zustand deaktiviert „Neu inventarisieren"/„Alle bekannten
Plattformen" während eines laufenden Aufrufs (verhindert Doppelklicks aus
dem Menü selbst). Der bereits bestehende, serverseitige Schutz
(`PlatformInventoryRepository.isRunning()`, seit 40k34r/s unverändert)
bleibt die eigentliche, verlässliche Absicherung gegen parallele Läufe -
die Menü-seitige Sperre ist nur eine zusätzliche, schnelle UI-Rückmeldung.

## Wiederverwendete Komponenten/APIs

`PlatformInventorySection` (im Geräteidentitäts-Dialog, unverändert),
`runAllKnownPlatforms()` (40k34p, unverändert), `moveRegisteredDeviceToInventory()`,
`revokeRegisteredDevice()`, `setIdentityDialogKey()` (alle bereits
bestehend). **Keine neue API, kein neuer Dispatcher, keine zweite
Statusverwaltung, keine zweite Historie, keine zweite Geräteidentität.**

## Bekannte Einschränkungen

- `clientPlatformOf()` ist eine **rein clientseitige, nur zur
  Menüsteuerung genutzte** Näherung (analog zum inzwischen korrigierten
  Backend-`platformOf()`, aber nicht direkt darauf zugreifend, da das
  Feld in der Listenantwort der registrierten Geräte nicht mitgeliefert
  wird). Sie entscheidet **nur**, welche Menügruppen sichtbar sind - die
  tatsächliche Plattformwahl für die Inventarisierung selbst bleibt
  vollständig serverseitig (`runPlatformInventory`/`runAllKnownPlatforms`).
  Eine künftige Vereinheitlichung (z.B. `platform` direkt in der Listen-
  API mitliefern) wäre eine sinnvolle, hier bewusst nicht vorgezogene
  Verbesserung.
- Mehrere der genannten Detaileinträge (Akku, Dienste, Ereignisprotokoll,
  systemd, Pakete, ONVIF, SNMP-Lesen) fehlen bewusst - siehe oben.
- Keine Tastatur-Pfeilnavigation *innerhalb* des geöffneten Menüs
  ergänzt (nur Escape zum Schließen) - die einzelnen Einträge sind aber
  normale, fokussierbare `<button>`-Elemente und damit per Tab
  erreichbar.

## Durchgeführte Prüfungen

**Kein echter Build, kein echter Browsertest möglich** in dieser Sandbox.
Stattdessen: isolierter TypeScript-Transpile-Lauf über die vollständige
`main.tsx` - 0 Diagnosen. Manuelle Prüfung: der neue Aufrufort
(`<RegisteredDeviceActionMenu d={d} .../>`) liegt nachweislich innerhalb
der tatsächlich verwendeten `registeredGroups`-Tabelle (per Zeilennummern-
Analyse bestätigt, nicht vermutet). Alle drei verbleibenden Vorkommen der
alten CSS-Klasse `device-inline-actions` im gesamten File wurden geprüft
und liegen nachweislich in anderen, nicht betroffenen Tabellen (Alias-
Liste, Merge-Kandidatenliste, Gerätebestand-„Übernehmen"-Aktion) - nicht
in der Registriert-Tabelle.

## Ehrliche Abschlussdokumentation

1. **Warum bisher keine neuen Aktionen sichtbar waren:** 40k34u hatte die
   Plattforminventarisierung nur im Geräteidentitäts-Dialog ergänzt, nicht
   in der Aktionsspalte der Registriert-Tabelle selbst.
2. **Tatsächlich geänderte Komponente:** `InventoryPage` (genauer: die
   darin gerenderte `registeredGroups`-Tabelle) sowie die neue Komponente
   `RegisteredDeviceActionMenu`.
3. **Nachweislich in der real verwendeten Tabelle?** Ja, per
   Zeilennummern-Analyse bestätigt - **nicht** nur vermutet.
4. **Übernommene bestehende Aktionen:** Details öffnen (vorher
   „Identität"), In Gerätebestand übernehmen, Registrierung aufheben -
   alle drei unverändert in ihrer Funktion, nur im Menü gebündelt.
5. **„Neu inventarisieren" für Android-Geräte sichtbar?** Ja, strukturell
   (Menüpunkt erscheint für jedes Gerät, unabhängig von der erkannten
   Plattform) - **nicht** durch einen echten Test mit den neun
   tatsächlichen Android-Geräten bestätigt.
6. **Ausschließlich `runPlatformInventory()`/`runAllKnownPlatforms()`
   verwendet?** Ja, keine zweite Inventarisierungslogik ergänzt.
7. **Historie aus dem Menü öffnen?** Ja - öffnet den Geräteidentitäts-
   Dialog, der den Identitätsverlauf enthält (kein direkter Sprung zu
   genau diesem Abschnitt implementiert - eine bekannte, kleine
   Einschränkung).
8. **Echter Frontend-Build durchgeführt?** Nein - kein `npm run
   build`/`vite build` in dieser Sandbox möglich, nur ein isolierter
   TypeScript-Syntax-Check.
9. **Im Browser getestet?** Nein.
10. **Nicht real getestet werden konnte:** jegliche echte Maus-/Tastatur-
    Interaktion mit dem Menü, die tatsächliche Positionierung/Nicht-
    Abschneiden in einem echten Browser-Layout, das Verhalten mit den neun
    tatsächlichen Android-Geräten, jegliches Zusammenspiel mit einem
    echten Build-Prozess.

Der reale Build- und Browsertest erfolgt anschließend lokal bei
Sebastian.
