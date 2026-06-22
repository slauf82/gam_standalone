# Schritt 37a – PDFBox/XMPBox-Konflikt-Fix

Basis:
- Schritt 37 mit zwei PDF-Buttons

Ziel:
- Button 1: ZUGFeRD-PDF mit OpenPDF wieder anzeigen
- Button 2: ZUGFeRD-PDF-Test mit OpenHTMLtoPDF anzeigen
- keine Security-Aenderungen
- keine LBD-Aenderungen
- noch keine PDF/UA-/WCAG-Optimierung

Fehlerursache aus dem Backend-Log:
- `NoSuchMethodError: XMPMetadata.getPDFAIdentificationSchema()` bei MustangProject/ZUGFeRD
- `NoSuchFieldError: PDType1Font.COURIER_BOLD_OBLIQUE` bei OpenHTMLtoPDF

Bewertung:
- Die GUI zeigte 403/Not Authorized, aber der Backend-Log zeigte einen internen PDFBox/XMPBox-Versionskonflikt.
- Die PDF-Endpunkte wurden erreicht; die PDF-Erzeugung brach intern ab.

Aenderung:
- PDFBox, FontBox und XMPBox werden im `backend/pom.xml` fest auf denselben PDFBox-2.x-Stack gesetzt.
- Verwendete Version: `2.0.31`
- Damit sollen MustangProject 2.23.1 und OpenHTMLtoPDF 1.0.10 denselben kompatiblen PDFBox-Unterbau nutzen.

Nicht geaendert:
- SecurityConfig
- LBD-Erkennung
- produktiver OpenPDF-PDF-Code
- PDF/UA-/WCAG-Struktur

Testreihenfolge:
1. Backend neu bauen.
2. Frontend starten.
3. Login testen.
4. Button 1: ZUGFeRD-PDF (OpenPDF) testen.
5. Button 2: ZUGFeRD-PDF (PDF/UA Test / OpenHTMLtoPDF) testen.
6. Erst wenn beide PDFs angezeigt werden, Schritt 37b fuer PDF/UA/WCAG beginnen.
