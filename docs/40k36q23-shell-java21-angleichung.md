# 40k36q23 – Shell-/Java-21-Angleichung

Dieser Schritt ist ausschließlich die Vorbereitung für den anschließenden macOS-backend-lokal-Stand 40k36r.

- `ensure-java.sh` an die robustere Windows-/PowerShell-Logik angeglichen.
- Java 21 wird auf macOS/Linux zusätzlich über die standardisierte JDK-Datei `release` validiert.
- macOS erkennt installierte JDKs zusätzlich über `/usr/libexec/java_home`.
- Apple Silicon (`arm64`) wird als Adoptium `aarch64`, Intel (`x86_64`) als `x64` behandelt.
- Download-Archiv wird vor Installation geprüft.
- Heruntergeladenes JDK wird bereits im Temp-Verzeichnis validiert.
- Ausführungsrechte für `java`/`javac` werden nach dem Entpacken abgesichert.
- Neues gemeinsames `scripts/activate-java.sh` setzt verbindlich `JAVA_HOME` und `PATH`.
- `start_backend.sh`, `scripts/run-backend-local.sh` und `scripts/build-backend.sh` verwenden denselben Java-Aktivierungsweg.
- `runtime/java-home.txt` ist damit auch unter macOS/Linux die gemeinsame Quelle für das ausgewählte JDK.
- Aussagekräftigere Diagnosen bei fehlgeschlagener Java-Einrichtung.
- Der eigentliche macOS-backend-lokal-Ausbau folgt als 40k36r.
