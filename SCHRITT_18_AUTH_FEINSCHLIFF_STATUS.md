# Schritt 18 – Auth-Feinschliff

Diese Version ergänzt die in Schritt 17 eingeführte Login-Modernisierung um die ersten echten Feinschliffe für den praktischen Test.

## Enthalten

- 2FA-Registrierung speichert den bestätigten Secret-Key explizit in `accounts.secretkey`.
- Die Speicherung erfolgt erst nach erfolgreicher Prüfung des eingegebenen TOTP-Codes.
- Login-Rate-Limit liefert nun HTTP 429 mit `retryAfterSeconds` und `Retry-After`-Header.
- Frontend zeigt bei zu vielen Fehlversuchen einen Countdown an.
- Login-Buttons werden während der Sperrzeit deaktiviert.
- API-Client wertet JSON-Fehlerantworten und `retryAfterSeconds` aus.
- Session-Ablauf/403/401 bleibt mit Auto-Logout/Redirect behandelt.

## Weiterhin vorbereitet

- Passkey-UI
- `account_passkeys`-Tabelle
- WebAuthn-Challenge-/Response-Implementierung folgt separat.

## Testhinweise

1. Backend bauen/starten.
2. Frontend ggf. mit `npm install` aktualisieren.
3. 2FA registrieren:
   - Benutzername und Passwort eingeben
   - QR-Code erzeugen
   - Code im Authenticator prüfen
   - 2FA speichern
   - in DB prüfen: `SELECT username, secretkey FROM accounts WHERE username = '...'`
4. 2FA-Login testen.
5. Mehrere falsche Passwort-Logins auslösen und Countdown prüfen.
