# 40k36t105 – kein äußerer Preflight mehr

Der Fehler in t104 war eine Ebene oberhalb des Single-Invoke-Transports:
`inspectSingle()` rief weiterhin zuerst `testConnection()` auf. Dieser erfolgreiche
Preflight verbrauchte seine temporäre `Invoke-Command -ComputerName`-Verbindung.
Danach startete t104 einen zweiten Invoke-Command für das Inventar, der AccessDenied
erhielt.

t105 entfernt diesen äußeren Preflight für Windows-Backends vollständig.

Windows-Inventarlauf:
1. kein `testConnection()` vor dem Inventar
2. genau ein SSPI `Invoke-Command`
3. dessen erster Remote-Schritt ist `GAM_PREFLIGHT_OK`
4. im selben Remote-Aufruf: Upload-Chunks -> Reassembly -> Inventar
5. im selben Remote-Aufruf: Base64-Ergebnis -> Result-Chunks
6. erst mit Ende dieses Aufrufs endet die Verbindung

Nicht-Windows-Backends behalten ihre bisherige Preflight-/Legacy-Logik.
Legacy NTLM für Linux/macOS bleibt unverändert.
