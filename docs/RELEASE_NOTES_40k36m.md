# Schritt 40k36m – Windows-Berechtigungen und Inventarisierungstiefe

## Neu

- Erkennung des Windows-Benutzers, der die Inventarisierung ausführt.
- Erkennung von Administrator- und SYSTEM-Rechten.
- Ermittlung der Windows-Integritätsstufe.
- Funktionsprüfung für CIM/WMI, Dienste und BitLocker.
- Einheitliche Einstufung der Inventarisierungstiefe: Standard, Erweitert, Administrator oder Vollzugriff.
- Verständliche Auflistung berechtigungsbedingter Einschränkungen.
- Neuer Windows-Bereich „Berechtigungen & Inventarisierungstiefe“ im Geräteidentitätsdialog.
- Erneute normale Berechtigungsprüfung direkt aus dem Dialog.
- Optionaler einmaliger WinRM-Lauf mit separatem Administratorkonto für entfernte Windows-Geräte.
- Kennwörter des erhöhten Laufs werden weder gespeichert noch protokolliert.
- Lokale Windows-Geräte müssen für erhöhte Rechte weiterhin mit einem erhöht gestarteten GAM-Prozess inventarisiert werden.

## Sicherheit

Die temporären Zugangsdaten werden ausschließlich für den unmittelbar ausgelösten WinRM-Lauf verwendet. Wiederherstellungsschlüssel, Kennwörter und andere Geheimnisse werden nicht in das Geräteinventar übernommen.
