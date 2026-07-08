# Schritt 39l – Rechnungsadministration Migration Hardening

## Ziel

Schritt 39l ist ein reiner Release-Härtungsfix auf Basis von 39k vor GAM 2.0.0.

Im Backend-Log wurden wiederholte MariaDB-Warnungen gemeldet:

```text
Error: 1060-42S21: Duplicate column name 'NAME'
```

Ursache war eine nicht vollständig idempotente Runtime-Migration rund um die Logo-Normalisierung.

## Änderungen

- `rechnungslogo.NAME` wird vor `ALTER TABLE` über `information_schema.COLUMNS` geprüft.
- `rechnungsgesellschaft.LOGO_ID` wird vor `ALTER TABLE` über `information_schema.COLUMNS` geprüft.
- Die Prüfung wurde an beiden Stellen ergänzt:
  - `InvoiceRepository`
  - `MasterDataAdminController`
- Die SQL-Migrationsdatei für 39l nutzt weiterhin `ADD COLUMN IF NOT EXISTS`.
- ID-0-Fallbacks und Logo-Normalisierung bleiben unverändert erhalten.

## Erwartetes Verhalten

- Erster Start: fehlende Spalten werden angelegt.
- Weitere Starts: vorhandene Spalten werden erkannt und nicht erneut angelegt.
- Keine `Duplicate column name 'NAME'`-Warnungen mehr beim erneuten Öffnen der Rechnungsadministration.

## Betroffene Dateien

- `backend/src/main/java/de/kopfzentrum/gam/invoice/InvoiceRepository.java`
- `backend/src/main/java/de/kopfzentrum/gam/masterdata/MasterDataAdminController.java`
- `sql/rechnungsadmin_migration_hardening_39l.sql`

## Fachlicher Stand

Keine fachliche Änderung gegenüber 39k.

Die Rechnungsadministration bleibt:

- normalisiert für Texte
- normalisiert für Logos
- mit ID-0-Fallbacks
- mit Logo-Wiederverwendung
- mit Löschschutz für verwendete Logos

## Release-Einschätzung

Dieser Stand ist als finaler Release-Candidate für GAM 2.0.0 gedacht.
