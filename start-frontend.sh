#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/frontend"
if ! command -v npm >/dev/null 2>&1; then
  echo "Node.js/npm wurde nicht gefunden."
  exit 1
fi
[ -d node_modules ] || npm install
npm run dev
