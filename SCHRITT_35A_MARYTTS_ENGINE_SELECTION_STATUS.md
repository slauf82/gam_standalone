# ÜBERHOLT – ersetzt durch Schritt 35 Piper TTS v1

Dieser alte MaryTTS-Schritt ist durch `SCHRITT_35_PIPER_TTS_V1_STATUS.md` ersetzt. MaryTTS wird in GAM v1.7.0 nicht mehr als Vorlesetechnik angeboten.

---

# Schritt 35a – MaryTTS Engine-Auswahl und echte Nutzung

## Ziel
MaryTTS soll nicht nur installiert und gestartet werden, sondern in der GAM-Oberfläche als Vorlesetechnik auswählbar sein und tatsächlich für das Vorlesen verwendet werden.

## Umgesetzt

- TTS-Einstellung `auto | marytts | browser` wieder aktiviert
- Standard ist nun `auto`
- MaryTTS erscheint als auswählbare Engine, sobald `/api/tts/status` `maryTtsReachable=true` meldet
- Bei MaryTTS-geeigneten Sprachen nutzt `auto` bevorzugt MaryTTS
- BrowserTTS bleibt sofortiger Fallback
- Ukrainisch bleibt mangels MaryTTS-Sprachmodul beim BrowserTTS-Fallback
- Vorlesen-Button stoppt nun auch laufende MaryTTS-Audioausgabe
- `/api/tts/audio` wird für MaryTTS-Audio verwendet
- GitHub-Release-URL als Standard-Downloadquelle gesetzt

## Sprachlogik

MaryTTS bevorzugt für:

- de
- en
- fr
- it
- ru
- sv
- tr

BrowserTTS-Fallback für:

- uk
- alle nicht von MaryTTS unterstützten Sprachen
- Fehlerfälle / nicht erreichbaren MaryTTS-Server

## Hinweis
Frontend-/Backend-Build konnte in dieser Umgebung nicht geprüft werden, weil die lokalen Abhängigkeiten nicht vollständig verfügbar sind.
