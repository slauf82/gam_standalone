# Schritt 40k31.2 – Lokale USB-Klassifizierung

## Ziel

Lokal am GAM-Server erkannte USB- und Plug-and-Play-Geräte werden im gemeinsamen Geräteverzeichnis nicht länger unter „Sonstige Geräte“, sondern unter „USB / Plug and Play“ einsortiert.

## Umsetzung

- Kategorie „USB / Plug and Play“ in den Gerätebaum des Geräteverzeichnisses aufgenommen.
- Passendes Stecker-Symbol für die Kategorie ergänzt.
- Inventarklassifizierung erkennt nun auch:
  - USB
  - lokale USB-Geräte
  - USB_LOCAL
  - Plug and Play / Plug-and-Play
  - PnP / Windows PnP
- Die Klassifizierung gilt sowohl für neue Discovery-Treffer als auch für bereits persistierte Geräte, deren Typ oder Bezeichnung einen eindeutigen USB-/PnP-Hinweis enthält.

## Ergebnis

Lokale USB-Geräte erscheinen konsistent zusammen mit allen weiteren USB-/PnP-Geräten unter „USB / Plug and Play“.
