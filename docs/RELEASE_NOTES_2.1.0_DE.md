# GAM Standalone 2.1.0 – Release Notes (Deutsch)

**Veröffentlichungsstand:** 18. Oktober 2026  
**Technische Basis:** 40k37e56c-fix48  
**Release-Typ:** Stabile Version 2.1.0

## Überblick

GAM Standalone 2.1.0 stellt eine plattformübergreifende Geräteerkennung und Inventarisierung mit gemeinsamen Geräteansichten bereit. Die Anwendung umfasst weiterhin die vorhandenen GAM-Funktionen unter anderem für Rechnungen, Benutzer und Rechte sowie Geräteverwaltung. Der bisherige Funktionsumfang wurde für dieses Release beibehalten.

## Geräteverwaltung und Inventarisierung

- **Windows:** lokale Inventarisierung und Ferninventarisierung über WinRM; bei Bedarf Wechsel zwischen modernem WinRM-Stack und Windows-nativem PowerShell/SSPI-Verfahren.
- **Linux:** lokale Inventarisierung und Ferninventarisierung über SSH.
- **macOS:** lokale Erfassung und SSH-Anbindung im plattformübergreifenden Gerätemanager.
- **Android:** Erfassung über ADB (USB/WLAN).
- **iOS/iPadOS:** Apple-Geräteerkennung und Inventarisierungswege über verfügbare USB-/WLAN-Schnittstellen, abhängig von Gerät, Betriebssystem und Berechtigungen.
- Mehrere Erkennungsquellen und Geräteidentitäten werden zusammengeführt; automatische Merge-Vorschläge setzen belastbare, erfolgreich inventarisierte Identitätsmerkmale voraus. Unterschiedliche Mobilplattformen werden nicht allein aufgrund gleicher Geräteklasse zusammengeführt.

## Abschließende Korrekturen

- Historische, teils irrtümlich als Erkennungsquellen angezeigte Werte werden nicht mehr als lange Liste einzelner Löschaktionen dargestellt; die gespeicherten Daten bleiben erhalten (fix47).
- Bei einem eindeutig inventarisierten Windows-Gerät werden widersprüchliche historische Linux-Analysefelder nicht zusätzlich eingeblendet (fix47; Darstellungsregel, keine Löschung).
- Inventarisierungsdetails blenden vollständig leere Einträge aus (fix47).
- Ein zusätzlicher externer `whoami.exe`-Aufruf im SSPI-Prüfweg wurde ersetzt (fix47).
- Die Hauptzeilen der Gerätetabelle behalten ihre Breite, wenn umfangreiche Inventarisierungsdetails aufgeklappt werden; lange Werte bleiben in den Details zugänglich (fix48).
- Frühere Verbesserungen umfassen unter anderem Node.js-Wiederverwendung, den Windows-Frontend-Start, Plattform-/Merge-Schutz und die Bereinigung von Java-Compilerwarnungen (fix38–fix46).

## Bestätigte Praxistests

Erfolgreich gemeldet wurden Windows lokal und über WinRM/SSPI, Linux backend-lokal und über SSH sowie die korrigierte Hauptzeilen-Darstellung. macOS- und Mobilgerätepfade wurden in der Entwicklung getestet. Dies ersetzt keine vollständige automatisierte Testmatrix aller Betriebssystemversionen und Gerätekonfigurationen.

## Installation und Aktualisierung

Das Archiv ist ein **Quell-/Projektpaket**. Backend und Frontend werden wie bisher mit den enthaltenen Startskripten gestartet (`start_backend.bat` / `.sh`, `start_frontend.bat` / `.sh`). Bestehende Konfigurations-, Datenbank- und Nutzerdaten vor einem Austausch sichern; lokale Anpassungen nicht unkontrolliert überschreiben. Hinweise zum Projekt und Start befinden sich unter `docs/`.

## Prüfhinweis

Die Integrität des Release-ZIP wurde geprüft. Ein vollständiger Maven-/TypeScript-Build ist in der hier verwendeten Umgebung nicht bestätigt; diese Schritte sollten beim GitHub-Release auf dem Zielsystem beziehungsweise in CI ausgeführt werden.

## Nächste Schritte

Nach Version 2.1.0 ist die Trennung in Plattformkern, öffentliche Core-Module und optionale Erweiterungsmodule mit eigenständiger Paket- und Versionsverwaltung geplant. Das `.gam`-Modulpaketformat ist **nicht** Bestandteil dieses Releases.
