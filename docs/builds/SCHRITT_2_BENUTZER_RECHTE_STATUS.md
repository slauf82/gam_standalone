# GAM 2.0 - Schritt 2 Benutzer/Rechte

Stand: 2026-05-23

## Ziel

Das komplette GAM 2.0 bekommt eine echte App-Struktur mit Rollenmenue und Benutzerverwaltung, ohne die bestehende Loginlogik zu zerstoeren.

## Neu umgesetzt

- Zentrales Rollenmodell `RoleCatalog`
- Menue-Endpunkt `/api/auth/menu`
- Admin-API fuer bestehende `accounts`-Tabelle
- Benutzerliste mit Suche
- Rollen aenderbar ueber `accounts.role`
- 2FA-Status sichtbar ueber `accounts.secretkey`
- Passwortstatus sichtbar, aber Passwoerter werden bewusst nicht veraendert
- Frontend-Shell mit Dashboard, Rechnungen, Inventar, Lager, Benutzer/Rechte
- Modulrahmen fuer Schritt 3 und 4 vorbereitet

## Bewusst kompatibel belassen

- Tabelle `accounts`
- Spalten `username`, `password`, `role`, `email`, `secretkey`
- Legacy-Passwortlogin
- 2FA/TOTP ueber bestehendes Secret

## Noch offen

- Benutzer neu anlegen
- Passwort neu setzen / migrieren
- feingranulare Rechte je Aktion
- Audit-Log fuer Admin-Aenderungen
- Passkey/WebAuthn spaeter
- Inventar/Geraete als Schritt 3
