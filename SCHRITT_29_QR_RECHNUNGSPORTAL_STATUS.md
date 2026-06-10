# Schritt 29 – QR-Rechnungsportal mit Sprachwahl

## Ziel

Patienten sollen eine gedruckte Rechnung per QR-Code scannen und anschließend digital abrufen können.

## Enthalten

- Neue Tabelle `invoice_access_tokens` wird beim Start automatisch angelegt
- Sichere zufällige Token statt Rechnungsnummer in URL
- QR-Code in der PDF-Rechnung
- QR-Code/Portal-Link in der Rechnungsdetailansicht
- Öffentliches Patientenportal unter `/api/invoice-portal/{token}`
- Sprachwahl im Portal: Deutsch, Englisch, Französisch, Ukrainisch
- Download der Rechnung in der gewählten Sprache
- Übersicht über bisherige, nicht stornierte Rechnungen desselben Patienten anhand `ADRESSID`
- Zugriffszähler und letzter Zugriff werden gespeichert
- Ablaufdatum über `app.invoice.portal.default-validity-days` vorbereitet

## Hinweise

- QR-Code nutzt den konfigurierbaren Basislink `app.invoice.portal.public-base-url`.
- Für lokale Tests ist der Default `http://localhost:8080/api/invoice-portal`.
- Für produktive Nutzung muss der öffentliche Basislink auf die erreichbare Serveradresse gesetzt werden.
- Maven konnte in dieser Umgebung nicht ausgeführt werden, weil Abhängigkeiten nicht nachgeladen werden konnten.

