# 40k36t61 – BitLocker vor SYSTEM abschließen

- Primär bleibt `Get-BitLockerVolume`.
- Bei Fehlern wie `Ungültiger Namespace` wird automatisch `manage-bde -status` verwendet.
- Der Fallback wertet Volume, Konvertierungsstatus, Prozentwert, Methode, Schutzstatus, Sperrstatus und Auto-Unlock aus.
- `BITLOCKER_READABLE=Ja`, sobald einer der beiden Wege echte Daten liefert.
- `PERMISSION_BITLOCKER` wird ebenfalls über den `manage-bde`-Fallback auf Ja gesetzt.
- Der primäre Fehler bleibt als `BITLOCKER_PRIMARY_ERROR` erhalten.
- Erst wenn beide Wege scheitern, bleibt BitLocker als nicht lesbar markiert.
- UAC/Admin-Pfad bleibt unverändert.
