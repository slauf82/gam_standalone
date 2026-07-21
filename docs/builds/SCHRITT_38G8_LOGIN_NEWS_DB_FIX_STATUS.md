# Schritt 38g8 – Login-News aus GAM1.0-News-Tabelle

## Ziel
Der Login-Newsbereich aus 38g7 soll nicht nur statische Konfigurationswerte anzeigen, sondern die bestehende GAM1.0-Tabelle `news` nutzen.

## Behoben
- `/api/communication/login-news` liest jetzt zuerst den neuesten Eintrag aus `news`.
- Es wird der letzte nicht-leere `NEWS`-Text per `ORDER BY ID DESC LIMIT 1` geladen.
- Wenn kein Datenbankeintrag verfügbar ist, greift weiterhin der bisherige Fallback aus `application-local.yml`.
- Der Login-Dialog zeigt damit automatisch die letzte GAM1.0-Newsmeldung an.

## Geänderte Datei
- `backend/src/main/java/de/kopfzentrum/gam/communication/CommunicationController.java`

## Hinweise
- Keine Datenbankänderung.
- Keine Frontendänderung nötig, da `LoginNewsBox` den bestehenden Endpunkt bereits nutzt.
- Backend-Maven-Build konnte in dieser Umgebung nicht ausgeführt werden.
