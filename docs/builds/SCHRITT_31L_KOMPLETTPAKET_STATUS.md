# Schritt 31l – Komplettpaket

Dieses Paket basiert auf dem vollständigen Schritt 31k und enthält zusätzlich die korrigierte `frontend/src/main.tsx` aus Schritt 31l.

## Enthalten

- vollständiges GAM-2.0-Projekt aus Schritt 31k
- gepatchte `frontend/src/main.tsx`
- Login-i18n-Fix aus Schritt 31l
- Vereinheitlichung der UI-Sprachkeys
- Rechnungsprogramm-UI fällt nicht mehr hart auf Deutsch zurück
- PDF-Sprache bleibt separat

## Test

1. Backend starten
2. Frontend starten
3. Login öffnen
4. Oberflächensprache auf English/Français/Українська umstellen
5. Prüfen:
   - Login-Hinweis
   - Anwendung wählen
   - Modulbuttons
   - Authentifizierungsbuttons
   - Benutzername/Passwort-Placeholder
   - Anmelden-Button
6. Rechnungsprogramm öffnen und prüfen, ob erste UI-Texte der Oberflächensprache folgen
