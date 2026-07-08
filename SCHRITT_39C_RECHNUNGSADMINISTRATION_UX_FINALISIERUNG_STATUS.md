# GAM 2.0 – Schritt 39c – Rechnungsadministration UX-Finalisierung

## Ziel

Schritt 39c finalisiert die Bedienbarkeit der Rechnungsadministration aus 39b2.
Gesellschaften und Filialen werden für Anwender nicht mehr als technische IDs geführt,
sondern über sprechende Namen ausgewählt. Die IDs bleiben intern erhalten und werden nur
im Hintergrund gespeichert.

## Änderungen

### Gesellschaftsauswahl

- Felder wie `rgesellschafts_id`, `RGESELLSCHAFTS_ID` und `GESELLSCHAFT_ID` werden in der Rechnungsadministration als Auswahlfeld angezeigt.
- Der Anwender sieht den Namen der Rechnungsgesellschaft.
- Gespeichert wird weiterhin die bestehende technische ID.
- Datenmodell und Migration bleiben kompatibel.

### Filialauswahl

- Felder wie `filiale_id` und `FILIALE_ID` werden als Auswahlfeld angezeigt.
- Der Anwender sieht den Filialnamen, optional mit Kürzel und Ort.
- Gespeichert wird weiterhin die bestehende technische ID.

### Abhängige Auswahl

- Wird eine Gesellschaft gewählt, wird die Filialauswahl soweit möglich auf passende Filialen eingeschränkt.
- Passt die aktuell gewählte Filiale nicht mehr zur neuen Gesellschaft, wird sie zurückgesetzt.
- Falls keine eindeutige Zuordnung vorhanden ist, bleiben alle Filialen auswählbar, damit Alt-GAM-Daten nicht blockiert werden.

### Tabellenanzeige

- In den Tabellen der Rechnungsadministration werden Gesellschafts- und Filialfelder sprechend angezeigt.
- IDs werden nur noch als Fallback angezeigt, falls kein Name aufgelöst werden kann.

### Suche

- In Produkt- und Gesellschaft/Filiale-Zuordnungstabellen kann auch nach sichtbaren Gesellschafts- und Filialnamen gesucht werden.

## Geänderte Dateien

- `frontend/src/main.tsx`

## Build-Hinweis

Frontend-Typprüfung wurde mit lokal verfügbarem TypeScript geprüft. Der reguläre Projektbuild konnte in dieser Umgebung nicht vollständig laufen, weil Maven beziehungsweise Node-Dependencies nicht aus dem Internet nachgeladen werden konnten.
