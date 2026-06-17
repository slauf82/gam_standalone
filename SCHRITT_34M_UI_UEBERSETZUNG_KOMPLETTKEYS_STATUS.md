# Schritt 34m – UI-Übersetzung: kompletter deutscher Quelltextbestand

## Ziel
Die neuen Oberflächensprachen dürfen nicht nur einzelne TTS-Schlüssel übersetzen.
Beim ersten Aufruf sollen alle bekannten deutschen UI-Quelltexte gesammelt, per
libretranslate-java/LibreTranslate-REST übersetzt und in den Zieltabellen gespeichert werden.

## Änderungen

- `frontend/src/i18n.ts`
  - `germanUiEntries()` ergänzt, damit auch die Basis-i18n-Schlüssel aus `i18n.ts` an die Runtime-Übersetzung gehen.

- `frontend/src/main.tsx`
  - `collectUiTranslationEntries()` sammelt jetzt:
    - `i18n.ts` German Dictionary
    - `UI_LABELS.de`
    - `GAM_UI_LABELS.de`
    - `UI_TEXT.de`
    - zentrale Modul-/Navigationslabels als stabile `module.*`-Keys
    - wichtige Readonly-Beschreibungen
  - `moduleText()` nutzt zuerst Runtime-Übersetzungen aus `module.*`, danach Fallback.

## Erwartetes Verhalten

Bei Auswahl von Italienisch, Schwedisch, Russisch oder Türkisch:

1. Deutscher UI-Text wird nur als Quelle verwendet.
2. Zieltext wird über DB-Cache oder LibreTranslate erzeugt.
3. Zieltext wird in `translate_ITALIAN`, `translate_SWEDISH`, `translate_RUSSIAN` oder `translate_TURKISH` gespeichert.
4. Die UI übernimmt die Runtime-Übersetzungen.

## Stabilitätsentscheidung

Browser-TTS aus Schritt 34k bleibt unverändert stabil. MaryTTS bleibt weiterhin deaktiviert/optional, bis das Bundle separat sauber gelöst wird.
