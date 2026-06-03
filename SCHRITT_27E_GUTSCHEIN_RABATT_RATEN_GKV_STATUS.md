# Schritt 27e – Gutschein/Rabatt/Raten sichtbar + GKV-Verordnungsgebühr

## Enthalten

- Gutschein/Rabatt/Ratenzahlung werden in der Rechnungs-Detailansicht ausgewiesen.
- PDF weist Gutschein/Rabatt/Ratenzahlung sichtbar aus.
- Stored Gross/ENDPREIS wird für Detail-Summen berücksichtigt.
- Ratenzahlung wird als Hinweis mit Ratenanzahl und ca. Rate angezeigt.
- Automatische Verordnungsgebühr für GKV-Produkte bei Training-Gesellschaft vorbereitet:
  - GKV-Erkennung über Code/Beschreibung/Kategorie enthält `GKV`
  - Verordnungsgebühr-Erkennung über Code/Beschreibung/Kategorie enthält `Verordnungsgebühr` / `Verordnungsgebuehr`
  - wird nur einmal ergänzt

## Hinweise

Die GKV-Automatik ist bewusst heuristisch umgesetzt, bis die genaue Produkt-ID der Verordnungsgebühr feststeht.
