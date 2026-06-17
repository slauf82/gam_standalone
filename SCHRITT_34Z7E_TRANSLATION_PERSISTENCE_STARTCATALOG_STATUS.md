# Schritt 34z7e – Translation-Persistenz und Startkatalog

Ziel:
- Neue feste UI-/Portal-/Rechnungs-Keys sollen nicht nur live übersetzt werden.
- Sie sollen dauerhaft in den `translation_*` Tabellen landen.
- Technische Keys wie `invoicePortalTitle`, `invoicePortalQrHint` oder `treatmentDate` sollen nicht kurz sichtbar sein.

Änderungen:
- `UiTranslationService` schreibt den gewachsenen deutschen Master-Keykatalog jetzt auch nach `translation_german`.
- Neue feste Keys aus Rechnung/Portal/TTS werden dadurch Teil des Master-Bestands.
- Zielsprachen nutzen weiter DB-first + current-language-only.
- `invoicePortalTitle`, `invoicePortalQrHint`, `invoicePortalHelp` ergänzt.
- Frontend lädt UI-Übersetzungskatalog im Shell-Start ohne 3,5s Verzögerung.
- Portal-Labels haben lokale Fallbacks in allen 8 Sprachen, damit keine technischen Keys sichtbar sind.
- Produktbeschreibungen bleiben ausdrücklich live-only und werden nicht gespeichert.

Hinweis:
- Nach dem ersten Aufruf einer neuen UI-Sprache sollten die Tabellen deutlich über 25 Einträge wachsen.
- Alte Tabellen mit nur 25 Einträgen werden beim Sprachaufruf nachgezogen.
