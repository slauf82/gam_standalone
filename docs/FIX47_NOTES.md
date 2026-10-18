# GAM 2.1.0 – fix47

Basis: fix46.

- Die lange Liste historischer Quellen mit Einzel-Löschen-Schaltflächen wurde aus dem Geräteidentitätsdialog entfernt. Gespeicherte Daten und Discovery-Zuordnungen bleiben unberührt.
- Registrierte Geräte: leere Detailbezeichnungen/-werte erscheinen nicht mehr als leere Zeilen. Bei verifiziertem Windows werden widersprüchliche, historisch gespeicherte Linux-Analysefelder in der Detaildarstellung unterdrückt; die Windows-Details bleiben erhalten. Keine automatische Datenbankbereinigung.
- WinRM: Beim Windows-nativen T111-Authentifizierungstest wird der Benutzername ohne zusätzlichen whoami.exe-Prozess über WindowsIdentity abgefragt. Der erfolgreiche SSPI-Aufrufpfad und die übrigen Authentifizierungsverfahren bleiben erhalten.

Hinweis: Der ursprüngliche whoami.exe-Fehler kann auch von anderen (diagnostischen) Aufrufen stammen; ein vollständiger Windows-Test ist weiterhin nötig. Ebenso ist die verbleibende UI-Darstellung auf realen Windows-Geräten zu bestätigen.
