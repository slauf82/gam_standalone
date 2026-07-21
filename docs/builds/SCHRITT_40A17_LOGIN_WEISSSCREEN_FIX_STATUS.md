# Schritt 40a17 – Login-Weißbildschirm-Fix

## Ursache
Beim Rendern des Login-Dialogs wurde `pageFromApplication(...)` aufgerufen. Diese Funktion existierte nicht. Dadurch brach React bereits vor Anzeige des Login-Dialogs mit einem JavaScript-Fehler ab.

## Korrektur
Die Modulfilterung verwendet jetzt die bestehende Zuordnung `LOGIN_APPLICATION_TO_PAGE` und als Fallback `normalizeStartPage(...)`.

## Beibehalten
- Einstellungen → Module
- Einstellungen → Workflows → Rechnungsworkflow
- globale Modulaktivierung
- Ausblendung deaktivierter Module

## Test
1. Frontend starten: Login-Dialog muss wieder erscheinen.
2. Einstellungen als Login-Ziel wählen.
3. Einstellungen → Module öffnen.
4. Einstellungen → Workflows → Rechnungsworkflow öffnen.
5. Ein optionales Modul deaktivieren, speichern und Frontend neu laden.
