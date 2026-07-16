# GAM 2.1.0 – Preview 1

Dieser Release basiert auf dem Entwicklungsstand **40h3**.

Enthalten sind unter anderem die abgeschlossene Migration von GAM 1.0, die neue Workflow-Infrastruktur sowie Kommunikations-, Aufgaben-, Labor- und Wartezimmerworkflow. Die neuen Module sind mehrsprachig eingebunden und besitzen eigene, wiedererkennbare Symbole.

Im Ordner `database` liegen eine leere Datenbankstruktur und eine anonymisierte Demonstrationsdatenbank für diesen Preview-Stand.

---

## Aktualisierung der Preview 1: vereinfachter Erststart

- automatische Prüfung auf ein geeignetes **JDK 21**
- lokale Einrichtung von **Eclipse Temurin 21**, wenn Java fehlt oder zu alt ist
- keine systemweite Java-Installation und keine Änderung bestehender Java-Konfigurationen
- Erststart-Assistent mit Auswahl zwischen leerer Praxis und anonymisierter Beispieldatenbank
- automatische MariaDB-Vorbereitung unter Windows

### Erste Schritte

1. `start-backend.bat` unter Windows beziehungsweise `start-backend.sh` unter Linux/macOS starten und dem Assistenten folgen.
2. Für notwendige Downloads während der erstmaligen Einrichtung ist Internetzugriff erforderlich.
3. Nach erfolgreichem Backendstart `start-frontend.bat` beziehungsweise `start-frontend.sh` starten.

> **Hinweis:** Der Internetzugriff wird nur für erforderliche Downloads während der Einrichtung benötigt. Der Normalbetrieb von GAM 2.0 ist offline möglich.
