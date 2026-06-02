# Schritt 23 – PDF-Archiv und Hotfolder

## Ziel

Beim Erzeugen des ZUGFeRD-/Factur-X-PDFs wird zusätzlich eine PDF-Kopie in einem internen Archivordner abgelegt. Optional kann eine zweite PDF-Kopie in einen konfigurierbaren Hotfolder geschrieben werden.

## Umsetzung

### Konfiguration

In `application.yml` und `application-local.yml` ergänzt:

```yaml
app:
  invoice:
    pdf:
      archive-enabled: true
      archive-folder: ./daten/rechnung/archiv
      hotfolder-enabled: false
      hotfolder-folder: ./daten/rechnung/hotfolder
      hotfolder-filename-mode: patient-number
```

Die Werte können über Umgebungsvariablen überschrieben werden:

```text
GAM_PDF_ARCHIVE_ENABLED=true
GAM_PDF_ARCHIVE_FOLDER=./daten/rechnung/archiv
GAM_PDF_HOTFOLDER_ENABLED=false
GAM_PDF_HOTFOLDER_FOLDER=./daten/rechnung/hotfolder
GAM_PDF_HOTFOLDER_FILENAME_MODE=patient-number
```

### Archiv-PDF

Wird mit sprechendem Dateinamen gespeichert, z. B.:

```text
Rechnung_2026-00123_Kopfzentrum_Mustermann_Max_20260602_174500.pdf
```

### Hotfolder-PDF

Standardmäßig wird die Patientennummer als Dateiname verwendet:

```text
123456.pdf
```

Alternative:

```text
GAM_PDF_HOTFOLDER_FILENAME_MODE=patient-number-and-invoice
```

Dann z. B.:

```text
123456_2026-00123.pdf
```

## Test

1. Falls gewünscht Hotfolder aktivieren:

```yaml
app:
  invoice:
    pdf:
      hotfolder-enabled: true
```

2. Backend sauber neu bauen und starten:

```bat
mvnw.cmd clean package
mvnw.cmd spring-boot:run
```

3. Rechnung im Frontend öffnen und ZUGFeRD-PDF erzeugen.

4. Prüfen:

```text
./daten/rechnung/archiv
./daten/rechnung/hotfolder
```

## Zusätzlich enthaltene Fachlogik-Korrektur

Gutschrift wurde fachlich korrigiert:

```text
Storno:
- Menge negativ
- Preis positiv

Gutschrift:
- Menge positiv
- Preis negativ
```

