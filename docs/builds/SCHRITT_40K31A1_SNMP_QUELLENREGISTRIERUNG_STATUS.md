# Schritt 40k31a1 – SNMP als eigenständige Erkennungsquelle

## Korrektur

Der in 40k31a implementierte SNMP-Adapter war technisch aktiv, wurde in der Benutzeroberfläche jedoch weiterhin nur als vorbereitete Datenquelle geführt. Dadurch blieb die sichtbare Zahl der Erkennungsquellen unverändert.

## Umsetzung

- SNMP (integriert) ist jetzt eine eigenständige lokale Erkennungsquelle.
- Die lokalen Discovery-Quellen steigen von 12 auf 13.
- Die Gesamtzahl der Erkennungsquellen erhöht sich dadurch um eins; bei der aktuellen Konfiguration von 16 auf 17.
- SNMP besitzt einen eigenen Aktiv-/Aus-Schalter.
- Die Discovery-Engine verwendet den neuen Schlüssel `SNMP` statt `PLAN_SNMP`.
- SNMP wurde aus den nur vorbereiteten/geplanten Datenquellen entfernt.
- Die Diagnosephase wird als „SNMP (integriert)“ angezeigt.
- Mehrere Smart-Life-Konfigurationen bleiben unverändert unter „Smart Life / Tuya Cloud (n)“ zusammengefasst.

## Build

Der Frontend-Produktionsbuild wurde erfolgreich ausgeführt. Bereits vorhandene TypeScript-Altwarnungen des Gesamtprojekts bleiben unverändert und sind nicht Bestandteil dieses Schritts.
