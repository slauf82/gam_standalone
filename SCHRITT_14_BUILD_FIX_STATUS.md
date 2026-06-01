# Schritt 14 – Build-/Konfigurationsfix nach erstem Echttest

Diese Version bündelt die beim ersten lokalen GAM-2.0-Test gefundenen Korrekturen.

## Enthaltene Pflichtfixes

- `JwtService.java` verwendet kein `String.padEnd(...)` mehr.
- JWT-Key-Padding ist jetzt Java-kompatibel umgesetzt.
- `application.yml` enthält die benötigten `app.security.*`- und `app.legacy-login.*`-Werte.
- `.env.example` und `backend/.env.example` verwenden nun `GAM_LBD_SEARCH_FOLDERS` passend zur Anwendungskonfiguration.
- Standard-Zeichensatz für die Beispiel-LBD ist `UTF-8`.
- `SecurityConfig.java` gibt lokale Testendpunkte frei:
  - `/api/system/startup-check`
  - `/api/invoices/lbd/preview`
  - PDF-/XML-Testexporte
- Startskripte setzen `SPRING_PROFILES_ACTIVE=local` robuster.
- Rechnungs-UI-Fixes aus Schritt 13 bleiben enthalten.

## Lokal erfolgreich nachgewiesen durch den Anwender im vorherigen Stand

- Backend startet
- MariaDB ist erreichbar
- Login funktioniert
- LBD-Datei wird gefunden und geparst
- PDF-Export funktioniert
- ZUGFeRD/XML-Export funktioniert

## Hinweis

In dieser Umgebung konnte Maven nicht ausgeführt werden, weil Maven/Abhängigkeiten nicht lokal verfügbar sind und kein Internetzugriff auf `repo.maven.apache.org` besteht. Der bekannte Compilerfehler `material.padEnd(...)` wurde aber gezielt entfernt und per Quelltextsuche verifiziert.
