# Schritt 38i4 – Benutzer/Rechte Abschlussfix

## Inhalt

- Recht löschen im Rechte-Tab ergänzt
- Löschbestätigung für userapplication-Zuordnungen
- Änderungsdetails/Toast beim Löschen von Rechten
- doppelte Rechtezuordnungen werden verhindert
- Layout der Gesellschafts-ID/Rollen-Felder stabilisiert
- Hoch/Runter-Buttons am Gesellschaftsfeld werden nicht mehr vom Rollenfeld verdeckt
- bestehender Benutzer-Löschbutton heißt zur Klarstellung jetzt „Benutzer löschen“

## Architektur

- Superadmin bleibt in accounts und hat Vollzugriff
- normale Modul-/Gesellschaftsrechte bleiben in userapplication
- Reports-Zugriff für GAM-1.0-Kompatibilität: Rechnungsprogramm mainuser/admin erhält Rechnung + Reports
