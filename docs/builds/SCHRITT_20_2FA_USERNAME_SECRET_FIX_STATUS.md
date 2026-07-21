# Schritt 20 – 2FA Username Secret Fix

Diese Version stabilisiert die 2FA-Registrierung.

## Geändert

- `accounts.secretkey` wird bei der 2FA-Registrierung nicht mehr über eine möglicherweise falsche Account-ID gespeichert.
- Die Speicherung erfolgt jetzt eindeutig über den Benutzernamen aus dem Registrierungsformular.
- Neue Repository-Methode: `updateSecretKeyByUsername(username, secretkey)`.
- Der Secretkey wird weiterhin erst nach erfolgreicher TOTP-Code-Bestätigung gespeichert.
- Der Vite/OXC-Fix in `frontend/src/api/client.ts` bleibt enthalten.
- Der `JwtService`-Fix ohne `String.padEnd()` bleibt enthalten.

## Erwarteter Testablauf

1. Passwort-Login funktioniert weiterhin.
2. In „2FA registrieren“ Benutzername und Passwort eingeben.
3. QR-Code scannen.
4. Code bestätigen.
5. Danach prüfen:

```sql
SELECT username, secretkey
FROM accounts
WHERE username = '<BENUTZERNAME>';
```

`secretkey` sollte anschließend nicht mehr `NULL` sein.

6. Danach 2FA-Login testen.
