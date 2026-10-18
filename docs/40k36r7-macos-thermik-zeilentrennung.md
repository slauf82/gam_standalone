# 40k36r7 – macOS Thermik-Zeilentrennung

Gezielter Fix aus dem realen r6-Test:
- CPU_THERMAL_LEVEL, GPU_THERMAL_LEVEL und SCHEDULER_THERMAL_LEVEL werden jeweils atomar mit `printf KEY=%s\n` ausgegeben.
- Auch wenn `sysctl` auf einem Mac für einen Thermikschlüssel keinen Wert liefert, entsteht eine vollständige leere `KEY=`-Zeile.
- Nachfolgende Felder wie USER_COUNT können dadurch nicht mehr in CPU_THERMAL_LEVEL hineinlaufen.
- Der bestehende pmset-Thermikblock bleibt unverändert.
