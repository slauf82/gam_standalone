# Schritt 40d37 – Rechnungsempfänger-Anrede nach PDF-Sprache

- Die Anrede im sichtbaren Rechnungsempfängerblock wird anhand der Rechnungs-/PDF-Sprache lokalisiert.
- Die Erkennung berücksichtigt jetzt sowohl den Anredetext als auch den LBD-Anredeindex (1 = männlich, 2 = weiblich).
- Vorschau und OpenHTMLtoPDF-/ZUGFeRD-PDF verwenden dieselbe robuste Erkennung.
- Beispiele Englisch: `Herr David Lehmann` → `Mr David Lehmann`, `Frau Heike Bernstein` → `Ms Heike Bernstein`.
