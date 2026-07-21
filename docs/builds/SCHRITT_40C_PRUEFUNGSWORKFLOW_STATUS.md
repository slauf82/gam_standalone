# Schritt 40c – Prüfungsworkflow

## Umgesetzt
- Neuer Workflowbereich **Einstellungen → Workflows → Prüfungsworkflow**.
- Konfigurierbare Frühwarnfrist.
- Aktivierbare Eskalation überfälliger Prüfungen.
- Vorbereitung automatischer Folgetermine anhand der vorhandenen Intervalle.
- Optionale Pflichtangaben für verantwortliche Person und Ergebnisnotiz.
- Konfigurierbare Aufgaben und Benachrichtigungen für gelbe und rote Zustände.
- Statusanzeige in Navigation und Dashboard konfigurierbar.
- Workflow-Zusammenfassung direkt im Modul **Prüfungen**.
- Bestehende rote/gelbe/grüne Statuslogik und Prüfungsdaten bleiben erhalten.

## Test
1. Backend und Frontend starten.
2. Einstellungen → Workflows → Prüfungsworkflow öffnen.
3. Frühwarnfrist und Optionen ändern und speichern.
4. Modul Prüfungen öffnen.
5. Prüfen, dass die Workflow-Zusammenfassung sichtbar ist und vorhandene überfällige/bald fällige Datensätze weiterhin rot/gelb dargestellt werden.
