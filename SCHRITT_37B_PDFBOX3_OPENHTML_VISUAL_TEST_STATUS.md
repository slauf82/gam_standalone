# Schritt 37B – Dual-PDF ohne ZUGFeRD-Konflikt im Testpfad

Basis:
- Schritt 37A

Ziel:
- Button 1 bleibt produktiver OpenPDF/ZUGFeRD-Pfad.
- Button 2 bleibt separater OpenHTMLtoPDF-Testpfad.
- Button 2 bettet in dieser Stufe bewusst noch kein ZUGFeRD/Factur-X-XML ein.
- PDFBox-Konflikte zwischen MustangProject und OpenHTMLtoPDF sollen durch einen einheitlichen PDFBox-3.x-Stack vermieden werden.

Änderungen:
- `pom.xml` auf PDFBox 3.0.7 gesetzt.
- `pdfbox-io` ergänzt.
- OpenHTMLtoPDF-Abhängigkeit von `com.openhtmltopdf:openhtmltopdf-pdfbox:1.0.10` auf `io.github.openhtmltopdf:openhtmltopdf-pdfbox:1.1.37` geändert, da diese Variante PDFBox 3 unterstützt.
- OpenHTML-Test-PDF erzeugt vorerst nur die visuelle PDF, ohne ZUGFeRD-Einbettung.
- HTML-Ausgabe für OpenHTMLtoPDF als XHTML mit `xmlns="http://www.w3.org/1999/xhtml"` angepasst.

Nicht geändert:
- Keine Security-Änderung.
- Keine LBD-Änderung.
- Keine PDF/UA-/WCAG-Optimierung.

Testreihenfolge:
1. Backend neu bauen.
2. Login prüfen.
3. Button 1: ZUGFeRD-PDF (OpenPDF) öffnen.
4. Button 2: PDF/UA-Test-PDF (OpenHTMLtoPDF, noch ohne ZUGFeRD) öffnen.
5. Erst danach PDF/UA-/WCAG-Optimierungen beginnen.

Hinweis:
- Build konnte in dieser Umgebung nicht ausgeführt werden, weil Maven hier nicht installiert ist.
