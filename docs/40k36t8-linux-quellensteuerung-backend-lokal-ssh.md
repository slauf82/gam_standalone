# 40k36t8

## Verbindliche Linux-Quellensteuerung

Der Plattformdispatcher prüft die aktivierten Erkennungsquellen unmittelbar vor
dem Start des Transportwegs.

| Backend-lokal (`LINUX_INVENTORY`) | SSH/Linux-Netzwerk (`LINUX_NETWORK`) | Verhalten |
|---|---|---|
| an | aus | lokaler GAM-Host: Backend-lokal; kein SSH |
| aus | an | auch lokaler GAM-Host: SSH, sofern Zugang konfiguriert |
| an | an | lokaler GAM-Host bevorzugt Backend-lokal; Remote über SSH |
| aus | aus | kein Linux-Inventarisierungslauf |

Zusätzlich prüft der SSH-Pfad vor dem eigentlichen Lauf, ob für das Ziel ein
aktivierter SSH-Zugang mit Benutzer vorhanden ist. Dadurch sind UI-Schalter und
tatsächlicher Plattformdispatcher jetzt konsistent.
