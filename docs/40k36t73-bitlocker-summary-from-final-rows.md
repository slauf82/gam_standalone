# 40k36t73 – BitLocker-Zusammenfassung aus finalen Volume-Zeilen

- Die BitLocker-Zusammenfassung wird nicht mehr aus Zwischenzählern übernommen.
- Sie wird direkt aus den bereits final formatierten Volume-Zeilen abgeleitet.
- `Vollständig entschlüsselt` / `Fully Decrypted` zählt damit sicher als nicht verschlüsselt.
- Schutz und Sperre werden ebenfalls aus den finalen Zeilen neu gezählt.
- Diagnosefeld `BITLOCKER_SUMMARY_SOURCE` zeigt die Herkunft der Zusammenfassung.
- Erwartung für den aktuellen Testrechner:
  `10 Volumes · 0 verschlüsselt · 0 geschützt · 0 gesperrt`.
- SYSTEM-, UAC-, SID-, Merge- und Plattformlogik bleiben unverändert.
