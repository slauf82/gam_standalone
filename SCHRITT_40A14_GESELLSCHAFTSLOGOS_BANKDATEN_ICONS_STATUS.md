# Schritt 40a14 – Gesellschaftslogos, Bankdaten und Icon-Trennung

- Vorschau und PDF verwenden das Logo der ausgewählten Gesellschaft.
- Der globale Logo-Datensatz ID 0 bleibt nur Notfall-Fallback und überschreibt nicht mehr die gesellschaftsspezifischen Legacy-Logos.
- Hochgeladene bzw. zugeordnete Logos mit LOGO_ID > 0 haben Vorrang.
- Bankdaten erscheinen in Vorschau, Vorlesetext und PDF nur bei zahlungsrelevanten Arten wie Überweisung, SEPA/Lastschrift oder Ratenzahlung.
- Dashboard und Reports besitzen jetzt klar unterschiedliche Symbole.

## Test
1. Mindestens zwei Gesellschaften mit unterschiedlichen Logos nacheinander auswählen.
2. Vorschau prüfen und PDF erzeugen; das Logo muss jeweils identisch und gesellschaftsspezifisch sein.
3. Zahlungsart Bar/Karte: keine Bankdaten.
4. Zahlungsart Überweisung/SEPA/Ratenzahlung: Bankdaten sichtbar.
5. Dashboard- und Reports-Symbol vergleichen.
