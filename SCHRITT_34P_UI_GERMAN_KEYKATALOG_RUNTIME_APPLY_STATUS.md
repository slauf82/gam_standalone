# Schritt 34p – UI German-Keykatalog und Runtime-Anwendung

- translate_GERMAN ist jetzt der führende Keykatalog.
- Es werden alle vorhandenen GERMAN.key-Einträge aus der Datenbank gelesen.
- Alt-GAM-Varianten mit reinen Keys oder abweichenden Spaltennamen werden robuster erkannt.
- ITALIAN.key / SWEDISH.key / RUSSIAN.key / TURKISH.key bleiben das Ziel-Schema.
- Nach dem Laden von Runtime-Übersetzungen wird ein Re-Render-Event ausgelöst, damit übersetzte Werte sofort in der Oberfläche sichtbar werden.
- Browser-TTS aus 34k bleibt unverändert.
