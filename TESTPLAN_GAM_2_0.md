# GAM 2.0 – Testplan für den ersten technischen Gesamttest

## 0. Voraussetzungen

- Java 21 installiert
- Maven installiert oder über IDE/Maven Wrapper verfügbar
- Node.js/npm installiert
- MariaDB/MySQL läuft
- `kopfzentruminventardb.sql` wurde in eine Testdatenbank importiert
- `.lbd`-Datei liegt in einem der unterstützten Orte:
  - `config/*.lbd`
  - `daten/rechnung/*.lbd`
  - Projektwurzel
  - oder per `GAM_LBD_FILE`

## 1. Konfiguration prüfen

Backend:

```bash
cd backend
cp .env.example .env
```

Wichtige Werte:

```text
GAM_DB_URL=jdbc:mariadb://localhost:3306/kopfzentruminventardb
GAM_DB_USER=...
GAM_DB_PASSWORD=...
GAM_JWT_SECRET=...
GAM_LBD_FILE=../config/deine-datei.lbd
```

## 2. Backend bauen

```bash
cd backend
mvn clean package
```

Wenn hier Fehler auftreten, zuerst dokumentieren:

- fehlende Dependency
- falsche Java-Version
- Import-/Kompilierfehler
- Datenbankabhängigkeit beim Start

## 3. Backend starten

```bash
mvn spring-boot:run
```

Danach prüfen:

```text
http://localhost:8080/actuator/health
http://localhost:8080/api/system/status
http://localhost:8080/api/system/startup-check
```

## 4. Login testen

Mit Passwort oder 2FA/TOTP:

```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "username": "DEIN_USER",
  "password": "DEIN_PASSWORT",
  "totp": "123456"
}
```

Erwartung:

- Token wird geliefert
- Rolle wird erkannt
- 2FA-Status wird korrekt erkannt

## 5. Geschützte APIs testen

Mit Token:

```text
Authorization: Bearer <TOKEN>
```

Prüfen:

- `/api/security/status`
- `/api/modules`
- `/api/invoices`
- `/api/inventory/devices`
- `/api/warehouse/items`
- `/api/workflow/tasks`

## 6. Rechnungsmodul testen

Ziel:

- Rechnungsliste lädt
- Nummernkreis-Vorschau liefert Wert
- `.lbd`-Vorschau lädt Empfänger
- Produkt-/Positionsdaten werden geladen
- ZUGFeRD-XML lässt sich separat abrufen
- PDF/ZUGFeRD-Endpunkt reagiert

## 7. Inventar/Lager testen

Ziel:

- Geräteübersicht lädt
- Gerätedetails laden
- Lagerübersicht lädt
- Bestand/Materialbewegung nur im Testmodus prüfen
- Verknüpfung Gerät ↔ Material prüfen

## 8. Workflowmodule testen

Ziel:

- Aufgaben laden
- Freigaben laden
- einfache CRUD-Funktionen in Testdaten prüfen

## 9. Fehler dokumentieren

Bitte pro Fehler notieren:

```text
Modul:
Aktion:
Fehlermeldung:
Backend-Log:
Screenshot:
Erwartetes Verhalten:
Tatsächliches Verhalten:
```

## 10. Abbruchkriterien

Sofort stoppen bei:

- Daten werden unerwartet gelöscht
- echte Rechnungsnummern werden verbraucht
- produktive Datenbank statt Testdatenbank verbunden
- PDF/ZUGFeRD erzeugt fehlerhafte Pflichtdaten ohne Warnung

## Ergebnis dieses Tests

Nach dem Test wissen wir, ob GAM 2.0 als Arbeitsversion technisch startfähig ist und welche konkreten Fehler für die nächste Runde behoben werden müssen.
