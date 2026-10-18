# 40k36t47 – WinRM ExecutionPolicy Bypass

- Basis: 40k36t46 Import-Fix.
- Das per WinRM übertragene temporäre `.ps1` wird nicht mehr mit `& $p` direkt ausgeführt.
- Stattdessen startet GAM einen separaten PowerShell-Prozess mit:
  `powershell.exe -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File <temp.ps1>`
- Die Windows-Execution-Policy wird dadurch nicht dauerhaft geändert; der Bypass gilt nur für diesen Prozess.
- Fehler-/Standardausgabe wird weiterhin vollständig in die Ergebnisdatei umgeleitet und über den bestehenden blockweisen Rückkanal abgeholt.
- Trust-Persistenz und Transportlogik aus t46 bleiben unverändert.
