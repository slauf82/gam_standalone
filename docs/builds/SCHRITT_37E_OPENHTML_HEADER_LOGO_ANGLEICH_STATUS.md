# Schritt 37E – OpenHTMLtoPDF Header/Logo-Abgleich

Basis:
- Schritt 37D

Ziel:
- Button 1 bleibt unverändert: OpenPDF + ZUGFeRD
- Button 2 bleibt OpenHTMLtoPDF ohne ZUGFeRD
- Button 2 gleicht den sichtbaren Kopfbereich an Button 1 an

Umgesetzt:
- Gesellschaftslogo links oben im OpenHTMLtoPDF-PDF ergänzt
- Logo-Dateien wie im OpenPDF-Pfad verwendet:
  - Gesellschaft 1: logo_AMAE_blau.png
  - Gesellschaft 2/3: logo_ACQUA_blau.png
  - Gesellschaft 6: Healthcode_logo_blau.png
  - Fallback: KOPFZENTRUM_LOGO.png
- Headerdaten angeglichen:
  - Firmenname
  - Straße
  - PLZ/Ort
  - E-Mail
- Ansprechpartner/Kontaktperson wird im linken Kopfbereich nicht mehr statt E-Mail ausgegeben
- QR-Code und Portal-Link aus 37D bleiben erhalten

Nicht geändert:
- keine PDF/A-3-Umstellung
- keine ZUGFeRD-Einbettung bei Button 2
- keine Security-Änderung
- keine LBD-Änderung
- kein produktiver PDF-Pfad geändert

Testziel:
- Button 1 öffnet weiter die produktive ZUGFeRD-PDF
- Button 2 öffnet die OpenHTMLtoPDF-Test-PDF
- Kopfbereich enthält Logo und E-Mail wie Button 1
