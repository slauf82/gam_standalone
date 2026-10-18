# 40k36p6cb – Erkennungsquellen finalisieren

- Einheitliche Bereitschaftszustände für alle Plattform-Erkennungsquellen: Nicht aktiviert, Nicht eingerichtet, Konfiguriert, Prüfung läuft, Bereit und Fehler.
- Verbindungstests liegen vollständig im Bereich Erkennungsquellen.
- WinRM nutzt weiterhin den realen Zielhost-Test.
- Linux SSH und macOS SSH erhalten einen gemeinsamen zentralen Testhost und einen echten SSH-Verbindungstest ohne vorherige Gerätezuordnung.
- Android USB/WLAN prüft den realen ADB-Server- und Adapterstatus.
- Lokale Windows-, Linux- und macOS-Quellen prüfen die Verfügbarkeit ihres lokalen Adapters.
- iOS/iPadOS führen bis zum nativen Adapter eine klar gekennzeichnete Browser-/Vorbereitungsprüfung aus.
- Diagnose zeigt letzten Test, Antwortzeit, letzten Fehler und Inventarisierungstiefe.
- Testergebnisse bleiben browserseitig erhalten und sind nach einem Neuladen weiterhin sichtbar.
- Nicht bereite Quellen erhalten einen eindeutigen Workflow-Hinweis.
