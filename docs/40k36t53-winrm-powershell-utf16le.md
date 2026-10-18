# 40k36t53 – WinRM PowerShell UTF-16LE

Gezielter Encoding-Fix auf Basis von 40k36t52.

## Änderung
Der per WinRM hochgeladene Inventarskript-Payload bleibt UTF-8/Base64.
Beim Rekonstruieren auf dem Windows-Ziel wird er jetzt explizit:

1. aus Base64 in UTF-8-Bytes dekodiert,
2. mit `Encoding.UTF8.GetString(...)` in Text umgewandelt,
3. mit `Encoding.Unicode` als UTF-16LE `.ps1` gespeichert.

Damit verarbeitet Windows PowerShell 5.1 auch deutsche Umlaute und Zeichen wie `·`
robust, unabhängig von ANSI-Codepage/BOM-Heuristiken.

UAC, Authentifizierung, Chunking, Rücktransport, Mapping und Persistenz bleiben unverändert.
