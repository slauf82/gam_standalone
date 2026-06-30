# Schritt 38p4 – Prüfungen Frühwarnsystem

Ziel: GAM soll Benutzer aktiv davor schützen, wichtige Geräteprüfungen zu vergessen.

## Enthalten

- rote Warnung für überfällige Prüfungen
- gelbe Warnung für Prüfungen innerhalb der kommenden 14 Tage
- Warnkacheln je Prüfungsbereich
- Klick auf rote/gelbe Warnung filtert die jeweilige Tabelle
- Fälligkeits-Spalte in den Prüfungstabellen
- optische Hervorhebung überfälliger und bald fälliger Zeilen
- Toast-Hinweis beim Laden eines Prüfungsbereichs

## Fälligkeitslogik

### Geräteprüfungen

Berechnet aus vorhandenen Feldern der Tabelle `kontrolle`:

- DATUMLETZTEPRÜFUNG_STK + INTERVALL_STK / INTERVALL
- DATUMLETZTEPRÜFUNG_MTK + INTERVALL_MTK / INTERVALL
- DATUMLETZTEPRÜFUNG_BGV_A3 + INTERVALL_BGV_A3 / INTERVALL

Intervalle werden standardmäßig als Monatsintervalle behandelt. Enthält der Intervalltext Hinweise auf Jahr/Tag, wird entsprechend gerechnet.

### Einweisungen

- DATUMFOLGEEINWEISUNG wird als Fälligkeitsdatum ausgewertet.

### Zukunftssicherheit

Wenn spätere Tabellen Felder mit `NAECHSTE`, `NÄCHSTE`, `FAELLIG` oder `FÄLLIG` enthalten, werden diese automatisch in die Warnlogik aufgenommen.

## Farben

- Rot = überfällig
- Gelb = innerhalb der nächsten 14 Tage fällig

## Nicht enthalten

- interne Termin-Zielgruppenlogik
- globale Startdashboard-Warnung für interne Termine
- Benutzer-/Rollen-/Filialzuordnung für Termine

Diese Punkte sind für einen späteren separaten Schritt vorgesehen.
