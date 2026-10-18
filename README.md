GAM Standalone
Version 2.1.0 · Plattformübergreifende Geräteinventarisierung und Verwaltungssoftware
[English README](README_EN.md) · [Release Notes 2.1.0](docs/RELEASE_NOTES_2.1.0_DE.md) · [Entwicklungsplan](docs/DEVELOPMENT_PLAN.md)
GAM (GAM Standalone / GAM 2.0) verbindet Geräteerkennung und Inventarisierung mit Verwaltungsfunktionen in einer webbasierten Anwendung. Ziel ist eine nachvollziehbare, stabile Verwaltung unterschiedlicher Geräte und betrieblicher Abläufe – mit einer langfristig modularen Architektur.
Aktueller Stand: GAM 2.1.0 ist die stabilisierte technische Basis. Der geplante GAM Core, der .gam-Modulmanager, ein Erweiterungskatalog sowie kostenlose und kostenpflichtige Erweiterungsmodule sind Zukunftsvorhaben, keine bereits in 2.1.0 enthaltenen Funktionen.

Was GAM 2.1.0 bietet
Geräteerkennung und Inventarisierung
Plattform	Unterstützte Wege
Windows	Backend-lokale Inventarisierung; remote über WinRM, einschließlich Windows-nativem PowerShell/SSPI-Fallback
Linux	Backend-lokale Inventarisierung; remote über SSH
macOS	Lokale Inventarisierung; SSH-Anbindung
Android	ADB über USB und WLAN
iOS / iPadOS	Apple-Erkennung und Inventarisierungswege über USB/WLAN, abhängig von Geräteversion, Berechtigungen und verfügbarem Transport


Zum Gerätebereich gehören außerdem:
- Erkennungsquellen und Betriebssystemzugänge konfigurieren, Verbindungen testen und Inventarisierungen ausführen.
- Geräte über mehrere Erkennungswege hinweg zuordnen; IP- und MAC-Adressen sowie die Quellen nachvollziehen.
- Vorsichtige automatische Zusammenführung auf Basis belastbarer Geräteidentitäten, statt allein anhand von Gerätenamen oder allgemeiner Geräteklassen.
- Umfangreiche Inventarisierungsdetails für Betriebssystem, Hardware und Software sowie weitere plattformabhängige Informationen.
- Stabil dargestellte Geräte-Hauptzeilen, auch wenn sehr lange Inventarisierungsdetails aufgeklappt werden.
Weitere vorhandene Verwaltungsfunktionen
GAM enthält neben dem Gerätemanager bereits Funktionen für Rechnungen und Rechnungsdaten, Benutzer- und Rechteverwaltung sowie weitere bestehende Verwaltungsbereiche, unter anderem Lager-/Materialfunktionen. Der konkrete Funktionsumfang unterscheidet sich je nach Bereich und Konfiguration.
Technische Grundlage
- Backend: Java 21, Spring Boot, Maven
- Frontend: React, TypeScript, Vite
- Datenbank: MariaDB/MySQL
- Bereitstellung: vollständiges Projekt mit Startskripten für Windows, Linux und macOS
Das bereitgestellte ZIP ist ein Projekt-/Quellcodepaket, kein fertig kompiliertes Installationsprogramm. Beim Erststart können Downloads für Entwicklungs-/Laufzeitkomponenten und Abhängigkeiten nötig sein.
Schnellstart
1. Das vollständige Release-Paket in ein eigenes Verzeichnis entpacken.
2. Backend starten:
   - Windows: start_backend.bat
   - Linux/macOS: ./start_backend.sh
3. Die initiale Einrichtung einschließlich Datenbankkonfiguration abschließen. Als Ausgangspunkt für manuelle Einstellungen dient env.example.
4. Frontend starten:
   - Windows: start_frontend.bat
   - Linux/macOS: ./start_frontend.sh
5. Die vom Frontend angezeigte lokale Adresse im Browser öffnen.
Hinweise: Für die erstmalige Einrichtung ist gegebenenfalls Internetzugriff erforderlich. Inventarisierungszugriffe auf andere Geräte benötigen passende Berechtigungen und aktivierte Dienste (beispielsweise WinRM, SSH oder ADB). Vor einem Upgrade sollten Datenbank, Konfiguration und eigene Anpassungen gesichert werden. Die Startskripte setzen die benötigte Toolchain teilweise automatisch auf.
Was ist neu in 2.1.0?
Die Version konsolidiert die plattformübergreifende Inventarisierung und verbessert insbesondere die Windows-WinRM-Anbindung mit SSPI-Fallback, die Auswahl belastbarer Merge-Kandidaten sowie den Frontend-Start. Zuletzt wurden fehlerhaft angezeigte historische Quellen, ein störender whoami.exe-Hilfsprozessaufruf und das Layout umfangreicher Detailansichten korrigiert.
Die vollständige Übersicht einschließlich Teststand und Einschränkungen steht in den [deutschen Release Notes](docs/RELEASE_NOTES_2.1.0_DE.md) und [englischen Release Notes](docs/RELEASE_NOTES_2.1.0_EN.md).
Ausblick: die modulare GAM-Plattform
Die kommenden Versionen sollen GAM schrittweise zu einer erweiterbaren Verwaltungs- und Integrationsplattform entwickeln:
Geplante Version	Schwerpunkt
2.1.5	GAM Core, Modulgrenzen, Schnittstellen und Grundlage des .gam-Paketformats
2.2.x	Zusätzliche Infrastruktur-/Virtualisierungsanbindungen und vertiefte Hardwareanalyse
2.3–2.4	Fachmodule, Integration externer Systeme, Rechte und Skalierbarkeit
2.5.0	Dokumentation, Qualitätssicherung und Produktreife
3.0–3.5	KI-Unterstützung und kontrollierte Automatisierung


