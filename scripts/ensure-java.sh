#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUNTIME_DIR="$ROOT_DIR/runtime/java"
SELECTION_FILE="$ROOT_DIR/runtime/java-home.txt"
REQUIRED_MAJOR=21

java_major() {
  local executable="$1"
  [ -x "$executable" ] || { echo 0; return; }

  local output first_line version
  output="$($executable -version 2>&1 || true)"
  first_line="$(printf '%s\n' "$output" | head -n 1)"

  # java:  openjdk version "21.0.11" ...
  # javac: javac 21.0.11
  # Die bisherige allgemeine sed-Regel war bei javac-Ausgaben zu gierig und
  # erkannte aus "javac 21.0.11" faelschlich die Hauptversion 0.
  version="$(printf '%s\n' "$first_line" | sed -nE 's/^[^0-9]*([0-9]+)(\.[0-9]+.*)?$/\1/p')"
  [ -n "$version" ] || version="$(printf '%s\n' "$first_line" | sed -nE 's/.*version[[:space:]]+"?([0-9]+).*/\1/p')"

  echo "${version:-0}"
}

validate_home() {
  local home="${1:-}"
  [ -n "$home" ] || return 1
  home="${home%\"}"; home="${home#\"}"
  [ -x "$home/bin/java" ] && [ -x "$home/bin/javac" ] || return 1
  local j jv
  j="$(java_major "$home/bin/java")"
  jv="$(java_major "$home/bin/javac")"
  [ "$j" -ge "$REQUIRED_MAJOR" ] 2>/dev/null && [ "$jv" -ge "$REQUIRED_MAJOR" ] 2>/dev/null || return 1
  printf '%s\n' "$home"
}

candidates=()
add_candidate() {
  local value="${1:-}"
  [ -n "$value" ] || return 0
  local existing
  for existing in "${candidates[@]:-}"; do [ "$existing" = "$value" ] && return 0; done
  candidates+=("$value")
}

add_candidate "$RUNTIME_DIR"
add_candidate "${JAVA_HOME:-}"
for cmd in javac java; do
  if command -v "$cmd" >/dev/null 2>&1; then
    resolved="$(command -v "$cmd")"
    resolved="$(cd "$(dirname "$resolved")" && pwd)/$(basename "$resolved")"
    add_candidate "$(cd "$(dirname "$resolved")/.." && pwd)"
  fi
done

# macOS java_home kennt installierte JDKs, auch wenn JAVA_HOME/PATH nicht gesetzt sind.
if [ "$(uname -s)" = "Darwin" ] && [ -x /usr/libexec/java_home ]; then
  while IFS= read -r home; do add_candidate "$home"; done < <(/usr/libexec/java_home -V 2>&1 | sed -nE 's#.*(/[^ ]+/Contents/Home).*#\1#p')
fi

# Typische Linux/macOS-Verzeichnisse.
for pattern in /usr/lib/jvm/* /Library/Java/JavaVirtualMachines/*/Contents/Home "$HOME"/.sdkman/candidates/java/*; do
  [ -d "$pattern" ] && add_candidate "$pattern"
done

selected=""
for candidate in "${candidates[@]}"; do
  if validated="$(validate_home "$candidate")"; then selected="$validated"; break; fi
done

if [ -n "$selected" ]; then
  mkdir -p "$(dirname "$SELECTION_FILE")"
  printf '%s\n' "$selected" > "$SELECTION_FILE"
  echo "[GAM] JDK $(java_major "$selected/bin/java") gefunden: $selected"
  exit 0
fi

command -v curl >/dev/null 2>&1 || { echo "[FEHLER] curl wird fuer den Java-Download benoetigt." >&2; exit 1; }
command -v tar >/dev/null 2>&1 || { echo "[FEHLER] tar wird fuer die Java-Einrichtung benoetigt." >&2; exit 1; }
OS="$(uname -s)"; ARCH_RAW="$(uname -m)"
case "$OS" in Linux) API_OS="linux" ;; Darwin) API_OS="mac" ;; *) echo "[FEHLER] Nicht unterstuetztes Betriebssystem: $OS" >&2; exit 1 ;; esac
case "$ARCH_RAW" in x86_64|amd64) API_ARCH="x64" ;; arm64|aarch64) API_ARCH="aarch64" ;; *) echo "[FEHLER] Nicht unterstuetzte Architektur: $ARCH_RAW" >&2; exit 1 ;; esac

echo "[GAM] Kein geeignetes JDK $REQUIRED_MAJOR gefunden. Eclipse Temurin wird lokal heruntergeladen..."
TMP_DIR="$(mktemp -d)"; trap 'rm -rf "$TMP_DIR"' EXIT
ARCHIVE="$TMP_DIR/java.tar.gz"
URL="https://api.adoptium.net/v3/binary/latest/$REQUIRED_MAJOR/ga/$API_OS/$API_ARCH/jdk/hotspot/normal/eclipse?project=jdk"
curl -fL --retry 3 --connect-timeout 20 "$URL" -o "$ARCHIVE"
mkdir -p "$TMP_DIR/unpack"; tar -xzf "$ARCHIVE" -C "$TMP_DIR/unpack"
JAVA_ROOT="$(find "$TMP_DIR/unpack" -type f -path '*/bin/javac' -print -quit | sed 's#/bin/javac$##')"
[ -n "$JAVA_ROOT" ] || { echo "[FEHLER] Das Java-Archiv enthaelt kein nutzbares JDK." >&2; exit 1; }
rm -rf "$RUNTIME_DIR"; mkdir -p "$(dirname "$RUNTIME_DIR")"; mv "$JAVA_ROOT" "$RUNTIME_DIR"
if ! validated_home="$(validate_home "$RUNTIME_DIR")"; then
  echo "[FEHLER] JDK konnte nicht validiert werden." >&2
  echo "[DIAGNOSE] Java:  $RUNTIME_DIR/bin/java -> $(java_major "$RUNTIME_DIR/bin/java")" >&2
  echo "[DIAGNOSE] Javac: $RUNTIME_DIR/bin/javac -> $(java_major "$RUNTIME_DIR/bin/javac")" >&2
  "$RUNTIME_DIR/bin/java" -version 2>&1 | head -n 1 | sed 's/^/[DIAGNOSE] /' >&2 || true
  "$RUNTIME_DIR/bin/javac" -version 2>&1 | head -n 1 | sed 's/^/[DIAGNOSE] /' >&2 || true
  exit 1
fi
printf '%s\n' "$RUNTIME_DIR" > "$SELECTION_FILE"
echo "[GAM] JDK $(java_major "$RUNTIME_DIR/bin/java") wurde lokal eingerichtet."
