# 40k36q18 – Inventarisierung abbrechen und Multi-Interface-Merge abschließen

## Inventarisierungen abbrechen
- Globale Sticky-Anzeige erhält `⏹ Inventarisierung abbrechen`.
- Der Abbruch gilt für Startinventarisierung, manuelle Plattforminventarisierung und andere Läufe, die über die zentrale Plattforminventarisierung laufen.
- Backend markiert laufende Datensätze als `ABGEBROCHEN` und unterbricht den zugehörigen Worker-Thread.
- Ein nach dem Abbruch noch eintreffendes Ergebnis überschreibt den Abbruchstatus nicht mehr.
- Startinventarisierung stoppt nach einer Abbruchanforderung und beginnt nicht noch weitere Geräte.
- macOS-Test-Button wird während des Laufs zu `⏹ macOS-Test abbrechen`.
- `Rechte prüfen und erneut inventarisieren` wird während des Laufs zu `⏹ Inventarisierung abbrechen`.

## Multi-Interface-Merge
- Eine Merge-Vorschau darf trotz unterschiedlicher MAC-Adressen geladen werden.
- Unterschiedliche MAC-Adressen blockieren weiterhin den tatsächlichen Merge, solange keine ausdrückliche Bestätigung vorliegt.
- `confirmCriticalConflict` gilt bei einem ausdrücklich bestätigten Multi-Interface-Fall zugleich als bewusste MAC-Konfliktbestätigung.
- Bei offener Vertrauensbewertung ist `Zusammenführung prüfen` immer sichtbar.
- Bei mehreren Kandidaten öffnet der Button die zentrale Kandidatenprüfung.
- Nach der Vorschau lautet die Aktion `Jetzt trotzdem zusammenführen`.
- Alle bekannten IP- und MAC-Adressen bleiben nach dem Merge über die bestehende Netzwerkidentitätsverwaltung am gemeinsamen Gerät erhalten.
