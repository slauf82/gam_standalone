# Schritt 40k10 – Discovery-Diagnose

- Sichtbare Zwischenstände für lokale Adapter, erste Nachbartabelle, aktiven Subnetzscan, zweite Nachbartabelle, SSDP und Konsolidierung.
- Jede Phase meldet `RUNNING`, `COMPLETED`, `SKIPPED` oder `FAILED` mit Zeitstempel und Trefferzahl.
- Backend-Logging im Format `[DISCOVERY] session=... phase=... status=...`.
- Getrennte Ereignisse für globalen Abschluss und bevorstehendes Schließen des Streams.
- 100 % werden nur nach Globalabschluss und vollständig gelesenem Stream angezeigt.
- Ein vorzeitig geschlossener Stream wird ausdrücklich als unvollständige Suche gemeldet.
