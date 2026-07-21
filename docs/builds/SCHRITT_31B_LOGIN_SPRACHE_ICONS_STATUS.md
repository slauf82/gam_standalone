# Schritt 31b – Login-Sprache & Modulicons

## Ziel

Die im Login gewählte Sprache steuert die Oberflächensprache der Anwendung.
Die PDF-Sprache im Rechnungsprogramm bleibt separat.

## Enthalten

- Sprachwahl im Login vorbereitet/aktiviert
- Oberflächensprache wird in `localStorage` unter `gam.uiLanguage` gespeichert
- Dokument-Sprache (`document.documentElement.lang`) wird gesetzt
- Modulbuttons erhalten Icons unter `frontend/public/icons`
- Modultexte sind über `frontend/src/i18n.ts` übersetzbar
- unterstützte UI-Sprachen:
  - Deutsch
  - English
  - Français
  - Українська

## Modulicons

- Rechnungsprogramm
- Geräteverzeichnis
- Lagerverwaltung
- Kassenbuch
- Aufgabenverwaltung
- Freigabemanagement
- Bestelltool
- Personaldaten
- Arbeitsplatzausstattung
- Preisliste
- Reports
- Administration

## Geänderte Dateien

- frontend/src/main.tsx

## Fachregel

- Login-Sprache = Oberflächensprache
- PDF-Sprache im Rechnungsprogramm = Dokumentsprache
- beide Sprachen bleiben unabhängig
