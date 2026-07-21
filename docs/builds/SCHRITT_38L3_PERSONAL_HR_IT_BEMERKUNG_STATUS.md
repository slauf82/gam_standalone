# Schritt 38l3 – Personaldaten HR-/IT-Bemerkung

## Enthalten

- Neue Spalte `BEMERKUNG_PERSONAL` direkt hinter `TELEFON2` vorbereitet.
- Bestehende Spalte `BEMERKUNG` bleibt erhalten und wird als IT-/Superadmin-Bemerkung geführt.
- HR-/Personalansicht endet bei `BEMERKUNG_PERSONAL`.
- Alle danach folgenden IT-/Admin-Spalten sind nur für Superadmin sichtbar.
- MasterData-Katalog `personnel` um `BEMERKUNG_PERSONAL` ergänzt.
- SQL-Migration: `sql/personal_bemerkung_personal_38l3.sql`.

## Sichtlogik

Personalabteilung sieht:

`NAME, VORNAME, STATUS, POSITION, FILIALE_ID, EMAIL, TELEFON, TELEFON2, BEMERKUNG_PERSONAL`

Superadmin sieht zusätzlich:

`DIENSTHANDY, VPN_TOKEN, DIENSTLAPTOP, BEMERKUNG`

## Hinweis

Die Datenbank bleibt eine gemeinsame Tabelle `personal`. Es entsteht keine zweite Personalverwaltung.
