#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUNTIME_DIR="$ROOT_DIR/runtime/java"
JAVA_BIN="$RUNTIME_DIR/bin/java"
JAVAC_BIN="$RUNTIME_DIR/bin/javac"
REQUIRED_MAJOR=21

java_major() {
  local executable="$1"
  [ -x "$executable" ] || { echo 0; return; }
  local version
  version="$($executable -version 2>&1 | awk -F'"' '/version/ {print $2; exit}')"
  [ -n "$version" ] || { echo 0; return; }
  if [[ "$version" == 1.* ]]; then
    echo "$version" | cut -d. -f2
  else
    echo "$version" | cut -d. -f1
  fi
}

if command -v java >/dev/null 2>&1; then
  SYSTEM_JAVA="$(command -v java)"
  SYSTEM_MAJOR="$(java_major "$SYSTEM_JAVA")"
  if [ "$SYSTEM_MAJOR" -ge "$REQUIRED_MAJOR" ] 2>/dev/null && command -v javac >/dev/null 2>&1; then
    echo "[GAM] Java $SYSTEM_MAJOR gefunden: $SYSTEM_JAVA"
    exit 0
  fi
  echo "[GAM] Vorhandenes Java $SYSTEM_MAJOR ist zu alt. GAM richtet Java $REQUIRED_MAJOR lokal ein."
fi

LOCAL_MAJOR="$(java_major "$JAVA_BIN")"
if [ "$LOCAL_MAJOR" -ge "$REQUIRED_MAJOR" ] 2>/dev/null && [ -x "$JAVAC_BIN" ]; then
  echo "[GAM] Lokale Java-Laufzeit $LOCAL_MAJOR ist vorhanden."
  exit 0
fi

command -v curl >/dev/null 2>&1 || { echo "[FEHLER] curl wird fuer den Java-Download benoetigt." >&2; exit 1; }
command -v tar >/dev/null 2>&1 || { echo "[FEHLER] tar wird fuer die Java-Einrichtung benoetigt." >&2; exit 1; }

OS="$(uname -s)"
ARCH_RAW="$(uname -m)"
case "$OS" in
  Linux) API_OS="linux" ;;
  Darwin) API_OS="mac" ;;
  *) echo "[FEHLER] Nicht unterstuetztes Betriebssystem: $OS" >&2; exit 1 ;;
esac
case "$ARCH_RAW" in
  x86_64|amd64) API_ARCH="x64" ;;
  arm64|aarch64) API_ARCH="aarch64" ;;
  *) echo "[FEHLER] Nicht unterstuetzte Architektur: $ARCH_RAW" >&2; exit 1 ;;
esac

echo "[GAM] Java $REQUIRED_MAJOR fehlt. Eclipse Temurin wird lokal fuer GAM heruntergeladen..."
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT
ARCHIVE="$TMP_DIR/java.tar.gz"
URL="https://api.adoptium.net/v3/binary/latest/$REQUIRED_MAJOR/ga/$API_OS/$API_ARCH/jdk/hotspot/normal/eclipse?project=jdk"
curl -fL --retry 3 --connect-timeout 20 "$URL" -o "$ARCHIVE"
mkdir -p "$TMP_DIR/unpack"
tar -xzf "$ARCHIVE" -C "$TMP_DIR/unpack"
JAVA_ROOT="$(find "$TMP_DIR/unpack" -mindepth 1 -maxdepth 2 -type f -path '*/bin/javac' -print -quit | sed 's#/bin/javac$##')"
[ -n "$JAVA_ROOT" ] || { echo "[FEHLER] Das Java-Archiv enthaelt keine nutzbare Laufzeit." >&2; exit 1; }
rm -rf "$RUNTIME_DIR"
mkdir -p "$(dirname "$RUNTIME_DIR")"
mv "$JAVA_ROOT" "$RUNTIME_DIR"
LOCAL_MAJOR="$(java_major "$JAVA_BIN")"
[ "$LOCAL_MAJOR" -ge "$REQUIRED_MAJOR" ] 2>/dev/null || { echo "[FEHLER] Java konnte nicht validiert werden." >&2; exit 1; }
echo "[GAM] Java $LOCAL_MAJOR wurde lokal unter runtime/java eingerichtet."
