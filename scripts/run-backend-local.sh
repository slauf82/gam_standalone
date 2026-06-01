#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
