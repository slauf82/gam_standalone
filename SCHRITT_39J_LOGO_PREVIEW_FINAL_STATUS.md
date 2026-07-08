# GAM 2.0 – Schritt 39j: Logo Preview Finalisierung

## Ziel

39j finalisiert die Rechnungsadministration im Bereich Logoverwaltung. Logos sollen nicht mehr nur als Pfad/Dateiname sichtbar sein, sondern direkt in der Administration als Bildvorschau angezeigt werden.

## Änderungen gegenüber 39i

### Logo-Upload

- Upload bleibt in der Rubrik **Logos**.
- Nach dem Upload wird der erzeugte Bildpfad direkt in das Feld übernommen.
- Die Vorschau wird unmittelbar unter dem Upload-Feld angezeigt.
- Unterstützte Formate bleiben PNG, JPG/JPEG, GIF und WEBP.

### Speicherung für Backend- und Frontend-Betrieb

Der Upload speichert die Datei nach Möglichkeit in mehrere erreichbare Upload-Verzeichnisse:

- `backend/src/main/resources/static/images/uploads`
- `src/main/resources/static/images/uploads`
- `frontend/public/images/uploads`
- `../frontend/public/images/uploads`

Dadurch funktioniert die Vorschau sowohl im Spring-Boot-Auslieferungsmodus als auch im lokalen Frontend-Entwicklungsmodus zuverlässiger.

### Logoübersicht

- In der Logotabelle wird neben dem Pfad ein kleines Thumbnail angezeigt.
- Defekte oder nicht erreichbare Bilder werden ausgeblendet, damit die Tabelle nicht kaputt wirkt.

### Gesellschafts-Logoauswahl

- In der Gesellschaftsverwaltung bleibt `LOGO_ID` als interne ID erhalten.
- Der Anwender wählt weiterhin den Logodatensatz aus einer Dropdown-Liste.
- Direkt unter der Dropdown-Liste wird das ausgewählte Logo angezeigt.
- In der Gesellschaftstabelle wird das zugeordnete Logo ebenfalls als kleines Thumbnail angezeigt.

### Fallback-Verhalten

- Wenn keine Logoauswahl vorhanden ist, bleibt der bestehende Standard-/Fallback-Pfad aktiv.
- Die Rechnungserstellung wird durch fehlende oder defekte Logos nicht blockiert.

## Fachlicher Stand

Mit 39j ist die Rechnungsadministration aus GAM-1.0-Sicht vollständig abgerundet:

- Gesellschaften
- Filialen / Gesellschaft-Filiale-Zuordnung
- Produkte, Preise, MwSt, Gültigkeiten
- getrennte Textbausteine für Anreden, Rechnungstexte, rechtliche Hinweise und Grußformeln
- normalisierte Textzuordnung je Gesellschaft
- ID-0-Fallback für Textbausteine
- Logoverwaltung
- Logoauswahl je Gesellschaft
- direkte Logo-Vorschau

## Build-Hinweis

In der ChatGPT-Ausführungsumgebung konnte kein vollständiger Maven-/Frontend-Build durchgeführt werden, weil Abhängigkeiten aus dem Internet nachgeladen werden müssten. Die Änderungen wurden direkt im bestehenden Projektstand vorgenommen.
