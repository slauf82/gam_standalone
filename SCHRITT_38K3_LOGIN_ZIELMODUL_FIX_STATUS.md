# Schritt 38k3 – Login-Zielmodul-Fix Patienten/Termine

## Ziel

Der Login-Direktzugriff auf die neueren Module muss zuverlässig funktionieren. Wenn im Login-Dialog **Patientenverwaltung** oder **Terminverwaltung** gewählt wird, darf GAM 2.0 nicht mehr auf das Rechnungsprogramm zurückfallen.

## Änderungen

- `Patientenverwaltung` wird eindeutig auf die interne Seite `patients` abgebildet.
- `Terminverwaltung` wird eindeutig auf die interne Seite `appointments` abgebildet.
- Startseiten werden zusätzlich normalisiert, damit auch gespeicherte Werte wie `patientenverwaltung`, `terminverwaltung`, `patient`, `appointment` oder `appointments` korrekt aufgelöst werden.
- Der Shell-Start nach erfolgreichem Login lässt `patients` und `appointments` jetzt als direkte Login-Zielmodule zu.
- Das bisherige Fallback auf `invoices` greift nicht mehr, wenn eines dieser Zielmodule ausdrücklich gewählt wurde.
- Modultext-/i18n-Schlüssel für `module.patients` und `module.appointments` ergänzt.

## Testfälle

1. Im Login-Dialog `Patientenverwaltung` wählen.
2. Einloggen.
3. Erwartung: GAM öffnet direkt die Patientenverwaltung.

4. Ausloggen.
5. Im Login-Dialog `Terminverwaltung` wählen.
6. Einloggen.
7. Erwartung: GAM öffnet direkt die Terminverwaltung.

8. Browser neu laden, wenn `gam_start_page = patients` oder `appointments` im LocalStorage steht.
9. Erwartung: Die gespeicherte Zielseite bleibt erhalten und wird nicht auf Rechnung umgebogen.

## Hinweise

Dieser Schritt verändert keine Datenbanktabellen und keine fachliche Patienten-/Terminlogik. Es handelt sich um einen gezielten Frontend-Fix für Login-Zielmodul, Startseite und Modul-Key-Abgleich.
