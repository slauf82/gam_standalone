# 40k36t100 – SSPI-Großnutzlast/Rückgabe

t99 bestätigt, dass Preflight und Vollinventar denselben Windows-nativen
SSPI/Invoke-Command-Kanal erreichen, das große Inventar aber mit 0 Zeichen zurückkommt.

t100 ändert daher nicht die Authentifizierung:
- gleicher ComputerName/Port/Credential/Invoke-Command-SSPI-Pfad
- keine neue PSSession
- PowerShell-Inventarskript wird als UTF-8/Base64-Nutzlast an einen kleinen Remote-Wrapper gegeben
- erst remote wird die Nutzlast dekodiert und als ScriptBlock ausgeführt
- Remote-Ergebnisse werden vor der Java-Prozessgrenze explizit über Console.Out als Text ausgegeben
- stdout/stderr werden mit der bereits vorhandenen BOM-/PowerShell-sicheren Dekodierung gelesen
- `[WINRM-SSPI-RETURN]` protokolliert Nutzlast- und Rückgabegrößen

Legacy NTLM bleibt für Linux/macOS mit lokalen Windows-Benutzern mit Leerzeichen
unverändert und unter Windows letzte Rückfallebene.
