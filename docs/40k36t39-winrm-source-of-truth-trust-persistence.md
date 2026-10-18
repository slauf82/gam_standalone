# 40k36t39 – WinRM Source of Truth + persistentes Zielvertrauen

- Basis: 40k36t38.
- Kritischer Fix: Die Vollinventarisierung prüft nicht länger nur die alte globale `gam_winrm_settings`.
- `configured(host)` verwendet jetzt dieselbe `settingsFor(host)`-Auflösung wie Verbindungstest und WinRM-Client:
  1. passendes aktives `gam_os_access_profile` für den Zielhost,
  2. nur falls keines vorhanden ist: alte globale WinRM-Einstellung als Kompatibilitätsfallback.
- `hasConfiguredAccess(host)` verwendet dieselbe zentrale Prüfung.
- Dadurch kann ein erfolgreich getestetes hostbezogenes Profil nicht mehr vor der Vollinventarisierung als „nicht konfiguriert“ verworfen werden.
- Zielvertrauen bleibt primär in `gam_winrm_trusted_targets` gespeichert und wird zusätzlich hostweise (ohne Zugangsdaten) in `data/winrm-trusted-targets.txt` gespiegelt.
- Trust-Wildcards bleiben verboten; „Vertrauen entziehen“ entfernt DB- und Spiegel-Eintrag.
- Authentifizierungsreihenfolge aus t38 bleibt unverändert.
