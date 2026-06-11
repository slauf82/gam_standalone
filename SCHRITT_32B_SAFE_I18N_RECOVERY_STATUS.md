# Schritt 32b – Safe i18n Recovery

## Basis

Schritt 32, nicht 32a.

## Warum

32a war zu aggressiv und führte zu einem weißen Bildschirm. Dieser Stand verwirft 32a vollständig und übernimmt nur sichere Minimalfixes.

## Fixes

- fehlende i18n-Keys ergänzt:
  - `newInvoice`
  - `invoiceSearch`
  - `checks`
  - `reports`
  - `usersRights`
  - `cancelInvoice`
  - `paymentAdvice`
  - `invoiceTypePaymentAdvice`
- `uiSafe(...)` ergänzt, um sichtbare technische Keys sicher abzufangen
- Modul-Mapping für `compliance/users/Prüfungen/Benutzer/Rechte` ergänzt
- sehr eng begrenzter Fix für Storno-Button
- sehr eng begrenzter Fix für `Payment advice`
- ZUGFeRD-Status bleibt an `ui(...)`

## Wichtig

Keine globalen Textersetzungen.
Keine Änderung der Navigationsarray-Struktur.
Keine Änderung der stabilen 32-Basis außer gezielten Minimalfixes.

## Sanity Scan

```text
Keine bekannten korrupten Fragmente gefunden.
```
