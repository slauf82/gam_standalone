# 40k36t40 – Legacy WinRM: Chunked PowerShell + ruhigeres Log

- Basis: 40k36t39.
- Behebt den bestätigten Fehler `Status 1: Die Befehlszeile ist zu lang.`.
- Das unveränderte PowerShell-Inventarskript wird Base64-kodiert, in 3000-Zeichen-Chunks nach `%TEMP%` übertragen, als `.ps1` rekonstruiert, ausgeführt und im `finally` gelöscht.
- Bei HTTP + IP wird nach fehlgeschlagenem MetricsHub direkt Legacy-NTLM verwendet. Legacy-Negotiate/SPNEGO wird in diesem Workgroup-/Nicht-Kerberos-Fall übersprungen, um den unnötigen CXF/Kerberos-Authorization-Loop-Stacktrace zu vermeiden.
- HTTPS/Hostname behält den bisherigen Negotiate/SPNEGO-Pfad.
- Zugangsdaten, Profilrouting und Inventarisierungsinhalt bleiben unverändert.
