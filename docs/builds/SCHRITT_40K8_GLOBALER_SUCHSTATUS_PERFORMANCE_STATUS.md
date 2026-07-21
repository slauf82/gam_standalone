# Schritt 40k8 – Globaler Suchstatus und Discovery-Performance

- Die Schaltfläche bleibt bis zum vollständigen Ende des HTTP-Discovery-Streams auf „Suche läuft weiter …“.
- Das `complete`-Ereignis beendet den UI-Zustand nicht mehr vorzeitig.
- Erst nach geschlossenem und vollständig gelesenem Stream werden 100 % und „Geräte suchen“ gesetzt.
- Zwei Renderzyklen stellen sicher, dass alle zuvor übertragenen Gerätetreffer sichtbar übernommen wurden.
- Der aktive Subnetzscan blockiert nicht mehr pro Treffer durch Reverse-DNS-Namensauflösung.
- Der Erreichbarkeitstimeout wurde vorsichtig von 350 auf 250 ms reduziert.
- Soforttreffer aus der Nachbartabelle bleiben unverändert vollständig.
