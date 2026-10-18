# 40k36t92 – einheitlicher PowerShell-Ausgabekanal

Referenz: 40k36t36 war mit SSPI/Invoke-Command nachweislich erfolgreich und gab
Inventardaten über die normale PowerShell-Pipeline zurück.

Die 9 noch vorhandenen direkten GAMKV-Ausgaben über `[Console]::Out.WriteLine(...)`
wurden auf `Write-Output (...)` umgestellt. Eine bereits vorhandene Pipeline-Ausgabe
bleibt unverändert. Damit verwenden nun alle GAMKV-Emit-Pfade den PowerShell-
Success-Output-Kanal.

Die Nutzlast `GAMKV|SCHLÜSSEL|BASE64WERT` bleibt identisch.

Ausführungswege:
- lokaler Standardbenutzer: Pipeline -> stdout
- lokaler Administrator: Pipeline -> stdout
- SYSTEM/UAC: vorhandener Wrapper sammelt `*>&1` in `result.txt`
- WinRM: `Invoke-Command` transportiert die Pipeline zurück

Unverändert bleiben der t36-basierte SSPI-Wrapper aus t91, die t88-Profil-/Identity-
Auflösung, das 90-s-WinRM-Timeout und der aktuelle vollständige Inventarisierungsumfang.
