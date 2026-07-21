# Schritt 34i – MaryTTS Diagnose + Silent Start

## Ziel

Der normale Ersteindruck darf keine irritierenden MaryTTS- oder javac-Hinweise mehr zeigen.
MaryTTS wird realistisch behandelt: Browser-TTS bleibt Standard, MaryTTS ist optional und wird nur genutzt, wenn ein echtes Bundle vorhanden ist.

## Änderungen

- Normale `[GAM TTS] Bundled MaryTTS runtime not present` Ausgabe entfernt.
- MaryTTS-Diagnose nur noch mit `GAM_MARYTTS_DIAGNOSTICS=true`.
- `MaryTtsRuntimeManager` nutzt keine veraltete `new URL(...)` API mehr.
- Maven Compiler: Annotation-Processing-Hinweis per `proc=none` unterdrückt.
- `INSTALL_MARYTTS_BUNDLE.bat` ergänzt.
- `CHECK_MARYTTS_BUNDLE.bat` ergänzt.

## Aktueller Sachstand MaryTTS

Im Projektpaket liegt bisher nur ein Platzhalter unter `tts/marytts`.
Das ist der Grund, warum MaryTTS nicht starten kann.

Für echtes MaryTTS muss vorhanden sein:

```text
tts/marytts/bin/marytts-server.bat
```

oder:

```text
backend/tts/marytts/bin/marytts-server.bat
```

## Standardverhalten

- Browser-/Windows-TTS ist Standard.
- MaryTTS wird nur verwendet, wenn es wirklich installiert und erreichbar ist.
- Fehlendes MaryTTS erzeugt im Normalbetrieb keine sichtbare Warnung mehr.

## Diagnosemodus

```bat
set GAM_MARYTTS_DIAGNOSTICS=true
start-backend.bat
```

oder:

```bat
CHECK_MARYTTS_BUNDLE.bat
```
