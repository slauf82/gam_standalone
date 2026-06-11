# Schritt 32 – stabile UI-/Favicon-Fixes auf Basis 31x

## Basis

Dieser Build basiert auf Schritt 31x, weil dort die Navigationstexte wieder korrekt waren.

## Übernommen aus 31y, aber vorsichtiger

- fehlende Übersetzungen ergänzt:
  - Prüfungen / Checks
  - Reports
  - Benutzer/Rechte / Users/permissions
  - Stornorechnung / Cancellation invoice
  - ZUGFeRD-Export bereit / ZUGFeRD export ready
- Favicon eingebunden:
  - `frontend/public/favicon.ico`
  - `frontend/favicon.ico`
  - Links in `frontend/index.html`

## Wichtig

Die Navigationsarray-Labels wurden bewusst **nicht** auf technische Keys wie `reports` oder `usersRights` umgebaut.
Stattdessen übersetzt `moduleText(...)` weiterhin die sichtbaren Labels. Dadurch sollten nicht mehr nur Keys oder falsche Texte erscheinen.

## Favicon kopiert

```text
True
```

## Hinweis

Browser-Favicons werden stark gecacht. Falls es nicht sofort sichtbar ist:
- Hard Reload
- Tab schließen und neu öffnen
- ggf. Browsercache für localhost leeren
