# 40k37e55k – FRITZ!Box-iPad wieder separat sichtbar

Ausgangsbasis: 40k37e55j (Linux pymobiledevice3 venv).

## Änderung
- FRITZ!Box-TR-064-Geräte werden wieder als eigenständige Discovery-Treffer emittiert.
- IP-Adresse und WLAN-MAC bleiben damit am FRITZ!Box-Rohdatensatz sichtbar.
- Der FRITZ!Box-Treffer wird vor einer Benutzerentscheidung nicht in Apple USB/WLAN/CoreDevice eingemischt.
- Neighbor/ARP darf den FRITZ!Box-Datensatz weiterhin ergänzen.
- Der bestehende Apple-WLAN/CoreDevice ↔ FRITZ!Box-Sonderfall bleibt bestätigungspflichtig; kein automatischer Apple/FRITZ-Merge.
- Ziel: iPad per manueller Zusammenführung um die von der FRITZ!Box gelieferte LAN-IP ergänzen und damit den Linux-Stand an den Windows-Teststand angleichen.

## Diagnose
Neue Logzeile:
`[DISCOVERY][40k37e55k][FRITZBOX][STANDALONE] ... ip=... mac=...`

## Erwarteter Test
1. Suchlauf mit aktivierter FRITZ!Box-Quelle und iPad im WLAN starten.
2. iPad muss als Apple-Mobile-Gerät und zusätzlich als FRITZ!Box-Treffer sichtbar sein.
3. FRITZ!Box-Treffer muss die LAN-IP des iPads zeigen.
4. Beide Treffer manuell zusammenführen.
5. Das zusammengeführte Gerät muss die LAN-IP behalten.
