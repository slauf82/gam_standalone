# 40k36q7 – macOS-Softwareinventarisierung

## Änderungen
- Vollständige macOS-Inventarisierung erfasst installierte Software deutlich breiter.
- Systemsoftware/systemweit installierte Software und benutzerspezifische Software werden getrennt.
- Systembereich kombiniert `/System/Applications`, `/Applications`, Installer-Pakete (`pkgutil`) und vorhandene Homebrew-Installationen.
- Alle realen lokalen Benutzer (UID >= 500) werden ermittelt; deren `~/Applications` werden dem jeweiligen Benutzer zugeordnet.
- Dynamische Zähler für Gesamtbestand, Systemsoftware, Benutzersoftware und Benutzer mit eigener Software.
- Im macOS-Inventar erscheint `Installierte Software` hierarchisch:
  - Systemsoftware & systemweit installiert
  - Benutzer
    - je Benutzer eigener Unterbereich
- Einträge zeigen Name, Version, Quelle und Pfad/Paket.
- Deduplizierung erfolgt vor der Übergabe an GAM.
- Für den umfangreicheren vollständigen Softwarelauf wurden die SSH-Inventarisierungs-Timeouts erhöht.
