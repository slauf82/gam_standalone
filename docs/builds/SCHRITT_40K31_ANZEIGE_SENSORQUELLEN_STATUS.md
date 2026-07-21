# Schritt 40k31 – Anzeige-, Sensordaten- und Quellenkonfiguration

## Umgesetzt

- Neuer aufklappbarer Bereich **Anzeige und Sensordaten** direkt im Gerätemanager.
- Persistente Anzeigeoptionen für kompakte Darstellung, leere/unbekannte Werte, Zeitstempel, Einheiten, Entity-IDs, Favoriten und Änderungsmarkierungen.
- Persistente Home-Assistant-Filter für Diagnose-, Konfigurations-, deaktivierte und versteckte Entitäten.
- Persistente Auswahl der Entitätsdomänen Sensor, Binary Sensor, Switch, Number, Select, Button, Climate, Cover, Light, Fan, Camera und Update.
- Gruppierte vorbereitete Integrationsquellen mit Status **Geplant** und dauerhafter Vormerkung.
- Breite Grundausstattung für typische Privat- und Unternehmensumgebungen: Netzwerk, Windows/Verzeichnisdienste/MDM, Linux/macOS, IoT, Virtualisierung, Container, Cloud/SaaS, NAS/Backup, Netzwerk/Security, Monitoring/Inventarisierung sowie Energie/Gebäudetechnik.
- Die vorhandenen Discovery- und Registrierungsfunktionen aus 40k30n bleiben unverändert erhalten.

## Build-Prüfung

- Frontend-Build erfolgreich.
- Backend-Build in der isolierten Build-Umgebung nicht möglich, da Maven nicht lokal vorhanden war und der Download wegen fehlender DNS-/Internetverbindung scheiterte.
