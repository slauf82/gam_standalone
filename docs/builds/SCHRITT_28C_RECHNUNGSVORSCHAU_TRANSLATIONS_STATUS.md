# Schritt 28c – Rechnungsvorschau & Translation-Logik

Diese Version baut auf Schritt 28b auf und ergänzt den alten GAM-Aufbau mit linkem Einstellungsbereich und rechter Rechnungsvorschau.

## Enthalten

- Rechte Rechnungsvorschau im Rechnungseditor
- Vorschau zeigt:
  - Rechnungsanrede
  - Rechnungstext
  - Positionsbereich
  - rechtlichen Hinweis
  - Grußformel
- Nutzung der bestehenden Translation-Tabellen:
  - translation_german
  - translation_english
  - translation_french
  - translation_ukrainian
- Verwendung der Alt-GAM-Schlüssel:
  - invoiceSalutationLabel0
  - invoiceInvoiceTextLabel0
  - invoiceLawHintLabel0
  - invoiceGreetingsLabel0
- Platzhalterersetzung:
  - <Anrede>
  - <Titel>
  - <Vorname>
  - <Namenszusatz>
  - <Nachname>
  - <Behandlungsdatum>
  - <Gesellschaftsname>
- Neuer Backend-Endpunkt:
  - GET /api/invoices/text-preview

## Ziel

Die Mehrsprachigkeit soll nicht nur im PDF wirken, sondern im Rechnungseditor sichtbar nachvollziehbar sein, ähnlich wie im alten PrimeFaces-Aufbau.

## Noch offen

- Exakte Gesellschafts-/Indexlogik aus dem alten RechnungController vollständig rekonstruieren
- Weitere Textindexe außer Label0 anbinden
- LibreTranslate-Fallback aktivieren und speichern
- Alle Oberflächentexte vollständig übersetzen
