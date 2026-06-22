# Schritt 37F – OpenHTMLtoPDF PDF/A-3-Testexport

Basis:
- Schritt 37E

Ziel:
- Button 1 bleibt unverändert: OpenPDF + ZUGFeRD/Factur-X
- Button 2 bleibt separater OpenHTMLtoPDF-Testpfad
- Button 2 erzeugt jetzt PDF/A-3U statt normales Test-PDF
- weiterhin noch ohne ZUGFeRD/Factur-X-Einbettung

Umgesetzt:
- OpenHTMLtoPDF-Builder auf `PDFA_3_U` gesetzt
- sRGB-ICC-Farbprofil über Java-ICC-Profil ergänzt
- Windows-Fonts optional registriert, damit PDF/A-Font-Einbettung wahrscheinlicher gelingt
- Tabellenkopf optisch näher an OpenPDF angepasst
- Debug-/Fallback-Hinweis durch PDF/A-3-Testhinweis ersetzt

Nicht geändert:
- keine Security-Änderung
- keine LBD-Änderung
- keine Änderung am produktiven OpenPDF/ZUGFeRD-Pfad
- noch keine ZUGFeRD-Einbettung in Button 2
- noch keine PDF/UA/WCAG-Optimierung

Testplan:
1. Login prüfen
2. Rechnung öffnen
3. Button 1 prüfen: OpenPDF + ZUGFeRD funktioniert weiterhin
4. Button 2 prüfen: OpenHTMLtoPDF erzeugt und öffnet PDF
5. Button-2-PDF mit PDF/A-Validator prüfen
6. Layout mit 37E vergleichen

Nächster Schritt bei Erfolg:
- Schritt 37G: ZUGFeRD/Factur-X-Einbettung für Button 2 auf das PDF/A-3-PDF anwenden
