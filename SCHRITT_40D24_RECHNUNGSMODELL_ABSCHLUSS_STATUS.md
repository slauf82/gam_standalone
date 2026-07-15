# Schritt 40d24 – Rechnungsmodell-Abschluss

- ADRESSID, KINDADRESSID und FIRMAADRESSID bleiben die alleinigen Rollenquellen bestehender Rechnungen.
- Aktuell geladene LBD-Dateien werden nicht mehr als Ausgabe-Fallback für PDF, Archiv/Hotfolder oder ZUGFeRD verwendet.
- RDATUM ist Rechnungsdatum; BDATUM ist Behandlungsdatum.
- Der Behandlungstermin ist bei neuen Rechnungen im Formular bearbeitbar und wird als BDATUM gespeichert.
- GPREIS = Gutscheinpreis.
- GBEMERKUNG = Gutscheinbemerkung.
- RPROZENT = Rabatt in Prozent.
- RBEMERKUNG = Rabattbemerkung.
- Die vier Gutschein-/Rabattfelder sind Bestandteil der zentralen InvoiceDocumentData.
- Historischer Fallback bleibt ausschließlich FADRESSE/FEMAIL, wenn ADRESSID nicht mehr auflösbar ist.
