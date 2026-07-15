# Schritt 40d3 – zentrale Marketing-Stammdaten und Auswahlfelder

## Umgesetzt
- Ziel-Filiale ausschließlich als Auswahl aus `filiale`.
- Quelllager ausschließlich als Auswahl aus zentralen Lagerstammdaten.
- Lagerarten zentral in der Administration pflegbar.
- Marketing-Aktionsarten zentral in der Administration pflegbar.
- Marketing-Materialarten zentral in der Administration pflegbar.
- Marketingaktionen speichern stabile IDs und zeigen die zugehörigen Namen an.
- Inaktive Stammdaten erscheinen nicht mehr in neuen Auswahlfeldern.
- Vorbelegung mit sinnvollen Startwerten für Lager-, Aktions- und Materialarten.

## Administration
Die neuen Kataloge erscheinen in der bestehenden Stammdatenverwaltung:
- Lagerarten
- Lager
- Marketing-Aktionsarten
- Marketing-Materialarten
- Filialen / Standorte (bereits vorhanden)

## Test
1. Administration öffnen und neue Aktions-/Material-/Lagerart anlegen.
2. Ein Lager anlegen und einer Lagerart zuordnen.
3. Filiale anlegen oder vorhandene Filiale verwenden.
4. Marketing öffnen: alle vier Werte müssen als Auswahl erscheinen.
5. Marketingaktion anlegen und prüfen, ob Tabelle und Historie die Namen korrekt anzeigen.
