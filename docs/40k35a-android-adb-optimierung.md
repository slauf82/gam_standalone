# Schritt 40k35a – Android-ADB-Optimierung

## Ziel

Die Android-Inventarisierung soll auch bei großen ADB-Ausgaben zuverlässig durchlaufen, keine unnötigen Timeout-Wartezeiten erzeugen und optionale fehlende Paketdetails verständlich melden.

## Änderungen

- Der stdout-/stderr-Reader beendet das Lesen nicht mehr beim Erreichen des Speicherlimits.
- Nach Erreichen des Limits wird die Prozessausgabe weiter bis EOF geleert, ohne weitere Daten zu speichern.
- Dadurch kann der ADB-Prozess sauber enden und blockiert nicht mehr am gefüllten Betriebssystem-Puffer.
- Kleine Systemabfragen verwenden kürzere, passende Zeitlimits; nur große Abfragen erhalten mehr Zeit.
- Zeitüberschreitungen und begrenzte Ausgaben werden als konkrete Teilhinweise gesammelt.
- Die Paketabfrage verwendet zuerst `dumpsys package packages` und fällt bei Bedarf auf `dumpsys package` zurück.
- Für Paketdetails stehen bis zu 8 MB Auswertungspuffer zur Verfügung; darüber hinaus wird weiter bis EOF gelesen.
- Vollständig erkannte Paketlisten mit nur optional fehlenden Detailfeldern werden als `ERFOLGREICH_MIT_HINWEISEN` bewertet.
- Entfernte Apps werden anhand der vollständigen Paketliste erkannt und nicht mehr von optionalen Detaildaten abhängig gemacht.
- Windows- und Linux-Startskripte setzen UTF-8 für Java-Ausgabe; Spring-Logging nutzt UTF-8 für Konsole und Datei.

## Erwartetes Testbild

- Die bisher wiederholt auftretenden 15-Sekunden-Timeouts sollten weitgehend oder vollständig verschwinden.
- Die Systeminventarisierung sollte deutlich schneller als die zuvor gemessenen rund 81 Sekunden sein.
- Die App-Inventarisierung darf bei großen Ausgaben weiterlaufen und alle erkannten Apps speichern.
- Bei fehlenden optionalen Details erscheint eine konkrete Hinweis-Meldung statt einer pauschalen Teilinventarisierung.
- Umlaute sollten nach Start über die mitgelieferten Skripte korrekt im Terminal und in der Logdatei erscheinen.
