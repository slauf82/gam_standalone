# 40k36q10 – Globaler Inventarisierungsstatus

- Plattforminventarisierungen sind jetzt GAM-weit sichtbar, nicht nur im Geräte-/Inventarisierungsdialog.
- Der Backend-Status basiert direkt auf `gam_discovery_platform_inventory_runs` und erfasst damit Start-, manuelle und Scheduler-Inventarisierungen gemeinsam.
- Die globale Statusleiste zeigt Herkunft des Laufs, Zahl aktiver Inventarisierungen, aktuelles Gerät und Plattform.
- Aufklappen zeigt alle aktuell laufenden Geräte/Plattformen und deren Laufzeit.
- `Inventarisierung öffnen` springt direkt in das Geräteverzeichnis.
- Während eines aktiven Laufs wird alle 2,5 Sekunden aktualisiert; im Leerlauf alle 10 Sekunden.
- Nach Abschluss bleibt der letzte Abschluss rund eine Minute sichtbar und verschwindet dann automatisch.
- Die Anzeige ist sticky und bleibt beim Wechsel zwischen GAM-Modulen sichtbar.
