#!/usr/bin/env bash
set -euo pipefail
BASE="${1:-http://localhost:8080}"
for ep in /actuator/health /api/system/startup-check /api/system/status; do
  echo
  echo "== ${ep} =="
  curl -fsS "${BASE}${ep}" || true
  echo
 done
