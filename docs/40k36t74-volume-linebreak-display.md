# 40k36t74 – saubere Zeilenumbrüche bei Laufwerks-/Volumeangaben

Reine Darstellungsanpassung vor Preview 5.

- Semikolon-getrennte Laufwerks- und Volumeangaben werden in der Detailansicht konsequent als einzelne Zeilen/Listeneinträge dargestellt.
- Ergänzt wurden insbesondere `Volumedetails`, `Volume-Details`, `BitLocker-Volumes` und `Physische Datenträger`.
- Dadurch beginnt der nächste Laufwerksdatensatz in einer neuen Zeile, statt dass Browser-Wrapping z. B. `... 8 GB frei; D ...` optisch auseinanderreißt.
- Inventarisierungswerte selbst werden nicht verändert.
- Die von Windows gemeldeten Größen von Netzlaufwerken bleiben unverändert erhalten.
- SYSTEM-, BitLocker-, Discovery-, Merge- und Plattformlogik aus t73 bleibt unverändert.
