# 40k36r – macOS backend-lokal

## Ziel
Ein bereits von GAM erkanntes macOS-Gerät wird ohne SSH inventarisiert, wenn es sich um den Rechner handelt, auf dem das GAM-Backend selbst läuft.

## Erkennung
Vor der macOS-Einzelinventarisierung prüft GAM auf einem macOS-Backend:
- Seriennummer des lokalen Macs gegen die registrierte Seriennummer,
- registrierte IP-Adresse gegen die aktuell lokalen Netzwerkadressen,
- Hostname/Computername als zusätzlicher Fallback.

Seriennummer und lokale IP sind die bevorzugten Merkmale. Dadurch funktioniert auch ein Mac mit WLAN und Ethernet.

## Inventarisierung
- Backend-lokal: vorhandenes macOS-Inventarskript wird direkt mit `/bin/sh` ausgeführt.
- Backend-lokal mit erhöhten Rechten: lokaler `sudo`-Pfad.
- Kein SSH und keine „Entfernte Anmeldung“ erforderlich.
- Remote-Mac: bestehender SSH-Pfad bleibt unverändert.
- Ergebnisse und Softwareinventar werden weiterhin in dieselbe registrierte Geräteidentität geschrieben.
- Protokoll kennzeichnet die Quelle als `Backend-lokal · macOS`.

## Allgemeine Grundlage
Die Trennung `backend-lokal` vs. `remote` ist damit für macOS umgesetzt und bildet die Grundlage, dieselbe Dispatcher-Idee anschließend konsistent für Linux und Windows zu verwenden. Android bleibt bewusst bei ADB.
