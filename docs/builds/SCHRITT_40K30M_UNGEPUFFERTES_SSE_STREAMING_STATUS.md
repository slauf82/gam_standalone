# Schritt 40k30m – Ungepuffertes SSE-Streaming

## Ziel
Die Discovery-Ereignisse sollen während des laufenden Suchlaufs sofort im Browser erscheinen und nicht erst nach Abschluss gesammelt abgespielt werden.

## Umsetzung
- `SseEmitter` für den Discovery-Kanal durch `StreamingResponseBody` mit eigener Ereigniswarteschlange ersetzt.
- Jeder SSE-Block wird direkt auf den Ausgabestrom geschrieben und anschließend explizit geflusht.
- Initialer 8-KiB-SSE-Kommentar erzwingt die frühe Übertragung der Antwort.
- Heartbeat-Kommentar alle fünf Sekunden verhindert Stillstand und Zwischenpufferung während längerer Scanphasen.
- Antwortheader ergänzt: `Cache-Control: no-cache, no-store, no-transform`, `Pragma: no-cache`, `X-Accel-Buffering: no`, `Content-Encoding: identity`.
- HTTP-Komprimierung im Backend deaktiviert, damit `text/event-stream` nicht gesammelt komprimiert wird.
- Weiterhin genau eine Session und genau ein Discovery-Worker je Suchlauf.
- Diagnoseausgaben aus 40k30l bleiben erhalten.
- Gruppen unter „Registriert“ bleiben standardmäßig eingeklappt.

## Prüfung
- Frontend-Produktionsbuild erfolgreich.
- Backend-Quellprüfung durchgeführt.
- Maven-Build war in der isolierten Umgebung nicht möglich, weil der Maven-Wrapper Maven aus dem Internet laden wollte und keine DNS-Verbindung verfügbar war.
- `node_modules` und temporäre Buildverzeichnisse sind nicht Bestandteil des Auslieferungs-ZIP.
