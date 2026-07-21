# Schritt 40d19 – Mengenscan, Scan-Rücknahme und npm-/Release-Fix

- Mengenscan mit frei wählbarer Stückzahl
- Teilrücknahme mit negativer Menge
- Rücknahme des letzten noch nicht rückgängig gemachten Scans
- vollständige Historie mit Mengenänderung, Barcode, Benutzer und Zeitpunkt
- Barcodeerkennung trägt den Code zunächst ein; Menge wird anschließend bestätigt
- npm-Registry auf https://registry.npmjs.org/ korrigiert
- interne OpenAI-Artifactory-URLs aus package-lock.json entfernt
- .npmrc mit Retry-Einstellungen ergänzt
- großes anonymisiertes SQL-Dump nicht mehr im Release-ZIP enthalten
- keine _work-, node_modules-, dist- oder target-Verzeichnisse im Quellpaket
