# 40k36t42 – EXECUTE-Fehler zurückholen + versionsunabhängiges WinRM-Trust

- Basis: 40k36t41.
- EXECUTE bricht bei PowerShell-Exceptions nicht mehr mit dem eigenen Exitcode 42 ab.
- Sämtliche PowerShell-Ausgaben (`*>&1`) werden in die Ergebnisdatei geschrieben.
- Auch ein Catch schreibt `GAM_EXEC_EXCEPTION=...` in die Ergebnisdatei; der bestehende blockweise Rückkanal holt sie anschließend zurück.
- Falls der WinRM-Wrapper selbst einen Nicht-0-Status liefert, versucht GAM trotzdem die Ergebnisdatei abzuholen.
- WinRM-Zielvertrauen wird jetzt versionsunabhängig gespeichert:
  - `GAM_DATA_DIR/winrm-trusted-targets.txt`, wenn `GAM_DATA_DIR` gesetzt ist;
  - sonst `${user.home}/.gam/winrm-trusted-targets.txt`;
  - nur als letzter Fallback weiterhin lokales `data/`.
- Eine vorhandene alte `data/winrm-trusted-targets.txt` wird einmalig in den persistenten Speicher migriert.
- Die Datenbank bleibt weiterhin die primäre Quelle; die Datei enthält ausschließlich freigegebene Hosts/IPs, keine Zugangsdaten.
