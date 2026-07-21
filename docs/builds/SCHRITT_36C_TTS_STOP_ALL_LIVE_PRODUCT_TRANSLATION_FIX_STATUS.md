# Schritt 36c – TTS Stop-All und Live-Produktübersetzung Fix

## Basis
- GAM 2.0 v1.7.0
- Aufbauend auf Schritt 36b
- PiperTTS bleibt Standard
- BrowserTTS bleibt Fallback

## Korrekturen

### 1. Stopptaste beendet jetzt alle laufenden Vorleseinstanzen
Vorher konnte der Button bereits wieder auf „Vorlesen“ stehen, während noch mehrere Audio-/BrowserTTS-Instanzen parallel liefen.

Behoben durch:
- zentrale Stop-Funktion `stopAllGamSpeech()`
- Abbruch aller BrowserTTS-Utterances
- mehrfaches `speechSynthesis.cancel()` für Browser-Restqueues
- Stoppen aller aktiven Piper-Audio-Objekte
- Abbruch laufender Piper-Fetch-Requests per `AbortController`
- Run-ID-Schutz gegen verspätet eintreffende Audioantworten

### 2. Kein Fallback-Start nach Stop
Wenn ein Piper-Request während des Downloads/Renderns abgebrochen wird, darf er nicht anschließend automatisch BrowserTTS starten.

Behoben durch:
- Run-ID-Prüfung im Piper-Fallback-Pfad
- abgebrochene Requests kehren still zurück

### 3. Produktbeschreibungen live übersetzen, aber nicht speichern
Produktbeschreibungen werden weiterhin nicht in die translation_* Tabellen geschrieben.

Behoben/verbessert durch:
- `/api/ui-translations/live` ist nun explizit freigegeben
- Live-Übersetzung unterstützt jetzt auch die neuen Sprachen `es`, `pt`, `nl`, `pl`, `cs`
- Vorschau übersetzt nur die tatsächlich verwendeten Produktpositionen
- gespeicherte Rechnungspositionen können ihre vorhandene Zeilenbeschreibung als Fallback nutzen
- keine DB-Persistenz für Produktbeschreibungen

## Testempfehlung
1. Backend starten
2. Frontend starten / Strg+F5
3. Rechnungsvorschau öffnen
4. Sprache auf Italienisch oder Englisch stellen
5. Produktbeschreibung prüfen
6. Vorlesen starten
7. während des Vorlesens Stop drücken
8. prüfen, dass wirklich alle laufenden Stimmen sofort stoppen

## Status
Testpaket erstellt. Lokaler Build wurde in der Sandbox nicht vollständig ausgeführt, weil externe Maven/NPM-Abhängigkeiten hier nicht zuverlässig verfügbar sind.
