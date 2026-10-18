# 40k36t10

macOS-Autoinventarisierung verwendet jetzt dieselbe konservative Evidenzlogik
wie Windows und Linux.

Eine automatische macOS-Vollinventarisierung startet nur bei:
- starker macOS-Evidenz, z.B. verifiziertem macOS-Inventar, Darwin/sw_vers,
  macOS Backend-lokal, SSH · macOS oder eindeutigem macOS-Gerätetyp; oder
- konkret konfiguriertem SSH-Zugang für die Zieladresse.

Ein schwacher macOS-Hinweis bzw. bloß `platformOf()='macOS'` reicht nicht mehr.

Das Debug-Log weist nun auch für macOS getrennt aus:
- starke Evidenz
- SSH konfiguriert
- schwacher macOS-Hinweis allein