Produktvision: Ein eigenständig nutzbarer, kostenlos zugänglicher GAM Core mit kostenlosen und optionalen kostenpflichtigen Erweiterungen. Module sollen separat verwaltet und versioniert werden können. Auch die Trennung von öffentlich entwickelten und privat gepflegten Erweiterungen ist vorgesehen. Diese Erweiterungs- und Lizenzierungsarchitektur ist noch nicht Bestandteil von 2.1.0.
Alle geplanten Versionsziele sind veränderbar. Weitere Details: [Entwicklungsplan](docs/DEVELOPMENT_PLAN.md).
Dokumentation
- [Release Notes 2.1.0 (Deutsch)](docs/RELEASE_NOTES_2.1.0_DE.md)
- [Release Notes 2.1.0 (English)](docs/RELEASE_NOTES_2.1.0_EN.md)
- [Entwicklungsplan / Roadmap](docs/DEVELOPMENT_PLAN.md)
- [GitHub-Release-Beschreibung](docs/GITHUB_RELEASE_2.1.0.md)
- [Weitere technische Dokumentation](docs/)
Projekt und Beiträge
GitHub: slauf82/gam_standalone
Fehlerberichte und reproduzierbare Testfälle sind hilfreich, besonders für unterschiedliche Plattformversionen und Verbindungswege. Vor größeren Änderungen sollten die bestehenden Inventarisierungs-, Authentifizierungs- und Geräteidentitätsfunktionen erneut geprüft werden.
Lizenz, Urheberrecht und offizielle Veröffentlichungen
GAM Standalone 2.1.0 wird als freie Software unter der GNU General Public License Version 3 (GPL-3.0-only) veröffentlicht. Der vollständige Lizenztext steht in LICENSE. Fremdkomponenten können eigenen Lizenzbedingungen unterliegen.
Die ursprüngliche Entwicklung und die offiziellen Veröffentlichungen von GAM erfolgen durch Sebastian Lauf. Bitte erhalte bei Weitergabe die nach GPLv3 erforderlichen Urheberrechts-, Lizenz- und sonstigen rechtlichen Hinweise.
Erweiterungen und eigene Fassungen sind willkommen: GAM darf unter den Bedingungen der GPLv3 genutzt, untersucht, verändert und weitergegeben werden. Das umfasst auch eigene Erweiterungen und die Nutzung von KI-Werkzeugen bei Änderungen oder beim Kompilieren. Bei der Weitergabe veränderter Fassungen müssen Änderungen entsprechend den GPLv3-Vorgaben kenntlich gemacht und die geltenden Lizenzpflichten eingehalten werden.
Offizielle GAM-Releases sind ausschließlich Veröffentlichungen des ursprünglichen Projekts beziehungsweise ausdrücklich autorisierter Herausgeber, insbesondere über das offizielle Repository. Veränderte Fassungen oder Builds Dritter sind inoffiziell und dürfen nicht irreführend als offizielle GAM-Veröffentlichungen dargestellt werden.
Die GPLv3-Lizenzierung des Quellcodes gewährt keine eigenständigen Markenrechte an GAM-Namen, Logos oder Projektkennzeichen. Für deren kennzeichenmäßige Verwendung können gesonderte Regeln gelten. Dieser Hinweis begründet nicht automatisch eine eingetragene Marke.
Hinweis zu KI-Werkzeugen: Es gibt kein Verbot KI-gestützter Änderungen oder Neukompilierungen. Zur besseren Nachvollziehbarkeit und Unterstützung empfiehlt das Projekt für den regulären Einsatz die offiziellen Releases. Eigene Builds sind bei korrekter Kennzeichnung weiterhin im Rahmen der GPLv3 zulässig.
Künftige Erweiterungsmodule: Ihre jeweiligen Lizenzen werden gesondert ausgewiesen. Bei technisch eng mit dem GPLv3-Core verbundenen Modulen ist die Lizenzkompatibilität vor dem Vertrieb zu prüfen; ein privates Repository allein hebt GPL-Pflichten nicht auf.
