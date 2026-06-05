# GAM Standalone

Ab Version 1.2.0 liegt eine anonymisierte Testdatenbank dem Release bei !

GAM 2.0 ist die Modernisierung und Weiterentwicklung eines historisch gewachsenen Praxis- und Verwaltungs­systems.

Das Projekt migriert die ursprüngliche Java-/JSF-Anwendung auf eine moderne Architektur mit Spring Boot, React und MariaDB und erweitert die bestehende Funktionalität um Mehrsprachigkeit, Barrierefreiheit und digitale Patientenservices.

## Hauptfunktionen

### Rechnungswesen

* Rechnungen
* Stornorechnungen
* Gutschriften
* Proforma-Rechnungen
* Zahlungsavis
* Gesellschaftsabhängige Nummernkreise
* PDF-Erzeugung
* ZUGFeRD-Unterstützung

### Mehrsprachigkeit

* Deutsch
* Englisch
* Französisch
* Ukrainisch

Funktionen:

* Mehrsprachige Rechnungstexte
* PDF-Sprachwahl
* Translation-Cache
* Datenbankgestützte Übersetzungen
* Erweiterbare Spracharchitektur

### Rechnungsvorschau

* WYSIWYG-Rechnungsvorschau
* Vorschau von Rechnungstexten
* Vorschau von Rabatten und Gutscheinen
* Vorschau von Ratenzahlungen
* Vorschau von Zahlungsinformationen

### Barrierefreiheit

* PDF/UA-Vorbereitung
* Vorlesefunktion
* Sprachabhängige Vorlesbarkeit
* Mehrsprachige Dokumente

### Patientenportal

* QR-Code auf Rechnungen
* Digitaler Rechnungsabruf
* Mehrsprachige Darstellung
* Rechnungshistorie
* PDF-Download
* Sprachwahl für Patienten

### Sicherheit

* Benutzer- und Rollenverwaltung
* Rechtekonzept
* TOTP-Zwei-Faktor-Authentifizierung
* Passkey/WebAuthn-Unterstützung

## Architektur

Backend:

* Java
* Spring Boot
* Spring Security
* MariaDB

Frontend:

* React
* TypeScript
* Vite

Dokumente:

* PDF
* ZUGFeRD
* Mehrsprachige Rechnungen

## Projektstatus

Der Schwerpunkt liegt aktuell auf dem Rechnungswesen, der Mehrsprachigkeit, der Barrierefreiheit und dem digitalen Patientenportal.

Weitere geplante Bereiche:

* Administration
* Reports und Auswertungen
* Erweiterung der Mehrsprachigkeit auf weitere Module
* Erweiterung des Patientenportals

## Lizenz

GNU GPL v3.0


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
