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

start_mariadb_service(){
  if command -v systemctl >/dev/null 2>&1; then
    sudo systemctl enable --now mariadb 2>/dev/null || \
      sudo systemctl enable --now mysql 2>/dev/null || \
      sudo systemctl start mariadb 2>/dev/null || \
      sudo systemctl start mysql 2>/dev/null || true
  elif command -v service >/dev/null 2>&1; then
    sudo service mariadb start 2>/dev/null || sudo service mysql start 2>/dev/null || true
  fi
}

wait_for_mariadb(){
  for _ in $(seq 1 30); do
    port_open && return 0
    sleep 1
  done
  return 1
}

install_mariadb(){
  info "MariaDB ist nicht erreichbar. Lokale Installation wird vorbereitet..."
  local package_status=0
  local package_manager=""

  # Paketmanager koennen wegen eines voellig fremden, bereits defekten Pakets
  # einen Fehlercode liefern, obwohl MariaDB selbst korrekt installiert wurde.
  # Deshalb wird der reale Systemzustand anschliessend separat validiert.
  if command -v apt-get >/dev/null 2>&1; then
    package_manager="apt-get"
    if ! sudo apt-get update; then
      warn "apt-get update meldete einen Fehler. Die MariaDB-Installation wird trotzdem versucht."
    fi
    sudo env DEBIAN_FRONTEND=noninteractive apt-get install -y mariadb-server mariadb-client || package_status=$?
  elif command -v dnf >/dev/null 2>&1; then
    package_manager="dnf"
    sudo dnf install -y mariadb-server mariadb || package_status=$?
  elif command -v yum >/dev/null 2>&1; then
    package_manager="yum"
    sudo yum install -y mariadb-server mariadb || package_status=$?
  elif command -v pacman >/dev/null 2>&1; then
    package_manager="pacman"
    sudo pacman -Sy --needed --noconfirm mariadb || package_status=$?
    if mariadb_components_present && [ ! -d /var/lib/mysql/mysql ] && command -v mariadb-install-db >/dev/null 2>&1; then
      sudo mariadb-install-db --user=mysql --basedir=/usr --datadir=/var/lib/mysql || true
    fi
  else
    fail "Kein unterstuetzter Paketmanager gefunden. Bitte MariaDB installieren und erneut starten."
  fi

  if [ "$package_status" -ne 0 ]; then
    warn "$package_manager beendete die Installation mit Fehlercode $package_status. Pruefe den tatsaechlichen MariaDB-Systemzustand..."
  fi

  if ! mariadb_components_present; then
    fail "MariaDB-Komponenten wurden nach dem Installationsversuch nicht gefunden. Bitte den Paketmanagerfehler beheben und erneut starten."
  fi

  start_mariadb_service
  if wait_for_mariadb; then
    if [ "$package_status" -ne 0 ]; then
      warn "MariaDB wurde trotz Paketmanagerfehler erfolgreich installiert und gestartet. Der fremde Paketfehler blockiert GAM nicht."
    else
      info "MariaDB wurde erfolgreich installiert und gestartet."
    fi
    return 0
  fi

  fail "MariaDB-Komponenten sind vorhanden, der Server ist aber unter $DB_HOST:$DB_PORT nicht erreichbar. Bitte den Dienststatus pruefen."
}

if ! port_open; then install_mariadb; else info "MariaDB/MySQL unter $DB_HOST:$DB_PORT erreichbar."; fi

