# Schritt 31c – Login UI Cleanup

## Ziel

Die Loginseite wird nach Schritt 31b bereinigt und optisch verbessert.

## Enthalten

- doppeltes Sprachfeld entfernt
- nur noch ein sichtbares Feld: "Sprache der Oberfläche"
- technische Anzeige "UILanguage" entfernt
- Modulicons deutlich vergrößert
- Modulbuttons auf Icon-über-Text-Layout umgestellt
- Oberflächensprache bleibt weiterhin funktional
- PDF-Sprache im Rechnungsprogramm bleibt weiterhin getrennt

## Erwartetes Layout

```text
GAM Logo

Sprache der Oberfläche
[ Deutsch ▼ ]

[ Icon ]
Rechnungsprogramm

[ Icon ]
Reports

Benutzername
Passwort
[Anmelden]
```

## Test

1. Loginseite öffnen
2. Prüfen, dass nur ein Sprachfeld sichtbar ist
3. Sprache wechseln
4. Prüfen, ob Modulbutton-Texte wechseln
5. Prüfen, ob Icons größer und oberhalb der Texte erscheinen
