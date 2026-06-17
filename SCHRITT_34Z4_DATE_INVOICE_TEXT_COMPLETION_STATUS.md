# Schritt 34z4 – Date Localization & Invoice Text Completion

Basis: Schritt 34z3 / 34z2

## Ziel

Letzter Mehrsprachigkeits- und Rechnungsvorschau-Patch vor v1.6.1.

## Enthalten

- Behandlungsdatum wieder als eigenes Feld ergänzt
- Rechnungsdatum und Behandlungsdatum stehen standardmäßig auf heutigem Datum
- Datumswerte bleiben intern ISO-kompatibel und werden nur für Anzeige/PDF lokalisiert
- Datumsanzeige im Formular folgt der Oberflächensprache
- Datumsanzeige in Vorschau/PDF folgt der PDF-/Rechnungssprache
- Rechnungstexte für Italienisch, Schwedisch, Türkisch und Russisch werden nicht mehr auf Deutsch normalisiert
- Invoice-TranslationService kennt jetzt it/sv/tr/ru mit korrekten Tabellen und Präfixen
- Produktbeschreibungen werden für Vorschau/PDF-Sprache mit stabilen productDescription.* Keys übersetzbar gemacht
- PDF nutzt übersetzte Produktbeschreibung, falls vorhanden
- Vorschau nutzt übersetzte Produktbeschreibung, falls vorhanden
- Zahlungsart wird in der Vorschau UI-sprachlich angezeigt
- Vorschautexte bleiben schwarz/lesbar auf hellem Vorschauhintergrund
- Login-/UI-Translation-Logik bleibt DB-first und nur aktuelle UI-Sprache
- Login-Performance aus 34x/34z2 bleibt unangetastet

## Technische Hinweise

- Frontend-Build wurde mit `npm run build` geprüft und war erfolgreich.
- Backend-Maven-Build konnte in dieser Umgebung nicht ausgeführt werden, weil `mvn` nicht verfügbar ist.
- `frontend/package.json` enthält nun die React-/Node-Typepakete als DevDependencies, damit der TypeScript-Build nach `npm install` sauber läuft.

## Testempfehlung

1. Backend starten
2. Frontend starten
3. Sprache Italienisch/Schwedisch/Türkisch/Russisch wählen
4. Neue Rechnung öffnen
5. Rechnungsdatum und Behandlungsdatum prüfen/ändern
6. PDF-Sprache wechseln
7. Vorschau prüfen:
   - Rechnungstext
   - Datumsformat
   - Zahlungsart
   - Produktbeschreibung
8. PDF erzeugen und dieselben Punkte prüfen
