# Schritt 34c2 – Embedded-Reste entfernen und Bundled-Hinweis entschärfen

## Ziel

Der normale Build- und Startpfad soll keine Embedded-MaryTTS-Warnungen mehr zeigen.

## Änderungen

- `EmbeddedMaryTtsService.java` ist im Paket nicht enthalten.
- `MaryTtsRuntimeManager` behandelt ein fehlendes MaryTTS-Bundle nicht mehr als Fehler.
- Wenn `tts/marytts` noch nicht befüllt ist, wird nur noch ein kurzer Fallback-Hinweis ausgegeben.
- `CLEANUP_OLD_EMBEDDED_TTS.bat` ergänzt, um alte Dateien aus einem vorherigen Zielordner zu entfernen.

## Wichtig

Wenn Schritt 34c über eine alte Installation kopiert wurde, kann die alte Datei im Zielordner erhalten bleiben.
Dann bitte einmal `CLEANUP_OLD_EMBEDDED_TTS.bat` ausführen oder die Datei manuell löschen:

```text
backend/src/main/java/de/kopfzentrum/gam/tts/EmbeddedMaryTtsService.java
```

Danach:

```text
mvn clean compile
```
