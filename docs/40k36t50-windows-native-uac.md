# 40k36t50

Backend-lokale erhöhte Windows-Inventarisierung nutzt native UAC via `Start-Process -Verb RunAs`. GAM fragt lokal kein Admin-Kennwort ab. Remote-Windows behält den WinRM-Credential-Pfad. SYSTEM bleibt separat.
