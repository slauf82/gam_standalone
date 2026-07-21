# Schritt 40k4 – Discovery Hybrid-Streaming

## Grundlage
- Stabiler vollständiger ARP-Suchlauf aus 40k1
- Live-Streaming, Fortschritt und Async-Security aus 40k3

## Änderung
- Vollständige ARP-Zeilen werden bereits während der Prozessausgabe verarbeitet und sofort an das Frontend übertragen.
- Parallel wird die gesamte Ausgabe gepuffert.
- Nach Prozessende wird der vollständige Puffer erneut konsolidiert und dedupliziert ausgewertet.
- Dadurch erscheinen frühe Treffer live, ohne die hohe Trefferquote der vollständigen 40k1-Suche zu verlieren.
- Registrierte, neue und gesamte Treffer bleiben getrennt sichtbar.

## Release-Cleanup
Nicht enthalten: node_modules, frontend/dist, backend/target, .git, IDE- und temporäre Dateien.
