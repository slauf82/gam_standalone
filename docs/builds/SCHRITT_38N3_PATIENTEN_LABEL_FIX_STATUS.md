# Schritt 38n3 – Patientenverwaltung Label-Fix

Status: umgesetzt

## Inhalt
- Der interne Modul-Key `patients` bleibt unverändert.
- Die sichtbare deutsche Beschriftung wird wieder als `Patientenverwaltung` ausgegeben.
- Der UI-Label-Fallback wurde um `patients` ergänzt.
- Die übrigen Sprach-Fallbacks wurden ebenfalls ergänzt, damit kein nackter Key angezeigt wird.
- Keine fachliche Änderung an Patientenverwaltung, Terminverwaltung oder Arbeitsplatzausstattung.

## Hintergrund
Nach 38n2 wurde in der Modulübersicht/Login-Auswahl teilweise der technische Key `patients` angezeigt. Ursache war ein fehlender UI-Label-Fallback für den kanonischen Modul-Key.
