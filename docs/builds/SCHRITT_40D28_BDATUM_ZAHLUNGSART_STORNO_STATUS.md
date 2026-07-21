# Schritt 40d28 – BDATUM, Zahlungsart und Stornobezeichnung

- Vorschau verwendet bei bestehenden Rechnungen `BDATUM` aus `rechnungsdetails`.
- Zahlungsart wird aus `rechnungsdetails.ZAHLUNGSART` bis zum Vorschau-Panel durchgereicht.
- Unbekannte, aber vorhandene Zahlungsarten werden als gespeicherter Originalwert angezeigt.
- Originalrechnung mit gesetztem STORNO-Status: `Rechnung ... – bereits storniert`.
- Belegnummer mit S-Suffix: `Stornorechnung ...S`.
