# Schritt 8 – Rechnungsmodul nutzbarer Arbeitsfluss

Diese Version baut auf Schritt 7 auf und zieht das Rechnungsmodul in Richtung erster produktiv nutzbarer GAM-2.0-Arbeitsversion.

## Neu

- Rechnungen können über denselben Editor neu angelegt oder bearbeitet werden.
- Rechnungspositionen können hinzugefügt, geändert und entfernt werden.
- Summen werden im Frontend live gegen das Backend berechnet.
- Rechnungsgesellschaften werden aus `rechnungsgesellschaft` geladen.
- Produkte/Leistungen kommen weiterhin aus `rechnungsdaten`.
- Bestehende Rechnungen können per `PUT /api/invoices/{number}` aktualisiert werden.
- Statusänderungen wie Storno/Gutschrift/Zahlungsavis sind über `PATCH /api/invoices/{number}/status` vorbereitet.
- Technisches Löschen ist separat als Draft-/Fehleingabe-Werkzeug vorhanden: `DELETE /api/invoices/{number}/draft`.
- ZUGFeRD/Factur-X bleibt Pflicht-Export über `/api/invoices/{number}/pdf`.
- Normal-PDF bleibt nur Debug/Fallback über `/api/invoices/{number}/pdf-debug`.

## Bewusst noch vorsichtig

- Storno ist fachlich bevorzugt; Löschen ist nur als technischer Notausgang für Fehltests vorgesehen.
- Nummernkreis bleibt kompatibel: höchste numerische `RNUMMER` + 1.
- Das finale alte Rechnungs-PDF-Layout ist noch nicht vollständig nachgebaut.
- ZUGFeRD-Validierung ist als Workflow und Status vorbereitet, muss beim ersten echten Build mit den konkreten Bibliotheken geprüft werden.

## Wichtige Tabellen

- `rechnungsdetails`
- `rechnung`
- `rechnungsdaten`
- `rechnungsgesellschaft`
- `.lbd`-Dateien für Rechnungsempfängerdaten

## Nächste sinnvolle Punkte

1. Alte Steuer-/Sonderfalllogik gegen reale Bestandsrechnungen prüfen.
2. PDF-Layout aus altem GAM genauer nachziehen.
3. ZUGFeRD/Factur-X mit echter Validierung gegen Validator testen.
4. Rechnungsläufe, Gutschriften und Storno fachlich finalisieren.
5. Danach Inventar/Lager-Workflows mit Rechnungen verknüpfen.
