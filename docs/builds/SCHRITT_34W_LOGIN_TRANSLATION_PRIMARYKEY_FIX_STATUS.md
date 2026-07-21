# Schritt 34w – Login/Translation Primary-Key-Fix

Ziel: Login wieder schnell und sauber machen.

Änderungen:
- Keine festen ID-INSERTs mehr in Runtime-Übersetzungstabellen.
- `ID` ist wieder rein technisch; fachlich maßgeblich ist der Translation-Key (`SPRACHE.key`).
- Vorhandene Übersetzungen werden per Key aktualisiert statt erneut angelegt.
- `translateUi(...)` synchronisiert, damit parallele Sprach-/Login-Requests nicht gleichzeitig denselben Key anlegen.
- Teure Duplikatbereinigung wird nicht mehr bei jedem Request ausgeführt.
- `AsyncRequestNotUsableException` wird im normalen Backendfenster nicht mehr als Warnspam angezeigt.

Beibehalten:
- Browser-TTS bleibt wie in 34k.
- `translation_german` bleibt Quell-/Referenzkatalog.
- Tabellen bleiben `translation_italian`, `translation_swedish`, `translation_turkish`, `translation_russian` usw.

Hinweis:
- Bestehende Duplikate aus 34v können in der DB noch vorhanden sein. Neue Requests sollten aber keine Primary-Key-Duplikate mehr erzeugen.
