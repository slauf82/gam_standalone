# Schritt 40k30b – flexibler Registrierungsworkflow

## Verhalten

GAM unterstützt nun drei frei kombinierbare Ausbaustufen:

1. Gerät wird erkannt.
2. Gerät wird registriert.
3. Registriertes Gerät wird in die eigentliche Geräteliste übernommen.

Über zwei Einstellungen kann jeder Anwender selbst entscheiden, ob die Zwischenschritte manuell oder automatisch erfolgen:

- Erkannte Geräte automatisch registrieren
- Registrierte Geräte automatisch in die Geräteliste übernehmen

Ist die zweite Einstellung aktiv, wird die erste automatisch mit aktiviert. Damit entspricht das Verhalten dem Windows-Geräte-Manager: erkannt, registriert und direkt in die Geräteliste übernommen.

## Weitere Korrekturen

- Bereits registrierte Discovery-Treffer bleiben im Bereich „Registriert“, solange sie nicht in die Geräteliste übernommen wurden.
- Neue Schaltfläche „In Geräteliste übernehmen“.
- Deregistrierung ist wieder verfügbar.
- Registrierungen werden unabhängig von der Geräteliste in `gam_discovery_registered_devices` gespeichert.
- Der große Aufklapppfeil öffnet ausschließlich die technischen Geräteinformationen und nicht mehr parallel den Bearbeitungsdialog.
