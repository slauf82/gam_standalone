# Schritt 34z7a – Patientenportal Compile-Fix

## Fix

`InvoicePortalController.java` enthielt in den neuen Portal-/TTS-Erweiterungen zwei fehlerhafte Java-String-Escapes:

- defektes Backslash-Escaping in `jsString(...)`
- defektes Double-Quote-Escaping in `escape(...)`

## Korrektur

- eigene `escapeJs(...)` Methode ergänzt
- `escape(...)` Java-syntaktisch korrigiert
- keine fachliche Änderung an Patientenportal/TTS/PDF-Sprache

## Erwartung

Backend sollte wieder kompilieren.
