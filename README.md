# GAM 2.0 Standalone v1.0.0

## Überblick

GAM 2.0 Standalone ist die modernisierte Nachfolgeversion des ursprünglichen GAM (Geräte-, Aufgaben-, Material- und Rechnungsmanagement). Ziel ist die Übernahme der bewährten Fachlogik des Originalsystems auf eine moderne technische Basis mit Spring Boot, React, TypeScript und MariaDB.

Der Fokus liegt auf langfristiger Wartbarkeit, einfacher Erweiterbarkeit und einer modernen Benutzeroberfläche, ohne die bewährten Arbeitsabläufe des ursprünglichen Systems zu verändern.

---

## Aktueller Funktionsumfang

### Benutzerverwaltung

* Login mit Benutzername und Passwort
* JWT-basierte Authentifizierung
* Rollen- und Rechteverwaltung
* Kompatibilität zu bestehenden Benutzerdaten
* Vorbereitung für 2-Faktor-Authentifizierung (TOTP)

### Rechnungsmodul

* Rechnungserstellung
* Rechnungsbearbeitung
* PDF-Erzeugung
* ZUGFeRD / Factur-X Export
* Automatische Rechnungsnummernvergabe
* Unterstützung von LBD-Empfängerdaten
* Vorschau von LBD-Dateien
* Gesellschaftsverwaltung
* Mehrere Rechnungspositionen
* Automatische Summenberechnung

### LBD-Unterstützung

* Automatische Suche nach LBD-Dateien
* UTF-8 und Windows-1252 Unterstützung
* Übernahme von:

  * Anrede
  * Titel
  * Vorname
  * Nachname
  * Adresse
  * Versicherungsdaten
  * Patientennummer

### System

* Spring Boot Backend
* React + TypeScript Frontend
* MariaDB Datenbank
* Maven Build System
* REST API
* JWT Security
* Rollenbasierte Zugriffskontrolle

---

## Fachliche Regeln

### Rechnungsarten

Normale Rechnung:

12345

Stornorechnung:

12345S

Gutschrift:

12345G

Proforma-Rechnung:

12346P

### Stornorechnung

* basiert auf der Originalrechnung
* übernimmt alle Positionen
* dient der vollständigen Stornierung der Ausgangsrechnung

### Gutschrift

* basiert auf der Originalrechnung
* übernimmt alle Positionen
* dient der Korrektur oder Rückerstattung

### Proforma-Rechnung

* besitzt eine eigene Rechnungsnummer
* endet mit dem Suffix "P"
* kann mehrere Positionen enthalten
* Positionen können später auf mehrere echte Rechnungen verteilt werden

---

## Geplante Erweiterungen

### Inventar

* Geräteverwaltung
* Seriennummern
* Standorte
* Prüfungen
* Wartungen

### Lager

* Lagerbestände
* Wareneingang
* Warenausgang
* Mindestbestände
* QR-Code-Unterstützung
* Barcode-Unterstützung

### Personal

* Mitarbeiterverwaltung
* Qualifikationen
* Schulungen
* Einweisungen

### Aufgaben und Freigaben

* Aufgabenmanagement
* Freigabeprozesse
* Dashboard

### Rechnungen

* Mahnwesen
* Textbausteine
* QR-Code auf Rechnungen
* Patientenportal für digitale Rechnungsbereitstellung

---

## Technischer Stack

Backend

* Java 21+
* Spring Boot
* Spring Security
* Maven
* MariaDB

Frontend

* React
* TypeScript
* Vite

Dokumente

* PDF
* ZUGFeRD / Factur-X
* XML

---

## Projektstatus

Version 1.0.0 stellt die erste vollständig lauffähige Arbeitsversion von GAM 2.0 dar.

Bereits erfolgreich getestet:

* Backend-Start
* Datenbankverbindung
* Benutzeranmeldung
* Rollenmodell
* LBD-Verarbeitung
* Rechnungserstellung
* PDF-Erzeugung
* ZUGFeRD-Export
* React-Frontend

GAM 2.0 befindet sich aktuell in aktiver Weiterentwicklung mit Schwerpunkt auf Rechnungswesen, Inventar, Lagerverwaltung und Benutzerfreundlichkeit.
