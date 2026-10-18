#!/usr/bin/env sh
set -eu
MODE="${1:-FULL}"
BASE_URL="${GAM_BASE_URL:-http://127.0.0.1:8080}"
case "$MODE" in DISCOVERY|INVENTORY|FULL) ;; *) echo "Mode: DISCOVERY, INVENTORY oder FULL" >&2; exit 2;; esac
curl --fail --silent --show-error --max-time 3600 -H 'Content-Type: application/json' -d "{\"mode\":\"$MODE\"}" "$BASE_URL/api/inventory/automation/run"
printf '\n'
