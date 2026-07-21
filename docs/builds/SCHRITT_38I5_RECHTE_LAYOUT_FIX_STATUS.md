# Schritt 38i5 – Benutzer/Rechte: Rechte-Layout ohne Überlappungen

## Ziel

Der Rechte-Tab in „Benutzer/Rechte“ wurde layoutseitig stabilisiert. Felder dürfen sich nicht mehr überlagern, sondern werden mit festen Grid-Bereichen und responsivem Umbruch angezeigt.

## Umgesetzt

- Rechte-Tab auf 12-Spalten-GDS-Grid umgestellt
- Anwendung, Anwendung manuell, Gesellschafts-ID und Rolle bekommen eigene stabile Grid-Bereiche
- Formularfelder erhalten `min-width: 0`, `max-width: 100%` und `box-sizing: border-box`
- Hoch/Runter-Spinner der Gesellschafts-ID bleiben sichtbar
- Buttonbereich für Rechte-Zuordnung bricht sauber um
- Bei kleineren Fenstern werden Felder untereinander bzw. zweispaltig angeordnet statt zu überlappen

## Ergebnis

Die Bereiche Gesellschafts-ID/Rolle, Button-Zuordnung/ID Anwendung sowie Anwendung/Anwendung manuell überdecken sich nicht mehr.
