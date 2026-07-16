# GAM 2.1.0 Preview 2 – JDK-Erkennungsupdate

Die Java-Erkennung beim Backendstart wurde überarbeitet.

## Korrektur

Ein bereits installiertes JDK 21 wird nun auch erkannt, wenn `javac` nicht separat im globalen `PATH` eingetragen ist.

## Unterstützte Fundwege

1. lokales GAM-JDK unter `runtime/java`
2. `JAVA_HOME`
3. `java` oder `javac` aus dem `PATH`
4. Windows-Java-Registry
5. typische Installationsverzeichnisse von Temurin, Oracle, Microsoft OpenJDK, Corretto, Zulu und Liberica
6. macOS `/usr/libexec/java_home`
7. typische Linux-JDK-Verzeichnisse

Entscheidend sind ein vorhandenes `java` und `javac` ab Hauptversion 21. Der Hersteller des JDK ist unerheblich.

Das erkannte JDK wird in `runtime/java-home.txt` festgehalten und vom Backendstart gezielt verwendet. Nur wenn kein geeignetes JDK gefunden wird, richtet GAM Eclipse Temurin 21 lokal ein.
