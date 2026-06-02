# Schritt 21 – 2FA-Registrierung ohne Passwortfeld

## Änderungen

- Passwortfeld im Tab „2FA registrieren“ entfernt.
- Frontend sendet bei 2FA-Setup nur noch den Benutzernamen.
- Backend erzeugt temporäres Secret anhand des Benutzernamens.
- Backend bestätigt den TOTP-Code gegen das temporäre Secret.
- `accounts.secretkey` wird erst nach erfolgreicher Bestätigung per Benutzername gespeichert.
- Der bekannte `client.ts`-Parserfix bleibt enthalten.
- Der `padEnd`-Fix bleibt enthalten.

## Test

1. Frontend starten.
2. Tab „2FA registrieren“ öffnen.
3. Benutzername eingeben.
4. QR-Code erzeugen.
5. QR-Code mit Authenticator scannen.
6. 6-stelligen Code bestätigen.
7. Danach prüfen:

```sql
SELECT username, secretkey
FROM accounts
WHERE username = '<benutzername>';
```

