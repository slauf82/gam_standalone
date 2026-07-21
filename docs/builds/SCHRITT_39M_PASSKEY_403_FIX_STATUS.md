# Schritt 39m – Passkey HTTP-403 Fix

## Ziel
Behebung der Regression, dass Passkey/WebAuthn-Aufrufe nach Schritt 39l mit HTTP 403 fehlschlagen können.

## Änderungen

### SecurityConfig
- `/api/auth/passkey/**` wird vollständig ohne JWT freigegeben.
- `OPTIONS /**` wird freigegeben, damit Browser-Preflight-Anfragen nicht mit 403 blockiert werden.
- CORS wurde für lokale Entwicklungs-/Standalone-Ports robuster gemacht:
  - `http://localhost:*`
  - `http://127.0.0.1:*`
  - entsprechende HTTPS-Varianten
- `AllowedHeaders` auf `*` erweitert.

### AuthController
- Passkey-RP-ID ist nicht mehr hart auf `localhost` gesetzt.
- RP-ID wird aus `Host` bzw. `X-Forwarded-Host` abgeleitet.
- Dadurch funktionieren lokale Aufrufe über `localhost` und `127.0.0.1` konsistenter.

### Frontend API Client
- Öffentliche Auth-Endpunkte senden keinen eventuell alten JWT-Token mehr mit:
  - `/auth/login`
  - `/auth/totp/**`
  - `/auth/passkey/**`
- Dadurch kann ein abgelaufener/ungültiger Token den Passkey-Testworkflow nicht mehr stören.

## Fachliche Änderung
Keine fachliche Änderung gegenüber 39l.

## Erwartetes Ergebnis
- Passkey-Status, Registrierung und Login liefern keine HTTP-403-Antwort mehr durch Security/CORS.
- Bestehende Rechnungsadministration aus 39k/39l bleibt unverändert.
