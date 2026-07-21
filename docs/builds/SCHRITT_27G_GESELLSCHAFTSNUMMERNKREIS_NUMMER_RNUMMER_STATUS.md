# Schritt 27g – Gesellschaftsabhängiger Nummernkreis

Dieser Build ergänzt den Fix für die Rechnungsnummernlogik:

- `rechnung.NUMMER` ist der Nummernkreis der Rechnungspositionen/Kopfdaten je Gesellschaft.
- `rechnungsdetails.RNUMMER` bleibt der Bezug der Rechnungsdetails zur Rechnung.
- Die nächste Rechnungsnummer wird jetzt gesellschaftsabhängig aus `rechnung.NUMMER` ermittelt.
- `companyId` wird vom Frontend über den Controller bis ins Repository weitergereicht.
- Beim Gesellschaftswechsel im Rechnungsformular wird die Nummer neu geladen.
- Beim Speichern einer neuen Rechnung wird die Nummer für die ausgewählte Gesellschaft berechnet.

Wichtiger Test:

1. Neue Rechnung öffnen.
2. Gesellschaft A wählen.
3. Vorgeschlagene Nummer notieren.
4. Gesellschaft B wählen.
5. Die vorgeschlagene Nummer muss sich nach dem Nummernkreis von Gesellschaft B richten.
6. Rechnung speichern und prüfen, ob `rechnungsdetails.RNUMMER` und `rechnung.NUMMER` korrekt gesetzt sind.
