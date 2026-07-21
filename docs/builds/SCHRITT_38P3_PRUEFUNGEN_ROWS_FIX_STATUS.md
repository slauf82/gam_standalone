# Schritt 38p3 – Prüfungen Rows-Fix

## Ziel

Die Katalog-Keys aus 38p2 funktionieren, aber die Tabellen zeigten nur die Spaltenköpfe.

## Ursache

Im Prüfungen-Frontend wurde die Antwort von `loadMasterDataRows(...)` direkt in `setRows(...)` geschrieben.
Die MasterData-API liefert jedoch ein Objekt:

```text
{ catalog, rows }
```

Dadurch landete nicht `data.rows`, sondern das komplette Antwortobjekt im Tabellen-State.

## Korrektur

```text
const data = await loadMasterDataRows(catalogKey, query || '', 300);
setRows(data.rows || []);
```

## Betroffen

- Geräteprüfungen / `device-checks` / `kontrolle`
- Inbetriebnahmen / `commissioning` / `inbetriebnahme`
- Einweisungen / `instructions` / `einweisung`

## Keine fachlichen Änderungen

- keine neuen Tabellen
- keine neuen Spalten
- keine Änderung an CRUD
- nur Anzeige-/Ladefix für Tabelleninhalte
