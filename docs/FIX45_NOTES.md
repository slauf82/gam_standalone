# GAM 2.1.0 – fix45: DiscoveryRegistrationRepository unchecked warning

- Entfernt die zwei rohen `Comparable`-Verwendungen in `earlier()` und `later()`.
- Vergleicht JDBC-TIMESTAMP/Date und LocalDateTime typsicher.
- Kein `@SuppressWarnings`, keine Änderungen an WinRM, Apple-Erkennung oder Merge-Kandidatenprüfung.
- `javac -Xlint:unchecked` erfolgreich gegen schmale lokale Spring/Jakarta-API-Stubs, keine unchecked-Warnungen dieser Quelldatei.
- Vollständiger Maven-Build in dieser Umgebung nicht ausgeführt; auf Zielsystem verifizieren.
