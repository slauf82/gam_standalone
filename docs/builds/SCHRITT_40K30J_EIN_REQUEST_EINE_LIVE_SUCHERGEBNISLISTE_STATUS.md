# Schritt 40k30j – ein Request, ein Scan, eine Live-Anzeige

- Der Startbutton verwendet nur noch `POST /api/inventory/discovery/scan-stream`.
- Derselbe Request führt exakt einen Discovery-Lauf aus und streamt Treffer, Fortschritt und Diagnosen unmittelbar als SSE.
- Der frühere parallele Session-Start mit anschließendem SSE-Abonnement wird vom Frontend nicht mehr verwendet.
- Es gibt keine zweite globale Ergebnisantwort und keine nachträgliche zweite Ergebnisanzeige.
- Ein Frontend-Lock verhindert Doppelklicks und parallele Scans.
- „Alle gefundenen Geräte registrieren“ registriert alle noch neuen eindeutigen Treffer nach Rückfrage.
- Die dauerhaft registrierten Geräte sind genauso nach Gerätekategorien gruppiert wie die Suchergebnisse.
- Treffer der letzten Suche und Gesamttreffer bleiben je registriertem Gerät sichtbar.
