# GAM 2.1.0 Preview 2 – Update 5

- Behebt die fehlerhafte Ablehnung vorhandener JDK-21-Installationen unter Windows.
- Liest die Java-Version primär aus der standardisierten JDK-Datei `release`.
- Nutzt `java -version` und `javac -version` nur noch ergänzend.
- Erkennt explizit JDK-21-Ordner unter `C:\Program Files\Java` und `C:\Program Files (x86)\Java`.
- Akzeptiert außerdem Pfade, die versehentlich direkt auf `bin` oder `java.exe` zeigen.
