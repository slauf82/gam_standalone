# Schritt 36e – PDF-/Vorschau-Gesellschaftskonsistenz

Status: umgesetzt als Release-Fix für GAM 2.0 v1.7.0
Datum: 2026-06-20

## Problem

Bei gleicher Rechnungsnummer konnte die Rechnungsvorschau die korrekte Gesellschaft anzeigen, während der PDF-/ZUGFeRD-Export eine andere Gesellschaft verwendete.

Beispiel:

- Vorschau: ACQUA Medical Ästhetik GmbH
- PDF: ACQUA Klinik
- gleiche Rechnungsnummer

Das ist release-blockierend, weil Vorschau, PDF, ZUGFeRD-XML, Archiv und Patientenportal immer dieselbe konkrete Rechnung inklusive Gesellschaft verwenden müssen.

## Ursache

Die Vorschau arbeitete bereits mit `selected.summary.companyId`.

Der PDF-/ZUGFeRD-Pfad verwendete dagegen teilweise nur die Rechnungsnummer:

- `repo.findDetail(number)`
- `zugferdService.export(number, lang)`
- `pdfService.render(number, lang)`
- `zugferdService.xml(number)`
- `archiveService.archive(number, bytes)`

In Alt-GAM sind Rechnungsnummern nicht überall sicher global eindeutig, weil Gesellschaften eigene Nummernkreise haben können. Ohne `companyId` kann die Datenbank bei gleicher Nummer die falsche Gesellschaft liefern.

## Fix

Die `companyId` wird jetzt durch den gesamten PDF-/Exportpfad durchgereicht:

- Frontend PDF-Link: `pdfUrl(number, lang, companyId)`
- Frontend XML-Link: `zugferdXmlUrl(number, companyId)`
- Backend `/api/invoices/{number}/pdf?companyId=...`
- Backend `/api/invoices/{number}/pdf-debug?companyId=...`
- Backend `/api/invoices/{number}/zugferd.xml?companyId=...`
- `ZugferdExportService.export(number, companyId, language)`
- `ZugferdExportService.check(number, companyId)`
- `ZugferdExportService.xml(number, companyId)`
- `InvoicePdfService.render(number, companyId, language)`
- `InvoicePdfService.renderVisualPdf(number, companyId, forZugferd, language)`
- `InvoicePdfArchiveService.archive(number, companyId, pdfBytes)`
- Patientenportal-PDF rendert mit der zum Portalzugriff passenden Gesellschaft.

## Ergebnis

Für eine ausgewählte Rechnung gilt jetzt verbindlich:

```text
selected.summary.number
+
selected.summary.companyId
=
konkrete Rechnung für Vorschau, PDF, XML, Archiv und Portal
```

Damit darf bei gleicher Rechnungsnummer keine falsche Gesellschaft mehr in PDF oder ZUGFeRD auftauchen.

## Testplan

1. Rechnung 3670 bei ACQUA Medical Ästhetik öffnen.
2. Vorschau prüfen: Gesellschaft muss ACQUA Medical Ästhetik sein.
3. ZUGFeRD-PDF öffnen.
4. PDF-Kopf, Absender, Bankdaten und Steuerdaten prüfen.
5. ZUGFeRD-XML herunterladen und Gesellschaft prüfen.
6. Patientenportal-Link öffnen und PDF dort erneut prüfen.
7. Falls dieselbe Rechnungsnummer in einer anderen Gesellschaft existiert, dort denselben Test durchführen.

## Release-Bewertung

Dieser Fix ist release-relevant und sollte vor GAM 2.0 v1.7.0 zwingend getestet werden.
