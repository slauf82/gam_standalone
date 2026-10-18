#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUNTIME_PARENT="$ROOT_DIR/runtime"
RUNTIME_DIR="$RUNTIME_PARENT/java"
SELECTION_FILE="$RUNTIME_PARENT/java-home.txt"
REQUIRED_MAJOR=21

java_major_from_text() {
  local text="${1:-}"
  local v=""
  v="$(printf '%s\n' "$text" | sed -nE 's/.*(version|javac)[[:space:]]+"?([0-9]+)(\.[0-9]+.*)?/\2/p' | head -n 1)"
  if [ -z "$v" ]; then
    v="$(printf '%s\n' "$text" | sed -nE 's/^[^0-9]*([0-9]+)(\.[0-9]+.*)?$/\1/p' | head -n 1)"
  fi
  printf '%s\n' "${v:-0}"
}

jdk_release_major() {
  local home="${1:-}"
  local release="$home/release"
  [ -f "$release" ] || { echo 0; return; }
  local line value
  line="$(grep -E '^JAVA_VERSION=' "$release" 2>/dev/null | head -n 1 || true)"
  value="${line#JAVA_VERSION=}"
  value="${value%\"}"; value="${value#\"}"
  java_major_from_text "$value"
}

java_major() {
  local executable="${1:-}"
  [ -x "$executable" ] || { echo 0; return; }
  local output
  output="$("$executable" -version 2>&1 || true)"
  java_major_from_text "$output"
}

normalize_home() {
  local home="${1:-}"
  [ -n "$home" ] || return 1
  home="${home%\"}"; home="${home#\"}"
  # Toleriert .../bin/java, .../bin/javac und .../bin.
  if [ -f "$home" ]; then
    home="$(cd "$(dirname "$home")/.." 2>/dev/null && pwd -P)" || return 1
  elif [ "$(basename "$home")" = "bin" ] && [ -d "$home" ]; then
    home="$(cd "$home/.." 2>/dev/null && pwd -P)" || return 1
  elif [ -d "$home" ]; then
    home="$(cd "$home" 2>/dev/null && pwd -P)" || return 1
  else
    return 1
  fi
  printf '%s\n' "$home"
}

validate_home() {
  local raw="${1:-}" home release_major java_v javac_v detected
  home="$(normalize_home "$raw")" || return 1
  [ -x "$home/bin/java" ] && [ -x "$home/bin/javac" ] || return 1

  # Wie unter Windows zuerst die standardisierte JDK-release-Datei verwenden.
  # Das ist besonders auf macOS robuster als nur stderr von nativen Binaries zu parsen.
  release_major="$(jdk_release_major "$home")"
  java_v="$(java_major "$home/bin/java")"
  javac_v="$(java_major "$home/bin/javac")"
  detected="$release_major"
  [ "$java_v" -gt "$detected" ] 2>/dev/null && detected="$java_v"
  [ "$javac_v" -gt "$detected" ] 2>/dev/null && detected="$javac_v"

  [ "$detected" -ge "$REQUIRED_MAJOR" ] 2>/dev/null || return 1
  printf '%s\n' "$home"
}

declare -a candidates=()
add_candidate() {
  local value="${1:-}" existing
  [ -n "$value" ] || return 0
  for existing in "${candidates[@]:-}"; do
    [ "$existing" = "$value" ] && return 0
  done
  candidates+=("$value")
}

mkdir -p "$RUNTIME_PARENT"

# Bereits von GAM gewähltes bzw. lokal eingerichtetes JDK zuerst.
if [ -f "$SELECTION_FILE" ]; then
  add_candidate "$(head -n 1 "$SELECTION_FILE" | tr -d '\r\n')"
fi
add_candidate "$RUNTIME_DIR"
add_candidate "${JAVA_HOME:-}"

# java/javac aus PATH (Symlinks möglichst auflösen).
for cmd in javac java; do
  if command -v "$cmd" >/dev/null 2>&1; then
    resolved="$(command -v "$cmd")"
    if command -v realpath >/dev/null 2>&1; then
      resolved="$(realpath "$resolved" 2>/dev/null || printf '%s' "$resolved")"
    fi
    add_candidate "$(dirname "$(dirname "$resolved")")"
  fi
done

# macOS: offizieller Apple-Fundweg für installierte JDKs.
if [ "$(uname -s)" = "Darwin" ] && [ -x /usr/libexec/java_home ]; then
  preferred="$(/usr/libexec/java_home -v "${REQUIRED_MAJOR}+" 2>/dev/null || true)"
  add_candidate "$preferred"
  while IFS= read -r home; do
    add_candidate "$home"
  done < <(/usr/libexec/java_home -V 2>&1 | sed -nE 's#.*(/[^[:space:]]+/Contents/Home).*#\1#p')
fi

