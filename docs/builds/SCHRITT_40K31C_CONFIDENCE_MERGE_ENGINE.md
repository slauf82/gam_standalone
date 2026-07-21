# Schritt 40k31c – Confidence-/Merge-Engine

Die Discovery-Zusammenführung verwendet ab diesem Stand keine starre IP-/MAC-Regel mehr, sondern eine gewichtete Identitätsbewertung.

## Bewertung
- Hardware-/Geräteseriennummer: bis 95 Punkte
- SNMP-Identität: 90 Punkte
- MAC-Adresse: 85 Punkte
- Tuya-/MQTT-Geräte-ID: 80 Punkte
- Hostname/Gerätename: 50 Punkte
- Gerätetyp: 15 Punkte
- Hersteller: 10 Punkte
- IP-Adresse: 5 Punkte
- Bestätigung durch mehrere unabhängige Quellen: bis 10 Zusatzpunkte

## Entscheidungen
- ab 95 Punkten: automatische Zusammenführung
- 60–94 Punkte: möglicher Dublettentreffer
- darunter: getrennte Geräte

Eine identische IP-Adresse allein führt ausdrücklich nie zu einer automatischen Zusammenführung. Abweichende starke Identitätsmerkmale begrenzen die automatische Bewertung. Die Engine ist für WMI/WinRM, SSH, Proxmox, Docker und MQTT erweiterbar.
