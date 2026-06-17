# Schritt 34o – UI Backend-Keykatalog

Ziel: Nicht nur `ttsMary` und `ttsRate`, sondern den vollständigen deutschen UI-Quellbestand übersetzen.

Änderungen:

- Backend enthält jetzt einen vollständigen deutschen UI-Keykatalog als Fallback.
- `translate_GERMAN` bleibt zusätzliche Quelle.
- Frontend-Keys überschreiben weiterhin gezielt, wenn sie mitgesendet werden.
- Alte Keyform bleibt erhalten: `ITALIAN.key`, `SWEDISH.key`, `RUSSIAN.key`, `TURKISH.key`.
- Browser-TTS aus Schritt 34k bleibt unverändert.

Erwartung:

Bei Auswahl von Italienisch werden deutlich mehr Einträge als nur `ITALIAN.ttsMary` und `ITALIAN.ttsRate` erzeugt.
