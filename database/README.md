# Datenbanken für GAM 2.1.0 Preview 1

## `gam_v2_1_0_preview1_empty.sql`

Leere Datenbankstruktur ohne fachliche Beispieldaten. Sie eignet sich für einen frischen Testaufbau. Beim ersten Start ergänzt GAM 2.1.0 Preview 1 weiterhin automatisch fehlende technische Tabellen und Standardwerte.

## `gam_demo_v2_1_0_preview1_anonymisiert.sql`

Anonymisierte, befüllte Demonstrationsdatenbank. Sie enthält den bisherigen Demo-Datenbestand sowie zusätzliche Beispieldatensätze für:

- Laborworkflow
- Wartezimmer und Patientenfluss

Die enthaltenen Namen, Kontaktinformationen und Vorgänge sind Demonstrationsdaten und nicht für den Produktivbetrieb bestimmt.

## Import

Beispiel mit MariaDB:

```bash
mariadb -u root -p gam2 < database/gam_v2_1_0_preview1_empty.sql
```

oder:

```bash
mariadb -u root -p gam2 < database/gam_demo_v2_1_0_preview1_anonymisiert.sql
```

Vor einem Import in eine vorhandene Datenbank sollte eine Sicherung erstellt werden.
