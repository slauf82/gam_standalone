# 40k36t6

## Linux-Softwarepersistierung
Der t5-Realtest zeigte:
- Linux-Erfassung komplett: ca. 14,3 s
- Identität speichern: 165 ms
- 3742 Softwareeinträge speichern: 373770 ms

Ursache: `JdbcTemplate.batchUpdate()` ist je nach JDBC-Treiber/URL ohne
`rewriteBatchedStatements` kein echter Netzwerk-Bulk und kann tausende einzelne
INSERT/UPSERT-Ausführungen erzeugen.

t6 verwendet deshalb explizite SQL-Multirow-INSERTs mit bis zu 250 Datensätzen
pro Statement. Das ist unabhängig von JDBC-Treiberoptionen ein echter Bulk-Pfad.
Doppelte Software-Schlüssel werden vor dem Schreiben innerhalb des Laufs
zusammengefasst.

## Windows-Startinventarisierung
`Windows ARP` bzw. ein anderer schwacher Texttreffer allein startet nicht mehr
automatisch einen WinRM-Lauf. Windows wird beim Start automatisch inventarisiert,
wenn entweder starke Windows-Inventarevidenz vorhanden ist (lokale
Windows-Identität/WMI/PowerShell/WinRM/Windows-Inventarisierung) oder für die
Zieladresse tatsächlich ein WinRM-Zugang konfiguriert ist.
