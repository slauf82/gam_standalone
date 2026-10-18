#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DB_NAME="${GAM_DB_NAME:-kopfzentruminventardb}"
DB_HOST="${GAM_DB_HOST:-127.0.0.1}"
DB_PORT="${GAM_DB_PORT:-3306}"
DB_USER="${GAM_DB_USER:-}"
DB_PASSWORD="${GAM_DB_PASSWORD:-}"
SCHEMA_FILE="$ROOT/database/gam_v2_1_0_preview2_empty.sql"
ENV_FILE="$ROOT/.env"

info(){ echo "[GAM-DB] $*"; }
warn(){ echo "[GAM-DB][WARNUNG] $*"; }
fail(){ echo "[GAM-DB][FEHLER] $*" >&2; exit 1; }
port_open(){ (echo >"/dev/tcp/$DB_HOST/$DB_PORT") >/dev/null 2>&1; }

mariadb_components_present(){
  command -v mariadbd >/dev/null 2>&1 || command -v mysqld >/dev/null 2>&1 || \
  command -v mariadb >/dev/null 2>&1 || command -v mysql >/dev/null 2>&1
}

OS_NAME="$(uname -s)"
ARCH_NAME="$(uname -m)"
PORTABLE_DB_BASE="$ROOT/tools/mariadb-runtime"
PORTABLE_DB_CURRENT="$PORTABLE_DB_BASE/current"
PORTABLE_DB_DATA="$PORTABLE_DB_BASE/data"
PORTABLE_DB_LOG="$PORTABLE_DB_BASE/mariadb.log"
PORTABLE_DB_PID="$PORTABLE_DB_BASE/mariadb.pid"
# FIX22: Unix-domain socket paths on macOS are limited (~103 bytes). Keep the
# socket outside the potentially long GAM installation path; all persistent
# runtime/data/log files remain inside GAM. UID prevents collisions per user.
PORTABLE_DB_SOCKET="${TMPDIR:-/tmp}/gam-mysql-$(id -u).sock"

ensure_homebrew(){
  command -v brew >/dev/null 2>&1 && return 0
  [ "$OS_NAME" = "Darwin" ] || return 1
  command -v curl >/dev/null 2>&1 || fail "curl wird fuer die automatische Homebrew-Installation benoetigt."
  info "Homebrew wurde nicht gefunden. Installiere Homebrew fuer die lokale macOS-Datenbank..."
  NONINTERACTIVE=1 /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)" || \
    fail "Homebrew konnte nicht automatisch installiert werden."
  if [ -x /opt/homebrew/bin/brew ]; then
    eval "$(/opt/homebrew/bin/brew shellenv)"
  elif [ -x /usr/local/bin/brew ]; then
    eval "$(/usr/local/bin/brew shellenv)"
  fi
  command -v brew >/dev/null 2>&1 || fail "Homebrew wurde installiert, ist fuer dieses Skript aber noch nicht im PATH."
}

install_portable_mysql_macos_fix20(){
  command -v curl >/dev/null 2>&1 || fail "curl wird fuer den portablen macOS-Datenbank-Fallback benoetigt."
  command -v tar >/dev/null 2>&1 || fail "tar wird fuer den portablen macOS-Datenbank-Fallback benoetigt."
  local mysql_version="${GAM_MYSQL_PORTABLE_VERSION:-8.4.11}" mysql_arch archive url tmp extracted server client
  case "$ARCH_NAME" in x86_64) mysql_arch="x86_64";; arm64) mysql_arch="arm64";; *) fail "FIX20: Keine portable MySQL-macOS-Runtime fuer Architektur '$ARCH_NAME' vorbereitet.";; esac
  archive="mysql-${mysql_version}-macos15-${mysql_arch}.tar.gz"
  url="https://cdn.mysql.com/Downloads/MySQL-8.4/${archive}"
  mkdir -p "$PORTABLE_DB_BASE"
  if [ ! -x "$PORTABLE_DB_CURRENT/bin/mysqld" ]; then
    tmp="$(mktemp -d "${TMPDIR:-/tmp}/gam-mysql.XXXXXX")" || fail "Temporaeres MySQL-Verzeichnis konnte nicht angelegt werden."
    info "FIX20 DB | mode=portable-mysql | arch=$ARCH_NAME | version=$mysql_version | homebrew=false | xcode=false"
    info "FIX20 DB | download=$url"
    curl -fL --retry 2 --connect-timeout 15 --progress-bar "$url" -o "$tmp/$archive" || { rm -rf "$tmp"; fail "Portable MySQL-Runtime konnte nicht heruntergeladen werden: $url"; }
    info "FIX20 DB | archiveBytes=$(wc -c < "$tmp/$archive" | tr -d ' ')"
    tar -xzf "$tmp/$archive" -C "$tmp" || { rm -rf "$tmp"; fail "Portable MySQL-Runtime konnte nicht entpackt werden."; }
    extracted="$(find "$tmp" -maxdepth 1 -type d -name "mysql-${mysql_version}-macos15-${mysql_arch}" -print -quit)"
    [ -n "$extracted" ] || { rm -rf "$tmp"; fail "Entpackte portable MySQL-Runtime wurde nicht gefunden."; }
    rm -rf "$PORTABLE_DB_CURRENT"; mv "$extracted" "$PORTABLE_DB_CURRENT" || { rm -rf "$tmp"; fail "Portable MySQL-Runtime konnte nicht nach $PORTABLE_DB_CURRENT verschoben werden."; }; rm -rf "$tmp"
  else
    info "FIX20 DB | portableRuntime=reuse | path=$PORTABLE_DB_CURRENT"
  fi
  server="$PORTABLE_DB_CURRENT/bin/mysqld"; client="$PORTABLE_DB_CURRENT/bin/mysql"
  [ -x "$server" ] || fail "Portable MySQL-Runtime ist unvollstaendig: mysqld fehlt."
  [ -x "$client" ] || fail "Portable MySQL-Runtime ist unvollstaendig: mysql-Client fehlt."
  mkdir -p "$PORTABLE_DB_DATA"
  if [ ! -d "$PORTABLE_DB_DATA/mysql" ]; then
    info "FIX20 DB | initdb=$PORTABLE_DB_DATA"
    "$server" --initialize-insecure --basedir="$PORTABLE_DB_CURRENT" --datadir="$PORTABLE_DB_DATA" --log-error="$PORTABLE_DB_LOG" || fail "Portable MySQL-Datenbank konnte nicht initialisiert werden."
  fi
  cat > "$PORTABLE_DB_BASE/my.cnf" <<EOF
