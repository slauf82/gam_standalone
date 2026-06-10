# Schritt 31l – main.tsx Login-i18n-Fix

Basis: vom Benutzer bereitgestellte aktuelle `frontend/src/main.tsx`.

## Gefixt

- Login-Hinweis nutzt jetzt `ui("legacyLoginHint")`
- `Application wählen` / `Anwendung wählen` nutzt jetzt `ui("chooseApplication")`
- Modulbuttons im Login nutzen jetzt `uiModule(app)`
- Login-Typ-Buttons übersetzt:
  - Passwort
  - 2FA registrieren
  - 2FA-Login
  - Passkey registrieren
  - Passkey-Login
- Platzhalter bleiben über `ui("username")` und `ui("password")`
- Loginbutton nutzt jetzt `ui("login")`
- 2FA-/Passkey-Hinweise und Buttons teilweise angebunden
- `currentUiLanguage()` liest jetzt beide Keys:
  - `gam_ui_language`
  - `gam.uiLanguage`
- `changeUiLanguage(...)` schreibt beide Keys
- globale `ui(...)`-Funktion fällt nicht mehr hart auf Deutsch zurück, sondern auf die gewählte Oberflächensprache
- Shell-Navigation nutzt Modulübersetzung über `moduleText(...)`

## Wichtig

Bitte diese Datei als `frontend/src/main.tsx` ersetzen.
