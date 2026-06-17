# Schritt 34y – Translation-Audit und Hintergrund-Angleichung

Ziel: IT/SV/TR/RU werden auf dieselbe Key-Basis wie die bestehenden Sprachen gebracht, ohne den Login/Modulstart erneut zu blockieren.

## Änderungen

- `translation_german` bleibt Master/Quelle.
- Fehlende Keys der aktuellen UI-Sprache werden nach dem Anzeigen der Oberfläche im Hintergrund nachgezogen.
- Bereits vorhandene gute Übersetzungen bleiben unverändert.
- Deutsche Fehlfüllungen gelten nicht mehr als gültige Übersetzung und werden neu übersetzt.
- Keine festen ID-Inserts mehr; IDs bleiben Datenbank-intern, um Primary-Key-Fehler zu vermeiden.
- Frontend holt den Übersetzungscache nach 3,5s und 12s nochmals ab, damit Hintergrundübersetzungen sichtbar werden, ohne den Login zu blockieren.
- Browser-TTS bleibt unverändert stabil wie in Schritt 34k/34x.

## Ergänzte Hilfen

- `sql/translation_audit_counts_34y.sql`
- `sql/cleanup_translation_duplicates_34y.sql`
- `sql/translation_audit_from_uploaded_sql_34y.csv`

## Erwartung

Nach Login/Sprachwechsel erscheint die Oberfläche schnell. Fehlende Übersetzungen können kurz deutsch erscheinen, werden aber im Hintergrund in `translation_italian`, `translation_swedish`, `translation_turkish` oder `translation_russian` nachgezogen und bei den späteren Cache-Refreshs übernommen.

## Nicht geändert

- Kein MaryTTS-Fix in diesem Schritt.
- Keine Änderung an Rechnungslogik.
- Keine erneute Login-Blockade durch Massenübersetzung.
