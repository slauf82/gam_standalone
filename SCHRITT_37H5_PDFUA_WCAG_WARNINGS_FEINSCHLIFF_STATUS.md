# Schritt 37h5 – PDF/UA- und WCAG-Warnungs-Feinschliff

Basis: Schritt 37h4.

## Ziel

Die nach 37h4 verbliebenen PAC-Warnungen sollen reduziert werden, ohne den funktionierenden PDF/A-3- und ZUGFeRD-Pfad zu gefährden.

## Änderungen

- Button 1 bleibt unverändert.
- Button 2 bleibt OpenHTMLtoPDF + PDF/A-3 + ZUGFeRD.
- PDF/UA-Metadaten und Dokumentnavigation aus 37h4 bleiben erhalten.
- Layoutbereiche im OpenHTMLtoPDF-Template werden nicht mehr über CSS `display: table/table-cell` aufgebaut.
- Header, Summenblock und Portalblock nutzen stattdessen Float-/Block-Layout.
- Echte Rechnungstabelle bleibt unverändert und semantisch als Tabelle erhalten.
- Keine riskanten Tabellenumbauten wie im ersten 37h-Versuch.

## Erwartung

- PDF bleibt optisch stabil.
- Tags bleiben vorhanden.
- PDF/UA- und WCAG-Warnungen sollten weiter sinken.
- ZUGFeRD-XML bleibt eingebettet.
