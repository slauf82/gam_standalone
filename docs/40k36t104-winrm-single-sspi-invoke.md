# 40k36t104 – alles in genau einem SSPI-Invoke-Command

Der gekoppelte Inventarpfad baut keine zweite Sitzung auf. Der komplette Remote-Lauf
ist genau ein authentifizierter Invoke-Command: Preflight als erster Remote-Schritt,
danach Reassembly der nummerierten Upload-Chunks, Inventarausführung, Base64-
Kodierung des Ergebnisses und nummerierte Result-Chunks im selben Aufruf.

Java prüft die Result-Chunk-Anzahl und Reihenfolge und dekodiert erst danach GAMKV.
Mit dem Ende dieses einen Invoke-Command endet auch die Verbindung.

Diagnose: `[WINRM-SSPI-SINGLE] OneInvoke=true`.
Legacy NTLM für Linux/macOS bleibt unverändert.
