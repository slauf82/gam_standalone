# Schritt 40k30a – Integrierte mDNS-/Bonjour-Engine

## Behobenes Problem

Die Discovery meldete auf Systemen ohne `dns-sd`, Bonjour oder `avahi-browse`:

- `status=SKIPPED`
- `Kein unterstütztes mDNS-Werkzeug verfügbar`

Damit hing mDNS von separat installierter Betriebssystemsoftware ab.

## Umsetzung

- Neue dependency-freie Java-mDNS-Engine über UDP-Multicast `224.0.0.251:5353`.
- Funktioniert grundsätzlich unter Windows, Linux und macOS ohne Zusatzinstallation.
- DNS-Kompression sowie PTR-, SRV-, A- und TXT-Records werden ausgewertet.
- Diensttyp-Ermittlung über `_services._dns-sd._udp.local`.
- Zusätzliche Abfragen verbreiteter Dienste, unter anderem HTTP(S), Drucker, SSH, SMB, AirPlay, Google Cast, Home Assistant, MQTT und ONVIF.
- Gefundene Service-Instanzen werden über SRV und A zur Geräte-IP aufgelöst.
- Vorhandene externe Werkzeuge bleiben als Fallback erhalten.
- Capability `mdns` ist nun immer verfügbar und nennt den integrierten Modus.

## Diagnosezustände

- `COMPLETED_NATIVE`: integrierte Engine erfolgreich ausgeführt.
- `COMPLETED_FALLBACK`: externes Werkzeug wurde als Fallback genutzt.
- `FAILED_NATIVE_NO_FALLBACK`: native Erkennung schlug fehl und kein Fallback war vorhanden.
- `FAILED`: auch der externe Fallback schlug fehl.

Ein leerer Trefferbestand ist kein Fehler: Die Discovery gilt als abgeschlossen, sobald der Multicast-Lauf technisch durchgeführt wurde.
