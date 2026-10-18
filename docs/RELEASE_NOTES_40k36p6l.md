# 40k36p6l – WinRM Java-21-Kompatibilität

- Ergänzt die von winrm4j/CXF benötigte `javax.xml.bind`-API und JAXB-Runtime für Java 21.
- Verhindert, dass ein fehlender WinRM-Laufzeitbaustein den REST-Aufruf ungefangen mit HTTP 500 beendet.
- Liefert stattdessen eine verständliche WinRM-Diagnose im Frontend.
- Behält winrm4j 0.12.3 bei, da keine neuere Version in Maven Central veröffentlicht ist.
