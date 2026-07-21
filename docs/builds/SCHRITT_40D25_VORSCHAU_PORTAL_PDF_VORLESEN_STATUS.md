# Schritt 40d25 – Bestandsrechnungsvorschau und PDF-Vorlesen im Portal

## Vorschau bestehender Rechnungen
- `/api/invoices/text-preview` akzeptiert nun `invoiceNumber`.
- Bei vorhandener Rechnungsnummer werden Empfänger und Behandlungsdatum über `InvoiceDocumentDataService` aus `ADRESSID`, `KINDADRESSID`, `FIRMAADRESSID` und `BDATUM` geladen.
- Eine aktuell geladene LBD wird bei bestehenden Rechnungen nicht verwendet.
- Neue, noch nicht gespeicherte Rechnungen verwenden weiterhin die LBD als Erfassungsquelle.
- Die Anrede wird persönlich aus Anrede, Titel und Namen gebildet; Konstruktionen wie `Sehr geehrte(r) Herr ...` entfallen.

## Patientenportal
- Der Button „Rechnung vorlesen“ lädt den vollständigen Text der tatsächlich erzeugten PDF.
- Dadurch werden Empfänger, Rechnungsdaten, sämtliche Produktpositionen, Mengen, Preise, Summen, Hinweise und Zahlungsinformationen vorgelesen.
- Zahlungsdokumente werden auf demselben Weg vollständig aus ihrer PDF vorgelesen.
- Der Vorlesebutton am Seitenende liest weiterhin ausschließlich die sichtbare Portalseite.
- Die Portalberechtigungen werden auch für die neuen Text-Endpunkte geprüft.

## Technische Umsetzung
- PDF-Textausgabe mit Apache PDFBox `PDFTextStripper`.
- Keine OCR; die vorhandene barrierefreie Textstruktur der PDF wird verwendet.
