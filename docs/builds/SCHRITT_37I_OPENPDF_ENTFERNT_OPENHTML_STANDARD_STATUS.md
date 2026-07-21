# Schritt 37i – OpenPDF entfernt, OpenHTMLtoPDF ist Standard

## Ziel

Nach bestandenen PAC-Prüfungen für PDF/UA, WCAG, Quality und AI wird der alte OpenPDF-Pfad entfernt.

## Änderungen

- OpenPDF-Abhängigkeit aus `backend/pom.xml` entfernt.
- `InvoicePdfService` entfernt.
- Produktiver PDF-Endpunkt `/api/invoices/{number}/pdf` nutzt jetzt OpenHTMLtoPDF + PDF/A-3 + ZUGFeRD/Factur-X + PDF/UA/WCAG-Feinschliff.
- Alter Testbutton für OpenHTMLtoPDF aus der Rechnung UI entfernt.
- `/pdf-openhtml` bleibt als Kompatibilitätsalias erhalten und nutzt denselben OpenHTMLtoPDF-ZUGFeRD-Pfad.
- Patientenportal-PDF nutzt ebenfalls den neuen ZUGFeRD/OpenHTMLtoPDF-Pflichtpfad.

## Nicht geändert

- LBD-Erkennung bleibt unverändert.
- Security/Auth bleibt unverändert.
- ZUGFeRD/XML-Erzeugung bleibt unverändert.
- PDF/UA/WCAG-Feinschliff aus 37h8 bleibt erhalten.

## Erwartung

Ein einziger PDF-Button erzeugt nun die produktive Rechnung als:

- PDF/A-3
- ZUGFeRD/Factur-X
- PDF/UA
- WCAG-konform
- QR-/Portal-fähig
