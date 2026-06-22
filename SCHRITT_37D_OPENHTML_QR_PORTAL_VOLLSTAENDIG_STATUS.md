# Schritt 37D – OpenHTMLtoPDF vollständiges visuelles PDF mit QR-Code

Basis: Schritt 37C.

Ziel:
- Button 1 bleibt unverändert: OpenPDF + ZUGFeRD/Factur-X.
- Button 2 bleibt OpenHTMLtoPDF-Testexport ohne ZUGFeRD.
- Button 2 enthält nun zusätzlich QR-Code und Portal-Link.
- Keine PDF/A-3-Umstellung.
- Keine ZUGFeRD-Einbettung im OpenHTMLtoPDF-Pfad.
- Keine Security- oder LBD-Änderungen.

Änderungen:
- InvoiceOpenHtmlPdfService erhält Zugriff auf InvoiceAccessTokenRepository, QrCodeService und Portal-Basis-URL.
- QR-Code wird als Base64-data-URI direkt in das XHTML eingebettet.
- Portal-Hinweis und Portal-URL werden im OpenHTMLtoPDF-PDF ausgegeben.

Test:
1. Button „ZUGFeRD-PDF (OpenPDF)“ muss weiterhin funktionieren.
2. Button „ZUGFeRD-PDF (PDF/UA Test)“ muss weiterhin funktionieren.
3. Button 2 muss QR-Code und Portal-Link anzeigen.
4. Es darf weiterhin kein ZUGFeRD in Button 2 eingebettet sein.
