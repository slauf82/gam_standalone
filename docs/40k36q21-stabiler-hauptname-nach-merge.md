# 40k36q21 – Stabiler Hauptname nach bestätigtem Merge

- Ein ausdrücklich bestätigter Geräte-Merge bestätigt zugleich den dabei gewählten Hauptnamen.
- Der zusammengeführte Zieldatensatz wird deshalb mit `manual_name=TRUE` gespeichert.
- Spätere Discovery-Treffer dürfen diesen Hauptnamen nicht mehr überschreiben.
- Temporäre/generische Discovery-Namen wie `macOS Browser 192.168.x.x` werden bei abweichendem Namen als Alias gespeichert.
- Manuelles Umbenennen über die bestehende Hauptnamensfunktion bleibt weiterhin möglich.
- Beispiel: Nach Merge bleibt `Admins Mac mini` Hauptname, auch wenn Browser-/WLAN-/Ethernet-Discovery später andere Bezeichnungen meldet.
