# 40k36q5 – Einheitliche Betriebssystemzugänge

## Ziel
Die doppelte Bedienstruktur aus Einzelzugang und Mehrfachzugängen wurde entfernt.

## Änderungen
- WinRM, SSH und ADB verwenden jeweils genau eine Listenstruktur.
- Ein einzelner Zugang ist einfach eine Liste mit einem Eintrag.
- Beliebig viele Zielsysteme können parallel angelegt und gespeichert werden.
- Pro Gruppe gibt es `+ Zugang hinzufügen` und `Alle speichern`.
- Jeder Eintrag kann weiterhin separat gespeichert, getestet, aktiviert/deaktiviert und entfernt werden.
- Die Plattformkarten beziehen Aktiv-/Konfigurationsstatus auf die gespeicherten Zugangsprofile.
- Verbindungstests werden pro konkretem Zielsystem im Bereich Betriebssystemzugänge ausgeführt.
- Die alten globalen Einzelkonfigurationen bleiben backendseitig nur als Kompatibilitäts-Fallback für ältere Installationen erhalten und erscheinen nicht mehr als zweite UI-Struktur.
- Die Sprünge `Zur WinRM-/SSH-/ADB-Konfiguration` öffnen automatisch den übergeordneten Bereich und den passenden Listenabschnitt und scrollen anschließend zum Ziel.
- SSH-Test und automatische SSH-Inventarisierung aus 40k36q4b bleiben erhalten.
