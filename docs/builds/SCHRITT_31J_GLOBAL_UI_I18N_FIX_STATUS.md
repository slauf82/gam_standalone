# Schritt 31j – Globaler UI-i18n-Fix

## Ziel

Die Oberflächensprache soll sichtbar und zuverlässig greifen:

- im Login
- bei der Anwendungsauswahl
- bei Modulnamen
- in ersten Bereichen des Rechnungsprogramms

## Ursache der vorherigen Probleme

Die Sprachwahl wurde teilweise gespeichert, aber nicht überall über dieselbe Render-Funktion verwendet.
Einige Bereiche hatten weiterhin feste deutsche Texte oder verwendeten eine andere/alte Übersetzungsfunktion.

## Fix

- zentrale Frontend-Funktion `ui(key)`
- zentrale Modulübersetzung `uiModule(labelOrKey)`
- Sprachwechsel aktualisiert React-State und nicht nur localStorage
- feste Logintexte durch `ui(...)` ersetzt
- Modulnamen in häufigen Render-Mustern durch `uiModule(...)` ersetzt
- kein Backend-Translation-Cache
- keine YAML-Änderungen
- PDF-Sprache bleibt separat

## Test

1. Login öffnen
2. Sprache auf Englisch/French/Ukrainisch stellen
3. prüfen:
   - Login-Hinweis
   - Anwendung wählen
   - Lesemodus-Hinweis
   - Modulnamen
   - Benutzername/Passwort/Anmelden
   - 2FA-/Passkey-Buttons
4. ins Rechnungsprogramm gehen
5. prüfen, ob erste UI-Labels mitgehen
6. PDF-Sprache separat testen
