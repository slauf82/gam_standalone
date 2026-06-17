# Schritt 34q – Translation-Tabellennamen-Fix

## Befund
GAM verwendet die Alt-GAM-Tabellen `translation_german`, `translation_english`, `translation_french`, `translation_ukrainian`.
Die Zwischenstände 34e–34p hatten teilweise versehentlich `translate_*` verwendet.

## Änderung
- UI-Übersetzungen lesen den deutschen Keykatalog aus `translation_german`.
- Zielsprachen schreiben in `translation_italian`, `translation_swedish`, `translation_russian`, `translation_turkish`.
- Schlüssel bleiben im Alt-GAM-Schema `ITALIAN.key`, `SWEDISH.key`, usw.
- Fehlerhafte `translate_GERMAN`-Zwischentabellen werden nur noch optional als Fallback gelesen.
- Browser-TTS aus 34k bleibt unverändert.

## Erwartung
Beim Umschalten auf Italienisch/Schwedisch/Russisch/Türkisch werden die passenden `translation_*` Tabellen gefüllt und die Runtime-Übersetzungen können vom Frontend verwendet werden.
