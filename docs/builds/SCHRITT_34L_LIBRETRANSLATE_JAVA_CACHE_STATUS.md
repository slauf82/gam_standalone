# Schritt 34l – UI-Übersetzungen über libretranslate-java/REST und DB-Cache

## Ziel

Die neuen UI-Sprachen dürfen nicht mehr dauerhaft mit deutschen Fallback-Texten gefüllt werden.
Der deutsche Text ist nur die Quelle. Für `it`, `sv`, `ru`, `tr`, `fr`, `en`, `uk` wird beim ersten Aufruf übersetzt und anschließend in der jeweiligen `translate_*`-Tabelle gespeichert.

## Umsetzung

- Browser-TTS aus Schritt 34k bleibt unverändert stabil.
- `UiTranslationService` nutzt zuerst eine vorhandene `libretranslate-java`-JAR im Classpath (`space.dynomake.libretranslate.Translator`).
- Es gibt keine harte Maven-Abhängigkeit, damit der Build nicht an einem externen Repository scheitert.
- Falls die JAR nicht vorhanden ist, wird derselbe LibreTranslate-REST-Weg genutzt.
- Zuerst wird lokal `http://localhost:5000/translate` probiert.
- Danach wird als schlanker Online-Fallback `https://translate.fedilab.app/translate` probiert.
- Deutsche Cache-Einträge in Zieltabellen werden nicht mehr als gültige Übersetzung akzeptiert.
- Wenn ein bestehender Eintrag identisch mit dem deutschen Quelltext ist, wird neu übersetzt und per UPDATE ersetzt.

## Konfiguration

```yaml
app:
  translation:
    libretranslate:
      enabled: true
      url: http://localhost:5000/translate
      fallback-url: https://translate.fedilab.app/translate
      api-key: ""
      java-client-enabled: true
      diagnostics: false
```

## Erwartung

Beim ersten Umschalten auf Italienisch entstehen echte italienische Texte in `translate_ITALIAN`.
Folgeaufrufe lesen die Datenbank und sind dadurch schnell.
