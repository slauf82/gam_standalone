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

# Preview 2: Java 21 automatisch pruefen und bei Bedarf lokal einrichten.
if ! ./scripts/ensure-java.sh; then
  echo
  echo "[FEHLER] Java 21 konnte nicht eingerichtet werden."
  echo "Fuer die erstmalige Einrichtung ist ein Internetzugriff erforderlich."
  exit 1
fi
if [ -f "$PWD/runtime/java-home.txt" ]; then
  export JAVA_HOME="$(head -n 1 "$PWD/runtime/java-home.txt" | tr -d '\r\n')"
  if [ -x "$JAVA_HOME/bin/java" ] && [ -x "$JAVA_HOME/bin/javac" ]; then
    export PATH="$JAVA_HOME/bin:$PATH"
    echo "[GAM] Verwendetes JDK: $JAVA_HOME"
  fi
elif [ -x "$PWD/runtime/java/bin/java" ]; then
  export JAVA_HOME="$PWD/runtime/java"
  export PATH="$JAVA_HOME/bin:$PATH"
fi
./mvnw clean package
if [ $? -ne 0 ]; then
  echo
  echo "Build fehlgeschlagen. Backend wird nicht gestartet."
  exit 1
fi
./mvnw spring-boot:run
