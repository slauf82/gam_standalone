#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"
echo "FIX33 TRACE FRONTEND ROOT | root=$ROOT_DIR | script=$0"
./scripts/ensure-node.sh || { echo; echo "[FEHLER] Node.js/npm konnte nicht automatisch eingerichtet werden."; exit 1; }
if [ -f "$ROOT_DIR/.gam-node-env" ]; then
  # Von ensure-node.sh erzeugter PATH fuer eine portable GAM-Node.js-Laufzeit.
  # shellcheck disable=SC1091
  source "$ROOT_DIR/.gam-node-env"
fi
command -v node >/dev/null 2>&1 || { echo "[FEHLER] Node.js ist nach der Einrichtung nicht im PATH."; exit 1; }
command -v npm >/dev/null 2>&1 || { echo "[FEHLER] npm ist nach der Einrichtung nicht im PATH."; exit 1; }
FIX33_FRONTEND_SOURCE="$ROOT_DIR/frontend/src/api/client.ts"
if ! grep -q "gamTrace=fix33" "$FIX33_FRONTEND_SOURCE"; then
  echo "[FEHLER] FIX33: Frontend-Quellcode-Marker fehlt: $FIX33_FRONTEND_SOURCE"
  exit 1
fi
echo "FIX33 FRONTEND SOURCE | marker=OK | source=$FIX33_FRONTEND_SOURCE"
cd "$ROOT_DIR/frontend"
# FIX41: Abhaengigkeiten ausserhalb des ueberschreibbaren GAM-Ordners halten.
CACHE_ROOT="${GAM_FRONTEND_CACHE_DIR:-$HOME/.gam-standalone/frontend-cache}"
mkdir -p "$CACHE_ROOT"
LOCK_HASH="$( (shasum -a 256 package-lock.json 2>/dev/null || sha256sum package-lock.json 2>/dev/null) | awk '{print $1}' )"
[ -n "$LOCK_HASH" ] || { echo "[FEHLER] package-lock.json fehlt."; exit 1; }
CACHE_MODULES="$CACHE_ROOT/$LOCK_HASH/node_modules"
if [ -d node_modules ] && [ ! -L node_modules ]; then
  if [ ! -d "$CACHE_MODULES" ]; then
    mkdir -p "$(dirname "$CACHE_MODULES")"
    mv node_modules "$CACHE_MODULES"
  else
    rm -rf node_modules
  fi
fi
if [ -L node_modules ]; then rm node_modules; fi
if [ -d "$CACHE_MODULES" ]; then
  ln -s "$CACHE_MODULES" node_modules
  echo "[GAM] Vorhandene Frontend-Abhaengigkeiten wiederverwendet."
else
  mkdir -p "$(dirname "$CACHE_MODULES")"
  echo "[GAM] Frontend-Abhaengigkeiten werden einmalig installiert..."
  npm ci
  mv node_modules "$CACHE_MODULES"
  ln -s "$CACHE_MODULES" node_modules
fi
echo "FIX33 VITE START | root=$(pwd)"
npm run dev
