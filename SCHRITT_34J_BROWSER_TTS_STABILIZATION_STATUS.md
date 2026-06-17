# Schritt 34j – Browser-TTS Stabilisierung

Dieser Schritt setzt Browser/Windows-TTS wieder als zuverlässigen Standard.

## Änderungen

- Browser-TTS fällt nicht mehr stumm aus, wenn MaryTTS ausgewählt aber nicht erreichbar ist.
- MaryTTS-Fehler führen automatisch zurück zu Browser/Windows-TTS.
- Lange Vorlesetexte werden in kleinere Abschnitte geteilt, damit Chrome/Edge nicht ohne Ton abbrechen.
- Stimmen werden beim ersten Zugriff erneut angestoßen, falls der Browser sie verzögert lädt.

## MaryTTS

MaryTTS bleibt optional/diagnostisch, bis eine echte MaryTTS-Runtime im Paket liegt und stabil startet.
