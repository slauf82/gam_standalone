# Schritt 31w – Favicon & Visual Consistency

## Enthalten

- Original-Favicon aus GAM 1.0 nach `frontend/public/favicon.ico` übernommen
- `frontend/index.html` verweist auf `/favicon.ico`
- alte GAM-1.0-Aktionsicons kopiert nach `frontend/public/old-gam-icons`
- Modulnavigation nach Login verwendet dieselbe Bildsprache wie der Login
- Aktionsicons im Rechnungsprogramm vorbereitet:
  - Suchen
  - Neue Rechnung
  - PDF/ZUGFeRD
  - XML/CSV

## Kopierte Aktionsicons

```text
cancel.png
check.png
csv.png
excel.png
pdf.png
```

## Hinweis

Die Hauptnavigation nutzt weiterhin die vorhandene `iconForModule(...)`-Zuordnung, dadurch sind Login und Shell optisch konsistenter.
