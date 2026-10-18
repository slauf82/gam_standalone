# 40k36r14 – Unicode/Emoji in Gerätekategorien

Fehlerbild:
`Incorrect string value ... for column gam_discovery_registered_devices.device_type`

Ursache:
Evidenzbasierte Gerätekategorien können Unicode/Emoji enthalten, z. B. `🖥 Android Display`.
Ältere MariaDB-Installationen besitzen für `device_type` bzw. die Historienfelder teilweise kein utf8mb4.

Fix:
- `gam_discovery_registered_devices.device_type` wird auf utf8mb4 migriert.
- `gam_discovery_device_type_history.old_device_type` und `new_device_type` ebenfalls.
- Neue Tabellen werden bereits mit utf8mb4 für diese Spalten angelegt.
- Die Migration ist idempotent und läuft beim Repository-Start.
- Kategorien werden nicht umbenannt oder Emoji abgeschnitten; die bestehende Klassifizierungslogik bleibt erhalten.
