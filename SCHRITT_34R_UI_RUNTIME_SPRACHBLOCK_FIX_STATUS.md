# Schritt 34r – UI Runtime Sprachblock Fix

Basis: Schritt 34q / Browser-TTS aus 34k bleibt unverändert.

Korrekturen:

- Neue UI-Sprachen `it`, `sv`, `tr`, `ru` laufen jetzt gemeinsam durch die Runtime-Übersetzungskette.
- Frontend normalisiert DB-Keys mit Präfixen wie `ITALIAN.key` oder `ITALIAN.UI.key` auf den reinen UI-Key `key`.
- Runtime-Übersetzungen werden in der Oberfläche bevorzugt verwendet.
- Falls Runtime noch nicht geladen ist, greift für die neuen Sprachen ein kleiner eingebauter Fallback aus `i18n.ts`, statt sofort Deutsch zu zeigen.
- `translation_italian`, `translation_swedish`, `translation_turkish`, `translation_russian` werden beim ersten Nicht-DE-Sprachaufruf vorbereitet.

Erwartung nach Test:

- Bei Auswahl von Italienisch/Schwedisch/Türkisch/Russisch sollte die UI nicht mehr vollständig deutsch bleiben.
- Die Zieltabellen sollten nicht mehr nur für Italienisch gefüllt werden.
