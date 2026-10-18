# 40k36t96 – SSPI-Launcher als echte PS1-Datei

t95 zeigte nach erfolgreichem SSPI-Preflight sofort `Exit=0 <keine>` und keinen
einzigen Stufenmarker. Damit wurde der mehrzeilige Transfer-Launcher über
`powershell.exe -Command -`/stdin nicht zuverlässig als zusammenhängendes Skript
ausgeführt.

t96 ändert ausschließlich diesen lokalen Launcher:
- temporäre `gam-winrm-sspi-launcher-*.ps1`
- UTF-8 mit BOM für Windows PowerShell 5.1
- Start über `powershell.exe ... -File <launcher.ps1>`
- Launcherdatei wird anschließend gelöscht

Die t95-Stufenprüfungen bleiben vollständig erhalten:
SESSION_CREATED, REMOTE_DIR_CREATED, PAYLOAD_COPIED, PAYLOAD_STARTED,
PAYLOAD_FINISHED, RESULT_REMOTE, RESULT_COPIED, CLEANUP_OK.

WinRM-Routing, SSPI-Preflight, Legacy-NTLM-Fallback für Linux/macOS,
Identity-/Profilauflösung und Inventarisierungsumfang bleiben unverändert.
