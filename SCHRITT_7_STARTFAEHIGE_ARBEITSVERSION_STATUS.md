# Schritt 7 – Startfähige GAM-2.0-Arbeitsversion

## Ziel

Aus der bisherigen Alpha-Gesamtstruktur wird eine Version, die lokal kontrolliert gestartet und technisch geprüft werden kann.

## Neu in dieser Version

- Startskripte für Backend und Frontend:
  - `start-backend.bat`
  - `start-backend.sh`
  - `start-frontend.bat`
  - `start-frontend.sh`
- `.env.example` auf Projektebene
- lokales Spring-Profil `application-local.yml`
- Logdatei unter `./logs/gam-backend.log`
- Spring Boot Actuator Healthcheck
- neuer Endpunkt:
  - `GET /api/system/startup-check`
- Prüfskripte:
  - `scripts/check-system.ps1`
  - `scripts/check-system.sh`
- neue Einstiegshilfe:
  - `START-HIER.md`

## Technische Prüfpunkte

Nach dem Start sollten diese Endpunkte erreichbar sein:

```text
GET /actuator/health
GET /api/system/startup-check
GET /api/system/status
```

## Was Schritt 7 noch nicht löst

- noch keine finale produktive Paketierung als EXE
- noch keine automatische DB-Migration
- noch keine vollständige Testabdeckung
- noch kein finaler ZUGFeRD-Validatorlauf
- noch kein fachlicher End-to-End-Test

## Nächster sinnvoller Schritt

Schritt 8 sollte das Rechnungsmodul fachlich produktiver machen:

- echte Pflichtfeldprüfung
- Rechnung endgültig speichern
- Positionen sauber bearbeiten
- ZUGFeRD-Validierungsstatus sichtbar machen
- PDF-Layout an altes GAM annähern
