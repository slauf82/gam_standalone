# Schritt 38g8 – GCS Kommunikation konsolidiert

## Ziel

Schritt 38g8 fasst die bisher verteilten Kommunikationsfunktionen als **GCS – GAM Communication Service** zusammen.

## Enthalten

- Neuer Navigationspunkt **Kommunikation**
- Kommunikationszentrale für SMTP, Login-News und Protokoll
- Anzeige der aktuellen Login-News aus der vorhandenen `news`-Tabelle
- Anzeige der letzten News-Einträge
- Kommunikationsprotokoll auf Basis des bestehenden Meldungs-/Toast-Systems
- Anklickbare Protokolleinträge mit Änderungsdetails
- Testmail aus der Kommunikationszentrale
- SMTP-Assistent bleibt zentral nutzbar
- Lokales Protokoll kann geleert werden

## Technische Hinweise

- Vollständiges Quellcodepaket ohne `node_modules`, `dist` und `target`
- Das Kommunikationsprotokoll ist in diesem Schritt weiterhin frontendseitig/localStorage-basiert
- Die Login-News verwendet weiterhin die vorhandene GAM-1.0-Tabelle `news`
- Eine spätere echte DB-Historie kann auf diesem GCS-Konzept aufbauen

## Build-Hinweis

Frontend-Build konnte in dieser Umgebung nicht vollständig ausgeführt werden, weil `frontend/node_modules` im schlanken Quellcodepaket bewusst nicht enthalten ist. Das Paket entspricht dem vereinbarten Standard: vollständig, aber ohne generierte Abhängigkeiten.
