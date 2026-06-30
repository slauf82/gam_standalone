# Schritt 38g6 – Direkter Bestell-Mailversand

## Ziel

Das Bestelltool kann Bestellungen weiterhin über den externen Mailclient vorbereiten, bietet nun aber zusätzlich eine direkte Versandoption über GAM an.

## Umgesetzt

- Zweite Versandoption im Bestelldialog:
  - Mailclient öffnen
  - Direkt per GAM senden
- Betreff und Mailtext bleiben intern vor dem Auslösen bearbeitbar.
- Bestellung auslösen startet weiterhin den Workflow.
- Bei direktem Versand wird nach Workflow-Erzeugung ein Backend-Endpunkt für den SMTP-Versand aufgerufen.
- Meldungsverlauf enthält Workflow- und Mailversanddetails.
- SMTP ist standardmäßig deaktiviert, damit GAM ohne Mailkonto weiterhin startet.
- SMTP kann über `application-local.yml` oder Umgebungsvariablen aktiviert werden.

## Konfiguration

Eine Freemail-Adresse kann aus Sicherheits-/Providergründen nicht automatisch registriert werden. Für Gmail, GMX, Outlook usw. wird üblicherweise ein App-Passwort oder eine SMTP-Freigabe benötigt.

Beispiel:

```env
GAM_ORDERS_MAIL_ENABLED=true
SPRING_MAIL_HOST=smtp.example.de
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=gam.bestellung@example.de
SPRING_MAIL_PASSWORD=<app-passwort>
SPRING_MAIL_SMTP_AUTH=true
SPRING_MAIL_STARTTLS_ENABLE=true
GAM_ORDERS_MAIL_FROM=gam.bestellung@example.de
```

## Geänderte Bereiche

- Backend: neuer `/api/orders/send-email` Endpunkt
- Backend: `spring-boot-starter-mail`
- Frontend: API-Funktion `sendOrderEmail(...)`
- Frontend: Bestelltool Versandoptionen
- Konfiguration: `application-local.yml`, `.env.example`

## Hinweis

Build konnte in dieser Umgebung nicht vollständig ausgeführt werden, weil weder `node_modules` noch ein erreichbares Maven-Repository verfügbar sind. Das Paket bleibt ein vollständiges Quellcodepaket ohne generierte Ordner.
