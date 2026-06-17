# Schritt 34c – Embedded-MaryTTS aus normalem Laufzeitpfad entfernt

Status: umgesetzt

## Ziel

Nach der erfolgreichen Umstellung auf Bundled MaryTTS sollen im normalen Backend-Status und beim Build keine Embedded-MaryTTS-Reste mehr sichtbar sein.

## Änderungen

- `EmbeddedMaryTtsService.java` aus dem Backend-Quellcode entfernt.
- Embedded-MaryTTS-Abhängigkeiten aus `backend/pom.xml` entfernt.
- MaryTTS-spezifische Maven-Repositories und Dependency-Management-Overrides für Embedded entfernt.
- `app.tts.marytts.embedded` aus `application.yml` und `application-local.yml` entfernt.
- `/api/tts/status` gibt keine Embedded-Felder mehr aus.
- Statusmodell auf Bundled-Architektur vereinfacht:
  - `mode: "bundled"`
  - `maryTtsBundled`
  - `maryTtsBundledStarted`
  - `maryTtsBundledAvailable`
  - `maryTtsBundledError`
  - `maryTtsReachable`
  - `browserFallback`

## Erwarteter Status

Beispiel:

```json
{
  "enabled": true,
  "engine": "marytts",
  "mode": "bundled",
  "maryTtsEnabled": true,
  "maryTtsBundled": true,
  "maryTtsBundledStarted": true,
  "maryTtsBundledAvailable": true,
  "maryTtsBundledError": null,
  "maryTtsReachable": true,
  "browserFallback": true
}
```

## Ergebnis

Die produktive TTS-Architektur ist jetzt klar:

```text
GAM Backend
  -> Bundled MaryTTS als separater Prozess
  -> Browser-Fallback falls MaryTTS nicht erreichbar ist
```

Embedded-MaryTTS ist nicht mehr Teil des normalen Build- und Laufzeitpfads.
