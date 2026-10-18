# 40k36r13 – Subnetz-/Broadcastadressen nicht als Geräte

- Discovery merkt sich die realen lokalen IPv4-Netze inklusive Prefix.
- Netzadresse und Broadcastadresse jedes lokalen Netzes werden plattformübergreifend als Geräte ausgeschlossen.
- Die Prüfung ist prefixbasiert und funktioniert daher auch für /25, /26, /27 usw.; sie beschränkt sich nicht auf `.0` und `.255`.
- Der aktive Subnetzscan erzeugt ebenfalls keine Netz-/Broadcastkandidaten.
- Echte Hosts im jeweiligen Subnetz bleiben unverändert auffindbar.
- Die Logik gilt gemeinsam für macOS, Linux und Windows.
