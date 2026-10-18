# 40k36t69 – SYSTEM-One-Shot mit nur einer UAC-Abfrage

- Die erste UAC-Sitzung übernimmt jetzt den kompletten Lebenszyklus der temporären Aufgabe:
  1. Aufgabe als `S-1-5-18` registrieren
  2. Aufgabe sofort starten
  3. auf `status.txt` des SYSTEM-Wrappers warten
  4. Aufgabe im `finally` stoppen/entfernen
- Der bisherige zweite erhöhte PowerShell-Start nur für `Unregister-ScheduledTask` wurde entfernt.
- Erwartung: pro SYSTEM-Inventarisierung genau eine UAC-Abfrage.
- Dasselbe Inventarisierungspayload, SID-Prüfung, BitLocker-Pfad und Ergebnisübernahme bleiben unverändert.
- Temporäre Dateien werden weiterhin vom Java-Teil nach Ergebnisübernahme entfernt.
