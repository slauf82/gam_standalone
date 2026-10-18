# 40k36t84 – SYSTEM-Softwaremodul härten

- Strukturierte Registry-Basis (HKLM/HKCU/HKU/temporär geladene Profile) wird bereits vor AppX/MSIX als `SOFTWARE_DETAILS` ausgegeben.
- Ein Fehler im optionalen AppX/MSIX-Teil kann die Registry-Software nicht mehr löschen.
- Der gesamte AppX/MSIX-Bereich besitzt zusätzlich einen äußeren Fehlerfang.
- `SOFTWARE_BASELINE_STATUS` zeigt die Anzahl der bereits gesicherten strukturierten Registry-Einträge.
- Fehlgeschlagene erhöhte Module schreiben den exakten Marker zusätzlich ins Backend-Log.
- Mojibake `â€“`/`â€”` wird zu `–`/`—` korrigiert.
- Storage-Fragmente im Format `x/y GB frei/gesamt; D` werden nicht mehr unter „Weitere Informationen“ einsortiert.
