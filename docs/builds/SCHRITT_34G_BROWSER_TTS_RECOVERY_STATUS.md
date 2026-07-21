# Schritt 34g – Browser-TTS Recovery

## Ziel

Der zuverlässige Browser-/Windows-TTS-Fallback ist wieder der Standardpfad. MaryTTS bleibt optional und wird nur genutzt, wenn es in den Vorleseeinstellungen explizit ausgewählt wird.

## Änderungen

- Standard-Vorlesetechnik wieder auf `browser` gesetzt.
- `auto` blockiert nicht mehr durch einen MaryTTS-Versuch.
- Browserstimmen werden mehrfach asynchron nachgeladen.
- Stimmenliste bleibt nicht leer, wenn für die gewählte Sprache keine passende Stimme installiert ist.
- Passende Stimmen werden bevorzugt angezeigt, andere Browserstimmen bleiben trotzdem auswählbar.
- MaryTTS bleibt manuell testbar, aber nicht mehr Teil des Standard-Erststarts.

## Erwartetes Verhalten

1. Frontend starten.
2. Rechnungsvorschau öffnen.
3. Vorleseeinstellungen öffnen.
4. Browser-/Windows-Stimmen erscheinen wieder zuverlässig.
5. Vorlesen funktioniert auch ohne MaryTTS-Bundle.

