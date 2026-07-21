# Schritt 40k31e – Discovery-Quellensteuerung und Stabilisierung

## Enthalten

- Fehler `discoverySourceBadges is not defined` behoben: Die Badge-Komponente steht nun sowohl Discovery als auch Geräteverzeichnis im gemeinsamen Modul-Scope zur Verfügung.
- Quellen-Badges bleiben dedupliziert und werden im Geräteverzeichnis wieder sicher gerendert.
- Home-Assistant-Brücke für Smart Life / Tuya begrenzt den Import auf eindeutig als Tuya/Smart-Life erkennbare Geräte.
- Nicht eindeutig zuordenbare Home-Assistant-Geräte werden nicht mehr als Tuya-Geräte übernommen.
- Discovery-Diagnose nennt geprüfte, übernommene und verworfene Geräte.
- Bestehende Quellenaktivierung, Statusmeldungen und Merge-Logik bleiben unverändert erhalten.

## Hinweis

Die Home-Assistant-REST-Zustandsdaten enthalten nicht bei jeder Installation die Integrationszugehörigkeit. Daher arbeitet die Brücke bewusst konservativ. Der Modus „Benutzerkonto + technisches Projekt“ bleibt für eine vollständige Tuya-Geräteliste die bevorzugte Verbindung.
