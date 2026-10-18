# 40k36t94 – SSPI-Datei-Transfer für WinRM-Vollinventar

Problem:
- Windows-native SSPI authentifiziert das lokale Konto mit Leerzeichen erfolgreich.
- `Invoke-Command`-stdout kommt in dieser Umgebung jedoch leer bei Java an.
- Legacy winrm4j/CXF scheitert für dieses Konto bereits bei NTLM mit Authorization Loop.

Lösung:
1. SSPI-Preflight bleibt unverändert.
2. Für das Vollinventar wird eine echte `PSSession` aufgebaut.
3. Das aktuelle GAM-Inventarskript wird mit `Copy-Item -ToSession` in ein Remote-TEMP-Verzeichnis übertragen.
4. Es läuft remote und schreibt seine komplette Ausgabe in `result.txt`.
5. `Copy-Item -FromSession` kopiert diese Datei direkt auf den GAM-Rechner zurück.
6. Java liest die lokale Ergebnisdatei direkt – kein Remoting-stdout für Nutzdaten.
7. Remote-TEMP und lokale Tempdateien werden anschließend bereinigt.
8. Legacy-NTLM-Chunk bleibt nur noch letzte Rückfallebene.

Damit bleiben Authentifizierung und Dateitransport vollständig im Windows-nativen SSPI/PSSession-Pfad.
