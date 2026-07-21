# GAM 2.0 – erster echter Build-Test

## 1. Voraussetzungen

- Java 21 installiert
- MariaDB/MySQL läuft
- Datenbank `kopfzentruminventardb` importiert
- Optional: Node.js 20+ für das Frontend

Maven muss nicht zwingend systemweit installiert sein. `mvnw.cmd`/`mvnw` versucht Maven 3.9.11 lokal unter `.mvn/` zu verwenden oder herunterzuladen.

## 2. Datenbank konfigurieren

Lege Umgebungsvariablen an oder ändere `backend/src/main/resources/application-local.yml`:

```text
GAM_DB_URL=jdbc:mariadb://localhost:3306/kopfzentruminventardb
GAM_DB_USER=root
GAM_DB_PASSWORD=deinpasswort
GAM_JWT_SECRET=bitte-ein-langes-geheimes-token-mit-32-zeichen
GAM_LBD_FILE=./config/beispiel.lbd.example
```

## 3. Backend bauen

Windows:

```bat
scripts\build-backend.bat
```

Linux/macOS:

```bash
./scripts/build-backend.sh
```

## 4. Backend starten

Windows:

```bat
scripts\run-backend-local.bat
```

Linux/macOS:

```bash
./scripts/run-backend-local.sh
```

## 5. Prüfen

Im Browser öffnen:

```text
http://localhost:8080/actuator/health
http://localhost:8080/api/system/startup-check
```

## 6. Frontend starten

```bash
cd frontend
npm install
npm run dev
```

Dann im Browser:

```text
http://localhost:5173
```

## 7. Wichtig für den ersten Fehlerbericht

Falls der Build fehlschlägt, bitte die erste Fehlermeldung ab `BUILD FAILURE` oder den ersten `Compilation failure`-Block kopieren. Damit kann ich gezielt den nächsten Fix bauen.
