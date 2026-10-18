#!/usr/bin/env bash
set -euo pipefail
OS_NAME="$(uname -s)"
ARCH_NAME="$(uname -m)"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="$ROOT_DIR/.gam-node-env"
# GAM runtime is stored outside the replaceable application directory.
# Existing installs from older GAM versions remain supported below.
PORTABLE_BASE="${GAM_NODE_RUNTIME_DIR:-$HOME/.gam-standalone/node-runtime}"
LEGACY_PORTABLE_BASE="$ROOT_DIR/tools/node-runtime"
GAM_HOMEBREW_MARKER="/usr/local/Homebrew/.gam-standalone-managed"
info(){ echo "[GAM-NODE] $*"; }
warn(){ echo "[GAM-NODE][WARNUNG] $*"; }
fail(){ echo "[GAM-NODE][FEHLER] $*" >&2; exit 1; }
node_major(){ command -v node >/dev/null 2>&1 || { echo 0; return; }; node -v 2>/dev/null | sed -nE 's/^v([0-9]+).*/\1/p'; }
node_ready(){ local major; major="$(node_major)"; command -v npm >/dev/null 2>&1 && [ "${major:-0}" -ge 18 ] 2>/dev/null; }
write_env(){
  local bin_dir="$1"
  printf 'export PATH=%q:"$PATH"\n' "$bin_dir" > "$ENV_FILE"
}
load_portable_if_present(){
  local base current
  for base in "$PORTABLE_BASE" "$LEGACY_PORTABLE_BASE"; do
    current="$base/current/bin"
    if [ -x "$current/node" ] && [ -f "$current/npm" ]; then
      export PATH="$current:$PATH"
      if node_ready; then
        write_env "$current"
        info "Verwende vorhandene Node.js-Laufzeit: $current"
        return 0
      fi
    fi
  done
  return 1
}
ensure_homebrew(){
  command -v brew >/dev/null 2>&1 && return 0
  command -v curl >/dev/null 2>&1 || fail "curl wird fuer die automatische Homebrew-Installation benoetigt."
  info "Homebrew wurde nicht gefunden. Installiere Homebrew fuer Node.js/npm..."
  NONINTERACTIVE=1 /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)" || return 1
  if [ -x /opt/homebrew/bin/brew ]; then eval "$(/opt/homebrew/bin/brew shellenv)"; elif [ -x /usr/local/bin/brew ]; then eval "$(/usr/local/bin/brew shellenv)"; fi
  command -v brew >/dev/null 2>&1
}
install_portable_node_macos(){
  command -v curl >/dev/null 2>&1 || fail "curl wird fuer den portablen Node.js-Fallback benoetigt."
  command -v tar >/dev/null 2>&1 || fail "tar wird fuer den portablen Node.js-Fallback benoetigt."
  local node_arch="" latest_dir="https://nodejs.org/dist/latest-v22.x" sums version archive url tmp extracted size
  case "$ARCH_NAME" in
    x86_64) node_arch="x64" ;;
    arm64) node_arch="arm64" ;;
    *) fail "Nicht unterstuetzte macOS-Architektur fuer Node.js: $ARCH_NAME" ;;
  esac
  info "Installiere portables Node.js fuer macOS $ARCH_NAME im persistenten Benutzerverzeichnis (ohne Homebrew)..."
  info "FIX18 NODE | arch=$node_arch | channel=latest-v22.x"
  sums="$(curl -fLsS --retry 2 --connect-timeout 15 "$latest_dir/SHASUMS256.txt")" || fail "Node.js-Pruefsummenliste konnte nicht geladen werden: $latest_dir/SHASUMS256.txt"
  archive="$(printf '%s\n' "$sums" | awk -v a="darwin-${node_arch}.tar.gz" '$2 ~ a"$" {print $2; exit}')"
  [ -n "$archive" ] || fail "In latest-v22.x wurde kein macOS-$node_arch-Archiv gefunden."
  version="${archive#node-}"; version="${version%-darwin-${node_arch}.tar.gz}"
  url="$latest_dir/$archive"
  info "FIX18 NODE | version=$version | url=$url"
  tmp="$(mktemp -d "${TMPDIR:-/tmp}/gam-node.XXXXXX")" || fail "Temporaeres Node.js-Verzeichnis konnte nicht angelegt werden."
  curl -fL --retry 2 --connect-timeout 15 --progress-bar "$url" -o "$tmp/$archive" || { rm -rf "$tmp"; fail "Node.js konnte nicht heruntergeladen werden: $url"; }
  size="$(wc -c < "$tmp/$archive" | tr -d ' ')"
  info "FIX18 NODE | downloadBytes=$size"
  [ "${size:-0}" -gt 10000000 ] 2>/dev/null || { rm -rf "$tmp"; fail "Node.js-Download ist unerwartet klein ($size Bytes)."; }
  tar -tzf "$tmp/$archive" >/dev/null 2>&1 || { rm -rf "$tmp"; fail "Node.js-Archiv ist nicht als gueltiges tar.gz lesbar."; }
  tar -xzf "$tmp/$archive" -C "$tmp" || { rm -rf "$tmp"; fail "Node.js-Archiv konnte nicht entpackt werden."; }
  extracted="$tmp/node-${version}-darwin-${node_arch}"
  [ -x "$extracted/bin/node" ] || { find "$tmp" -maxdepth 3 -type f -name node -print 2>/dev/null || true; rm -rf "$tmp"; fail "Entpacktes Node.js enthaelt kein ausfuehrbares bin/node."; }
  [ -e "$extracted/bin/npm" ] || { rm -rf "$tmp"; fail "Entpacktes Node.js enthaelt kein bin/npm."; }
  mkdir -p "$PORTABLE_BASE"
  rm -rf "$PORTABLE_BASE/current"
  mv "$extracted" "$PORTABLE_BASE/current" || { rm -rf "$tmp"; fail "Node.js konnte nicht nach $PORTABLE_BASE/current verschoben werden."; }
  export PATH="$PORTABLE_BASE/current/bin:$PATH"
  write_env "$PORTABLE_BASE/current/bin"
  rm -rf "$tmp"
  node_ready || fail "Portable Node.js-Runtime wurde abgelegt, aber node/npm konnten nicht validiert werden (node=$(command -v node || true), npm=$(command -v npm || true))."
  info "FIX18 NODE | READY | node=$(node -v) | npm=$(npm -v) | path=$PORTABLE_BASE/current/bin"
}

