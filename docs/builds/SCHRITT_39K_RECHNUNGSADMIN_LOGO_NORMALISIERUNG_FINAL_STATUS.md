# Schritt 39k – Rechnungsadministration Logo-Normalisierung Final

## Ziel

Die Logoverwaltung verhält sich jetzt wie die normalisierten Textbausteine: Logos werden einmal im Logokatalog gepflegt und anschließend in Gesellschaften nur noch ausgewählt.

## Änderungen gegenüber 39j

- Logos sind echte Stammdaten im Katalog `rechnungslogo`.
- `rechnungslogo` wurde um `NAME` erweitert.
- `rechnungsgesellschaft.LOGO_ID` verweist auf ein vorhandenes Logo.
- Gesellschaftsmaske bietet:
  - vorhandenes Logo auswählen,
  - direkte Vorschau,
  - optional neues Logo hochladen und sofort der Gesellschaft zuordnen.
- Logokatalog bietet:
  - Logo-Name,
  - Logo-URL/-Pfad,
  - Upload mit Vorschau.
- Mehrfachverwendung ist möglich: mehrere Gesellschaften können dieselbe `LOGO_ID` nutzen.
- Logo `ID 0` ist der Systemstandard/Fallback.
- Logo `ID 0` ist geschützt und nicht lösch-/änderbar.
- Verwendete Logos können nicht gelöscht werden; der Benutzer erhält einen Hinweis mit der Anzahl der Gesellschaften.
- Vorschau/PDF nutzen bei fehlender oder defekter Gesellschaftszuordnung das Logo `ID 0` bzw. den bestehenden Notfall-Fallback.

## Fachliches Ergebnis

Die Rechnungsadministration ist damit konsistent normalisiert:

- Anreden: Katalog → Gesellschaftszuordnung
- Rechnungstexte: Katalog → Gesellschaftszuordnung
- Rechtliche Hinweise: Katalog → Gesellschaftszuordnung
- Grußformeln: Katalog → Gesellschaftszuordnung
- Logos: Katalog → Gesellschaft

## Migrationshinweis

Zusätzlich enthalten:

- `sql/rechnungsadmin_logo_normalisierung_39k.sql`

Die Anwendung legt die erforderlichen Strukturen beim Start zusätzlich defensiv selbst an.
