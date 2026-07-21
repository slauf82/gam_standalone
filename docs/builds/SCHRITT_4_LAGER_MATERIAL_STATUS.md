# GAM 2.0 – Schritt 4 Lager/Material

Stand: 2026-05-23

## Ziel

Das Lager-/Materialmodul ist jetzt als echter Modulbereich ergänzt. Es bleibt kompatibel zur bestehenden Datenbank und liest zunächst die vorhandenen Tabellen, statt neue Strukturen zu erzwingen.

## Enthalten

### Backend

Neues Package:

```text
de.kopfzentrum.gam.warehouse
```

Neue APIs:

```text
GET   /api/warehouse/items?q=&kind=all&onlyWithStock=false&limit=100
GET   /api/warehouse/items/{kind}/{id}
GET   /api/warehouse/stats
PATCH /api/warehouse/items/{kind}/{id}/stock
```

Verwendete Bestandstabellen:

```text
lager
verbrauchsmaterial
geräte_neu_vmaterial
```

### Frontend

- neuer Modulbereich „Lager/Material“
- Suche über Lagerartikel und Verbrauchsmaterial
- Filter:
  - alle
  - Lager
  - Verbrauchsmaterial
  - nur mit Bestand
- Statistik-Kacheln
- Detailansicht
- Bestandsmenge manuell setzen
- Anzeige von Geräte-Verknüpfungen bei Verbrauchsmaterial

## Kompatibilitätsentscheidung

Die vorhandenen Tabellen bleiben unverändert:

```text
lager.CODE / BESCHREIBUNG / LAGERORT / MENGE
verbrauchsmaterial.ID / Name / Eigenschaften / Anzahl / EMail_Hersteller
```

GAM 2.0 fasst diese Daten nur in einer gemeinsamen Oberfläche zusammen.

## Bewusst noch offen

- separates Bewegungs-/Audit-Journal für Lagerbuchungen
- automatische Mindestbestandswarnungen
- Bestellung/Nachbestellung
- Lieferantenmodul
- Barcode/QR-Code
- Verknüpfung von Lagerbewegungen mit Rechnungen
- feingranulare Rechte für Bestand ändern

## Nächster Schritt

Schritt 5: restliche GAM-Module als Rahmen und Priorisierung:

- Aufgaben/Freigaben
- Personal
- Reports/Exporte
- News/Themes/Ordnerfreigaben
- Kassenbuch/Zahlungsavis/Storno/Gutschrift
