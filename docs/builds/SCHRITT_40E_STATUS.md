# Schritt 40e – Kommunikationsworkflow (erster Build)

## Umgesetzt
- Eigenständige Kommunikationszentrale als geführter Workflow
- Kommunikationskanäle: E-Mail, Telefon, Brief, persönlich und intern
- Richtungen: Eingang, Ausgang und intern
- Statusablauf: Neu → In Bearbeitung → Wartet auf Antwort → Erledigt → Archiviert
- Zuständigkeit, Priorität, Fälligkeit und Ergebnisnotiz
- Suche und Filter nach Status und Verantwortlichem
- Kennzeichnung überfälliger Vorgänge
- Persistenz in `communication_workflow_item`
- Konfiguration unter Einstellungen → Workflows → Aufgaben & Kommunikation
- Einstellbare Pflichtfelder, Standardfrist, Frühwarnung, Dashboard-/Benachrichtigungs- und Aufgabenoptionen
- Bestehender SMTP-Kommunikationsassistent bleibt in der Kommunikationszentrale verfügbar

## Buildhinweis
Der Quellstand wurde strukturell geprüft. Ein vollständiger Frontend-/Backend-Build war in der isolierten Buildumgebung nicht möglich, weil weder die npm-Abhängigkeiten noch Maven lokal verfügbar waren und kein Netzzugriff für deren Bezug bestand.