[mysqld]
basedir=$PORTABLE_DB_CURRENT
datadir=$PORTABLE_DB_DATA
bind-address=$DB_HOST
port=$DB_PORT
socket=$PORTABLE_DB_SOCKET
pid-file=$PORTABLE_DB_PID
log-error=$PORTABLE_DB_LOG
skip-name-resolve
[client]
socket=$PORTABLE_DB_SOCKET
port=$DB_PORT
EOF
  info "FIX22 DB | configured=$PORTABLE_DB_BASE/my.cnf | bind=$DB_HOST:$DB_PORT | socket=$PORTABLE_DB_SOCKET"
}
start_portable_mysql_macos_fix20(){
  local server="$PORTABLE_DB_CURRENT/bin/mysqld"; [ -x "$server" ] || return 1; port_open && return 0
  info "Starte portable MySQL-8.4-LTS-Runtime im GAM-Verzeichnis..."
  "$server" --defaults-file="$PORTABLE_DB_BASE/my.cnf" >/dev/null 2>&1 & local child=$!
  echo "$child" > "$PORTABLE_DB_BASE/launcher.pid" 2>/dev/null || true
  info "FIX20 DB | start=portable-mysql | pid=$child | endpoint=$DB_HOST:$DB_PORT"
}
install_mariadb_macos_macports_binary(){
  # FIX19: MariaDB Foundation no longer publishes a current Intel-macOS PKG for
  # the selected server release. On Intel Tahoe we therefore use MacPorts as
  # the platform underlay. This deliberately changes only provisioning; GAM's
  # database/schema/backend logic remains untouched.
  command -v curl >/dev/null 2>&1 || fail "curl wird fuer den Intel-macOS-MariaDB-Fallback benoetigt."
  local mp_version="${GAM_MACPORTS_VERSION:-2.12.6}"
  local mp_pkg="MacPorts-${mp_version}-26-Tahoe.pkg"
  local mp_url="https://distfiles.macports.org/MacPorts/${mp_pkg}"
  local tmp pkg_path port_bin

  info "macOS ohne Homebrew erkannt. Verwende ausschliesslich vorgebaute MacPorts-Binaerarchive fuer MariaDB."
  info "FIX23 MARIADB | mode=macports-binary-only | arch=$ARCH_NAME | macos=$(sw_vers -productVersion 2>/dev/null || echo unbekannt) | macports=$mp_version | mariadb=10.11"

  port_bin="$(command -v port || true)"
  if [ -z "$port_bin" ] && [ -x /opt/local/bin/port ]; then port_bin=/opt/local/bin/port; fi
  if [ -z "$port_bin" ]; then
    tmp="$(mktemp -d "${TMPDIR:-/tmp}/gam-macports.XXXXXX")" || fail "Temporaeres MacPorts-Verzeichnis konnte nicht angelegt werden."
    pkg_path="$tmp/$mp_pkg"
    info "FIX23 MARIADB | macportsPackage=$mp_pkg"
    curl -fL --retry 2 --connect-timeout 15 --progress-bar "$mp_url" -o "$pkg_path" || { rm -rf "$tmp"; fail "MacPorts-Tahoe-PKG konnte nicht heruntergeladen werden: $mp_url"; }
    info "FIX23 MARIADB | macportsPackageBytes=$(wc -c < "$pkg_path" | tr -d ' ')"
    info "MacPorts wird einmalig als macOS-Unterbau installiert. macOS fordert dafuer Administratorrechte an."
    sudo /usr/sbin/installer -pkg "$pkg_path" -target / || { rm -rf "$tmp"; fail "MacPorts-Tahoe-PKG konnte nicht installiert werden."; }
    rm -rf "$tmp"
    [ -x /opt/local/bin/port ] || fail "MacPorts wurde installiert, /opt/local/bin/port fehlt jedoch."
    port_bin=/opt/local/bin/port
  fi

  export PATH="/opt/local/bin:/opt/local/sbin:/opt/local/lib/mariadb-10.11/bin:$PATH"
  info "FIX23 MARIADB | port=$port_bin"
  info "FIX23 MARIADB | install=binary-only | sourceBuild=false | fullXcodeRequired=false"
  sudo "$port_bin" -N -b install mariadb-10.11 mariadb-10.11-server || fail "MariaDB 10.11 konnte nicht aus vorgebauten MacPorts-Binaerarchiven installiert werden. FIX23 startet bewusst keinen Quellcode-Build und verlangt kein Full Xcode."

  local mp_data=/opt/local/var/db/mariadb-10.11
  local mp_bin=/opt/local/lib/mariadb-10.11/bin
  if [ ! -d "$mp_data/mysql" ]; then
    info "FIX23 MARIADB | initdb=$mp_data"
    sudo mkdir -p "$mp_data"
    sudo chown _mysql:_mysql "$mp_data" 2>/dev/null || true
    if [ -x "$mp_bin/mariadb-install-db" ]; then
      sudo -u _mysql "$mp_bin/mariadb-install-db" --basedir=/opt/local --datadir="$mp_data" || fail "MacPorts-MariaDB-Datenverzeichnis konnte nicht initialisiert werden."
    else
      fail "mariadb-install-db wurde nach der MacPorts-Installation nicht gefunden."
    fi
  fi

  # MacPorts disables networking by default. GAM needs the existing JDBC path
  # 127.0.0.1:3306, so only the local server underlay is configured accordingly.
  local cnf_dir=/opt/local/etc/mariadb-10.11
  local cnf="$cnf_dir/my.cnf"
  sudo mkdir -p "$cnf_dir"
  sudo /bin/sh -c "cat > '$cnf'" <<EOF
[mysqld]
datadir=$mp_data
bind-address=$DB_HOST
port=$DB_PORT
socket=/opt/local/var/run/mariadb-10.11/mysqld.sock

[client]
socket=/opt/local/var/run/mariadb-10.11/mysqld.sock
port=$DB_PORT
EOF
  info "FIX23 MARIADB | configured=$cnf | bind=$DB_HOST:$DB_PORT"
}

