# Schritt 33h – MaryTTS Transitive-Dependency-Repository-Fix

## Problem

Nach Schritt 33g waren die MaryTTS-Hauptartefakte auf `5.2.1` korrigiert.
Der Build scheiterte danach noch an drei transitiven Legacy-Abhängigkeiten:

* `com.twmacinta:fast-md5:2.7.1`
* `gov.nist.math:Jampack:1.0`
* `de.dfki.lt.jtok:jtok-core:1.9.3`

Diese liegen nicht in Maven Central.

## Fix

Gezielt ergänzt wurden Repositories für diese Alt-Abhängigkeiten:

* XNAT Libs Release
* Terrestris Public Repository
* DFKI MLT Maven Repository
* Bookmap Maven Ext Mirror

Zusätzlich wurde `dependencyManagement` für die drei Artefakte ergänzt.

## Wichtig

Maven cached fehlgeschlagene Auflösungen. Bitte nutzen:

```bat
scripts\purge-marytts-failed-cache-and-build.bat
```

oder manuell:

```bat
rmdir /s /q "%USERPROFILE%\.m2\repository\com\twmacinta\fast-md5"
rmdir /s /q "%USERPROFILE%\.m2\repository\gov\nist\math\Jampack"
rmdir /s /q "%USERPROFILE%\.m2\repository\de\dfki\lt\jtok\jtok-core"
mvn -U clean package
```
