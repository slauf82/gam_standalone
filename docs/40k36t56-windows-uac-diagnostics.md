# 40k36t56 – UAC-Diagnose

Nur der lokale UAC-Pfad wurde instrumentiert.

Neue Backend-Logs:
- `[WINDOWS-UAC] Anforderung empfangen`
- Payload/Wrapper-Pfade
- `Start-Process -Verb RunAs wird aufgerufen`
- Launcher-PID
- Launcher-Exit/Ausgabe
- Vorhandensein von Status-, Ergebnis- und Trace-Datei
- Trace des erhöhten Wrappers mit Benutzer/Adminstatus
- Ergebnisgröße oder konkreter Fehler

Der erhöhte Wrapper schreibt zusätzlich `trace.txt`:
- `WRAPPER_STARTED`
- `USER=... ADMIN=True/False`
- `PAYLOAD_DONE`
- bzw. `WRAPPER_EXCEPTION=...`

WinRM, Mapping, Persistenz, t53-Encoding und Routing aus t55 bleiben unverändert.
