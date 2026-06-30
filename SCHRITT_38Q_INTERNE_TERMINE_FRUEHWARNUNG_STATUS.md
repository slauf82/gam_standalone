# Schritt 38q – Interne Termin-Frühwarnung

Status: umgesetzt auf Basis 38p4.

Enthalten:

- Terminverwaltung als praxisinterne Terminverwaltung geschärft.
- OpenReception bleibt später für Patiententermine vorgesehen.
- Rote Warnung für überfällige interne Termine.
- Gelbe Warnung für interne Termine innerhalb der nächsten 14 Tage.
- Warnbereich im Terminmodul.
- Toast-Hinweis beim Öffnen der Terminverwaltung.
- Klick auf Warnung filtert die Tabelle.
- Zielgruppenfelder vorbereitet:
  - alle Benutzer
  - einzelner Benutzer
  - Rolle
- Tabellenanzeige zeigt Fälligkeit und Zielgruppe.

Hinweis:
Die aktuelle Terminverwaltung speichert wie bisher lokal im Frontend. Die Zielgruppenlogik ist als erste Stufe im Modul vorbereitet und kann später auf eine Datenbanktabelle umgestellt werden.
