# GAM 2.0 – Schritt 1: Rechnungsmodul weiter ausgebaut

Stand: 2026-05-23

## Ziel dieses Pakets

Dieses Paket baut auf `gam-standalone-phase1c-zugferd` auf und fokussiert den ersten der fünf nächsten Schritte:

> Rechnungsmodul fertigstellen und ZUGFeRD/Factur-X als Pflichtpfad stabilisieren.

## Neu gegenüber Phase 1C

### Backend

- Rechnungssuche über `/api/invoices?q=...`
- Filter-Grundlage für Gesellschaft, Gutschrift, Storno und Zahlungsavis
- Detail-API liefert jetzt zusätzlich berechnete Summen
- Nummernkreis-API:
  - `/api/invoices/numbers/next`
  - kompatibel: höchste numerische `RNUMMER` aus `rechnungsdetails` + 1
- Duplikatsschutz beim Speichern einer Rechnung
- mehrere Rechnungspositionen pro neuer Rechnung vorbereitet
- Export-Vorprüfung:
  - `/api/invoices/{nummer}/export-check`
- ZUGFeRD-Export bricht bei harten Fehlern ab, statt still ein fehlerhaftes Dokument zu erzeugen
- ZUGFeRD XML kann weiterhin separat geprüft werden:
  - `/api/invoices/{nummer}/zugferd.xml`
- Debug-PDF bleibt als Fallback:
  - `/api/invoices/{nummer}/pdf-debug`

### PDF/ZUGFeRD

- sichtbares PDF-Layout erweitert:
  - Rechnungsgesellschaft oben
  - Empfänger aus `.lbd`
  - Rechnungsdatum, Benutzer, LBD-Datei
  - Positionsliste
  - Netto/MwSt/Gesamt
  - Bankverbindung/Steuerdaten der Rechnungsgesellschaft
- `/pdf` bleibt der verbindliche ZUGFeRD/Factur-X-PDF-Export
- `/pdf-debug` bleibt bewusst nur technischer Fallback

### Frontend

- Suche in Rechnungsliste
- Anzeige der berechneten Summen im Detail
- Export-Check-Anzeige je Rechnung
- neue Rechnung mit mehreren Positionen vorbereitet
- Anzeige der nächsten Rechnungsnummer

## Noch bewusst offen

Das ist noch nicht das komplett fertige alte Rechnungsprogramm, sondern der nächste echte Ausbau:

- finales altes Drucklayout noch nicht vollständig rekonstruiert
- LBD-Felder müssen ggf. nach echtem Dateibeispiel nachjustiert werden
- harte externe ZUGFeRD/EN16931-Validierung noch nicht aktiviert
- Mahnungen/Storno/Wiederholungen noch nicht migriert
- DATEV-/Buchhaltungslogik noch nicht migriert
- Adressauswahl aus alter `adressen`-Tabelle noch nicht als UI umgesetzt

## Wichtig

Die Logik bleibt absichtlich kompatibel zur alten Datenbank:

- `rechnung`
- `rechnungsdetails`
- `rechnungsdaten`
- `rechnungsgesellschaft`
- `.lbd`-Dateien

Der nächste sinnvolle Schritt wäre danach:

**Schritt 2: Benutzer/Rechte komplett + GAM-Hauptmenü/Modulrahmen**
