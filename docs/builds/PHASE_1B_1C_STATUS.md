# GAM Standalone – Phase 1B/1C Status

Stand: 2026-05-23

## Ziel dieser Version

Diese Version ergänzt das Phase-1A-Fundament um den ersten echten vertikalen Rechnungsfluss:

1. vorhandene Rechnungen anzeigen
2. Produkte aus `rechnungsdaten` laden
3. neue Rechnung speichern
4. Rechnungsdetails in `rechnungsdetails` schreiben
5. Rechnungspositionen in `rechnung` schreiben
6. Summen berechnen
7. einfache PDF-Vorschau/Download erzeugen
8. `.lbd`-Empfänger weiterhin lesen und anzeigen

## Neu im Backend

### Neue Endpunkte

- `GET /api/invoices/draft`
  - liefert nächste Rechnungsnummer, Gesellschaften, heutiges Datum und `.lbd`-Empfänger

- `POST /api/invoices/calculate`
  - berechnet netto/MwSt/brutto für übergebene Positionen

- `POST /api/invoices`
  - legt eine Rechnung an
  - schreibt in `rechnungsdetails`
  - schreibt Positionen in `rechnung`

- `GET /api/invoices/{number}/pdf`
  - erzeugt eine einfache PDF-Rechnung als ersten Export

- `GET /api/invoices/companies`
  - lädt Rechnungsgesellschaften aus `rechnungsgesellschaft`

### Verwendete Bestandstabellen

- `rechnungsdetails`
- `rechnung`
- `rechnungsdaten`
- `rechnungsgesellschaft`
- `accounts`

## Wichtig: PDF-Status

Der PDF-Export ist bewusst noch **kein finales Original-Layout**.

Aktuell ist es ein technischer Grundexport mit:

- Rechnungsnummer
- Datum
- Gesellschaft
- `.lbd`-Empfänger, falls vorhanden
- Positionstabelle
- Gesamtbetrag

Das alte Rechnungsformular/Layout wird in einer späteren Phase nachgezogen.

## Wichtig: Speichern-Status

Das Speichern ist als erster produktiver Schnitt umgesetzt, aber noch konservativ:

- eine oder mehrere Positionen können gespeichert werden
- Rechnungsnummer wird automatisch aus `MAX(RNUMMER)+1` vorgeschlagen, falls keine Nummer übergeben wird
- `ENDPREIS` wird als Bruttosumme gespeichert
- Benutzername kommt aus JWT/Login, fallback `gam2`
- Zahlungsart fallback `unbekannt`
- Gesellschaft fallback ID `2`

Noch nicht umgesetzt:

- echte Adressauswahl aus `adressen`
- Kind-/Firma-Adressen-Auswahl
- altes Drucklayout
- Rechnungstexte/Anreden/Grussformeln/Hinweise
- Logo-Auswahl
- Storno/Gutschrift-Workflow
- Zahlungsavis-Workflow
- mehrere komfortable Positionen im Frontend
- exakte alte Rundungs-/Rabatt-/Ratenlogik, falls vorhanden

## Frontend

Neu:

- Button „Neue Rechnung“
- einfache Rechnungserfassung mit Gesellschaft-ID, Produkt und Menge
- Speichern gegen Backend
- PDF-Link in Rechnungsdetailansicht

## Testvorschlag

Erst nach Datenbankimport testen:

1. MariaDB starten/importieren
2. Backend konfigurieren
3. Login mit bestehendem `accounts`-Benutzer
4. `.lbd` in `config/` oder `daten/rechnung/` ablegen
5. Rechnungen anzeigen
6. Neue Testrechnung mit einem Produkt anlegen
7. PDF öffnen

## Hinweis

Diese Version ist ein technischer Meilenstein, aber noch nicht der finale produktive Rechnungsersatz.
Sie beweist den wichtigsten Ablauf: Login → Datenbank → Rechnung anlegen → speichern → PDF.
