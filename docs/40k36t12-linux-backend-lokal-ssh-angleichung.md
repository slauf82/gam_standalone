# 40k36t12

Linux Backend-lokal wurde an den umfangreicheren SSH-Inventarisierungspfad angeglichen.

Erweitert sichtbar sind insbesondere vollständige laufende Dienste, Netzwerkadapter
mit IPv4, Distribution-ID/-Version, Paketmanager, Software-Gesamtzahl, Root-Dateisystem,
LUKS/dm-crypt/crypttab/Swap, LVM/RAID/Btrfs/ZFS, TPM/systemd-cryptenroll,
Storage-Werkzeuge, Berechtigungsdaten und aktualisierbare Pakete.

Ziel: Derselbe Linux-Rechner liefert über Backend-lokal und SSH fachlich weitgehend
denselben Datensatz. Nur transport- und zeitabhängige Werte dürfen abweichen.
