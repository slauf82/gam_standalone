# Schritt 37h1 – PDF/UA-Lite: stabile Metadatenbasis

Basis:
- Schritt 37g: OpenHTMLtoPDF → PDF/A-3 → ZUGFeRD/Factur-X funktioniert

Ziel:
- Kein Umbau der HTML-/Tabellenstruktur
- Kein Risiko für den funktionierenden OpenHTMLtoPDF-Renderer
- Nur stabile PDF-Katalog-/Metadatenbasis nach der ZUGFeRD-Einbettung setzen

Änderungen:
- Button 1 bleibt unverändert
- Button 2 bleibt OpenHTMLtoPDF + PDF/A-3 + ZUGFeRD
- Nach Mustang/ZUGFeRD-Einbettung werden für Button 2 erneut gesetzt:
  - Dokumenttitel, z. B. `Rechnung 3663`
  - Sprache, z. B. `de`
  - ViewerPreference `DisplayDocTitle`
  - MarkInfo-Basis
  - einfache Dokumentinformationen Creator/Producer/Subject

Bewusst nicht enthalten:
- keine H1/H2-Umstrukturierung
- keine Tabellenänderungen
- kein thead/tbody-Umbau über den funktionierenden 37g-Stand hinaus
- keine RoleMap-/StructTree-Manipulation
- keine Security-/LBD-Änderung

Erwartung:
- PDF-Erzeugung bleibt stabil wie in 37g
- PAC sollte mindestens beim Dokumenttitel/Metadaten besser werden
- Tags/Structure Tree werden wahrscheinlich noch nicht vollständig gelöst; das folgt in 37h2
