# GAM 2.1.0 – Schritt 40k31g

## Registrierte Geräte: Gerätetyp direkt ändern

- Gerätetyp in „Registriert“ als Combobox auswählbar
- Bestätigungsdialog vor der Änderung
- sofortige Umsortierung in die neue Gerätekategorie ohne neuen Scan
- manuelle Zuordnung wird dauerhaft markiert
- spätere Discovery-Scans überschreiben den manuell festgelegten Typ nicht
- semantische Zusammenführung respektiert manuelle Gerätetypen
- Änderungsverlauf in `gam_discovery_device_type_history`
  - Identitätsschlüssel
  - alter Gerätetyp
  - neuer Gerätetyp
  - Benutzer
  - Zeitpunkt
- API-Endpunkt: `PUT /api/inventory/discovery/registered/device-type`

## Datenbank

Die benötigte Spalte und Historientabelle werden beim Start automatisch ergänzt.
