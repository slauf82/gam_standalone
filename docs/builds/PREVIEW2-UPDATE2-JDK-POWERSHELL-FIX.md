# GAM 2.1.0 Preview 2 – Update 2

## Behoben

- PowerShell-Konflikt mit der schreibgeschützten Systemvariable `$HOME` in `scripts/ensure-java.ps1` behoben.
- Der Parameter der JDK-Prüfung heißt nun eindeutig `JdkHomePath`.
- Vorhandene JDK-21-Installationen können dadurch wieder korrekt über `JAVA_HOME`, `PATH`, Registry und Standardinstallationsordner geprüft werden.
- Ein unnötiger Fallback-Download wird vermieden, wenn bereits ein geeignetes JDK vorhanden ist.
