# Schritt 40k9 – bestätigte Discovery und globaler Suchstatus

- Aktiver Subnetzscan erzeugt keine eigenständigen Geräte mehr.
- Ping/Reachability dient nur zum Aktualisieren der ARP-/Neighbor-Tabelle.
- Neue Geräte werden erst nach Bestätigung durch ARP/Neighbor mit MAC, lokalen Adapter oder SSDP/UPnP angezeigt.
- Dadurch werden die False Positives aus 40k8 (nahezu komplettes /24) verhindert.
- Der Button bleibt bis zum vollständig geschlossenen Stream aktiv und zeigt den Abschluss zusätzlich kurz sichtbar an.
- Soforttreffer aus der vorhandenen Nachbartabelle bleiben unverändert schnell.
