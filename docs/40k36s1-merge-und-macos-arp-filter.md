# 40k36s1

- Automatische Merge-Kandidaten besitzen eine eindeutige Aktion `Zusammenführen`; sie öffnet weiterhin sicher die Vorschau vor dem eigentlichen Merge.
- Beim Start der Vorschau erscheint sofort eine sichtbare Statusmeldung; Fehler bleiben als Meldung sichtbar.
- Checkbox und Bestätigungstext im Merge-Dialog bilden eine gemeinsame linksbündige, umbrechende Zeile.
- macOS-ARP-Einträge ohne bestätigte MAC sowie `incomplete`/`failed`/`unreachable` werden nicht als Geräte registriert.
- Damit erzeugt der aktive Subnetzscan unter macOS nicht mehr aus seinen eigenen unvollständigen ARP-Cache-Einträgen ein Gerät pro geprüfter Subnetzadresse.
