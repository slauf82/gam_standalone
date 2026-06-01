$ErrorActionPreference = "Stop"
$base = $args[0]
if (-not $base) { $base = "http://localhost:8080" }
Write-Host "Pruefe GAM Backend: $base"
$endpoints = @(
  "/actuator/health",
  "/api/system/startup-check",
  "/api/system/status"
)
foreach ($ep in $endpoints) {
  Write-Host "`n== $ep =="
  try {
    Invoke-RestMethod -Uri "$base$ep" -Method Get | ConvertTo-Json -Depth 10
  } catch {
    Write-Host "FEHLER: $($_.Exception.Message)" -ForegroundColor Red
  }
}
