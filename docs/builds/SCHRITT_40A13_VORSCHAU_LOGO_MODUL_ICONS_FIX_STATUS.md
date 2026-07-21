# Schritt 40a13 – Vorschau-Logo und Modul-Icons

## Änderungen

- Gesellschaftslogos werden in der Rechnungsvorschau auch dann korrekt aufgelöst, wenn die Datenbank nur einen Dateinamen wie `KOPFZENTRUM_LOGO.png` oder einen relativen Pfad enthält.
- Absolute Webpfade, Uploadpfade und externe HTTP(S)-Logos bleiben unterstützt.
- Administration verwendet nun auch in der Navigationsleiste dasselbe moderne goldene Zahnrad wie im Login-Dialog und Dashboard.
- Kommunikation verwendet überall eine einheitliche Kachel mit blauem Briefumschlag. Die bisherige blaue Farbe des Umschlags bleibt erhalten.

## Kurztest

1. Rechnung mit einer Gesellschaft öffnen und die Vorschau anzeigen: Das zugeordnete Logo muss sichtbar sein.
2. PDF erzeugen: Das Logo muss weiterhin wie bisher sichtbar sein.
3. Login-Dialog, Dashboard und Navigation vergleichen: Administration muss überall das goldene Zahnrad verwenden.
4. Kommunikation in Login, Dashboard und Navigation prüfen: überall blaue Briefumschlag-Kachel.
5. Browser bei alten Icons einmal mit `Strg+F5` neu laden.
