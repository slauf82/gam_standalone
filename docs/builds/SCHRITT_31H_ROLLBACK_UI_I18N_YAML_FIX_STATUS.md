# Schritt 31h – Rollback auf funktionierende UI-i18n + YAML-Fix

## Anlass

Schritt 31f/31g hat zwar den YAML-Fehler behoben, war aber funktional ein Rückschritt:
Die zuvor teilweise funktionierende Login-/Oberflächenübersetzung aus 31e griff nicht mehr.

## Fix

Dieser Stand basiert wieder auf:

```text
Schritt 31e – Login-i18n inklusive Modulnamen
```

und übernimmt nur die sichere Bereinigung gegen doppelte YAML-`app:`-Blöcke.

## Enthalten

- funktionierende Frontend-Übersetzung aus 31e wiederhergestellt
- Modulnamen im Login weiterhin übersetzbar
- Logintexte weiterhin übersetzbar
- kein neuer UI-Translation-Cache, der die Anzeige überlagert
- keine doppelte YAML-`app:`-Konfiguration
- PDF-Sprache im Rechnungsprogramm bleibt separat

## Nicht enthalten

- der experimentelle Backend-UI-Translation-Cache aus 31f
- LibreTranslate-Autocache für die gesamte Oberfläche

## Empfehlung

Erst die statische UI-i18n stabilisieren.
Danach kann ein Backend-Translation-Cache erneut gezielt und kleiner eingebaut werden.
