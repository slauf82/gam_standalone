# 40k36t85 – PowerShell-Scope-Fix im Windows-SOFTWARE-Modul

Ursache des SYSTEM-Fehlers war die Verwendung von `$script:records += ...` und
`$script:sourceCounts += ...` innerhalb von `AddRegistryLocation`.

Im erhöhten One-Shot läuft das Modul in einem zusätzlichen `& { ... }`-Scope.
Dadurch zeigte `$script:` nicht auf die lokalen Sammler des SOFTWARE-Moduls.

Fix:
- `records` und `sourceCounts` sind nun explizite `System.Collections.ArrayList`.
- `AddRegistryLocation` erhält beide Sammler als Parameter.
- Einträge werden mit `.Add()` angefügt, nicht mehr mit `+=`.
- Auch HKLM verwendet dieselben mutable Collections.
- HKCU, HKU und temporär geladene NTUSER.DAT-Profile schreiben damit garantiert
  in dieselben Sammler – unabhängig vom zusätzlichen UAC/SYSTEM-Scope.
- Alle t84-Härtungen und Diagnosepfade bleiben erhalten.
