# Schritt 36W – Stabilisierung: LBD 36F + PDF 36I + Benutzer aus PDF entfernt

Basis:
- stabiler PDF-/ZUGFeRD-Stand aus Schritt 36I
- stabile LBD-Erkennung aus Schritt 36F
- keine OpenHTMLtoPDF-Umschaltung im produktiven PDF-Pfad
- keine neuen Security-Experimente

Änderungen in 36W:
- Benutzeranzeige aus Rechnungs-PDF entfernt
- Benutzeranzeige aus Rechnungsvorschau/Vorlesetext entfernt
- LBD-Daten können vor dem Speichern im Rechnungsformular nachbearbeitet werden
- Rechnung kann vollständig mit manueller Empfängeradresse gespeichert werden
- manuelle Empfängeradresse wird in FADRESSE/FEMAIL gespeichert
- ZUGFeRD/XML und Exportcheck verwenden zuerst die gespeicherte Empfängeradresse
- wenn eine manuelle Empfängeradresse gespeichert ist, entsteht keine unnötige LBD-Warnung mehr

Bewusst nicht enthalten:
- keine neue PDF-Engine
- keine weiteren PDF/UA-/WCAG-Experimente
- kein Security-Matcher-Umbau
- kein Ersatz des stabilen OpenPDF-ZUGFeRD-Pfads

Nächster Schritt:
- Schritt 37 mit zweitem separatem PDF/UA-Testbutton starten
- produktiver OpenPDF-ZUGFeRD-Button bleibt dabei unverändert stabil
