# Schritt 40b – Zahlungsworkflow v1

## Enthalten
- eigener persistenter Zahlungsstatus pro Rechnung und Gesellschaft
- Status: OFFEN, TEILZAHLUNG, BEZAHLT, STORNIERT
- Buchung beliebig vieler Teilzahlungen
- offene Summe wird automatisch berechnet
- vollständige Zahlung per Ein-Klick-Aktion
- Zahlungshistorie mit Betrag, Benutzer, Zeitpunkt und Notiz
- sichtbares Zahlungsworkflow-Panel direkt unter dem Rechnungsworkflow
- Fälligkeit und Ampelrahmen im Zahlungsbereich
- Einstellungen → Workflows → Zahlungsworkflow als eigener Unterpunkt

## Test
1. Rechnung öffnen: Zahlungsworkflow wird mit Rechnungsbetrag initialisiert.
2. Teilbetrag buchen: Status TEILZAHLUNG, Restbetrag korrekt.
3. Restbetrag buchen: Status BEZAHLT, offen 0,00 EUR.
4. Verlauf öffnen: beide Buchungen vorhanden.
5. Browser/Backend neu starten: Zustand bleibt erhalten.
6. Zwei Rechnungen testen: Zahlungen bleiben getrennt.
7. Stornieren und Zurücksetzen testen.
8. Rechnungsworkflow, PDF, ZUGFeRD, Vorschau und Sonderbelege gegenprüfen.
