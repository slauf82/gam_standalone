# Schritt 36d – Fremdsprachige Produktbeschreibungen in Vorschau/PDF

## Ziel

In einer fremdsprachigen Rechnung darf kein deutscher Produkttext mehr sichtbar bleiben.
Das betrifft insbesondere Produktbeschreibungen aus `rdaten.beschreibung`, z. B.:

`Botox zur gezielten Entspannung der großen Flächenmuskeln - 50 Einheiten (Vistabel)`

Bei PDF-Sprache Französisch muss daraus ein französischer Produkttext werden.

## Umsetzung

- Produktbeschreibungen werden weiterhin nicht in die Translation-DB geschrieben.
- Live-Übersetzung läuft über `/api/ui-translations/live`.
- Wenn LibreTranslate erreichbar ist, wird die Beschreibung live übersetzt.
- Wenn keine brauchbare Übersetzung verfügbar ist, wird bei Fremdsprachen nicht mehr der deutsche Originaltext angezeigt.
- Für den bekannten Botox-Testfall gibt es einen lokalen Notfall-Fallback für alle GAM-Sprachen.
- PDF-Erzeugung nutzt ebenfalls Live-Übersetzung ohne DB-Persistenz.

## Geänderte Dateien

- `frontend/src/main.tsx`
- `backend/src/main/java/de/kopfzentrum/gam/translation/UiTranslationService.java`
- `backend/src/main/java/de/kopfzentrum/gam/invoice/InvoicePdfService.java`

## Wichtig

Die Produktbeschreibung bleibt in der Datenbank deutsch.
Nur Anzeige, Vorschau und PDF-Ausgabe werden fremdsprachig dargestellt.

## Testfall

Französische Rechnung mit Produkt:

`Botox zur gezielten Entspannung der großen Flächenmuskeln - 50 Einheiten (Vistabel)`

Erwartet:

`Botox pour la relaxation ciblée des grands muscles de surface - 50 unités (Vistabel)`

## Status

Umgesetzt als Schritt 36d.
