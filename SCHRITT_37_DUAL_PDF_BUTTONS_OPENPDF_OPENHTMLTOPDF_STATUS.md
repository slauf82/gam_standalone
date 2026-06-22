# Schritt 37 – Dualer PDF-Export

Basis:
- Schritt 36w / v1.7.4 stabil
- produktiver OpenPDF-/ZUGFeRD-Pfad bleibt unverändert

Ziel dieses ersten Schritt-37-Pakets:
- Zwei PDF-Buttons im Rechnungsdetail
- Button 1: ZUGFeRD-PDF (OpenPDF) – stabiler Produktivpfad
- Button 2: ZUGFeRD-PDF (PDF/UA Test) – separater OpenHTMLtoPDF-Testpfad
- XML-Download bleibt unverändert
- keine PDF/UA-/WCAG-Optimierungen in diesem Paket
- keine Security-Experimente außerhalb des zusätzlichen gültigen Direktlink-Matchers

Wichtig:
- Der OpenHTMLtoPDF-Pfad ist ausdrücklich ein Testexport.
- Der stabile OpenPDF-Pfad darf dadurch nicht ersetzt oder verändert werden.
- Erst wenn beide Buttons zuverlässig PDFs anzeigen, folgen PDF/UA- und WCAG-Optimierungen ausschließlich im Testpfad.

Neue technische Bestandteile:
- `InvoiceOpenHtmlPdfService`
- Endpoint `/api/invoices/{number}/pdf-openhtml`
- Frontend-Link `pdfOpenHtmlUrl(...)`
- zusätzlicher Button `ZUGFeRD-PDF (PDF/UA Test)`

Testreihenfolge:
1. Login prüfen
2. Rechnung öffnen
3. Button `ZUGFeRD-PDF (OpenPDF)` prüfen
4. Button `ZUGFeRD-PDF (PDF/UA Test)` prüfen
5. XML-Download prüfen
6. Erst danach PAC/WCAG-Optimierungen beginnen
