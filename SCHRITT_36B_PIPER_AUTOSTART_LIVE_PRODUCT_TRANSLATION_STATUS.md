# Schritt 36b - Piper Autostart und Live-Produktbeschreibung

## Ziel

Schritt 36b stabilisiert den erfolgreichen Schritt 36a:

- `start-backend.bat` soll PiperTTS automatisch vorbereiten, falls Piper oder die Standardstimmen fehlen.
- Der normale Start soll weiterhin nur aus `start-backend.bat` und `start-frontend.bat` bestehen.
- Produktbeschreibungen werden live in die gewählte Rechnungs-/PDF-Sprache übersetzt.
- Produktbeschreibungsübersetzungen werden **nicht** in die Datenbank geschrieben.

## Änderungen

### Backend-Start

`start-backend.bat` prüft jetzt vor dem Backend-Build:

- `tts/piper/piper.exe`
- `tts/piper/voices/de_DE-thorsten-medium.onnx`
- `tts/piper/voices/en_US-lessac-medium.onnx`
- `tts/piper/voices/fr_FR-siwis-medium.onnx`

Wenn etwas fehlt, wird automatisch ausgeführt:

```bat
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\install-piper-standard-voices.ps1"
```

Bei einem Fehler startet das Backend trotzdem, damit Browser-TTS als Fallback verfügbar bleibt.

### Piper-Verhalten

- de/en/fr werden als Standardpaket vorbereitet.
- Weitere Sprachen werden weiterhin bei Bedarf automatisch nachgeladen.
- Der erste Test einer neuen Sprache kann Browser-Fallback sein.
- Nach erfolgreichem Download nutzt der zweite Test die lokale Piper-Stimme.

### Live-Produktbeschreibung

Im Rechnungseditor und in der Vorschau werden Produktbeschreibungen jetzt über `/api/ui-translations/live` übersetzt.

Wichtig:

- keine Persistenz in `translation_*`
- keine neuen DB-Einträge für Produkttexte
- Anzeige und Vorlesen verwenden die Live-Übersetzung, sobald verfügbar
- deutscher Originaltext bleibt Fallback

## Testplan

1. Frische Installation ohne `tts/piper` starten.
2. `start-backend.bat` ausführen.
3. Prüfen, ob Piper automatisch installiert wird.
4. `start-frontend.bat` ausführen.
5. Rechnungsprogramm öffnen.
6. PDF-/Rechnungssprache auf Italienisch oder Polnisch stellen.
7. Produktbeschreibung in Editor/Vorschau prüfen.
8. Vorlesen testen.
9. Prüfen: Produktbeschreibung darf nicht in `translation_*` persistiert werden.

## Status

Testpaket für lokalen Windows-Test gebaut.
