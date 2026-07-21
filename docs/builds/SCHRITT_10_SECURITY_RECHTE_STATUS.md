# Schritt 10 – Rechte/Sicherheit gehärtet

Stand: GAM 2.0 Arbeitsversion – Security-/Rollen-Härtung.

## Ziel

Die bestehende Login-Logik aus der `accounts`-Tabelle bleibt kompatibel erhalten, aber die neue GAM-2.0-API wird jetzt serverseitig stärker abgesichert.

## Neu umgesetzt

- Serverseitige Rollenregeln pro API-Bereich:
  - `/api/admin/**` nur Admin/Administrator/Superadmin
  - `/api/invoices/**` nur Admin/Rechnung
  - `/api/inventory/**` nur Admin/Inventar
  - `/api/warehouse/**` nur Admin/Lager
  - `/api/inventory-warehouse/**` für Admin/Inventar/Lager
  - `/api/gam/personnel/**` für Admin/Personal
  - `/api/gam/cashbook/**` für Admin
- Method-Security vorbereitet (`@EnableMethodSecurity`)
- JWT-Filter protokolliert abgewiesene Tokens
- Login-Audit-Logging für erfolgreiche/fehlgeschlagene Logins
- In-Memory Login-Rate-Limiter als erste Brute-Force-Bremse
- Einheitliche JSON-Fehlerantworten für 403/400/ResponseStatusException
- Security-Status-API: `/api/security/status`
- CORS um PATCH erweitert

## Bewusst unverändert

- `accounts`-Tabelle bleibt unverändert.
- Passwort-/2FA-Kompatibilität bleibt erhalten.
- `secretkey` bleibt die Basis für bestehende TOTP-Logik.
- Passkeys/WebAuthn werden weiterhin nur vorbereitet, noch nicht aktiviert.

## Konfiguration

Neue optionale Umgebungsvariablen:

```env
GAM_LOGIN_MAX_FAILURES=8
GAM_LOGIN_LOCK_MINUTES=10
```

## Noch offen vor produktivem Einsatz

- Rate-Limiter bei Mehrinstanzbetrieb in DB/Redis auslagern
- optional Audit-Tabelle statt nur Logdatei
- Rechte ggf. später feiner als Rollenmodell abbilden
- Passkey/WebAuthn als spätere Erweiterung
- Build/Compile mit lokaler Maven-/Gradle-Umgebung prüfen
