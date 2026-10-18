# 40k36q27 – Starter-Konsolidierung

Vor dem Entfernen wurden die Unterstrich- und Bindestrichvarianten tatsächlich verglichen.

## Ergebnis des Vergleichs
Die `_`-Varianten waren zuletzt nur Weiterleitungen auf die `-`-Varianten. Die vollständige Startlogik lag deshalb in den Bindestrichdateien. Diese Logik wurde vor dem Löschen vollständig in die historischen `_`-Starter übernommen.

## Einzige offizielle Starter ab q27
- `start_backend.bat`
- `start_frontend.bat`
- `start_backend.sh`
- `start_frontend.sh`

Die Dateien `start-backend.*` und `start-frontend.*` wurden entfernt.

## Funktionsumfang erhalten
- Backend `.sh`: Java 21, `.env`, MariaDB/GAM-Datenbank, Build und Spring-Boot-Start.
- Frontend `.sh`: automatische Node.js/npm-Einrichtung, `npm install`, Vite-Start.
- Backend `.bat`: Java 21, MariaDB/GAM-Datenbank, Piper-Prüfung/Installation, Build und Spring-Boot-Start.
- Frontend `.bat`: jetzt ebenfalls automatische Node.js/npm-Einrichtung über `scripts/ensure-node.ps1`, danach `npm install` und Frontend-Start.

## Projektweite Bereinigung
Alle Text-/Skript-/Dokumentationsverweise wurden von den Bindestrichnamen auf die Unterstrichnamen umgestellt. Eine abschließende Suche ergab keine verbliebenen Verweise auf `start-backend.*` oder `start-frontend.*`.
