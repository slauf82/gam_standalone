# Schritt 34z5 – LoginDialog Translation Completion

## Ziel

Der LoginDialog soll in den vier neuen UI-Sprachen (Italienisch, Schwedisch, Türkisch, Russisch) vollständig übersetzt wirken, so wie bereits Deutsch, Englisch, Französisch und Ukrainisch.

## Änderungen

- Ergänzt feste LoginDialog-Fallbacks für:
  - legacyLoginHint
  - uiLanguage
  - chooseApplication
  - moduleVisibilityHint
  - username
  - password
  - login
  - passwordLogin
  - totpRegister
  - totpLogin
  - passkeyRegister
  - passkeyLogin
  - TOTP-/Passkey-Hinweise und Aktionsbuttons
- Fehlende LoginDialog-Keys explizit in den UI-Keykatalog aufgenommen.
- Deutsche Runtime-/DB-Fehlfüllungen werden beim Merge für Fremdsprachen verworfen, damit sie gute lokale Übersetzungen nicht mehr überdecken.
- GAM-1.0-Logik bleibt erhalten:
  - DB-first
  - nur aktuelle UI-Sprache
  - kein Mehrsprachen-Massenlauf
  - Login bleibt schnell

## Nicht geändert

- Rechnungsmodul wird in diesem Schritt bewusst nicht erweitert.
- MaryTTS bleibt unverändert.
- Browser-TTS bleibt wie in 34k/34z4.
- Keine Backend-Architekturänderung.

## Build-Hinweis

Frontend-Build konnte in der Paketumgebung nicht vollständig geprüft werden, weil `node_modules` im ZIP nicht enthalten ist. Die Änderung ist rein TypeScript/React-seitig in `frontend/src/main.tsx` umgesetzt.
