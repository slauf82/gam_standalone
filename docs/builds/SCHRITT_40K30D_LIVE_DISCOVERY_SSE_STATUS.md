# Schritt 40k30d – echte Live-Discovery per SSE

## Umsetzung

- Discovery-Stream von NDJSON auf `text/event-stream` (Server-Sent Events) umgestellt.
- Jedes gefundene Gerät, jede Phase und jede Diagnosemeldung wird sofort als einzelnes SSE-Ereignis geschrieben und geflusht.
- Antwort-Header verhindern Proxy- und Browser-Pufferung (`Cache-Control: no-store`, `X-Accel-Buffering: no`).
- Das Frontend verarbeitet den Stream blockweise während die Suche noch läuft.
- Nach jedem Ereignis wird dem Browser ein Render-Zyklus ermöglicht, sodass Gerätelisten, Trefferzahlen, Phase und Fortschritt tatsächlich live sichtbar nachgeführt werden.
- Der bestehende globale Abschluss und die Prüfung auf einen vollständig geschlossenen Stream bleiben erhalten.

## Sichtbares Verhalten

Während das Backend bereits Geräte protokolliert, erscheinen diese nun unmittelbar im Frontend. Die Fortschrittsanzeige bewegt sich mit den Phasen weiter und die einklappbaren Discovery-Zwischenstände werden fortlaufend ergänzt.
