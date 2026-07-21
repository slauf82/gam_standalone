# Schritt 30b – Reportarten nach Alt-GAM-Quellcode

## Rekonstruierte Reportarten

Aus dem alten `RechnungJpaController` und den Export-Entities wurden folgende Reportarten übernommen:

- Übersicht Belege
- Alle Daten / Positionsdaten (`ExportAllData`)
- DATEV (`ExportDatev`)
- Debitoren (`ExportDebitoren`)
- Umsatz (`ExportUmsatz`)
- Umsatz je Arzt / Auftraggeber (`ExportUmsatzjeArzt`)
- Umsatz je Filiale (`ExportUmsatzjeFiliale`)
- Tagesliste (`Tagesliste`)
- Produktranking (`ExportProduktranking`)

## Bedienung

- Von-Datum
- Bis-Datum
- Gesellschaft
- Reportart
- Download als XLSX, XLS, CSV oder DATEV-CSV

## Hinweis

Die Reportarten orientieren sich an den alten NamedNativeQueries. Einige Spalten wurden für GAM 2.0 in ein gemeinsames Reportmodell überführt, damit alle Reportarten über dieselbe moderne React-Oberfläche und dieselben Exportformate nutzbar sind.
