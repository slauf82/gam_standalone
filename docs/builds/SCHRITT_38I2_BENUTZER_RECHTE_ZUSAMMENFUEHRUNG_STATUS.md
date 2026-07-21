# Schritt 38i2 – Benutzer/Rechte zusammengeführt

## Ziel
Benutzerverwaltung und Rechteverwaltung fachlich zusammenführen, ohne die bestehende GAM-1.0-Logik zu verfälschen.

## Umgesetzt

- Benutzer/Rechte ist nun die zentrale Ansicht für Benutzer und deren Rechte.
- Benutzer-Dialog besitzt einen zusätzlichen Tab „Rechte“.
- Normale Modul-/Gesellschaftsrechte werden weiterhin über `userapplication` gepflegt.
- Superadmins werden weiterhin zentral über `accounts.role = superadmin` behandelt.
- Superadmins erhalten bewusst Vollzugriff und benötigen keine `userapplication`-Einträge.
- In der Benutzertabelle wird sichtbar, ob ein Benutzer Superadmin ist oder wie viele userapplication-Zuordnungen vorhanden sind.
- Rechtezuordnungen können direkt im Benutzerkontext angelegt, bearbeitet und gelöscht werden.
- Das separate Modul „Rechteverwaltung“ bleibt als Hinweis-/Detailzugang vorhanden und verweist auf Benutzer/Rechte.
- Toasts und Änderungsdetails verwenden weiterhin den GDS-/GCS-Meldungsverlauf.

## Datenlogik

- `accounts`: Benutzerstammdaten, Passwort, 2FA, zentrale Kontorolle, Superadmin.
- `userapplication`: normale Rechte pro Benutzer, Anwendung, Gesellschaft und Rolle.

## Build-Hinweis

Frontend-Build konnte in der Erzeugungsumgebung nicht ausgeführt werden, weil `frontend/node_modules` absichtlich nicht Bestandteil des Austauschpakets ist. Das Paket bleibt vollständig als Quellcodepaket ohne generierte Ordner.
