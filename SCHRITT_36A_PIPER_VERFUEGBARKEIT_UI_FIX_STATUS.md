# Schritt 36a – Piper Verfügbarkeits- und UI-Fix

## Ausgangslage

Nach Schritt 36 wurde Piper trotz vorhandener und manuell funktionsfähiger `tts/piper/piper.exe` in der Oberfläche weiterhin als `Piper (nicht verfügbar)` angezeigt und war ausgegraut.

## Ursache

Die Erkennung war noch zu eng:

- Backend prüfte primär den konfigurierten relativen Pfad `./tts/piper/piper.exe`.
- Je nach Startkontext kann das Arbeitsverzeichnis aber vom erwarteten Projektwurzelordner abweichen.
- Die UI sperrte Piper hart, wenn `/api/tts/status` `piperAvailable=false` meldete.
- Für Auto-Download-Sprachen war die UI ebenfalls zu streng, weil Piper erst bei bereits installierter Stimme als auswählbar galt.

## Änderungen

### Backend

`TtsService` prüft jetzt mehrere plausible Piper-Pfade:

- konfigurierter Pfad
- `tts/piper/piper.exe`
- `../tts/piper/piper.exe`
- `../../tts/piper/piper.exe`
- absolute Varianten aus dem aktuellen Arbeitsverzeichnis

Außerdem werden Voice-Verzeichnisse ebenfalls robuster erkannt:

- konfiguriertes `voices-dir`
- `tts/piper/voices`
- `../tts/piper/voices`
- Verzeichnis relativ zur gefundenen `piper.exe`

Der tatsächlich gefundene Pfad wird für den Audio-Aufruf verwendet.

### Diagnose

`/api/tts/status` liefert jetzt zusätzlich:

- `piperDiagnostics.workingDirectory`
- `piperDiagnostics.configuredPiperExecutable`
- `piperDiagnostics.resolvedPiperExecutable`
- `piperDiagnostics.checkedExecutablePaths`
- `piperDiagnostics.configuredVoicesDir`
- `piperDiagnostics.resolvedVoicesDir`
- `piperDiagnostics.checkedVoiceDirs`

Damit ist sofort sichtbar, wo das Backend wirklich sucht.

### Frontend

Piper wird nicht mehr hart ausgegraut, nur weil die Engine-Erkennung noch nicht erfolgreich war.

Wenn die Sprache von Piper unterstützt wird, bleibt Piper auswählbar. Falls Engine oder Voice beim Abspielen fehlen, greift die bestehende Logik:

1. Auto-Download starten, sofern möglich
2. BrowserTTS als Fallback verwenden

## Ergebnis

Schritt 36a behebt die Situation:

- `piper.exe` vorhanden, aber Backend-Arbeitsverzeichnis anders
- Voice-Dateien vorhanden, aber relativer Pfad anders
- Piper in UI fälschlich ausgegraut

## Test

1. `INSTALL_PIPER_TTS.bat` ausführen
2. `CHECK_PIPER_TTS.bat` ausführen
3. Backend neu starten
4. Frontend mit `Strg+F5` neu laden
5. `http://localhost:8080/api/tts/status` prüfen
6. In den TTS-Einstellungen Piper auswählen

Wichtig bei Fehlern: Den Inhalt von `/api/tts/status` prüfen, insbesondere `piperDiagnostics.checkedExecutablePaths` und `piperDiagnostics.resolvedPiperExecutable`.
