# 40k36t82 – Windows: benutzerübergreifende Software- und App-Inventarisierung

## Klassische Anwendungen
- HKLM 64/32 Bit: systemweite Software.
- HKCU 64/32 Bit: aktueller Ausführungsbenutzer.
- Admin/SYSTEM: zusätzlich alle bereits geladenen echten Benutzer-Hives unter HKU.
- Admin/SYSTEM: nicht geladene lokale Profile werden über `Win32_UserProfile` gefunden und deren `NTUSER.DAT` temporär mit `reg.exe load` eingebunden.
- Bereits geladene Hives und der aktuelle HKCU-Hive werden nicht doppelt verarbeitet.
- Nur von GAM selbst geladene temporäre Hives werden anschließend wieder entladen.
- SID wird soweit möglich in `DOMAIN\Benutzer` aufgelöst.

## Store / AppX / MSIX
- Standardlauf: `Get-AppxPackage` für den aktuellen Benutzer.
- Admin/SYSTEM: `Get-AppxPackage -AllUsers`, mit Benutzerzuordnung über `PackageUserInformation`, soweit Windows sie liefert.
- `Get-AppxProvisionedPackage -Online`: systemweit provisionierte Apps für neue Benutzer.
- Deduplizierung erfolgt pro Benutzer/Scope/Paket, damit dieselbe App bei mehreren Benutzern nicht fälschlich zu einem Eintrag zusammenfällt.

## Transparenz
Das Protokoll weist aus, ob nur der aktuelle Benutzer oder alle erreichbaren Benutzerprofile geprüft wurden und wie viele zusätzliche NTUSER.DAT-Hives geladen bzw. nicht geladen werden konnten.
