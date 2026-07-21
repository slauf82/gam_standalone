# Schritt 40k7 – Sofort-Discovery und Deregistrierung

- Nachbartabellen werden als schnelle Sofortquelle zuerst vollständig ausgewertet.
- Hostnamenauflösung blockiert die ersten Treffer nicht mehr.
- Vertiefter Subnetzscan, zweite Nachbarauswertung und SSDP laufen anschließend weiter.
- Zähler werden ausschließlich aus der deduplizierten Ergebnisliste berechnet.
- LOCAL-, ARP- und weitere Treffer derselben IP/MAC werden zusammengeführt.
- Registrierte Geräte können deregistriert oder deregistriert und ignoriert werden.
