# Schritt 38q2 – Prüfungen im Login auswählbar

## Ziel
Das Modul **Prüfungen** soll bereits im Login-Dialog auswählbar sein und nach erfolgreichem Login direkt geöffnet werden können.

## Umsetzung
- `Prüfungen` in `LOGIN_APPLICATIONS` ergänzt.
- Mapping `Prüfungen -> compliance` ergänzt.
- `compliance` in die harte Login-Zielseiten-Whitelist aufgenommen.
- Alias-Mapping ergänzt:
  - `prüfungen`
  - `pruefungen`
  - `geräteprüfungen`
  - `geraetepruefungen`
  - `checks`
  - `compliance`
  - `module.compliance`
  - `module.checks`
- Icon-/Modul-Key-Erkennung für Prüfungen/Checks ergänzt.

## Ergebnis
- Prüfungen ist im Login-Dialog sichtbar.
- Nach Login kann direkt ins Prüfungsmodul gewechselt werden.
- Nachgelagerte Navigation bleibt unverändert.

## Build-Hinweis
Kein lokaler Build durchgeführt, da die üblichen lokalen Abhängigkeiten hier nicht verfügbar sind.
