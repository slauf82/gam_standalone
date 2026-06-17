# Schritt 34u – Translation SQL Audit + Persistenz-Korrektur

Auswertung der exportierten `translation_*` Tabellen:

- `translation_german` ist die beste Referenz fuer IDs und bestehende Keys.
- Die neuen Tabellen enthielten vor allem `module.*`-Keys, aber noch nicht den vollstaendigen Login-/Rechnungs-UI-Bestand.
- Die Modulbuttons im LoginDialog wurden live korrekt uebersetzt, aber nicht konsequent in allen Zieltabellen persistiert.
- Der Button rechts in der Rechnungsseite (`Stornorechnung 3650S`) nutzte noch einen hart codierten deutschen Text. Links in der Suche lief es bereits ueber die Uebersetzungsfunktion.

Aenderungen in 34u:

1. Backend: `translation_german` bleibt Referenz fuer vorhandene IDs.
2. Backend: deutsche Defaults und Frontend-Keys werden zusaetzlich in den Quellkatalog aufgenommen, auch wenn `translation_german` bereits gefuellt ist.
3. Backend: neue Keys erhalten stabile synthetische IDs oberhalb des aktuellen deutschen Maximalwerts.
4. Backend: Zieltabellen (`translation_italian`, `translation_swedish`, `translation_turkish`, `translation_russian`) werden dadurch nicht nur mit den alten 25/292 DB-Keys, sondern auch mit Modul-/Runtime-Keys gefuellt.
5. Frontend: Login-Modulbuttons enthalten jetzt auch `module.invoice` und `module.usersRights` als eigene stabile Keys.
6. Frontend: rechter Storno-Button verwendet jetzt `ui('cancelInvoice')` statt hart codiertem `Stornorechnung`.
7. Frontend: `invoiceLabel(...)` nutzt ebenfalls UI-Keys fuer Storno/Gutschrift/Zahlungsavis/Rechnung.

Wichtig:

- Browser-TTS bleibt unveraendert auf dem funktionierenden Stand aus 34k.
- Alte falsch/halb gefuellte Zieltabellen koennen beim naechsten Lauf durch ID-Angleichung teilweise ueberschrieben bzw. ergaenzt werden.
- Falls du die Zieltabellen vorher leerst, wird der Effekt sauberer sichtbar.
