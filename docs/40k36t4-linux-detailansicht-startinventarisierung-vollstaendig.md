# 40k36t4

Konsolidiert die Linux-SSH-Inventarisierung und deren Darstellung.

- Start-/automatische Linux-Inventarisierung verwendet denselben verifizierten Pfad wie der manuelle Lauf.
- Strukturierte Linux-Software wird auch nach Startinventarisierungen direkt gespeichert; kein Rückfall auf Software (0).
- Linux-Detailansicht berücksichtigt hostbezogene SSH-Profile aus Betriebssystemzugängen.
- Die Geräteidentität wird beim Öffnen und nach Plattform-Inventarisierungsereignissen neu geladen.
- Alle bereits vom SSH-Inventarisierer erfassten System-, Basis-Hardware-, Speicher-, Netzwerk-, Rollen-, Berechtigungs- und Statusdaten werden sichtbar gemacht.
- Grunddaten priorisieren eine verifizierte Linux-/SSH-Quelle vor Windows ARP als Anzeigequelle; weitere Quellen bleiben erhalten.
- Rohfelder VENDOR/UUID/SERIAL/CHASSIS werden sauber zeilenweise ausgegeben; verhindert zusammengeklebte Werte wie SERIAL=CHASSIS=3.
- Alte offensichtlich fehlerhafte SERIAL=/CHASSIS=-Serienwerte werden bei einer verifizierten Inventarisierung bereinigt.
- Software-Zähler erhalten ein Leerzeichen vor der Klammer.
