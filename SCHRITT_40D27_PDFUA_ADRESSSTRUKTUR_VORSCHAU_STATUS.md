# Schritt 40d27 – PDF/UA-, Adressstruktur- und Vorschau-Fix

- Firmenname im Rechnungskopf nicht mehr überschriftenähnlich formatiert.
- Adresszeilen werden als Blockabsätze ausgegeben; sichtbares `<br/>` entfällt.
- Empfängeradresse in Zahlungsdokumenten verwendet keine impliziten Span-Strukturen mehr.
- OpenHTMLtoPDF-Generalwarnungen werden auch im lokalen Profil auf ERROR begrenzt.
- Ohne ausgewählte Rechnung zeigt die Suche die aktuelle LBD-Vorschau; bei Auswahl wird sie sofort durch gespeicherte Rechnungsdaten ersetzt.
