# Schritt 40d16 – Zahlungsdokument: persönliche Anrede und PDF-Lesezeichen

## Behoben

- Zahlungsdokumente werden nach der OpenHTMLtoPDF-Erzeugung durch die vorhandene PDFBox-PDF/UA-Nachbearbeitung geführt.
- Für die sichtbare H1-Überschrift wird ein echtes PDF-Outline-Lesezeichen auf Seite 1 erzeugt.
- Bereits gespeicherte Rechnungsadressen werden wieder in Anrede, Titel, Vorname und Nachname zerlegt.
- Unterstützt werden unter anderem `Herr`, `Herrn`, `Frau`, `Divers`, `Mx`, `Dr.`, `Prof.`, `Prof. Dr.` und `PD Dr.`.
- Dadurch entstehen wieder persönliche Anreden wie `Sehr geehrter Herr Dr. Mustermann,` und `Sehr geehrte Frau Prof. Dr. Mustermann,`.
- Nur wenn die gespeicherte Adresse keine eindeutige Anrede enthält, bleibt der neutrale Fallback `Guten Tag ...` bestehen.

## Test

1. Zahlungserinnerung oder Mahnung für einen Empfänger mit gespeicherter Anrede neu öffnen/erzeugen.
2. Persönliche Anrede kontrollieren.
3. PDF in PAC/Quality prüfen.
4. Im PDF-Navigationsbereich muss ein Lesezeichen mit demselben Titel wie die H1 erscheinen.