prepare_homebrew_prefix_fix25(){
  # A fresh GAM directory can meet a partially bootstrapped system-wide
  # Homebrew from an earlier test. Repair only Homebrew-owned prefix paths;
  # never chown /usr/local recursively. Homebrew itself runs unprivileged.
  local uid gid
  uid="$(id -u)"
  gid="$(id -g)"
  info "FIX25 HOMEBREW | prefixRepair=true | prefix=/usr/local | user=$(id -un) | adminRequired=true"
  sudo mkdir -p \
    /usr/local/bin /usr/local/sbin /usr/local/etc /usr/local/include \
    /usr/local/lib /usr/local/share /usr/local/var /usr/local/opt \
    /usr/local/Cellar /usr/local/Caskroom /usr/local/Frameworks \
    /usr/local/var/homebrew /usr/local/var/homebrew/locks || return 1
  sudo chown "$uid:$gid" \
    /usr/local/bin /usr/local/sbin /usr/local/etc /usr/local/include \
    /usr/local/lib /usr/local/share /usr/local/var /usr/local/opt \
    /usr/local/Cellar /usr/local/Caskroom /usr/local/Frameworks \
    /usr/local/var/homebrew /usr/local/var/homebrew/locks || return 1
  if [ -d /usr/local/Homebrew ]; then
    sudo chown -R "$uid:$gid" /usr/local/Homebrew || return 1
  fi
  info "FIX25 HOMEBREW | prefixRepair=ok | brewRunsAsUser=$(id -un)"
}

