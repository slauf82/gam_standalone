# 40k36t79 – Storage-Finalisierung

- Ein frischer Windows-Inventarlauf ersetzt nun sämtliche alten Storage- und BitLocker-Detailfelder im Discovery-Merge. Alte t75/t77-Fragmente können den neuen t78-Stand nicht mehr überlagern.
- Lokale Laufwerke werden aus `Win32_LogicalDisk` gelesen und mit `Get-Volume` ergänzt, falls dort weitere feste Laufwerksbuchstaben vorhanden sind.
- Netzlaufwerke werden separat ausgegeben; potentiell synthetische Kapazitätswerte (z. B. 8 EB) werden bewusst nicht als belastbare Größe übernommen.
- Optische Laufwerke und Wechseldatenträger bleiben eigene Kategorien.
- WinRM verwendet dieselben neuen Kategorien wie die lokale Windows-Inventarisierung.
- Die in t78 korrigierte BitLocker-Ausgabe (`C: – ...`, keine leeren Felder, echte Zähler) wird durch das Entfernen alter Merge-Werte nun zuverlässig sichtbar.
