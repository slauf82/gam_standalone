# Schritt 6 – CRUD- und Workflow-Grundlagen

Stand: 2026-05-23

## Ziel

Die GAM-2.0-Gesamtstruktur aus Schritt 5 wurde erweitert, damit nicht mehr nur Listen/Read-only-Rahmen vorhanden sind, sondern erste echte Bearbeitungs- und Workflow-Grundlagen entstehen.

## Neu enthalten

### Aufgabenmodul

Neue Backend-API unter `/api/workflow/tasks`:

- Aufgaben suchen/listen
- Aufgabe anzeigen
- Aufgabe anlegen
- Aufgabe bearbeiten
- Aufgabe löschen nur für Admin/Superadmin
- Filter nach Status, Filiale und Suchtext

Datenquelle bleibt die bestehende Tabelle `aufgaben`.

### Freigabemodul

Neue Backend-API unter `/api/workflow/approvals`:

- Freigaben suchen/listen
- Freigabe anzeigen
- Freigabe anlegen
- Freigabe bearbeiten
- Freigabe löschen nur für Admin/Superadmin
- Filter nach Status, Filiale und Suchtext

Datenquelle bleibt die bestehende Tabelle `freigabe`.

### Inventar/Geräte

Neue Schreib-Endpunkte:

- neues Gerät in `geräte_neu` anlegen
- Gerät in `geräte_neu` bearbeiten
- Altgerät in `geräte` bearbeiten
- Filialzuordnung für neue Geräte wird mitgeführt

Bewusst noch nicht enthalten: riskantes Löschen von Geräten. Das sollte erst nach Test und Backup ergänzt werden.

### Lager/Material

Ergänzt:

- Bestand direkt setzen
- Bestand per Delta bewegen, z. B. +5 oder -2

Datenquelle bleibt `lager` und `verbrauchsmaterial`.

## Sicherheits-/Kompatibilitätsprinzip

- Bestehende Tabellen bleiben erhalten.
- Keine destruktiven Datenbankmigrationen.
- Schreibfunktionen sind auf Admin/Superadmin bzw. angemeldete Nutzer beschränkt.
- Kritische Löschfunktionen bleiben zurückhaltend.
- Fachlogik wird modern angebunden, aber nicht blind ersetzt.

## Noch offen

- Frontend-Formulare vollständig ausbauen
- Historien/Audit-Log für Änderungen
- Benutzerbezogene Modulrechte feiner abbilden
- Validierungen aus altem JSF-Projekt übernehmen
- Pflichtfelder und Feldlängen exakt angleichen
- PDF/ZUGFeRD final validieren
- Build lokal mit Maven/Gradle prüfen

## Einordnung

Mit Schritt 6 bewegt sich GAM 2.0 von der reinen Alpha-Gesamtstruktur in Richtung nutzbarer Arbeitsversion. Die App ist weiterhin nicht produktionsfertig, enthält aber jetzt die ersten echten Bearbeitungs-Workflows für Aufgaben, Freigaben, Inventar und Lager.
