# Schritt 40k5 – Discovery Engine 2.0

- Plattformabhängige Discovery für Windows, Linux und macOS.
- Windows: PowerShell `Get-NetNeighbor`, `arp -a`, aktiver IPv4-Subnetzscan, erneute Nachbartabelle und SSDP.
- Linux: `ip neigh`, `arp`, aktiver IPv4-Subnetzscan, erneute Nachbartabelle und SSDP.
- macOS: `arp -an`, aktiver IPv4-Subnetzscan, erneute Nachbartabelle und SSDP.
- Suche endet erst, nachdem alle verfügbaren Quellen abgeschlossen sind.
- Treffer werden live und dedupliziert übertragen.
- PowerShell 7 ist nicht erforderlich; vorhandene Windows PowerShell wird genutzt. Fehlt PowerShell, laufen ARP, aktiver Scan und SSDP weiter.
- SNMP, USB und Bluetooth/BLE bleiben für spätere spezialisierte Adapter vorbereitet.
