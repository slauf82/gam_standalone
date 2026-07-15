# Schritt 40d34 – Zahlungsworkflow: PDF-Sprache beim internen Dokumentabruf

## Ziel
Zahlungserinnerungen, Mahnungen und Inkassodokumente müssen auch beim Öffnen aus dem internen Zahlungsworkflow in der aktuell ausgewählten Rechnungs-/PDF-Sprache gerendert werden. Die GAM-Oberfläche bleibt unabhängig davon in der Oberflächensprache.

## Änderungen

### Frontend
- `PaymentWorkflowPanel` erhält die aktuell ausgewählte `pdfLanguage` aus der Rechnungsansicht.
- Neue Zahlungsschreiben werden mit dieser PDF-Sprache erzeugt; der bisher fest gesetzte Wert `de` wurde entfernt.
- Beim Öffnen eines bestehenden Zahlungsdokuments wird `lang=<pdfLanguage>` an den Backend-Endpunkt übergeben.
- Ein Sprachwechsel in der Rechnungsansicht wirkt damit unmittelbar auf den nächsten Dokumentabruf; eine erneute Speicherung ist nicht erforderlich.

### Backend
- `GET /api/invoices/{number}/payment-workflow/documents/{id}/pdf` akzeptiert nun optional `lang`.
- Ist `lang` gesetzt, wird das Dokument rückwirkend in dieser Sprache neu gerendert.
- Ohne `lang` bleibt die ursprünglich am Dokument gespeicherte Sprache der Fallback.
- Der Download-Dateiname verwendet die lokalisierte Dokumentbezeichnung der tatsächlich gerenderten Sprache.

## Fachliche Sprachtrennung
- Zahlungsworkflow-Oberfläche: Oberflächensprache
- Zahlungserinnerung/Mahnung/Inkasso als PDF: ausgewählte PDF-Sprache
- Dokumentvorlesen im Patientenportal: PDF-Sprache

## Unverändert
- Rechteprüfung und Gesellschaftszuordnung
- Zahlungsstatus, Fristen und Beträge
- PDF/UA-Nachbearbeitung
- Patientenportal-Mehrsprachenlogik aus 40d33
