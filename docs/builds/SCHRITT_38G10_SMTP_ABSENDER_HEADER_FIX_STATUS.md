# Schritt 38g10 – SMTP Absender/Header Fix

## Ziel

Der direkte Mailversand darf nicht mehr daran scheitern, dass Absenderdaten aus UI/YAML/Copy-Paste als mehrere Adressen oder kombinierte Header interpretiert werden.

## Änderungen

- Absenderadresse wird robust aus `from` extrahiert.
- Anzeigename bleibt getrennt und wird nicht als Teil der Adresse validiert.
- Doppelte gleiche Adressen werden auf eine Adresse normalisiert.
- Unterschiedliche mehrere Adressen werden weiterhin sauber abgelehnt.
- Semikolon, Anführungszeichen und Headerumbrüche werden entschärft.
- `From`, `To` und SMTP-Username nutzen dieselbe Normalisierung.

## Erwartetes Ergebnis

`neu.gam@gmx.de`, `GAM <neu.gam@gmx.de>` oder versehentlich kopierte Varianten mit Anzeigename werden als genau eine Adresse behandelt.
