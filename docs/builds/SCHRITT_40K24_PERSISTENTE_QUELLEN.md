# Schritt 40k24 – Persistente Quellenverwaltung

- FRITZ!Box-, Home-Assistant- und Smart-Life-/Tuya-Quellen werden serverseitig in MariaDB gespeichert.
- Aktiv-/Aus-Zustände aller lokalen Discovery-Quellen werden in `gam_discovery_builtin_sources` gespeichert.
- Die Konfiguration liegt nicht im Frontend-Build und bleibt beim Darüberkopieren einer neuen Version erhalten, sofern dieselbe GAM-Datenbank weiterverwendet wird.
- Neue testbare Quellen: Reverse-DNS-Namensauflösung und mDNS/Bonjour.
- Alle lokalen Quellen können einzeln aktiviert oder deaktiviert werden.
