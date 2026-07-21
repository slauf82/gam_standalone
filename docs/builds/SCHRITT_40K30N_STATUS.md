# Schritt 40k30n – pufferungsunabhängige Live-Anzeige

- Discovery-Ereignisse werden weiterhin unmittelbar über Callbacks erzeugt.
- Neue Long-Polling-Schnittstelle liefert Ereignisse spätestens nach einer Sekunde aus.
- Das Frontend verarbeitet jedes Ereignis sofort und wartet nicht auf den globalen Abschluss.
- SSE bleibt als Diagnose-/Kompatibilitätsweg erhalten, wird vom Frontend aber nicht mehr verwendet.
- Genau ein Worker wird pro Discovery-Session gestartet.
