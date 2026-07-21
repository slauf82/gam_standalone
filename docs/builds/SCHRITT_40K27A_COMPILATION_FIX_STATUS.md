# Schritt 40k27a – Compilation-Fix

## Behoben

- Doppelte Definition von `normalizeMac(String)` in `DeviceDiscoveryService` entfernt.
- Die bereits vorhandene robustere Implementierung bleibt erhalten. Sie behandelt `null` sicher und normalisiert MAC-Adressen unabhängig von Trennzeichen.
- Dadurch ist der Maven-Compilerfehler „Methode normalizeMac(java.lang.String) ist bereits definiert“ behoben.

## Funktionsumfang

Der Funktionsumfang von 40k27 bleibt unverändert:

- quellenübergreifende Identitätszusammenführung
- dauerhafte Speicherung registrierter Geräte
- manueller oder automatischer Registrierungsmodus
- gemeinsame Struktur für Altgeräte und registrierte Discovery-Geräte
