# Schritt 24 – userapplication-Rechte & gesellschaftsbezogene Rechnungssuche

## Enthaltene Anpassungen

### Rechte-/Zugriffsmodell

- Historische GAM-Tabelle `userapplication` angebunden.
- `SUPERADMIN` bleibt globaler Bypass: keine weitere Modul-/Gesellschaftsprüfung nach erfolgreichem Login.
- Alle anderen Benutzer werden über `userapplication` geprüft:
  - `USERNAME`
  - `APPLICATION`
  - `RGESELLSCHAFTS_ID`
  - `ROLE`
- Rollenlogik:
  - `user`: grundlegender Zugriff / Lesen
  - `mainuser`: zusätzlicher Report-/Exportzugriff
  - `admin`: Änderungsrechte innerhalb der jeweiligen Anwendung/Gesellschaft
  - `superadmin`: überall Zugriff

### Rechnungsprogramm

- Rechnungssuche jetzt mit Gesellschaftsauswahl im Frontend.
- Suche wird erst nach Auswahl einer Gesellschaft ausgeführt.
- Backend akzeptiert `companyId` als Suchfilter.
- Rechnungszugriff wird gegen `userapplication` für `Rechnungsprogramm` und `RGESELLSCHAFTS_ID` geprüft.
- Schreibende Rechnungsaktionen erfordern `admin` in `userapplication` oder `superadmin`.
- Report-/Exportprüfung ist für `mainuser` und `admin` vorgesehen.

### Startskripte

- `start-backend.bat` führt vor dem Start automatisch `mvnw.cmd clean package` aus.
- `start-backend.sh` führt vor dem Start automatisch `./mvnw clean package` aus.
- Backend wird nur gestartet, wenn der Build erfolgreich war.
- `SPRING_PROFILES_ACTIVE=local` wird gesetzt.
- `logs`-Ordner wird automatisch erzeugt.

## Weiterhin enthaltene Dauer-Fixes

- `JwtService` ohne `padEnd()`.
- TOTP mit SHA1, 6 Stellen, 30 Sekunden.
- `allow-totp-only: true`.
- 2FA-Registrierung speichert `accounts.secretkey` erst nach erfolgreicher Bestätigung.
- Passkey-Testworkflow.
- PDF-Archiv und Hotfolder-Konfiguration.
- Gutschrift: Menge positiv, Preis negativ.
- Storno: Menge negativ, Preis positiv.

## Testempfehlung

Nach dem Entpacken/Drüberkopieren:

```bat
start-backend.bat
```

oder unter Linux/macOS:

```bash
./start-backend.sh
```

Danach Frontend:

```bat
cd frontend
npm install
npm run dev
```

## Besonders testen

- Login als Superadmin: alle Module sichtbar/zugänglich.
- Login als normaler Benutzer: nur Module aus `userapplication` sichtbar.
- Rechnungsprogramm: Gesellschaft auswählen, dann suchen.
- User ohne `userapplication`-Eintrag für eine Gesellschaft darf dort nicht schreiben.
- `mainuser` darf Reports/Export prüfen.
- `admin` darf innerhalb der Anwendung/Gesellschaft ändern.
