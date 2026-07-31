# Schritt 40k33b10 – Merge-Diagnose: Schutzregeln deutlich hervorheben

## Ausgangslage

Die Merge-Diagnose zeigte bereits Übereinstimmungswert, Konflikte und
Schutzregeln an - aber alle optisch gleichrangig. Ein Benutzer konnte auf
einen scheinbar guten Übereinstimmungswert (z.B. 75 %) achten und eine
gleichzeitig aktive kritische Schutzregel (z.B. zwei gleichzeitig aktive
IP-Adressen) übersehen. 40k33b10 stellt eine klare visuelle Rangfolge her:
kritische Schutzregeln stehen immer sichtbar VOR und ÜBER dem Prozentwert.

## Geänderte Dateien

Backend:
- `backend/.../inventory/DiscoveryRegistrationRepository.java` – neue,
  gemeinsam genutzte Hilfsmittel: `FieldComparisonRow`/`compareIdentityFields()`
  (ein einziger Feld-Vergleich für Merge-Dialog UND Integritätsprüfung, ersetzt
  zwei zuvor getrennte Implementierungen), `conflictSeverity()`/
  `highestConflictSeverity()` (ordnet bereits vorhandene Konflikttexte den drei
  geforderten Schutzstufen zu), `criticalConflictMessage()` (die konkrete
  Warnformulierung aus dem Auftrag).
- `backend/.../inventory/DeviceIdentityService.java` – nutzt jetzt die
  gemeinsame Vergleichslogik statt einer eigenen Kopie; `IntegrityResult` um
  `criticalityLevel`/`criticalMessage` erweitert (dieselbe Einordnung wie im
  Merge-Dialog).
- `backend/.../inventory/DeviceMergeService.java` – `Candidate` und
  `MergePreviewResult` um `criticalityLevel`, `criticalMessage` und
  `fieldComparison` erweitert; SLF4J-Logging ergänzt.

Frontend:
- `frontend/src/api/client.ts` – neue Felder an den entsprechenden Typen.
- `frontend/src/main.tsx` – `DeviceMergeDialog` (Kandidatenliste + Vorschau)
  und `IntegritySection` zeigen jetzt die neue, dreistufige Schutzdarstellung.
- `frontend/src/style.css` – neue, auffällige Darstellungsklassen.

Dokumentation:
- `docs/40k33b10-merge-schutzregeln.md` (diese Datei).

## Architekturentscheidungen (wie gefordert eingehalten)

- **Keine neue Merge-Logik, keine zweite Konflikterkennung, keine zweite
  Bewertung:** Die drei Schutzstufen werden ausschließlich aus den bereits von
  `DeviceIdentityConfidenceEngine.assess()` gelieferten Konflikttexten
  abgeleitet (`"abweichende MAC-Adresse"`, `"abweichende Seriennummer"`,
  `"abweichende IP-Adresse bei gleichzeitig erreichbaren Geräten"`, sowie der
  seit 40k33b7 bestehende SSH-Hostkey-Vergleich). Es wurde keine einzige neue
  Vergleichsregel eingeführt - nur eine Einordnung bereits vorhandener Texte in
  🔴 KRITISCH / 🟠 HOCH / 🟡 PRÜFHINWEIS per Schlüsselwort-Zuordnung.
- **Keine Veränderung der bestehenden Score-Berechnung:** `score`/
  `confidencePercent` werden unverändert von der Engine übernommen; die neuen
  Felder sind rein zusätzliche, abgeleitete Darstellungsinformationen.
- **Konsistenz zwischen Merge-Dialog und Integritätsprüfung:** Da beide jetzt
  dieselbe `compareIdentityFields()`/`conflictSeverity()`-Logik verwenden, wird
  derselbe Konflikt an beiden Stellen automatisch identisch eingestuft - es
  gibt technisch keine Möglichkeit mehr für eine widersprüchliche Bewertung.

## Die drei Schutzstufen

Zuordnung ausschließlich anhand der bereits vorhandenen Konflikttexte:

