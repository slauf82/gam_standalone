# 40k36t88 – WinRM-Zielauflösung / vermutlicher Endfix

- Plattforminventarisierung reicht jetzt `identityKey`, IP und Gerätenamen an WinRM weiter.
- OS-Access-Profile werden in dieser Priorität aufgelöst:
  1. exakte `identity_key`-Bindung
  2. exakte IP/Host-Bindung
  3. exakter Hostname-Hinweis
  4. Legacy-Globalfallback nur wenn kein passendes OS-Access-Profil gefunden wurde
- Für Preflight und Vollinventar wird der im Profil gespeicherte Host verwendet. Damit
  kann ein erfolgreich getestetes Hostname-Profil nicht mehr beim Inventarlauf durch
  die Geräte-IP ersetzt werden.
- Die Geräteidentität bleibt trotzdem an der bekannten Geräte-IP hängen.
- Der moderne Vollinventarlauf wurde von 30 auf 90 Sekunden angehoben; Legacy war
  bereits bei 90 Sekunden.
- Backend-Logs nennen Adresse, Verbindungshost, Identity-Key, Profil-ID/-Name, Benutzer,
  Port, HTTPS und Zugangsquelle.
