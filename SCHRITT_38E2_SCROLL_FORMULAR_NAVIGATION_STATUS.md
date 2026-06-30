# Schritt 38e2 – Scrollbare Listen und Formularnavigation

## Inhalt

- Geräteverzeichnis: Klick auf **Neues Gerät** springt automatisch zum Formular und fokussiert das Namensfeld.
- Geräteverzeichnis: Bei geladenem Gerät erscheint **⬇ Zum Formular**.
- Geräteverzeichnis: Ergebnisliste ist intern vertikal scrollbar.
- Lagerverwaltung: Klick auf **Neuer Eintrag** springt automatisch zum Formular und fokussiert das Namensfeld.
- Lagerverwaltung: Bei geladenem Eintrag erscheint **⬇ Zum Formular**.
- Lagerverwaltung: Ergebnisliste ist intern vertikal scrollbar.
- Modul-Administration: Klick auf **Neu** springt automatisch zum Formular und fokussiert das erste Feld.
- Modul-Administration: Bei geladenem Datensatz erscheint **⬇ Zum Formular**.
- Modul-Administration: Aktionsliste und Vorschautabelle sind scrollbar begrenzt.
- Inventar ↔ Lager: Zuordnungen und Materialbewegungen sind ebenfalls scrollbar gekapselt.

## Ziel

Bei großen Datenmengen muss nicht mehr durch die gesamte Seite gescrollt werden. Listen erhalten eigene Scrollbereiche, während Formularbereiche gezielt per Sprungfunktion erreichbar sind.

## Hinweis

Dieser Schritt ändert nur Frontend-Bedienbarkeit und CSS. Keine API- oder Datenbanklogik wurde verändert.
