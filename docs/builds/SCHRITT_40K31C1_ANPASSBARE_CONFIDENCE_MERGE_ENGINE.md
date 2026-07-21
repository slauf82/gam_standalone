# Schritt 40k31c1 – anpassbare Confidence-/Merge-Engine

Die in 40k31c eingeführte gewichtete Identitätsbewertung ist nun vollständig konfigurierbar.

- Schwellwert für automatische Zusammenführung
- Schwellwert für mögliche Dubletten
- Gewichte für MAC, Seriennummern, Geräte-ID, Hostname, IP, Hersteller und Typ
- Quellenboni
- Auto-Merge ein/aus
- harte Identitätskonflikte als Merge-Sperre
- Schutzregel: IP-Adresse darf niemals allein zusammenführen
- persistente Speicherung in `gam_discovery_merge_settings`
- Validierung: Dublettenschwellwert muss unter dem Auto-Merge-Schwellwert liegen
- Standardwerte können jederzeit wiederhergestellt werden
