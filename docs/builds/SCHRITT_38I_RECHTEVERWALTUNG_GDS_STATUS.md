# Schritt 38i – Rechteverwaltung nach GDS

## Ziel

Schritt 38i ergänzt den zweiten Teil des Testblocks 38h/38i: Nach der Benutzerverwaltung folgt die direkte Verwaltung der Benutzer-Anwendungsrechte aus der historischen GAM-1.0-Tabelle `userapplication`.

## Enthalten

- Neues Modul **Rechteverwaltung**
- Direktzugriff im Login-Dialog
- Modulwechsel im laufenden GAM-Betrieb
- Tabelle der Benutzer-Anwendungszuordnungen
- Tabellenzeile direkt anklickbar
- Neue Rechtezuordnung anlegen
- Rechtezuordnung bearbeiten
- Rechtezuordnung löschen
- Felder: Benutzer, Anwendung, Gesellschafts-ID, Rolle
- Rollen: `user`, `mainuser`, `admin`, `viewer`
- Anwendungsliste aus vorhandener Tabelle `application` und vorhandenen Zuordnungen
- Benutzerliste aus vorhandener Accountverwaltung
- Toast-Meldungen und Änderungsdetails über GDS/GCS

## Backend

Neue Endpunkte:

- `GET /api/admin/permissions`
- `GET /api/admin/permissions/applications`
- `POST /api/admin/permissions`
- `PATCH /api/admin/permissions/{id}`
- `DELETE /api/admin/permissions/{id}`

## Wichtig

Die Rechteverwaltung arbeitet bewusst mit der bestehenden GAM-1.0-Struktur `userapplication`, damit die Altlogik nicht parallel neu erfunden wird.

## Paketregel

Das Paket ist vollständig, enthält aber keine generierten Ordner:

- kein `node_modules`
- kein `dist`
- kein `target`
