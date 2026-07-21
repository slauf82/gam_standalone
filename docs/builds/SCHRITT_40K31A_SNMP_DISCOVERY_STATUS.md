# Schritt 40k31a – SNMP-Discovery

## Umgesetzt

- integrierter, rein lesender SNMP-v1-Adapter ohne externes Pflichtprogramm
- Abfrage der Standard-System-OIDs `sysDescr`, `sysObjectID` und `sysName`
- parallele Abfrage aller während des Netzwerkscans erkannten IPv4-Ziele
- Live-Übernahme der SNMP-Treffer in die bestehende Discovery-Ergebnisliste
- Zusammenführung mit bereits über ARP, mDNS, SSDP, FRITZ!Box, Home Assistant oder Tuya erkannten Geräten
- Diagnosephase `SNMP` mit Ziel- und Trefferzahl
- Grundklassifizierung für Router, Switches, Access Points, Drucker, NAS, USV und Server
- Herstellererkennung für verbreitete Netzwerk-, Drucker-, NAS- und Security-Anbieter

## Aktivierung

Der vorhandene Schalter **SNMP (integriert)** unter „Netzwerk und Basisdienste“ aktiviert die Abfrage.

Standardwerte:

- Community: `public`
- Port: `161`
- Timeout: `350 ms`

Optionale Konfiguration über Umgebungsvariablen:

- `GAM_SNMP_COMMUNITY`
- `GAM_SNMP_PORT`
- `GAM_SNMP_TIMEOUT_MS`

Alternativ als Java-Systemeigenschaften:

- `gam.discovery.snmp.community`
- `gam.discovery.snmp.port`
- `gam.discovery.snmp.timeout-ms`

## Paketbereinigung

`frontend/node_modules` und `frontend/dist` werden nicht in das Quellpaket aufgenommen. Dadurch bleibt der Build wieder ungefähr auf der Größe von Schritt 40k31, statt bei jedem Folgeschritt kompilierte oder heruntergeladene Frontend-Dateien zu vervielfachen.
