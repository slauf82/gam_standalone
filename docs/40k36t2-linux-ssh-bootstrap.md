# 40k36t2 – Linux-SSH-Bootstrap und Statuskorrektur

## Behobener Praxisfehler
Ein echter Ubuntu-24.04-SSH-Zugang konnte in GAM Handshake, Authentifizierung und Testbefehl erfolgreich ausführen, während die Linux-Inventarisierung gleichzeitig als „nicht erreichbar“ gemeldet wurde.

Ursache war ein alter kurzer SSH-Banner-Vortest. Antwortete der Banner nicht innerhalb des engen Zeitfensters, wurde der Linux-Inventarpfad vor dem bereits funktionierenden Embedded-SSH-Client abgebrochen. Zusätzlich verlangte die manuelle Linux-Inventarisierung vorhandene Linux-Hinweise, obwohl der erste SSH-Lauf diese Hinweise gerade erst erzeugen soll.

## Änderungen
- Ein konfigurierter SSH-Zugang kann die Linux-Erkennung selbst bootstrappen.
- Der rohe Banner-Vortest ist nur noch Diagnose und blockiert die Inventarisierung nicht mehr.
- Der Embedded-SSH-Client wird bei konfiguriertem Zugang direkt verwendet.
- Linux wird anschließend anhand echter SSH-Inventardaten (`KERNEL`, `DISTRO`, `OS`) bestätigt.
- Erfolgreiches SSH und fehlgeschlagene Betriebssystemerkennung werden vom Zustand „nicht erreichbar“ getrennt.
- Strukturierte Linux-Softwaredaten werden auch beim automatischen Plattformlauf gespeichert.

Basis: 40k36t1.
