# 40k36q22 – Verwaiste Inventarisierungsläufe bereinigen

- Beim Start des Backends werden alle noch mit `LAEUFT` gespeicherten Plattforminventarisierungen automatisch auf `ABGEBROCHEN` gesetzt.
- Begründung: Worker-Threads aus einem vorherigen Backend-Prozess können nach einem Neustart nicht mehr weiterlaufen.
- Die Historie bleibt erhalten; `finished_at` wird gesetzt.
- Standardmeldung: `Inventarisierung durch Backend-Neustart oder unerwartetes Ende unterbrochen.`
- Zusätzlich besitzt der globale Inventarisierungsstatus ein Sicherheitsnetz:
  - `LAEUFT`-Datensätze älter als 12 Stunden werden automatisch als veraltet abgebrochen.
- Dadurch kann ein alter Lauf die globale Sticky-Anzeige nicht mehr unbegrenzt als aktiv erscheinen lassen.
- Normale aktuelle Inventarisierungen sind davon nicht betroffen.
