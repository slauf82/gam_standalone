# Schritt 38g – Bestelltool nach GDS

## Ziel

Das historische GAM-Modul **Bestelltool** wird aus der reinen Lesemodus-Shell herausgelöst und als nutzbares GDS-Modul vorbereitet.

## Inhalt

- Bestelltool als eigenes Modul in der Navigation
- Bedarfsliste aus Lager/Verbrauchsmaterial
- Meldebestand und Zielbestand einstellbar
- Suche über Material, Eigenschaften und Hersteller-E-Mail
- Bestellentwürfe mit modalem Dialog
- Entwurfsstatus: Entwurf, bestellt, geliefert, erledigt, storniert
- Bestelltext für E-Mail/Weiterverarbeitung kopierbar
- Sticky Toolbar, GamDialog, GamScrollArea und Toast-Meldungen

## Hinweis

Die Bestellentwürfe werden in diesem Schritt bewusst lokal im Browser gespeichert. Die Lagerbestände werden nicht verändert. Eine spätere Backend-/Datenbankpersistenz kann darauf aufbauen.

## Build

Frontend-Quellcode wurde aktualisiert. Paketstandard: vollständiger Quellcode ohne `node_modules`, `dist`, `target`, `.git` und Cache-Verzeichnisse.
