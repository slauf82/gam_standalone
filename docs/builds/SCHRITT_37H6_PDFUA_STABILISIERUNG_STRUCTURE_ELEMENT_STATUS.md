# Schritt 37h6 – PDF/UA-Stabilisierung Structure-Element

Basis: Schritt 37h5.

Ziel:
- den in 37h5 neu entstandenen PDF/UA-Fehler bei `Structure elements` beseitigen
- PDF/A-3 + ZUGFeRD + PDF/UA-Tagging beibehalten
- keine riskanten Tabellenumbauten

Änderungen:
- fehleranfälliges `<main>`-Wrapping entfernt
- `role="presentation"` auf Layout-Containern entfernt
- Header-/Summen-/Portal-Layout auf den stabileren 37h4-Ansatz zurückgeführt
- Metadaten, Dokumenttitel, Sprache, Outline und ZUGFeRD-Pfad bleiben erhalten

Hinweis:
37h5 reduzierte einzelne Tag-Zähler, erzeugte aber einen neuen PDF/UA-Fehler. 37h6 priorisiert wieder Stabilität und PAC-Fehlerfreiheit vor Warnungsreduktion.
