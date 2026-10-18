# 40k36t125 – WinRM-Verbindungstest: SSPI vor Legacy

- Der WinRM-Verbindungstest verwendet jetzt dieselbe Authentifizierungsreihenfolge wie die Inventarisierung: MetricsHub modern -> Windows-native SSPI Current Context -> Legacy winrm4j/CXF.
- Sobald SSPI erfolgreich ist, wird Legacy nicht mehr getestet und die Oberfläche kann den tatsaechlich bevorzugten SSPI-Weg anzeigen.
- Auf Nicht-Windows-Backends wird der SSPI-Schritt automatisch uebersprungen; Legacy bleibt unveraendert verfuegbar.
- Keine Aenderung an Inventarisierungslogik, Legacy-Timeouts oder Local-vor-Remote-Prioritaet.
