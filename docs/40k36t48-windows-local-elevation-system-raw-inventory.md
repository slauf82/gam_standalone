# 40k36t48 – Windows-Parität und Inventarisierungstiefen

- Backend-lokales Windows wird nicht mehr nur über `windows-local:`/Protokollherkunft erkannt, sondern zusätzlich über aktuellen Rechnernamen und lokale IP-Adressen. Dadurch darf der eigene Windows-Host nicht mehr fälschlich WinRM verlangen.
- Windows zeigt wie Linux/macOS eine Aktion „Mit erhöhten Rechten neu inventarisieren“. Auch beim backend-lokalen Gerät können dafür einmalige explizite WinRM-Admin-Zugangsdaten verwendet werden; das Kennwort wird nicht gespeichert.
- Neue Windows-spezifische Aktion „Mit SYSTEM-Rechten neu inventarisieren“.
  - t48 führt sie sicher aus, wenn GAM backend-lokal bereits als `NT AUTHORITY\SYSTEM` läuft.
  - t48 erzeugt bewusst noch keinen persistenten SYSTEM-Dienst oder geplanten Task.
- Vollständige dekodierte WinRM-GAMKV-Rohdaten werden versionsunabhängig unter `~/.gam/inventory-raw/windows/<host>.gamkv.txt` gespeichert. Parser-/Anzeigeverbesserungen können damit künftig ohne erneuten Transport auf den Rohdaten aufbauen.
- t47 WinRM/NTLM/ExecutionPolicy-/Chunk-/Marker-Transport bleibt unverändert.
