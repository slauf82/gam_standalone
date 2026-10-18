# 40k36t60 – Admin-BitLocker und Vollständigkeitsdiagnose

Auf Basis des funktionierenden t59-UAC-Pfads.

- Der UAC-/Admin-Launcher selbst bleibt unverändert.
- BitLocker-Lesbarkeit wird nicht mehr ausschließlich über `Get-BitLockerVolume` beurteilt.
- Falls das Cmdlet auf einer Windows-Edition fehlt, prüft GAM als Fallback `manage-bde -status`.
- Neue Diagnosefelder:
  - `PERMISSION_BITLOCKER_METHOD`
  - `PERMISSION_BITLOCKER_ERROR`
- Die Einschränkung enthält bei Fehlschlag jetzt die konkrete Ursache.
- Nach dem gemeinsamen UAC-Lauf protokolliert GAM erfolgreiche/fehlgeschlagene Module sowie die Zahl der übernommenen Felder.
- Damit lässt sich vor der SYSTEM-Stufe sauber unterscheiden: fehlende Rechte vs. fehlendes Cmdlet/Windows-Edition vs. fehlerhaftes Teilmodul.
