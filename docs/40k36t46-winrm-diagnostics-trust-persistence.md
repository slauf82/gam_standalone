# 40k36t46

- Bei fehlenden `GAMKV`-Grunddaten wird die dekodierte Remote-PowerShell-Ausgabe (bis 4000 Zeichen) sichtbar geloggt.
- Ein WinRM-Lauf ohne `COMPUTER_NAME` wird nicht mehr als erfolgreich gewertet.
- Persistentes WinRM-Zielvertrauen wurde gehärtet:
  - versionsunabhängiger Mirror bleibt unter `GAM_DATA_DIR` bzw. `${user.home}/.gam`;
  - atomisches Schreiben;
  - Schreib-/Lesefehler werden nicht mehr verschluckt;
  - `isTrusted()` lädt den persistenten Mirror erneut, wenn die DB nach Versionswechsel/Neuaufbau leer ist;
  - Logs zeigen den tatsächlich verwendeten Dateipfad.