install_homebrew_intel_tahoe_fix24(){
  # FIX24: Homebrew's current installer no longer bootstraps this Intel/Tahoe
  # edge case reliably. Keep this strictly in the macOS underlay and pin the
  # bootstrap so `brew update` cannot silently move us to a different policy.
  [ "$OS_NAME" = "Darwin" ] || return 1
  [ "$ARCH_NAME" = "x86_64" ] || return 1
  command -v curl >/dev/null 2>&1 || fail "curl wird fuer den Intel-Tahoe-Homebrew-Bootstrap benoetigt."
  local brew_tag="${GAM_HOMEBREW_INTEL_BOOTSTRAP_TAG:-6.0.22}"
  local brew_root=/usr/local/Homebrew
  local tmp archive

  if [ -x /usr/local/bin/brew ]; then
    export PATH="/usr/local/bin:/usr/local/sbin:$PATH"
    info "FIX25 HOMEBREW | existingBootstrap=true | repairBeforeUse=true"
    prepare_homebrew_prefix_fix25 || fail "FIX25: Homebrew-Prefixrechte konnten nicht repariert werden."
    return 0
  fi

  info "FIX25 MARIADB | mode=homebrew-sonoma-bottle | arch=x86_64 | macos=$(sw_vers -productVersion 2>/dev/null || echo unbekannt) | bottle=sonoma"
  info "FIX24 HOMEBREW | bootstrapTag=$brew_tag | prefix=/usr/local | autoUpdate=false"
  tmp="$(mktemp -d "${TMPDIR:-/tmp}/gam-homebrew.XXXXXX")" || fail "Temporaeres Homebrew-Verzeichnis konnte nicht angelegt werden."
  archive="$tmp/homebrew-${brew_tag}.tar.gz"
  curl -fL --retry 2 --connect-timeout 15 --progress-bar \
    "https://github.com/Homebrew/brew/archive/refs/tags/${brew_tag}.tar.gz" -o "$archive" || {
      rm -rf "$tmp"; fail "FIX24: Homebrew-Bootstrap $brew_tag konnte nicht geladen werden.";
    }
  info "FIX24 HOMEBREW | archiveBytes=$(wc -c < "$archive" | tr -d ' ')"
  tar -xzf "$archive" -C "$tmp" || { rm -rf "$tmp"; fail "FIX24: Homebrew-Bootstrap konnte nicht entpackt werden."; }
  local extracted="$tmp/brew-${brew_tag}"
  [ -x "$extracted/bin/brew" ] || { rm -rf "$tmp"; fail "FIX24: Homebrew-Bootstrap ist unvollstaendig."; }

  info "FIX24 HOMEBREW | install=$brew_root | adminRequired=true"
  sudo mkdir -p /usr/local/bin /usr/local/sbin
  sudo rm -rf "$brew_root"
  sudo mv "$extracted" "$brew_root"
  sudo chown -R "$(id -u):$(id -g)" "$brew_root"
  sudo ln -sf "$brew_root/bin/brew" /usr/local/bin/brew
  rm -rf "$tmp"
  export PATH="/usr/local/bin:/usr/local/sbin:$PATH"
  [ -x /usr/local/bin/brew ] || fail "FIX25: Homebrew wurde eingerichtet, /usr/local/bin/brew fehlt jedoch."
  prepare_homebrew_prefix_fix25 || fail "FIX25: Homebrew-Prefixrechte konnten nach dem Bootstrap nicht vorbereitet werden."
  # FIX27: Persistente Systemmarkierung. Sie liegt absichtlich im von GAM
  # installierten Homebrew selbst und ueberlebt neue GAM-Verzeichnisse.
  # Ein bereits vorhandenes Benutzer-Homebrew bekommt diese Markierung nie.
  : > "$brew_root/.gam-standalone-managed" || fail "FIX27: GAM-Homebrew-Markierung konnte nicht geschrieben werden."
  info "FIX27 HOMEBREW | managedByGAM=true | marker=$brew_root/.gam-standalone-managed"
  info "FIX25 HOMEBREW | ready=true | version=$(/usr/local/bin/brew --version 2>/dev/null | head -1 || echo unbekannt)"
}

install_mariadb_homebrew_fix24(){
  local brew_bin
  brew_bin="$(command -v brew || true)"
  [ -n "$brew_bin" ] || return 1
  export HOMEBREW_NO_AUTO_UPDATE=1
  export HOMEBREW_NO_INSTALL_FROM_API=0

  if "$brew_bin" list --versions mariadb@10.11 >/dev/null 2>&1; then
    info "FIX24 MARIADB | homebrewFormula=mariadb@10.11 | state=already-installed"
    return 0
  fi

  if [ "$ARCH_NAME" = "x86_64" ]; then
    # Homebrew maintainers explicitly state that the Sonoma x86_64 bottles are
    # intended to serve Intel Sequoia/Tahoe too. --force-bottle prevents a
    # source build; failure is handled as an underlay failure, never by changing
    # GAM's MariaDB schema.
    info "FIX24 MARIADB | homebrewFormula=mariadb@10.11 | bottle=sonoma-x86_64 | forceBottle=true | sourceBuild=false"
    "$brew_bin" install --force-bottle mariadb@10.11 || return 1
  else
    info "FIX24 MARIADB | homebrewFormula=mariadb@10.11 | bottle=native | sourceBuild=false"
    "$brew_bin" install --force-bottle mariadb@10.11 || return 1
  fi
  "$brew_bin" list --versions mariadb@10.11 >/dev/null 2>&1
}

install_mariadb_macos(){
  # FIX24 keeps GAM on real MariaDB. The difficult Intel/Tahoe path now tries
  # Homebrew's existing x86_64 Sonoma bottle first; Homebrew maintainers state
  # that Sonoma Intel bottles are reused on Sequoia/Tahoe. No ARM bottle, no
  # MySQL fallback, no schema compatibility patch and no source build.
  info "FIX25 MARIADB | mysqlFallback=false | schemaCompatibilityPatch=false | sourceBuild=false"

  if command -v brew >/dev/null 2>&1; then
    if [ "$ARCH_NAME" = "x86_64" ]; then
      info "FIX25 HOMEBREW | existingBootstrap=true | repairBeforeUse=true"
      prepare_homebrew_prefix_fix25 || fail "FIX25: Homebrew-Prefixrechte konnten nicht repariert werden."
    fi
    info "Homebrew gefunden. Installiere/verwende MariaDB 10.11 als Bottle."
    install_mariadb_homebrew_fix24 || fail "FIX25: MariaDB 10.11 konnte nicht als Homebrew-Bottle installiert werden. Ein Source-Build wird bewusst nicht gestartet."
    return
  fi

  if [ "$ARCH_NAME" = "x86_64" ]; then
    info "Intel-macOS ohne Homebrew erkannt. FIX24 versucht den Homebrew-Sonoma-x86_64-Bottle-Pfad."
    install_homebrew_intel_tahoe_fix24
    install_mariadb_homebrew_fix24 || fail "FIX24: Der Homebrew-Sonoma-x86_64-Bottle fuer MariaDB 10.11 konnte auf diesem Intel-macOS nicht installiert werden. Kein ARM-Bottle und kein Source-Build wurden verwendet."
    return
  fi

  # Apple Silicon: current Homebrew has native Tahoe bottles. Existing
  # provisioning is retained, but installation remains bottle-only.
  ensure_homebrew
  install_mariadb_homebrew_fix24 || fail "FIX24: Native MariaDB-10.11-Homebrew-Bottle-Installation ist fehlgeschlagen."
}

