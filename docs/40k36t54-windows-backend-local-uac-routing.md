# 40k36t54 – Windows Backend-lokal → UAC

- Frontend entscheidet nicht mehr per Identity-/Protocol-Heuristik, ob ein Windows-Gerät backend-lokal ist.
- Neue Backend-Abfrage `/windows/backend-local?identityKey=...` verwendet dieselbe robuste `isCurrentWindowsHost(...)`-Logik wie der Dispatcher.
- Backend-lokal: „Mit erhöhten Rechten neu inventarisieren“ startet direkt den nativen UAC-Pfad, ohne Benutzername/Kennwort-Dialog.
- Remote: bestehender WinRM-Credential-Dialog bleibt unverändert.
- t53 Encoding-Fix und der UAC-Launcher selbst bleiben unangetastet.
