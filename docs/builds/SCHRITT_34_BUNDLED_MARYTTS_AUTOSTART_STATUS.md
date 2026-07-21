# Schritt 34 – Bundled MaryTTS Autostart auf Basis Schritt 33k

Basis: Schritt 33k – TTS Settings UI.

Änderungen:

- Embedded MaryTTS ist standardmäßig deaktiviert (`GAM_MARYTTS_EMBEDDED=false`).
- Bundled MaryTTS wird als separater Prozess automatisch gestartet, wenn `tts/marytts` vorhanden ist.
- `/api/tts/status` löst Embedded MaryTTS bei deaktiviertem Embedded-Modus nicht mehr aus.
- Statusfelder ergänzt: `maryTtsBundledStarted`, `maryTtsBundledAvailable`, `maryTtsBundledError`.
- Schritt-33k-Änderungen bleiben erhalten: TTS-Einstellungen UI, Stimme, Geschwindigkeit, Engine-Auswahl und bessere Root-Cause-Diagnose für Embedded-Fehler.

Erwarteter Normalfall:

```json
{
  "maryTtsEmbedded": false,
  "maryTtsEmbeddedAvailable": false,
  "maryTtsEmbeddedError": "Embedded MaryTTS disabled (bundled mode preferred)",
  "maryTtsBundled": true,
  "maryTtsBundledStarted": true,
  "maryTtsReachable": true,
  "browserFallback": true
}
```

Wenn keine MaryTTS-Distribution unter `tts/marytts` liegt, bleibt Browser-Fallback aktiv.
