# Schritt 40k31c2a – Smart-Life-/Tuya-Geräteabruf über Benutzer-UID

## Ziel
Smart-Life- und Tuya-Smart-Geräte aus einem im Tuya-Cloud-Projekt verknüpften App-Konto werden nicht mehr ausschließlich über die allgemeine Projektgeräteliste gesucht.

## Änderungen
- Neues persistentes Feld `user_uid` je Tuya-Quelle.
- Automatische Datenbankmigration für bestehende Installationen.
- Geräteabruf in dieser Reihenfolge:
  1. `/v1.3/iot-03/devices?source_type=tuyaUser&source_id=<UID>`
  2. `/v1.0/users/<UID>/devices`
  3. bisherige Projektgeräteliste als Fallback
- Cursor-Paginierung über `has_more` und `last_row_key`.
- Verbindungstest zeigt Authentifizierung, UID, API-Abfrageweg, Gerätezahl und Diagnosekette.
- Eine erfolgreiche Authentifizierung mit null Geräten wird als eigener Fehlerzustand angezeigt.
- Klassifizierung für Wassersensoren und Luftwärmetauscher ergänzt.

## Konfiguration
Die UID wird unter Geräteverwaltung → Erkennungsquellen → Smart Life / Tuya Cloud eingetragen. Sie stammt aus dem verknüpften App-Konto des Tuya-Cloud-Projekts.
