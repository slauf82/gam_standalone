# 40k36t81 – Windows Store/AppX/MSIX

- Klassische Registry-/Win32-Software bleibt unverändert gezählt.
- `Get-AppxPackage` inventarisiert Apps des aktuellen Windows-Benutzers.
- `Get-AppxProvisionedPackage -Online` inventarisiert provisionierte/systemweite Apps, soweit im aktuellen Rechtekontext lesbar.
- Beide Quellen werden in dieselbe strukturierte Softwaretabelle gespeichert, aber mit eigenen Quellenkennungen.
- UI: eigener Bereich `Installierte Apps` mit `Benutzer-Apps` und `System-/Provisioned-Apps`.
- Android-App-Bereiche aus t80 bleiben ausschließlich Android-spezifisch.
