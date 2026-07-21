# Schritt 38k6 – Modul-Key-Audit Login-Fix

## Ziel
Patientenverwaltung und Terminverwaltung werden beim Login exakt wie die bereits funktionierenden Module Geräteverzeichnis und Lagerverwaltung behandelt.

## Änderungen
- Kanonische Login-Zielseiten zentral abgesichert.
- Patientenverwaltung: `Patientenverwaltung`, `patienten`, `patient`, `patients`, `module.patients` → `patients`.
- Terminverwaltung: `Terminverwaltung`, `termine`, `termin`, `kalender`, `appointment`, `appointments`, `module.appointments` → `appointments`.
- Login speichert zusätzlich den kanonischen Page-Key in `gam_selected_application_page`.
- Shell liest harte Login-Ziele sofort vor Rollen-/Menü-Fallbacks.
- Patientenverwaltung und Terminverwaltung in den Backend-Rollen für Admin/Superadmin ergänzt.
- `userapplication`-Mapping um Patientenverwaltung und Terminverwaltung ergänzt.

## Erwartetes Verhalten
- Auswahl Geräteverzeichnis → Login startet im Geräteverzeichnis.
- Auswahl Lagerverwaltung → Login startet in der Lagerverwaltung.
- Auswahl Patientenverwaltung → Login startet in der Patientenverwaltung.
- Auswahl Terminverwaltung → Login startet in der Terminverwaltung.
- Rechnungsprogramm ist nur noch Fallback bei wirklich unbekannten Zielmodulen.
