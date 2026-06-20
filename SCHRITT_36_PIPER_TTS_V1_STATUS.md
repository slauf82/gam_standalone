# GAM 2.0 v1.7.0 – Schritt 36 – PiperTTS v1

## Status

Schritt 36 ersetzt den MaryTTS-Versuch aus Schritt 35 vollständig durch PiperTTS.

MaryTTS wird in der aktiven Backend- und Frontend-Logik nicht mehr angeboten.

## Zielarchitektur

- PiperTTS ist die Standard-Vorlesetechnik.
- Browser Speech bleibt als Fallback erhalten.
- UI-Sprache und Vorlesesprache bleiben gekoppelt.
- Deutsch, Englisch und Französisch sind das Standardpaket.
- Weitere Sprachen werden bei Bedarf automatisch nachinstalliert.

## Standardpaket

Diese Stimmen werden durch `INSTALL_PIPER_TTS.bat` vorbereitet:

| Sprache | Piper-Voice |
|---|---|
| Deutsch | `de_DE-thorsten-medium` |
| Englisch | `en_US-lessac-medium` |
| Französisch | `fr_FR-siwis-medium` |

## Automatische Nachinstallation

Wenn eine andere UI-/Vorlesesprache genutzt wird, prüft das Backend beim ersten Vorleseversuch:

1. Ist `piper.exe` vorhanden?
2. Ist die passende `.onnx`-Datei vorhanden?
3. Ist die passende `.onnx.json`-Datei vorhanden?
4. Falls nein: Download im Hintergrund starten.
5. Bis der Download fertig ist: Browser-Fallback verwenden.

Automatische Sprachen:

| Sprache | Piper-Voice |
|---|---|
| Italienisch | `it_IT-paola-medium` |
| Spanisch | `es_ES-sharvard-medium` |
| Portugiesisch | `pt_PT-tugao-medium` |
| Niederländisch | `nl_NL-ronnie-medium` |
| Polnisch | `pl_PL-gosia-medium` |
| Tschechisch | `cs_CZ-jirka-low` |
| Schwedisch | `sv_SE-nst-medium` |
| Türkisch | `tr_TR-dfki-medium` |
| Russisch | `ru_RU-ruslan-medium` |
| Ukrainisch | `uk_UA-ukrainian_tts-medium` |

## Backend

Geändert:

- `de.kopfzentrum.gam.tts.TtsService`
- `de.kopfzentrum.gam.tts.TtsController`
- `de.kopfzentrum.gam.tts.TtsStatus`
- `application.yml`
- `application-local.yml`

Neue Statusfelder:

- `installedLanguages`
- `supportedLanguages`
- `standardLanguages`
- `downloadingLanguages`
- `downloadErrors`
- `autoDownload`

Neuer Endpoint:

```text
POST /api/tts/install/{language}
```

Der Endpoint kann eine Voice-Installation gezielt starten. Im normalen Ablauf ist er nicht notwendig, weil `/api/tts/audio` fehlende Voices automatisch anstößt.

## Frontend

Geändert:

- `frontend/src/api/client.ts`

Das Frontend kann den erweiterten TTS-Status lesen. Die bestehende Piper-/Browser-Fallback-Kette bleibt kompatibel:

- Piper verfügbar und Voice installiert → Piper liest vor.
- Voice fehlt → Backend startet Download und antwortet kurz mit `503`.
- Frontend fällt auf BrowserTTS zurück.
- Nach erfolgreichem Download nutzt der nächste Vorleseversuch Piper.

## Skripte

Geändert/ergänzt:

- `INSTALL_PIPER_TTS.bat`
- `CHECK_PIPER_TTS.bat`
- `scripts/install-piper-standard-voices.ps1`
- `tts/piper/README_PIPER_TTS.md`

## Konfiguration

```yaml
app:
  tts:
    enabled: true
    engine: piper
    browser-fallback: true
    piper:
      home: ./tts/piper
      executable: ./tts/piper/piper.exe
      voices-dir: ./tts/piper/voices
      auto-download: true
```

