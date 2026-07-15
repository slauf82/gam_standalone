# Schritt 40d11 – konsolidierter Build- und Testfix

## Behoben

- `InvoicePortalController`: Rechnungsdatum wird robust aus dem gespeicherten String in `LocalDate` überführt; die beiden Java-Compilerfehler sind beseitigt.
- Patientenportal: Datumsdarstellung richtet sich nach der ausgewählten Sprache.
- Patientenportal: Sprachauswahl um Spanisch, Portugiesisch, Niederländisch, Polnisch und Tschechisch ergänzt.
- Zahlungsworkflow: Aufschub um +1, +3 oder +7 Tage.
- Jeder Aufschub bleibt in der Zahlungshistorie protokolliert.
- Marketingmodul: `marketing` ist jetzt Bestandteil der persistenten Moduleinstellungen und kann dauerhaft deaktiviert werden.
- Kamerastart: vorhandener Stream wird vor einem Neustart beendet; verständliche Meldungen bei blockierter, belegter oder nicht mehr verfügbarer Kamera.

## Testhinweise

1. Backend kompilieren und starten.
2. Unter Einstellungen > Module Marketing deaktivieren, speichern und Seite neu laden.
3. Marketing muss deaktiviert bleiben; erneutes Aktivieren muss ebenfalls gespeichert werden.
4. Patientenportal in allen verfügbaren Sprachen öffnen und Rechnungs-/Dokumentdaten kontrollieren.
5. Im Zahlungsworkflow +1, +3 und +7 Tage testen und Historie prüfen.
6. Kamera über `localhost` starten; bei belegter Webcam muss eine verständliche Diagnose erscheinen.

## Buildhinweis

In der Erstellungsumgebung war keine lokale Maven-Installation bzw. kein Maven-Wrapper vorhanden. Die geänderten Java-Dateien wurden statisch auf Typen, Klammerung und die gemeldeten Fehlerstellen geprüft.
