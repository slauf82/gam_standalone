# 40k37e56c-fix38 — Releasevorbereitung 2.1.0

- Backend-Maven-Version auf `2.1.0` gesetzt.
- Frontend-Version und Root-Package des npm-Lockfiles auf `2.1.0` synchronisiert.
- `WindowsRemoteInventoryService`: Deprecation-Abhängigkeit von `org.apache.http.client.config.AuthSchemes` entfernt. Die beiden unveränderten, von winrm4j ausgewerteten Scheme-Namen `Negotiate` (SPNEGO) und `NTLM` bleiben erhalten. Keine Änderung an Reihenfolge, Fallback, Zugangsdaten oder Netzwerkpfad.
- `DiscoveryRegistrationRepository`: Keine ungesicherte Änderung; der konkrete `unchecked`-Diagnoseort muss erst mit `javac -Xlint:unchecked` aus einem vollständigen Maven-Build nachgewiesen werden. Keine Warnungen unterdrückt.
- Historische SQL-Skriptdateinamen mit `preview3` bleiben bewusst bestehen, da der FirstRunSetupController diese als Dateipfade verwendet; Umbenennung wäre eine funktionale Datenbank-Migrationsänderung. Historische Build-Dokumentation bleibt erhalten.
- Apple USB/WLAN, Pairing, PMD3, Erkennung, Inventarisierung, Merge und die zentralisierten Detailgruppen sind unverändert.

## Noch zu prüfen
- `mvn -f backend/pom.xml clean compile` (idealerweise `-Dmaven.compiler.showWarnings=true` und `-Xlint:unchecked,deprecation`).
- `npm ci && npm run typecheck` im Frontend.
