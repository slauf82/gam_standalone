# 40k36t71 – Plattformangleichung vor Preview 5

Kleine, bewusst konservative Angleichung der Inventarisierungsprotokolle:

- Linux erhält wie Windows/macOS einen expliziten `Berechtigungsmodus`.
- Linux erhält `Inventarisierungstiefe` und `Berechtigungseinschränkungen`.
- Linux setzt `Nicht verfügbare Module` bei vollständigem Lauf explizit auf `keine`,
  damit keine alten Merge-Werte stehen bleiben.
- macOS erhält zusätzlich den expliziten `Berechtigungsmodus`.
- Die plattformspezifischen Rechtekonzepte bleiben erhalten:
  Windows: Standard / Administrator / SYSTEM
  Linux/macOS: Standard / Erweitert / Administrator / Root
- Verschlüsselung bleibt plattformspezifisch:
  Windows BitLocker, Linux LUKS/dm-crypt, macOS FileVault/APFS.
- Keine Discovery-, Launcher-, SSH-, WinRM-, ADB- oder SYSTEM-Logik wurde verändert.
