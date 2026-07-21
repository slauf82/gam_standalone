# GAM 2.1.0 Preview 2 – Update 3

- Erkennt installierte JDKs ausdrücklich unter `C:\Program Files\Java\jdk-*`, einschließlich `jdk-21.0.10`.
- Prüft zusätzlich `ProgramW6432`, `ProgramFiles`, `ProgramFiles(x86)`, `JAVA_HOME`, PATH und Registry.
- Verwendet intern keine missverständliche Eigenschaft `Home` mehr, sondern `JdkHome`.
- Eine fehlgeschlagene Komforterkennung blockiert den Start nicht, wenn `java` und `javac` bereits funktionsfähig über PATH erreichbar sind.