| Schutzstufe | Bereits vorhandener Konflikttext |
|---|---|
| 🔴 KRITISCH | "abweichende IP-Adresse bei gleichzeitig erreichbaren Geräten", "abweichende Seriennummer", abweichender SSH-Hostkey (seit 40k33b7) |
| 🟠 HOCH | "abweichende MAC-Adresse" |
| 🟡 PRÜFHINWEIS | jeder sonstige, von der Engine gemeldete Konflikttext |

**Ehrlich benannte Lücke:** Die im Auftrag als Beispiele genannten
"unterschiedliche Hardware-UUIDs", "unterschiedliche BIOS-/System-UUIDs",
"widersprüchliche Geräteklassen" und "unvereinbare Betriebssystem-/
Plattformdaten" werden von der bestehenden `DeviceIdentityConfidenceEngine`
aktuell **nicht** als eigene Konflikttypen erkannt (nur MAC, Seriennummer und
gleichzeitig-aktive-IP). Da der Auftrag ausdrücklich "keine neue fachliche
Konfliktlogik erfinden" fordert, wurden diese Beispiele **nicht** nachgebildet
- sie würden eine echte neue Konflikterkennung benötigen. Sollten diese
Konflikttypen in einem späteren Schritt eingeführt werden, greift die hier
gebaute Einordnung automatisch mit (die Klassifizierung arbeitet rein
textbasiert auf der bereits vorhandenen `conflicts`-Liste).

## Darstellung im Merge-Dialog

- **Kandidatenliste:** Sammelbanner ("❗ KRITISCHE SCHUTZREGEL AKTIV") erscheint
  sofort, wenn mindestens ein Kandidat kritisch ist - ohne dass ein Detail
  geöffnet werden muss. Kritische Zeilen sind zusätzlich per Rahmen/Hintergrund
  hervorgehoben (nicht nur Farbe), das kritische Detail (Warntext +
  vollständige tabellarische Gegenüberstellung) erscheint sofort darunter,
  nicht erst nach weiteren Klicks.
- **Übereinstimmungsanzeige:** Bei kritischer Schutzregel steht nie nur "75 %"
  - immer "75 % Übereinstimmung" gefolgt von "❗ Kritische Schutzregel aktiv"
  in derselben Zelle, optisch nicht grün/positiv eingefärbt.
- **Vorschau:** Großer, klar umrandeter roter Warnbereich oberhalb der
  Gerätedaten, mit der im Auftrag geforderten Beispiel-Formulierung für den
  Fall "gleichzeitig unterschiedliche IP-Adressen". Darunter die vollständige
  tabellarische Gegenüberstellung (IP-Adresse, Erreichbar, MAC-Adresse,
  Seriennummer, Hostname, SSH-Hostkey, Betriebssystem/Kategorie, Discovery-
  Quellen, Rollen, Zeitpunkt) mit hervorgehobener Konfliktzeile.
- **Bestätigung vor der Zusammenführung:** Bei kritischer Schutzregel (oder
  weiterhin bei reinem MAC-Konflikt) erscheint eine Checkbox „Konflikt bewusst
  geprüft“ direkt im roten Warnbereich. Der Zusammenführen-Button bleibt
  deaktiviert, bis die Checkbox aktiviert ist, und trägt dann die Beschriftung
  „❗ Trotzdem zusammenführen“ statt der neutralen Standardbeschriftung. Der
  normale Ablauf ohne Schutzregel bleibt unverändert (Button sofort aktiv,
  neutrale Beschriftung).
- **Integritätsprüfung (40k33b7):** zeigt bei „Integritätswarnung“ jetzt
  denselben roten Warnbereich mit derselben Formulierung wie der Merge-Dialog.

## Barrierearme Darstellung

Jede kritische Markierung kombiniert mindestens: Symbol (❗/🔴/🟠/🟡), Wort
("KRITISCH"/"Hoher Konflikt"/"Prüfhinweis"), vollständigen Warntext und einen
sichtbaren Rahmen/Hintergrund - nicht ausschließlich Farbe.

## Logging

