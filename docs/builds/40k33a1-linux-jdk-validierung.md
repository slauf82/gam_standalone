# Schritt 40k33a1 – Linux-JDK-Validierung

## Fehlerbild

Auf einem frisch eingerichteten Ubuntu-System wurde Eclipse Temurin 21 erfolgreich nach
`runtime/java` heruntergeladen und entpackt. `runtime/java/bin/java -version` funktionierte,
der Assistent meldete dennoch:

`[FEHLER] JDK konnte nicht validiert werden.`

## Ursache

Die allgemeine Versionsauswertung in `scripts/ensure-java.sh` erkannte die Ausgabe
`javac 21.0.11` wegen eines zu gierigen regulaeren Ausdrucks faelschlich als Hauptversion `0`.
Dadurch scheiterte die gemeinsame Pruefung von `java` und `javac`, obwohl beide korrekt
installiert waren.

## Korrektur

- getrennt robuste Erkennung der ersten Versionszahl aus `java -version` und `javac -version`
- Unterstuetzung aktueller Temurin-Ausgaben wie `21.0.11`
- konkrete Diagnoseausgaben fuer erkannte Java- und Javac-Hauptversionen im Fehlerfall
- bereits vorhandenes lokales JDK wird beim naechsten Start ohne erneuten Download erkannt
