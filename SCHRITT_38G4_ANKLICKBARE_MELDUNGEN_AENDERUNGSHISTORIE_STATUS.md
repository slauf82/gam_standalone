# Schritt 38g4 – Anklickbare Meldungen / Änderungshistorie

## Ziel

Toast-Meldungen und der Meldungsverlauf sollen nicht nur anzeigen, dass etwas gespeichert wurde, sondern bei relevanten Aktionen auch nachvollziehbar machen, was geändert wurde.

## Umgesetzt

- Meldungen im Verlauf sind anklickbar, wenn Änderungsdetails vorhanden sind.
- Toasts sind ebenfalls anklickbar, wenn Details vorhanden sind.
- Detaildialog zeigt:
  - Modul
  - Aktion
  - Zeitpunkt
  - Datensatz/Referenz
  - geänderte Felder mit Vorher/Nachher-Werten
- Erste frontendseitige Änderungshistorie ohne Backend- oder Datenbankänderung.
- Unterstützt aktuell insbesondere:
  - Aufgaben
  - Freigaben
  - Bestelltool
  - Modul-Administration
  - Inventar ↔ Lager Materialbuchung

## Technischer Hinweis

Die Änderungshistorie ist in diesem Schritt bewusst UI-/Sitzungsbasiert. Sie dient zum schnellen Nachsehen, was gerade geändert wurde. Eine revisionssichere, dauerhafte Historie wäre ein späterer Backend-/Datenbank-Schritt.

## Build

Frontend-Build erfolgreich getestet.
