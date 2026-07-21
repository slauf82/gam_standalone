# Schritt 40k12 – Discovery-Async-Timeout

## Ziel

Die vollständige Discovery-Pipeline darf nicht mehr durch das standardmäßige Spring-MVC-Async-Timeout nach ungefähr 30 Sekunden beendet werden.

## Umsetzung

- `spring.mvc.async.request-timeout` ist nun auf 180000 ms voreingestellt.
- Der Wert bleibt über `GAM_ASYNC_REQUEST_TIMEOUT_MS` konfigurierbar.
- Die kurzen, quellenspezifischen Timeouts für Neighbor-Befehle und SSDP bleiben unverändert bestehen.
- Nach der SSDP-Phase können damit Konsolidierung, globaler Abschluss, Stream-Schließen und die 100-%-Anzeige zuverlässig übertragen werden.

## Erwarteter Ablauf

1. Lokale Adapter
2. Erste ARP-/Neighbor-Auswertung
3. Aktiver Subnetzscan
4. Zweite ARP-/Neighbor-Auswertung
5. SSDP / UPnP
6. Konsolidierung
7. Globaler Abschluss
8. Stream-Schließen
9. 100 % / Suche abgeschlossen

## Konfiguration

Optional kann das Timeout beim Start überschrieben werden:

```text
GAM_ASYNC_REQUEST_TIMEOUT_MS=180000
```
