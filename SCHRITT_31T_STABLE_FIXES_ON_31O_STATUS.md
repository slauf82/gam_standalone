# Schritt 31t – stabile Fixes auf Basis 31o

## Basis

Dieser Stand basiert auf dem funktionierenden Schritt 31o.

## Fixes

### LBD

- Standarddatei `beispiel.lbd`
- Suchpfad `C:/GAM2/config`
- robuste relative Suchpfade für Start aus Projektroot oder `backend/`
- Fallback: `beispiel.lbd`, danach `max.mustermann.lbd`
- `config/beispiel.lbd` im Paket enthalten

### Vorlesen/Stoppen

- Vorlesen/Stoppen wurde vorsichtig als Toggle-Button umgesetzt
- grün = Vorlesen starten
- rot = Vorlesen läuft / Stoppen

### Rechnungslabels

- sichtbare Rechnungslabel werden über `translatedInvoiceLabel(...)` vorbereitet

## Wichtig

Dieser Build vermeidet die beschädigte `main.tsx` aus 31p/31q und nimmt 31o als saubere Grundlage.
