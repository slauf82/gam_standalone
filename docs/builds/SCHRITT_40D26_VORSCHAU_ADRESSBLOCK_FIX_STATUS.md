# Schritt 40d26 – Vorschau-Adressblock aus gespeicherten Rechnungsdaten

## Behoben
- Bei bestehenden Rechnungen verwendet die Vorschau für den sichtbaren Empfängerblock nicht mehr die aktuell geladene LBD.
- Der Backend-Endpunkt `/invoices/text-preview` liefert nun zusätzlich den tatsächlich aus `ADRESSID`, `KINDADRESSID` und `FIRMAADRESSID` aufgelösten Rechnungsempfänger zurück.
- Die Rechnungsübersicht übergibt diesen Empfänger an `InvoiceTextPreviewPanel`.
- Neue, noch nicht gespeicherte Rechnungen verwenden weiterhin die aktuell eingelesene LBD beziehungsweise die manuell erfasste Adresse.
- Anrede, Adressblock und Rechnungstexte stammen bei bestehenden Rechnungen damit aus derselben gespeicherten Rechnungsdatenquelle.

## Erwartetes Verhalten
- Bestehende Rechnung: Adresse ausschließlich aus gespeicherten Rechnungsrollen oder historischem `FADRESSE`-Fallback.
- Neue Rechnung: LBD-/manuelle Adresse bleibt unverändert nutzbar.
- Eine Muster-LBD kann den Adressblock einer bestehenden Rechnung nicht mehr beeinflussen.
