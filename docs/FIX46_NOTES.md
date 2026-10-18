# FIX46 – Windows Frontend Start / node_modules

Basis: fix45.

- Windows `start_frontend.bat` nutzt keine `mklink /J`-Junction und verschiebt `node_modules` nicht mehr in einen externen Ordner. Auf gemappten Laufwerken wie `L:` kann das Erzeugen einer Junction mit Zugriff verweigert scheitern.
- `node_modules` liegt direkt in `frontend/` und bleibt beim Drueberkopieren erhalten.
- Eine Hash-Datei in `node_modules/` verhindert Neuinstallationen bei unveraenderter `package-lock.json`.
- Falls das vorherige Skript bereits eine Junction angelegt hat, wird nur diese Verknuepfung entfernt, nicht ihr Ziel.
- Fehlende Vite- und TypeScript-Dateien werden nach `npm ci` geprueft.
- Optionaler npm-Downloadcache befindet sich in `%LOCALAPPDATA%\GAM-Standalone\npm-cache`.

Validierung: ZIP-CRC und enthaltene Dateien geprueft. Windows-CMD-Lauf nicht in dieser Umgebung moeglich.

Die Meldung `3 high severity vulnerabilities` stammt von npm audit und ist getrennt vom Junction-Fehler zu behandeln.
