# Schritt 38k – Terminverwaltung GDS

Status: umgesetzt auf Basis von Schritt 38j.

## Enthalten

- neues Modul „Terminverwaltung“
- Login-/Modulzugriff ergänzt
- Terminliste mit Suche
- Termin anlegen / bearbeiten / löschen
- Patient aus Patientenverwaltung übernehmbar
- Datum, Beginn, Ende, Terminart, Status, Raum, Behandler
- Erinnerung für spätere GCS-Kommunikation vorbereitet
- Toasts und Änderungsdetails über bestehende GDS-Meldungen

## Hinweise

- Speicherung erfolgt analog 38j lokal über localStorage als testbare Zwischenstufe.
- Backend-/DB-Persistenz kann später auf vorhandene GAM-Tabellen oder neue Termin-Tabellen umgestellt werden.
