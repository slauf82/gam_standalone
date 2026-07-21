# Schritt 39d – Rechnungsadministration: State-Fix, Textsatz-Zuordnung, Logo-Upload

## Ausgangspunkt
Basis ist Schritt 39c.

## Behoben

### Wechsel zwischen Administrationsrubriken
- Beim Wechsel der Rubrik werden Tabelle, Katalog, Suchtext, Dialog und Formularzustand sofort zurückgesetzt.
- Dadurch bleiben keine sichtbaren Restzeilen mehr aus der vorherigen Rubrik stehen, während neue Daten geladen werden.

### Rechnungstexte als GAM-1.0-naher Textsatz
- Die bisher getrennten Bereiche Anreden, Rechnungstexte, rechtliche Hinweise und Grußformeln werden in der Rechnungsadministration als gemeinsamer Bereich „Rechnungstext-Sets“ gepflegt.
- Ein Textsatz enthält:
  - Anrede
  - Rechnungstext
  - rechtlichen Hinweis
  - Grußformel
  - eine gemeinsame Rechnungsgesellschaft-Zuordnung
- Die Zuordnung erfolgt über den Namen der Rechnungsgesellschaft; die ID bleibt intern.
- Backend ergänzt bei Bedarf automatisch die Spalte `RGESELLSCHAFTS_ID` in den vier Alt-GAM-Texttabellen.

### Logo-Upload
- Im Bereich Logos kann eine Bilddatei hochgeladen werden.
- Erlaubt sind PNG, JPG, JPEG, GIF und WEBP.
- Die Datei wird unter `/images/uploads/...` abgelegt.
- Der erzeugte Pfad wird direkt in das URL-Feld übernommen und kann sofort gespeichert werden.
- Tabellen- und Formularansicht zeigen eine Logo-Vorschau.

## Technische Hinweise
- Neuer Backend-Endpunkt: `/api/gam/admin/masterdata/invoice-text-sets`
- Neuer Backend-Endpunkt: `/api/gam/admin/masterdata/invoice-logos/upload`
- Neue Frontend-Clientfunktionen:
  - `loadInvoiceTextSets`
  - `createInvoiceTextSet`
  - `updateInvoiceTextSet`
  - `uploadInvoiceLogo`

## Build-Hinweis
Im Container konnte kein Build ausgeführt werden, weil Maven lokal nicht vorhanden war und der Wrapper Maven wegen fehlender Internetauflösung nicht nachladen konnte. Änderungen wurden gezielt und strukturkompatibel vorgenommen.
