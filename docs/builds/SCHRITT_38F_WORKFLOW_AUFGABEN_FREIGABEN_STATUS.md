# Schritt 38f – Workflow: Aufgaben & Freigaben nach GDS 1.0

## Ziel

Schritt 38f baut die ersten Workflow-Fachmodule direkt auf dem neuen GAM Design System (GDS) auf.

Die bisherige reine Leseansicht für Aufgabenverwaltung und Freigabemanagement wurde durch produktivere Moduloberflächen ersetzt.

## Umgesetzt

### Aufgabenverwaltung

- Neue GDS-Modulansicht für Aufgaben
- Sticky-Aktionsleiste mit:
  - Neue Aufgabe
  - Suche
  - Statusfilter
  - Filiale-ID
  - Suchen
- Übersichtstabelle mit festem Kopf im Scrollbereich
- Bearbeitung im modalen Dialog
- Neuerfassung im modalen Dialog
- Speichern mit Toast-Meldung
- Fehler bleiben im Dialog sichtbar
- Löschen vorhandener Aufgaben
- Statistik-Kacheln für Aufgaben/Freigaben

### Freigabemanagement

- Neue GDS-Modulansicht für Freigaben
- Sticky-Aktionsleiste mit:
  - Neue Freigabe
  - Suche
  - Statusfilter
  - Filiale-ID
  - Suchen
- Übersichtstabelle mit festem Kopf im Scrollbereich
- Bearbeitung im modalen Dialog
- Neuerfassung im modalen Dialog
- Speichern mit Toast-Meldung
- Fehler bleiben im Dialog sichtbar
- Löschen vorhandener Freigaben
- Statistik-Kacheln für Aufgaben/Freigaben

### API-Anbindung Frontend

Ergänzt in `frontend/src/api/client.ts`:

- `loadWorkflowTasks(...)`
- `createWorkflowTask(...)`
- `updateWorkflowTask(...)`
- `deleteWorkflowTask(...)`
- `loadWorkflowApprovals(...)`
- `createWorkflowApproval(...)`
- `updateWorkflowApproval(...)`
- `deleteWorkflowApproval(...)`
- `loadWorkflowStats()`

### GDS-Nutzung

Genutzt werden die in Schritt 38e6 vorbereiteten UI-Bausteine:

- `GamDialog`
- `GamStickyToolbar`
- `GamScrollArea`
- `notifySaved(...)`
- `notifySaveError(...)`
- globale Toast-/Meldungsverlauf-Logik

## Build-Test

Frontend-Build erfolgreich getestet mit:

```bash
cd frontend
npm run build
```

Hinweis: Das Build-Script wurde robuster gemacht und ruft TypeScript/Vite direkt über `node ./node_modules/...` auf, weil die entpackten `.bin`-Shims in dieser Umgebung defekt waren.

Backend-Maven-Build konnte in dieser Umgebung nicht ausgeführt werden, weil weder `mvn` noch `mvnw` vorhanden ist.

## Keine Änderungen

- Keine Datenbankstrukturänderungen
- Keine Backend-Neulogik notwendig, vorhandene `/api/workflow/...` Endpunkte werden genutzt
- Keine Änderungen am Rechnungsmodul

## Testempfehlung

1. Login direkt in Aufgabenverwaltung testen.
2. Neue Aufgabe anlegen.
3. Aufgabe bearbeiten.
4. Aufgabe suchen und filtern.
5. Aufgabe löschen.
6. Freigabemanagement öffnen.
7. Neue Freigabe anlegen.
8. Freigabe bearbeiten.
9. Freigabe suchen und filtern.
10. Toast-Meldungen und Meldungsverlauf prüfen.
