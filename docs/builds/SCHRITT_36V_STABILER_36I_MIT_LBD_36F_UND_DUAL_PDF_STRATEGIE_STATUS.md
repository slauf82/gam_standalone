# Schritt 36V – Stabiler 36I-Stand mit LBD aus 36F

Basis:
- Backend/PDF/ZUGFeRD aus stabilem Schritt 36I / v1.7.3
- LBD-/Rechnungsempfänger-UI aus stabilem Schritt 36F / v1.7.0

Ziel:
- Login, Security und produktiver ZUGFeRD-PDF-Pfad bleiben stabil wie 36I
- LBD-Erkennung/Empfängeranzeige wird auf den funktionierenden Stand aus 36F zurückgesetzt
- Keine OpenHTMLtoPDF-Umstellung im produktiven PDF-Pfad
- Keine riskanten Security-Matcher-Änderungen

Strategie für PDF/UA/WCAG ab jetzt:
- produktiver Button bleibt: ZUGFeRD-PDF OpenPDF/stabil
- OpenHTMLtoPDF/PDF-UA/WCAG nur als separater Testexport in einem späteren Schritt
- alte stabile Variante wird erst deaktiviert, wenn PDF/UA und WCAG mit Prüfbericht bestanden sind

Hinweis:
- Dieser Schritt enthält bewusst keinen erneuten Engine-Wechsel.
- Der separate Testbutton für OpenHTMLtoPDF wird erst nach stabiler Rückkehr von Login/LBD/PDF ergänzt.
