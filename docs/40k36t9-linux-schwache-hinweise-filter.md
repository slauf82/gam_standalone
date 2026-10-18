# 40k36t9

Linux-Autoinventarisierung folgt nun derselben konservativen Evidenzlogik wie Windows.

Automatische Linux-Vollinventarisierung startet nur bei:
- starker Linux-Evidenz (verifizierte Linux-Inventarisierung, Distribution,
  Backend-lokal, SSH-Inventar, Linux-Gerätetyp etc.), oder
- konkret konfiguriertem SSH-Zugang für die Zieladresse.

Ein schwacher Linux-Hinweis allein (z.B. generischer Netzwerkfingerprint,
SNMP-Text, Hostname, allgemeiner Dienst oder bloß `platformOf()='Linux'`)
reicht nicht mehr.

Im Debug-Log werden starke Evidenz, SSH-Konfiguration und
`schwacher Linux-Hinweis allein` separat ausgegeben.
