# Schritt 40a3 – Rechnungsworkflow UI-Integration

- Workflow-Status direkt in der Rechnungsdetailansicht sichtbar
- Prozessleiste Entwurf → Prüfung → Freigegeben → Versendet → Abgeschlossen
- nur zulässige Statusaktionen werden angeboten
- Rückgabe zur Bearbeitung mit optionaler Notiz
- vollständiger Historien-Dialog
- Basis: Schritt 40a2

## Test
1. Rechnung auswählen: Status muss als Entwurf erscheinen.
2. Alle zulässigen Übergänge durchlaufen.
3. Seite und Backend neu laden: Status bleibt erhalten.
4. Historie öffnen und Benutzer, Zeitpunkt, Wechsel und Notiz prüfen.
5. Storno, Gutschrift, PDF und ZUGFeRD unverändert testen.
