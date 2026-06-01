# Erster technischer Gesamttest – Kurzablauf

## Reihenfolge

1. ZIP entpacken
2. SQL-Dump in eine Testdatenbank importieren
3. `.lbd`-Datei nach `config/` legen oder `GAM_LBD_FILE` setzen
4. Backend konfigurieren
5. Backend bauen
6. Backend starten
7. Smoke-Test-Script ausführen
8. Frontend starten
9. Login und Hauptmodule prüfen

## Wichtig

Bitte zunächst nur gegen eine Kopie deiner Datenbank testen.

## Schnellprüfung Windows

```powershell
cd scripts
.\smoke-test.ps1 -BaseUrl http://localhost:8080
```

## Schnellprüfung Linux/macOS/Git Bash

```bash
cd scripts
chmod +x smoke-test.sh
./smoke-test.sh http://localhost:8080
```

## Interpretation

- HTTP 200/204: Endpunkt grundsätzlich erreichbar
- HTTP 401/403: bei geschützten Endpunkten ohne Token normal
- HTTP 500: Backend-/Datenbank-/Mappingfehler, bitte Log prüfen
- Verbindung verweigert: Backend läuft nicht oder Port falsch
