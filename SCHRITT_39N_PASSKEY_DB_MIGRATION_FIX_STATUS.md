# Schritt 39n – Passkey DB Migration Fix / GitHub v2.0.2

## Ziel

Schritt 39n behebt den nach Schritt 39m noch verbliebenen Passkey-Fehler in Bestandsdatenbanken.
Die Security-Freigabe aus 39m funktioniert, aber in migrierten Datenbanken konnte die Tabelle
`account_passkeys` fehlen. Dadurch scheiterte `/api/auth/passkey/login/options` mit SQL-Fehler 1146.

## Behoben

- Tabelle `account_passkeys` wird beim Backend-Start automatisch angelegt.
- Anlage ist idempotent über `CREATE TABLE IF NOT EXISTS`.
- fehlende Passkey-Spalten werden bei Bedarf ergänzt.
- Passkey-Abfragen prüfen die Tabelle vor dem Zugriff.
- Wenn Passkey-Schema wider Erwarten nicht verfügbar ist, bleibt klassisches Login/TOTP nutzbar.
- Passkey-Login liefert dann keine bekannten Credentials statt mit `SQLSyntaxErrorException` abzustürzen.

## Technische Details

Datei geändert:

- `backend/src/main/java/de/kopfzentrum/gam/auth/PasskeyRepository.java`

Neue/abgesicherte Tabelle:

- `account_passkeys`

Wichtige Felder:

- `id`
- `account_id`
- `credential_id`
- `public_key`
- `device_name`
- `sign_count`
- `active`
- `created_at`
- `last_used_at`

## Release-Zuordnung

- interner Entwicklungsschritt: `39n`
- GitHub-Release: `v2.0.2`
- Zweck: Patchrelease nach GAM 2.0.1 für Passkey-Persistenz/Migration

## Erwartetes Ergebnis

Nach dem Start sollte der bisherige Fehler nicht mehr auftreten:

```text
Table 'kopfzentruminventardb.account_passkeys' doesn't exist
```

Passkey-Registrierung und Passkey-Login können danach die Tabelle verwenden.
