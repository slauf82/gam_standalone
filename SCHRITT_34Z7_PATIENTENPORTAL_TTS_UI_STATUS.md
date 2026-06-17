# Schritt 34z7 – Patientenportal / Vorlesen / UI-Restübersetzungen

Basis: Schritt 34z6.

## Enthalten

- Patientenportal erweitert um getrennte Portal-/Oberflächensprache und Rechnungs-/PDF-Sprache.
- PDF-Links im Patientenportal verwenden die gewählte PDF-Sprache, nicht zwingend die Portalsprache.
- Patientenportal unterstützt jetzt alle 8 Sprachen: de, en, fr, uk, it, sv, tr, ru.
- Vorlesen/Stop im Patientenportal als kombinierter Umschaltbutton wie in der Rechnungsvorschau.
- Jede einzelne Rechnung im Patientenportal hat zusätzlich einen eigenen Vorlesen-Button.
- Sprach-Tags für Browser-TTS erweitert: it-IT, sv-SE, tr-TR, ru-RU.
- Navigationsmapping für Prüfungen, Reports und Benutzer/Rechte stabilisiert.
- Kurzzeile unter GAM 2.0 für IT/SV/TR/RU ergänzt.
- TypeScript 6 Build-Kompatibilität: ignoreDeprecations auf 6.0 gesetzt.

## Nicht geändert

- Produktbeschreibungen werden weiterhin nur live übersetzt und nicht in die DB geschrieben.
- Login bleibt DB-first/current-language-only.
- Keine Mehrsprachen-Massenpflege.

## Test

Frontend-Build erfolgreich mit `npm run build`.
Backend-Maven konnte in dieser Umgebung nicht geprüft werden, da `mvn` nicht verfügbar ist.
