# GAM 2.1.0 – 40k37e56c-fix43

## Automatische Merge-Kandidaten
- Es werden nur noch Paare angezeigt, für die **beide** Geräte mindestens einen erfolgreich abgeschlossenen Plattform-Inventarisierungslauf (`gam_discovery_platform_inventory_runs`, Status `ERFOLGREICH`) besitzen.
- Zusätzlich ist eine exakte Übereinstimmung einer belastbaren Gerätekennung notwendig: Seriennummer/Geräte-ID, MAC-Adresse, Hardware-/System-UUID, Provisioning-UDID, SSH-Hostkey oder ADB-Build-Fingerprint.
- Gemeinsame Bezeichnungen, Gerätekategorien, Netzwerkquellen oder eine mögliche Kombination Apple-WLAN/FRITZ!Box sind **kein eigenständiger Identitätsnachweis**.
- Bestehende Plattform-Konfliktsperren bleiben aktiv; starke technische Widersprüche werden herausgefiltert.
- Die Filterung betrifft `DeviceMergeService.findCandidates()`, nicht die allgemeine Quellenerkennung oder bewusst manuell ausgelöste Merge-Operationen.

## Bestandsschutz
- Kein Eingriff in Apple-Transport, PMD3, RemotePairing, USERSPACE_RSD, USB/WLAN, Plattforminventarisierung oder Datenbank-Inhalte.
- Kein automatisches Bereinigen oder Löschen bestehender Geräte.
- Das Matching während eines laufenden Suchlaufs (`DeviceIdentityConfidenceEngine`) bleibt unverändert. Dessen automatische Merge-Logik sollte separat geprüft werden, wenn auch dort solche Fälle beobachtet werden.

## Prüfgrenzen
- ZIP-CRC und Quelltext-/Strukturprüfungen durchgeführt.
- Vollständiger Maven-Build nicht möglich: `mvn` ist in der Ausführungsumgebung nicht installiert.
