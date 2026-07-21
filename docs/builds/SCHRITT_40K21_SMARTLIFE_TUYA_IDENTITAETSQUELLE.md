# Schritt 40k21 – Smart Life / Tuya als Identitätsquelle

## Enthalten

- Mehrere Smart-Life-/Tuya-Cloud-Quellen parallel
- Auswahl zwischen Smart Life und Tuya Smart
- Auswahl der Tuya-Cloud-Region
- Access ID / Client ID und Access Secret / Client Secret
- Speichern, Bearbeiten, Löschen und Verbindungstest
- Signierte Tuya-Cloud-Anfragen per HMAC-SHA256
- Anzeige der gefundenen und online erreichbaren Geräte
- Gerätevorschau mit automatischer Typzuordnung
- Einbindung in den bestehenden Discovery-Fortschritt und die Diagnostik
- Zusammenführung primär über MAC-Adresse, danach IP-Adresse; Tuya-ID/UUID als Cloud-Identität

## Typzuordnung

GAM versucht unter anderem folgende Kategorien direkt zu erkennen:

- Steckdose / Schalter
- Beleuchtung
- Sensor
- Robotik
- Kamera / Videoüberwachung
- Klima & Gebäudetechnik
- Rollladen / Beschattung
- Türschloss / Zutritt
- Gateway / Zentrale
- Haushaltsgeräte
- Energie / Wechselrichter
- IR-Hub / Fernbedienung

Nur wenn keine konkretere Zuordnung möglich ist, wird `Smart Life / Tuya Gerät` verwendet.

## Voraussetzung

Die normale Smart-Life-E-Mail-/Kennwort-Anmeldung reicht für die offizielle Tuya Cloud API nicht aus. Benötigt werden Access ID und Access Secret eines Tuya-Cloud-Projekts, das mit dem verwendeten Smart-Life- beziehungsweise Tuya-Smart-Konto verknüpft wurde.

## Build-Hinweis

Die Quellen wurden in dieser Umgebung erstellt und geprüft. Ein vollständiger Maven-Build war nicht möglich, weil Maven lokal nicht installiert ist.
