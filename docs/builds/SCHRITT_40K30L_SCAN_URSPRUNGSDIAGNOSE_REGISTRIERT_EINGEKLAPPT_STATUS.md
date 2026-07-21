# Schritt 40k30l – Scan-Ursprungsdiagnose und eingeklappte Registriert-Gruppen

## Änderungen

- Jeder Discovery-Session-Start erhält eine fortlaufende `sessionNo`.
- Jeder tatsächlich gestartete Discovery-Worker erhält eine fortlaufende `workerNo`.
- Jeder Eintritt in die Discovery-Engine erhält eine fortlaufende `scanNo`.
- Backend-Protokolle verwenden einheitlich das Präfix `[DISCOVERY-TRACE]`.
- Protokolliert werden Session-Endpunkt, SSE-Endpunkt, deaktivierte Legacy-Endpunkte, Worker-Start/-Ende und direkter Eintritt in die Discovery-Engine.
- Caller-Stack, Thread, Zeitbezug, Client-Trace-ID, User-Agent und Remote-Adresse helfen, einen zusätzlichen Aufrufer eindeutig zu finden.
- Das Frontend erzeugt je Klick eine `clientTrace` und protokolliert Session-POST, SSE-Verbindungsaufbau, jedes SSE-Ereignis und Stream-Ende in der Browserkonsole.
- Die Untergruppen innerhalb von **Registriert** sind standardmäßig eingeklappt. Der Hauptbereich **Registriert** bleibt bei vorhandenen Geräten geöffnet.

## Auswertung

Backend nach `DISCOVERY-TRACE` filtern. Entscheidend sind:

- `ENGINE_SCAN_START`: zählt reale Discovery-Läufe.
- Zwei unterschiedliche `scanNo` bedeuten sicher zwei echte Scanaufrufe.
- `caller=` zeigt den Java-Aufrufspfad.
- `LEGACY_ENDPOINT_CALLED` oder `DIRECT_STREAM_ENDPOINT_CALLED` zeigt einen alten Frontend-/Client-Aufruf.
- `clientTrace=` ordnet Backend-Aufrufe genau einem Browser-Klick zu.
