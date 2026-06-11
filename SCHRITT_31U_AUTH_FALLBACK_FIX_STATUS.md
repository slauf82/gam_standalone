# Schritt 31u – Auth-Fallback-Fix

## Problem

Nach dem Login war das Rechnungsprogramm kurz sichtbar, danach wurde wieder der Login-Dialog angezeigt.

## Ursache

`frontend/src/api/client.ts` löste bei jedem 401/403 automatisch aus:

```ts
logout();
window.dispatchEvent(new CustomEvent("gam-auth-expired"));
```

Dadurch konnte ein einzelner noch nicht vollständig migrierter oder temporär verbotener API-Call die komplette Sitzung beenden.

## Fix

- Automatischer Logout nur noch bei `/auth/me`
- Andere 401/403-Fehler bleiben lokale API-Fehler und werfen nicht mehr die komplette Oberfläche zurück auf Login
- `loadMenu()`-Fehler nutzen ein Superadmin-Fallback-Menü statt Logout

## Erwartetes Verhalten

Nach Login bleibt die Shell sichtbar. Falls ein einzelner Modul-Call fehlschlägt, bleibt die Anwendung bedienbar.
