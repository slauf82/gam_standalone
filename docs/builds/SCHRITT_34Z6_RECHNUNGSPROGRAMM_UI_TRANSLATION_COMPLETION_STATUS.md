# Schritt 34z6 – Rechnungsprogramm UI Translation Completion

Ziel: Rechnungsprogramm-Oberfläche in allen 8 UI-Sprachen deutlich vollständiger übersetzen.

Umgesetzt:
- sichtbare Rechnungsmodul-Resttexte als stabile UI-Keys/Fallbacks ergänzt
- Navigation: Prüfungen, Reports, Benutzer/Rechte über Modul-/UI-Keys abgesichert
- Rechnungssuche: Neue Rechnung, Rechnung suchen, Rechnung bearbeiten, Gesellschaft, Suche, Platzhalter abgesichert
- Beleglisten: Rechnung, Stornorechnung, Gutschrift, Zahlungsavis über übersetzte Belegart-Labels abgesichert
- Aktionen: Gutschrift erstellen, Technisch löschen, Stornorechnung erzeugen abgesichert
- Vorschau: Einstellungen wieder oberhalb der Rechnungsvorschau angeordnet
- Produktbeschreibungen: Live-Übersetzung nach PDF-Sprache ergänzt, ohne Persistenz in translation_* Tabellen
- PDF-Erzeugung: Produktbeschreibung wird nicht mehr automatisch in die DB geschrieben
- DB-first/current-language-only Logik bleibt erhalten
- LoginDialog aus 34z5 bleibt unverändert

Hinweis:
- Frontend TypeScript-Syntax mit `npx tsc --noEmit --ignoreDeprecations 6.0` geprüft.
- Vite-Build direkt mit `npx vite build` erfolgreich geprüft.
- Maven-Backend-Build konnte in dieser Umgebung nicht ausgeführt werden, weil Maven nachgeladen werden müsste und repo.maven.apache.org nicht erreichbar war.
