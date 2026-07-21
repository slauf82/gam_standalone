# GAM 2.0 – Schritt 7: startfähige Arbeitsversion vorbereiten

Diese Version ist darauf ausgelegt, den ersten technischen Starttest vorzubereiten.
Sie ist noch kein fertiges Produktivsystem, aber die Projektstruktur ist jetzt deutlich testbarer.

## 1. Voraussetzungen

- Java 21
- MariaDB/MySQL mit importierter `kopfzentruminventardb`
- Maven, falls direkt aus dem Quellcode gestartet werden soll
- Node.js/npm für das Frontend

## 2. Konfiguration

Datei kopieren:

```text
.env.example -> .env
```

Dann in `.env` mindestens prüfen:

```text
GAM_DB_URL=jdbc:mariadb://localhost:3306/kopfzentruminventardb
GAM_DB_USER=root
GAM_DB_PASSWORD=...
GAM_JWT_SECRET=...
GAM_LBD_FILE=./config/deine-datei.lbd
```

## 3. Backend starten

Windows:

```text
start-backend.bat
```

Linux/macOS:

```bash
./start-backend.sh
```

Direkt:

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

## 4. Frontend starten

Windows:

```text
start-frontend.bat
```

Linux/macOS:

```bash
./start-frontend.sh
```

## 5. Schnelltest

Nach Backendstart im Browser öffnen:

```text
http://localhost:8080/actuator/health
http://localhost:8080/api/system/startup-check
http://localhost:8080/api/system/status
```

Oder PowerShell:

```powershell
./scripts/check-system.ps1
```

## 6. Erwartung

Der erste gute technische Zustand ist erreicht, wenn:

- Backend startet ohne Stacktrace
- `/actuator/health` liefert `UP`
- `/api/system/startup-check` zeigt `database.ok=true`
- `.lbd` wird gefunden oder sauber als fehlend gemeldet
- Frontend startet mit Vite

## 7. Wichtiger Hinweis

Die Startfähigkeit ersetzt noch nicht den fachlichen Gesamttest.
Dieser Schritt dient dazu, die Umgebung und die technische Basis prüfbar zu machen.

## Schritt 8 Hinweis

Das Rechnungsmodul hat jetzt einen ersten durchgehenden Create/Edit-Workflow. Für echte Fachtests sollte zuerst die MariaDB-Verbindung stehen und eine Kopie der Bestandsdatenbank verwendet werden. Der ZUGFeRD-PDF-Export ist der Pflichtpfad; der normale PDF-Endpunkt ist nur Debug/Fallback.
