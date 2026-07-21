# Schritt 37i.1 – Build-Fix nach OpenPDF-Entfernung

## Ziel

Nach der Entfernung der OpenPDF-Abhängigkeit dürfen keine alten `com.lowagie.*`-Imports mehr kompiliert werden.

## Änderung

Die alten Dateien wurden als harmlose Legacy-Platzhalter ohne OpenPDF-Imports ergänzt/überschrieben:

- `InvoicePdfService.java`
- `InvoicePdfAccessibility.java`

Dadurch werden bei einer Extraktion über einen bestehenden Arbeitsstand noch vorhandene alte OpenPDF-Dateien überschrieben und Maven kompiliert nicht mehr gegen `com.lowagie.*`.

## Produktiver PDF-Pfad

Der produktive Export bleibt unverändert:

- OpenHTMLtoPDF
- PDF/A-3
- ZUGFeRD / Factur-X
- PDF/UA
- WCAG

## Wichtig

Die Platzhalter enthalten keine Spring-Annotationen und erzeugen keine Beans. Sie dienen nur dem sicheren Build-Cleanup.
