# Schritt 37h8 – PDF/UA Span-/Admissible-Location-Feinschliff

Basis: Schritt 37h7

Ziel:
- PAC-Warnungen zu `Span Structured Elements` weiter reduzieren
- PAC-Warnungen zu `Content in admissible locations` weiter reduzieren
- PDF/A-3 + ZUGFeRD/Factur-X + PDF/UA-Tagging stabil beibehalten
- keine Änderungen am produktiven Button 1

Umsetzung:
- OpenHTMLtoPDF-HTML weiter semantisch entschärft
- Text in Header, Empfängerblock, Meta-Block, Summenblock, Bankblock und Portalblock stärker in echte Absatz-Elemente (`<p>`) verschoben
- direkte Textknoten in Layout-Containern reduziert
- `<br />`-Ketten in Bank-/Metadaten-/Summenbereichen entfernt
- Rechtshinweis und Grußformel werden absatzweise gerendert
- Logo/QR bleiben wie in 37h7 ohne `<figure>`/`<img>`-Strukturelemente
- keine echten Rechnungstabellen-Umbauten

Nicht geändert:
- LBD-Erkennung
- Security/Auth
- Button 1 OpenPDF/ZUGFeRD
- ZUGFeRD-XML-Erzeugung
- PDF/A-3-Konfiguration

Hinweis:
- Maven konnte in der Buildumgebung nicht geprüft werden, weil kein Zugriff auf repo.maven.apache.org bestand.
