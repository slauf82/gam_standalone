# Schritt 38g3 – Dialog-Fokus-Fix

## Ziel

Der automatische Fokus in `GamDialog` darf nur beim Öffnen des Dialogs gesetzt werden. Während der Bearbeitung darf der Fokus nicht nach jedem Tastendruck auf das erste Eingabefeld zurückspringen.

## Umsetzung

- `GamDialog` fokussiert das erste Feld nur noch einmal pro Dialogöffnung.
- `onClose` wird intern per Ref aktuell gehalten, damit der Escape-Handler nicht bei jedem Render neu aufgebaut werden muss.
- Änderungen in Formularfeldern lösen keinen erneuten Autofokus mehr aus.
- Gilt zentral für Aufgaben, Freigaben, Bestelltool, Moduladministration und alle weiteren GDS-Dialoge.

## Ergebnis

Nach dem Öffnen steht der Cursor weiterhin sinnvoll im ersten Feld. Wenn der Benutzer danach ein anderes Feld anklickt, bleibt der Fokus dort und springt nicht mehr zurück.

## Build

Frontend-Build erfolgreich getestet.
