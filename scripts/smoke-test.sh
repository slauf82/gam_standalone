#!/usr/bin/env bash
set +e
BASE_URL="${1:-http://localhost:8080}"
TOKEN="${GAM_TOKEN:-}"

call_endpoint() {
  local name="$1"
  local path="$2"
  local auth="$3"
  local headers=()
  if [[ "$auth" == "auth" && -n "$TOKEN" ]]; then
    headers=(-H "Authorization: Bearer $TOKEN")
  fi
  code=$(curl -s -o /tmp/gam_smoke_body.txt -w "%{http_code}" --max-time 20 "${headers[@]}" "$BASE_URL$path")
  if [[ "$code" == "200" || "$code" == "204" ]]; then
    echo "[OK]   $name -> $code $path"
  else
    echo "[WARN] $name -> $code $path"
  fi
}

echo "GAM 2.0 Smoke-Test gegen $BASE_URL"
echo "----------------------------------------"
call_endpoint "Actuator Health" "/actuator/health"
call_endpoint "System Status" "/api/system/status"
call_endpoint "Startup Check" "/api/system/startup-check"
call_endpoint "Security Status" "/api/security/status" auth
call_endpoint "Module" "/api/modules" auth
call_endpoint "Invoices" "/api/invoices" auth
call_endpoint "Invoice Number Preview" "/api/invoices/number-preview" auth
call_endpoint "LBD Preview" "/api/invoices/lbd/preview" auth
call_endpoint "Inventory Devices" "/api/inventory/devices" auth
call_endpoint "Warehouse Items" "/api/warehouse/items" auth
call_endpoint "Workflow Tasks" "/api/workflow/tasks" auth
echo "----------------------------------------"
echo "Hinweis: 401/403 ist ohne Token bei geschützten APIs normal. 500 sollte geprüft werden."
