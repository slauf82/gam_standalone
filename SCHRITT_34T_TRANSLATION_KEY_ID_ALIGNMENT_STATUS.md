# Schritt 34t – Translation-Key-/ID-Angleichung

## Ziel
Die neuen UI-Sprachen sollen nicht nur ungefähr übersetzt werden, sondern dieselbe saubere Referenzstruktur wie die bestehenden Sprachen verwenden.

Referenzbestand:

- `translation_german`
- `translation_english`
- `translation_french`
- `translation_ukrainian`

Neue Zielsprachen:

- `translation_italian`
- `translation_swedish`
- `translation_turkish`
- `translation_russian`

## Änderungen

- `translation_german` wird als führender Key-/ID-Katalog genutzt.
- Für jede deutsche Referenzzeile wird in der Zielsprache derselbe Key erzeugt.
- Wenn möglich wird dieselbe `ID` wie in `translation_german` verwendet.
- Bestehende Zielzeilen werden aktualisiert statt unnötig doppelt angelegt.
- Alte `LANGUAGE.UI.key`-Zwischenstände werden weiterhin als Lesefallback akzeptiert, aber in `LANGUAGE.key` normalisiert.
- Zusätzliche Runtime-Texte aus dem LoginDialog können weiterhin gespeichert werden, falls sie noch nicht im deutschen Referenzkatalog enthalten sind.

## Erwartung
Nach Auswahl einer neuen Sprache sollten die Tabellen der neuen Sprachen schrittweise auf dieselbe Key-Menge wie die Referenzsprachen anwachsen.

Beispielprüfung:

```sql
select id, translate_description from translation_german order by id;
select id, translate_description from translation_italian order by id;
select id, translate_description from translation_swedish order by id;
select id, translate_description from translation_turkish order by id;
select id, translate_description from translation_russian order by id;
```

Die IDs sollten sich an `translation_german` orientieren.

## Hinweis
Der Backend-Compile konnte in der Paketumgebung nicht ausgeführt werden, weil `mvn` dort nicht installiert ist. Die Änderung ist auf Java-Quellcodeebene umgesetzt.
