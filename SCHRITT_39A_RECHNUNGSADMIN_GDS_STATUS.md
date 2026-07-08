# Schritt 39a – Rechnungsverwaltung / Admin-Teil

## Ziel

Erster Aufbau des administrativen Rechnungsbereichs auf Basis der vorhandenen GAM-1.0-Tabellen.

## Enthalten

- neues Modul **Rechnungsverwaltung**
- Login-Direktauswahl **Rechnungsverwaltung**
- Navigation nach Login
- GDS-/MasterData-basierte Bearbeitung vorhandener Tabellen
- keine neue Datenbankstruktur
- keine vereinfachten Ersatzfelder

## Umgesetzte Kataloge

- `rechnungsgesellschaft`
- `rechnungsdaten`
- `rechnungsgesellschaft_filiale`
- `rechnungsanrede`
- `rechnungstext`
- `rechnungsrechtlicherhinweis`
- `rechnungsgrussformel`
- `rechnungslogo`

## Fachlicher Schwerpunkt

Die Tabelle `rechnungsdaten` bleibt stichtagsfähig:

- `preis1`
- `preisneu`
- `preisalt`
- `preis_gueltigab`
- `mwst`
- `mwstalt`
- `mwst_gueltigab`
- `gültig_ab`
- `gültig_bis`

Damit bleiben Preisänderungen, Mehrwertsteuerumstellungen und zeitlich begrenzte Angebotszeiträume fachlich erhalten.

## Hinweis

39a ist noch nicht die neue Rechnungserfassung, sondern die administrative Grundlage dafür.
