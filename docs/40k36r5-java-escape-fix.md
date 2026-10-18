# 40k36r5 – Java-Escape-Fix macOS-Hardwareanalyse

Korrigiert die in r4 vom Maven-Compiler gemeldeten drei unzulässigen Escapezeichen in `MacOsInventoryService.java`.

- Datenträger-RegEx: `\/dev\/disk` wurde durch eine Java-textblock-sichere awk-Schreibweise `[/]dev[/]disk` ersetzt.
- RAM-RegEx: `BANK ...\/DIMM` wurde durch `BANK ...[/]DIMM` ersetzt.
- Die Shell-Semantik bleibt unverändert.
- Isolierte `javac`-Prüfung auf Parser-/Escape-Fehler: 0 Treffer.
