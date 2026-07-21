# Schritt 40a – Rechnungsworkflow Foundation

GitHub-Ziel: GAM 2.1.0

## Neu

- Generische, persistente Workflow-Grundlage fuer Rechnungen
- Statuskette: ENTWURF -> PRUEFUNG -> FREIGEGEBEN -> VERSENDET -> ABGESCHLOSSEN
- Rueckgabe von PRUEFUNG zu ENTWURF moeglich
- Vollstaendige Aenderungshistorie mit Benutzer, Zeit und Notiz
- Automatische und idempotente Anlage der Tabellen `invoice_workflow_state` und `invoice_workflow_history`
- REST-Endpunkte zum Laden und Fortschalten des Rechnungsworkflows
- Frontend-API vorbereitet

## Sicherheit

- Leserechte fuer Workflow-Status folgen den Rechnungsleserechten
- Statuswechsel erfordern Rechnungs-Schreibrechte
- Bestehende Storno-, Gutschrift- und Zahlungsavis-Logik bleibt getrennt und unveraendert

## Endpunkte

- `GET /api/invoices/{nummer}/workflow`
- `POST /api/invoices/{nummer}/workflow/transition`

Beispiel fuer einen Wechsel:

```json
{
  "status": "PRUEFUNG",
  "note": "Inhalt fachlich geprueft"
}
```
