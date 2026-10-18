# 40k36t86 – PowerShell-Typkonvertierung im SOFTWARE-Modul

- Numerische Werte werden nicht mehr linksseitig mit Text über `+` verknüpft.
- Diagnosewerte für Profil-Hives und Registry-Basis verwenden jetzt den PowerShell-Formatoperator `-f`.
- Auch Source-Counts und AppX-Fehlertexte sind auf explizite Stringformatierung umgestellt.
- Dadurch kann PowerShell keine Texte mehr als `System.Int32` interpretieren.
- Der t85-ArrayList-/Scope-Fix bleibt vollständig erhalten.
