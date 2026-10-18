# 40k36r6 – macOS Hardwareanalyse: Fallback-Korrekturen

Korrekturen aus dem realen r5-Test:
- physische `/dev/diskN` werden robust aus `diskutil list` ermittelt, damit der native SMART-Fallback ohne smartctl Datenträger erreicht;
- RAM-awk ohne die fehlerhafte Zeichenklasse;
- CPU/GPU/Scheduler-Thermikfelder werden auch bei leeren sysctl-Werten immer mit eigenem Zeilenende ausgegeben.

smartmontools wird weiterhin nicht automatisch installiert.
