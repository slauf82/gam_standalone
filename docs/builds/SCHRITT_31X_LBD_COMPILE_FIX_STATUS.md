# Schritt 31x – LBD Compile-Fix

## Fehler

Backend-Kompilierung schlug fehl:

```text
Symbol nicht gefunden: Methode findFirstLbdFile()
```

Betroffene Klassen:

- `StartupCheckController`
- `SystemStatusController`

## Ursache

Beim LBD-Fix wurde `findFile(...)` erweitert, aber die ältere Komfortmethode `findFirstLbdFile()` wurde nicht mehr bereitgestellt.

## Fix

`LbdService` enthält wieder:

```java
public Optional<Path> findFirstLbdFile() {
  try {
    return findFile(null);
  } catch (IOException e) {
    return Optional.empty();
  }
}
```

## Scan

```text
findFirstLbdFile vorhanden: True
```
