# GAM 2.0 v1.7.0 – Schritt 35: Piper TTS v1

## Ziel

MaryTTS wird aus der aktiven Vorlesetechnik entfernt. Piper ist die neue Standard-Engine, Browser Speech bleibt Fallback.

## Enthaltene Änderungen

- Backend `/api/tts/status` liefert Piper-Status.
- Backend `/api/tts/audio` erzeugt WAV über `tts/piper/piper.exe`.
- MaryTTS Runtime Manager entfernt.
- MaryTTS-Konfiguration aus `application.yml` und `application-local.yml` entfernt.
- Frontend bietet MaryTTS nicht mehr als Vorlesetechnik an.
- UI-/TTS-Sprachen erweitert auf 13 Sprachen.
- Piper-Voice-Erkennung über `.onnx` + `.onnx.json`.

## Unterstützte Sprachen

Deutsch, Englisch, Französisch, Italienisch, Spanisch, Portugiesisch, Niederländisch, Polnisch, Tschechisch, Schwedisch, Türkisch, Russisch, Ukrainisch.

## Ordner

```text
tts/piper/piper.exe
tts/piper/voices/<voice>.onnx
tts/piper/voices/<voice>.onnx.json
```

## Test

1. `piper.exe` ablegen.
2. Mindestens eine Voice mit `.onnx` und `.onnx.json` ablegen.
3. Backend starten.
4. `GET /api/tts/status` prüfen.
5. In der Rechnungsvorschau „Vorlesen“ testen.

Wenn Piper oder die konkrete Voice fehlt, fällt das Frontend auf Browser Speech zurück.
