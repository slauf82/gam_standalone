# Schritt 40k11 – Streaming- und Neighbor-Timeout-Fix

## Ziel
Die in 40k10 nachgewiesene Variante C wird behoben: Die Suche darf bei der zweiten ARP-/Neighbor-Auswertung nicht hängen oder abbrechen, und bereits gefundene Geräte müssen während der laufenden Suche sichtbar werden.

## Änderungen
- Neighbor-Systembefehle werden mit einem harten Prozess-Timeout von 8 Sekunden ausgeführt.
- Die Prozessausgabe wird in einem separaten Daemon-Reader gelesen, sodass ein blockierendes `readLine()` den Timeout nicht mehr unwirksam macht.
- Nach Prozessende gilt ein zusätzlicher kurzer Lesetimeout; hängende Reader werden abgebrochen.
- Fehler einer einzelnen Neighbor-Quelle werden protokolliert, blockieren aber die weitere Pipeline nicht.
- Die zweite Neighbor-Auswertung verzichtet auf blockierende Reverse-DNS-Auflösung.
- NDJSON wird weiterhin nach jedem Ereignis serverseitig geflusht.
- Das Frontend gibt nach jeweils acht empfangenen Ereignissen einen Render-Zyklus frei, damit große Trefferblöcke nicht erst beim Streamende gemeinsam sichtbar werden.

## Erwarteter Ablauf
1. Lokale Adapter
2. Erste ARP-/Neighbor-Auswertung mit sofort sichtbaren Treffern
3. Aktiver Subnetzscan
4. Zweite ARP-/Neighbor-Auswertung mit Timeout-Schutz
5. SSDP/UPnP
6. Konsolidierung
7. Globalabschluss und 100 %

## Build-Hinweis
In der Erstellungsumgebung waren Maven und die bewusst nicht mitgelieferten Frontend-`node_modules` nicht verfügbar. Daher konnten Maven-Compile und Vite-Produktionsbuild hier nicht vollständig ausgeführt werden. Die geänderten Java-/TypeScript-Blöcke wurden strukturell geprüft.
