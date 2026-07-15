# Schritt 40d21 – PDF-Log- und Zahlungsart-SQL-Bereinigung

- Zahlungsart wird aus `rechnungsdetails.ZAHLUNGSART` über `RNUMMER` und Gesellschaft geladen.
- Die fehlerhafte Abfrage auf `rechnung.ZAHLUNGSART` wurde entfernt.
- Nicht unterstützte PDF-CSS-Eigenschaften `object-fit`, `-fs-bookmark-level` und `overflow-wrap` wurden ersetzt bzw. entfernt.
- Dokumentbeschreibungen werden zusätzlich als `dc.description` in das XHTML geschrieben.
- PDF/UA-Metadaten bleiben über `InvoicePdfAccessibility` finalisiert.
- Der bekannte OpenHTMLtoPDF-False-Positive zur Dokumentbeschreibung wird gezielt auf Logger-Ebene ausgeblendet; andere Renderer-Warnungen bleiben sichtbar.
