# Schritt 40d18 – ZXing-Barcode-Fallback und sauberes Release-ZIP

## Umgesetzt

- Native Browser-API `BarcodeDetector` bleibt bevorzugter Scanner.
- Fehlt die native API, startet automatisch `@zxing/browser`.
- Unterstützt werden gängige 1D- und 2D-Barcodes über ZXing MultiFormatReader.
- Kameraauswahl, Livebild, manuelle Eingabe und USB-Scanner bleiben erhalten.
- Nach erfolgreicher Erkennung wird der Barcode wie bisher an den Marketing-Beep-Workflow übergeben.
- Doppelerkennungen werden zeitlich entprellt.
- Beim Stoppen oder Wechseln werden ZXing-Scanner und Kamera-Stream sauber beendet.
- Die Oberfläche zeigt den aktiven Modus an: native Erkennung oder ZXing-Software-Scanner.

## Abhängigkeiten

- `@zxing/browser` 0.1.5
- `@zxing/library` 0.21.3

Die Versionen wurden bewusst kompatibel zu üblichen Node-Versionen vor Node 24 gewählt.

## Buildprüfung

- Vite-Produktionsbuild erfolgreich.
- Der vollständige TypeScript-Lauf meldet weiterhin bereits vorhandene Altfehler außerhalb des Marketing-Scanners.
- Keine neuen JSX-/Parsefehler durch 40d18.

## Release-Bereinigung

Das ZIP enthält nur die eigentliche Projektstruktur. Nicht enthalten sind:

- `_work`-Verzeichnisse
- `node_modules`
- temporäre Buildordner
- lokale Cache- und Logdateien
