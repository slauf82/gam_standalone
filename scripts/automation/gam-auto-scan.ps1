param(
  [ValidateSet('DISCOVERY','INVENTORY','FULL')]
  [string]$Mode = 'FULL',
  [string]$BaseUrl = 'http://127.0.0.1:8080',
  [int]$TimeoutSeconds = 3600
)

$ErrorActionPreference = 'Stop'
$uri = ($BaseUrl.TrimEnd('/')) + '/api/inventory/automation/run'
$body = @{ mode = $Mode } | ConvertTo-Json -Compress
Write-Host "GAM Automationslauf: $Mode -> $uri"
try {
  $result = Invoke-RestMethod -Method Post -Uri $uri -ContentType 'application/json' -Body $body -TimeoutSec $TimeoutSeconds
  $result | ConvertTo-Json -Depth 8
  if (-not $result.success) { exit 2 }
  exit 0
} catch {
  Write-Error ("GAM Automationslauf fehlgeschlagen: " + $_.Exception.Message)
  exit 1
}
