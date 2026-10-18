# 40k36t63

- manage-bde -status ist der bevorzugte BitLocker-Lesepfad.
- Pfadauflösung: Sysnative → System32 → PATH.
- Get-BitLockerVolume wird nur noch als Fallback verwendet.
- Dadurch blockiert ein defekter Storage-/BitLocker-CIM-Pfad den UAC-Lauf nicht mehr minutenlang.
- Bei BitLocker lesbar = Nein kann backend-lokal „BitLocker-WMI-Provider reparieren“ gewählt werden.
- GAM führt gezielt mofcomp.exe auf win32_encryptablevolume.mof aus.
- Danach wird root\CIMV2\Security\MicrosoftVolumeEncryption direkt verifiziert.
- Kein WMI-Repository-Reset, kein Dienst, keine dauerhafte Änderung außer der Provider-Registrierung.
