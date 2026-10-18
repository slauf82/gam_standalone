$ErrorActionPreference = "Stop"
function Node-Ready {
  try {
    $node = (Get-Command node -ErrorAction Stop).Source
    $npm = (Get-Command npm.cmd -ErrorAction SilentlyContinue)
    if (-not $npm) { $npm = Get-Command npm -ErrorAction Stop }
    $v = (& $node --version 2>$null)
    $major = [int](($v -replace '^v','').Split('.')[0])
    return ($major -ge 18)
  } catch { return $false }
}
# FIX41: GUI/alte Shell kann einen veralteten PATH haben.
$nodeDirs = @(
  "$env:ProgramFiles\nodejs",
  "${env:ProgramFiles(x86)}\nodejs",
  "$env:APPDATA\npm",
  "$env:LOCALAPPDATA\Programs\nodejs",
  "$env:LOCALAPPDATA\Volta\bin"
)
foreach ($dir in $nodeDirs) {
  if ($dir -and (Test-Path (Join-Path $dir 'node.exe')) -and (Test-Path (Join-Path $dir 'npm.cmd'))) {
    $env:Path = "$dir;$env:Path"
    break
  }
}
$env:Path = [Environment]::GetEnvironmentVariable('Path','Machine') + ';' + [Environment]::GetEnvironmentVariable('Path','User') + ';' + $env:Path
if (Node-Ready) {
  Write-Host "[GAM] Node.js $(& node --version) / npm $(& npm --version) gefunden."
  exit 0
}
Write-Host "[GAM] Node.js/npm fehlt oder ist zu alt. Automatische Installation wird vorbereitet..."
$winget = Get-Command winget -ErrorAction SilentlyContinue
if (-not $winget) {
  Write-Error "winget wurde nicht gefunden. Node.js 18+ kann nicht automatisch installiert werden."
  exit 1
}
& winget install --id OpenJS.NodeJS.LTS --exact --silent --accept-package-agreements --accept-source-agreements
if ($LASTEXITCODE -ne 0) {
  Write-Error "Node.js LTS konnte mit winget nicht installiert werden."
  exit 1
}
# Current process PATH may not refresh automatically.
$env:Path = [Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [Environment]::GetEnvironmentVariable("Path","User")
if (-not (Node-Ready)) {
  Write-Error "Node.js/npm wurde installiert, ist in diesem Prozess aber noch nicht erreichbar."
  exit 1
}
Write-Host "[GAM] Node.js $(& node --version) / npm $(& npm --version) ist einsatzbereit."