`DeviceMergeService` protokolliert jetzt auf Debug-Ebene: welche Schutzstufe
für welches Kandidatenpaar ausgelöst wurde, welche Konflikttexte beteiligt
waren, sowie bei der Bestätigung, ob der Benutzer einen kritischen Konflikt
bewusst bestätigt hat (`confirmMacConflict`/`confirmCriticalConflict`). Es
werden keine Zugangsdaten protokolliert.

## Tests / durchgeführte Prüfungen in dieser Umgebung

Kein JDK, kein funktionierendes `npm install` in dieser Sandbox - kein echter
Build möglich. Stattdessen geprüft:

- Klammernbilanz aller drei geänderten Backend-Dateien (ausgeglichen).
- Isolierter TypeScript-Transpile-Lauf über `client.ts` und die vollständige
  `main.tsx` nach jeder Änderung - durchgehend 0 Diagnosen.
- Alle Funktionsgrenzen im Frontend nach den umfangreichen Änderungen am
  Merge-Dialog per grep erneut verifiziert.
- Manuelle Nachverfolgung, dass `criticalityLevel`/`fieldComparison` in
  `Candidate`, `MergePreviewResult` und `IntegrityResult` konsistent über
  dieselbe geteilte Methode befüllt werden (keine zweite Implementierung).

**Nicht durchgeführt:** echter Maven-/Vite-Build, automatisierte Tests, Test
gegen zwei real gleichzeitig aktive Geräte mit unterschiedlicher IP-Adresse.
Bitte vor dem Produktiveinsatz verifizieren.

## Bekannte Einschränkungen

- Backend-seitige Blockierung vor der eigentlichen Zusammenführung besteht
  weiterhin nur für den MAC-Konflikt-Fall (`knownMacs.size()>1`, unverändert
  seit 40k33b4) - das war bereits vor diesem Schritt so und wurde nicht
  erweitert, um die bestehende Merge-Ausführung nicht anzufassen
  ("keine Veränderung der bestehenden Score-Berechnung, sofern nicht für die
  Darstellung zwingend erforderlich"). Die Absicherung gegen unbewusstes
  Bestätigen bei Serien-/IP-/SSH-Hostkey-Konflikten erfolgt daher auf
  Oberflächenebene (deaktivierter Button bis zur Checkbox) statt als
  zusätzliche Server-seitige Sperre.
- Hardware-/BIOS-UUID- und Plattform-/Kategorie-Konflikte werden nicht
  gesondert erkannt (siehe „ehrlich benannte Lücke“ oben) - die bestehende
  Engine liefert diese Konflikttypen aktuell nicht.

## Manuelle Testanleitung

1. Zwei Geräte mit unterschiedlichen bekannten MAC-Adressen als Kandidaten
   anzeigen lassen: Zeile muss 🟠 „Hoher Konflikt“ zeigen (keine rote
   Kritisch-Markierung, da MAC laut Auftrag "Hoher Konflikt" ist).
2. Zwei Geräte mit unterschiedlichen Seriennummern (oder simuliert
   unterschiedlichen, gleichzeitig erreichbaren IPs) als Kandidaten anzeigen
   lassen: Zeile UND Sammelbanner müssen ❗ KRITISCH zeigen, direkt in der
   Liste, ohne dass Details geöffnet werden müssen.
3. Vorschau für einen kritischen Kandidaten öffnen: großer roter Warnbereich
   muss oberhalb der Gerätedaten erscheinen; „Zusammenführung bestätigen“-
   Button muss deaktiviert sein, bis „Konflikt bewusst geprüft“ angehakt
   wird; danach Beschriftung „❗ Trotzdem zusammenführen“.
4. Normalen, unkritischen Kandidaten öffnen: Ablauf muss wie vorher
   funktionieren (Button sofort aktiv, keine Warnbereiche).
5. Bei einem Gerät mit Integritätswarnung (40k33b7) im Bereich
   „Integritätsprüfung“ öffnen: derselbe rote Warnbereich mit derselben
   Formulierung wie im Merge-Dialog muss erscheinen.
6. Regressionstest: bestehende Zusammenführung ohne Konflikt, Alias-
   Verwaltung, Wiederauftrennung, Linux-Inventarisierung müssen unverändert
   funktionieren.
