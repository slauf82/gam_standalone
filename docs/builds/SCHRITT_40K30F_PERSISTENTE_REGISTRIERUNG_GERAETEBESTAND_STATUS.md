# Schritt 40k30f – Persistente Registrierung und Gerätebestand

## Umgesetzt

- Die bisherige Geräteliste heißt fachlich eindeutig **Gerätebestand**.
- Registrierte, aber noch nicht übernommene Geräte werden dauerhaft in der separaten Tabelle
  `gam_discovery_registered_devices` gespeichert.
- Die Tabelle speichert neben der stabilen Identität auch Name, Typ, IP-Adresse, MAC-Adresse,
  Seriennummer, Hersteller, Discovery-Quelle, Status sowie Erkennungs- und Registrierungszeiten.
- Bereits mit 40k30e erzeugte Tabellen werden beim Start automatisch um die neuen Spalten ergänzt.
- Ein Neuladen der Browserseite oder ein Neustart des Backends verliert registrierte Geräte nicht mehr.

## Vollständig reversibler Workflow

1. **Neu erkannt → Registriert**: `Registrieren`
2. **Registriert → Gerätebestand**: `In Gerätebestand übernehmen`
3. **Gerätebestand → Registriert**: `Aus Gerätebestand entfernen`
4. **Registriert → Neu erkannt**: `Registrierung aufheben`

Altgeräte und neu übernommene Geräte werden erst im Gerätebestand gemeinsam angezeigt.

## Live-Discovery

- Die Discovery läuft weiterhin in einem eigenen Worker und blockiert die SSE-Verbindung nicht.
- Treffer, Fortschritt, aktuelle Phase und Diagnosemeldungen werden beim Entstehen veröffentlicht.
- Ein großer initialer SSE-Kommentar erzwingt die frühe Übertragung durch Servlet-Container,
  Browser und mögliche Proxy-Puffer.
- Im Frontend steigen Trefferzähler und Fortschrittsanzeige bereits während der laufenden Suche.
- Der Abschluss ist ausschließlich das letzte Ereignis und löst nicht erst die Ergebnisanzeige aus.

## Standardautomatik

- Automatisch registrieren: **aktiv**
- Automatisch kategorisieren: **aktiv**
- Automatisch in den Gerätebestand übernehmen: **inaktiv**
