# Schritt 34x – Login schnell, Übersetzung im Hintergrund

## Ziel
Der Login und der direkte Wechsel ins gewählte Modul dürfen nicht mehr durch Runtime-Übersetzungen blockiert werden.

## Änderungen
- `useUiTranslationCache(...)` unterstützt jetzt `enabled` und `delayMs`.
- LoginDialog startet keine DB-/LibreTranslate-Pflege mehr.
- App-Rahmen startet keine globale Übersetzungspflege mehr.
- Shell startet die Übersetzungspflege erst verzögert nach dem Rendern.
- Es wird nur noch die aktuell gewählte UI-Sprache gepflegt, nicht mehr automatisch `it/sv/tr/ru` gemeinsam.
- Pro Browser-Session wird eine Sprache nur einmal angefragt.
- Browser-TTS bleibt unverändert auf dem stabilen 34k-Stand.

## Erwartung
- Nach Login erscheint das Rechnungsprogramm deutlich schneller.
- Übersetzungen können nachziehen, dürfen aber die Navigation nicht blockieren.
- Weniger parallele `/api/ui-translations`-Requests.
- Weniger abgebrochene AsyncRequest-Warnungen.