start_mariadb_macos(){
  if command -v brew >/dev/null 2>&1; then
    export PATH="$(brew --prefix mariadb@10.11 2>/dev/null)/bin:$PATH"
    info "Starte MariaDB 10.11 ueber Homebrew Services..."
    brew services start mariadb@10.11 >/dev/null 2>&1 || brew services restart mariadb@10.11 >/dev/null 2>&1 || true
    return
  fi
  if [ -x "$PORTABLE_DB_CURRENT/bin/mysqld" ]; then
    start_portable_mysql_macos_fix20
    return
  fi
  if [ -x /opt/local/bin/port ]; then
    export PATH="/opt/local/bin:/opt/local/sbin:/opt/local/lib/mariadb-10.11/bin:$PATH"
    info "Starte MariaDB 10.11 ueber den MacPorts-Unterbau..."
    sudo /opt/local/bin/port -N load mariadb-10.11-server >/dev/null 2>&1 || true
    sleep 2
    if port_open; then
      info "FIX23 MARIADB | start=macports | reachable=true | endpoint=$DB_HOST:$DB_PORT"
      return
    fi
    local server=/opt/local/lib/mariadb-10.11/bin/mariadbd
    if [ -x "$server" ]; then
      info "FIX23 MARIADB | serviceStartFailed=true | directStart=$server"
      sudo -u _mysql "$server" --defaults-file=/opt/local/etc/mariadb-10.11/my.cnf >/tmp/gam-mariadb-start.log 2>&1 &
    fi
    return
  fi
  # Existing non-MacPorts fallback retained for already provisioned systems.
  info "Starte MariaDB auf Intel-macOS ohne Homebrew..."
  for plist in /Library/LaunchDaemons/org.mariadb.mysqld.plist /Library/LaunchDaemons/com.mariadb.mysqld.plist; do
    if [ -f "$plist" ]; then sudo launchctl load -w "$plist" 2>/dev/null || sudo launchctl kickstart -k "system/$(basename "$plist" .plist)" 2>/dev/null || true; fi
  done
}

detect_linux_package_manager(){
  if command -v apt-get >/dev/null 2>&1; then echo apt
  elif command -v dnf >/dev/null 2>&1; then echo dnf
  elif command -v yum >/dev/null 2>&1; then echo yum
  elif command -v pacman >/dev/null 2>&1; then echo pacman
  else echo ""
  fi
}

install_mariadb_linux(){
  local pm package_status=0
  pm="$(detect_linux_package_manager)"
  [ -n "$pm" ] || fail "Kein unterstuetzter Linux-Paketmanager gefunden (apt/dnf/yum/pacman)."

  info "Installiere MariaDB ueber Linux-Paketmanager '$pm'..."
  case "$pm" in
    apt)
      if ! sudo apt-get update; then
        warn "apt-get update meldete einen Fehler. Die MariaDB-Installation wird trotzdem versucht."
      fi
      sudo env DEBIAN_FRONTEND=noninteractive apt-get install -y mariadb-server mariadb-client || package_status=$?
      ;;
    dnf)
      sudo dnf install -y mariadb-server mariadb || package_status=$?
      ;;
    yum)
      sudo yum install -y mariadb-server mariadb || package_status=$?
      ;;
    pacman)
      sudo pacman -Sy --needed --noconfirm mariadb || package_status=$?
      if mariadb_components_present && [ ! -d /var/lib/mysql/mysql ] && command -v mariadb-install-db >/dev/null 2>&1; then
        info "Initialisiere MariaDB-Datenverzeichnis fuer pacman/Arch..."
        sudo mariadb-install-db --user=mysql --basedir=/usr --datadir=/var/lib/mysql || true
      fi
      ;;
  esac

  if [ "$package_status" -ne 0 ]; then
    warn "$pm beendete die Installation mit Fehlercode $package_status. Pruefe den tatsaechlichen MariaDB-Systemzustand..."
  fi
  mariadb_components_present || fail "MariaDB-Komponenten wurden nach dem Installationsversuch nicht gefunden."
}

start_mariadb_linux(){
  if command -v systemctl >/dev/null 2>&1; then
    sudo systemctl enable --now mariadb 2>/dev/null || \
      sudo systemctl enable --now mysql 2>/dev/null || \
      sudo systemctl start mariadb 2>/dev/null || \
      sudo systemctl start mysql 2>/dev/null || true
  elif command -v service >/dev/null 2>&1; then
    sudo service mariadb start 2>/dev/null || sudo service mysql start 2>/dev/null || true
  else
    warn "Weder systemctl noch service gefunden. MariaDB-Dienst kann nicht automatisch gestartet werden."
  fi
}

