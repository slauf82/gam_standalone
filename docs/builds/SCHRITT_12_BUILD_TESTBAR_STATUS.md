# Schritt 12 – Build wirklich testbar machen

Stand: Build-/Startpaket für den ersten echten lokalen Test.

## Neu in diesem Paket

- Maven-Start über `mvnw` / `mvnw.cmd` aus dem Projekt-Root.
- Backend-POM auf reproduzierbarere Versionen und Java 21 ausgerichtet.
- `application.yml` und `application-local.yml` ergänzt.
- Windows-/Linux-Skripte für Backend-Build und lokalen Start ergänzt.
- Testanleitung für den ersten echten Build ergänzt.
- Healthcheck und Startup-Check bleiben aus Schritt 7/11 erhalten.

## Wichtiger Hinweis

In dieser Umgebung ist kein Maven installiert. Ich konnte deshalb weiterhin keinen echten Maven-Build ausführen.
Das Paket ist aber jetzt so vorbereitet, dass du lokal mit Java 21 und Internetzugang bzw. lokalem Maven testen kannst.

## Erste Befehle

Windows:

```bat
scripts\build-backend.bat
scripts\run-backend-local.bat
```

Linux/macOS:

```bash
./scripts/build-backend.sh
./scripts/run-backend-local.sh
```

Alternativ direkt:

```bash
./mvnw clean package -DskipTests
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

## Erwartete nächste Arbeit

Beim ersten echten Build können noch Compile-Fehler durch API-Imports, Mustangproject-Versionen oder Spaltennamen auftreten. Diese Fehler sind jetzt der nächste sinnvolle Fixpunkt.
