# 40k36t89 – WinRM SSPI Console.Out Rückgabefix

Der Windows-native PowerShell/SSPI-Fallback konnte erfolgreich authentifizieren und
das Remote-Skript ausführen, lieferte beim Vollinventar aber 0 Zeichen zurück.

Ursache:
Das gemeinsame Windows-Inventarskript schreibt GAMKV-Daten über
`[Console]::Out.WriteLine(...)`. `Invoke-Command` transportiert primär PowerShell-
Pipeline-Ausgabe; Console.Out wurde daher im nativen SSPI-Fallback nicht an GAM
zurückgegeben.

Fix:
- Im Remote-ScriptBlock wird `[Console]::Out` temporär auf einen `StringWriter` umgebogen.
- Nach Ausführung des gemeinsamen Inventarskripts wird der ursprüngliche Writer wiederhergestellt.
- Der gepufferte GAMKV-Text wird anschließend mit `Write-Output` in die PowerShell-Pipeline gegeben.
- Der bestehende GAMKV-Parser bleibt unverändert.
- Zusätzliche Diagnose meldet Zeichenanzahl bzw. weiterhin leere Rückgabe.

Damit bleiben Preflight, Profilauflösung, SSPI-Authentifizierung und gemeinsames
Inventarskript aus t88a unverändert.
