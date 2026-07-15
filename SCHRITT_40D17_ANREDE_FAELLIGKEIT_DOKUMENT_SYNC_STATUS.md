# Schritt 40d17 – Anrede und Fälligkeitsdatum synchronisiert

## Behoben

- Das im Zahlungsworkflow gespeicherte Fälligkeitsdatum wird vom PDF-Renderer nicht mehr durch ein neu berechnetes Standarddatum ersetzt.
- Nach `+1 Tag`, `+3 Tage` oder `+7 Tage` wird das zuletzt erzeugte Zahlungsdokument sofort neu gerendert und in der Datenbank ersetzt.
- Patientenportal und interner Zahlungsworkflow verwenden damit denselben aktuellen Fälligkeitstermin.
- Historische Anredecodes werden unterstützt: `1`, `2`, `3`, `b'1'`, `b'2'`, `b'3'` sowie Herr/Herrn/Frau/Divers/Mx/Mr/Ms/Mrs.
- Akademische Titel werden in der richtigen Reihenfolge erkannt, insbesondere `Prof. Dr.` und `PD Dr.`.
- Persönliche Anreden bleiben erhalten; nur bei wirklich fehlenden Angaben wird der neutrale Fallback verwendet.

## Test

1. Zahlungsdokument erzeugen und Fälligkeitsdatum im PDF kontrollieren.
2. Im Zahlungsworkflow `+1`, `+3` oder `+7 Tage` wählen.
3. Dasselbe aktuelle Dokument erneut öffnen.
4. Das PDF muss den neuen Termin anzeigen.
5. Empfänger mit Herr/Frau und Titel prüfen.
