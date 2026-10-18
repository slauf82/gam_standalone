# 40k36q28 – Demoimport idempotent und Fehleranzeige korrigiert

- Ursache aus macOS-Test: MariaDB 1068 `Multiple primary key defined`.
- Der Fehler trat beim erneuten Anwenden des additiven Runtime-Schemas auf einer bereits initialisierten Datenbank auf.
- Wiederholbare Strukturfehler werden beim Runtime-Schema jetzt gezielt ignoriert:
  - 1060: duplicate column
  - 1061: duplicate key/index
  - 1068: multiple primary key
- Alle anderen SQL-Fehler bleiben echte Abbrüche und enthalten Anweisungsnummer + MariaDB-Code.
- DemoDatabaseController erhält einen eigenen REST-Fehlerhandler; SQL-/Importfehler werden als HTTP 500 mit klarer JSON-Meldung zurückgegeben und nicht mit Berechtigungsfehlern vermischt.
- Die ADMIN/SUPERADMIN-Berechtigung bleibt unverändert.
