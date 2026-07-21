# Schritt 40k19 – Mehrere FRITZ!Box-Identitätsquellen

## Umgesetzt

- Beliebig viele FRITZ!Boxen als getrennte Quellen verwaltbar
- Bezeichnung, Standort/Filiale, Host, Port, optionaler Benutzer und Kennwort je Quelle
- Quellen einzeln aktivieren, deaktivieren, speichern, testen und löschen
- Sichtbarer Verbindungstest mit Modell, FRITZ!OS, Geräteanzahl und Vorschau je Quelle
- Alle aktiven Quellen werden während der Discovery nacheinander abgefragt
- Geräte werden vorrangig anhand der MAC-Adresse quellenübergreifend zusammengeführt
- IP-Adressen, die in mehreren Quellen vorkommen, werden nicht unsicher zur Identifikation verwendet
- Herkunftsdaten (Quellen-ID, Quellenname, Standort) werden intern an FRITZ!Box-Geräten mitgeführt
- Vorhandene 40k18a/40k18b-Einzelkonfiguration wird beim ersten Start automatisch als „FRITZ!Box 1“ übernommen
- Kennwort-only-Anmeldung und Benutzername-plus-Kennwort bleiben unterstützt

## Datenbank

Neue Tabelle `gam_fritzbox_sources`. Die bisherige Einzelkonfiguration in `gam_settings` bleibt unangetastet und dient nur einmalig als Migrationsquelle.

## Build-Hinweis

Ein vollständiger Maven-/Frontend-Build konnte in der Erstellungsumgebung nicht ausgeführt werden, weil Maven sowie `frontend/node_modules` nicht vorhanden waren und kein Internetzugriff zum Nachladen bestand. Das Releasepaket enthält bewusst keine Build-Artefakte oder Abhängigkeitsordner.
