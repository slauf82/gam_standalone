# Schritt 33j – TTS UI-, Status- und Sprachfix

## Fixes

* `/api/tts/status` für Diagnose freigegeben
* Auswahlfeld `Vorlesesprache` in der Rechnungsvorschau ergänzt
* `Automatisch` nutzt PDF-Sprache, danach Oberflächensprache
* Vorleselogik nutzt `effectiveTtsLanguage(...)`
* MaryTTS-Bundle-Hinweis als optionalen Hinweis entschärft

## Gepatchte Security-Dateien

```text
backend/src/main/java/de/kopfzentrum/gam/config/SecurityConfig.java
```
