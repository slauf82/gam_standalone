# Schritt 15 – Rechnungsarten / Storno / Gutschrift / Proforma

Diese Version ergänzt die fachlichen Rechnungsarten nach GAM-Regel:

- Stornorechnung = Originalrechnungsnummer + `S`
- Gutschrift = Originalrechnungsnummer + `G`
- Proforma-Rechnung = eigene Rechnungsnummer + `P`

## Enthalten

### Backend

- Neue Endpunkte:
  - `POST /api/invoices/{number}/cancel`
  - `POST /api/invoices/{number}/credit`
  - `POST /api/invoices/proforma`
- Stornorechnung wird nicht mehr nur markiert, sondern als eigene Rechnung `<Original>S` angelegt.
- Gutschrift wird als eigene Rechnung `<Original>G` angelegt.
- Proforma erhält eine eigene Nummer mit Suffix `P`.
- Positionen werden bei Storno/Gutschrift aus der Originalrechnung übernommen.
- Storno/Gutschrift-Positionen werden negativ übernommen.
- Storno- und Gutschriftrechnungen werden über die normale Rechnungssuche/Detailansicht geladen.
- Bereits vorhandene `<Original>S` oder `<Original>G` werden nicht doppelt erstellt, sondern zurückgegeben.
- Bekannter Maven-Fix bleibt enthalten: `JwtService` verwendet kein `String.padEnd(...)` mehr.

### Frontend

- Detailansicht enthält neue Aktionen:
  - `Stornorechnung <Nummer>S`
  - `Gutschrift <Nummer>G`
- Sonderrechnungen mit Endung `S`, `G` oder `P` werden gegen erneute Storno-/Gutschriftaktion gesperrt.
- Neue Rechnung kann als `Proforma +P` erstellt werden.
- UI-Fixes aus Schritt 13/14 bleiben enthalten.

## Testvorschlag

1. Normale Rechnung mit mindestens einer Position anlegen.
2. Rechnung öffnen.
3. `Stornorechnung <Nummer>S` klicken.
4. Prüfen:
   - Rechnung `<Nummer>S` wird angelegt.
   - Positionen sind vorhanden.
   - Beträge/Mengen sind negativ.
   - PDF/XML lassen sich öffnen.
5. Originalrechnung suchen.
6. `Gutschrift <Nummer>G` klicken.
7. Prüfen:
   - Rechnung `<Nummer>G` wird angelegt.
   - Positionen sind vorhanden.
   - Beträge/Mengen sind negativ.
8. Neue Rechnung als `Proforma +P` anlegen.
9. Prüfen:
   - Rechnungsnummer endet mit `P`.
   - Positionen sind vorhanden.

## Hinweis

Diese Version wurde im aktuellen Container nicht mit Maven kompiliert, weil kein Internetzugriff zum Nachladen von Maven/Dependencies verfügbar war. Der vorherige bekannte Compile-Fehler `padEnd` ist in dieser Version explizit korrigiert.
