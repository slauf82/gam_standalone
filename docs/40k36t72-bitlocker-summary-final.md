# 40k36t72 – BitLocker-Zusammenfassung final korrigiert

- `Vollständig entschlüsselt` / `Fully Decrypted` zählt immer als **nicht verschlüsselt**.
- Ein expliziter Prozentwert `0` / `0,0 %` zählt ebenfalls als nicht verschlüsselt.
- Nur Prozentwerte > 0 oder bekannte verschlüsselte/konvertierende Statuswerte zählen als verschlüsselt.
- Zusätzliche Plausibilitätsprüfung: wenn alle erfassten Volume-Zeilen explizit `Vollständig entschlüsselt` melden, wird der Zähler auf 0 gesetzt.
- Erwartung für den aktuellen Testrechner:
  `10 Volumes · 0 verschlüsselt · 0 geschützt · 0 gesperrt`.
- SYSTEM-One-Shot, eine UAC-Abfrage, SID-Erkennung, Plattformangleichungen und Merge-Logik bleiben unverändert.
