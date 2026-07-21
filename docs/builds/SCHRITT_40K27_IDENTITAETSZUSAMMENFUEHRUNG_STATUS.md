# Schritt 40k27 – Identitätszusammenführung

## Ziel
Mehrfach erkannte Geräte aus verschiedenen Discovery-Quellen werden bereits während der Suche zu einem gemeinsamen Gerät zusammengeführt.

## Umsetzung
- Identitätsabgleich über normalisierte MAC-Adresse
- zusätzlicher Abgleich über Serien-/Quellidentität
- zusätzlicher Abgleich über IPv4-Adresse
- vorsichtiger Fallback über Gerätename und kompatiblen Gerätetyp
- Quellen werden am gemeinsamen Gerät gesammelt
- spezifische Namen und Typen haben Vorrang vor generischen Discovery-Bezeichnungen
- Online- und Registrierungsstatus werden quellenübergreifend zusammengeführt
- aktualisierte zusammengeführte Geräte werden erneut an das Frontend gestreamt
- bestehende dauerhafte Registrierung bleibt unverändert erhalten

## Ausblick 40k28
40k28 erweitert das gemeinsame Gerät um eine aufklappbare Hardware- und Softwareinventarisierung. Bei Computern sind insbesondere Betriebssystem, installierte Software, CPU, RAM-Ausbau und Maximalbestückung, Massenspeicher sowie SMART-Werte vorgesehen. Soweit andere Quellen wie Home Assistant entsprechende Informationen liefern, werden diese ebenfalls am jeweiligen Gerät eingeblendet.

## Buildhinweis
Der Maven-Build konnte in der isolierten Umgebung nicht ausgeführt werden, weil kein lokales Maven vorhanden war und der externe Maven-Download nicht erreichbar war.