# Bei einer lokalen Neuinstallation einen eigenen GAM-Benutzer erzeugen. Dadurch
# bleibt das distributionsabhaengige Root-/Unix-Socket-Login unangetastet.
if [ -z "$DB_USER" ]; then
  DB_USER="gam"
  if [ -z "$DB_PASSWORD" ]; then
    if command -v openssl >/dev/null 2>&1; then DB_PASSWORD="$(openssl rand -hex 16)"; else DB_PASSWORD="gam-$(date +%s)-$RANDOM"; fi
  fi
  if ! command -v mariadb >/dev/null 2>&1 && ! command -v mysql >/dev/null 2>&1; then
    fail "MariaDB-Client wurde nicht gefunden."
  fi
  ADMIN_CLIENT="$(command -v mariadb || command -v mysql)"
  ESC_PASS=${DB_PASSWORD//\'/\'\'}
  sudo "$ADMIN_CLIENT" <<SQL
CREATE DATABASE IF NOT EXISTS \`$DB_NAME\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS '$DB_USER'@'localhost' IDENTIFIED BY '$ESC_PASS';
CREATE USER IF NOT EXISTS '$DB_USER'@'127.0.0.1' IDENTIFIED BY '$ESC_PASS';
ALTER USER '$DB_USER'@'localhost' IDENTIFIED BY '$ESC_PASS';
ALTER USER '$DB_USER'@'127.0.0.1' IDENTIFIED BY '$ESC_PASS';
GRANT ALL PRIVILEGES ON \`$DB_NAME\`.* TO '$DB_USER'@'localhost';
GRANT ALL PRIVILEGES ON \`$DB_NAME\`.* TO '$DB_USER'@'127.0.0.1';
FLUSH PRIVILEGES;
SQL
  touch "$ENV_FILE"
  sed -i '/^GAM_DB_URL=/d;/^GAM_DB_NAME=/d;/^GAM_DB_USER=/d;/^GAM_DB_PASSWORD=/d;/^GAM_DB_PORT=/d' "$ENV_FILE"
  {
    echo "GAM_DB_URL=jdbc:mariadb://127.0.0.1:$DB_PORT/$DB_NAME"
    echo "GAM_DB_NAME=$DB_NAME"
    echo "GAM_DB_USER=$DB_USER"
    echo "GAM_DB_PASSWORD=$DB_PASSWORD"
    echo "GAM_DB_PORT=$DB_PORT"
  } >> "$ENV_FILE"
  info "Lokaler GAM-Datenbankbenutzer wurde eingerichtet."
fi

CLIENT="$(command -v mariadb || command -v mysql || true)"
if [ -z "$CLIENT" ]; then
  warn "MariaDB/MySQL ist erreichbar, aber kein lokaler DB-Client (mariadb/mysql) wurde gefunden."
  info "Schema-Validierung wird uebersprungen. Das Backend verbindet sich direkt per JDBC (eigener Treiber im Backend enthalten)."
  exit 0
fi
ARGS=(-h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" --default-character-set=utf8mb4)
[ -n "$DB_PASSWORD" ] && ARGS+=("-p$DB_PASSWORD")

"$CLIENT" "${ARGS[@]}" -e "CREATE DATABASE IF NOT EXISTS \`$DB_NAME\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>/dev/null || fail "Zieldatenbank konnte nicht angelegt oder geprueft werden."
[ -f "$SCHEMA_FILE" ] || fail "Leeres GAM-Schema fehlt: $SCHEMA_FILE"


mapfile -t EXPECTED_TABLES < <(grep -iE '^CREATE TABLE `[^`]+`' "$SCHEMA_FILE" | sed -E 's/^CREATE TABLE `([^`]+)`.*/\1/' | sort -u)
[ "${#EXPECTED_TABLES[@]}" -gt 0 ] || fail "Aus der Schemadatei konnten keine erwarteten Tabellen ermittelt werden."

missing_tables(){
  local table exists
  for table in "${EXPECTED_TABLES[@]}"; do
    exists="$("$CLIENT" "${ARGS[@]}" -Nse "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$DB_NAME' AND table_name='${table//\'/\'\'}';" 2>/dev/null || echo 0)"
    [ "$exists" = "1" ] || printf '%s\n' "$table"
  done
}

mapfile -t MISSING < <(missing_tables)
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

mapfile -t STILL_MISSING < <(missing_tables)
[ "${#STILL_MISSING[@]}" -eq 0 ] || fail "Schema bleibt unvollstaendig. Noch fehlend: ${STILL_MISSING[*]}"
for core in accounts news rechnungsgesellschaft gam_settings; do
  exists="$("$CLIENT" "${ARGS[@]}" -Nse "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$DB_NAME' AND table_name='$core';")"
  [ "$exists" = "1" ] || fail "Kern-Tabelle '$core' fehlt nach der Datenbankinitialisierung."
done
COUNT="$("$CLIENT" "${ARGS[@]}" -Nse "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$DB_NAME';")"
info "GAM-Datenbankschema erfolgreich validiert ($COUNT Tabellen, alle Kern-Tabellen vorhanden)."
