# Schritt 38l5 – Personal: Tabellenansicht alle sichtbaren Spalten

## Ziel
Die Tabellenansicht im Modul Personaldaten soll dieselbe Rollensicht nutzen wie der Bearbeiten-Dialog.

## Umsetzung
- HR-/Personalabteilung sieht weiterhin nur die HR-Spalten:
  - ID
  - NAME
  - VORNAME
  - STATUS
  - POSITION
  - FILIALE_ID
  - EMAIL
  - TELEFON
  - TELEFON2
  - BEMERKUNG_PERSONAL
- Superadmin sieht in der Tabelle nun alle sichtbaren `personal`-Spalten.
- Die vorherige Superadmin-Kurzliste wurde entfernt.
- Die Spaltenauswahl bleibt dynamisch und richtet sich nach den tatsächlich vom MasterData-Backend gelieferten Feldern.
- Bearbeiten-Dialog und Tabellenansicht sind dadurch konsistent.

## Wichtig
Es wurden keine neuen Datenbankfelder eingeführt.
