# 40k36t106

t105 entfernte korrekt den äußeren Preflight, änderte aber gleichzeitig den technischen
PowerShell-Launcher. t106 übernimmt deshalb den nachweislich funktionierenden direkten
`attemptWindowsNative()`-Preflight-Transport 1:1:

- `powershell.exe -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -Command -`
- Launcher per UTF-8 stdin
- identische UTF-8/Base64-Decodierung von Host/User/Passwort
- identische PSCredential-Erzeugung
- identisches `$invoke=@{ComputerName=...;Port=...;Credential=...;ErrorAction='Stop'}`
- identische `Invoke-Command @invoke -ScriptBlock { param($remoteText) ... } -ArgumentList $remoteText`

Innerhalb genau dieses einen Invoke-Command:
Preflight -> Upload-Chunk-Reassembly -> Inventarskript -> Ergebnis-Base64 ->
nummerierte Download-Chunks -> Ende.

Kein äußerer Preflight, kein New-PSSession, kein zweiter Login.
Legacy-NTLM für Linux/macOS bleibt unverändert.
