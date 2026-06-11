# Schritt 33 – sprachabhängige Vorlesestimmen

## Ziel

Die Vorlesefunktion soll nicht mehr z.B. englische Texte mit einer deutschen Stimme sprechen.

## Umsetzung

- neue Funktion `preferredSpeechVoice(...)`
- automatische Stimmenauswahl je Sprache:
  - Deutsch: `de-DE`, `de-AT`, `de-CH`, `de`
  - Englisch: `en-US`, `en-GB`, `en-AU`, `en-CA`, `en`
  - Französisch: `fr-FR`, `fr-CA`, `fr-BE`, `fr`
  - Ukrainisch: `uk-UA`, `uk`
- falls vorhanden, wird `utterance.voice` explizit gesetzt
- falls keine passende Stimme vorhanden ist, bleibt Fallback über `utterance.lang`
- `speechSynthesis.getVoices()` wird beim Öffnen der Vorschau vorbereitet
- `onvoiceschanged` wird berücksichtigt

## Hinweis

Die verfügbaren Stimmen hängen vom Betriebssystem und Browser ab.
Unter Windows müssen zusätzliche Stimmen ggf. in den Windows-Sprachoptionen installiert werden.