# Übliche Linux/macOS-Installationsorte.
for pattern in \
  /usr/lib/jvm/* \
  /Library/Java/JavaVirtualMachines/*/Contents/Home \
  "$HOME"/Library/Java/JavaVirtualMachines/*/Contents/Home \
  "$HOME"/.sdkman/candidates/java/* \
  "$HOME"/.jdks/*; do
  [ -d "$pattern" ] && add_candidate "$pattern"
done

selected=""
for candidate in "${candidates[@]:-}"; do
  if validated="$(validate_home "$candidate" 2>/dev/null)"; then
    selected="$validated"
    break
  fi
done

if [ -n "$selected" ]; then
  printf '%s\n' "$selected" > "$SELECTION_FILE"
  echo "[GAM] JDK $(jdk_release_major "$selected") gefunden: $selected"
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

echo "[GAM] Kein geeignetes JDK $REQUIRED_MAJOR gefunden."
echo "[GAM] Lade Eclipse Temurin fuer $API_OS/$API_ARCH lokal in GAM herunter ..."

TMP_DIR="$(mktemp -d "${TMPDIR:-/tmp}/gam-java.XXXXXX")"
trap 'rm -rf "$TMP_DIR"' EXIT
ARCHIVE="$TMP_DIR/java.tar.gz"
UNPACK="$TMP_DIR/unpack"
URL="https://api.adoptium.net/v3/binary/latest/$REQUIRED_MAJOR/ga/$API_OS/$API_ARCH/jdk/hotspot/normal/eclipse?project=jdk"

curl -fL --retry 3 --retry-delay 2 --connect-timeout 20 --max-time 600 "$URL" -o "$ARCHIVE"
[ -s "$ARCHIVE" ] || { echo "[FEHLER] Java-Download ist leer." >&2; exit 1; }
tar -tzf "$ARCHIVE" >/dev/null 2>&1 || { echo "[FEHLER] Das heruntergeladene Java-Archiv ist ungueltig." >&2; exit 1; }

mkdir -p "$UNPACK"
tar -xzf "$ARCHIVE" -C "$UNPACK"

JAVA_ROOT="$(find "$UNPACK" -type f -path '*/bin/javac' -print -quit | sed 's#/bin/javac$##')"
[ -n "$JAVA_ROOT" ] || { echo "[FEHLER] Das Java-Archiv enthaelt kein nutzbares JDK." >&2; exit 1; }

# Einige Archiv-/Dateisystemkombinationen verlieren Ausfuehrungsbits.
chmod +x "$JAVA_ROOT/bin/java" "$JAVA_ROOT/bin/javac" 2>/dev/null || true

# Erst im Temp-Verzeichnis validieren. So zerstoert ein fehlerhaftes Archiv kein
# bereits vorhandenes runtime/java.
if ! validated_tmp="$(validate_home "$JAVA_ROOT")"; then
  echo "[FEHLER] Das heruntergeladene JDK konnte bereits vor der Installation nicht validiert werden." >&2
  echo "[DIAGNOSE] OS/Arch: $OS / $ARCH_RAW -> $API_OS / $API_ARCH" >&2
  echo "[DIAGNOSE] JDK-Kandidat: $JAVA_ROOT" >&2
  echo "[DIAGNOSE] release-Version: $(jdk_release_major "$JAVA_ROOT")" >&2
  echo "[DIAGNOSE] java-Version:    $(java_major "$JAVA_ROOT/bin/java")" >&2
  echo "[DIAGNOSE] javac-Version:   $(java_major "$JAVA_ROOT/bin/javac")" >&2
  "$JAVA_ROOT/bin/java" -version 2>&1 | head -n 3 | sed 's/^/[DIAGNOSE] /' >&2 || true
  "$JAVA_ROOT/bin/javac" -version 2>&1 | head -n 2 | sed 's/^/[DIAGNOSE] /' >&2 || true
  exit 1
fi

rm -rf "$RUNTIME_DIR"
mkdir -p "$RUNTIME_DIR"

# Inhalt des normalisierten JDK-Homes kopieren. Das funktioniert sowohl fuer das
# macOS-Bundle (.../Contents/Home) als auch fuer Linux-JDK-Archive.
cp -R "$validated_tmp"/. "$RUNTIME_DIR"/
chmod +x "$RUNTIME_DIR/bin/java" "$RUNTIME_DIR/bin/javac" 2>/dev/null || true

if ! validated_home="$(validate_home "$RUNTIME_DIR")"; then
  echo "[FEHLER] JDK wurde heruntergeladen, konnte unter runtime/java aber nicht validiert werden." >&2
  echo "[DIAGNOSE] Ziel: $RUNTIME_DIR" >&2
  echo "[DIAGNOSE] release-Version: $(jdk_release_major "$RUNTIME_DIR")" >&2
  echo "[DIAGNOSE] java-Version:    $(java_major "$RUNTIME_DIR/bin/java")" >&2
  echo "[DIAGNOSE] javac-Version:   $(java_major "$RUNTIME_DIR/bin/javac")" >&2
  exit 1
fi

printf '%s\n' "$validated_home" > "$SELECTION_FILE"
echo "[GAM] JDK $(jdk_release_major "$validated_home") wurde lokal eingerichtet: $validated_home"
