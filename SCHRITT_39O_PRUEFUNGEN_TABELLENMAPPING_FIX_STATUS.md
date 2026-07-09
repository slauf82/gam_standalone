# Schritt 39o – Prüfungen Tabellenmapping Fix

## Anlass

Beim Backend-Start bzw. beim Laden der Modulübersicht wurde der Anzeigename `kontrolle/inbetriebnahme/einweisung` als echter Tabellenname behandelt. Dadurch entstand der Datenbankfehler:

```text
Table 'kopfzentruminventardb.kontrolle/inbetriebnahme/einweisung' doesn't exist
```

Fachlich korrekt sind drei getrennte Tabellen:

- `kontrolle`
- `inbetriebnahme`
- `einweisung`

## Änderung

- Modulübersicht `Prüfungen` verwendet jetzt eine Mehrtabellen-Zählung.
- Die Anzeige zeigt `kontrolle, inbetriebnahme, einweisung` statt eines falschen zusammengesetzten Tabellennamens.
- Die Gesamtzahl wird aus den vorhandenen drei Tabellen summiert.
- Die `count()`-Hilfsfunktion ignoriert ungültige Tabellennamen mit `/` frühzeitig und erzeugt dadurch keine unnötigen SQL-Warnungen mehr.

## Keine fachliche Änderung

Der bestehende `/api/gam/compliance`-Endpunkt bleibt unverändert und liest weiterhin getrennt aus:

- Kontrollen
- Inbetriebnahmen
- Einweisungen

## GitHub-Zuordnung

Empfohlen als Patch nach `v2.0.2`, z. B. `v2.0.3`, falls 2.0.2 bereits veröffentlicht wurde.
