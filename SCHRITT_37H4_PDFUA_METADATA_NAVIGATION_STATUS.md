# Schritt 37h4 – PDF/UA Metadaten und Navigation

Basis: Schritt 37h3.

Ziel dieses Schritts ist PAC-Feinschliff, ohne den stabilen OpenHTMLtoPDF-Rendererpfad oder die Tabellenstruktur wieder zu riskieren.

## Enthalten

- Button 1 bleibt unverändert.
- Button 2 bleibt OpenHTMLtoPDF + PDF/A-3 + ZUGFeRD/Factur-X.
- Keine riskanten Tabellen- oder Layout-Umbauten.
- Nach der ZUGFeRD-Einbettung werden erneut gesetzt:
  - Dokumenttitel
  - Sprache
  - DisplayDocTitle
  - MarkInfo
  - PDF/UA-XMP-Metadaten mit `pdfuaid:part=1`
  - PDF/A-3-U-XMP-Basisdaten
- Ein einfacher PDF-Outline-Eintrag wird erzeugt, damit PAC/WCAG 2.4 Navigable besser erfüllt werden kann.

## Erwarteter Test

1. Button 2 muss weiterhin ein PDF öffnen.
2. Die eingebettete ZUGFeRD/Factur-X-XML muss weiterhin vorhanden sein.
3. PAC erneut prüfen:
   - PDF/UA Metadata sollte verbessert sein.
   - WCAG Navigable sollte verbessert sein.
   - Structure-Tree-Warnungen können noch vorhanden sein und werden im nächsten Schritt separat behandelt.

## Nicht geändert

- Keine Security-Änderung.
- Keine LBD-Änderung.
- Kein Umbau der HTML-Tabellenstruktur.
- Keine Änderung am produktiven Button 1.
