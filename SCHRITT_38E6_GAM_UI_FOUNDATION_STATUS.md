# Schritt 38e6 – GAM UI Foundation

## Ziel

Schritt 38e6 legt die wiederverwendbare UI-Grundlage für die kommenden GAM-Module an.
Die Bedienmuster aus 38e3 bis 38e5 werden damit als zentrale Bausteine vorbereitet.

## Enthalten

- `GamDialog`
  - modaler Dialog
  - abgedunkelter Hintergrund
  - ESC = Abbrechen/Schließen
  - Fokus auf erstes Eingabefeld möglich
  - Größenvarianten: small, medium, large, wide

- `GamStickyToolbar`
  - einheitliche sticky Aktionsleiste
  - für Neu/Suche/Filter/Bearbeiten

- `GamScrollArea`
  - einheitlicher Scrollbereich für lange Listen und Tabellen

- Notification-Helfer
  - `notifySaved(...)`
  - `notifySaveError(...)`

## Zweck

Neue Module wie Schritt 38f und Schritt 38g sollen künftig direkt diese UI-Bausteine verwenden.
Dadurch entsteht ein konsistentes Bedienkonzept:

- Übersicht bleibt Übersicht
- Bearbeiten erfolgt im Dialog
- Suche und Aktionen bleiben sichtbar
- Speichern erzeugt Toast-Meldungen
- Fehler bleiben nachvollziehbar

## Technische Hinweise

- Keine Backend-Änderungen
- Keine Datenbank-Änderungen
- Keine API-Änderungen
- Bestehende Module bleiben funktionsgleich
- Die neuen Komponenten sind bewusst als Grundlage für die nächsten Schritte vorbereitet

## Testempfehlung

1. Frontend bauen/starten.
2. Geräteverzeichnis öffnen.
3. Lagerverwaltung öffnen.
4. Modul-Administration öffnen.
5. Prüfen, dass die bisherigen Dialoge, Toasts und Sticky-Leisten unverändert funktionieren.
6. Schritt 38f/38g anschließend auf Basis der neuen UI-Bausteine entwickeln.
