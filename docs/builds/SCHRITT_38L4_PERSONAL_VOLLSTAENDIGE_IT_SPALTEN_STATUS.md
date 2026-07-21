# Schritt 38L4 – Personaldaten vollständige HR-/IT-Spaltentrennung

Stand: 29.06.2026

## Ziel

Die Personalverwaltung wertet die tatsächliche Struktur der Tabelle `personal` vollständig aus.
Die HR-/Personalabteilungsansicht endet bei `bemerkung_personal`; alle folgenden Spalten sind IT-/Superadmin-Felder.

## HR-Spalten

- ID
- NAME
- VORNAME
- STATUS
- POSITION
- FILIALE_ID
- EMAIL
- TELEFON
- TELEFON2
- BEMERKUNG_PERSONAL

## IT-/Superadmin-Spalten

- EMAIL_PW_EXTERN
- EMAIL_PW_INTERN
- RECHNER_IP
- OFFICE_LIZENZ
- PLONE_BENUTZER
- PLONE_PW
- MICROSOFT_KONTO
- MICROSOFT_PW
- DIENSTHANDY
- VPN_TOKEN
- VPN_PIN
- QNAP_BENUTZER
- QNAP_PW
- DIENSTLAPTOP
- BEMERKUNG

## Umsetzung

- MasterData-Katalog `personnel` enthält jetzt alle vorhandenen Spalten der Tabelle `personal`.
- Frontend kennt alle IT-Spalten und zeigt sie nur für Superadmins an.
- Passwort-/PIN-Felder werden als Passwortfelder gerendert.
- Bit-Felder werden als Ja/Nein-Auswahl dargestellt.
- HR sieht keine IT-/Admin-Spalten.
- Keine erfundenen Spalten.
- Keine zweite Personalverwaltung.
