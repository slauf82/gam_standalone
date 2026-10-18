# 40k36s – macOS Softwarequellen

Erweitert den bestehenden macOS-Softwarebestand für gezielte Tests auf echter Apple-Hardware.

Erfasste Quellen:
- normale `.app`-Bundles unter `/System/Applications`, `/Applications` und Benutzer-`Applications`;
- Mac-App-Store-Apps nativ über `_MASReceipt`, also auch ohne Zusatztool;
- Installer-Pakete über `pkgutil`;
- Homebrew Formulae;
- Homebrew Casks;
- seltene benutzerspezifische Homebrew-Präfixe.

Optional:
- Wenn `mas` installiert ist, werden `mas version`, Anzahl und Rohdetails von `mas list` zusätzlich diagnostisch erfasst.
- Fehlt `mas`, funktioniert die native Mac-App-Store-Erkennung weiterhin.

Für den Test sind Quellenzähler sichtbar, damit sich Installieren/Entfernen einer App oder eines Brew-Pakets direkt vergleichen lässt.
