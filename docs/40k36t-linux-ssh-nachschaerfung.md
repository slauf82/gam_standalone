# 40k36t – Linux/SSH-Nachschärfung

Aufbauend auf dem stabilisierten gemeinsamen Embedded-SSH-Pfad wird Linux an die strukturierte Softwareinventarisierung aus macOS angeglichen.

## Softwarequellen
Das Linux-SSH-Inventar liefert nun strukturierte Einträge für:
- Debian/Ubuntu: APT / dpkg
- Arch/CachyOS: pacman
- Fedora/RHEL/openSUSE-nahe RPM-Systeme: RPM / DNF / YUM
- Alpine: APK
- Snap
- Flatpak
- Homebrew Formulae auf Linux, falls Homebrew vorhanden ist

Die Einträge werden in `gam_discovery_software_inventory` gespeichert und erscheinen dadurch automatisch in der in 40k36s3 eingeführten Software-Suche und den einklappbaren Quellkategorien.

## SSH
Es wird weiterhin der gemeinsame Apache-MINA-SSH-Client verwendet. Passwort- und Key-Authentifizierung, gestufter Verbindungstest, Host-spezifische SSH-Konfiguration und der einmalige sudo-Lauf bleiben erhalten.

## Fallback
Fehlende Paketmanager oder optionale Werkzeuge brechen die Inventarisierung nicht ab. Linux bleibt distributionsübergreifend; es wird kein einzelner Paketmanager vorausgesetzt.
