# Schritt 39b – Rechnungserfassung Stichtagslogik

## Ziel

39b aktiviert die stichtagsbezogene Produktlogik in der Rechnungserfassung.

## Enthalten

- Produktliste wird abhängig vom Rechnungsdatum geladen
- Produkte mit `gültig_ab` erscheinen erst ab diesem Datum
- Produkte mit `gültig_bis` werden nach diesem Datum nicht mehr angeboten
- Preis wird aus `preis1`, `preisneu`, `preisalt` und `preis_gueltigab` zum Rechnungsdatum ermittelt
- Mehrwertsteuer wird aus `mwst`, `mwstalt` und `mwst_gueltigab` zum Rechnungsdatum ermittelt
- Ausgewählte Positionen übernehmen den wirksamen Preis und den wirksamen MwSt.-Satz
- Diese Werte werden in der Rechnungsposition gespeichert und damit historisch eingefroren

## Beispiel

- Rechnung am 30.06.2020: alter MwSt.-Satz
- Rechnung am 01.07.2020: gesenkter MwSt.-Satz
- Rechnung am 01.01.2021: wieder regulärer MwSt.-Satz

## Hinweis

39b ist bewusst ein Testschritt. Die eigentliche Positions- und Summenlogik wird mit 39c weiter ausgebaut.
