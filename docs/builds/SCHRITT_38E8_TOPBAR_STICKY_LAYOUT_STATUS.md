# Schritt 38e8 – Topbar und Sticky-Leisten sauber getrennt

## Ziel

Die globale Meldungs-/Logout-Leiste darf keine Suchfelder oder Aktionsleisten der Module verdecken.

## Umgesetzt

- Globale Topbar mit **Meldungen** und **Logout** bleibt dauerhaft ganz oben sichtbar.
- Meldungen stehen etwas weiter links/rechts entzerrt und überdecken keine Eingabefelder mehr.
- Der Logout-Button wurde aus dem normalen Seitenheader in die globale Topbar verlegt.
- Modulinterne Sticky-Aktionsleisten docken darunter an.
- Reihenfolge beim Scrollen:

```text
1. Meldungen + Logout
2. Neu / Suche / Filter
3. Tabellenkopf / Datenzeilen
```

## Betroffene Bereiche

- Geräteverzeichnis
- Lagerverwaltung
- Modul-Administration
- Inventar ↔ Lager
- alle weiteren Bereiche mit `.sticky-actionbar`

## Technische Hinweise

- Neue globale CSS-Abstände:
  - `--gam-global-topbar-height`
  - `--gam-sticky-toolbar-top`
- `body.gam-authenticated` erhält automatisch oberen Abstand.
- `.sticky-actionbar` nutzt nun `top: var(--gam-sticky-toolbar-top)`.
- `GamNotificationCenter` kann jetzt optional den Logout-Button aufnehmen.

## Build

Frontend-Build erfolgreich ausgeführt:

```text
npm install
npm run build
```

## Backend / Datenbank

Keine Backend-, API- oder Datenbankänderungen.
