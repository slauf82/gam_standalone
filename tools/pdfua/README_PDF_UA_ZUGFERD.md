# PDF/UA + ZUGFeRD Prüfung

Schritt 36i ergänzt die PDF-Erzeugung so, dass die sichtbare Rechnung mit PDF/UA-relevanten Informationen erzeugt wird und anschließend weiterhin als ZUGFeRD/Factur-X-PDF über Mustangproject exportiert wird.

## Was technisch gesetzt wird

- Dokumentensprache (`/Lang`)
- Dokumenttitel und ViewerPreference `DisplayDocTitle`
- XMP-Metadaten inklusive `pdfuaid:part=1`
- MarkInfo `/Marked true`
- Tagged-PDF-Aktivierung, sofern die verwendete OpenPDF-Version `PdfWriter#setTagged()` bereitstellt
- Rollen für Tabellen, Tabellenköpfe, Tabellenzellen und Bilder, sofern OpenPDF die Accessibility-Methoden bereitstellt
- Alternativtexte für Logo und QR-Code, sofern OpenPDF die Accessibility-Methoden bereitstellt
- ZUGFeRD/Factur-X XML bleibt über Mustangproject eingebettet

## Empfohlene externe Prüfung

Für eine belastbare Freigabe sollten erzeugte Rechnungen mit einem Prüftool validiert werden:

- PAC 2024 / PDF Accessibility Checker
- Adobe Acrobat Preflight
- veraPDF für PDF/A-Aspekte
- ZUGFeRD/Factur-X Validator oder Mustangproject-Validierung

## Testfälle

Mindestens diese PDFs prüfen:

1. Deutsche Rechnung mit Logo und QR-Code
2. Französische Rechnung mit übersetzten Produktbeschreibungen
3. Rechnung mit mehreren Tabellenpositionen
4. ZUGFeRD-PDF aus `/api/invoices/{nummer}/pdf`
5. Debug-PDF aus `/api/invoices/{nummer}/pdf-debug`

## Wichtig

PDF/UA und ZUGFeRD müssen gemeinsam erhalten bleiben. Falls ein Prüftool meldet, dass Tags oder Tabellenstruktur fehlen, muss die konkrete OpenPDF-Unterstützung weiter ausgebaut oder die PDF-Erzeugung perspektivisch auf eine Bibliothek mit vollständiger PDF/UA-Unterstützung umgestellt werden.
