# Geänderte Dateien 40k36q

- `backend/pom.xml` – Apache MINA SSHD 2.19.0.
- `backend/src/main/java/de/kopfzentrum/gam/inventory/EmbeddedSshClientService.java` – eingebetteter Java-SSH-Client.
- `backend/src/main/java/de/kopfzentrum/gam/inventory/LinuxNetworkDiscoveryService.java` – Linux-SSH auf eingebetteten Client umgestellt.
- `backend/src/main/java/de/kopfzentrum/gam/inventory/MacOsInventoryService.java` – macOS-SSH auf eingebetteten Client umgestellt.
- `backend/src/main/java/de/kopfzentrum/gam/inventory/BrowserClientDiscoveryRepository.java` – persistente halblokale Browser-Clients.
- `backend/src/main/java/de/kopfzentrum/gam/inventory/BrowserClientDiscoveryController.java` – Browser-Client-Registrierung.
- `backend/src/main/java/de/kopfzentrum/gam/inventory/DeviceDiscoveryService.java` – Browser-Clients in Discovery-Pipeline integriert.
- `backend/src/main/java/de/kopfzentrum/gam/inventory/BuiltinDiscoverySettingsRepository.java` – Browsererkennung als schaltbare Discovery-Quelle.
- `frontend/src/api/client.ts` – Browser-Client-API.
- `frontend/src/main.tsx` – Erfassung verfügbarer Browser-/Plattformdaten und neue Discovery-Quelle.
- `docs/VERSION-SCHRITT.txt` – Versionsstand 40k36q.
