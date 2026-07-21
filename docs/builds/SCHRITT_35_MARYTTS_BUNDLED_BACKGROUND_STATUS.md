# ÜBERHOLT – ersetzt durch Schritt 35 Piper TTS v1

Dieser alte MaryTTS-Schritt ist durch `SCHRITT_35_PIPER_TTS_V1_STATUS.md` ersetzt. MaryTTS wird in GAM v1.7.0 nicht mehr als Vorlesetechnik angeboten.

---

# Schritt 35 – MaryTTS Bundled Background Runtime

Status: Testversion für GAM 2.0 v1.7.0

## Ziel

MaryTTS soll nicht mehr manuell vorbereitet werden müssen, sondern vom Backend als optionaler Nebenprozess behandelt werden:

1. Backend startet sofort weiter.
2. BrowserTTS bleibt sofort als Fallback verfügbar.
3. MaryTTS wird im Hintergrund geprüft.
4. Fehlt MaryTTS, wird die Runtime im Hintergrund heruntergeladen und installiert.
5. Das problematische Luxembourgish-Modul (`*lang-lb*.jar`) wird automatisch deaktiviert.
6. MaryTTS wird auf Port `59125` gestartet.
7. Sobald erreichbar, meldet `/api/tts/status` `maryTtsReachable=true`.

## Wichtige Eigenschaften

- Backend-Start wird nicht durch MaryTTS blockiert.
- Fehlende oder fehlerhafte MaryTTS-Installation verhindert den GAM-Start nicht.
- BrowserTTS bleibt immer als Fallback aktiv.
- Manuelles Installationsskript wurde mit Direktlink, Größenprüfung, ZIP-Signaturprüfung und `lang-lb`-Deaktivierung korrigiert.

## Relevante Statusfelder

`/api/tts/status` liefert zusätzlich:

- `maryTtsBundledInstalled`
- `maryTtsBundledInstalling`
- `maryTtsBundledStarting`
- `maryTtsBundledAutoInstall`
- `maryTtsBundledStarted`
- `maryTtsBundledAvailable`
- `maryTtsReachable`

## Konfiguration

```yaml
app:
  tts:
    marytts:
      autostart: true
      auto-install: true
      home: ./tts/marytts
      port: 59125
```

Environment-Overrides:

- `GAM_MARYTTS_AUTOSTART=false`
- `GAM_MARYTTS_AUTO_INSTALL=false`
- `GAM_MARYTTS_HOME=...`
- `GAM_MARYTTS_PORT=59125`
- `GAM_MARYTTS_DOWNLOAD_URL=...`

## Test

1. `tts/marytts` ggf. löschen oder leer lassen.
2. Backend starten.
3. Backend muss sofort weiter starten.
4. Nach kurzer Zeit `http://localhost:59125` prüfen.
5. `/api/tts/status` prüfen.

Alternativ:

```bat
INSTALL_MARYTTS_BUNDLE.bat
CHECK_MARYTTS_BUNDLE.bat
```
