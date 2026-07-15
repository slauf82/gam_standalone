# Rechnungsmodell – zentrale Dokumentdaten (40d22)

## Verbindliche Datenwege

- Rechnungsbezogene Kopfdaten stammen aus `rechnungsdetails`.
- Die Empfängeradresse wird über `rechnungsdetails.ADRESSID` auf `adressen.ID` aufgelöst.
- `FADRESSE` und `FEMAIL` sind nur historische Fallbackfelder, falls die referenzierte Adresse fehlt.
- Die Zahlungsart stammt aus `rechnungsdetails.ZAHLUNGSART`.
- Proformarechnungen verwenden entsprechend `p_rechnungsdetails`.

## Zentrale Implementierung

`InvoiceDocumentDataService` liefert ein einheitliches `InvoiceDocumentData` für:

- Rechnungs-PDF
- Zahlungsdokumente
- Patientenportal
- ZUGFeRD/E-Rechnung
- Mahnwesen und Inkasso

Damit dürfen einzelne Dokumentdienste keine eigenen Adressparser oder abweichenden Zahlungsart-Abfragen mehr einführen.

## Adressfelder

Die Stammdaten werden aus `adressen` übernommen:

`ANREDE`, `TITEL`, `VORNAME`, `NAMENSZUSATZ`, `NACHNAME`, `STRASSE`, `PLZ`, `ORT`, `LAND`, `PATIENTENNUMMER`, `GEBDATUM`, `VERSICHERTENNUMMER`, `VERSICHERTENART`.

HTML-Zeilenumbrüche in historischen Textfeldern werden vor der Ausgabe bereinigt.
