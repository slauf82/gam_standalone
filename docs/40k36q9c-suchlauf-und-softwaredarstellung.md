# 40k36q9c – Suchlaufabschluss und macOS-Softwaredarstellung

- Nach einem Discovery-/Suchlauf wird keine zweite asynchrone Plattforminventarisierung mehr gestartet.
- Suchlauf beendet bedeutet damit: kein durch Discovery ausgelöster macOS-/Linux-Nachlauf mehr.
- Bewusste SSH-Verbindungstests dürfen weiterhin ihre automatische Inventarisierung auslösen.
- Scheduler INVENTORY/FULL behält die explizite vollständige Inventarisierung.
- macOS-Software wird ausschließlich hierarchisch dargestellt:
  - Software
    - Installierte Software
    - Software-Werkzeuge
    - Softwareupdates
- Homebrew, MacPorts und Xcode Command Line Tools stehen unter Software-Werkzeuge.
- `Verfügbare Updates` erscheint ausschließlich unter Softwareupdates und nicht mehr als flacher Software-/Paketmanager-Wert.
