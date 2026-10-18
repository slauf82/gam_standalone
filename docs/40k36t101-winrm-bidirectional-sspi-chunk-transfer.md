# 40k36t101 – bidirektionaler SSPI-Chunk-Transfer

Wiederherstellung des früher bewährten Prinzips für große Windows-WinRM-Inventare.

## Hinweg
- INVENTORY_SCRIPT -> UTF-8 -> Base64
- 3072-Zeichen-Chunks
- nummerierte Invoke-Command-Uploads über denselben Windows-nativen SSPI-Pfad
- remote Zusammenbau in `%TEMP%/gam_sspi_<uuid>/payload.b64`

## Remote-Ausführung
- Base64 zusammensetzen/dekodieren
- Inventarskript als ScriptBlock ausführen
- komplette Ausgabe als UTF-8-Text erfassen
- Ergebnis wieder Base64-kodieren und remote puffern

## Rückweg
- Ergebnis-Base64 in 3072-Zeichen-Chunks
- jeder Chunk separat über Invoke-Command zurück
- lokale Reihenfolge/Länge prüfen
- Base64 zusammensetzen und UTF-8 dekodieren
- erst danach GAMKV-Parser

## Diagnose
- GAM_CHUNK_UPLOAD_INIT
- GAM_CHUNK_UPLOAD x/y
- GAM_CHUNK_EXEC_OK
- GAM_CHUNK_DOWNLOAD_INIT
- GAM_CHUNK_DOWNLOAD x/y
- GAM_CHUNK_DONE
- [WINRM-SSPI-CHUNK]

Kleine Preflights bleiben auf dem bisherigen direkten SSPI-Pfad.
Legacy winrm4j/CXF + NTLM bleibt für Linux/macOS bei lokalen Windows-Benutzern
mit Leerzeichen unverändert erhalten und ist unter Windows weiterhin letzter Fallback.
