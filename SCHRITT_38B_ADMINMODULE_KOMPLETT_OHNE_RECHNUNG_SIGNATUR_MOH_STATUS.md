# Schritt 38b – Adminmodule außerhalb Rechnung vervollständigt

Basis: Schritt 38 / GAM 2.0 v1.7.5-Arbeitsstand.

## Ziel

Die Administrationsbereiche aus GAM 1.0 sollen – mit Ausnahme von Signatur, MOH-Auswertung und Rechnungsadministration – in GAM 2.0 sichtbar und bearbeitbar werden.

## Ergänzt

Zusätzlich zu Schritt 38 wurden weitere historische GAM-1.0-Stammdatenbereiche aufgenommen:

- Anwendungen
- Benutzer-Anwendungen / Rollen-Zuordnung
- Menübaum / Rechtebaum
- Oberflächen-Themes
- einfache Gesellschaftsliste
- altes Geräteverzeichnis
- Filiale-Geräte-Zuordnung
- Gerät-Verbrauchsmaterial-Zuordnung
- Mitarbeiterkürzel
- Namenskonten
- Kassenbuch-Kopfdaten
- Ordnerfreigaben
- Ordnerfreigabe-Auswahl
- installierte Software je Arbeitsplatz
- News / Hinweise

## Bewusst ausgespart

- Rechnungsadministration
- Signatur
- MOH-Auswertung
- Rechnungstabellen und Rechnungstexte

## Technische Änderung

Der generische MasterData-Controller unterstützt nun auch nicht-numerische bzw. manuell vergebene Primärschlüssel wie `lager.CODE` und `kassenbuchoben.MANDANTENNUMMER`.

## Nächster Schritt

Nach erfolgreichem Test kann die Roadmap auf den letzten großen Block reduziert werden:

- Rechnungsadministration
