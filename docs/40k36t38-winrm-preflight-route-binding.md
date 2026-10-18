# 40k36t38 – WinRM Preflight-/Inventarisierungsweg vereinheitlicht

- Basis: 40k36t37.
- Verbindungstest und Vollinventarisierung verwenden dieselben gespeicherten Zugangsdaten.
- Vor dem Vollinventar wird ein Preflight mit genau diesen Daten ausgeführt.
- War der Preflight nur über Legacy winrm4j/CXF + NTLM erfolgreich, wird das Vollinventar direkt über exakt diesen Pfad ausgeführt.
- War MetricsHub oder Windows-SSPI erfolgreich, bleibt der moderne Pfad bevorzugt.
- Timeout des Vollinventars wurde für den Remote-Sammellauf auf 30 s angehoben.
- Backend-Logs nennen Host, Benutzer, Preflight-Stack und tatsächlich verwendeten Vollinventar-Pfad.
- Hinweis: Die Spalte `admin` im Plattformstatus ist nicht als WinRM-Benutzer zu interpretieren; die WinRM-Diagnose erfolgt jetzt explizit im Backend-Log.
