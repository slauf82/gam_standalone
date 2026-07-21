# Schritt 38e9 – Login-/Logout-Meldungen im globalen Meldungssystem

## Ziel

Das globale Toast- und Meldungsverlaufssystem wird nicht mehr nur für CRUD- und Speichervorgänge verwendet, sondern auch für Authentifizierungsvorgänge.

## Umgesetzt

- Login erfolgreich erzeugt eine grüne Toast-Meldung.
- Login fehlgeschlagen erzeugt eine rote Toast-Meldung und bleibt im Verlauf sichtbar.
- 2FA-Login erfolgreich/fehlgeschlagen nutzt dasselbe Meldungssystem.
- Passkey-Login erfolgreich/fehlgeschlagen nutzt dasselbe Meldungssystem.
- 2FA-Registrierung, QR-Erzeugung und Bestätigung erzeugen Info-/Erfolgs-/Fehlermeldungen.
- Passkey-Registrierung und Passkey-Statusprüfung erzeugen Info-/Erfolgs-/Fehlermeldungen.
- Logout erfolgreich erzeugt eine grüne Toast-Meldung.
- Logout fehlgeschlagen erzeugt eine rote Toast-Meldung.
- Sitzungsablauf erzeugt eine Warnmeldung.
- Das Meldungszentrum ist nun auch im Loginbildschirm aktiv, damit Loginmeldungen sichtbar und im Verlauf abrufbar sind.

## Verhalten

- Standardmeldungen verschwinden nach ca. 5 Sekunden.
- Fehler bleiben stehen, bis sie manuell geschlossen werden.
- Alle Meldungen werden im Menü „Meldungen“ gesammelt.
- Logout bleibt weiterhin nur im authentifizierten Zustand sichtbar.

## Build

Frontend-Build erfolgreich getestet.

## Technische Änderung

Nur Frontend/UI. Keine Backend-, API- oder Datenbankänderungen.
