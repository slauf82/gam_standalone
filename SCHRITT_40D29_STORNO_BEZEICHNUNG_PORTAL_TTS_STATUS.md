# Schritt 40d29 – Stornobezeichnung und Portal-TTS

## Belegbezeichnungen
- Originalrechnung bleibt `Rechnung <Nummer>`.
- Bei storniertem Original erscheint darunter `bereits storniert`.
- Belege mit S-Suffix heißen `Stornorechnung <Nummer>S`.
- Vorschau, PDF, Detailansicht, Suchliste, Portal und Archivbezeichnung verwenden dieselbe Trennung von Belegart und Status.

## Patientenportal
- Vorlesetechnik ist auswählbar: Piper TTS oder Browser/Windows.
- Standard beim ersten Aufruf ist Piper TTS.
- Auswahl wird in LocalStorage gespeichert.
- Piper-Audio wird über `/api/tts/audio` erzeugt.
- Bei nicht verfügbarem Piper erfolgt automatischer Browser-TTS-Fallback.
- Stop beendet Browser-Sprache und laufendes Piper-Audio.
