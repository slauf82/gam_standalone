# GAM 2.0
Ab Version 1.2.0 liegt eine anonymisierte Testdatenbank dem Release bei !

<<<<<<< Updated upstream
GAM 2.0 ist die Modernisierung und Weiterentwicklung eines historisch gewachsenen Praxis- und Verwaltungs­systems.

Das Projekt migriert die ursprüngliche Java-/JSF-Anwendung auf eine moderne Architektur mit Spring Boot, React und MariaDB und erweitert die bestehende Funktionalität um Mehrsprachigkeit, Barrierefreiheit und digitale Patientenservices.
=======
Modernisierung des alten JSF-/GlassFish-GAM-Projekts zu Spring Boot + React/TypeScript + MariaDB.
>>>>>>> Stashed changes

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

<<<<<<< Updated upstream
GNU GPL v3.0
=======
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

## Demo-LBD / First-Run Experience

Ab Schritt 29e enthält das Projekt neutrale Demo-LBD-Dateien unter:

```text
config/demo-lbd/
```

Standardmäßig wird `max.mustermann.lbd` als Demo-Empfänger verwendet, wenn keine andere LBD-Datei ausgewählt wurde. Dadurch können Rechnungsvorschau, PDF-Erzeugung, ZUGFeRD-Prüfung und QR-Rechnungsportal direkt nach dem Start getestet werden.

Empfohlene lokale Konfiguration:

```yaml
app:
  invoice:
    lbd:
      file: ${GAM_LBD_FILE:max.mustermann.lbd}
      search-folders: ${GAM_LBD_SEARCH_FOLDERS:./config/demo-lbd,./config,./daten/rechnung,.}
```


## Reports und Exporte

Ab Schritt 30 enthält GAM 2.0 ein erstes Rechnungsreport-Modul.

Funktionen:

- Zeitraumfilter von/bis
- optionaler Gesellschaftsfilter
- Reportübersicht für Rechnungen, Stornos, Gutschriften, Proforma und Zahlungsavis
- Export als XLSX
- Export als XLS
- Export als CSV
- DATEV-nahe CSV-Vorbereitung als Alternative zum früheren Excel-Zwischenschritt

Die DATEV-CSV ist bewusst als Vorbereitungsformat umgesetzt und sollte vor produktiver Nutzung mit Steuerberatung/Buchhaltung abgestimmt werden.


## Schritt 30b – Reportarten nach Alt-GAM

Das Reportmodul unterstützt jetzt die aus GAM 1.0 rekonstruierten Reportarten:

- Alle Daten / Positionsdaten
- DATEV
- Debitoren
- Umsatz
- Umsatz je Arzt / Auftraggeber
- Umsatz je Filiale
- Tagesliste
- Produktranking

Alle Reportarten können über Von-/Bis-Datum, Gesellschaft und Reportart geladen und als XLSX, XLS, CSV oder DATEV-nahe CSV exportiert werden.

## Schritt 31 – Login Experience & Modulübersicht

Ab Schritt 31 werden alle historischen GAM-Anwendungen in der Oberfläche sichtbar. Vollständig migrierte Module sind nutzbar, noch offene Bereiche erscheinen als sichere Lesemodus-/Shell-Module.

Zusätzlich wurde die Loginseite optisch näher an GAM 1.0 angelehnt:

- grauer Seitenhintergrund
- hervorgehobenes weißes Loginpanel
- großes GAM-Logo
- Anwendungswahl als Button-Leiste

Damit ist die Gesamtstruktur von GAM bereits sichtbar, auch wenn einzelne Fachmodule später noch weiter ausgebaut werden.

## Schritt 31b – Login-Sprache und Modulicons

Die Loginseite unterstützt nun eine Oberflächensprache. Diese gilt für die Anwendung selbst und ist bewusst von der PDF-Sprache im Rechnungsprogramm getrennt.

Unterstützte Oberflächensprachen:

- Deutsch
- English
- Français
- Українська

Zusätzlich erhalten die Modulbuttons Icons, damit die historischen GAM-Anwendungen bereits im Login und in der Modulübersicht schneller erfassbar sind.


## Schritt 31c – Login UI Cleanup

Die Loginseite wurde nach der Einführung der Oberflächensprache bereinigt:

- nur noch ein Sprachfeld
- größere Modulicons
- Icon-über-Text-Layout für Modulbuttons
- klarere optische Trennung von Loginbereich und Hintergrund

## Schritt 31d – Login-i18n vervollständigt

Die Loginseite übersetzt nun auch die bisher noch fest deutsch dargestellten Texte:

- Login-Hinweis
- Anwendungsauswahl
- Lesemodus-Hinweis
- Benutzername / Passwort / Anmelden
- 2FA- und Passkey-Buttons

Die Oberflächensprache bleibt weiterhin unabhängig von der PDF-Sprache im Rechnungsprogramm.

## Schritt 31e – Login-i18n inklusive Modulnamen

Die Oberflächensprache im Login übersetzt nun auch die Modulnamen und die restlichen Logintexte.
Die interne Modulauswahl bleibt unverändert; übersetzt wird nur die Anzeige.

## Schritt 31h – UI-i18n Rollback + YAML-Fix

Dieser Stand stellt die funktionierende Login-/Modulübersetzung aus Schritt 31e wieder her und entfernt nur den YAML-Risikopunkt mit doppelten `app:`-Blöcken.
Der experimentelle Backend-UI-Translation-Cache aus 31f ist in diesem Stand bewusst nicht aktiv.

## Schritt 31i – Login UI i18n Bindings

Die restlichen festen Login- und Modultexte wurden an die bestehende Frontend-i18n angebunden.
Dieser Schritt verzichtet bewusst auf den experimentellen Backend-Translation-Cache und nutzt die bereits funktionierende Sprachlogik.

## Schritt 31j – Globaler UI-i18n-Fix

Die Oberflächensprache wird nun zentral über `ui(...)` und `uiModule(...)` gerendert. Der Sprachwechsel aktualisiert den React-State direkt, sodass Login und Modulnamen zuverlässig mitwechseln. Die PDF-Sprache im Rechnungsprogramm bleibt davon getrennt.

## Schritt 31k – Login-Resttexte i18n-Fix

Die noch deutsch bleibenden Login-Resttexte, Platzhalter und Authentifizierungsbuttons wurden gezielt an die vorhandene Frontend-Übersetzung angebunden.

## Schritt 31l – Login-i18n Komplettpaket

Dieser Stand enthält den gezielten `main.tsx`-Fix für die noch deutsch bleibenden Logintexte und vereinheitlicht die Verwendung der Oberflächensprache.

## Schritt 31m – UI Hardcoded Text Cleanup

Sichtbare hart codierte Texte in `frontend/src/main.tsx` wurden weitgehend auf `ui(...)` und `uiModule(...)` umgestellt. Ziel ist eine konsistente Oberflächensprache vor der nächsten fachlichen Weiterentwicklung.

## Schritt 31n – Modulzugriff / White-Screen-Fix

Modulbereiche sind nun durch eine Error-Boundary geschützt. Superadmin/Admin erhalten eine vollständige Fallback-Modulliste, sodass historische Modul-Shells sichtbar bleiben und Renderfehler nicht mehr die komplette Oberfläche weiß werden lassen.
>>>>>>> Stashed changes
