# Schritt 33d – MaryTTS Embedded

## Ziel

MaryTTS soll nicht nur als externer Server angebunden sein, sondern direkt im Java-Backend verfügbar sein.

## Umsetzung

- Embedded MaryTTS über Maven-Artefakte
- `EmbeddedMaryTtsService`
- `/api/tts/audio` nutzt zuerst Embedded MaryTTS
- wenn Embedded MaryTTS nicht verfügbar ist, fällt GAM auf den externen/bundled MaryTTS-Server und danach Browser-TTS zurück

## Eingebundene MaryTTS-Artefakte

- `marytts-runtime:5.2.1`
- `marytts-lang-de:5.2.1`
- `marytts-lang-en:5.2.1`
- `marytts-lang-fr:5.2.1`
- `voice-bits3-hsmm:5.2` (Deutsch)
- `voice-cmu-slt-hsmm:5.2.1` (Englisch)
- `voice-enst-camille-hsmm:5.2` (Französisch)

## Diagnose

`GET /api/tts/status` zeigt zusätzlich:

- `maryTtsEmbeddedAvailable`
- `maryTtsEmbeddedError`
- `maryTtsEmbeddedVoices`

## Hinweis zur Größe

Das ZIP enthält den Quellcode und die Maven-Konfiguration. Die MaryTTS-JARs/Voice-JARs werden beim Maven-Build aus Maven Central geladen und landen dann in der gebauten Anwendung.
