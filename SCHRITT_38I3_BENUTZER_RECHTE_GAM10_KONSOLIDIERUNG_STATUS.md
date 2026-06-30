# Schritt 38i3 – Benutzer/Rechte GAM-1.0-Konsolidierung

## Ziel

Benutzer und Rechte werden fachlich weiter konsolidiert. Die GAM-1.0-Rollenlogik fuer das Rechnungsprogramm wird beruecksichtigt, obwohl Reports in GAM 2.0 ein eigenes Modul sind.

## Umgesetzt

- Der separate Navigationsbutton `Rechteverwaltung` entfaellt.
- Es bleibt ein zentraler Bereich `Benutzer/Rechte`.
- Die doppelte Rolle `administrator` wurde entfernt.
- `admin` bleibt als eindeutige Admin-Rolle erhalten.
- `superadmin` bleibt zentral in `accounts` und hat absichtlich Vollzugriff.
- Normale Modul- und Gesellschaftsrechte bleiben in `userapplication`.
- Fuer das Rechnungsprogramm gilt GAM-1.0-kompatibel:
  - `user` = normaler Rechnungszugriff
  - `mainuser` = Rechnungszugriff plus Reports
  - `admin` = administrativer Modulzugriff plus Reports
- Reports werden im Frontend fuer `Rechnungsprogramm` nicht mehr automatisch bei jeder Rolle angezeigt, sondern nur bei `mainuser` oder `admin`.

## Hinweise

- Die alte Anwendung `Rechteverwaltung` wird weiterhin auf `Benutzer/Rechte` abgebildet, falls historische Daten diesen Namen enthalten.
- Das versteckte technische Fallback `PermissionsPage` bleibt nur als Sicherheitsnetz bestehen, ist aber nicht mehr direkt in der Navigation sichtbar.
