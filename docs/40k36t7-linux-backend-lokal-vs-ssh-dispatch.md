# 40k36t7

## Linux Plattformdispatcher
- Lokaler GAM-Host wird über `LinuxInventoryDiscoveryService` (Backend-lokal) inventarisiert.
- Für Backend-lokal ist kein SSH-Zugang erforderlich.
- Entfernte Linux-Geräte verwenden weiterhin `LinuxNetworkDiscoveryService` über SSH.
- Der Dispatcher erkennt den lokalen Host über IP, MAC oder Hostname.

## Strukturierte Software
- Backend-lokal liefert nun ebenfalls strukturierte Linux-Softwaredatensätze in dieselbe
  `gam_discovery_software_inventory`-Tabelle wie SSH.
- Unterstützte Quellen lokal: APT/dpkg, RPM, Pacman, APK, Flatpak, Snap.
- Damit kann der geplante Backend-lokal/SSH-Vergleich auch die Softwaredaten sinnvoll vergleichen.

## Ziel
Transportweg-unabhängige Informationen sollen fachlich vergleichbar sein.
Unterschiede dürfen nur aus Quelle/Berechtigungen/SSH-spezifischen Daten entstehen.
