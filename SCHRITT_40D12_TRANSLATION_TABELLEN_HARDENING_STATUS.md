# Schritt 40d12 – Translation-Tabellen-Härtung

## Behoben

- Alle unterstützten Übersetzungstabellen werden beim Backendstart idempotent mit `CREATE TABLE IF NOT EXISTS` initialisiert.
- Tabellenprüfung erfolgt über `information_schema.TABLES`; fehlende Tabellen werden nicht mehr absichtlich per fehlschlagendem SELECT abgefragt.
- Dadurch entstehen keine wiederholten MariaDB-Warnungen `1146-42S02 Table ... doesn't exist` mehr.
- Der Rechnungs-/Dokumentübersetzungsdienst verwendet dieselbe Absicherung wie der UI-Übersetzungsdienst.
- Spanisch, Portugiesisch, Niederländisch, Polnisch und Tschechisch sind auch im Rechnungsübersetzungsdienst als eigene Tabellenziele registriert.
- Alle Übersetzungstabellen sind in der Administration unter Mehrsprachigkeit als Kataloge verfügbar.

## Vollständig geprüfter Tabellenkatalog

- translation_german
- translation_english
- translation_french
- translation_ukrainian
- translation_italian
- translation_swedish
- translation_turkish
- translation_russian
- translation_spanish
- translation_portuguese
- translation_dutch
- translation_polish
- translation_czech

## Test

1. Backend starten.
2. Jede Sprache einmal im Login, in der Oberfläche und im Patientenportal auswählen.
3. Im Backendlog dürfen keine Meldungen `Table ... translation_* doesn't exist` erscheinen.
4. Administration → Mehrsprachigkeit öffnen und prüfen, ob alle Sprachkataloge auswählbar sind.
5. Backend erneut starten; die Tabellenanlage muss ohne Duplicate-/Already-exists-Warnungen durchlaufen.

## Buildhinweis

Ein Maven-Build konnte in der Arbeitsumgebung nicht ausgeführt werden, weil weder Maven noch ein Maven-Wrapper vorhanden war. Die geänderten Dateien wurden statisch auf Imports, Methodendopplungen, Tabellenlisten und SQL-Strukturen geprüft.
