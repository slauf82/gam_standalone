# Rechnungsmodell 40d23 – Rollen und Datumsfelder

## Rollen aus `rechnungsdetails`

- `ADRESSID`: erwachsene Hauptperson, Rechnungsempfänger bzw. Elternteil/Ansprechpartner.
- `KINDADRESSID`: behandeltes minderjähriges Kind. Ist dieses Feld gesetzt, darf das Kind nicht zugleich Rechnungsempfänger sein.
- `FIRMAADRESSID`: Firma beziehungsweise Kostenübernehmer. Für die sichtbare Rechnungsanschrift hat die Firma Vorrang; `ADRESSID` bleibt als natürliche Hauptperson/Ansprechpartner erhalten.

## Datumsfelder

- `RDATUM`: Rechnungsdatum.
- `BDATUM`: Behandlungsdatum.

Beide Werte werden getrennt geladen und dürfen nicht gegenseitig ersetzt werden.

## Schutzlogik

- Eine über `ADRESSID` geladene minderjährige Person darf keine Rechnung erhalten.
- Für einen Kinderfall müssen eine volljährige Hauptperson in `ADRESSID` und das behandelte Kind in `KINDADRESSID` vorliegen.
- Eine Firma wird unabhängig über `FIRMAADRESSID` geführt.
- Alle IDs werden gegen `ADRESSEN.ID` validiert.
