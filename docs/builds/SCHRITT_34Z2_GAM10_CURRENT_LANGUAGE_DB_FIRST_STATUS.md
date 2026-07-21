# Schritt 34z2 – GAM-1.0-Logik für UI-Übersetzungen

Ziel: Die Mehrsprachigkeit folgt wieder der bewährten GAM-1.0-Regel.

## Regel

- `translation_german` ist die Master-Keyliste.
- Die Oberfläche fragt nur die aktuell gewählte UI-Sprache an.
- Für diese Sprache gilt:
  1. Erst DB-Cache prüfen.
  2. Wenn ein brauchbarer Zieltext vorhanden ist, wird dieser verwendet.
  3. Wenn der Key fehlt oder noch deutschen Text enthält, wird nur für diese aktive Sprache LibreTranslate genutzt.
  4. Das Ergebnis wird in `translation_<sprache>` gespeichert.
- Andere Sprachen werden nicht nebenbei übersetzt oder befüllt.
- Login und Modulwechsel bleiben schnell; Übersetzungspflege läuft nur für die aktive Sprache im Hintergrund.

## Ergebnis

Beispiel Italienisch:

- UI-Sprache = Italienisch
- Quelle = `translation_german`
- Cache = `translation_italian`
- Fehlende Keys werden als `ITALIAN.<key>` nachgezogen

Beispiel Schwedisch:

- UI-Sprache = Schwedisch
- Quelle = `translation_german`
- Cache = `translation_swedish`
- Fehlende Keys werden als `SWEDISH.<key>` nachgezogen

## Wichtig

Dieser Schritt macht keine Mehrsprachen-Massenpflege mehr. Jede Sprache wird erst dann gepflegt, wenn sie wirklich als Oberflächensprache aktiv ist.

Browser-TTS bleibt unverändert wie in Schritt 34k.
