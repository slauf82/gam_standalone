# Schritt 39f – Rechnungsadministration: übersichtlichere Textzuordnung

## Ziel
Die Auswahlmasken in der Rechnungsadministration sollen bei langen Texten nicht überlappen oder zu gedrängt wirken.

## Umsetzung
- Dialogformular der Rechnungsadministration nutzt jetzt ein eigenes Grid: `invoice-admin-editor-grid`.
- Lange Textfelder bzw. Textbausteine belegen volle Breite.
- Mittlere Auswahlfelder wie Gesellschaft, Filiale, Anrede und Grußformel belegen halbe Breite.
- Kurze Felder dürfen bis zu drei pro Zeile stehen.
- Responsive Fallbacks:
  - unter ca. 1150 px: höchstens zwei kurze Felder pro Zeile
  - unter ca. 760 px: ein Feld pro Zeile

## Ergebnis
Die Textzuordnung ist ruhiger lesbar: weniger Dropdowns nebeneinander, lange Texte bekommen genug Platz, und die Maske bleibt auf kleineren Bildschirmen bedienbar.
