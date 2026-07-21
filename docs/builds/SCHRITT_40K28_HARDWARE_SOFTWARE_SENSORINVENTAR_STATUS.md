# Schritt 40k28 – Hardware-, Software- und Sensorinventarisierung

## Umgesetzt

- Registrierte Geräte besitzen in der gemeinsamen Geräteliste einen Aufklapppfeil.
- Die technische Detailansicht ist direkt unter dem jeweiligen Gerät eingebettet.
- Bereiche werden nur angezeigt, wenn tatsächlich Werte vorhanden sind.
- Unterstützte Darstellungsbereiche:
  - System und Betriebssystem
  - CPU, Mainboard, RAM-Ausbau und maximaler RAM
  - Grafikkarte
  - Laufwerke, SMART-Zustand und SSD-/NVMe-Temperatur
  - CPU-, Mainboard-, Chipsatz- und VRM-Temperaturen
  - Lüfter, Spannungen und Leistungsaufnahme
  - Netzwerk und Geräteidentitäten
  - installierte Software aus der bestehenden GAM-Softwarezuordnung
- Für Home Assistant und weitere Quellen ist die Ansicht quellenneutral vorbereitet: gelieferte technische Werte erscheinen am zugehörigen Gerät, leere Bereiche bleiben verborgen.
- Altgeräte und neu registrierte Geräte verbleiben dauerhaft in derselben gemeinsamen Struktur.

## Datenquellen

Die Oberfläche ist für WMI/LibreHardwareMonitor, Linux hwmon/lm-sensors, smartctl, Home Assistant, SNMP und Hersteller-APIs vorbereitet. 40k28 erfindet keine Werte; angezeigt werden nur vorhandene oder von einer Quelle gelieferte Daten.
