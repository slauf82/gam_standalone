# Schritt 39g – Rechnungsadministration: normalisierte Textbausteine korrigiert

## Ziel
Der Test von 39f zeigte, dass die Oberfläche wieder wie der 39d-Textsatz wirkte. Das war fachlich falsch für GAM 1.0.

## Korrektur
Die Rechnungsadministration verwendet jetzt wieder klar getrennte Kataloge:

- Anreden
- Rechnungstexte
- Rechtliche Hinweise
- Grußformeln
- Logos

Zusätzlich gibt es eine eigene Rubrik:

- Textzuordnung Gesellschaft

Dort wird pro Gesellschaft ausgewählt:

- welche Anrede verwendet wird
- welcher Rechnungstext verwendet wird
- welcher rechtliche Hinweis verwendet wird
- welche Grußformel verwendet wird

## Fachlicher Vorteil
Ein Textbaustein muss nur einmal gepflegt werden. Dieselbe Grußformel, Anrede oder derselbe rechtliche Hinweis kann mehreren Gesellschaften zugeordnet werden, ohne den Text mehrfach zu duplizieren.

## Technische Umsetzung
Neu ist die normalisierte Zuordnungstabelle:

`rechnungstext_gesellschaft_zuordnung`

mit den Feldern:

- `RGESELLSCHAFTS_ID`
- `ANREDE_ID`
- `RECHNUNGSTEXT_ID`
- `RECHTLICHER_HINWEIS_ID`
- `GRUSSFORMEL_ID`

Die Tabelle wird beim Start automatisch angelegt, falls sie noch fehlt.

## Vorschau / Rechnung
Die Rechnungsvorschau liest die zugeordneten Textbausteine je Gesellschaft aus dieser Tabelle. Wenn keine Zuordnung existiert, greift weiterhin der bisherige Übersetzungs-/Fallbacktext.

## Hinweis zum Build
In dieser Umgebung konnte Maven nicht ausgeführt werden, weil der lokale Maven-Wrapper Maven aus `repo.maven.apache.org` nachladen wollte und kein DNS/Internet verfügbar war.