## Testablauf

1. `INSTALL_PIPER_TTS.bat` ausführen.
2. Backend starten.
3. `GET http://localhost:8080/api/tts/status` prüfen.
4. Rechnungsvorschau öffnen.
5. Vorlesen in Deutsch, Englisch oder Französisch testen.
6. UI-/PDF-Sprache z. B. auf Polnisch stellen.
7. Vorlesen starten.
8. Erster Versuch kann Browser-Fallback nutzen, während der Download läuft.
9. Danach sollte Polnisch über Piper laufen.

## Ergebnis

Schritt 36 liefert die gewünschte Mischform:

- keine große Erstinstallation,
- keine Engine-Auswahl für MaryTTS,
- keine MaryTTS-JAR-/Voice-Probleme,
- Standardsprachen sofort nutzbar,
- weitere Sprachen automatisch nachrüstbar,
- Browser-Fallback bleibt als Sicherheitsnetz erhalten.

## Korrektur 36.1a - Piper Engine Download

Die erste Schritt-36-Version konnte auf Systemen scheitern, weil versucht wurde,
Piper ueber eine einzelne `piper.exe` bzw. ueber Python/pip bereitzustellen.
Das reicht unter Windows nicht zuverlaessig aus.

Korrigiert:

- `INSTALL_PIPER_TTS.bat` nutzt jetzt `scripts/install-piper-standard-voices.ps1`.
- Das PowerShell-Skript laedt das komplette offizielle Windows-Paket:
  `piper_windows_amd64.zip`.
- Das ZIP wird entpackt und alle Begleitdateien werden nach `tts/piper/` kopiert.
- Danach werden die Standardstimmen Deutsch, Englisch und Franzoesisch geladen.
- Zum Abschluss wird ein echter WAV-Test mit `de_DE-thorsten-medium` ausgefuehrt.
- `/api/tts/status` enthaelt jetzt zusaetzlich Diagnoseinformationen:
  - `piperAvailabilityMessage`
  - `piperDiagnostics`

Erwarteter Ordner nach erfolgreicher Installation:

```text
tts/
  piper/
    piper.exe
    onnxruntime.dll
    espeak-ng-data/
    voices/
      de_DE-thorsten-medium.onnx
      de_DE-thorsten-medium.onnx.json
      en_US-lessac-medium.onnx
      en_US-lessac-medium.onnx.json
      fr_FR-siwis-medium.onnx
      fr_FR-siwis-medium.onnx.json
```

Piper gilt erst dann als verfuegbar, wenn nicht nur `piper.exe`, sondern auch
Begleitdateien wie `onnxruntime.dll` oder `espeak-ng-data` vorhanden sind.

## Fix 2 – Piper-Auswahl nicht mehr zu streng ausgrauen

Korrektur nach Test:

- `piperAvailable` wird nicht mehr von exakt `onnxruntime.dll` oder `espeak-ng-data` abhängig gemacht.
- Wenn `tts/piper/piper.exe` vorhanden ist, wird Piper in der UI grundsätzlich als auswählbar behandelt.
- Der echte Funktionstest bleibt die WAV-Erzeugung über `CHECK_PIPER_TTS.bat` bzw. `/api/tts/audio`.
- Auto-Download-Sprachen werden in der UI nicht mehr als „nicht verfügbar“ ausgegraut, solange Piper vorhanden ist und die Sprache unterstützt wird.
- Die Statusdiagnose zeigt nun zusätzlich die Einträge im Piper-Ordner, um falsche ZIP-/Unterordner-Strukturen leichter zu erkennen.

Wichtig nach Installation/Korrektur:

1. `INSTALL_PIPER_TTS.bat` ausführen.
2. `CHECK_PIPER_TTS.bat` ausführen.
3. Backend neu starten.
4. Browser-Frontend neu laden, ggf. mit Strg+F5.

