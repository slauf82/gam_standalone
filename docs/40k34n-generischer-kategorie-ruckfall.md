# Schritt 40k34n – Generischer Kategorie-Rückfall aus dem gemeldeten Typfeld

## Gemeldeter Fall

Zwei Geräte (`Basti-Win11Test`, `192.168.2.106` / MS-7B86) mit dem bereits
erkannten Gerätetyp **„Computer / Netzwerkadapter"** landeten in
„Sonstige Geräte" statt in „Computer" - obwohl das Wort „Computer" bereits
eindeutig im Typfeld stand.

## Ursache (im Code nachvollzogen, nicht vermutet)

Die bestehende Computer-Erkennung in `classifyDevice()` prüfte bisher nur:

```ts
if(/^computer$/i.test((dType||'').trim())||explicitComputerType||explicitComputerIdentity)return 'Computer';
```

- `/^computer$/i` verlangt eine **exakte** Übereinstimmung - „Computer /
  Netzwerkadapter" ist nicht exakt „Computer".
- `explicitComputerType` prüft nur enge Modellbegriffe (Desktop, Notebook,
  Laptop, Workstation, Mini-PC, MacBook, iMac) - keiner davon steckt in
  „Netzwerkadapter".
- `explicitComputerIdentity` prüft Name/Hersteller/Ort, nicht das Typfeld
  selbst.

Ein bereits **korrekt als „Computer" erkanntes** Gerät, dessen Typfeld
zusätzlichen, beschreibenden Text enthält (hier offenbar „/
Netzwerkadapter" als Herkunftshinweis), fiel dadurch durch alle Regeln und
landete im generischen Auffangbecken.

## Umgesetzte, generische Lösung (wie vorgeschlagen)

Statt nur die Computer-Regel gezielt zu erweitern, wurde ein **genereller
Mechanismus** ergänzt: `canonicalCategoryFromType(dType)` prüft, ob das
gemeldete Typfeld eines der bereits bestehenden ~20 Kategorienamen als
**eigenständiges Wort** enthält (z.B. „computer", „router", „switch",
„nas", „drucker", „kamera", „server" usw.) - unabhängig davon, was
zusätzlich im Typfeld steht. Das deckt beliebige künftige
„<Kategorie> / <Zusatz>"-Formulierungen automatisch ab, ohne jede einzeln
als Sonderfall kodieren zu müssen.

**Bewusste Ausnahme:** „Access Points" ist **nicht** in dieser generischen
Wortliste enthalten. Genau diese Kategorie war die Ursache der in 40k34l/m
behobenen Fehlklassifizierung (Android-Geräte, die zuvor fälschlich als
„Access Point" gespeichert waren). Ein blindes Vertrauen in ein
möglicherweise veraltetes oder falsches Typfeld hätte diese bereits
behobene Korrektur an dieser Stelle wieder aufgehoben - das wurde mit
einem eigenen Regressionstest ausdrücklich geprüft (siehe unten).

## Platzierung - bewusst als allerletzter Rückfall

`canonicalCategoryFromType()` wird ausschließlich **ganz am Ende** von
`classifyDevice()` aufgerufen - erst nachdem **alle** bestehenden,
spezifischeren Regeln (USB, AVM, Kamera, Router, Access Points, NAS,
Drucker, Smartphone/Tablet, Energiesysteme, Server, Computer-Spezialfälle,
Switches, Telefonie, Audio, Fernseher, Haushalt, Robotik, Klima, Smart
Home, Medizingeräte, Elektrogeräte) erfolglos geprüft wurden. Dadurch
rettet der neue, generische Rückfall **ausschließlich** Geräte, die
vorher tatsächlich in „Sonstige Geräte"/„Unbekannte Geräte" gelandet wären
- für alle bereits korrekt klassifizierten Geräte ändert sich nichts.

## Nicht Bestandteil

Keine Änderung an Backend, `DeviceEvidenceEngine`, den plattform-
spezifischen Classifiern, an Discovery, Merge oder Datenbank. Kein neues
Polling, kein `setTimeout`.

## Durchgeführte statische Prüfungen

**Es konnte in dieser Sandbox weiterhin kein echter Build durchgeführt
werden.** Stattdessen:

- Isolierter TypeScript-Transpile-Lauf über die vollständige `main.tsx` -
  0 Diagnosen.
- **Empirischer Node.js-Test** mit den exakten, gemeldeten Werten
  (`"Computer / Netzwerkadapter"`) - beide Fälle liefern jetzt korrekt
  „Computer".
- **Regressionstest**: `dType="Access Point"` liefert über den neuen
  generischen Pfad bewusst `null` (kein Treffer) - die 40k34l/m-Korrektur
  bleibt dadurch unangetastet.
- Funktionsgrenzen erneut per grep verifiziert (`canonicalCategoryFromType`,
  `classifyDevice`, `DeviceManagerPanel`, `InventoryPage` korrekt auf
  Modulebene getrennt).

```text
fachlich umgesetzt
statisch geprüft (inkl. empirischem Test der beiden gemeldeten Geräte und
  eines gezielten Regressionstests)
nicht durch echten Build verifiziert
```

Der reale Build- und Funktionstest erfolgt anschließend lokal bei
Sebastian.

## Bekannte Grenzen

- Die generische Wortliste deckt aktuell nur die bereits bestehenden
  Kategorienamen ab - neue, noch nicht existierende Kategorien müssten
  weiterhin manuell ergänzt werden (das war aber ohnehin nie anders zu
  erwarten).
- Ein theoretisches Restrisiko: ein Gerät, dessen Typfeld zufällig ein
  Kategorie-Wort als Teil eines anderen Kontexts enthält (z.B. eine
  hypothetische „Computergesteuerte Klimaanlage"), könnte durch diesen
  allerletzten Rückfall der falschen Kategorie zugeordnet werden - da
  dieser Pfad aber nur greift, wenn **keine** spezifischere Regel bereits
  vorher gezogen hat, ist das Risiko gering und betrifft ausschließlich
  Geräte, die vorher ohnehin nur „Sonstige Geräte" gewesen wären.
