# Schritt 40d22 – Rechnungsmodell-Konsolidierung

- Zentrale Klasse `InvoiceDocumentDataService` eingeführt.
- `rechnungsdetails.ADRESSID` wird gegen `adressen.ID` aufgelöst.
- Zahlungsart wird ausschließlich aus `rechnungsdetails.ZAHLUNGSART` gelesen.
- `FADRESSE/FEMAIL` bleiben nur als historischer Fallback bestehen.
- Rechnungs-PDF und Zahlungsdokumente verwenden dieselben aufgelösten Dokumentdaten.
- Byte-Literale und HTML-Umbruchreste werden zentral bereinigt.
- Proformarechnungen werden über `p_rechnungsdetails` unterstützt.
- Datenmodell-Dokumentation unter `docs/datenmodell/rechnungsmodell-40d22.md` ergänzt.
- Anonymisierte Beispieldatenbank wieder in das Hauptpaket aufgenommen.
