# Schritt 34e – UI-Sprache über LibreTranslate/DB-Cache

## Ziel

Die in Schritt 34b sichtbaren neuen Oberflächensprachen sollen nicht nur auswählbar sein, sondern beim ersten Aufruf über LibreTranslate erzeugt und anschließend lokal in der Datenbank gecacht werden.

## Fehlerursache

Die Sprachliste enthielt bereits z. B. `it`, `sv`, `tr`, `ru`, aber die UI-Texte wurden noch aus statischen Frontend-Fallbacks gelesen. Für Italienisch gab es daher zwar eine auswählbare Sprache, aber keine geladene Runtime-Übersetzung. Deshalb blieb die Oberfläche faktisch deutsch.

## Änderung

- Neuer Backend-Endpunkt: `POST /api/ui-translations`
- Neue Backend-Klassen:
  - `UiTranslationController`
  - `UiTranslationService`
- Beim ersten Aufruf einer UI-Sprache wird eine Sprach-Tabelle angelegt, z. B.:
  - `translate_ITALIAN`
  - `translate_SWEDISH`
  - `translate_TURKISH`
  - `translate_RUSSIAN`
- Fehlende Einträge werden über LibreTranslate von Deutsch in die Ziel-Sprache übersetzt.
- Die Ergebnisse werden mit `TRANSLATE_DESCRIPTION` und `TRANSLATED_TEXT` gespeichert.
- Folgeaufrufe lesen aus der Datenbank und sind dadurch deutlich schneller.
- Wenn LibreTranslate nicht erreichbar ist, bleibt GAM bedienbar und verwendet Fallbacktexte.

## Konfiguration

Standardwerte:

```yaml
app:
  translation:
    libretranslate:
      enabled: true
      url: http://localhost:5000/translate
```

Per Environment überschreibbar:

```text
GAM_LIBRETRANSLATE_ENABLED=true
GAM_LIBRETRANSLATE_URL=http://localhost:5000/translate
```

## Erwartetes Verhalten

1. Oberflächensprache auf Italienisch stellen.
2. Frontend ruft `/api/ui-translations` auf.
3. Backend erstellt bei Bedarf `translate_ITALIAN`.
4. LibreTranslate erzeugt die fehlenden italienischen Texte.
5. Die UI wird mit den gecachten Übersetzungen neu gerendert.
6. Beim nächsten Aufruf kommen die Texte direkt aus der Datenbank.

## Hinweis

Diese Änderung betrifft die Oberflächensprache. Die PDF-Sprache bleibt weiterhin separat steuerbar.
