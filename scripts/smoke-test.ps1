param(
  [string]$BaseUrl = "http://localhost:8080",
  [string]$Token = ""
)

$ErrorActionPreference = "Continue"

function Test-Endpoint {
  param(
    [string]$Name,
    [string]$Path,
    [string]$Method = "GET",
    [bool]$Auth = $false
  )

  $headers = @{}
  if ($Auth -and $Token -ne "") {
    $headers["Authorization"] = "Bearer $Token"
  }

  try {
    $response = Invoke-WebRequest -Uri "$BaseUrl$Path" -Method $Method -Headers $headers -UseBasicParsing -TimeoutSec 20
    Write-Host "[OK] $Name -> $($response.StatusCode) $Path"
  } catch {
    $status = "ERR"
    if ($_.Exception.Response -ne $null) {
      $status = [int]$_.Exception.Response.StatusCode
    }
    Write-Host "[WARN] $Name -> $status $Path"
  }
}

Write-Host "GAM 2.0 Smoke-Test gegen $BaseUrl"
Write-Host "----------------------------------------"

Test-Endpoint "Actuator Health" "/actuator/health"
Test-Endpoint "System Status" "/api/system/status"
Test-Endpoint "Startup Check" "/api/system/startup-check"
Test-Endpoint "Security Status" "/api/security/status" -Auth $true
Test-Endpoint "Module" "/api/modules" -Auth $true
Test-Endpoint "Invoices" "/api/invoices" -Auth $true
Test-Endpoint "Invoice Number Preview" "/api/invoices/number-preview" -Auth $true
Test-Endpoint "LBD Preview" "/api/invoices/lbd/preview" -Auth $true
Test-Endpoint "Inventory Devices" "/api/inventory/devices" -Auth $true
Test-Endpoint "Warehouse Items" "/api/warehouse/items" -Auth $true
Test-Endpoint "Workflow Tasks" "/api/workflow/tasks" -Auth $true

Write-Host "----------------------------------------"
Write-Host "Hinweis: 401/403 ist ohne Token bei geschützten APIs normal. 500 sollte geprüft werden."
