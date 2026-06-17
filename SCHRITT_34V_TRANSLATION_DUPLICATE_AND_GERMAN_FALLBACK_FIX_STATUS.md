# Schritt 34v – Translation Duplicate & German Fallback Fix

Basis: Schritt 34u / stabile Browser-TTS-Basis aus 34k.

## Ziel

Die neuen Sprachen dürfen keine ständig neu angelegten Translation-Datensätze erzeugen.
Pro Sprache und Key soll genau ein Datensatz existieren. Bereits übersetzte Texte bleiben erhalten.

## Änderungen

- Key-Erkennung erweitert: `ITALIAN.key`, `SWEDISH.key`, `TURKISH.key`, `RUSSIAN.key` und `.UI.`-Zwischenformen werden alle auf denselben Basisschlüssel reduziert.
- Upsert-Logik sucht nicht mehr nur nach exakter `TRANSLATE_DESCRIPTION`, sondern nach normalisiertem Key.
- Vorhandene Übersetzungen werden aktualisiert statt erneut eingefügt.
- Doppelte Keys werden bereinigt; es bleibt jeweils der erste Datensatz erhalten.
- Deutsche Fehlfüllungen werden nicht mehr als gültige Zielübersetzung akzeptiert.
- `module.checks` / `module.compliance` erhalten manuelle Fallbacks, damit `Prüfungen` nicht unverändert in IT/SV/TR/RU stehen bleibt.
- Frontend übergibt bei Modulbuttons keine deutschen Fallbacktexte mehr als bekannte Zielübersetzung.

## Erwartung

Nach Sprachwechsel / Login:

- keine ständig steigenden IDs mehr für dieselben Keys
- `SPRACHE.loginDialogHeader` und `SPRACHE.loginTabLogin` bleiben unangetastet, wenn bereits übersetzt
- `SPRACHE.module.checks` wird nicht mehr als `Prüfungen` gespeichert
- neue Übersetzungen werden weiterhin bei Bedarf ergänzt

## Hinweis

Bestehende Duplikate aus älteren Zwischenständen werden beim nächsten Übersetzungslauf pro Zieltabelle bereinigt. Browser-TTS bleibt unverändert wie in Schritt 34k.
