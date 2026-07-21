# Schritt 40b2 – Zahlungsworkflow 403- und Rechnungsintegrations-Fix

## Behoben

- Zahlungsworkflow bleibt direkt bei der geöffneten Rechnung sichtbar.
- Berechtigungsprüfung verwendet denselben Anwendungsschlüssel wie das Rechnungsprogramm.
- Gesellschaftsrechte werden robust gegen Request-, Rechnungs- und globale Gesellschaftszuordnung geprüft.
- Lesezugriff berücksichtigt vorhandene Öffnen-, Schreiben- oder Reportberechtigungen.
- Schreibaktionen bleiben auf Schreibberechtigungen begrenzt.
- Bei einem verbleibenden Fehler bleibt ein sichtbares Zahlungsworkflow-Panel mit verständlicher Meldung stehen.

## Test

1. Anmelden und Rechnungsprogramm öffnen.
2. Eine vorhandene Rechnung auswählen.
3. Direkt unter dem Rechnungsworkflow muss der Zahlungsworkflow erscheinen.
4. Es darf beim Laden kein HTTP 403 mehr erscheinen.
5. Zahlung buchen und prüfen, ob der Status und offene Betrag aktualisiert werden.
6. Mit einem reinen Lesebenutzer prüfen: Workflow sichtbar, Schreibaktionen müssen weiterhin geschützt bleiben.
