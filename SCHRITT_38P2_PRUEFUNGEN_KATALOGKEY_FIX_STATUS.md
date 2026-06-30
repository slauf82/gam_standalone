# Schritt 38p2 – Prüfungen Katalog-Key-Fix

## Ziel
Die drei Unterbereiche im Modul Prüfungen dürfen beim Laden nicht mehr den MasterData-Katalog `undefined` anfragen.

## Behoben
- Geräteprüfungen nutzt fest `device-checks` → Tabelle `kontrolle`
- Inbetriebnahmen nutzt fest `commissioning` → Tabelle `inbetriebnahme`
- Einweisungen nutzt fest `instructions` → Tabelle `einweisung`
- React-`key` wird nicht mehr als fachlicher Prop missbraucht
- `catalogKey` wird explizit an den Editor übergeben

## Ursache
Im Frontend wurde das Feld `key` gleichzeitig als React-Key und als fachlicher Katalogschlüssel verwendet. React reicht `key` jedoch nicht als normales Prop weiter. Dadurch kam im Editor `catalogKey = undefined` an.

## Ergebnis
Alle drei Prüfungsbereiche laden wieder mit gültigem MasterData-Katalog.
