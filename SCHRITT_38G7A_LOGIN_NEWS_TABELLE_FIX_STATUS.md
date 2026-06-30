# Schritt 38g7a – Login-News aus vorhandener GAM-1.0-News-Tabelle

## Ziel
Die News/Hinweise im Login sollen nicht nur aus einer neuen lokalen Konfiguration kommen, sondern die bereits vorhandene und im Adminbereich bearbeitbare Tabelle `news` aus GAM 1.0 verwenden.

## Umsetzung
- `/api/communication/login-news` liest zuerst die bestehende Tabelle `news`.
- Die letzte vorhandene News-Meldung wird im Login angezeigt.
- Die Spalte wird robust und case-insensitive ausgewertet (`NEWS`, `news`, `MELDUNG`, `TEXT`, `HINWEIS`).
- Nur wenn keine Datenbank-News gefunden wird, greift der Fallback aus `application-local.yml`.

## Ergebnis
Der Login-Newsbereich zeigt die vorhandene Administrationsmeldung an, sobald die Tabelle `news` Daten enthält.

## Build
Frontend unverändert. Backend-Maven-Build in dieser Umgebung nicht ausgeführt.
