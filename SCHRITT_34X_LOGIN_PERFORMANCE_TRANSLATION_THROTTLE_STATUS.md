# Schritt 34x – Login-Performance / Translation-Throttle

Ziel:
- Login und Wechsel ins Rechnungsprogramm dürfen nicht durch UI-Übersetzungen blockiert werden.
- Runtime-Übersetzungen laufen nicht mehr als 5-Sprachen-Massenjob direkt beim Login.

Änderungen:
- Frontend lädt nach Login nur noch die aktuell gewählte UI-Sprache.
- Übersetzungsrequest startet verzögert, damit Shell/Rechnungsprogramm zuerst rendern können.
- Backend begrenzt synchrone LibreTranslate-Aufrufe pro Request (`app.translation.libretranslate.max-sync-auto-translations`, Default 3).
- Bereits bekannte/cached Übersetzungen werden weiter sofort genutzt und persistiert.
- Fehlende Texte werden nicht mehr als deutsche Zieltexte gespeichert.

Browser-TTS bleibt unverändert aus Schritt 34k.
