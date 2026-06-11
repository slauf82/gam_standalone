# Schritt 33c – MaryTTS Autostart / No-Config-Modus

## Ziel

MaryTTS soll aus GAM-Sicht ohne Benutzerkonfiguration funktionieren.

## Umsetzung

- MaryTTS bleibt Standard-Engine.
- GAM sucht automatisch nach einem lokalen MaryTTS-Bundle unter:
  - `./tts/marytts`
- Wenn dort ein Launcher vorhanden ist, versucht GAM beim Backend-Start MaryTTS automatisch zu starten.
- Wenn MaryTTS bereits auf Port 59125 läuft, wird es direkt genutzt.
- Wenn kein Bundle vorhanden ist, bleibt Browser-TTS/Fallback aktiv.

## Neue Diagnosefelder

`GET /api/tts/status` liefert zusätzlich:

- `maryTtsEmbedded`
- `maryTtsBundled`
- `maryTtsHome`
- `maryTtsReachable`

## Wichtig

Dieser Schritt integriert den Autostart und die No-Config-Struktur.
Das vollständige MaryTTS-Binary/Voice-Paket ist aus Größen-/Lizenzgründen noch nicht direkt im ZIP enthalten.

Sobald ein MaryTTS-Paket in `tts/marytts` liegt, funktioniert es ohne weitere GAM-Konfiguration.
