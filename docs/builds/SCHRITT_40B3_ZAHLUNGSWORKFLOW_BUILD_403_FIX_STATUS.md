# Schritt 40b3 – Zahlungsworkflow Build- und 403-Fix

## Behoben

- Java-Textblöcke in `PaymentWorkflowRepository` korrekt formatiert.
- Die 76 Folgefehler aus Zeile 17/18 werden dadurch beseitigt.
- Rechteprüfung des Zahlungsworkflows an den funktionierenden Rechnungsworkflow angeglichen.
- Lesen nutzt `canOpen(account, "Rechnungsprogramm", companyId)`.
- Schreiben nutzt `canWrite(account, "Rechnungsprogramm", companyId)`.
- Gesellschaft wird aus der tatsächlich gefundenen Rechnung verwendet.

## Test

1. Backend mit Maven kompilieren und starten.
2. Bestehende Rechnung öffnen.
3. Zahlungsworkflow muss ohne HTTP 403 geladen werden.
4. Testzahlung buchen und Seite neu laden.
5. Zahlung und Status müssen erhalten bleiben.
