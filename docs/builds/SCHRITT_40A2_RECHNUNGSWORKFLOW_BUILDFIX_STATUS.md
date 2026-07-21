# Schritt 40a2 – Rechnungsworkflow Buildfix

GitHub-Ziel: GAM 2.1.0

## Behoben

- `GamPermissionService.canRead(...)` durch die vorhandene Methode `canOpen(Account, String, Integer)` ersetzt.
- Übergabe des Benutzernamens als `String` an `canWrite(...)` durch Übergabe von `AuthenticatedUser.account()` ersetzt.
- Modulschlüssel an den bestehenden Rechnungscontroller angeglichen: `Rechnungsprogramm`.

## Betroffene Datei

- `backend/src/main/java/de/kopfzentrum/gam/invoice/InvoiceWorkflowController.java`

## Test

1. `mvn clean compile` bzw. den normalen GAM-Build ausführen.
2. Rechnung öffnen und Workflowstatus abrufen.
3. Statusübergang mit schreibberechtigtem Benutzer testen.
4. Benutzer ohne Rechnungsrecht muss HTTP 403 erhalten.
