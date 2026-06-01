#!/usr/bin/env sh
set -e
BASEDIR=$(cd "$(dirname "$0")" && pwd)
if command -v mvn >/dev/null 2>&1; then
  exec mvn -f "$BASEDIR/backend/pom.xml" "$@"
fi
MVN_DIR="$BASEDIR/.mvn/apache-maven-3.9.11"
MVN_ZIP="$BASEDIR/.mvn/apache-maven-3.9.11-bin.zip"
if [ ! -x "$MVN_DIR/bin/mvn" ]; then
  echo "Maven wurde nicht gefunden. Versuche Maven 3.9.11 lokal in .mvn/ zu laden..."
  if command -v curl >/dev/null 2>&1; then
    curl -L -o "$MVN_ZIP" "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.11/apache-maven-3.9.11-bin.zip"
  elif command -v wget >/dev/null 2>&1; then
    wget -O "$MVN_ZIP" "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.11/apache-maven-3.9.11-bin.zip"
  else
    echo "Bitte Maven installieren oder apache-maven-3.9.11 nach .mvn/ entpacken." >&2
    exit 1
  fi
  unzip -q "$MVN_ZIP" -d "$BASEDIR/.mvn"
fi
exec "$MVN_DIR/bin/mvn" -f "$BASEDIR/backend/pom.xml" "$@"
