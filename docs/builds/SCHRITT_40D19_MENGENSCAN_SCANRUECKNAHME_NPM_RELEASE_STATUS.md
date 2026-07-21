# Schritt 40d19 – Mengenscan, Scan-Rücknahme und Release-Bereinigung

## Marketingworkflow
- Mengenscan mit frei wählbarer Stückzahl
- Standardmenge 1
- Mengenbuchung in einem Durchlauf
- Mengenrücknahme mit negativer Buchung
- Letzten Scan vollständig zurücknehmen
- Historie mit Mengenänderung, Barcode, Benutzer und Zeitpunkt
- Doppelscan-Schutz des Kamera-Scanners bleibt erhalten

## Frontend-Paketquelle
- package-lock.json verweist ausschließlich auf https://registry.npmjs.org/
- lokale .npmrc mit öffentlicher npm-Registry und Wiederholungslogik
- keine interne OpenAI-/CAAS-Artifactory-Adresse im Release

## Release-Bereinigung
- kein _work-Ordner
- kein node_modules
- keine temporären Buildordner
- keine .bak-Dateien
- anonymisierte Beispieldatenbank nicht im Hauptpaket
- Beispieldatenbank als separates GitHub-Release-Asset vorgesehen

## Build
Der direkte Vite-Produktionsbuild wurde erfolgreich ausgeführt. Der kombinierte TypeScript-Schritt meldet weiterhin bereits vorhandene Altfehler außerhalb des Marketingmoduls.
