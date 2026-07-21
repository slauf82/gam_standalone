# Schritt 31d – Login-i18n vervollständigt

## Ziel

Alle sichtbaren Texte im Login-Dialog reagieren auf die gewählte Oberflächensprache.

## Konkret übersetzt

- Kompatibler Login über bestehende accounts-Tabelle.
- Sprache der Oberfläche
- Anwendung wählen / Application wählen
- Hinweis zu historischen GAM-Anwendungen und Lesemodus
- Benutzername
- Passwort
- Anmelden
- 2FA registrieren
- 2FA-Login
- Passkey registrieren
- Passkey-Login

## Wichtig

Die Trennung bleibt erhalten:

- Login-Sprache = Oberflächensprache
- PDF-Sprache im Rechnungsprogramm = Dokumentsprache

## Test

1. Login öffnen
2. Sprache auf Englisch, Französisch und Ukrainisch wechseln
3. Prüfen, ob alle oben genannten Texte direkt mitwechseln
4. Login durchführen
5. Prüfen, dass die PDF-Sprache im Rechnungsprogramm separat bleibt
