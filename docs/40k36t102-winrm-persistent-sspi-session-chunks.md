# 40k36t102
Eine persistente PSSession für den gesamten bidirektionalen SSPI-Chunk-Transfer:
New-PSSession einmal; INIT, Upload-Chunks, Ausführung, Download-Chunks und Cleanup
ausschließlich via Invoke-Command -Session; Remove-PSSession erst ganz am Ende.
Kleine Preflights und Legacy-NTLM für Linux/macOS bleiben unverändert.
