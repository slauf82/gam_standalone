# 40k36t103 – Preflight und Inventar in demselben Session-Lebenszyklus

Wiederherstellung der historischen Kopplung:

1. genau eine Windows-native SSPI/PSSession wird aufgebaut
2. der Preflight ist der ERSTE Befehl innerhalb genau dieser Session
3. nur bei erfolgreichem `GAM_PREFLIGHT_OK` bleibt dieselbe Session offen
4. INIT + alle Upload-Chunks laufen über diese Session
5. Inventarskript wird in dieser Session ausgeführt
6. alle Download-Chunks laufen über dieselbe Session
7. Remote-Cleanup
8. Session wird erst ganz am Ende geschlossen

Damit gibt es zwischen Preflight und Vollinventar keinen zweiten Login und keinen
zweiten Session-Aufbau.

Chunkgröße: 3072 Base64-Zeichen.

Neue Diagnose:
- GAM_SESSION_OPEN
- GAM_PREFLIGHT_OK session=...
- GAM_CHUNK_UPLOAD ...
- GAM_CHUNK_EXEC_OK
- GAM_CHUNK_DOWNLOAD ...
- GAM_CHUNK_DONE
- GAM_SESSION_CLOSED
- [WINRM-SSPI-COUPLED]

Legacy NTLM für Linux/macOS bleibt unverändert.
