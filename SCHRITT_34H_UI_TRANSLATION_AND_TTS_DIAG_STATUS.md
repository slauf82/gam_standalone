# Schritt 34h – UI-Übersetzung aktivieren und TTS sauber trennen

- `/api/ui-translations` ist nun `permitAll`, damit die Login-Oberfläche bereits vor dem Login übersetzt werden kann.
- Die gewählte UI-Sprache wird über einen React-State im App/Shell-Bereich geführt und triggert Re-Render + Cache-Nachladung.
- Tabellen wie `translate_ITALIAN` werden mit robust quotiertem Tabellennamen angelegt.
- Browser-TTS bleibt Standard; MaryTTS bleibt optional und muss separat über Diagnose/Bundle-Pfad geprüft werden.

Erwartung für Italienisch:
1. UI-Sprache `Italiano` auswählen.
2. Backend-Endpunkt `/api/ui-translations` wird aufgerufen.
3. Tabelle `translate_ITALIAN` wird angelegt.
4. Beim ersten Aufruf werden Texte via LibreTranslate erzeugt/gecached.
5. Folgeaufrufe kommen aus der Datenbank.
