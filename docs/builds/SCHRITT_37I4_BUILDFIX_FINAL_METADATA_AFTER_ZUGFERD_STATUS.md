# Schritt 37i.4 – Build-/PAC-Fix: PDF/UA-Metadaten nach ZUGFeRD finalisieren

Basis: Schritt 37i.3

Fix:
- Der Standard-PDF-Export `export(...)` finalisiert PDF/UA-/WCAG-Metadaten jetzt wieder nach der Mustang/ZUGFeRD-Einbettung.
- Damit werden PDF/UA-Identifier, XMP-Titel, DisplayDocTitle und Bookmark/Outline als letzter Postprocessing-Schritt gesetzt.
- `exportWithBasePdf(...)` hatte diesen Schritt bereits; der Standardpfad wurde jetzt angeglichen.
- OpenPDF bleibt entfernt.
