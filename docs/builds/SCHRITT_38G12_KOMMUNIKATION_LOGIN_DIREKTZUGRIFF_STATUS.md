# Schritt 38g12 – Kommunikation im Login-Direktzugriff

## Ziel
Das neue Modul **Kommunikation** muss im Login-Dialog direkt auswählbar sein, damit der Benutzer nach erfolgreichem Login direkt in den GCS-/Kommunikationsbereich wechseln kann.

## Änderungen

- Login-Zielauswahl um **Kommunikation** ergänzt.
- Mapping `Kommunikation -> communication` ergänzt.
- Startseiten-Logik erlaubt `communication` direkt nach erfolgreichem Login.
- Modultext-/Key-Erkennung erkennt `Kommunikation`, `communication` und `GCS`.
- Eigenes Icon `frontend/public/icons/communication.svg` ergänzt.
- i18n-Einträge für `communication` ergänzt.

## Betroffene Dateien

- `frontend/src/main.tsx`
- `frontend/src/i18n.ts`
- `frontend/public/icons/communication.svg`

## Hinweise

Keine Backend- oder Datenbankänderungen.
Das Paket bleibt ein vollständiges Quellcodepaket ohne `node_modules`, `dist` und `target`.

## Build

Frontend-Build konnte in dieser Umgebung nicht erneut ausgeführt werden, weil `node_modules` bewusst nicht im Paket enthalten ist und externe Paketinstallation hier nicht verfügbar ist.
