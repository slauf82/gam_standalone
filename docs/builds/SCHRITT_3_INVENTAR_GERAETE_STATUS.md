# Schritt 3 – Inventar/Geräte

Stand: Modulrahmen für Inventar/Geräte wurde ergänzt.

## Enthalten

- Backend-Package `de.kopfzentrum.gam.inventory`
- Geräteübersicht aus beiden Bestandswelten:
  - alte Tabelle `geräte`
  - neue Tabelle `geräte_neu`
- Filial-/Standortbezug:
  - `filiale`
  - `filiale_geräte_neu`
  - `rechnungsgesellschaft`
- Detail-API für Geräte
- Material-/Verbrauchsmaterial-Verknüpfung für `geräte_neu_vmaterial`
- Statistik-API für Altgeräte, neue Geräte, Filialzuordnungen, Medizin-/Elektrogeräte und außer Betrieb gesetzte Geräte
- Frontend-Seite `Inventar/Geräte`
- Suche nach Name, Typ, Seriennummer, Inventarnummer, Hersteller, Standort, Filiale
- Filter nach Datenquelle: alle / Altbestand / Geräte neu
- Option „nur aktive Geräte“

## Neue APIs

```text
GET /api/inventory/devices?q=&source=all&activeOnly=false&limit=100
GET /api/inventory/devices/{source}/{id}
GET /api/inventory/stats
```

## Bewusste Kompatibilitätsentscheidung

Die alten Tabellen werden nicht zusammengeführt oder bereinigt. GAM 2.0 liest zunächst beide Strukturen parallel:

```text
geräte       = historischer Geräte-/Inventarbestand
geräte_neu   = neuerer Gerätebestand, u. a. Drucker/Standort/IP
```

Damit bleibt die alte Logik erhalten, aber die UI kann beide Welten einheitlich anzeigen.

## Noch offen für spätere Schritte

- Geräte neu anlegen/bearbeiten/löschen
- Medizinprodukte-spezifische Prüf-/Einweisungslogik
- Verknüpfung mit `einweisung`, `kontrolle`, `inbetriebnahme`
- Vollständige Historie
- Software-/Arbeitsplatz-Verknüpfung aus `arbeitsplatz`/`software`
- Exportfunktionen

## Testhinweis

Das Paket wurde strukturell erzeugt. Ein Maven-/Node-Build wurde in dieser Umgebung nicht ausgeführt.
