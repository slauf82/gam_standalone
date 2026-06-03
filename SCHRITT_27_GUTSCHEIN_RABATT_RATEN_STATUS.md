# Schritt 27 – Gutschein, Rabatt und Ratenzahlung

Diese Testversion erweitert das Rechnungsmodul um weitere ursprüngliche GAM-Fachlogik.

## Enthalten

- Gutschein-Felder im Rechnungseditor
  - Gutscheintext
  - Gutscheinbetrag
- Rabatt-Felder im Rechnungseditor
  - Rabatt als Prozentwert
  - Rabatt als Betrag
- Ratenzahlung im Rechnungseditor
  - 1 bis 5 Raten
  - automatische Grundlogik zur Erzeugung von Zahlungsavis-Belegen
- Speicherung der Zusatzwerte in vorhandenen Feldern der Tabelle `rechnungsdetails`
  - `GPREIS`
  - `RPROZENT`
  - `RBEMERKUNG`
  - `RATENANZAHL`
- Zahlungsavis-Grundlogik über Tabelle `zahlungsavis`
- Erkennung von Zahlungsavis-Nummern in der UI
- Bestehende Fixes aus Schritt 26 bleiben enthalten

## Hinweise

Die Zahlungsavis-Logik ist als testbare Grundlogik umgesetzt. Die spätere Administrations- und Reportlogik des Rechnungsmoduls bleibt für die nächsten Schritte vorgesehen.

## Testempfehlung

1. Normale Rechnung mit Gutschein erstellen
2. Normale Rechnung mit Rabatt in Prozent erstellen
3. Normale Rechnung mit Rabattbetrag erstellen
4. Rechnung mit 2–5 Raten erstellen
5. Prüfen, ob Zahlungsavis-Einträge entstehen
6. Prüfen, ob Storno/Gutschrift/Proforma weiterhin funktionieren
