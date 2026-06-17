# Schritt 34z7d – treatmentDate, Keykatalog und Vorlesen-Button

## Ziel
- `treatmentDate` darf in der Vorschau nicht mehr als technischer Key sichtbar werden.
- Der große Vorlesen-Button in der Rechnungsvorschau soll optisch zentriert und etwas kräftiger wirken.
- Neue feste UI-/Rechnungs-/Portal-Keys sollen nicht durch eine alte Pro-Sprache-Sperre in der Browser-Session hängen bleiben.

## Änderungen
- `TranslationService` ergänzt `treatmentDate` und `dueDate` auch im deutschen Rechnungslabel-Katalog.
- EN/FR/UK Rechnungslabel-Katalog ebenfalls um `treatmentDate`/`dueDate` ergänzt.
- `InvoiceTextPreviewService.labels()` liefert `treatmentDate`, `serviceDate`, `dueDate` mit aus.
- Frontend-Fallback für Rechnungsvorschau nutzt jetzt zusätzlich `INVOICE_METADATA_LABELS`, bevor ein technischer Key angezeigt werden kann.
- UI-Translation-Request-Sperre arbeitet nun pro Sprache + Keykatalog-Fingerprint, nicht nur pro Sprache.
- Großer Vorlesen-/Stop-Button in `.invoice-preview-settings` zentriert und größer.

## Nicht geändert
- Produktbeschreibungen bleiben nur Live-Übersetzung und werden nicht in die DB geschrieben.
- Keine Mehrsprachen-Massenpflege; weiterhin nur aktuelle UI-Sprache.
- Keine fachliche Änderung an Rechnung/PDF/ZUGFeRD.

## Build-Hinweis
- Backend-Maven konnte in dieser Umgebung nicht geprüft werden (`mvn` nicht vorhanden).
- Frontend-Build konnte in dieser Umgebung nicht geprüft werden (`node_modules` nicht vorhanden).
