# GAM Standalone

Ab Version 1.2.0 liegt eine anonymisierte Testdatenbank dem Release bei !

Modernisierung des alten JSF-/GlassFish-GAM-Projekts zu Spring Boot + React/TypeScript + MariaDB.

## Aktueller Stand

Dieses Paket enthält Phase 1A + Phase 1B/1C sowie Schritt 1–4:

- kompatibler Login über bestehende `accounts`-Tabelle
- vorbereitete Passwort-/2FA-Kompatibilität
- JWT-Session-Grundlage
- `.lbd`-Empfängerdatei-Suche und Vorschau
- Rechnungsübersicht aus Bestandsdaten
- Produktliste aus `rechnungsdaten`
- neue Rechnung anlegen
- Speichern in `rechnungsdetails` und `rechnung`
- ZUGFeRD/Factur-X-E-Rechnungs-Export als primärer PDF-Export
- Benutzer-/Rechteverwaltung mit Rollenmenü
- Inventar-/Gerätemodul aus `geräte` und `geräte_neu`
- Lager-/Materialmodul aus `lager` und `verbrauchsmaterial`

Details stehen in:

- `PHASE_1A_STATUS.md`
- `PHASE_1B_1C_STATUS.md`
- `PHASE_1C_ZUGFERD_STATUS.md`
- `SCHRITT_1_RECHNUNGSMODUL_STATUS.md`
- `SCHRITT_2_BENUTZER_RECHTE_STATUS.md`
- `SCHRITT_3_INVENTAR_GERAETE_STATUS.md`
- `SCHRITT_4_LAGER_MATERIAL_STATUS.md`

## Backend starten

```bash
cd backend
mvn spring-boot:run
```

Konfiguration über Umgebungsvariablen oder `backend/src/main/resources/application.yml`:

```bash
GAM_DB_URL=jdbc:mariadb://localhost:3306/kopfzentruminventardb
GAM_DB_USER=root
GAM_DB_PASSWORD=passwort
GAM_JWT_SECRET=bitte-langes-secret-setzen
GAM_LBD_FILE=./config/meine-datei.lbd
```

## Frontend starten

```bash
cd frontend
npm install
npm run dev
```

## .lbd-Dateien

Gesucht wird standardmäßig in:

```text
./config
./daten/rechnung
.
```

Der Pfad kann über `GAM_LBD_FILE` überschrieben werden.

## Wichtiger Hinweis

Das ist noch nicht das vollständige GAM 2.0. Es ist inzwischen ein breiter Modulrahmen mit Rechnungen, Benutzer/Rechte, Inventar/Geräte und Lager/Material. Das finale Rechnungsformular, Adresslogik, Freigaben, Layouts und Speziallogik werden schrittweise ergänzt. Ab Phase 1C ist `/api/invoices/{number}/pdf` bereits als ZUGFeRD/Factur-X-Pflichtexport vorgesehen; `/pdf-debug` bleibt als Fallback erhalten.


## Stand Schritt 5

GAM 2.0 besitzt jetzt die Gesamtstruktur mit Rechnungen, Benutzer/Rechte, Inventar, Lager sowie Rahmenmodulen fuer Aufgaben, Freigaben, Personal, Kassenbuch, Pruefungen und Reports.

Die neu hinzugefuegten Rahmenmodule sind bewusst zuerst read-only, damit bestehende Fachlogik nicht versehentlich veraendert wird.

## Schritt 6 – CRUD/Workflows

Diese Version erweitert die GAM-2.0-Gesamtstruktur um erste echte Schreibfunktionen:

- Aufgaben anlegen/bearbeiten/löschen
- Freigaben anlegen/bearbeiten/löschen
- Geräte bearbeiten bzw. neue Geräte anlegen
- Lagerbestände setzen oder per Bewegung ändern

Details siehe `SCHRITT_6_CRUD_WORKFLOWS_STATUS.md`.

## Schritt 7 – Startfähigkeit

Diese Version ergänzt Startskripte, lokales Profil, Healthchecks und eine klare Testanleitung.
Der Einstieg ist jetzt `START-HIER.md`.

Wichtige technische Prüfpunkte:

```text
http://localhost:8080/actuator/health
http://localhost:8080/api/system/startup-check
http://localhost:8080/api/system/status
```

Details siehe `SCHRITT_7_STARTFAEHIGE_ARBEITSVERSION_STATUS.md`.

---

## Schritt 11: erster technischer Gesamttest

Für den ersten systematischen Test bitte zuerst `START-HIER.md`, danach `TESTPLAN_GAM_2_0.md` lesen.

Schnellprüfung nach Backend-Start:

```powershell
scripts\smoke-test.ps1 -BaseUrl http://localhost:8080
```

oder:

```bash
./scripts/smoke-test.sh http://localhost:8080
```

Ohne Token sind 401/403 bei geschützten Endpunkten normal. Wichtig sind zunächst:

- `/actuator/health`
- `/api/system/status`
- `/api/system/startup-check`

Diese sollten ohne schwerwiegenden Fehler antworten.

---

## Schritt 12: erster echter Build-Test

Für den lokalen Test bitte zuerst `START-HIER-SCHRITT12.md` lesen.

Kurzfassung Windows:

```bat
scripts\build-backend.bat
scripts\run-backend-local.bat
```

Kurzfassung Linux/macOS:

```bash
./scripts/build-backend.sh
./scripts/run-backend-local.sh
```
