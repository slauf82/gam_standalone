# Schritt 38h – Benutzerverwaltung nach GDS-Standard

## Ziel

Schritt 38h macht das Modul **Benutzer/Rechte** erstmals als echtes GDS-Verwaltungsmodul nutzbar.

## Enthalten

- Benutzerliste mit Suche
- direkte Bearbeitung per Klick auf Tabellenzeile
- modaler Bearbeitungsdialog
- Benutzer anlegen
- Benutzer speichern
- Benutzer löschen mit Eigenschutz gegen Selbstlöschung
- Rolle bearbeiten
- Name und E-Mail bearbeiten
- Passwort neu setzen
- 2FA/Secret zurücksetzen
- Toast-Meldungen und Änderungsdetails
- Einträge im Kommunikations-/Meldungsprotokoll

## Backend

- `/api/admin/accounts` erweitert um POST
- `/api/admin/accounts/{id}` weiterhin PATCH
- `/api/admin/accounts/{id}/password` ergänzt
- `/api/admin/accounts/{id}` DELETE ergänzt
- Passwort wird kompatibel zur bestehenden accounts-Tabelle als SHA-256 gespeichert

## Build

- Frontend-Build erfolgreich getestet
- Backend-Maven-Build konnte in dieser Umgebung nicht ausgeführt werden, weil Maven nachgeladen werden wollte und kein Repository-Zugriff verfügbar war
