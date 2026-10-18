# 40k36t52 – WinRM/Windows PowerShell UTF-8-Fix

Gezielter Encoding-Fix auf Basis von 40k36t51.

## Ursache
Das per WinRM übertragene Inventarskript wurde als UTF-8 ohne BOM rekonstruiert.
Windows PowerShell 5.1 interpretiert `.ps1`-Dateien ohne BOM nicht zuverlässig als
UTF-8. Literale Zeichen im Skript wie `Datenträger`, `verschlüsselt` oder `·`
wurden dadurch schon beim Parsen zu `DatentrÃ¤ger`, `verschlÃ¼sselt` bzw. `Â·`.

## Fix
Beim Rekonstruieren der temporären `.ps1` werden vor die unveränderten UTF-8-
Payloadbytes explizit die BOM-Bytes `EF BB BF` gesetzt. Damit erkennt Windows
PowerShell 5.1 die Datei als UTF-8.

Der Rücktransport (UTF-8-Ergebnisdatei → Byte-Base64 → Java UTF-8), WinRM-Auth,
Chunking, Mapping, Persistenz und die UAC-Logik bleiben unverändert.
