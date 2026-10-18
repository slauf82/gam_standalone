# 40k36t64 – BitLocker-Hänger vermeiden

- Der direkte `Get-BitLockerVolume`-Aufruf im SECURITY-Modul wurde entfernt.
- BitLocker wird zentral im STORAGE-Modul erfasst.
- `manage-bde -status` bleibt der bevorzugte Lesepfad.
- Nur wenn `manage-bde` keine verwertbaren Daten liefert, wird `Get-BitLockerVolume` noch als Fallback versucht.
- Dieser Fallback läuft isoliert in einem PowerShell-Job mit 10-Sekunden-Timeout.
- Dasselbe gilt für die reine Berechtigungsprüfung.
- Dadurch kann ein defekter `MSFT_Volume`-/Storage-CIM-Pfad den kompletten 300-Sekunden-UAC-Lauf nicht mehr blockieren.
- Admin-UAC, SYSTEM-One-Shot-Task, WinRM und die t63-Provider-Reparatur bleiben unverändert.
