# GAM 2.1.0 Preview 1 – aktualisierter Erststart

Diese aktualisierte Preview vereinfacht die Ersteinrichtung weiter.

## Automatisch geprüft und eingerichtet

- **JDK 21:** Vorhandene geeignete Java-Installation wird verwendet. Fehlt sie, lädt GAM Eclipse Temurin 21 lokal nach `runtime/java` herunter.
- **MariaDB:** Unter Windows wird eine vorhandene Installation verwendet oder bei Bedarf eine portable GAM-Instanz eingerichtet.
- **Datenbank:** Der Erststart-Assistent bietet eine leere Praxis oder die anonymisierte Beispieldatenbank an.
- **Piper TTS:** Unter Windows werden fehlende Standardkomponenten beim Erststart vorbereitet; Browser-TTS bleibt als Fallback erhalten.

Die lokale Java-Installation verändert keine systemweite Java-Konfiguration und benötigt keine Administratorrechte.

## Erste Schritte

1. Starten Sie `start-backend.bat` unter Windows oder `start-backend.sh` unter Linux/macOS.
2. Folgen Sie dem Erststart-Assistenten.
3. Sobald das Backend erfolgreich läuft, starten Sie `start-frontend.bat` beziehungsweise `start-frontend.sh`.

> **Hinweis:** Für notwendige Downloads während der erstmaligen Einrichtung ist Internetzugriff erforderlich. Der anschließende Normalbetrieb von GAM 2.0 ist offline möglich.
