# Schritt 38m – Kassenbuch GDS

## Ziel
Das historische Kassenbuch ist nicht mehr nur als Lesemodul sichtbar, sondern als bearbeitbares GDS-Fachmodul integriert.

## Enthalten
- Modul `cashbook` bleibt vollständig registriert.
- Login-Direktzugriff auf Kassenbuch bleibt möglich.
- Kassenbuch nutzt den vorhandenen MasterData-Katalog `cashbook` und damit die Tabelle `kassenbuch`.
- Keine erfundenen Datenbankfelder.
- Tabellenansicht oben mit allen vom Katalog gelieferten Kassenbuchspalten.
- Zeilen sind direkt anklickbar.
- Neuer Kassenbucheintrag.
- Bearbeiten bestehender Kassenbucheinträge.
- Löschen bestehender Kassenbucheinträge.
- Suche / Aktualisieren.
- Summenkacheln für Einnahmen, Ausgaben und Saldo der geladenen Auswahl.
- GDS-Sticky-Aktionsleiste.
- Modaler Bearbeiten-Dialog.
- Toast-Meldungen und Änderungsdetails über GCS.

## Wichtige Felder
- ID
- DATUM
- GESCHÄFTSVORGANG
- STEUER
- EINNAHMEN
- AUSGABEN
- BESTAND
- GEGENKONTO
- MANDANTENNUMMER

## Hinweise
Die spätere tiefere Verzahnung mit Rechnungen, DATEV-Export, Monatsabschluss und Kassenprüfung bleibt bewusst eine spätere Ausbaustufe. 38m konzentriert sich auf stabile CRUD-Funktionalität für die bestehende Kassenbuch-Tabelle.
