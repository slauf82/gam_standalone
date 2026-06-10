# Schritt 28g – Vorschau/PDF-Textgleichheit

Ziel: Die rechte Rechnungsvorschau zeigt alle Textbestandteile, die auch in der PDF erscheinen.

## Enthalten

- Vorschau verwendet die vom Backend gelieferten Translation-Labels aus `translation_*`.
- Ergänzte Vorschauzeilen für:
  - Kundendatei / LBD-Datei
  - Benutzer
  - Zahlungsart
  - ZUGFeRD-/Factur-X-Hinweis
  - Netto / MwSt / Gesamt
  - Bankdaten
  - Steuernummer / USt-ID
- Tabellenüberschriften in der Vorschau werden aus denselben Translation-Keys wie die PDF abgeleitet:
  - `invoiceAmount`
  - `invoiceProductCode`
  - `invoiceDescription`
  - `invoiceTaxRate`
  - `invoiceSinglePrice`
  - `invoiceTotalPrice`
- Rabatt/Gutschein/Ratenzahlung nutzen ebenfalls die Translation-Labels aus dem Backend.
- Vorlesetext enthält nun ebenfalls Kundendatei, Benutzer, Zahlungsart, Summen und Bank-/Steuerdaten.

## Technische Hinweise

- Der Backend-Endpunkt `/api/invoices/text-preview` liefert jetzt zusätzlich ein `labels`-Objekt.
- `InvoiceTextPreview` wurde um `documentTitle` und `labels` erweitert.
- Die Vorschau bleibt damit näher am PDFRenderer und vermeidet voneinander abweichende Frontend-/PDF-Texte.

## Noch offen

- Externe PDF/UA-Validierung.
- Vollständige Vereinheitlichung in eine gemeinsame `InvoiceDocument`-Datenstruktur für PDF und Vorschau.
