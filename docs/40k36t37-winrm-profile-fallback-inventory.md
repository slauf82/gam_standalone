# 40k36t37 – WinRM Profil-/Fallback-Vereinheitlichung

- Basis: 40k36t36.
- Bei mehreren aktiven WinRM-Profilen für denselben Host wird jetzt das zuletzt gespeicherte/getestete Profil verwendet (`updated_at DESC, id DESC`), nicht mehr das älteste Profil.
- Hintergrund: Verbindungstest mit `Sebastian Lauf` war erfolgreich, Plattforminventarisierung griff aber auf ein älteres `admin`-Profil zurück.
- Die bestehende Inventarisierung verwendet weiterhin MetricsHub zuerst und fällt bei Bedarf auf Legacy winrm4j/CXF (NTLM) zurück.
- Zusätzliche Backend-Logs zeigen den tatsächlich aufgelösten Benutzernamen und den für die Inventarisierung erfolgreichen Transportpfad.
