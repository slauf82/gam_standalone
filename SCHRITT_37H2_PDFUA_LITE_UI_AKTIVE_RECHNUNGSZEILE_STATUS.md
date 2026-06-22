# Schritt 37h2 – PDF/UA-Lite bleibt stabil + aktive Rechnungszeile in Suche

Basis:
- Schritt 37h1

Ziele:
- funktionierenden OpenHTMLtoPDF-Pfad aus 37h1 unverändert stabil halten
- keine riskanten Tabellen-/HTML-Strukturänderungen für PDF/UA in diesem Schritt
- UI-Verbesserung in der Rechnungssuche ergänzen

Umgesetzt:
- ausgewählte Rechnungszeile in der Suchliste wird sofort beim Klick markiert
- aktive Zeile erhält sichtbare farbliche Hervorhebung
- Hover- und Tastaturfokus-Zustand ergänzt
- ausgewählte Zeile erhält zusätzlich ein kleines Label „Ausgewählt“
- ARIA-current für die aktive Ergebniszeile ergänzt

Nicht geändert:
- Button 1 OpenPDF + ZUGFeRD
- Button 2 OpenHTMLtoPDF + PDF/A-3 + ZUGFeRD + 37h1-Metadaten
- keine LBD-/Security-Änderungen
- keine erneuten Tabellenstruktur-Experimente
