# GAM 2.0 – Phase 1A Status

Stand: Phase 1A Fundament

## Ziel dieser Version

Diese Version ist noch kein fertiges Rechnungsprogramm, sondern stabilisiert die Grundlage:

- echte MariaDB-Anbindung über die bestehende Datenbank `kopfzentruminventardb`
- kompatibles Account-Modell auf Basis der alten Tabelle `accounts`
- Login-Service als zentrale Stelle für Passwort/2FA-Entscheidung
- JWT-Session für das neue Frontend
- Rollenübernahme aus `accounts.role`
- `.lbd`-Dateisuche und Vorschau bleibt erhalten
- Systemstatus-API für erste Diagnose

## Neue/verbesserte Endpunkte

```text
POST /api/auth/login
GET  /api/auth/me
GET  /api/system/status
GET  /api/invoices/lbd/preview
GET  /api/invoices
GET  /api/invoices/{nummer}
```

## Login-Kompatibilität

Bestehende Tabelle bleibt unverändert:

```sql
accounts(id, username, password, fullname, role, email, secretkey)
```

Standardverhalten in dieser Phase:

```yaml
app.legacy-login.allow-sha256-password: true
app.legacy-login.allow-totp-only: true
app.legacy-login.require-totp-when-secret-exists: false
```

Das bedeutet:

- alter SHA-256-Passwortlogin bleibt möglich
- 2FA/TOTP über `secretkey` bleibt möglich
- wenn ein `secretkey` vorhanden ist, wird 2FA aktuell noch nicht zwingend erzwungen
- diese Einstellung kann später ohne Datenbankänderung verschärft werden

## .lbd-Dateien

Suchreihenfolge:

1. explizit gesetzter Pfad per `GAM_LBD_FILE`
2. `./config`
3. `./daten/rechnung`
4. Projektwurzel

Konfiguration:

```bash
GAM_LBD_FILE=./config/meine-datei.lbd
GAM_LBD_CHARSET=windows-1252
```

## Noch nicht produktiv fertig

Noch offen für Phase 1B:

- Rechnung anlegen
- Rechnungspositionen bearbeiten
- Summenberechnung serverseitig
- Speichern neuer Rechnungen
- PDF-Erzeugung
- Nummernkreis/Validierung

## Testreihenfolge später

1. MariaDB starten/importieren
2. Backend starten
3. `/api/system/status` prüfen
4. Login mit bestehendem Benutzer testen
5. `.lbd`-Preview prüfen
6. Rechnungsübersicht lesen
