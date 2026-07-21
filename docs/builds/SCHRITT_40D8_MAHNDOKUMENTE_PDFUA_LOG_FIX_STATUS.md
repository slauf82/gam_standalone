# Schritt 40d8 – Mahndokumente PDF/UA- und Log-Fix

## Behoben

- Die nicht vorhandene Tabelle `invoice_company_logo` wird nicht mehr abgefragt.
- Zahlungserinnerungen, Mahnungen und Inkassohinweise verwenden dieselbe Gesellschaftslogo-Auflösung wie Rechnungen.
- Windows-Schriften (Arial/Segoe UI) werden vor der PDF-Erzeugung registriert.
- Die wiederholte OpenHTMLtoPDF-Warnung `Font list is empty` entfällt bei vorhandenen Windows-Schriften.
- Rechnung und Zahlungsdokumente enthalten eine Dokumentbeschreibung in den HTML-Metadaten.
- Die Warnung `No document description provided. Document will not be PDF/UA compliant.` wird damit behoben.
- PDF/UA-Tagging und PDF/A-3-U bleiben aktiviert.
- Die in 40d7 ergänzte authentifizierte Dokumentausgabe und die Modulgruppierung nach dem Login bleiben erhalten.

## Test

1. Backend neu starten.
2. Zahlungserinnerung, Mahnung 1–3 und Inkassohinweis jeweils im Zahlungsworkflow öffnen.
3. Dieselben Dokumente im Patientenportal öffnen.
4. Prüfen, dass Text und Logo sichtbar sind.
5. Backend-Protokoll auf folgende Meldungen prüfen:
   - keine fehlende Tabelle `invoice_company_logo`
   - keine Serie von `Font list is empty`
   - keine fehlende Dokumentbeschreibung
6. Neu erzeugte PDFs erneut mit PAC prüfen.
