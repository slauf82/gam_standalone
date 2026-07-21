# Schritt 36h – Backend Auto-DB-Setup

Version: **GAM 2.0 v1.7.2**

## Ziel

`start-backend.bat` soll einen neuen Test-PC möglichst selbständig vorbereiten, ohne vorhandene Systeme zu gefährden.

## Enthalten

- Prüfung auf MariaDB/MySQL unter `localhost:3306`
- vorhandene MariaDB/MySQL-Installationen werden respektiert
- keine vorhandene Datenbank wird überschrieben
- optionales portables MariaDB-Setup unter `tools/mariadb`
- Import der anonymisierten Demo-Datenbank nur, wenn die Ziel-Datenbank fehlt oder leer ist
- Integration in `start-backend.bat`
- `start-frontend.bat` bleibt frei von Datenbanklogik

## Sicherheitsregeln

- keine automatische Deinstallation oder Ersetzung vorhandener MariaDB/MySQL-Installationen
- kein Import in eine bereits befüllte Datenbank
- keine Änderung an fremden Datenbanken
- bei Fehlern startet das Backend trotzdem weiter, damit Diagnose möglich bleibt

## Standarddaten

- Datenbank: `kopfzentruminventardb`
- Host: `localhost`
- Port: `3306`
- Benutzer: `root`
- Passwort: leer, sofern nicht in `.env` geändert
- Demo-SQL: `sql/beispiel_v1_7_0_anonymisiert.sql`

## Konfiguration

In `.env` können gesetzt werden:

```properties
GAM_DB_AUTO_SETUP=true
GAM_DB_NAME=kopfzentruminventardb
GAM_DB_PORT=3306
GAM_DB_USER=root
GAM_DB_PASSWORD=
GAM_DEMO_SQL=sql\beispiel_v1_7_0_anonymisiert.sql
GAM_MARIADB_ZIP_URL=
```

Wenn `GAM_DB_AUTO_SETUP` leer bleibt, fragt `start-backend.bat` beim ersten fehlenden DB-Server nach.

## Testplan

1. PC ohne MariaDB starten
2. `start-backend.bat` ausführen
3. portable MariaDB-Einrichtung bestätigen
4. prüfen, ob `tools/mariadb` angelegt wurde
5. prüfen, ob `kopfzentruminventardb` importiert wurde
6. Backend startet
7. `start-frontend.bat` starten
8. Login mit Demo-Benutzer testen
9. vorhandene befüllte DB testen: kein Überschreiben

## Hinweis

Der automatische Download nutzt bevorzugt ein MariaDB-Windows-ZIP. Falls MariaDB die Download-URL ändert, kann `GAM_MARIADB_ZIP_URL` in `.env` auf eine aktuelle ZIP-Datei gesetzt werden.
