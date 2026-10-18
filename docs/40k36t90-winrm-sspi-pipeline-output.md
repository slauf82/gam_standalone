# 40k36t90 – WinRM SSPI echte Pipeline-Ausgabe

Der Windows-native SSPI-Fallback führt das gemeinsame Inventarskript weiterhin unverändert
inhaltlich aus. Unmittelbar vor `ScriptBlock::Create()` wird nur für diesen Remoting-Pfad
der Ausgabetransport angepasst:

`[Console]::Out.WriteLine(` wird zu `Write-Output (`

Damit gelangen die bestehenden Base64-GAMKV-Zeilen über die PowerShell-Pipeline zurück
zu `Invoke-Command`. Lokale Inventarisierung, Admin/SYSTEM, MetricsHub und Legacy-WinRM
bleiben unverändert.
