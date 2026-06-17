# Schritt 34k – Browser-TTS Hörbarkeits-Fix

Ziel: Browser/Windows-TTS ist wieder der stabile, sofort hörbare Standard.

Änderungen:

- gespeicherte MaryTTS/Auto-Testeinstellungen werden beim Laden auf Browser zurückgesetzt
- MaryTTS wird beim Vorlese-Klick nicht mehr angesteuert
- SpeechSynthesisUtterance-Objekte werden global gehalten, damit Chromium/Edge sie nicht vorzeitig verwirft
- nach `speechSynthesis.cancel()` wird mit kurzem Delay gesprochen, damit Chrome/Edge nicht stumm bleiben
- lange Texte werden in kurze Segmente geteilt

MaryTTS bleibt separat zu diagnostizieren und wird erst wieder aktiv geschaltet, wenn die Runtime wirklich sauber vorhanden und startbar ist.
