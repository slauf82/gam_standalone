#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
export GAM_ROOT="$(pwd)"
echo "GAM BUILD | 40k37e56c-fix32 | root=$GAM_ROOT"
echo "FIX33 TRACE BACKEND ROOT | pwd=$(pwd) | script=$0"
FIX33_SOURCE="backend/src/main/java/de/kopfzentrum/gam/inventory/InventoryController.java"
if ! grep -q "FIX33 LEGACY TEST ROUTE INTERCEPT" "$FIX33_SOURCE"; then
  echo "[FEHLER] fix33-Quellcode-Marker fehlt: $FIX33_SOURCE"
  echo "Das Backend wird nicht mit einem unvollstaendig aktualisierten Quellbaum gestartet."
  exit 1
fi
echo "GAM UPDATE CHECK | fix32 source marker=OK"
mkdir -p logs
if [ -f .env ]; then
  set -a
  # shellcheck disable=SC1091
  . ./.env
  set +a
fi
export SPRING_PROFILES_ACTIVE=local
export JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 ${JAVA_TOOL_OPTIONS:-}"
export LANG="${LANG:-C.UTF-8}"
export LC_ALL="${LC_ALL:-C.UTF-8}"

# 40k36q23: Java 21 ueber denselben Shell-Aktivierungsweg wie alle Backend-Skripte.
# shellcheck disable=SC1091
source ./scripts/activate-java.sh
# 40k33a4: Datenbank vor dem Spring-Boot-Start plattformgerecht einrichten.
if ! ./scripts/ensure-gam-database.sh; then
  echo
  echo "[FEHLER] MariaDB und die GAM-Datenbank konnten nicht eingerichtet werden."
  echo "Das Backend wird nicht ohne validierte Datenbank gestartet."
  exit 1
fi
# Das Datenbank-Setup kann beim Erststart lokale Zugangsdaten in .env erzeugen.
if [ -f .env ]; then
  set -a
  . ./.env
  set +a
fi

./mvnw clean package
if [ $? -ne 0 ]; then
  echo
  echo "Build fehlgeschlagen. Backend wird nicht gestartet."
  exit 1
fi
FIX33_CLASS="backend/target/classes/de/kopfzentrum/gam/inventory/InventoryController.class"
FIX33_SERVICE_CLASS="backend/target/classes/de/kopfzentrum/gam/inventory/AppleMobileDiscoveryService.class"
if [ ! -f "$FIX33_CLASS" ] || [ ! -f "$FIX33_SERVICE_CLASS" ]; then
  echo "[FEHLER] FIX33: kompilierte Apple-Klassen fehlen unter backend/target/classes."
  exit 1
fi
if ! grep -a -q "FIX33 LEGACY TEST ROUTE INTERCEPT" "$FIX33_CLASS"; then
  echo "[FEHLER] FIX33: InventoryController.class enthaelt den FIX33-Marker nicht."
  exit 1
fi
if ! grep -a -q "FIX33 PAIRING BRIDGE" "$FIX33_SERVICE_CLASS"; then
  echo "[FEHLER] FIX33: AppleMobileDiscoveryService.class enthaelt den FIX33-Marker nicht."
  exit 1
fi
echo "FIX33 COMPILED CLASSES | controller=OK | service=OK | target=$(pwd)/backend/target/classes"
echo "FIX33 SPRING START | mavenPom=$(pwd)/backend/pom.xml"
./mvnw spring-boot:run
