# 40k36q15 – Software in Gerätezeile und Startinventarisierungs-Ergebnisse

- Die aufgeklappte registrierte Gerätezeile lädt für macOS denselben strukturierten Softwarebestand wie der macOS-Inventarisierungsdialog.
- Darstellung:
  - Software
    - Installierte Software
      - Systemsoftware & systemweit installiert
      - Benutzer
        - je Benutzer eigener Bereich
- Bei leerem strukturiertem Softwarebestand bleibt ein Diagnosehinweis sichtbar.
- Nach Abschluss eines globalen Inventarisierungslaufs wird ein Frontend-Ereignis ausgelöst.
- Geräteübersicht, aufgeklappte Softwaredaten und Plattform-Inventarisierungsstatus werden danach automatisch aktualisiert.
- Startinventarisierungen werden dadurch ohne Seitenreload in den Einzelansichten sichtbar.
- `latestRunsByPlatform` liefert jetzt auch `triggeredBy`.
- Die Plattform-Inventarisierung zeigt den Auslöser (Startinventarisierung / Scheduler / manuell) explizit an.
- Startinventarisierung nutzt weiterhin denselben `runAllKnownPlatforms` / `runPlatformInventory`-Pfad wie Einzelinventarisierungen und landet damit in derselben Laufhistorie.