rm -f "$ENV_FILE"
# GUI launches may omit common Node.js directories from PATH.
for candidate in /opt/homebrew/bin /usr/local/bin /opt/local/bin "$HOME/.nvm/current/bin"; do
  if [ -x "$candidate/node" ] && [ -f "$candidate/npm" ]; then
    export PATH="$candidate:$PATH"
    break
  fi
done
if node_ready; then info "Systemweites Node.js $(node -v) / npm $(npm -v) gefunden; keine Installation erforderlich."; exit 0; fi
if load_portable_if_present; then info "Portables Node.js $(node -v) / npm $(npm -v) gefunden; keine Installation erforderlich."; exit 0; fi
info "Node.js/npm fehlt oder Node.js ist zu alt. Automatische Installation wird vorbereitet..."
case "$OS_NAME" in
  Darwin)
    # FIX27: Ein bereits vor GAM vorhandenes Homebrew bleibt der bevorzugte
    # macOS-Pfad. Nur das von GAM selbst fuer den Standalone-Unterbau
    # eingerichtete Intel-Homebrew darf Node nicht auf Homebrew umschalten.
    if command -v brew >/dev/null 2>&1 && [ ! -f "$GAM_HOMEBREW_MARKER" ]; then
      info "FIX27 NODE | homebrew=preexisting | strategy=homebrew"
      info "Homebrew gefunden. Verwende Homebrew fuer Node.js/npm..."
      brew list --versions node >/dev/null 2>&1 || brew install node
    elif [ "$ARCH_NAME" = "x86_64" ]; then
      if command -v brew >/dev/null 2>&1; then
        info "FIX27 NODE | homebrew=gam-managed | strategy=portable-node22"
      else
        info "FIX27 NODE | homebrew=absent | strategy=portable-node22"
      fi
      install_portable_node_macos
    elif command -v brew >/dev/null 2>&1; then
      info "Homebrew gefunden. Verwende Homebrew fuer Node.js/npm..."
      brew list --versions node >/dev/null 2>&1 || brew install node
    elif [ "$ARCH_NAME" = "arm64" ]; then
      if ensure_homebrew; then
        brew list --versions node >/dev/null 2>&1 || brew install node
      else
        warn "Homebrew konnte nicht automatisch eingerichtet werden; verwende portables Node.js als Fallback."
        install_portable_node_macos
      fi
    else
      fail "Nicht unterstuetzte macOS-Architektur fuer Node.js: $ARCH_NAME"
    fi
    ;;
  Linux)
    if command -v apt-get >/dev/null 2>&1; then sudo apt-get update || warn "apt-get update meldete einen Fehler."; sudo apt-get install -y nodejs npm
    elif command -v dnf >/dev/null 2>&1; then sudo dnf install -y nodejs npm
    elif command -v yum >/dev/null 2>&1; then sudo yum install -y nodejs npm
    elif command -v pacman >/dev/null 2>&1; then sudo pacman -Sy --needed --noconfirm nodejs npm
    else fail "Kein unterstuetzter Linux-Paketmanager fuer Node.js/npm gefunden."; fi ;;
  *) fail "Nicht unterstuetztes Betriebssystem fuer automatische Node.js/npm-Installation: $OS_NAME" ;;
esac
node_ready || fail "Node.js/npm wurde installiert, konnte aber nicht mit mindestens Node.js 18 validiert werden."
info "Node.js $(node -v) / npm $(npm -v) ist einsatzbereit."
