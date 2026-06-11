# Schritt 33a – TTS-Fallback-Kette

## Ziel

Für jede verfügbare Sprache soll eine möglichst passende Stimme gewählt werden, ohne dass der Benutzer manuell Windows-Stimmen konfigurieren muss.

## Umsetzung

Die Browser-/Systemstimmen werden automatisch durchsucht.

Fallback-Ketten:

```text
de → de-DE/de-AT/de-CH/de → en → de
en → en-US/en-GB/en-AU/en-CA/en → de
fr → fr-FR/fr-CA/fr-BE/fr → en → de
uk → uk-UA/uk → ru-RU/ru → en → de
```

## Verhalten

- Wenn eine passende Stimme vorhanden ist, wird `utterance.voice` explizit gesetzt.
- Wenn keine Zielstimme vorhanden ist, wird automatisch die nächste Fallback-Sprache gewählt.
- Ukrainisch fällt auf Russisch, danach Englisch, danach Deutsch zurück.
- Im Browser-Log steht zur Kontrolle:

```text
GAM TTS voice <lang> <voice.name> <voice.lang>
```

## Hinweis

MaryTTS/Backend-TTS ist damit noch nicht eingebaut. Dieser Schritt nutzt ausschließlich aktuell verfügbare Browser-/Systemstimmen, aber mit sinnvoller Fallback-Kette.
