# fix44 – Java-Compilerwarnungen (gezielte Bereinigung)

- WindowsRemoteInventoryService.java: veralteten Builder-Aufruf `setAuthenticationScheme(authScheme)` durch die in winrm4j 0.12.3 dokumentierte fluente API `authenticationScheme(authScheme)` ersetzt. Der Authentifizierungswert und die Legacy-Fallback-Abläufe wurden nicht verändert.
- DiscoveryRegistrationRepository.java: unverändert, weil ohne `-Xlint:unchecked`-Detailausgabe und vollständigen Maven-Compilerlauf keine verifizierbare Warnungsursache festgestellt werden konnte. Keine Warnungsunterdrückung.
- Funktionale Gerätelogik, Apple-Code, Merge-Schutz aus fix43 und Node-Cache bleiben unverändert.

## Verifikation auf Entwicklerrechner

```bash
cd backend
mvn clean compile -Dmaven.compiler.showWarnings=true '-Dmaven.compiler.compilerArgs=-Xlint:deprecation,-Xlint:unchecked'
```

Das ZIP wurde auf CRC geprüft. Mangels Maven und vollständiger Abhängigkeiten konnte hier keine Java-Kompilierung durchgeführt werden.
