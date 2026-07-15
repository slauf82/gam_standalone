# Schritt 40d2 – Marketingworkflow Backend-Buildfix

## Behoben

- `MarketingCampaignRepository.java` vollständig neu formatiert.
- `MarketingWorkflowSettingsRepository.java` vollständig neu formatiert.
- Ungültige Java-Textblöcke entfernt.
- SQL-Textblöcke beginnen jetzt jeweils korrekt nach einem Zeilenumbruch.
- Beide Repository-Dateien sind wieder normal mehrzeilig aufgebaut und wartbar.
- Statische Java-Syntaxprüfung durchgeführt; es verbleiben nur erwartbare fehlende Spring-Abhängigkeiten außerhalb eines Maven-Builds, keine Parse-/Textblockfehler.

## Unverändert enthalten

- Marketingaktionen
- Filial- und Materialzuordnung
- Beep-/Scanner-Erfassung
- Statusübergänge
- Historie
- Einstellungen des Marketingworkflows
- Modulaktivierung und UI-Integration

## Test

1. Backend mit `mvn clean compile` oder `mvn spring-boot:run` starten.
2. Prüfen, dass keine Compilerfehler in den beiden Marketing-Repositories auftreten.
3. Marketingmodul öffnen.
4. Aktion anlegen, Beep buchen und Status ändern.
5. Einstellungen unter `Einstellungen → Workflows → Marketingworkflow` speichern und erneut laden.
