# Schritt 38g11 – Maildialog-Verbesserung

## Ziel
Der Mailversand wird vor der weiteren Nutzung in anderen GAM-Modulen bedienbarer und robuster gemacht.

## Enthalten

- Kommunikationsassistent mit erweitertem Maildialog
- Live-Prüfung für SMTP-Server, Port, Benutzer, Passwort, Absender, Reply-To und Testempfänger
- Normalisierte Anzeige der tatsächlich verwendeten E-Mail-Adressen
- getrennte Felder für Absender-E-Mail und Anzeigename
- optionale Reply-To-Adresse
- Versandprüfung vor Testmail
- Mailprüfung im Bestelltool-Dialog
- Bestellung wird bei ungültigen Maildaten nicht mehr ausgelöst
- Backend-SMTP-Konfiguration erweitert um fromName und replyTo
- Reply-To wird beim Versand gesetzt, wenn vorhanden

## Hinweis
Das Paket ist ein vollständiges Quellcodepaket ohne generierte Ordner wie node_modules, dist oder target.
