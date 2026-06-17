# Schritt 34b – MaryTTS-Sprachsatz in UI und TTS-Auswahl

Basis: Schritt 34a aus 33k mit Bundled MaryTTS und Frontend-Fix.

## Ziel
Die Oberflächen- und TTS-Sprachauswahl wird auf die Sprachen erweitert, die für MaryTTS/Bundled-TTS relevant sind. Dadurch kann GAM in diesen Sprachen konsistent Sprache, UI-Auswahl und Vorlesefunktion anbieten.

## Ergänzte Sprachen
- Deutsch (`de`)
- Englisch (`en`)
- Französisch (`fr`)
- Ukrainisch (`uk`, mit russischem MaryTTS-Fallback)
- Italienisch (`it`)
- Schwedisch (`sv`)
- Türkisch (`tr`)
- Russisch (`ru`)

## Geänderte Bereiche
- `frontend/src/i18n.ts`
  - `UiLanguage` erweitert
  - `UI_LANGUAGES` erweitert
  - Login-/Basis-Labels für neue Sprachen ergänzt
  - Normalisierung erweitert

- `frontend/src/main.tsx`
  - `GamLanguage` erweitert
  - zentrale `LANGUAGES`-Liste erweitert
  - UI-/TTS-Sprachauswahl nutzt dieselbe Sprachliste
  - TTS-Fallback-Ketten erweitert
  - Browser-Stimmen-Hints erweitert
  - `speechLang(...)` erweitert

- `backend/src/main/java/de/kopfzentrum/gam/tts/TtsService.java`
  - Backend-Fallback-Ketten für `it`, `sv`, `tr`, `ru` ergänzt
  - MaryTTS-Locale-Mapping erweitert

- `frontend/src/api/client.ts`
  - TTS-Status-Typ um Bundled-Statusfelder ergänzt

## Hinweis
Nicht alle Fachtexte sind bereits vollständig professionell übersetzt. Für neue Sprachen greifen fehlende Labels weiterhin auf Deutsch/Englisch zurück. Der technische Sprachpfad ist aber vorbereitet: UI-Auswahl, TTS-Auswahl, Browser-Fallback und MaryTTS-Locale-Mapping sind erweitert.

## Prüfung
Eine syntaktische TS/TSX-Transpile-Prüfung wurde durchgeführt. Ein vollständiger `npm run build` war in der Umgebung nicht möglich, weil die entpackte ZIP keine installierten `node_modules` enthält.
