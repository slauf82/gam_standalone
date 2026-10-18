# 40k36t91 – bekannten funktionierenden SSPI-Wrapper aus t36 wiederhergestellt

Referenz: 40k36t36 `winrm-whitespace-sspi-fallback`, vom Benutzer als WinRM-erfolgreich bestätigt.

Änderung:
- `attemptWindowsNative(...)` wurde funktional auf den t36-Stand zurückgesetzt.
- Keine Console.Out-Umleitung aus t89.
- Kein dynamisches Ersetzen von `[Console]::Out.WriteLine` durch `Write-Output` aus t90.
- Wieder exakt:
  `$result=Invoke-Command ...`
  `$result | ForEach-Object { $_ }`
- Lediglich die heutige Diagnose ergänzt weiterhin die Zeichenanzahl.

Unverändert bleiben:
- t88/t88a Ziel-/Profilauflösung über Identity, IP und Hostname.
- heutiger kompletter Windows-Inventarisierungsumfang.
- 90-Sekunden-Vollinventar-Timeout.
- t787/t86 Software-/AppX-/SYSTEM-Härtungen.
