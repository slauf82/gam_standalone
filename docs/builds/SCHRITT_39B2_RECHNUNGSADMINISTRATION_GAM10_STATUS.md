# Schritt 39b2 – Rechnungsadministration GAM-1.0-nah

## Ziel

Die Administration des Rechnungsprogramms wird als eigenes Modul **Rechnungsadministration** geführt und nicht mit der Rechnungserfassung vermischt.

## Enthalten

- eigenes Login-/Navigationsziel Rechnungsadministration
- Rechnungsprodukte aus `rechnungsdaten` pflegbar
- Produkt anlegen / bearbeiten / löschen
- Preisfelder bleiben stichtagsfähig:
  - `preis1`
  - `preisneu`
  - `preisalt`
  - `preis_gueltigab`
- MwSt-Felder bleiben stichtagsfähig:
  - `mwst`
  - `mwstalt`
  - `mwst_gueltigab`
- Angebotszeitraum bleibt erhalten:
  - `gültig_ab`
  - `gültig_bis`
- Rechnungstexte editierbar:
  - Anreden
  - Rechnungstexte
  - rechtliche Hinweise
  - Grußformeln
- nach Speichern eines Rechnungstextes wird für die aktuelle Oberflächensprache eine Übersetzung angestoßen
- Beschriftungen in der Produktpflege sind fachlich lesbarer, die Originalspalten bleiben erhalten

## Wichtig

Die Rechnungserfassung nutzt die gepflegten Werte weiterhin stichtagsbezogen. Alte Rechnungen dürfen durch spätere Änderungen nicht verändert werden.
