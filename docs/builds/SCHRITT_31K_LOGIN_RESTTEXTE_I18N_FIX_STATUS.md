# Schritt 31k – Login-Resttexte i18n-Fix

## Ziel

Die noch deutsch bleibenden Texte im Login werden gezielt an die vorhandene `ui(...)`-Funktion angebunden.

## Konkret korrigiert

- `Kompatibler Login über bestehende accounts-Tabelle.`
- `Application wählen` / `Anwendung wählen`
- Login-Typ-Buttons:
  - Passwort
  - 2FA registrieren
  - 2FA-Login
  - Passkey registrieren
  - Passkey-Login
- Textfeld-Platzhalter:
  - Benutzername
  - Passwort
- Loginbutton:
  - Anmelden

## Wichtig

- Keine neue Backend-i18n.
- Kein LibreTranslate-Experiment.
- Keine YAML-Änderungen.
- PDF-Sprache bleibt separat.
