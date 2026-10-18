# 40k36r11 – macOS Java-Ausgabenormalisierung

Die beiden letzten Fehler werden jetzt in der Java-Ausgabeschicht behoben:

- SMART-Diagnose wird nicht mehr aus dem möglicherweise veralteten Rohfeld übernommen, sondern aus `SMARTCTL_AVAILABLE`, `SMARTCTL_EXECUTABLE` und `SMARTCTL_PATH` abgeleitet.
- Bei `BATTERY_PRESENT != ja` zeigt die Detailausgabe für Akkuwerte konsequent `nicht vorhanden`.
- Falls ein Akku vorhanden ist, aber ein Wert fehlt oder offensichtlich aus verketteten Rohschlüsseln besteht, wird `nicht verfügbar` angezeigt.
- Die Rohdatenerfassung bleibt unverändert; korrigiert wird gezielt die Entwickler-/Frontend-relevante Detailausgabe.
