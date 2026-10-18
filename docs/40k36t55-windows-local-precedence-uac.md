# 40k36t55 – Backend-lokales Windows hat Vorrang vor WinRM

- `isCurrentWindowsHost(...)` vergleicht jetzt robust:
  - Windows-Hostname (`COMPUTERNAME`, Hostname, Canonical Hostname),
  - alle lokalen IP-Adressen,
  - alle lokalen MAC-Adressen.
- Kurzname und FQDN werden normalisiert.
- MAC-Adressen werden ohne Trennzeichen verglichen.
- Sobald der Datensatz der aktuelle Windows-Rechner ist, gewinnt backend-lokal immer vor WinRM.
- Das Frontend verwendet weiterhin die serverseitige `windows/backend-local`-Abfrage aus t54.
- Auf dem eigenen Windows-Rechner muss „Mit erhöhten Rechten neu inventarisieren“ deshalb direkt UAC auslösen; kein Benutzername/Kennwort-Dialog.
- WinRM bleibt ausschließlich für andere Windows-Rechner.
