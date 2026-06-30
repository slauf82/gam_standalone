# Schritt 38k4 – Login-Zielmodul Final-Fix

## Ziel
Der direkte Login in Patientenverwaltung und Terminverwaltung fiel trotz Auswahl weiterhin ins Rechnungsprogramm zurück.

## Ursache
Die Zielmodul-Auswahl war zwar gespeichert, konnte aber beim Wechsel vom Login-Dialog in den authentifizierten App-Zustand durch den zentralen Startseiten-/Fallback-Mechanismus übersteuert werden.

## Umsetzung
- Login-Ziel zusätzlich in `sessionStorage` gesichert
- Ziel wird beim App-Wechsel direkt konsumiert
- `Shell` wird mit Startseiten-Key neu montiert
- Patientenverwaltung und Terminverwaltung haben Vorrang vor dem Rechnungsprogramm-Fallback
- Ziel wird nach erfolgreicher Anwendung aus `sessionStorage` entfernt

## Erwartetes Verhalten
- Login-Ziel „Patientenverwaltung“ öffnet direkt die Patientenverwaltung
- Login-Ziel „Terminverwaltung“ öffnet direkt die Terminverwaltung
- Rechnungsprogramm wird nur noch geöffnet, wenn es ausdrücklich ausgewählt wurde oder kein anderes gültiges Ziel vorhanden ist
