# Schritt 16 – Rechnungsarten mit echter GAM-Tabellenlogik

Diese Version korrigiert die Belegarten nach den bestätigten GAM-Fachregeln.

## Fachregeln

### Normale Rechnung

- Kopf: `rechnungsdetails`
- Positionen: `rechnung`
- Nummernkreis: aus `rechnungsdetails.RNUMMER`

### Storno

- Kopf: `rechnungsdetails`
- Positionen: `storno`
- Nummer: Original-Rechnungsnummer + `S`
- Produkt, Preis und MwSt bleiben positiv
- Nur die Menge wird negativ

### Gutschrift

- Kopf: `rechnungsdetails`
- Positionen: `gutschrift`
- Nummernkreis: aus `gutschrift.NUMMER`
- Produkt, Preis und MwSt bleiben positiv
- Nur die Menge wird negativ

### Proforma

- Kopf: `p_rechnungsdetails`
- Positionen: `p_rechnung`
- Nummernkreis: aus `p_rechnungsdetails.ID`
- Angezeigte Nummer: `<ID>P`

## Technische Änderungen

- `InvoiceRepository` trennt jetzt normale Rechnung, Storno, Gutschrift und Proforma nach Tabellen.
- `findLines()` lädt abhängig vom Belegtyp aus `rechnung`, `storno`, `gutschrift` oder `p_rechnung`.
- Die Suche berücksichtigt zusätzlich Proforma-Belege aus `p_rechnungsdetails`.
- Proforma wird nicht mehr in `rechnung/rechnungsdetails` gespeichert.
- Gutschriften erhalten nicht mehr einfach die Originalnummer + `G`, sondern nutzen ihren eigenen Nummernkreis aus `gutschrift`.
- Storno verwendet weiterhin Originalnummer + `S`.
- Der bekannte `JwtService.padEnd`-Buildfehler bleibt behoben.

## Hinweis

Maven konnte in der Erzeugungsumgebung nicht kompiliert werden, weil dort kein Internetzugriff für den Maven-Download verfügbar ist. Die Version ist für den lokalen Test auf dem bereits eingerichteten GAM2-System gedacht.
