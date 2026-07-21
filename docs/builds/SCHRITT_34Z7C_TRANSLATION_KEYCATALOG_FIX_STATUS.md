# Schritt 34z7c – Translation-Keykatalog / treatmentDate-Fix

## Ziel

Kleiner Konsolidierungsfix nach 34z7b.

## Änderungen

- `treatmentDate` wird nicht mehr als technischer Key angezeigt, wenn Backend-Labels unvollständig sind.
- Preview-/PDF-Metadatenlabels nutzen einen robusteren Fallback:
  - Backend-Label nur verwenden, wenn es kein technischer Key ist.
  - sonst UI-Keykatalog nach PDF-Sprache verwenden.
- Feste Rechnungs-/Portal-/Metadatenlabels wurden in den Frontend-Keykatalog ergänzt.
- Deutsche Master-UI-Keys im Backend wurden erweitert, damit diese Keys in `translation_<sprache>` dauerhaft angelegt werden können.
- Produktbeschreibungen bleiben bewusst **nur Live-Übersetzung** und werden nicht in die DB übernommen.

## Bewusst unverändert

- Keine Mehrsprachen-Massenpflege.
- Weiterhin DB-first / current-language-only.
- Login-Performance bleibt unangetastet.
- Keine fachliche Rechnungslogik geändert.

## Testhinweis

Bitte insbesondere prüfen:

- UI Englisch / PDF Deutsch: `Behandlungsdatum` statt `treatmentDate`
- UI Italienisch / PDF Deutsch: Rechnungskopf Deutsch, Buttons Italienisch
- DB: neue UI-Keys sollten nach aktivem Sprachaufruf ergänzt werden; Produktbeschreibungen nicht.
