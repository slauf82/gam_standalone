# Schritt 33b – MaryTTS-Konfiguration als Standard

## Ziel v1.6.0

- MaryTTS als konfiguriertes Standard-TTS-Backend
- Browser-/System-TTS bleibt vorhanden
- Fallback-Kette bleibt vorhanden

## Backend

Neue Endpunkte:

- `GET /api/tts/status`
- `POST /api/tts/audio`

Standardkonfiguration:

```yaml
app:
  tts:
    enabled: true
    engine: marytts
    browser-fallback: true
    marytts:
      enabled: true
      endpoint: http://localhost:59125/process
```

## Frontend

- Browser-Stimme wird zuerst genutzt, wenn passend vorhanden.
- Wenn keine passende Browser-Stimme gefunden wird, wird `/api/tts/audio` mit MaryTTS versucht.
- Wenn MaryTTS nicht erreichbar ist, bleibt die Anwendung bedienbar.

## Hinweis

MaryTTS wird angebunden und standardmäßig konfiguriert, aber nicht als großes Binary/Voice-Paket ins ZIP eingebettet.
MaryTTS muss lokal laufen, z.B. auf `http://localhost:59125`.
