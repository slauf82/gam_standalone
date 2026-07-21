# Schritt 28b – Rechnungstexte aus bestehenden translation_* Tabellen

- nutzt die echten Alt-GAM-Schlüssel wie invoiceSalutationLabel0, invoiceInvoiceTextLabel0, invoiceLawHintLabel0, invoiceGreetingsLabel0
- ersetzt Platzhalter wie <Anrede>, <Vorname>, <Nachname>, <Behandlungsdatum>, <Gesellschaftsname>
- wandelt -br- in Zeilenumbrüche um
- verwendet bestehende Tabellen translation_german, translation_english, translation_french, translation_ukrainian
- kein neues Tabellenmodell
