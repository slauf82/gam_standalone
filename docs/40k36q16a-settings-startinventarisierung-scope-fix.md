# 40k36q16a – Settings-Fix Startinventarisierung

Behoben:
- `startupInventorySettings is not defined` im Modul Einstellungen → Geräteverwaltung.
- Fehlende API-Imports für `StartupInventorySettings`, `loadStartupInventorySettings` und `saveStartupInventorySettings` wiederhergestellt.
- Fehlende React-States `startupInventorySettings`, `startupInventoryBusy` und `startupInventoryMessage` wiederhergestellt.
- Persistente Startinventarisierungs-Einstellung wird beim Laden der SettingsPage wieder eingelesen.
- Speichern/Aktivieren/Deaktivieren verwendet weiterhin den q13-Backend-Endpunkt.
- Dashboard-Workflowstand aus q16 bleibt vollständig enthalten.
