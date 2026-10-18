# 40k36r12 – macOS SMART/Akku Root-Cause-Fix

- SMART: Die Shell normalisierte `SMART_DIAGNOSIS`, ausgegeben wurde jedoch weiterhin `SMARTCTL_DIAGNOSTIC`. Die Ausgabe verwendet jetzt die tatsächlich normalisierte Variable.
- Akku: `BATTERY_PRESENT` wird jetzt zuerst ermittelt. Erst danach werden Akkuwerte ausgegeben.
- Desktop-Mac ohne Akku: alle Akku-Unterfelder werden als vollständige `KEY=nicht_vorhanden`-Zeilen ausgegeben.
- MacBook mit Akku: die realen Akkuwerte werden weiterhin normal erhoben.
- Die Java-Ausgabeschicht aus r11 bleibt als zusätzlicher Schutz bestehen.
