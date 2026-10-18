# 40k36t43 – WinRM RESULT_META robust parsen

- Basis: 40k36t42.
- `LEN=...` wird jetzt mit einer Regex (`LEN\s*=\s*(\d+)`) aus der rohen WinRM-Ausgabe extrahiert.
- Dadurch stören CR/LF, zusätzliche Leerzeichen oder weitere Shell-Ausgabe die Längenbestimmung nicht mehr.
- Der bestehende blockweise Rücktransport kann danach fortgesetzt werden.
- Die im Log beobachteten `ReflectionServiceFactoryBean`-INFO-Zeilen werden gezielt auf WARN reduziert.
- Executable-Rechte für `*.sh`, `mvnw`, ggf. `gradlew` und `*.command` bleiben erhalten.
