# 40k36t787 – Software-Abschluss und WinRM-Angleichung

## Lokaler Administrator-/SYSTEM-Lauf
- Ein SOFTWARE-Modul gilt nicht mehr als komplett fehlgeschlagen, wenn Registry-Basis,
  `SOFTWARE_DETAILS`, `SOFTWARE_COUNT` und `SOFTWARE_CAPTURED_COUNT` vollständig vorliegen
  und nur ein optionaler später Nachlauf einen Fehler meldet.
- Der optionale Teilfehler bleibt als Warnung/Diagnose sichtbar, löscht aber nicht mehr
  die bereits gültige Softwareinventarisierung.

## WinRM
`WindowsRemoteInventoryService` verwendet bereits
`WindowsInventoryDiscoveryService.remoteFullInventoryScript()` und damit dasselbe
`softwareScript()` wie die lokale Inventarisierung. t787 gleicht nun auch die Darstellung
und Diagnosefelder vollständig an:
- HKLM/HKCU/HKU/NTUSER.DAT je nach Remote-Rechten
- Store/AppX/MSIX
- `Get-AppxPackage -AllUsers` bei ausreichenden Rechten
- Provisioned Apps
- Benutzerumfang, Profil-Hive-Diagnose, AppX-Lesbarkeit und AppX-Diagnose
- strukturierte Ablage weiterhin über `replaceWindowsFromDetails(...)`

Der erreichbare Umfang bleibt bewusst rechteabhängig: Ein nicht administrativer WinRM-
Zugang kann nicht dieselbe Tiefe liefern wie ein administrativer Zugang.
