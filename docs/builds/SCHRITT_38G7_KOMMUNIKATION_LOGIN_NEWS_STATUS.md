# Schritt 38g7 – Kommunikation / SMTP-Assistent / Login-News

## Inhalt

- Kommunikationsbereich im Bestelltool ergänzt.
- SMTP-Assistent mit GMX-Standardwerten (`mail.gmx.net`, Port `587`, STARTTLS) ergänzt.
- SMTP-Daten können lokal im Browser gepflegt und für den direkten Bestellversand verwendet werden.
- Testmail-Funktion ergänzt.
- `application-local.yml` um Kommunikations-/Mail-Defaults erweitert.
- Öffentlicher Login-News-Endpunkt ergänzt.
- Login-Dialog zeigt wichtige Hinweise/News an, wenn aktiviert.
- Backend-Kommunikations-Endpunkt für Settings/Login-News ergänzt.
- Direkter Bestellmailversand kann wahlweise über Backend-Konfiguration oder lokal eingegebene SMTP-Daten laufen.

## Sicherheit / Konfiguration

- Keine Passwörter im Repository.
- GMX ist nur als Provider-Standard vorbelegt.
- Benutzername/Passwort/App-Passwort bleiben lokal oder werden über Umgebungsvariablen gesetzt.
- Eine automatische Registrierung einer Freemail-Adresse ist bewusst nicht enthalten.

## Test

- Frontend-Build erfolgreich getestet.
- Backend-Maven-Build konnte in dieser Umgebung nicht abgeschlossen werden, weil Maven/Repository-Zugriff nicht verfügbar war.

## Paketstandard

- Vollständiges Quellcodepaket.
- Ohne `node_modules`, `dist`, `target`, `.git` und Cache-Verzeichnisse.
