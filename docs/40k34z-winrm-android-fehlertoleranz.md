# Schritt 40k34z – WinRM-Quelle und robuste Android-Inventarisierung

## WinRM
- WinRM ist jetzt eine echte, serverseitig persistierte Erkennungs-/Inventarisierungsquelle im Gerätemanager.
- Konfigurierbar sind Aktivierung, Benutzername, Kennwort, HTTP/HTTPS und Port.
- Das Kennwort wird nie wieder an das Frontend ausgeliefert; dort erscheint nur `passwordConfigured`.
- Ein Testgerät kann direkt aus der Oberfläche per Hostname oder IP geprüft werden.
- Standardports: HTTP 5985, HTTPS 5986.
- Bestehende Property-/Umgebungsvariablen bleiben als Fallback erhalten, solange noch keine Datenbankkonfiguration gepflegt wurde.
- Die bisher nur als „geplant“ geführte WinRM-Karte wurde entfernt.

## Android
- Große ADB-Ausgaben werden bereits während der Prozesslaufzeit parallel gelesen. Dadurch kann der stdout-Pipe-Puffer den ADB-Prozess nicht mehr blockieren.
- `getprop`, `df`, `ps -A`, große `dumpsys`-Abfragen sowie die System-App-Liste können dadurch vollständig Daten liefern.
- Jeder Teilbefehl besitzt weiterhin einen eigenen Timeout und kann die restliche Inventarisierung nicht abbrechen.
- Fehler und leere Teilabfragen werden gesammelt und erst in der Abschlussmeldung ausgegeben.
- Bei einem echten Timeout bleiben bereits empfangene Teildaten erhalten.

## Geänderte Dateien
- `backend/src/main/java/de/kopfzentrum/gam/inventory/AndroidAdbService.java`
- `backend/src/main/java/de/kopfzentrum/gam/inventory/WindowsRemoteInventoryService.java`
- `backend/src/main/java/de/kopfzentrum/gam/inventory/WinRmSettingsRepository.java`
- `backend/src/main/java/de/kopfzentrum/gam/inventory/InventoryController.java`
- `frontend/src/api/client.ts`
- `frontend/src/main.tsx`

## Prüfung
Eine vollständige Maven-/Vite-Kompilierung war in der isolierten Arbeitsumgebung nicht möglich, weil Maven nicht installiert war, der Wrapper seine Distribution ohne Internet nicht laden konnte und das Frontend-Archiv keine `node_modules` enthielt. Quellstruktur, Klammerbilanzen, Endpunkte und Import-/Aufrufnamen wurden statisch geprüft.
