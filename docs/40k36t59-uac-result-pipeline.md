# 40k36t59 – UAC-Ergebnis über PowerShell-Pipeline

Fix auf Basis von 40k36t58.

## Ursache
Der erhöhte Gesamtprozess lief erfolgreich (`ADMIN=True`, Exit 0), aber `result.txt`
blieb leer. Grund: Die Windows-Module emittierten GAMKV mit
`[Console]::Out.WriteLine(...)`. Diese Ausgabe umgeht die PowerShell-Success-Pipeline,
die der erhöhte Wrapper in `result.txt` umleitet.

## Fix
Nur im kombinierten UAC-Gesamtskript werden die GAMKV-Emitter für den erhöhten Lauf
von `[Console]::Out.WriteLine(...)` auf `Write-Output ...` umgeschrieben.
Auch die `MODULE_<NAME>`-Statusmarker laufen dort über `Write-Output`.

Die normalen lokalen Module außerhalb UAC, WinRM, UAC-Elevation, Routing, Mapping und
Persistenz bleiben unverändert.
