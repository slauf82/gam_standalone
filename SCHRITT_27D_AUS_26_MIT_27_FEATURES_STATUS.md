# Schritt 27d – aus Schritt 26 + Schritt-27-Funktionen

Basis: letzter bekannter kompilierbarer Schritt 26.

Enthalten:

- Gutschein zuschaltbar im Rechnungseditor
- Gutscheintext und Gutscheinbetrag
- Rabatt zuschaltbar
- Rabattart Prozent/Betrag
- Rabattwert
- Ratenzahlung 1–5 Raten
- Zahlungsavis-Grundlogik für Ratenzahlung
- Beleglabel-Fix aus Schritt 26 bleibt enthalten
- companyId-Fix aus Schritt 25 bleibt enthalten
- PDF-Belegtyp-Fix:
  - Rechnung
  - Stornorechnung
  - Gutschrift
  - Proforma-Rechnung
  - Zahlungsavis

Build-Hinweis:

Dieser Stand wurde aus Schritt 26 und den Schritt-27-Funktionsänderungen neu zusammengesetzt. Die bekannten Java-Kompilierfehler aus 27b/27c wurden korrigiert:

- InvoiceUpdateRequest wird nicht mehr an eine reine InvoiceCreateRequest-Methode übergeben.
- Primitive double-Werte werden nicht mehr mit null verglichen.

Lokaler Test:

```bat
start-backend.cmd
```

oder:

```bat
mvnw.cmd clean package
mvnw.cmd spring-boot:run
```
