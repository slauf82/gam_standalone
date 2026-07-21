# Schritt 40k31c3b – Smart-Life-/Tuya-Konfigurationen sicher entfernen

## Enthalten

- Einzelne gespeicherte Smart-Life-/Tuya-Verbindungen lassen sich wieder vollständig löschen.
- Der DELETE-Endpunkt liefert nun eine JSON-Bestätigung statt einer leeren Antwort. Dadurch scheitert der Frontend-Request nicht mehr beim Parsen einer leeren 204-Antwort.
- Zusätzlicher Button **Alle löschen** entfernt sämtliche falschen oder nicht mehr benötigten Tuya-Konfigurationen nach einer Sicherheitsabfrage.
- Nach dem Löschen werden Auswahl, Kennwortfeld, Testergebnis und lokale UI-Zustände zuverlässig zurückgesetzt.
- Fehler beim Löschen werden sichtbar gemeldet und nicht mehr still verschluckt.
- Neue Tuya-Konfigurationen starten nicht mehr mit einer zwingenden Home-Assistant-Verknüpfung.

## Technischer Hinweis zum einfachen Konto-Login

Ein direkter Login in ein bestehendes Smart-Life-Konto allein mit Benutzername und Kennwort ist über die öffentliche Tuya-Cloud-API nicht als allgemeiner Server-Login freigegeben. Der offizielle einfache Drittanbieterweg verwendet User Code und QR-Code-Freigabe über die Smart-Life-/Tuya-Smart-App. Die bisherige Home-Assistant-Brücke wurde deshalb nicht als Voraussetzung beibehalten.
