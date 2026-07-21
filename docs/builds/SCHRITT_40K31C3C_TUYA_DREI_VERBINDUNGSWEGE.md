# GAM 2.1.0 – Schritt 40k31c3c

## Smart Life / Tuya: drei Verbindungswege

1. **Benutzername und Kennwort** (neuer Standardweg)
   - Smart Life oder Tuya Smart
   - Land/Vorwahl
   - E-Mail-Adresse oder Telefonnummer
   - Kennwort
   - automatische Ermittlung der Benutzer-UID
   - Geräteabruf über die angemeldete Benutzerzuordnung

2. **Home Assistant**
   - bestehende Home-Assistant-Verbindung auswählen
   - der bisherige fehleranfällige Namensfilter wurde entfernt
   - alle von der ausgewählten Quelle gelieferten geräteartigen Entitäten werden für die Tuya-Brücke berücksichtigt

3. **Direkte Tuya Cloud**
   - Access ID / Client ID
   - Access Secret
   - optionale Benutzer-UID
   - bleibt als Expertenmodus erhalten

## Löschen

Einzelne Konfigurationen und alle Tuya-Konfigurationen können weiterhin vollständig entfernt werden.

## Datenbankmigration

Die Tabelle `gam_tuya_sources` wird automatisch um folgende Felder ergänzt:

- `account_username`
- `account_password`
- `country_code`
- `app_schema`

## Technischer Hinweis

Die Kontoanmeldung verwendet Tuya `POST /v1.0/iot-01/associated-users/actions/authorized-login`. Für die Signierung benötigt GAM weiterhin eine einmalig hinterlegte Access ID und ein Access Secret des Tuya-Projekts. Diese Felder liegen im einfachen Modus eingeklappt unter „Technische Projektanbindung“.
