# Schritt 38k5 – Login-Zielmodul Whitelist-Fix

## Ziel
Patientenverwaltung und Terminverwaltung müssen nach dem Login genauso direkt geöffnet werden wie Geräteverzeichnis und Lagerverwaltung.

## Änderungen

- Patientenverwaltung und Terminverwaltung werden als harte Login-Ziele gepuffert.
- Das gewählte Zielmodul wird zusätzlich in `gam_forced_login_target_page` gespeichert.
- Das Login-Ziel wird nicht mehr zu früh aus `sessionStorage` entfernt.
- Die Shell liest das Ziel vor dem Rechnungsprogramm-Fallback erneut aus.
- Nach erfolgreicher Übernahme wird der temporäre Zielpuffer sauber entfernt.

## Erwartetes Verhalten

- Login mit Ziel „Patientenverwaltung“ öffnet direkt die Patientenverwaltung.
- Login mit Ziel „Terminverwaltung“ öffnet direkt die Terminverwaltung.
- Geräteverzeichnis und Lagerverwaltung bleiben unverändert funktionsfähig.
- Rechnungsprogramm bleibt nur Fallback, wenn kein gültiges Ziel gewählt wurde.
