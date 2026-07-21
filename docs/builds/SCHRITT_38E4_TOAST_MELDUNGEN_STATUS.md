# Schritt 38e4 – Toast-Meldungen und Meldungsverlauf

## Ziel

GAM 2.0 erhält ein GAM-1.0-ähnliches Benachrichtigungssystem:

- Meldungen erscheinen oben rechts.
- Erfolgreiches Speichern schließt den Bearbeitungsdialog automatisch.
- Fehlermeldungen bleiben sichtbar, bis sie aktiv geschlossen werden.
- Die letzten Meldungen können über ein kleines Menü erneut angesehen werden.

## Umgesetzt

### Globale Toast-Meldungen

- Neue zentrale UI-Funktion `gamNotify(...)`.
- Neue Komponente `GamNotificationCenter`.
- Anzeige oben rechts.
- Standarddauer: 5 Sekunden.
- Fehler können dauerhaft sichtbar bleiben (`timeoutMs = 0`).
- Meldungen können manuell geschlossen werden.

### Meldungsverlauf

- Menübutton `Meldungen` oben rechts.
- Anzeige der letzten Meldungen mit Uhrzeit.
- Verlauf hält bis zu 25 Meldungen.

### Speichern / Fehlerfälle

In folgenden Bereichen wurden Toast-Meldungen ergänzt:

- Rechnungseditor
- Geräteverzeichnis
- Lagerverwaltung
- Modul-Administration
- Inventar ↔ Lager Materialbuchungen

### Dialogverhalten

Bei erfolgreichem Speichern schließen die modalen Bearbeitungsdialoge automatisch:

- Geräteverzeichnis
- Lagerverwaltung
- Modul-Administration

Bei Fehlern bleibt der Dialog geöffnet, damit Eingaben korrigiert werden können.

## Keine Änderungen

- Keine Backend-Änderungen
- Keine Datenbank-Änderungen
- Keine API-Änderungen

## Hinweis

Die Anzeigedauer ist in `GamNotificationCenter` aktuell zentral als Standardwert `5000` Millisekunden gesetzt und kann später in Programmeinstellungen überführt werden.
