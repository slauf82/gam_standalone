# 40k36t78 – Storage-Plattformangleichung

## Windows
- Laufwerke werden in lokale Laufwerke, Netzlaufwerke, optische Laufwerke und Wechseldatenträger getrennt.
- Netzlaufwerke erhalten nur dann Kapazitätswerte, wenn Windows echte Werte liefert; keine 8-EB-Ersatzwerte.
- BitLocker-Zeilen verwenden `C: – ...` statt `C:: ...`.
- Leere BitLocker-Felder werden nicht mehr ausgegeben.
- Die BitLocker-Zusammenfassung verwendet die tatsächlich ermittelten Verschlüsselungszähler.

## Plattformübergreifende Darstellung
Storage wird für die bestehenden vier Plattformen in dieselbe fachliche Struktur eingeordnet:
- Physische Datenträger
- Lokale Laufwerke & Volumes
- Netzlaufwerke
- Optische Laufwerke
- Wechseldatenträger
- Verschlüsselung

Linux-Daten wie LUKS/dm-crypt/LVM, macOS-Daten wie APFS/FileVault und Android-Speicherwerte werden in diese gemeinsame Struktur einsortiert. Nicht vorhandene Kategorien werden nicht angezeigt.

iOS/iPadOS bleibt bewusst für 40k37 außen vor.
