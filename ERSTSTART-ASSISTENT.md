# GAM 2.1.0 Preview 1 – Erststart-Assistent

Beim Start prüft GAM automatisch, ob die konfigurierte Datenbank bereits eingerichtet ist.

Ist keine nutzbare GAM-Datenbank vorhanden oder enthält sie noch kein Administratorkonto, erscheint vor dem Login der Willkommensassistent.

## Ablauf

1. Oberflächensprache auswählen
2. erstes Administratorkonto anlegen
3. optionale Praxisangaben erfassen
4. Startvariante auswählen:
   - **Neue leere Praxis** – vollständige Tabellenstruktur ohne Beispieldaten
   - **Beispieldatenbank** – anonymisierte Demo mit Patienten, Terminen, Rechnungen, Aufgaben, Kommunikation, Labor und Wartezimmer
5. GAM erstellt beziehungsweise initialisiert die Datenbank automatisch
6. Anmeldung mit dem neu angelegten Administratorkonto

## Sicherheit

- Der Assistent ist nur erreichbar, solange GAM noch nicht eingerichtet ist.
- Sobald eine initialisierte Datenbank mit mindestens einem Konto erkannt wird, werden die Einrichtungsendpunkte gesperrt.
- Das Administratorpasswort muss mindestens acht Zeichen enthalten und wird als SHA-256-Hash in der kompatiblen `accounts`-Struktur gespeichert.
- Bestehende Datenbanken werden nicht überschrieben.

## Datenbankzugang

GAM verwendet weiterhin die konfigurierten Werte:

- `GAM_DB_URL`
- `GAM_DB_USER`
- `GAM_DB_PASSWORD`

Der konfigurierte MariaDB-Benutzer benötigt beim Erststart die Berechtigung, die angegebene Datenbank anzulegen und Tabellen zu erstellen.
