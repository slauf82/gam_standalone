# Schritt 40k30e – echte Live-Discovery und klare Stufentrennung

## Umgesetzt

- Die Discovery läuft jetzt in einem eigenen Backend-Worker und ist vollständig vom HTTP-Request getrennt.
- `POST /api/inventory/discovery/sessions` startet die Suche und liefert sofort eine Session-ID zurück.
- `GET /api/inventory/discovery/sessions/{sessionId}/events` überträgt Treffer, Fortschritt und Diagnosemeldungen per SSE.
- Ein serverseitiger Ereignispuffer verhindert, dass Treffer zwischen Suchstart und Aufbau des SSE-Kanals verloren gehen.
- Neue Geräte, Identitätsergänzungen und Fortschrittsmeldungen werden beim Entstehen ausgeliefert und nicht erst nach dem Gesamtabschluss nachträglich abgespielt.
- Die bisherige Stream-Schnittstelle bleibt aus Kompatibilitätsgründen erhalten, wird vom Frontend aber nicht mehr verwendet.

## Registrierungsworkflow

Die drei Zustände werden nun klar getrennt:

1. **Neu erkannt** – noch nicht registriert.
2. **Registriert, noch nicht in der Geräteliste** – GAM kennt die Identität dauerhaft, sie gehört aber noch nicht zum verwalteten Bestand.
3. **Geräteliste / Bestand** – erst nach manueller oder automatischer Übernahme gemeinsam mit Alt- und Bestandsgeräten.

Bei aktivierter automatischer Registrierung wird die Registrierung bereits beim Live-Treffer im Backend ausgeführt. Das Gerät erscheint dadurch sofort im separaten Bereich „Registrierte Geräte, noch nicht in der Geräteliste“.

## Standardverhalten

- automatische Registrierung: EIN
- automatische Übernahme in die Geräteliste: AUS
- automatische Kategoriezuordnung: EIN

Damit bleibt die eigentliche Geräteliste bewusst kuratiert.

## Prüfung

- Frontend-Build erfolgreich.
- Backend-Maven-Build konnte in der Arbeitsumgebung nicht ausgeführt werden, weil Maven nicht installiert ist.
