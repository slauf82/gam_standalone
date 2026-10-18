param(
  [ValidateSet('DISCOVERY','INVENTORY','FULL')]
  [string]$Mode = 'FULL',
  [string]$DailyAt = '02:00',
  [string]$TaskName = 'GAM 2.0 automatische Geräteinventarisierung'
)

$ErrorActionPreference = 'Stop'
$runner = Join-Path $PSScriptRoot 'gam-auto-scan.ps1'
if (-not (Test-Path $runner)) { throw "Runner nicht gefunden: $runner" }
if ($DailyAt -notmatch '^([01]\d|2[0-3]):[0-5]\d$') { throw 'DailyAt muss HH:mm sein, z. B. 02:00.' }

$escapedRunner = $runner.Replace('"','\"')
$action = "powershell.exe -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File `"$runner`" -Mode $Mode"
Write-Host "Erzeuge Windows-Aufgabe '$TaskName' täglich um $DailyAt als SYSTEM."
& schtasks.exe /Create /TN $TaskName /SC DAILY /ST $DailyAt /TR $action /RU SYSTEM /RL HIGHEST /F
if ($LASTEXITCODE -ne 0) { throw "schtasks.exe meldete Fehlercode $LASTEXITCODE. Bitte PowerShell als Administrator ausführen." }
Write-Host 'Aufgabe wurde eingerichtet.'
Write-Host "Test: schtasks.exe /Run /TN `"$TaskName`""
