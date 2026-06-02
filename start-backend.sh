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
export SPRING_PROFILES_ACTIVE=local
./mvnw clean package
if [ $? -ne 0 ]; then
  echo
  echo "Build fehlgeschlagen. Backend wird nicht gestartet."
  exit 1
fi
./mvnw spring-boot:run
