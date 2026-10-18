# 40k36r2 – Loopback gehört zum Backend-Rechner

- `127.0.0.1`, `::1` und `0:0:0:0:0:0:0:1` werden als Loopback-Adressen behandelt.
- Ein halblokaler Browser-Client, der GAM über localhost aufruft, wird nicht mehr mit Loopback als eigenständige physische Geräteadresse gespeichert. GAM ersetzt die Loopback-Adresse durch eine echte aktive lokale Backend-Adresse (IPv4 bevorzugt).
- macOS-Einzelinventarisierung erkennt eine vorhandene Loopback-Adresse ausdrücklich als backend-lokal und fällt nicht mehr fälschlich auf SSH zurück.
- Die Merge-Bewertung behandelt Loopback gegen eine echte Geräteadresse nicht mehr als „abweichende IP-Adresse bei gleichzeitig erreichbaren Geräten“. Loopback ist kein zweiter Netzwerkweg und kein physischer Gegenbeweis.
- Bereits vorhandene alte Loopback-Datensätze bleiben sichtbar und können dadurch leichter mit dem echten Backend-Gerät zusammengeführt werden; neue Browser-Treffer sollen keine neuen Loopback-Dubletten mehr erzeugen.
