# Schritt 38l – Personaldaten GDS

Basis: Schritt 38k6 – Modul-Key-Audit Login-Fix

## Ziel

Das Modul **Personaldaten** wurde als vollwertiges GDS-Modul vorbereitet. Dabei wurde bewusst zwischen Personalabteilung und Superadmin unterschieden.

## Fachliche Trennung

### Personalabteilung / normale Personalansicht

Sichtbar und bearbeitbar sind nur HR-relevante Daten:

- Personalnummer
- Vorname / Nachname / Titel
- Abteilung
- Berufsbezeichnung
- Beschäftigungsart
- Status
- Eintritt / Austritt
- Telefon / Mobil / E-Mail
- Anschrift
- Gesellschaftszuordnung
- Bemerkung

### Superadmin

Nur Superadmins sehen zusätzlich den geschützten IT-Bereich:

- IT-Benutzer
- Windows-/Loginname
- Postfach
- Geräte-/Portal-Login
- Passworthinweis
- Initial-/Übergabepasswort
- VPN/Zugang
- Superadmin-Notiz

Diese Felder sind für die Personalabteilung nicht sichtbar.

## GDS

- Modulregistrierung bleibt auf der aktuellen 38k6-Logik
- Login-Zielmodul Personaldaten bleibt gültig
- Sticky-Aktionsleiste
- Tabelle oben
- Detaildialog
- Toasts und Änderungsdetails
- lokale Bearbeitung mit späterer DB-Anbindung vorbereitet

## Hinweis

Die vorhandene historische GAM-Personaldaten-Leseansicht wird beim ersten Öffnen übernommen und in das neue GDS-Format normalisiert. Danach arbeitet das Modul lokal weiter, bis die finale DB-Schreiblogik nachgezogen wird.
