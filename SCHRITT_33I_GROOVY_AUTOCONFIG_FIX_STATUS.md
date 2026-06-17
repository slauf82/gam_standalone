# Schritt 33i – Groovy-Autoconfiguration-Fix

## Problem

Der Backend-Start scheiterte nach erfolgreicher MaryTTS-Abhängigkeitsauflösung an:

```text
Error creating bean with name 'groovyMarkupConfigurer'
GroovyTemplateAutoConfiguration
Unable to make private java.lang.Class(...) accessible
```

## Ursache

MaryTTS bringt transitiv `groovy-all-2.4.5` mit.
Spring Boot erkennt Groovy auf dem Classpath und aktiviert automatisch Groovy-Templates.
GAM nutzt aber keine Groovy-Templates.

## Fix

Die Groovy-Template-Autokonfiguration wurde deaktiviert:

```java
@SpringBootApplication(exclude = GroovyTemplateAutoConfiguration.class)
```

Zusätzlich wurde sie in YAML ausgeschlossen.

## Optionales Startscript

```text
scripts\run-backend-with-marytts-groovy-fix.bat
```

Dieses setzt zusätzlich:

```text
--add-opens=java.base/java.lang=ALL-UNNAMED
```

als Sicherheitsnetz.
