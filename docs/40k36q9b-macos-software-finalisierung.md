# 40k36q9b – macOS Softwarefinalisierung

## Behoben
- Strukturierte macOS-Software wird direkt aus dem Inventarisierungsergebnis übernommen; kein fragiles Rückparsen des Discovery-Protokolls mehr.
- Bestehende q9/q9a-Geräte mit leerer Softwaretabelle können beim Laden aus vorhandenen Protokolldetails selbst repariert werden.
- Benutzersoftware wird pro lokalem Benutzer verifiziert.
- `0` Benutzerprogramme gilt nur dann als bestätigt, wenn die Benutzerbereiche vollständig lesbar/geprüft waren.
- Pro Benutzer werden Home-Verzeichnis, Prüfstatus und gefundene Anzahl angezeigt.
- Seltene benutzerspezifische Homebrew-Präfixe werden zusätzlich geprüft.
- macOS-Ausgabegrenzen für Hardwarefelder wurden stabilisiert, damit fehlende Einzelwerte nicht den folgenden `KEY=`-Block verschlucken.
- Betroffen u.a. Chip/Prozessor, Boot-ROM/System-Firmware/SMC und Hostname/Proxy.
- Erfolgsmeldungen nennen die Zahl der strukturiert gespeicherten Softwareeinträge und den Benutzersoftware-Prüfstatus.
