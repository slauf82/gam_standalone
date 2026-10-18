# 40k36q9 – macOS Software, Updates und Status

## Änderungen
- Der obere macOS-Verbindungsstatus wird mit dem vorhandenen macOS-Testreport synchronisiert.
- `Testreport Verbindung = OK` gilt auch nach einem Seiten-/Detailreload als geprüfte SSH-Verbindung.
- Nach normaler und erhöhter macOS-Inventarisierung wird der sichtbare SSH-Status sofort aktualisiert.
- Installierte macOS-Software wird nicht mehr nur über die lange Discovery-Protokollzeichenkette dargestellt.
- Neue plattformneutrale Tabelle `gam_discovery_software_inventory` speichert Software strukturiert je Geräteidentität.
- Felder: Plattform, Scope (SYSTEM/USER), Benutzer, Name, Version, Bundle/Paket, Pfad, Quelle.
- Der aktuelle macOS-Softwarebestand wird nach jeder erfolgreichen Inventarisierung vollständig ersetzt/aktualisiert.
- Die Oberfläche lädt den strukturierten Softwarebestand über einen eigenen API-Endpunkt.
- macOS-Darstellung:
  - Software
    - Installierte Software
      - Systemsoftware & systemweit installiert
      - Benutzer
        - je Benutzer eigener Unterbereich
    - Softwareupdates
- `softwareupdate -l` liefert neben der Anzahl nun soweit verfügbar auch die Update-Namen.
- macOS-Updates werden nicht mehr fachlich dem generischen Paketmanager zugeordnet.
