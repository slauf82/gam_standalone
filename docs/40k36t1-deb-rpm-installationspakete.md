# 40k36t1 – DEB-/RPM-Installationspakete

Zusätzlich zum installierten Linux-Paketbestand erkennt GAM nun bereitliegende `.deb`- und `.rpm`-Installationsdateien.

- eigene Quellen `DEB-Pakete` und `RPM-Pakete`
- getrennt von `APT / dpkg` bzw. `RPM / DNF / YUM`
- Paketname, Version, Architektur und Dateipfad werden soweit möglich aus dem Paket gelesen
- typische Ablageorte `/home`, `/root`, `/tmp` und `/var/tmp`, begrenzte Suchtiefe und maximal 200 Treffer je Format
- eine vorhandene Datei wird neutral als Installationspaket inventarisiert; GAM behauptet daraus keine Installationsabsicht
- zugleich wurde der 40k36t-Transport der mehrzeiligen Softwareliste korrigiert: die komplette Softwareausgabe wird nun als ein SSH-Inventarfeld übertragen
