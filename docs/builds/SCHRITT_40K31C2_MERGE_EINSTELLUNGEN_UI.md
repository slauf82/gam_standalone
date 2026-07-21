# Schritt 40k31c2 – Merge-Einstellungen in der Geräteverwaltung

Die in 40k31c1 eingeführte konfigurierbare Confidence-/Merge-Engine ist nun über die zentrale Einstellungsoberfläche erreichbar.

## Neuer Menüpfad

**Einstellungen → Geräteverwaltung**

## Einstellbar

- Schwellwert für automatische Zusammenführung
- Schwellwert für mögliche Dubletten
- Gewichtungen für MAC-Adresse, Hardware-Seriennummer, SNMP-Identität, Geräte-ID, Hostname, IP-Adresse, Hersteller und Gerätetyp
- Quellenboni ab zwei beziehungsweise drei übereinstimmenden Quellen
- automatische Zusammenführung aktiv/inaktiv
- harte Identitätskonflikte als Auto-Merge-Sperre
- IP-Adresse niemals als alleinige Merge-Grundlage

## Bedienung

- Speichern mit Validierung: Dublettenschwellwert muss unter dem Auto-Merge-Schwellwert liegen
- Wiederherstellung der Standardwerte
- unmittelbare Anzeige der aktuell resultierenden Entscheidungsbereiche
- Erfolgs- und Fehlermeldungen über GAM-Toasts
- Gerätemanager verweist auf die zentrale Konfiguration und enthält keine doppelte Einstellungsmaske mehr

## Prüfung

Der Frontend-Produktionsbuild wurde mit Vite erfolgreich durchgeführt. Das Build-Verzeichnis und `node_modules` sind aus dem Übergabearchiv entfernt.
