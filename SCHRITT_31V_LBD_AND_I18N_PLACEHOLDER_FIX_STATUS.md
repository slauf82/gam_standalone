# Schritt 31v – LBD und i18n-Platzhalter-Fix

## Problem

Nach Schritt 31u funktionierte die Anwendung wieder, aber es blieben zwei sichtbare Platzhalter:

- `invoicePreviewTitle`
- `lbdMissingPlaceholder`

Außerdem wurde `beispiel.lbd` noch nicht zuverlässig geladen.

## Fix

### Übersetzung

Die fehlenden Keys wurden in allen Sprachblöcken ergänzt:

- `invoicePreviewTitle`
- `previewTitle`
- `lbdMissingPlaceholder`

### LBD

Die LBD-Suche wurde erneut robuster gemacht:

- Standarddatei bleibt `beispiel.lbd`
- Suchpfade enthalten:
  - `C:/GAM2/config`
  - `C:\GAM2\config`
  - `./config`
  - `../config`
  - `./config/demo-lbd`
  - `../config/demo-lbd`
  - `./daten/rechnung`
  - `../daten/rechnung`
- Charset-Default auf `windows-1252`
- `beispiel.lbd` liegt im Paket in:
  - `config/beispiel.lbd`
  - `backend/config/beispiel.lbd`
  - `config/demo-lbd/beispiel.lbd`

## Test

1. Backend und Frontend neu starten.
2. Dashboard prüfen: `.lbd gefunden`.
3. Rechnungsvorschau prüfen: keine `lbdMissingPlaceholder`-Anzeige.
4. Titel prüfen: kein `invoicePreviewTitle` sichtbar.
