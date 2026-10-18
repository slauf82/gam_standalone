# 40k36q20 – Inventarisierung stoppen in der globalen Topbar

- Der Abbruchbutton wird bei laufender Inventarisierung jetzt in der festen oberen Aktionsleiste angezeigt.
- Reihenfolge: `Inventarisierung stoppen` → `Meldungen` → `Logout`.
- Der Button ist deutlich rot und hebt sich von Meldungen/Logout ab.
- Der alte zusätzliche Abbruchbutton in der darunterliegenden Inventarisierungs-Statusanzeige wurde entfernt, damit keine überlappenden oder doppelten Abbruchaktionen entstehen.
- Die obere Aktionsleiste fragt den globalen Inventarisierungsstatus während einer angemeldeten Sitzung regelmäßig ab.
- Nach Abbruch zeigt der Button einen Busy-Zustand und verschwindet anschließend, sobald kein Lauf mehr aktiv ist.
- Der bestehende q18-Backend-Abbruch wird unverändert verwendet.
