# 40k36t83 – Windows AppX/MSIX Abschluss

- AppX/MSIX-AllUsers-Erfassung für Administrator/SYSTEM bleibt getrennt von der klassischen Registry-Software.
- `PackageUserInformation.UserSecurityId` wird versionsrobust per `ToString()` gelesen und soweit möglich in Kontonamen aufgelöst.
- Provisioned Apps bleiben separat.
- Neue Diagnosefelder zeigen unmittelbar, ob `Get-AppxPackage -AllUsers` und `Get-AppxProvisionedPackage -Online` tatsächlich lesbar waren.
- Fehler werden als AppX/MSIX-Diagnose sichtbar statt still verschluckt.
- Die UI zeigt die Anzahl der ermittelten Benutzerzuordnungen.
- HKLM/HKCU/HKU/NTUSER.DAT aus t82 bleiben unverändert.
