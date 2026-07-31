# Schritt 40k34o – Drag & Drop zur Kategorie-Umsortierung

## Ziel

Wunsch: Geräte per Ziehen (Drag & Drop) direkt in eine andere Kategorie-
Gruppe verschieben können, statt jedes Mal über das „Typ"-Dropdown eine
Kategorie auswählen zu müssen - für schnellere, angenehmere Bedienung bei
mehreren zu korrigierenden Geräten.

## Umsetzung

- Jede Geräte-Zeile in der Ansicht „Registrierte Geräte" ist jetzt
  `draggable` (native HTML5-Drag-and-Drop-API, keine zusätzliche
  Bibliothek).
- Jede Kategorie-Gruppe (`<details className="device-category">`) ist ein
  Drop-Ziel: beim Ziehen über eine Gruppe wird diese optisch hervorgehoben
  (blauer Rahmen/Hintergrund), beim Loslassen wird das gezogene Gerät in
  diese Kategorie verschoben.
- **Das bestehende Dropdown bleibt vollständig unverändert erhalten** -
  aus zwei Gründen bewusst nicht ersetzt:
  1. **Barrierefreiheit**: Drag & Drop ist ohne Maus (Tastatur,
     Screenreader) nicht bedienbar. Das Dropdown bleibt der zugängliche
     Weg für alle, die keine Maus/kein Touch-Drag nutzen können oder
     wollen.
  2. **Neue Kategorie anlegen**: Diese Funktion („➕ Neue Gerätekategorie …")
     erfordert eine Texteingabe und lässt sich per Drag & Drop naturgemäß
     nicht abbilden.
- Ein kurzer Hinweistext oberhalb der Kategorie-Liste macht die neue
  Möglichkeit auffindbar, ohne aufdringlich zu sein.

## Wiederverwendung des bestehenden, zentralen Update-Pfads

Es wurde **keine zweite Update-Logik** für Drag & Drop gebaut. Die
bisherige `changeRegisteredDeviceType()`-Funktion wurde in zwei Teile
aufgeteilt:

- **`applyDeviceTypeChange(d, next)`** (neu) - die eigentliche, gemeinsame
  Kernlogik: Bestätigungsdialog, API-Aufruf, Übernahme über den bereits
  bestehenden zentralen Update-Pfad `applyRegisteredDeviceUpdate()` (aus
  40k34k), Erfolgs-/Fehlermeldung. Funktioniert unabhängig von einem
  `<select>`-Element.
- **`changeRegisteredDeviceType(d, selectedValue, select)`** (bestehend,
  nur umgebaut) - behält die dropdown-spezifische Sonderbehandlung
  („Neue Kategorie anlegen"-Dialog, Zurücksetzen des `<select>`-Werts bei
  Abbruch/Fehler) und ruft für die eigentliche Änderung jetzt
  `applyDeviceTypeChange()` auf.
- Der neue Drag-&-Drop-`onDrop`-Handler ruft **dieselbe**
  `applyDeviceTypeChange()`-Funktion auf.

Dadurch laufen Dropdown-Auswahl und Drag & Drop durch **exakt denselben**
Bestätigungsdialog, denselben API-Aufruf, dieselbe Erfolgsmeldung und
denselben zentralen State-Update-Pfad - keine Duplikation, kein
abweichendes Verhalten zwischen den beiden Bedienwegen.

## Verhalten

- Ziehen eines Geräts auf seine **eigene, aktuelle** Kategorie: kein
  Bestätigungsdialog, keine Änderung (still ignoriert, wie bei einer
  No-Op-Auswahl im Dropdown).
- Ziehen auf eine **andere** Kategorie: derselbe Bestätigungsdialog wie
  bisher beim Dropdown („Gerät „X" wirklich von „Y" nach „Z" verschieben?").
- Manuelle Zuordnungen (`manualDeviceType`) werden dabei genauso behandelt
  wie bei einer Dropdown-Änderung - keine Änderung an der
  Schutzlogik selbst.
- Mehrere Geräte gleichzeitig ziehen ist mit nativem HTML5-Drag-and-Drop
  nicht vorgesehen - ein Gerät pro Zieh-Vorgang, wie im Dropdown auch.

## Nicht Bestandteil

Keine Änderung an Backend, Evidence-Erkennung, Discovery, Merge oder
Datenbank. Kein neues Polling, kein `setTimeout`. Die Korrekturen aus
40k34k/k1/l/m/n bleiben unverändert.

## Durchgeführte statische Prüfungen

**Es konnte in dieser Sandbox weiterhin kein echter Build und kein echter
Browser-Test durchgeführt werden** (keine Maus-/Touch-Interaktion in dieser
Sandbox simulierbar). Stattdessen:

- Isolierter TypeScript-Transpile-Lauf über die vollständige `main.tsx` -
  0 Diagnosen.
- Funktionsgrenzen (`DeviceManagerPanel`, `InventoryPage`) erneut per grep
  verifiziert - unverändert korrekt getrennt.
- Kreuzabgleich: `applyDeviceTypeChange()` wird an genau den zwei
  vorgesehenen Stellen aufgerufen (Dropdown-Änderung intern,
  Drag-&-Drop-`onDrop`-Handler) - keine Duplikation der Kernlogik.
- Manuelle Durchsicht der HTML5-Drag-and-Drop-API-Verwendung
  (`draggable`, `onDragStart`/`onDragEnd`/`onDragOver`/`onDragLeave`/
  `onDrop`, `dataTransfer.setData`/`getData`) auf Vollständigkeit -
  sowohl `dataTransfer`-Daten als auch der React-State
  (`draggedIdentityKey`) tragen die Identität, damit die Übertragung auch
  bei einem im Einzelfall nicht unterstützten `dataTransfer`-Zugriff
  funktioniert.

```text
fachlich umgesetzt
statisch geprüft
nicht durch echten Build oder echte Browser-Interaktion verifiziert
```

Der reale Build- und Bedientest (insbesondere das tatsächliche Ziehen mit
Maus/Touch) erfolgt anschließend lokal bei Sebastian.

## Bekannte Grenzen

- Kein Mehrfachauswahl-Drag (jeweils nur ein Gerät pro Ziehvorgang).
- Auf Touch-Geräten funktioniert natives HTML5-Drag-and-Drop je nach
  Browser eingeschränkt oder gar nicht - das Dropdown bleibt dort die
  zuverlässige Alternative.
- Kein visuelles „Ziehbild" (Drag-Ghost) individuell gestaltet - es wird
  das browserseitige Standardverhalten verwendet.
