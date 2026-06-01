# Schritt 11 – erster technischer Gesamttest

Stand: GAM 2.0 Arbeitsversion / technischer Gesamttest vorbereitet

## Ziel dieses Schritts

Dieser Schritt macht aus der bisherigen Entwicklungsbasis eine testbare Gesamtversion. Es geht noch nicht um den fachlichen Produktivtest, sondern um den ersten vollständigen technischen Durchlauf:

1. Backend bauen/starten
2. Frontend bauen/starten
3. MariaDB-Verbindung prüfen
4. Login prüfen
5. Rollen-/Security-Status prüfen
6. Rechnungsmodul prüfen
7. ZUGFeRD-/PDF-Endpunkte prüfen
8. Inventar/Lager/Workflow-Endpunkte prüfen
9. Fehler und offene Punkte strukturiert sammeln

## Neu in Schritt 11

- `TESTPLAN_GAM_2_0.md` mit Testreihenfolge
- `docs/ERSTER_TECHNISCHER_GESAMTTEST.md` als kompakte Anleitung
- `scripts/smoke-test.ps1` für Windows/PowerShell
- `scripts/smoke-test.sh` für Linux/macOS/Git Bash
- `test/gam-2.0-smoke.http` für IntelliJ/VS Code REST Client
- `sql/gam2_test_notizen.sql` als optionale SQL-Notizdatei für Testdaten/Prüfungen
- `.env.test.example` für einen klaren Testmodus

## Was dieser Schritt bewusst NICHT macht

- keine produktive Datenmigration ändern
- keine alten Tabellen automatisch umbauen
- keine Rechnungen automatisch erzeugen
- keine echten Daten löschen
- keine finale ZUGFeRD-Zertifizierung ersetzen

## Erwartetes Ergebnis

Nach diesem Schritt kannst du erstmalig systematisch testen, ob GAM 2.0 technisch zusammenläuft:

- App startet
- DB erreichbar
- Login liefert Token
- geschützte APIs reagieren korrekt
- Rechnungs-/Inventar-/Lager-/Workflowmodule sind erreichbar
- Fehler sind reproduzierbar dokumentierbar

## Wichtiger Hinweis

Da der Code hier nicht mit Maven gegen deine lokale Datenbank kompiliert/gestartet werden konnte, ist Schritt 11 als Testpaket vorbereitet. Der erste echte Lauf passiert auf deinem System mit Maven/Java/MariaDB.
