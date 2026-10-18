# 40k36t70 – Abschlussfix BitLocker-Zählung

- Explizite Statuswerte `Fully Decrypted`, `Vollständig entschlüsselt`
  und `Vollstaendig entschluesselt` haben jetzt Vorrang.
- Solche Volumes zählen immer als **nicht verschlüsselt**, selbst wenn der
  Prozentwert fehlt oder nicht sauber geparst werden kann.
- Erst wenn kein explizit entschlüsselter Status vorliegt, wird der Prozentwert
  ausgewertet.
- SYSTEM-One-Shot, eine UAC-Abfrage, SID-Erkennung, Ergebnis-Merge und Cleanup
  bleiben unverändert.
- Erwartung für den aktuellen Testrechner:
  `10 Volumes · 0 verschlüsselt · 0 geschützt · 0 gesperrt`.
