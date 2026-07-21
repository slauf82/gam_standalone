# Schritt 40d36 – Personenanreden in PDF-Sprache

## Ziel
Die Personenanreden `Herr` und `Frau` werden in Rechnungsvorschau, PDF-Adressblock, Rechnungstext und Vorlesetext konsequent anhand der gewählten PDF-Sprache lokalisiert.

## Umsetzung
- Zentrale Lokalisierung in `InvoiceTextPreviewService` für Platzhalter `<Anrede>`.
- Erweiterte Lokalisierung in `InvoiceOpenHtmlPdfService` für alle unterstützten Sprachen.
- Der Empfängerblock der PDF nutzt ebenfalls die lokalisierte Anrede.
- Die React-Vorschau und ihr Vorlesetext lokalisieren die Anrede identisch.
- Unterstützte Sprachen: de, en, fr, uk, it, sv, tr, ru, es, pt, nl, pl, cs.

## Unverändert
- Titel wie Dr. oder Prof. werden nicht übersetzt.
- Oberflächensprache und PDF-Sprache bleiben getrennt.
- Zahlungsdokumente behalten ihre bereits vorhandene sprachabhängige persönliche Anrede.
