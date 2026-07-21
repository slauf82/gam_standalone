# Schritt 22 – Passkey-Testworkflow

Diese Version ergänzt den Loginbereich nach Passwort und 2FA um einen ersten lokalen WebAuthn-/Passkey-Testworkflow.

## Enthalten

- TOTP dauerhaft auf SHA1 / 6 Stellen / 30 Sekunden gesetzt
- `allow-totp-only` standardmäßig aktiviert
- Passkey-Registrierung über Browser-WebAuthn (`navigator.credentials.create`)
- Passkey-Login über Browser-WebAuthn (`navigator.credentials.get`)
- Speicherung der Credential-ID in `account_passkeys`
- Neue Backend-Endpunkte:
  - `POST /api/auth/passkey/register/options`
  - `POST /api/auth/passkey/register/finish`
  - `POST /api/auth/passkey/login/options`
  - `POST /api/auth/passkey/login/finish`
- Bestehende Pflicht-Fixes bleiben enthalten:
  - `JwtService` ohne `padEnd`
  - LBD UTF-8
  - 2FA Secret per Username
  - Vite/OXC `??`/`||` Parserfix

## Wichtiger Hinweis

Der Passkey-Workflow ist ein lokaler Testworkflow. Er speichert und erkennt Credential-IDs und nutzt die Browser-WebAuthn-Dialoge. Für einen produktiven WebAuthn-Betrieb muss die serverseitige Prüfung der Assertion-Signatur und der Public-Key-Verifikation vollständig gehärtet werden.

## Testhinweise

Nach dem Einspielen:

```bat
mvnw.cmd clean package
mvnw.cmd spring-boot:run
```

Frontend ggf. aktualisieren:

```bat
cd frontend
npm install
npm run dev
```

Passkey-Test:

1. Tab `Passkey registrieren`
2. Benutzername eintragen
3. Passkey registrieren
4. Tab `Passkey-Login`
5. Benutzername eintragen
6. Mit Passkey anmelden
