# Schritt 13 – Rechnungs-UI und Ersttest-Fixes

Diese Version fasst die beim ersten echten GAM-2.0-Test gefundenen Korrekturen zusammen.

## Enthalten

- `SecurityConfig` erweitert:
  - `/api/system/startup-check` lokal abrufbar
  - `/api/invoices/lbd/preview` lokal abrufbar
  - PDF-/XML-Testexport-Endpunkte lokal abrufbar
- `StartupCheckController` zeigt aktive Spring-Profile nun über Spring `Environment` statt über `System.getProperty`.
- `application.yml` und `application-local.yml` enthalten jetzt vollständige `app.*`-Konfiguration:
  - JWT Secret
  - JWT Laufzeit
  - Legacy-Login/SHA256
  - TOTP-Kompatibilität
  - LBD-Datei/Suchpfade/Charset
  - ZUGFeRD aktiviert
- Rechnungsoberfläche aufgeräumt:
  - Modus `Neue Rechnung`
  - Modus `Rechnung suchen`
  - Modus `Rechnung bearbeiten`
  - Suche wird nur noch im Suchmodus angezeigt
- Rechnungseditor verbessert:
  - kompakteres Produktfeld
  - Menge und Positionsbutton besser sichtbar
  - Buttontext: `+ Position übernehmen`
  - beim Speichern wird die aktuell ausgewählte Position automatisch übernommen, wenn noch keine Position hinzugefügt wurde
  - kompakte `.lbd`-Empfängeranzeige direkt im Editor
- CSS-Feinschliff für Produktspalte, Positionstabelle und Rechnungsmenü.

## Hinweise

- Das Frontend wurde mit `vite build` syntaktisch geprüft.
- Backend-Kompilierung konnte in dieser Umgebung nicht erneut durchgeführt werden, weil Maven-Abhängigkeiten hier nicht online nachgeladen werden konnten. Auf deinem System war der Build bereits erfolgreich.
- Für echte Produktion sollten PDF/XML-Endpunkte später wieder JWT-geschützt und über `fetch` mit Token geöffnet werden.

