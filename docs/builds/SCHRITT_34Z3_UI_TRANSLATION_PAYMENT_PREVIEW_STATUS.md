# Schritt 34z3 – UI Translation Completion, Zahlungsart und Vorschau-Kontrast

Basis: Schritt 34z2.

## Ziel

34z3 ergänzt die letzten sichtbaren UI-Übersetzungslücken, ohne die GAM-1.0-Logik wieder zu verlassen:

- DB zuerst
- nur aktuelle Oberflächensprache wird gepflegt
- keine Mehrsprachen-Massenpflege
- Login bleibt schnell

## Enthaltene Änderungen

### LoginDialog

Die folgenden Texte bleiben im zentralen UI-Keykatalog und werden bei der aktiven Sprache über DB/LibreTranslate gepflegt:

- Kompatibler Login über bestehende accounts-Tabelle.
- Alle historischen GAM-Anwendungen sind sichtbar; noch nicht vollständig migrierte Module starten im Lesemodus.
- Passwort
- 2FA registrieren
- 2FA-Login
- Passkey registrieren
- Passkey-Login
- Anmelden

### Rechnungsprogramm

Ergänzt bzw. vereinheitlicht:

- Navigationsbuttons: Prüfungen, Reports, Benutzer/Rechte
- Neue Rechnung
- Rechnung suchen
- Rechnung bearbeiten
- Rechnung suchen / Suchen in der Suche
- Rechnung
- Zahlungsavis
- Gutschrift
- Gutschrift erstellen
- Technisch löschen

### Zahlungsart

Zahlungsart wird wieder explizit abgefragt und definiert:

- unbekannt
- Barzahlung
- Kartenzahlung
- Überweisung

Die Zahlungsart-Texte sind UI-Keys und werden bei neuen Sprachen ebenfalls über DB/LibreTranslate gepflegt.

### Rechnungsvorschau

Die UI-Texte der Rechnungsvorschau orientieren sich wieder an der Oberflächensprache, nicht an der PDF-Sprache.

Wichtig:

- UI-Beschriftungen: Oberflächensprache
- Rechnungsinhalt/PDF-Texte: PDF-Sprache

### Lesbarkeit

Der graue Text in der Rechnungsvorschau wurde kontrastreicher gemacht:

- Vorschau-Hintergrund hellgrau
- Vorschau-Text schwarz
- `.muted` innerhalb der Vorschau nicht mehr grau auf grau

## Prüfung

`npx vite build` wurde erfolgreich ausgeführt.

`npm run build` wurde nicht als abschließender Maßstab genutzt, weil das Projekt aktuell mit TypeScript latest und fehlenden React-Typdefinitionen Typprüfungswarnungen ausgibt. Der Vite-Transform selbst ist erfolgreich.
