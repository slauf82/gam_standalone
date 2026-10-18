# 40k36r3 – macOS-Hardwareanalyse vorgezogen

Dieser Ausbau zieht ausschließlich den macOS-Anteil der später vorgesehenen Hardwareanalyse vor, damit der aktuell verfügbare Test-Mac vollständig geprüft werden kann. Die allgemeine Roadmap bleibt unverändert.

## Neue Hardwaredaten
- SMART-Basisstatus über `diskutil`
- optionale tiefe SMART/NVMe-Werte über vorhandenes `smartctl`
  - Gesundheit
  - Temperatur
  - Betriebsstunden
  - Percentage Used
  - Media/Data Integrity Errors
  - Critical Warning
  - Power Cycles
  - Unsafe Shutdowns
  - Data Units Read/Written
- physische Datenträger mit Protokoll und intern/extern
- RAM-Module, Typ, Geschwindigkeit, Hersteller/Teilenummer/Seriennummer soweit macOS liefert
- ECC-Hinweis und Hinweis auf separat gemeldete DIMM-Module
- GPU und Display
- USB-Geräte
- Thunderbolt
- PCI
- Netzwerkhardware
- Lüfteranzahl soweit IOKit sie meldet
- zusätzliche macOS-Thermik-Level soweit das System sie bereitstellt

## smartctl
Die Inventarisierung installiert bewusst nichts automatisch. Ist `smartctl` bereits vorhanden, nutzt GAM die tieferen Werte. Fehlt es, bleiben die nativen `diskutil`-SMART-Werte verfügbar und `SMART-Werkzeug smartctl = nein` zeigt transparent, warum keine tieferen Attribute vorliegen. So verändert eine reine Inventarisierung den Mac nicht.
