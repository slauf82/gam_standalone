# Schritt 40k15 – Discovery-Pfeil und Paketbereinigung

## Änderungen

- Der einklappbare Bereich „Discovery-Zwischenstände“ besitzt jetzt denselben sichtbaren Dreieckspfeil wie die Geräteklassen.
- Der Pfeil dreht sich beim Aufklappen um 90 Grad.
- Die automatische Öffnung bei Discovery-Fehlern bleibt unverändert erhalten.
- `frontend/node_modules` wird nicht mehr im Auslieferungs-ZIP mitgeführt.
- Der bereits erzeugte Frontend-Produktionsbuild unter `frontend/dist` bleibt enthalten.

## Ursache der Größe von 40k14

Das Paket enthielt versehentlich den lokalen Abhängigkeitsordner `frontend/node_modules`. Dieser belegte entpackt ungefähr 152 MB und erhöhte das komprimierte ZIP auf rund 96 MB.
