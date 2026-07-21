# Schritt 31m – UI Hardcoded Text Cleanup

## Ziel

Möglichst viele hart codierte sichtbare Texte aus `frontend/src/main.tsx` wurden auf `ui(...)` bzw. `uiModule(...)` umgestellt.

## Schwerpunkt

- Login
- Anwendungsauswahl
- Authentifizierungsbuttons
- Dashboard
- Hauptnavigation
- Rechnungsprogramm: Suche, Detail, Editor, Vorschau-Grundlabels
- Patientenportalbox
- Status-/Hinweistexte

## Wichtige technische Korrektur

Die globale Funktion

```ts
function ui(key:string, lang:GamLanguage='de')
```

wurde geändert auf:

```ts
function ui(key:string, lang:GamLanguage=currentUiLanguage())
```

Damit fallen Module nicht mehr automatisch auf Deutsch zurück.

## Noch zu prüfen

Einige deutsche Texte können weiterhin als Dictionary-Werte enthalten sein. Das ist korrekt.
Entscheidend ist, ob sie sichtbar noch hart gerendert werden.

Scan-Hinweise nach dem Patch:

```text
Kompatibler Login
Anwendung wählen
Neue Rechnung
Rechnung suchen
Rechnung bearbeiten
Gesellschaft
Bitte wählen
Suche Nummer
Suchen
Bitte links
PDF-Sprache
Digitales Rechnungsportal
Portal öffnen
Zahlungsart
Rechnungsdatum
Gutschein
Rabatt
Ratenzahlung
Entfernen
Speichern fehlgeschlagen
Proforma konnte
Bitte mindestens eine Position
```

## Empfehlung

Jetzt einmal alle Login- und Rechnungsbereiche auf EN/FR/UK durchklicken und die verbliebenen sichtbaren Resttexte notieren.
