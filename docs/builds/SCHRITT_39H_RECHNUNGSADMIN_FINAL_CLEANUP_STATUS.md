# GAM 2.0 – Schritt 39h – Rechnungsadministration Final Cleanup

## Ziel
Abschlussbereinigung der Rechnungsadministration nach dem 39g-Teststand.

## Änderungen

### 1. Normalisierte Textbausteinlogik finalisiert
Die alte 39d-Logik der gemeinsamen Rechnungstext-Sets wurde aus der Oberfläche und dem Backend-Endpunkt entfernt.

Es bleiben fachlich korrekt getrennt:

- Rechnungsanreden
- Rechnungstexte
- Rechtliche Hinweise
- Grußformeln
- Textzuordnung je Gesellschaft

Damit müssen identische Anreden oder Grußformeln nur einmal gepflegt werden und können beliebig vielen Gesellschaften zugeordnet werden.

### 2. Logo-Zuordnung je Gesellschaft
Logos werden weiterhin einmalig in der Logoverwaltung hochgeladen und gespeichert.

Neu:

- Rechnungsgesellschaften besitzen ein Feld `LOGO_ID`
- die Gesellschaft wählt ein vorhandenes Logo aus
- mehrere Gesellschaften können dasselbe Logo verwenden
- Vorschau/PDF bevorzugt die zugeordnete Logo-URL
- falls kein Logo zugeordnet ist, bleibt die bisherige Fallback-Logik erhalten

### 3. Datenbank-Kompatibilität
Beim Start werden fehlende Hilfsstrukturen angelegt bzw. ergänzt:

- `rechnungstext_gesellschaft_zuordnung`
- `rechnungslogo`
- `rechnungsgesellschaft.LOGO_ID`

### 4. UI-Bereinigung
Die Rechnungsadministration zeigt keine alte kombinierte Textsatzrubrik mehr.

Die fachliche Struktur ist jetzt:

1. Gesellschaften
2. Produkte / Preise / MwSt
3. Gesellschaft/Filiale
4. Anreden
5. Rechnungstexte
6. Rechtliche Hinweise
7. Grußformeln
8. Textzuordnung Gesellschaft
9. Logos

## Testhinweise

Empfohlener Testablauf:

1. In `Logos` ein Logo hochladen und speichern.
2. In `Gesellschaften` dieses Logo im Feld `Logo` auswählen.
3. Anrede, Rechnungstext, rechtlichen Hinweis und Grußformel jeweils separat anlegen.
4. Unter `Textzuordnung Gesellschaft` eine Gesellschaft auswählen und die vier Textbausteine zuordnen.
5. Rechnungsvorschau/PDF prüfen.

## Ergebnis
Die Rechnungsadministration ist damit fachlich wieder näher an GAM 1.0, aber normalisiert und wartbarer umgesetzt.
