# 40k36t77
- UTF-8-Mojibake in Inventarisierungsdetails wird in Backend und Frontend normalisiert.
- Der korrekte Kapazitäts-/Laufwerksblock wird in `Datenträger & Verschlüsselung` zusammengeführt.
- Alte Fragmentfelder wie `8 GB frei; D -> NTFS` werden aus `Weitere Informationen` ausgefiltert.
- Ein `Hardware-ID`-Feld wird nur dann dem Storage-Bereich zugeordnet, wenn es tatsächlich Kapazitätsangaben enthält.
- t76-Concurrency-, SYSTEM-, Admin- und BitLocker-Logik bleibt unverändert.
