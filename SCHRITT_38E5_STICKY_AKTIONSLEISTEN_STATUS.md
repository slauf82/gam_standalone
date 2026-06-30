# Schritt 38e5 – Sticky Aktionsleisten

## Ziel

Lange Verwaltungslisten sollen produktiver bedienbar sein. Wichtige Aktionen wie Neu, Suche, Filter und Auswahl bleiben beim Scrollen sichtbar.

## Umgesetzt

### Geräteverzeichnis

- Aktionsleiste bleibt beim Scrollen sichtbar.
- „Neues Gerät“ wurde in die Aktionsleiste integriert.
- Suche und Quellenfilter bleiben jederzeit erreichbar.
- Tabellenkopf bleibt innerhalb der Ergebnisliste sichtbar.

### Lagerverwaltung

- Aktionsleiste bleibt beim Scrollen sichtbar.
- „Neuer Eintrag“, Suche und Auswahl Lager/Verbrauchsmaterial sind in einer Zeile zusammengeführt.
- Filter „nur mit Bestand“ bleibt jederzeit erreichbar.
- Tabellenkopf bleibt innerhalb der Lagerliste sichtbar.

### Modul-Administration

- Adminbereich, Suche, Laden und Neu bleiben beim Scrollen sichtbar.
- Aktionsliste und Vorschautabelle bleiben getrennt scrollbar.

### Inventar ↔ Lager

- Buchungszeile bleibt beim Scrollen sichtbar.
- Material-ID, Geräte-ID, Menge, Grund und Buchungsbuttons bleiben erreichbar.

## Technische Änderungen

- Neue CSS-Klasse `.sticky-actionbar`.
- Sticky Tabellenköpfe für `.scroll-table thead th`.
- Mobile Ansicht bleibt geschützt: Sticky-Leisten werden auf kleinen Bildschirmen wieder normal dargestellt.

## Nicht geändert

- Keine Backend-Änderungen.
- Keine Datenbank-Änderungen.
- Keine API-Änderungen.

## Hinweis

Dieser Schritt baut auf den modalen Dialogen aus Schritt 38e3 und den Toast-Meldungen aus Schritt 38e4 auf.
