# Schritt 34z7f – Preview-/TTS-Key-Alias-Fix

Stand: 2026-06-17

## Ziel

- `invoicePreviewHelp` darf nicht mehr als technischer Key sichtbar werden.
- `accessibilityNote` darf nicht mehr als technischer Key sichtbar werden.
- `readAloud` / `stopReading` müssen sofort übersetzt erscheinen.
- Der große Vorlesen-Button bekommt größere, zentrierte Schrift.

## Umsetzung

- `invoicePreviewHelp` wurde in den deutschen Master-Keykatalog aufgenommen.
- Frontend-Alias-Keys werden beim Start aus vorhandenen Übersetzungen ergänzt.
- `tui(...)` nutzt jetzt den Fallback, wenn `ui(...)` nur den Key selbst zurückliefert.
- Button-Schriftgröße wurde erhöht.

## Abgrenzung

- Produktbeschreibungen bleiben live-only und werden nicht in die DB geschrieben.
- Keine neue Mehrsprachen-Massenpflege.
