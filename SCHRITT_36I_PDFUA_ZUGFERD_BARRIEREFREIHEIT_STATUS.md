# Schritt 36i – PDF/UA + ZUGFeRD Barrierefreiheit

Version: GAM 2.0 v1.7.3  
Basis: v1.7.2 / Schritt 36h

## Ziel

Die erzeugten Rechnungs-PDFs sollen barriereärmer und PDF/UA-orientiert erzeugt werden, ohne die bestehende ZUGFeRD/Factur-X-Funktion zu beschädigen.

## Umgesetzt

- PDF/UA-Metadaten ergänzt
- `pdfuaid:part=1` im XMP-Paket ergänzt
- Dokumentensprache `/Lang` gesetzt
- Dokumenttitel und `DisplayDocTitle` gesetzt
- MarkInfo `/Marked true` gesetzt
- Tagged-PDF-Aktivierung vorbereitet und defensiv über `PdfWriter#setTagged()` aktiviert, sofern OpenPDF diese Methode bereitstellt
- Tabellen, Tabellenköpfe, Tabellenzellen, Logo und QR-Code erhalten Accessibility-Rollen, sofern OpenPDF die entsprechenden Methoden bereitstellt
- Alternativtexte für Logo und QR-Code ergänzt, sofern OpenPDF diese Attribute unterstützt
- ZUGFeRD/Factur-X Export bleibt über Mustangproject erhalten
- Statusmeldung von ZUGFeRD weist nun auf Schritt 36i hin
- Prüfhilfe `CHECK_PDF_UA_ZUGFERD.bat` ergänzt
- Prüfdokumentation unter `tools/pdfua/README_PDF_UA_ZUGFERD.md` ergänzt

## Nicht geändert

- ZUGFeRD/Factur-X XML-Erzeugung bleibt unverändert
- PDF-Archiv und Hotfolder bleiben unverändert
- Rechnungsdatenlogik bleibt unverändert

## Wichtiger Testhinweis

Die Sandbox konnte Maven/Frontend-Abhängigkeiten nicht herunterladen. Der Build und eine echte PDF/UA-Prüfung mit PAC 2024 oder Acrobat Preflight müssen lokal erfolgen.

Empfohlene Prüfung:

1. Backend starten
2. Rechnung als ZUGFeRD-PDF erzeugen
3. PDF mit PAC 2024 prüfen
4. Eingebettetes ZUGFeRD/Factur-X XML prüfen
5. Lesereihenfolge, Sprache, Tabellen und Alternativtexte kontrollieren

## Zielzustand

GAM soll Rechnungen erzeugen, die sowohl für elektronische Rechnungsprozesse als auch für Screenreader und barrierearme Nutzung geeignet sind.
