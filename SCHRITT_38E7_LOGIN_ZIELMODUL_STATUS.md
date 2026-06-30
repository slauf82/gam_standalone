# Schritt 38e7 – Login-Zielmodul respektieren

## Ziel

Wenn im Login-Dialog ein Zielmodul ausgewählt wird, soll GAM 2.0 nach erfolgreicher Anmeldung direkt dieses Modul öffnen.

Beispiel:

- Auswahl: **Lagerverwaltung**
- Login erfolgreich
- Start direkt in der **Lagerverwaltung** statt im Rechnungsprogramm

## Umsetzung

- Die im Login gewählte Anwendung wird gespeichert.
- Die Auswahl wird auf die interne `Page`-Route abgebildet.
- Nach Password-, 2FA- und Passkey-Login wird das Zielmodul an die Shell übergeben.
- Die Shell öffnet bevorzugt das gewählte Zielmodul.
- Bei fehlender Berechtigung oder fehlendem Menüeintrag wird auf vorhandene Module zurückgefallen.
- Die letzte Auswahl bleibt für den nächsten Login erhalten.

## Unterstützte Zielmodule

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

## Zusätzlich

- `tsconfig.json` wurde für aktuelle TypeScript-Versionen auf `ignoreDeprecations: "6.0"` angepasst.
- Ein doppelter Übersetzungsschlüssel `language` im frühen UI-Textblock wurde bereinigt.

## Test

Frontend-Build wurde erfolgreich ausgeführt:

```text
npm run build
✓ built
```

## Geänderte Bereiche

- Frontend Login-Zielauswahl
- Frontend Shell-Startseite
- TypeScript-Build-Kompatibilität

Keine Backend-, Datenbank- oder API-Änderungen.
