# Schritt 40d23a – Buildfix joinNonBlank

Behoben wurde der Compilerfehler in `InvoiceOpenHtmlPdfService`:

- fehlende Hilfsmethode `joinNonBlank(String... values)` ergänzt
- leere und null-Werte werden übersprungen
- Namensbestandteile werden mit genau einem Leerzeichen verbunden
- Rollenlogik aus 40d23 bleibt unverändert

Betroffener Aufruf:

`personName(...)` für Titel, Vorname, Namenszusatz und Nachname.