wait_for_mariadb(){
  for _ in $(seq 1 45); do
    port_open && return 0
    sleep 1
  done
  return 1
}

install_and_start_mariadb(){
  info "MariaDB ist nicht erreichbar. Lokale Installation/Start wird vorbereitet..."
  case "$OS_NAME" in
    Darwin)
      install_mariadb_macos
      start_mariadb_macos
      ;;
    Linux)
      install_mariadb_linux
      start_mariadb_linux
      ;;
    *)
      fail "Nicht unterstuetztes Betriebssystem fuer automatische MariaDB-Einrichtung: $OS_NAME"
      ;;
  esac

  if wait_for_mariadb; then
    info "MariaDB ist unter $DB_HOST:$DB_PORT erreichbar."
    return 0
  fi

  if [ "$OS_NAME" = "Darwin" ] && command -v brew >/dev/null 2>&1; then
    warn "Homebrew-Service-Status:"
    brew services list 2>/dev/null | grep -E '^(mariadb|Name)' || true
  elif [ "$OS_NAME" = "Linux" ] && command -v systemctl >/dev/null 2>&1; then
    warn "systemd-Status (Kurzfassung):"
    systemctl --no-pager --full status mariadb 2>/dev/null | tail -n 20 || \
      systemctl --no-pager --full status mysql 2>/dev/null | tail -n 20 || true
  fi

  fail "MariaDB wurde vorbereitet, ist aber unter $DB_HOST:$DB_PORT nicht erreichbar."
}

find_db_client(){
  local client=""
  # FIX21: If GAM owns the portable DB runtime, always use its matching client.
  # A MacPorts client may be on PATH from fix19 even though that server is not used.
  if [ "$OS_NAME" = "Darwin" ] && [ -x "$PORTABLE_DB_CURRENT/bin/mysql" ]; then
    printf '%s\n' "$PORTABLE_DB_CURRENT/bin/mysql"
    return 0
  fi
  client="$(command -v mariadb || command -v mysql || true)"
  if [ -n "$client" ]; then printf '%s\n' "$client"; return 0; fi
  if [ "$OS_NAME" = "Darwin" ] && command -v brew >/dev/null 2>&1; then
    local prefix
    prefix="$(brew --prefix mariadb 2>/dev/null || true)"
    if [ -x "$prefix/bin/mariadb" ]; then printf '%s\n' "$prefix/bin/mariadb"; return 0; fi
    if [ -x "$prefix/bin/mysql" ]; then printf '%s\n' "$prefix/bin/mysql"; return 0; fi
  fi
  if [ "$OS_NAME" = "Darwin" ]; then
    local candidate
    for candidate in "$PORTABLE_DB_CURRENT/bin/mysql" /opt/local/lib/mariadb-10.11/bin/mariadb /opt/local/lib/mariadb-10.11/bin/mysql /usr/local/mariadb/bin/mariadb /usr/local/mariadb/bin/mysql /usr/local/mysql/bin/mariadb /usr/local/mysql/bin/mysql; do
      if [ -x "$candidate" ]; then printf '%s\n' "$candidate"; return 0; fi
    done
  fi
  return 1
}

if ! port_open; then install_and_start_mariadb; else info "MariaDB/MySQL unter $DB_HOST:$DB_PORT erreichbar."; fi

