# Schritt 40k1 – PostMapping-Import-Fix

Behoben wurde ein Backend-Compilefehler in `InventoryController.java`.

## Änderung

Ergänzt wurde der fehlende Import:

```java
import org.springframework.web.bind.annotation.PostMapping;
```

Damit kann der Discovery-Endpunkt `POST /api/inventory/discovery/scan` wieder kompiliert werden.
