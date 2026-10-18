#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
export GAM_ROOT="$ROOT_DIR"
cd "$ROOT_DIR"
# shellcheck disable=SC1091
source "$ROOT_DIR/scripts/activate-java.sh"
export JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 ${JAVA_TOOL_OPTIONS:-}"
echo "[GAM] Baue Backend mit Java aus $JAVA_HOME"
./mvnw clean package -DskipTests
