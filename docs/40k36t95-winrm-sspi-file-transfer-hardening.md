# 40k36t95 – SSPI-Dateiweg mit harten Stufenprüfungen

Jede Stufe des Windows-nativen SSPI-Dateitransfers wird jetzt geprüft und protokolliert:

- `SESSION_CREATED`
- `REMOTE_DIR_CREATED`
- `PAYLOAD_COPIED size=...`
- `PAYLOAD_STARTED`
- `PAYLOAD_FINISHED size=...`
- `RESULT_REMOTE size=...`
- `RESULT_COPIED size=...`
- `CLEANUP_OK`

Fehlt Datei oder Größe, wird sofort eine Exception ausgelöst und der PowerShell-Launcher
endet mit Exit 91. Damit kann der Pfad nicht mehr mit Exit 0 und leerer Ergebnisdatei
scheinbar erfolgreich durchlaufen.

Legacy NTLM bleibt unverändert als Rückfallebene für Linux/macOS und als letzter
Fallback unter Windows.
