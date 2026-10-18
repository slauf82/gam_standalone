# 40k36t36 – WinRM-Whitespace: Windows-native SSPI-Fallback

- Basis: 40k36t35a.
- MetricsHub winrm-java 2.1.00 bleibt Standard für alle normalen Konten sowie MicrosoftAccount.
- Nur lokale `HOST\Benutzer`-Konten mit Whitespace erhalten nach einem MetricsHub-Fehlschlag einen Windows-nativen PowerShell/SSPI-Fallback.
- Der Fallback nutzt `Invoke-Command` und damit denselben Windows-Authentifizierungspfad, der im manuellen Referenztest mit `Sebastian Lauf` funktioniert.
- Das Kennwort steht nicht in der Prozesskommandozeile; das PowerShell-Skript wird über STDIN übertragen.
- Auf Nicht-Windows-GAM-Backends bleibt MetricsHub der Standard; dort wird kein PowerShell/SSPI-Fallback behauptet.
- MicrosoftAccount-Benutzernamen werden von der Whitespace-Sonderlogik nicht verändert.
