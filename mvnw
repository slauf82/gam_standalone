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

  # Auf einer frischen Linux-Installation existiert .mvn noch nicht.
  # curl/wget koennen die Zieldatei nur anlegen, wenn das Verzeichnis vorher erstellt wurde.
  mkdir -p "$BASEDIR/.mvn"
  rm -f "$MVN_ZIP"

  if command -v curl >/dev/null 2>&1; then
    curl -fL -o "$MVN_ZIP" "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.11/apache-maven-3.9.11-bin.zip"
  elif command -v wget >/dev/null 2>&1; then
    wget -O "$MVN_ZIP" "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.11/apache-maven-3.9.11-bin.zip"
  else
    echo "Bitte Maven installieren oder apache-maven-3.9.11 nach .mvn/ entpacken." >&2
    exit 1
  fi

  if [ ! -s "$MVN_ZIP" ]; then
    echo "[FEHLER] Maven-Archiv wurde nicht oder leer heruntergeladen: $MVN_ZIP" >&2
    exit 1
  fi
  if ! command -v unzip >/dev/null 2>&1; then
    echo "[FEHLER] Zum Entpacken von Maven wird 'unzip' benoetigt." >&2
    exit 1
  fi

  echo "[GAM] Entpacke Maven 3.9.11 nach $BASEDIR/.mvn ..."
  unzip -q "$MVN_ZIP" -d "$BASEDIR/.mvn"
  rm -f "$MVN_ZIP"

  if [ ! -x "$MVN_DIR/bin/mvn" ]; then
    echo "[FEHLER] Maven wurde entpackt, aber nicht unter $MVN_DIR/bin/mvn gefunden." >&2
    exit 1
  fi
  echo "[GAM] Maven 3.9.11 wurde lokal eingerichtet."
fi
exec "$MVN_DIR/bin/mvn" -f "$BASEDIR/backend/pom.xml" "$@"
