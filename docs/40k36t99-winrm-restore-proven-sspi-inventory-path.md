# 40k36t99 – bewährten SSPI-Inventarpfad wiederhergestellt

Der erfolgreiche Windows-native SSPI-Whitespace-Preflight wird für das Vollinventar
nicht mehr verlassen.

Wiederhergestellt ist der bereits bewiesene Pfad aus 40k36t36:
- Windows-native PowerShell/SSPI
- Invoke-Command
- identische Credentials/Host/Port/SSL-Konfiguration wie beim Preflight
- `$result=Invoke-Command ...`
- `$result | ForEach-Object { $_ }`
- keine zusätzliche `New-PSSession -Credential` für das Vollinventar

Damit ist das Vollinventar wieder direkt an denselben SSPI-Mechanismus gekoppelt,
der unmittelbar zuvor den Preflight erfolgreich ausgeführt hat.

Legacy winrm4j/CXF + NTLM bleibt unverändert als notwendiger Pfad für Linux/macOS
bei lokalen Windows-Benutzern mit Leerzeichen sowie als letzte Rückfallebene.

Der t94–t98-Dateitransfercode bleibt vorerst im Quelltext, wird im erfolgreichen
SSPI-Preflight-Zweig aber nicht mehr verwendet. So minimiert t99 die Änderung und
stellt gezielt den bewiesenen Ablauf wieder her.
