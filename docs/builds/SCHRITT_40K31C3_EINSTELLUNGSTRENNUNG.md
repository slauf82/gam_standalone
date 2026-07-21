# Schritt 40k31c3 – Trennung von Merge-Regeln und Anzeigeeinstellungen

## Ziel
Technische, systemweit wirksame Regeln der Confidence-/Merge-Engine werden konsequent von benutzerbezogenen Anzeige- und Arbeitsoptionen getrennt.

## Einstellungen → Geräteverwaltung
Dort verbleiben ausschließlich:
- Schwellwert für automatische Zusammenführung
- Schwellwert für mögliche Dubletten
- Gewichtungen der Identitätsmerkmale
- Quellenboni
- automatische Zusammenführung
- Blockierung bei harten Konflikten
- Schutzregel „IP-Adresse niemals allein verwenden“
- Wiederherstellung der Standardwerte

## Gerätemanager
Dort verbleiben:
- kompakte Darstellung
- leere oder unbekannte Werte ausblenden
- Zeitstempel, Einheiten und Entity-IDs anzeigen
- Favoriten und Änderungen hervorheben
- sichtbare Home-Assistant-Entitätstypen
- sichtbare Entitätsdomänen
- vorbereitete Anzeigeoptionen künftiger Datenquellen

Diese Optionen verändern nicht die Identitätsbewertung und lösen keine neue Discovery aus.

## Bereinigung
Die nicht mehr sichtbaren Schreib- und Reset-Funktionen für Merge-Regeln wurden aus dem Gerätemanager entfernt. Der Gerätemanager lädt die zentralen Regeln nur noch lesend, damit seine Dublettenanzeige dieselben Schwellwerte wie das Backend verwendet.
