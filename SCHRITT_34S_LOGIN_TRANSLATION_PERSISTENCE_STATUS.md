# Schritt 34s – LoginDialog-Übersetzungen persistieren

Basis: Schritt 34r / stabile Browser-TTS-Logik aus 34k.

## Ziel

Der LoginDialog darf keine Live-/Fallback-Übersetzungen anzeigen, ohne diese auch in der passenden `translation_<sprache>`-Tabelle zu speichern.

## Änderungen

- `UiTranslationRequest` akzeptiert jetzt zusätzlich `knownTranslations`.
- Das Frontend sendet pro Ziel-Sprache bekannte, bereits sichtbare Zieltexte mit.
- Das Backend persistiert diese Texte im Alt-GAM-Schema `SPRACHE.key`.
- Modul-Auswahlbuttons im LoginDialog werden als `module.*`-Keys mitgesendet.
- Betroffen sind insbesondere `translation_italian`, `translation_swedish`, `translation_turkish`, `translation_russian`.
- Browser-TTS bleibt unverändert wie in Schritt 34k.

## Erwartung

Nach Auswahl von Italienisch/Schwedisch/Türkisch/Russisch sollten nicht nur sichtbare Texte erscheinen, sondern auch passende Einträge in den jeweiligen Tabellen landen.
