# Schritt 40k31c3a – Smart-Life-Einfachassistent

## Ziel
Smart-Life-/Tuya-Geräte sollen ohne Tuya-Developer-Portal, UID, Access ID oder Secret eingebunden werden können.

## Umsetzung
- Neuer Standardmodus **„Einfach über Home Assistant“**.
- Vorhandene und bereits getestete Home-Assistant-Verbindung wird ausgewählt.
- GAM liest die dort verfügbaren Geräte und ordnet Smart-Life-/Tuya-Geräte automatisch zu.
- Ein einziger Button speichert, prüft die Verbindung und zeigt die gefundenen Geräte.
- Technische Diagnose ist eingeklappt und stört normale Nutzer nicht.
- Der bisherige direkte Tuya-Cloud-Zugang bleibt als **Expertenmodus** erhalten.
- Datenbankmigration ergänzt `connection_mode` und `homeassistant_source_id` automatisch.

## Hinweis
Der einfache Modus nutzt bewusst die funktionierende offizielle Tuya-Integration von Home Assistant als Brücke. Ein eigener Tuya-Cloud-Projektzugang ist dafür nicht erforderlich.
