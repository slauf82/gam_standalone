#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p logs
if [ -f .env ]; then
  set -a
  # shellcheck disable=SC1091
  . ./.env
  set +a
fi
if ! command -v mvn >/dev/null 2>&1; then
  echo "Maven wurde nicht gefunden. Bitte Maven installieren oder Backend-JAR vorher bauen."
  exit 1
fi
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
