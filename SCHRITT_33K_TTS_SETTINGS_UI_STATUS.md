# Schritt 33k – TTS-Einstellungen in der UI

## Ziel

Die bisherige Vorlesesprache war nur eine Sprachwahl. Jetzt gibt es echte Vorleseinstellungen.

## Rechnungsvorschau

Ergänzt wurden:

* Vorlesesprache
  * Automatisch
  * Deutsch
  * English
  * Français
  * Українська
* Vorlesetechnik
  * Automatisch
  * Browser/Windows
  * MaryTTS
* Stimme
  * Automatische Stimme
  * verfügbare Browser-/Windows-Stimmen passend zur Sprache
* Geschwindigkeit
  * 0.60x bis 1.50x

## Verhalten

* `Automatisch` nutzt PDF-Sprache, danach Oberflächensprache.
* Browser/Windows erlaubt männliche/weibliche Stimmen, soweit im System vorhanden.
* MaryTTS nutzt Backend-TTS.
* Bei Geschwindigkeit wird `utterance.rate` gesetzt.

## Patientenportal

Die gemeinsame Komponente `TtsSettingsControls` ist vorbereitet, damit dieselben Einstellungen auch im Patientenportal eingebunden werden können.
Falls das Portal im aktuellen `main.tsx` separat gerendert wird, sollte dort dieselbe Komponente mit Scope `patient-portal` eingebaut werden.
