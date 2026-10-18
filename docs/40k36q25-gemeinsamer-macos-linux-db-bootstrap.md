# 40k36q25 – Gemeinsamer macOS-/Linux-Datenbank-Bootstrap

- `ensure-gam-database.sh` bleibt bewusst ein gemeinsames Skript.
- OS-spezifische Teile sind jetzt klar getrennt:
  - `install_mariadb_macos()`
  - `start_mariadb_macos()`
  - `install_mariadb_linux()`
  - `start_mariadb_linux()`
- Gemeinsame Logik bleibt gemeinsam:
  - Portprüfung
  - DB-Client-Erkennung
  - GAM-Datenbank und Benutzer anlegen
  - `.env` schreiben
  - Schema prüfen/reparieren
  - Kern-Tabellen validieren

## macOS
- Homebrew wird bei Bedarf automatisch installiert.
- MariaDB wird über `brew install mariadb` bereitgestellt.
- Dienststart über `brew services start mariadb`.
- Client-Fallback über `brew --prefix mariadb`.
- lokale Root-/Socket-Administration ohne `sudo mariadb`.

## Linux
- Paketmanager werden automatisch erkannt:
  - apt/apt-get
  - dnf
  - yum
  - pacman
- MariaDB-Server und -Client werden passend installiert.
- Dienststart über `systemctl`, mit `service` als Fallback.
- Für Arch/pacman wird bei leerem Datenverzeichnis `mariadb-install-db` vorbereitet.
- Root-/Socket-Administration probiert mehrere distributionsübliche Wege.
- Bei Startfehlern wird ein kurzer systemd-Status ausgegeben.

## Portabilität
- Keine Verwendung von `mapfile`.
- Kein Linux-spezifisches `sed -i`.
- Bash-3.2-kompatible Arrays/Schleifen für macOS.
- Shell-Syntaxprüfung für alle relevanten Start-/Java-/DB-Skripte.
