# Schritt 37G – OpenHTMLtoPDF + PDF/A-3 + ZUGFeRD/Factur-X

Basis: Schritt 37F.

Ziel:
- Button 1 bleibt unverändert: OpenPDF + ZUGFeRD/Factur-X.
- Button 2 erzeugt zuerst das OpenHTMLtoPDF-PDF/A-3-Basis-PDF.
- Anschließend wird die ZUGFeRD/Factur-X-XML über den bestehenden MustangProject-Pfad eingebettet.

Geändert:
- `/api/invoices/{number}/pdf-openhtml` nutzt nun `zugferdService.exportWithBasePdf(...)`.
- Button 2 liefert wieder `X-GAM-E-Invoice: ZUGFeRD/Factur-X`.
- Dateiname: `rechnung-<nummer>-openhtmltopdf-zugferd.pdf`.
- Der sichtbare Hinweis im PDF lautet wieder, dass die maschinenlesbare ZUGFeRD/Factur-X-XML im PDF enthalten ist.
- Keine Security-Änderungen.
- Keine LBD-Änderungen.
- Keine PDF/UA-/WCAG-Optimierungen in diesem Schritt.

Testziel:
1. Button 1 öffnet weiterhin die bekannte OpenPDF-ZUGFeRD-Rechnung.
2. Button 2 öffnet die OpenHTMLtoPDF-ZUGFeRD-Testrechnung.
3. XML ist in Button 2 eingebettet.
4. Layout bleibt wie in Schritt 37F.

Nächster Schritt nach erfolgreichem Test:
- Schritt 37H: PDF/UA- und WCAG-Optimierung nur für Button 2.
