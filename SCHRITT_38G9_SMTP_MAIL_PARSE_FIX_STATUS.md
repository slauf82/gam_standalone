# Schritt 38g9 – SMTP-Mail-Parsing-Fix

## Ziel

Der direkte Bestell-Mailversand scheiterte nach 38g8 mit Meldungen wie:

- `501 Syntax error in parameters or arguments`
- `Bestell-E-Mail konnte nicht versendet werden: Could not parse mail`

Ursache war sehr wahrscheinlich eine zu empfindliche Verarbeitung von From-/To-Headern und Mailadressen im Zusammenspiel mit GMX/Jakarta Mail.

## Änderungen

- Direkter Mailversand nutzt jetzt `MimeMessage` + `MimeMessageHelper` statt `SimpleMailMessage`.
- Empfängeradresse wird robust normalisiert und validiert.
- Absenderadresse wird robust normalisiert und validiert.
- Displaynamen wie `GAM 2.0 <neu.gam@gmx.de>` werden sauber verarbeitet.
- Reine Adressen wie `neu.gam@gmx.de` funktionieren ebenfalls.
- Header-Zeilenumbrüche in Betreff/Absendername werden entfernt.
- SMTP-Benutzername wird auf reine Mailadresse normalisiert.
- Fehlermeldungen sind jetzt genauer:
  - ungültiger Empfänger
  - ungültiger Absender
  - Mail konnte nicht erzeugt werden
  - Mailserver hat Versand abgelehnt

## Lokale Testkonfiguration

`application-local.yml` ist für die einfache lokale Testbarkeit mit dem vorbereiteten GMX-Testkonto vorbelegt:

- SMTP-Server: `mail.gmx.net`
- Port: `587`
- STARTTLS: `true`
- Benutzer: `neu.gam@gmx.de`
- Passwort lokal in `application-local.yml`
- Absender: `neu.gam@gmx.de`
- Absendername: `GAM 2.0`

## Sicherheitshinweis

Die echten GMX-Zugangsdaten sind nur für die lokale Testumgebung gedacht und dürfen nicht in ein öffentliches Repository übernommen werden.

## Build

- Frontend-Build konnte in dieser Umgebung nicht ausgeführt werden, weil `node_modules` im schlanken Quellcodepaket bewusst nicht enthalten ist.
- Backend-Maven-Build konnte in dieser Umgebung nicht ausgeführt werden, weil `mvn` hier nicht verfügbar ist.
