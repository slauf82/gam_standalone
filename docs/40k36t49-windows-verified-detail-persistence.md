# 40k36t49 – Windows-Details lokal und remote sichtbar

## Ursache
Der modulare lokale Windows-Collector lieferte bereits umfangreiche Werte (OS, BIOS, CPU/RAM,
Storage, Netzwerk, Software, Sicherheit, Updates, Berechtigungen). Der Dispatcher speicherte
diesen technisch verifizierten Inventarlauf aber mit `recordDiscoveryHit`. Bei Geräten, die
ursprünglich aus einer anderen Discovery-Quelle stammten, wurde die neue Windows-Inventarquelle
deshalb als `pending_discovery_protocol` behandelt. Die sichtbare Geräteansicht blieb bei den
alten Grunddaten.

## Fix
- Backend-lokale Windows-Inventarisierung verwendet jetzt `recordVerifiedInventoryHit`, genau wie
  verifizierte SSH-/WinRM-Inventare.
- Die vollständigen Windows-Detailfelder werden damit sofort in das sichtbare Protokoll übernommen.
- `SOFTWARE_DETAILS` des lokalen Windows-Laufs werden strukturiert in
  `gam_discovery_software_inventory` persistiert.
- Dasselbe gilt für einen echten lokalen SYSTEM-Lauf.
- Remote-WinRM bleibt auf dem in t47/t48 bewiesenen Vollinventar-/Chunk-Pfad.
- UI kennzeichnet einen lokalen Windows-Datensatz als `PowerShell backend-lokal` statt pauschal WinRM.
