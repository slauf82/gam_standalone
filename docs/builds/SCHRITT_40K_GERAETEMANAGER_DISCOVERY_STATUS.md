# Schritt 40k – Gerätemanager und Discovery-Grundlage

- Gerätemanager als Arbeitsansicht oberhalb des bestehenden Geräteverzeichnisses
- automatische lokale Netzwerk-/ARP-Erkennung
- lokale Netzwerkadapter und Windows-Netzwerkgeräte als Registrierungsvorschläge
- Zustände: neu erkannt, bereits registriert, ignoriert
- Übernahme in `geräte_neu` erst nach Bestätigung
- Dublettenprüfung über IP, Gerätename und technische Kennung
- vorbereitete Discovery-Adapter für mDNS, SSDP/UPnP, SNMP, USB und Bluetooth/BLE
- bestehende Geräteverwaltung, Prüfungen und Scan-Engine bleiben fachlich getrennt
