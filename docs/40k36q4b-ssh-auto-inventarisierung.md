# 40k36q4b – SSH als echte Quelle + automatische Inventarisierung

- Erfolgreicher SSH-Verbindungstest startet automatisch die Plattformbestimmung.
- Darwin → macOS-SSH-Inventarisierung; Linux → Linux-SSH-Inventarisierung.
- Erst bei tatsächlich gelieferten Inventardaten wird `SSH · macOS` bzw. `SSH · Linux` als bestätigte Quelle gespeichert.
- Gilt sowohl für den gemeinsamen SSH-Testhost als auch für q3-Mehrfachzugänge.
- Gerätemanager besitzt ein eigenes SSH-Quellenbadge.
- Verbindungserfolg und Inventarisierungserfolg werden in der Testmeldung getrennt kenntlich gemacht.
