# Schritt 38l2 – Personaldaten auf Tabelle personal begrenzt

## Ziel

Die Personalverwaltung wird auf die vorhandene Tabelle `personal` zurückgeführt.
Es werden in der ersten Ausbaustufe keine zusätzlichen Komfortfelder und keine neue Personalstruktur erfunden.

## Umsetzung

- Personaldaten-Modul nutzt den vorhandenen MasterData-Katalog `personnel`.
- Datenquelle bleibt die Tabelle `personal`.
- UI-Felder werden aus den vorhandenen `personal`-Spalten gebildet.
- HR-Sicht zeigt nur personalabteilungs-relevante Spalten.
- Superadmin-Sicht zeigt zusätzlich vorhandene IT-/Admin-Spalten derselben Tabelle.
- Keine separaten Personaldaten außerhalb von `personal`.
- Lokale 38l-Demo-Speicherung wurde entfernt.
- Speichern erfolgt wieder gegen Backend/MasterData-CRUD.
- Löschen wurde im MasterData-Controller ergänzt.

## Sichtkonzept

### Personalabteilung

- NAME
- VORNAME
- STATUS
- POSITION
- FILIALE_ID
- EMAIL
- TELEFON
- TELEFON2
- BEMERKUNG

### Nur Superadmin

- DIENSTHANDY
- VPN_TOKEN
- DIENSTLAPTOP

Diese Spalten sind vorhandene `personal`-Spalten aus dem bisherigen Katalog und werden nicht neu erfunden.
