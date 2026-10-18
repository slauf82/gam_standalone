# 40k36t67 – SYSTEM-Erkennung per SID

- Der SYSTEM-Launcher aus t66 bleibt unverändert.
- Das gemeinsame Windows-Inventarisierungspayload erkennt SYSTEM nun sprachunabhängig über die SID `S-1-5-18`.
- Die bisherige Prüfung gegen den englischen Kontonamen `NT AUTHORITY\SYSTEM` wurde entfernt.
- Damit funktioniert die Erkennung auch auf deutschem Windows (`NT-AUTORITÄT\SYSTEM`) und anderen Sprachversionen.
- Erwartetes Ergebnis eines erfolgreichen SYSTEM-Laufs:
  - Administratorrechte: Ja
  - SYSTEM-Rechte: Ja
  - Integritätsstufe: System
  - Inventarisierungstiefe: Vollzugriff
- Inventarmodule, BitLocker-Pfad, UAC-Launcher, SYSTEM-One-Shot-Task und Cleanup bleiben unverändert.
