# 40k36q24 – macOS Homebrew-/MariaDB-Bootstrap

- macOS erkennt fehlendes Homebrew und installiert es automatisch über den offiziellen Homebrew-Installer.
- Nach der Installation wird Homebrew aus `/opt/homebrew` (Apple Silicon) bzw. `/usr/local` (Intel) sofort in den laufenden Skript-PATH aufgenommen.
- MariaDB wird unter macOS automatisch über `brew install mariadb` installiert.
- Der Dienst wird mit `brew services start mariadb` gestartet.
- Bereits installierte MariaDB wird respektiert und nicht erneut installiert.
- MariaDB-Client wird zusätzlich direkt über `brew --prefix mariadb` gefunden.
- macOS verwendet für die lokale Administration keinen `sudo mariadb`-Aufruf mehr; stattdessen wird Root-/Socket-Administration des Homebrew-Dienstes verwendet.
- GAM-Datenbank und GAM-Benutzer werden anschließend automatisch erzeugt.
- `.env`-Aktualisierung funktioniert jetzt auch mit BSD/macOS `sed`; Linux-spezifisches `sed -i` wurde entfernt.
- `mapfile` wurde entfernt, damit das Skript mit dem von macOS mitgelieferten Bash 3.2 funktioniert.
- Bestehende Linux-Paketmanagerpfade (apt/dnf/yum/pacman) bleiben erhalten.
- Dieser Schritt bereitet den anschließenden macOS-backend-lokal-Test 40k36r vor.
