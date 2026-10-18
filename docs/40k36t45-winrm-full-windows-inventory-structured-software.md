# 40k36t45 – vollständige Windows-Inventarisierung über WinRM

- Basis: 40k36t44.
- Der alte WinRM-Minimalcollector (`SOFTWARE_SAMPLE`) wurde entfernt.
- WinRM verwendet jetzt dieselben modularen PowerShell-Abfragen wie die lokale Windows-Inventarisierung:
  SYSTEM, BIOS/BOARD, CPU/MEMORY, STORAGE, NETWORK, SOFTWARE, SECURITY, MAINTENANCE und UPDATES.
- Der bereits funktionierende bidirektionale Chunk-/Marker-Transport bleibt bestehen.
- Vollinventar-Timeout für den modularen Remote-Lauf: 90 Sekunden.
- GAMKV-Ausgaben werden remote genauso Base64-sicher geparst wie lokal.
- Windows-Software wird zusätzlich strukturiert in `gam_discovery_software_inventory` gespeichert.
- HKLM-Quellen werden als SYSTEM, HKCU-Quellen als USER geführt; Version, Registry-Paket-ID, Installationspfad und Quelle werden mitgeführt.
- Das Backend loggt nach dem Decode eine kompakte Plausibilitätszeile mit Grunddaten, Softwareanzahl, Laufwerken und Netzwerkadaptern.
