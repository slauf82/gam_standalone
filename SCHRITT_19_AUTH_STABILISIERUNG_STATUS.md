# Schritt 19 – Auth-Stabilisierung

Diese Version stabilisiert die in Schritt 18 eingeführten Authentifizierungsfunktionen.

## Enthaltene Korrekturen

- Vite/OXC Parser-Fehler in `frontend/src/api/client.ts` behoben
  - `??` und `||` werden nicht mehr ungeklammert gemischt
  - `retryAfterSeconds` wird nun über eine separate Variable berechnet
- 2FA-Registrierungsablauf mit temporärem Secret bleibt enthalten
  - `/api/auth/totp/setup` erzeugt ein Secret und eine otpauth-URL
  - `/api/auth/totp/confirm` prüft den Code gegen das übergebene temporäre Secret
  - `accounts.secretkey` wird erst nach erfolgreicher Bestätigung gespeichert
- 2FA-Login bleibt über bestehende GAM-Logik angebunden
- Login-Countdown bei Rate-Limit bleibt enthalten
- Auto-Logout/Redirect bei abgelaufener Session bleibt enthalten

## Testfokus

1. Frontend starten und prüfen, dass der Vite-Parserfehler nicht mehr auftritt.
2. Passwort-Login testen.
3. 2FA-Registrierung testen:
   - QR-Code anzeigen
   - Code im Authenticator erzeugen
   - Code bestätigen
   - Prüfen, ob `accounts.secretkey` gesetzt wird
4. 2FA-Login mit dem gespeicherten Secret testen.
5. Rate-Limit/Countdown testen.

