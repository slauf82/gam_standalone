# Schritt 17 – Authentifizierung & Login-Modernisierung

Diese Version ergänzt den Loginbereich von GAM 2.0 um die aus dem alten GAM bekannte Trennung der Anmeldewege.

## Enthalten

- Login-Tabs im Frontend:
  - Passwort-Login
  - 2FA registrieren
  - 2FA-Login
  - Passkey registrieren
  - Passkey-Login
- 2FA-Registrierung mit TOTP-Secret und QR-Code für Authenticator-Apps
- 2FA-Bestätigung schreibt das Secret in `accounts.secretkey`
- 2FA-Login nutzt die bestehende Loginlogik
- Passkey-Tabs und Backend-Status-Endpunkt vorbereitet
- SQL-Skript für `account_passkeys`
- Automatischer Logout/Redirect bei abgelaufener oder ungültiger Session

## Wichtige Hinweise

- Passkeys/WebAuthn sind in dieser Version vorbereitet, aber noch nicht vollständig aktiv.
- Für echte Passkeys werden im nächsten Schritt WebAuthn-Challenges, Credential-Speicherung und Browser-API-Anbindung ergänzt.
- Die bestehende Passwort-Anmeldung bleibt unverändert kompatibel.

## SQL

Das SQL-Skript liegt unter:

`sql/account_passkeys.sql`

Die Tabelle verwendet `INT` für `account_id`, passend zur bestehenden Tabelle `accounts`.
