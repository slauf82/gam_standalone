# GAM 2.0 Schritt 36 - PiperTTS

Piper ist ab Schritt 36 die einzige Backend-Vorlesetechnik. MaryTTS wird nicht mehr angeboten.

## Standardpaket

`INSTALL_PIPER_TTS.bat` richtet Piper ein und laedt diese Standardstimmen:

- Deutsch: `de_DE-thorsten-medium`
- Englisch: `en_US-lessac-medium`
- Franzoesisch: `fr_FR-siwis-medium`

## Automatische Nachinstallation

Wenn die UI-/Vorlesesprache auf eine andere Sprache gewechselt wird, startet das Backend beim ersten Vorleseversuch automatisch den Voice-Download im Hintergrund. Solange die Stimme noch fehlt, nutzt das Frontend den Browser-Fallback.

Geplante Auto-Download-Sprachen:

- Italienisch `it_IT-paola-medium`
- Spanisch `es_ES-sharvard-medium`
- Portugiesisch `pt_PT-tugao-medium`
- Niederlaendisch `nl_NL-ronnie-medium`
- Polnisch `pl_PL-gosia-medium`
- Tschechisch `cs_CZ-jirka-low`
- Schwedisch `sv_SE-nst-medium`
- Tuerkisch `tr_TR-dfki-medium`
- Russisch `ru_RU-ruslan-medium`
- Ukrainisch `uk_UA-ukrainian_tts-medium`

## Status

`GET /api/tts/status` zeigt:

- `installedLanguages`
- `supportedLanguages`
- `standardLanguages`
- `downloadingLanguages`
- `downloadErrors`
- `autoDownload`
