# Schritt 40d15 – Zahlungsdokumente: PDF/UA-Struktur und persönliche Anrede

## Änderungen

- Sichtbarer Dokumenttitel ist jetzt eine echte `H1`-Überschrift.
- Die Überschrift erzeugt über `-fs-bookmark-level: 1` ein passendes PDF-Lesezeichen.
- Es gibt keine zusätzliche technische Titelzeile im Dokumentkörper.
- Persönliche Anrede nutzt Anredeindex, Anredetext, Titel und Nachnamen.
- Deutsche Beispiele: `Sehr geehrter Herr Dr. Mustermann,` bzw. `Sehr geehrte Frau Dr. Mustermann,`.
- Englische Beispiele: `Dear Mr Mustermann,` bzw. `Dear Ms Mustermann,`.
- Nur bei fehlender oder uneindeutiger Anrede bleibt `Guten Tag Vorname Nachname,` als Fallback.

## PAC-Ziel

- Warnung wegen fehlender Überschrift beseitigen.
- Warnung wegen `Zahlungserinnerung` an möglicherweise unzulässiger Stelle beseitigen.
- Überschrift und Lesezeichen müssen inhaltlich übereinstimmen.

## Test

Zahlungserinnerung, Mahnung 1–3 und Inkassoschreiben neu erzeugen und jeweils mit PAC prüfen.