# Bei einer lokalen Neuinstallation einen eigenen GAM-Benutzer erzeugen. Dadurch
# bleibt das distributionsabhaengige Root-/Unix-Socket-Login unangetastet.
if [ -z "$DB_USER" ]; then
  DB_USER="gam"
  if [ -z "$DB_PASSWORD" ]; then
    if command -v openssl >/dev/null 2>&1; then DB_PASSWORD="$(openssl rand -hex 16)"; else DB_PASSWORD="gam-$(date +%s)-$RANDOM"; fi
  fi
  ADMIN_CLIENT="$(find_db_client || true)"
  [ -n "$ADMIN_CLIENT" ] || fail "MariaDB-Client wurde nicht gefunden."
  ESC_PASS=${DB_PASSWORD//\'/\'\'}

  ADMIN_SQL="$(mktemp "${TMPDIR:-/tmp}/gam-db-admin.XXXXXX.sql")"
  cat > "$ADMIN_SQL" <<SQL
CREATE DATABASE IF NOT EXISTS \`$DB_NAME\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS '$DB_USER'@'localhost' IDENTIFIED BY '$ESC_PASS';
CREATE USER IF NOT EXISTS '$DB_USER'@'127.0.0.1' IDENTIFIED BY '$ESC_PASS';
ALTER USER '$DB_USER'@'localhost' IDENTIFIED BY '$ESC_PASS';
ALTER USER '$DB_USER'@'127.0.0.1' IDENTIFIED BY '$ESC_PASS';
GRANT ALL PRIVILEGES ON \`$DB_NAME\`.* TO '$DB_USER'@'localhost';
GRANT ALL PRIVILEGES ON \`$DB_NAME\`.* TO '$DB_USER'@'127.0.0.1';
FLUSH PRIVILEGES;
SQL
  if [ "$OS_NAME" = "Darwin" ]; then
    if [ -x "$PORTABLE_DB_CURRENT/bin/mysqld" ] && [ "$ADMIN_CLIENT" = "$PORTABLE_DB_CURRENT/bin/mysql" ]; then
      # FIX21: The portable MySQL instance was initialized with --initialize-insecure.
      # Administer exactly that instance via its private Unix socket and its matching
      # client. Never fall through to MacPorts/Homebrew/root/sudo authentication.
      info "FIX22 DB | admin=portable-socket | client=$ADMIN_CLIENT | socket=$PORTABLE_DB_SOCKET | user=root"
      "$ADMIN_CLIENT" --defaults-file="$PORTABLE_DB_BASE/my.cnf" --protocol=SOCKET --socket="$PORTABLE_DB_SOCKET" --user=root --skip-password < "$ADMIN_SQL" 2>"$PORTABLE_DB_BASE/admin-error.log" || {
        warn "FIX22 DB | portable Root-/Socket-Administration fehlgeschlagen; Details: $PORTABLE_DB_BASE/admin-error.log"
        rm -f "$ADMIN_SQL"
        fail "Portable MySQL-Runtime ist gestartet, aber die GAM-Datenbank konnte ueber ihren lokalen Socket nicht angelegt werden."
      }
      info "FIX22 DB | admin=portable-socket | result=ok"
    else
      # Existing Homebrew/MacPorts MariaDB path remains unchanged.
      "$ADMIN_CLIENT" -u root < "$ADMIN_SQL" 2>/dev/null ||       "$ADMIN_CLIENT" < "$ADMIN_SQL" 2>/dev/null ||       sudo "$ADMIN_CLIENT" -u root < "$ADMIN_SQL" 2>/dev/null ||       sudo -u _mysql "$ADMIN_CLIENT" -u root < "$ADMIN_SQL" 2>/dev/null ||       { rm -f "$ADMIN_SQL"; fail "MariaDB ist gestartet, aber die lokale Root-/Socket-Administration konnte die GAM-Datenbank nicht anlegen."; }
    fi
  elif [ "$OS_NAME" = "Linux" ]; then
    # Debian/Ubuntu nutzen haeufig unix_socket fuer root; bei anderen Distributionen
    # funktioniert oft mariadb -u root direkt. Beide Wege werden robust probiert.
    sudo "$ADMIN_CLIENT" < "$ADMIN_SQL" 2>/dev/null ||       sudo "$ADMIN_CLIENT" -u root < "$ADMIN_SQL" 2>/dev/null ||       "$ADMIN_CLIENT" -u root < "$ADMIN_SQL" 2>/dev/null ||       { rm -f "$ADMIN_SQL"; fail "MariaDB ist gestartet, aber die lokale Root-/Socket-Administration konnte die GAM-Datenbank nicht anlegen."; }
  else
    rm -f "$ADMIN_SQL"
    fail "Lokale Datenbankadministration fuer $OS_NAME wird nicht unterstuetzt."
  fi
  rm -f "$ADMIN_SQL"
  touch "$ENV_FILE"
  ENV_TMP="$(mktemp "${TMPDIR:-/tmp}/gam-env.XXXXXX")"
  sed '/^GAM_DB_URL=/d;/^GAM_DB_NAME=/d;/^GAM_DB_USER=/d;/^GAM_DB_PASSWORD=/d;/^GAM_DB_PORT=/d' "$ENV_FILE" > "$ENV_TMP"
  cat "$ENV_TMP" > "$ENV_FILE"
  rm -f "$ENV_TMP"
  {
    echo "GAM_DB_URL=jdbc:mariadb://127.0.0.1:$DB_PORT/$DB_NAME"
    echo "GAM_DB_NAME=$DB_NAME"
    echo "GAM_DB_USER=$DB_USER"
    echo "GAM_DB_PASSWORD=$DB_PASSWORD"
    echo "GAM_DB_PORT=$DB_PORT"
  } >> "$ENV_FILE"
  info "Lokaler GAM-Datenbankbenutzer wurde eingerichtet."
fi

CLIENT="$(find_db_client || true)"
if [ -z "$CLIENT" ]; then
  warn "MariaDB/MySQL ist erreichbar, aber kein lokaler DB-Client (mariadb/mysql) wurde gefunden."
  info "Schema-Validierung wird uebersprungen. Das Backend verbindet sich direkt per JDBC (eigener Treiber im Backend enthalten)."
  exit 0
fi
ARGS=(-h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" --default-character-set=utf8mb4)
[ -n "$DB_PASSWORD" ] && ARGS+=("-p$DB_PASSWORD")

"$CLIENT" "${ARGS[@]}" -e "CREATE DATABASE IF NOT EXISTS \`$DB_NAME\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>/dev/null || fail "Zieldatenbank konnte nicht angelegt oder geprueft werden."
[ -f "$SCHEMA_FILE" ] || fail "Leeres GAM-Schema fehlt: $SCHEMA_FILE"


EXPECTED_TABLES=()
while IFS= read -r table; do
  [ -n "$table" ] && EXPECTED_TABLES+=("$table")
done < <(grep -iE '^CREATE TABLE `[^`]+`' "$SCHEMA_FILE" | sed -E 's/^CREATE TABLE `([^`]+)`.*/\1/' | sort -u)
[ "${#EXPECTED_TABLES[@]}" -gt 0 ] || fail "Aus der Schemadatei konnten keine erwarteten Tabellen ermittelt werden."

missing_tables(){
  local table exists
  for table in "${EXPECTED_TABLES[@]}"; do
    exists="$("$CLIENT" "${ARGS[@]}" -Nse "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$DB_NAME' AND table_name='${table//\'/\'\'}';" 2>/dev/null || echo 0)"
    [ "$exists" = "1" ] || printf '%s\n' "$table"
  done
}

MISSING=()
while IFS= read -r table; do
  [ -n "$table" ] && MISSING+=("$table")
done < <(missing_tables)
if [ "${#MISSING[@]}" -gt 0 ]; then
  info "Datenbankschema ist unvollstaendig. Fehlende Tabellen: ${MISSING[*]}"
  # Der GAM-Benutzer besitzt absichtlich nur Rechte auf die GAM-Datenbank.
  # Deshalb erfolgt die Reparatur direkt und selektiv in der Zieldatenbank:
  # Nur CREATE-/ALTER-Anweisungen der tatsaechlich fehlenden Tabellen werden
  # aus dem Basisschema uebernommen. Vorhandene Tabellen und Daten bleiben unberuehrt.
  REPAIR_SQL="$(mktemp "${TMPDIR:-/tmp}/gam-schema-repair.XXXXXX.sql")"
  MISSING_FILE="$(mktemp "${TMPDIR:-/tmp}/gam-missing-tables.XXXXXX.txt")"
  cleanup_repair(){ rm -f "$REPAIR_SQL" "$MISSING_FILE"; }
  trap cleanup_repair EXIT
  printf '%s\n' "${MISSING[@]}" > "$MISSING_FILE"

  {
    echo 'SET FOREIGN_KEY_CHECKS=0;'
    awk -v missing_file="$MISSING_FILE" '
      BEGIN {
        while ((getline table < missing_file) > 0) missing[table] = 1
        close(missing_file)
        RS = ";"
        ORS = ";\n"
      }
      {
        statement = $0
        for (table in missing) {
          create_plain = "CREATE TABLE `" table "`"
          create_safe  = "CREATE TABLE IF NOT EXISTS `" table "`"
          alter_plain  = "ALTER TABLE `" table "`"
          if (index(statement, create_plain) || index(statement, create_safe) || index(statement, alter_plain)) {
            sub(create_plain, create_safe, statement)
            print statement
            break
          }
        }
      }
    ' "$SCHEMA_FILE"
    echo 'SET FOREIGN_KEY_CHECKS=1;'
  } > "$REPAIR_SQL"

  SELECTED_COUNT="$(grep -ciE '^[[:space:]]*(CREATE TABLE|ALTER TABLE)' "$REPAIR_SQL" || true)"
  [ "$SELECTED_COUNT" -gt 0 ] || fail "Aus dem Basisschema konnten keine Reparaturanweisungen fuer die fehlenden Tabellen erzeugt werden."
  info "Trage fehlende Tabellen direkt in '$DB_NAME' nach ($SELECTED_COUNT Schemaanweisungen, keine temporaere Datenbank erforderlich)..."
  "$CLIENT" "${ARGS[@]}" "$DB_NAME" < "$REPAIR_SQL" || fail "Fehlende GAM-Tabellen konnten nicht direkt in der Zieldatenbank nachgetragen werden."
  cleanup_repair
  trap - EXIT
else
  info "Alle ${#EXPECTED_TABLES[@]} Tabellen aus dem GAM-Basisschema sind vorhanden."
fi

# gam_settings wird von mehreren Modulen benoetigt, gehoert aber nicht zum historischen Basisschema.
"$CLIENT" "${ARGS[@]}" "$DB_NAME" -e "CREATE TABLE IF NOT EXISTS gam_settings (setting_key VARCHAR(120) NOT NULL, setting_value TEXT NULL, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, updated_by VARCHAR(255) NULL, PRIMARY KEY(setting_key)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;" || fail "Tabelle gam_settings konnte nicht angelegt oder validiert werden."

STILL_MISSING=()
while IFS= read -r table; do
  [ -n "$table" ] && STILL_MISSING+=("$table")
done < <(missing_tables)
[ "${#STILL_MISSING[@]}" -eq 0 ] || fail "Schema bleibt unvollstaendig. Noch fehlend: ${STILL_MISSING[*]}"
for core in accounts news rechnungsgesellschaft gam_settings; do
  exists="$("$CLIENT" "${ARGS[@]}" -Nse "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$DB_NAME' AND table_name='$core';")"
  [ "$exists" = "1" ] || fail "Kern-Tabelle '$core' fehlt nach der Datenbankinitialisierung."
done
COUNT="$("$CLIENT" "${ARGS[@]}" -Nse "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$DB_NAME';")"
info "GAM-Datenbankschema erfolgreich validiert ($COUNT Tabellen, alle Kern-Tabellen vorhanden)."
