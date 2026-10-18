# 40k36t57 – UAC Launcher-Quoting

Fix auf Basis von 40k36t56.

Ursache:
Der generierte PowerShell-Launcher enthielt für den Wrapper-Pfad versehentlich
`+w+` als Literal innerhalb der Argumentliste. PowerShell brach deshalb bereits
beim Parsen ab; der UAC-Wrapper wurde nie gestartet.

Fix:
- Wrapper-Pfad wird zuerst in `$wrapper='C:\...\elevated-wrapper.ps1'` gesetzt.
- `ArgumentList` wird als echtes PowerShell-Array aufgebaut:
  `@('-NoLogo','-NoProfile','-NonInteractive','-ExecutionPolicy','Bypass','-File',$wrapper)`
- `Start-Process ... -Verb RunAs` erhält dieses Array direkt.

Die t56-Diagnose bleibt vollständig erhalten.
