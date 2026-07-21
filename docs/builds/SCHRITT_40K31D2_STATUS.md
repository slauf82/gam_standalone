# Schritt 40k31d2 – kompakte Quellen-Badges

## Umsetzung

- Discovery-Quellen werden in der Geräteansicht als kompakte Badges angezeigt.
- Gleiche Quellen erscheinen pro Gerät nur einmal.
- Verbindungsnamen wie „Smart Life 2“ oder wiederholte technische Bezeichnungen blähen die Anzeige nicht mehr auf.
- Unterstützte Badges: Home Assistant, Tuya Cloud, Windows, FRITZ!Box, SNMP, Docker, MQTT, ONVIF, mDNS, SSDP, USB, Bluetooth, NetBIOS, DHCP und DNS.
- Unbekannte Quellen erhalten ein neutrales Fallback-Badge.
- Die Backend-Zusammenführung verwendet eine stabile Reihenfolge und trennt Quellen sowohl bei Komma als auch beim Mittelpunkt.

## Hinweis zum Build

Die Änderungen wurden auf Basis von 40k31d1 vorgenommen. Ein vollständiger Maven-Testlauf war in der Bearbeitungsumgebung nicht möglich, weil Maven nicht installiert war und der Maven-Wrapper ohne Internetzugang keine Distribution laden konnte.
