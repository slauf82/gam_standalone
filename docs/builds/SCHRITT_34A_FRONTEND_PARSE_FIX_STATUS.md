# Schritt 34a – Frontend Parse Fix

Basis: Schritt 33k + Schritt 34 Bundled MaryTTS Auto-Start

Fix:
- `playMaryTtsAudio(...)` ist jetzt korrekt als `async function` deklariert.
- Ein versehentlich übrig gebliebenes `async` vor den TTS-Typdefinitionen wurde entfernt.
- `normalizeGamLanguage(...)` wurde ergänzt, damit die TTS-Sprachauswahl stabil auf `de/en/fr/uk` normalisiert.
- `toggleReading()` im Rechnungsvorschau-Frontend wurde neu strukturiert, damit die Klammerung wieder korrekt ist.

Prüfung:
- `npx vite build` wurde ausgeführt.
- Ergebnis: Vite-Transform erfolgreich, kein Parse-Fehler mehr in `src/main.tsx`.

Hinweis:
- `npm run build` wurde nicht vollständig als TypeScript-Projektprüfung bewertet, weil die ZIP bewusst ohne `node_modules` ausgeliefert wird und lokale Dependency-Versionen abweichen können.
