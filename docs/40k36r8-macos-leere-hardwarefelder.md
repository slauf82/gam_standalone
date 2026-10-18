# 40k36r8 – macOS: leere Hardwarefelder robust behandeln

- CPU_THERMAL_LEVEL, GPU_THERMAL_LEVEL und SCHEDULER_THERMAL_LEVEL erhalten bei fehlender sysctl-Ausgabe explizit `nicht_verfügbar`.
- Dadurch entstehen keine leeren `KEY=`-Zeilen mehr, die bei der Auswertung Folgefelder wie USER_COUNT verschlucken können.
- Zusätzlich werden beobachtete optionale Batterie-/Netzteilfelder bei fehlender Ausgabe explizit auf `nicht_verfügbar` gesetzt, damit derselbe Effekt dort nicht auftreten kann.
- Der funktionierende pmset-Thermikblock und die restliche Hardwareinventarisierung bleiben unverändert.
