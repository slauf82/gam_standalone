# Schritt 40k35c – Android-Apps im Bereich „Registriert“

## Änderungen

- Registrierte Android-Geräte erhalten einen Aufklapppfeil auch dann, wenn ihre Zusatzdaten ausschließlich aus dem App-Inventar stammen.
- Unter dem Gerät werden `Benutzer-Apps` und `System-Apps` als getrennte, aufklappbare Kategorien dargestellt.
- Benutzer-Apps werden zusätzlich nach Android-Benutzerprofil gruppiert.
- Ein einzelnes Profil wird direkt geöffnet; bei mehreren Profilen stehen getrennte Unterbereiche bereit.
- Android-Benutzer werden über `cmd user list` beziehungsweise als Fallback über `pm list users` erkannt.
- Benutzer-Apps werden je Benutzer-ID inventarisiert (`pm list packages --user <ID> -3`).
- Gleiche Paketnamen können für mehrere Benutzer getrennt gespeichert werden.
- Bestehende App-Datensätze werden beim Start automatisch auf den mehrbenutzerfähigen Primärschlüssel migriert.

## Datenmodell

`gam_discovery_installed_apps` enthält neu:

- `android_user_id`
- `android_user_name`

Der Primärschlüssel besteht aus Geräteidentität, Paketname und Android-Benutzer-ID.

## Kompatibilität

Bereits vorhandene Datensätze werden Benutzer 0 zugeordnet. Beim nächsten App-Inventarlauf werden System- und Benutzer-Apps anhand der neuen Struktur aktualisiert.
