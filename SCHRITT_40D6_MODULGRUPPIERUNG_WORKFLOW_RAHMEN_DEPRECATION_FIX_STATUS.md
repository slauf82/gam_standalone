# Schritt 40d6 – Modulgruppierung, Workflow-Rahmen und Deprecation-Fix

## Behoben
- Der überflüssige und veraltete Aufruf `PdfRendererBuilder.useFastMode()` wurde aus `InvoiceOpenHtmlPdfService` entfernt.
- PDF/UA-, PDF/A-3-U- und ZUGFeRD-Konfiguration bleiben unverändert.

## Modulauswahl
Die Modulauswahl im Logindialog und unter **Einstellungen → Module** nutzt nun dieselben Gruppen:
- Kernmodule
- Praxis und Abrechnung
- Organisation und Betrieb
- Workflow-Module

## Workflow-Erkennung
Workflow-Module erhalten einen violett-blauen Rahmen, damit sie sofort erkennbar sind. Rot, Gelb und Grün bleiben ausschließlich Statusfarben.

Aktuell als Workflow-Module markiert:
- Aufgabenverwaltung
- Freigabemanagement
- Prüfungen
- Marketing

Der Rahmen wird in der Login-Modulauswahl, in der Hauptnavigation und in **Einstellungen → Module** konsistent verwendet.

## Test
1. Backend kompilieren und prüfen, dass `InvoiceOpenHtmlPdfService` keine Deprecation-Meldung mehr auslöst.
2. Logindialog öffnen und die vier Modulgruppen prüfen.
3. Workflow-Module auf den violett-blauen Rahmen prüfen.
4. Nach Login dieselbe Kennzeichnung in der Navigation prüfen.
5. Unter Einstellungen → Module dieselbe Gruppierung und Kennzeichnung prüfen.
