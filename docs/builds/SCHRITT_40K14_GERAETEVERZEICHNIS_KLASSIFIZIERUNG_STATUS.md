# Schritt 40k14 – Geräteverzeichnis und strengere Klassifizierung

## Umgesetzt

- Das registrierte Geräteverzeichnis ist wie die Discovery nach Geräteklassen gruppiert.
- Kategorien sind standardmäßig eingeklappt und zeigen die jeweilige Geräteanzahl.
- AVM-Systeme werden getrennt von Computern geführt.
- Solar-, Wechselrichter-, Batterie- und weitere Energiesysteme werden vor Computerregeln klassifiziert.
- Die pauschale Einstufung lokaler Adaptertreffer als Computer wurde entfernt.
- Die Kategorie Computer wird nur noch bei eindeutigen Computermerkmalen verwendet.
- Nicht eindeutig klassifizierbare Discovery-Geräte landen unter „Unbekannte Geräte“.
- Nicht eindeutig klassifizierbare registrierte Geräte landen unter „Sonstige Geräte“.

## Computermerkmale

Computer werden nur noch bei klaren Hinweisen erkannt, beispielsweise:

- Windows
- Computer
- Desktop
- Notebook
- Laptop
- MacBook
- iMac
- eigenständige Bezeichnung „PC“

## Build

Der Vite-Produktionsbuild wurde erfolgreich erstellt. Im bestehenden Gesamtprojekt sind weiterhin bereits zuvor vorhandene TypeScript-Typprüfungsfehler außerhalb dieses Schritts vorhanden; sie verhindern den Vite-Produktionsbuild nicht.
