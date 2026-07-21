# Schritt 40d23

- ADRESSID, KINDADRESSID und FIRMAADRESSID als getrennte Rollen modelliert.
- Minderjährige Rechnungsempfänger werden serverseitig blockiert.
- Kinderrechnung erst mit volljährigem Elternteil/Vertreter und KINDADRESSID möglich.
- Firmenkostenübernahme über FIRMAADRESSID getrennt aufgelöst.
- RDATUM als Rechnungsdatum und BDATUM als Behandlungsdatum getrennt übernommen.
- Rechnungs- und Zahlungsdokumente verwenden den effektiven Rechnungsempfänger.
