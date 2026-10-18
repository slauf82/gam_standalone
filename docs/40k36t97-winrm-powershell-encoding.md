# 40k36t97 – robuste Windows-PowerShell-Dateidekodierung

t96 erreichte den echten SSPI-Dateiweg, scheiterte aber beim Java-Einlesen mit
`MalformedInputException: Input length = 1`.

t97 liest Launcher-stdout, Launcher-stderr und die zurückkopierte result.txt deshalb
nicht mehr blind als UTF-8.

Reihenfolge:
1. UTF-8 BOM erkennen
2. UTF-16LE BOM erkennen
3. UTF-16BE BOM erkennen
4. BOM-los streng als UTF-8 versuchen
5. bei ungültigem UTF-8 Windows-1252
6. letzter verlustfreier Ein-Byte-Fallback ISO-8859-1

Die t95/t96-Stufenmarker, der PS1-Launcher, SSPI-Routing und Legacy-NTLM für
Linux/macOS bleiben unverändert.
