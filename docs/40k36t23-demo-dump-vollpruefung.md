# 40k36t23 – Demo-Dump-Vollprüfung

Der First-Run-Demo-Dump wurde nach dem MariaDB-1064-Befund nicht nur an der sichtbaren Fehlerstelle, sondern vollständig strukturell geprüft.

## Reparaturen

1. `aufgaben`: beschädigter Übergang bei ID 1854/1855 repariert und ID 1855 als anonymisierter Demo-Datensatz ergänzt.
2. `einweisung`: beschädigter Übergang nach ID 160 repariert und ID 161 als anonymisierter Demo-Datensatz ergänzt.
3. Die Korrekturen wurden in den drei anonymisierten Demo-Dump-Varianten synchron gehalten.

## Prüfergebnis des tatsächlich verwendeten First-Run-Dumps

- 541 SQL-Statements
- 477 INSERT-Statements
- 50 DELETE-Statements
- 10 SET-Statements
- 1 USE, 1 START TRANSACTION, 1 COMMIT
- keine offenen Quotes
- keine unbekannten Statement-Fragmente
- alle INSERT-Tupel besitzen exakt die Anzahl Werte, die ihre jeweilige Spaltenliste verlangt
- maximales Einzelstatement: ca. 190 KiB

Die 8-MiB-Sicherheitsgrenze aus 40k36t22 bleibt aktiv und dient weiterhin als Schutz gegen erneut beschädigte oder unvollständige Dumps.
