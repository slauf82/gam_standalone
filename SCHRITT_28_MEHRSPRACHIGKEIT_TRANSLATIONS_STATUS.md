# Schritt 28 – Mehrsprachigkeit & Translation-System

## Ziel

Rekonstruktion der alten GAM-Mehrsprachigkeit für Oberfläche und PDF-Rechnungen.

## Enthalten

- Nutzung der bestehenden Alt-GAM-Tabellen:
  - `translation_german`
  - `translation_english`
  - `translation_french`
  - `translation_ukrainian`
- Kein neues Translation-Datenmodell.
- Neuer `TranslationService` im Backend.
- PDF-Texte werden über Translation-Schlüssel aufgelöst.
- Sprachabhängige PDF-Titel:
  - Rechnung
  - Stornorechnung
  - Gutschrift
  - Proforma-Rechnung
  - Zahlungsavis
- Separate PDF-Sprachwahl im Rechnungsbereich:
  - Deutsch
  - English
  - Français
  - Українська
- Login-Sprachwahl für die spätere Oberflächenübersetzung vorbereitet.
- Fallback auf deutsche Texte, wenn eine Übersetzung fehlt.

## Noch bewusst offen

- LibreTranslate-Fallback ist in dieser ersten Version noch nicht aktiv geschaltet.
- Gesellschaftsspezifische Textvarianten sind vorbereitet, aber müssen anhand des Alt-Codes weiter rekonstruiert werden.
- Die vollständige Übersetzung der gesamten React-Oberfläche folgt in einem späteren Schritt.

## Hinweis

Diese Version ist als erste testbare Rekonstruktion des Translation-Systems gedacht.
