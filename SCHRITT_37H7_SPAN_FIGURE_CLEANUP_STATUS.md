# Schritt 37h7 – PDF/UA/WCAG Span-/Figure-Cleanup

Basis: Schritt 37h6

Ziel:
- verbliebene PAC-Warnungen zu `Span` und `Figure` reduzieren
- Hinweise zu `content in admissible locations` entschärfen
- keine Änderung am stabilen PDF/A-3- und ZUGFeRD-Pfad

Änderungen:
- Gesellschaftslogo im OpenHTMLtoPDF-Pfad nicht mehr als `<img>`, sondern als CSS-Hintergrund gerendert
- QR-Code im OpenHTMLtoPDF-Pfad nicht mehr als `<img>`, sondern als CSS-Hintergrund gerendert
- Portal-URL nicht mehr als `<span>`, sondern als Blockelement gerendert
- Firmenname im Kopfbereich ohne `<strong>` umgesetzt, stattdessen CSS-Klasse `.company-name`
- keine Tabellenstruktur-Experimente
- keine Änderungen an Button 1
- keine Security-/LBD-Änderungen

Hinweis:
- URL zum Portal bleibt als Text vorhanden, dadurch bleibt der QR-Code funktional ersetzbar.
- Logo ist im Dokumenttext durch die Gesellschaftsdaten bereits repräsentiert.

Testziel:
- Button 2 öffnet weiterhin PDF/A-3 + ZUGFeRD
- XML bleibt eingebettet
- PAC: weniger/keine Figure-/Span-bezogenen Warnungen
