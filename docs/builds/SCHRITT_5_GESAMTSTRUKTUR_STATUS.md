# GAM 2.0 – Schritt 5 Gesamtstruktur

Diese Version erweitert GAM 2.0 vom Rechnungs-/Inventar-/Lagerkern zur vollständigen App-Struktur.

## Neu angebundene Modulrahmen

- Aufgaben (`aufgaben`)
- Freigaben (`freigabe`)
- Personal (`personal`)
- Kassenbuch (`kassenbuch`, `kassenbuchoben`)
- Prüfungen / Inbetriebnahmen / Einweisungen (`kontrolle`, `inbetriebnahme`, `einweisung`)
- Ordnerfreigaben (`ordnerfreigabe`, `arbeitsplatz`)
- News (`news`)
- Reports/Zusammenfassung

## Bewusste Sicherheitsentscheidung

Diese Module sind in Schritt 5 zuerst als Lese-/Rahmenmodule umgesetzt.
Schreibfunktionen werden erst aktiviert, wenn die alte Fachlogik je Modul abgeglichen ist.

## Weiterhin enthalten

- Login über bestehende `accounts`-Tabelle
- 2FA/TOTP-Kompatibilität über `secretkey`
- Rechnungsmodul mit ZUGFeRD/Factur-X als Pflicht-Export
- `.lbd`-Empfängerdateien
- Inventar/Geräte
- Lager/Material
- Rollenabhängige Navigation

## Nächster sinnvoller Schritt

Vor dem ersten großen Test sollte als nächstes eine Konsolidierung erfolgen:

1. alte XHTML-Seiten gegen neue Module mappen
2. fehlende Spezialfunktionen markieren
3. sensitive Felder im Personalbereich schützen/ausblenden
4. ZUGFeRD validieren
5. Build lokal mit Maven/Gradle prüfen
