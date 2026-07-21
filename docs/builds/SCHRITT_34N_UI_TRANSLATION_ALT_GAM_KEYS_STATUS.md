# Schritt 34n – UI-Übersetzung mit Alt-GAM-kompatiblen Keys

Basis: Schritt 34m / stabile Browser-TTS-Basis aus 34k.

## Ziel

Die UI-Übersetzung nutzt wieder das bekannte Alt-GAM-Key-Schema:

```text
GERMAN.key
ENGLISH.key
FRENCH.key
UKRAINIAN.key
ITALIAN.key
SWEDISH.key
RUSSIAN.key
TURKISH.key
```

Nicht mehr:

```text
ITALIAN.UI.key
```

## Änderungen

- `UiTranslationService` nutzt `LANGUAGE.key` statt `LANGUAGE.UI.key`.
- Bestehende `translate_GERMAN`-Einträge werden als zusätzliche deutsche Quelle gelesen.
- Alte `LANGUAGE.UI.key`-Einträge werden weiterhin gelesen und bei Brauchbarkeit nach `LANGUAGE.key` übernommen.
- Deutsche Zielwerte in nicht-deutschen Tabellen gelten nicht als gültige Übersetzung und werden neu übersetzt.
- Browser-TTS bleibt unverändert stabil.

## Erwartetes Verhalten

Beim Wechsel auf Italienisch entstehen bzw. aktualisieren sich Einträge wie:

```text
translate_ITALIAN / ITALIAN.loginTitle
translate_ITALIAN / ITALIAN.invoiceSearch
translate_ITALIAN / ITALIAN.ttsMary
```

Der deutsche Text dient nur als Quelle und wird nicht mehr als fertiger Zieltext gespeichert.
