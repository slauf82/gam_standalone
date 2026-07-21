# Schritt 37H3 – PDF/UA/WCAG: Renderer-Tagging ohne Tabellenumbau

Basis:
- Schritt 37H2
- stabiler OpenHTMLtoPDF-Pfad aus 37G/37H1
- UI-Verbesserung aus 37H2 bleibt erhalten

Ziel:
- PDF/UA/WCAG erneut angehen, ohne den in 37H verursachten Tabellenabsturz zu wiederholen
- OpenHTMLtoPDF soll selbst Tagged PDF erzeugen
- PDF/A-3 + ZUGFeRD/Factur-X bleiben erhalten

Änderungen:
- `PdfRendererBuilder.usePdfUaAccessbility(true)` für Button 2 aktiviert
- keine riskanten HTML-Tabellenumbauten
- Tabellenkopf nur minimal mit `scope="col"` ergänzt
- bestehende Metadaten aus 37H1 bleiben erhalten:
  - Titel
  - Sprache
  - DisplayDocTitle
  - MarkInfo
- Button 1 bleibt unverändert

Bewusst nicht geändert:
- keine neuen Security-Regeln
- keine LBD-Änderungen
- keine ZUGFeRD-Logikänderung
- keine Umstellung des produktiven OpenPDF-Pfads

Testziel:
1. Button 1 erzeugt weiterhin das stabile OpenPDF-ZUGFeRD-PDF.
2. Button 2 erzeugt weiterhin ein PDF.
3. Button 2 enthält weiterhin die ZUGFeRD/Factur-X-XML.
4. PAC erneut prüfen:
   - Tags sollten nicht mehr 0 sein
   - Structure-Tree sollte sich verbessern
   - WCAG 1.3 Adaptable sollte sich verbessern

Hinweis:
Falls `usePdfUaAccessbility(true)` in der verwendeten OpenHTMLtoPDF-Version anders benannt ist oder nicht verfügbar ist, zeigt der Maven-Build den Methodenfehler direkt an. Dann bleibt der nächste sichere Fix eine Versions-/API-Anpassung, nicht ein neuer Tabellenumbau.
