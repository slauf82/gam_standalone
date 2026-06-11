# GAM Standalone

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

## Schritt 31t – stabile Fixes auf Basis 31o

31o als stabile Grundlage plus sauberer Backend-LBD-Fix und vorsichtiger Vorlesen/Stoppen-Toggle.

## Schritt 31u – Auth-Fallback-Fix

401/403-Fehler einzelner Modul-Endpunkte lösen nicht mehr automatisch einen kompletten Logout aus. Nur `/auth/me` beendet die Sitzung hart.

## Schritt 31v – LBD und i18n-Platzhalter-Fix

Fehlende i18n-Keys für Rechnungsvorschau/LBD wurden ergänzt und die LBD-Suche für `beispiel.lbd` robuster gemacht.

## Schritt 31w – Favicon & Visual Consistency

Originales GAM-Favicon und alte Aktionsicons wurden wieder eingebunden. Die Modulnavigation nutzt nun dieselbe Bildsprache wie der Login.

## Schritt 31x – LBD Compile-Fix

`findFirstLbdFile()` wurde in `LbdService` wieder ergänzt, damit Startup- und Systemstatus-Controller kompilieren.

## Schritt 32 – stabile UI-/Favicon-Fixes

Auf Basis von 31x wurden die kleinen UI-Übersetzungsreste und das Favicon korrigiert, ohne die funktionierende Navigationslogik umzubauen.

## Schritt 32b – Safe i18n Recovery

Recovery auf Basis von 32. 32a wird verworfen; nur sichere Minimalfixes für i18n-Reststellen werden übernommen.

## Schritt 32c – Text-only i18n Fix

Korrigiert nur Textübersetzungen für Prüfungen, Reports, Benutzer/Rechte und Stornorechnung, ohne Navigation oder Funktionalität umzubauen.

## Schritt 33 – sprachabhängige Vorlesestimmen

Die Vorlesefunktion wählt nun automatisch eine passende Browser-/Systemstimme zur gewählten Sprache aus.

## Schritt 33a – TTS-Fallback-Kette

Die Vorlesefunktion nutzt nun pro Sprache eine Fallback-Kette der aktuell verfügbaren Browser-/Systemstimmen. Ukrainisch fällt bei fehlender Stimme auf Russisch, danach Englisch und Deutsch zurück.

## Schritt 33b – MaryTTS-Konfiguration als Standard

MaryTTS ist als Standard-TTS-Backend konfiguriert; Browser-TTS und Fallback-Kette bleiben erhalten.

## Schritt 33c – MaryTTS Autostart / No-Config-Modus

GAM sucht beim Start automatisch nach einem lokalen MaryTTS-Bundle unter `tts/marytts` und startet es, falls vorhanden. Keine Benutzerkonfiguration erforderlich.

## Schritt 33d – MaryTTS Embedded

MaryTTS wird nun direkt im Java-Backend über Maven-Artefakte eingebunden. Deutsch, Englisch und Französisch sind als eingebettete MaryTTS-Sprachen/Stimmen vorbereitet.
