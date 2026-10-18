# 40k36t107 – SSPI A/B-Diagnose im selben Build

Ziel: Nicht mehr zwei verschiedene historische Builds vergleichen müssen.

Bei Windows/SSPI führt derselbe Inventarstart jetzt zuerst den unveränderten,
kleinen `attemptWindowsNative(..., "whoami", false, 12000)`-Aufruf aus.
Das ist der bisher nachweislich erfolgreiche Preflight-Pfad.

Danach wird der t106-Vollpfad ausgeführt.

Neue Logs:
- `[WINRM-T107-AB] Phase=SMALL_EXACT_PROBE ...`
- bei Erfolg: `Kleiner SSPI-Aufruf ist GRUEN`
- `[WINRM-T107-AB] Phase=FULL_SINGLE_INVOKE ...`

Damit sehen wir in einem einzigen Lauf:
A) ob der kleine bekannte SSPI-Aufruf aktuell wirklich funktioniert,
B) ob der große Aufruf scheitert,
C) Exitcode, Outputlänge, Stack, Fehlercode und Fehlerdetail beider Varianten.

Legacy NTLM bleibt unverändert.
Diese Version ist bewusst eine Diagnoseversion; der kleine Probeaufruf und der
Vollaufruf sind zwei getrennte Remoteaufrufe, damit der Unterschied sichtbar wird.
