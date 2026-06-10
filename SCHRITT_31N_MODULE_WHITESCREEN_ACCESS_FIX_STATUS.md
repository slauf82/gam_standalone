# Schritt 31n – Modulzugriff / White-Screen-Fix

## Ziel

Ein Klick auf einzelne historische Module darf die Oberfläche nicht mehr komplett weiß werden lassen.

## Enthalten

- `ModuleErrorBoundary` für Modulbereiche
- Superadmin/Admin erhält eine vollständige Fallback-Modulliste
- Modulfehler werden als Warnkarte angezeigt statt als White Screen
- Shell bleibt benutzbar
- defensive Behandlung von `rows`/`records`

## Erwartetes Verhalten

Wenn ein Modul noch nicht vollständig migriert ist oder Daten fehlen:

```text
Dieses Modul konnte nicht vollständig angezeigt werden. Die Anwendung bleibt nutzbar.
```

statt weißem Bildschirm.

## Hinweis

Falls ein konkretes Modul danach weiterhin fachlich leer bleibt, ist das kein Login-/Rechteproblem mehr, sondern ein fehlender Loader/Backend-Endpunkt oder eine noch nicht rekonstruierte Fachlogik.
