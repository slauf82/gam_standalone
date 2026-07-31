# Schritt 40k34t – Gerätebestand vollständig mit Discovery, Geräteidentität und Plattforminventarisierung verbinden

## Bisherige Trennung zwischen InventoryDevice und DeviceIdentity

`InventoryDevice`/`InventoryPage` ist ein **eigenständiges, deutlich älteres**
Asset-Verwaltungssystem, das lange vor der gesamten Discovery-/Android-
Architektur existierte. Es basiert auf zwei Legacy-Tabellen (`geräte`,
`geräte_neu`, disambiguiert über das Feld `source`) mit eigener Filial-/
Gesellschafts-/Materialverwaltung. `InventoryDevice` (das Java-Record)
enthält **keinerlei** Bezug zur Discovery-Welt: kein `identityKey`, nicht
einmal eine MAC-Adresse - nur `id, source, name, type, serialNumber,
inventoryNumber, manufacturer, ip, location, branchId/Code/Name,
medicalDevice, electricalDevice, inventoryRelevant, active, inUse,
acquiredAt, note`.

## Tatsächliche Ursache des fehlenden Buttons (jetzt zweifelsfrei bestätigt)

**Ja, die fehlende Verbindung zwischen `InventoryDevice` und der Discovery-
Geräteidentität war tatsächlich die Ursache** - und noch konkreter als in
40k34s vermutet: `InventoryWriteController.moveRegisteredToInventory()`
(der Endpunkt hinter „In Gerätebestand übernehmen") rief bisher **immer**
`registrations.deregisterByKey(identityKey)` auf, **nachdem** das neue
`InventoryDevice` angelegt wurde - die ursprüngliche Discovery-Identität
wurde also bei **jedem einzelnen** Verschieben in den Gerätebestand
unwiderruflich gelöscht, ohne dass irgendeine Verbindung dokumentiert
wurde. Der Button „Neu inventarisieren" existierte zwar korrekt im
Geräteidentitäts-Dialog (Discovery-Ansicht) - für Geräte, die bereits in
den Gerätebestand verschoben wurden, gab es aber schlicht **keine**
Möglichkeit mehr, dorthin zurückzufinden.

## Geprüft, aber verworfen: identityKey einfach neu berechnen

`DiscoveryRegistrationRepository.resolveKey(ip, mac, serial, name)` ist
zwar deterministisch, aber mit fester Priorität **MAC > Seriennummer > IP >
Name**. Da `InventoryDevice` keine MAC-Adresse speichert, hätte eine
Neuberechnung bei jedem MAC-basierten Originalschlüssel einen **anderen**
Schlüssel ergeben als ursprünglich - keine verlässliche Lösung. Deshalb
wurde stattdessen eine **explizite, additive Verknüpfungstabelle** gewählt
(wie im Auftrag selbst als zulässige Alternative vorgesehen).

## Gewählte kanonische Geräteidentität

Die kanonische Identität bleibt ausschließlich `identityKey`
(`gam_discovery_registered_devices`) - **keine zweite Geräteidentität**
wurde eingeführt. Neu ist ausschließlich eine reine **Verknüpfungstabelle**.

## Schema-/Modelländerungen

**Neue, additive Tabelle** `gam_inventory_identity_links`:

```sql
CREATE TABLE IF NOT EXISTS gam_inventory_identity_links (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  source VARCHAR(32) NOT NULL,
  inventory_device_id INT NOT NULL,
  identity_key VARCHAR(255) NOT NULL,
  matched_by VARCHAR(64) NOT NULL,
  matched_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  ambiguous BOOLEAN NOT NULL DEFAULT FALSE,
  UNIQUE KEY uq_gam_inventory_identity_link (source, inventory_device_id)
)
```

Keine bestehende Tabelle wurde verändert. `InventoryDeviceDetail` (Java-
Record, reines DTO) wurde additiv um `identityKey, platform,
platformStatus, discoveryProtocol` erweitert - **ein gemeinsames DTO**
statt mehrerer Einzelabfragen, wie im Auftrag gefordert.

## Wie wurden vorhandene Geräte nachverknüpft?

Zwei getrennte Wege, klar dokumentiert:

1. **Sofort, 100% zuverlässig, für alle KÜNFTIGEN Verschiebungen:**
   `moveRegisteredToInventory()` ruft jetzt `identityLinks.link(...)`
   **bevor** die ursprüngliche Discovery-Identität gelöscht wird - zu
   diesem Zeitpunkt ist die Zuordnung zu 100% sicher, keine Ratelogik
   nötig.
2. **Best-Effort-Nachverknüpfung für bereits VOR 40k34t verschobene
   Bestandsgeräte** (`InventoryIdentityLinkRepository.
   linkExistingUnlinkedDevices()`, ausgelöst über den neuen Button „Mit
   Discovery verknüpfen"): Da die ursprüngliche Discovery-Identität dieser
   Altgeräte bereits gelöscht ist, kann nur verknüpft werden, wenn
   **aktuell noch** eine übereinstimmende Discovery-Identität existiert
   (z.B. weil ein späterer Suchlauf dasselbe physische Gerät erneut
   gefunden hat). Zuordnungsreihenfolge, ausschließlich mit tatsächlich
   vorhandenen, stabilen Merkmalen:
   1. Seriennummer (exakt, muss eindeutig sein)
   2. IP-Adresse **und** Name gemeinsam (exakt, muss eindeutig sein)
   - Name allein wird **nie** als Zuordnungsmerkmal verwendet, wie
     ausdrücklich gefordert. Mehrdeutige Treffer werden **nicht**
     verknüpft, sondern protokolliert (`log.info("[INVENTORY-LINK]
     Mehrdeutige Zuordnung übersprungen: ...")`) und bleiben ehrlich als
     ungeklärt stehen.

**Ehrlich benannt:** Für Altgeräte, deren physisches Gerät seit dem
Verschieben **nie erneut** von Discovery gefunden wurde, bleibt keine
Verknüpfung möglich - es existiert schlicht keine Discovery-Identität
mehr, mit der verglichen werden könnte. Das ist keine Fehlfunktion, sondern
eine Konsequenz des zuvor bestehenden `deregisterByKey()`-Verhaltens, das
mit diesem Schritt für die Zukunft behoben, aber für die Vergangenheit
nicht rückwirkend heilbar ist.

## API-Integration

**Keine zweite Plattforminventarisierungs-API.** `InventoryDeviceDetail`
liefert jetzt `identityKey`/`platform`/`platformStatus` direkt mit (ein
gemeinsames DTO, keine Einzelabfragen). Sobald `identityKey` bekannt ist,
ruft das Frontend **direkt den bereits bestehenden** Endpunkt
`POST /api/inventory/discovery/identity/platform-inventory` auf - denselben,
den auch der Geräteidentitäts-Dialog seit 40k34p verwendet. Es wurde
**bewusst kein** zusätzlicher `/devices/{source}/{id}/platform-inventory`-
Endpunkt behalten (ein erster Entwurf davon wurde während der Umsetzung
wieder entfernt, da er die im Auftrag ausdrücklich verbotene „zweite API"
gewesen wäre).

Ein einziger neuer, zusätzlicher Endpunkt war nötig, für den es **keine**
bereits bestehende Entsprechung gab: `POST /api/inventory/devices/link-existing`
(Best-Effort-Nachverknüpfung, siehe oben).

## UI-Integration

Die tatsächlich verwendete `InventoryPage`-Zeilen-Detailansicht
(`technicalSections`, dieselbe bereits bestehende Struktur wie „Hardware",
„Laufwerke & SMART" usw.) wurde um einen neuen, standardmäßig
**aufgeklappten** Abschnitt „Discovery & Plattforminventarisierung"
ergänzt:

- Ist das Gerät verknüpft: zeigt `identityKey` und Plattform, und rendert
  **dieselbe, bereits bestehende** `PlatformInventorySection`-Komponente
  (aus 40k34p/r, unverändert) - Plattform, Status, letzter Lauf, Button
  „Neu inventarisieren" je Plattform sowie „Alle bekannten Plattformen
  erneut inventarisieren" erscheinen dadurch **identisch** zur Discovery-
  Ansicht, keine zweite UI-Implementierung.
- Ist das Gerät nicht verknüpft: eine klare, verständliche Meldung statt
  eines Fehlers oder leerer Fläche, mit Hinweis auf den neuen Button „Mit
  Discovery verknüpfen".

Ein neuer Button „Mit Discovery verknüpfen" wurde direkt neben „Gesamten
Gerätebestand leeren" ergänzt (löst die Best-Effort-Nachverknüpfung aus).

## Automatische Inventarisierung

**Unverändert.** Die in 40k34r/s bestehende automatische Inventarisierung
läuft weiterhin ausschließlich über `gam_discovery_registered_devices` und
den bereits bestehenden Dispatcher - sie wird durch die neue Verknüpfung
**nicht** doppelt ausgelöst, da die Verknüpfungstabelle rein lesend in die
Anzeige einfließt und an keiner Stelle selbst eine Inventarisierung
anstößt.

## Manuelle Neuinventarisierung im Gerätebestand

Nutzt **exakt** denselben Weg wie im Geräteidentitäts-Dialog:
`PlatformInventorySection` mit dem aufgelösten `identityKey` - identische
Cooldown-freie manuelle Auslösung, identischer Schutz vor Doppelläufen
(`isRunning()`-Prüfung in `runPlatformInventory()`, seit 40k34r/s
unverändert), identische sich selbst begrenzende Status-Aktualisierung.

## Datenprioritäten und Merge

**Unverändert.** Es wurde **keine neue parallele Merge-Logik** ergänzt -
`recordDiscoveryHit()` bleibt die einzige Stelle, an der Discovery-/
Inventardaten in eine Geräteidentität einfließen. Der Gerätebestand
(`InventoryDevice`) selbst bleibt von dieser Konsolidierung unberührt -
seine eigenen, manuell gepflegten Felder (Filiale, Gesellschaft, Material,
Inventarnummer usw.) werden durch die neue Verknüpfung **nicht**
überschrieben, da die Verknüpfung ausschließlich lesend zusätzliche
Discovery-/Plattforminformationen ANZEIGT, ohne `InventoryDevice`-Felder zu
verändern.

## Bekannte Einschränkungen

- Altgeräte ohne aktuell noch existierende, übereinstimmende Discovery-
  Identität können nicht automatisch nachverknüpft werden (siehe oben) -
  das ist eine Datenlücke aus der Vergangenheit, keine neue Funktions-
  lücke.
- Die Best-Effort-Nachverknüpfung lädt aktuell bis zu 10.000 Gerätebestand-
  Einträge auf einmal (`InventorySearchCriteria(..., 10000, 0)`), um N+1-
  Abfragen zu vermeiden - bei sehr viel größeren Bestandsgrößen wäre eine
  Seitenverarbeitung nötig; das wurde als für die aktuelle Größenordnung
  ausreichend eingestuft, aber nicht gegen eine wirklich große Datenbank
  getestet.
- Es wurde keine automatische, im Hintergrund laufende Nachverknüpfung
  ergänzt (z.B. beim Start) - sie muss über den neuen Button manuell
  ausgelöst werden. Das ist eine bewusste, konservative Entscheidung
  (keine unerwartete Hintergrundaktivität), keine technische
  Notwendigkeit.

## Durchgeführte statische Prüfungen

**Es konnte in dieser Sandbox weiterhin kein echter Build und kein echter
Test mit den neun Android-Geräten durchgeführt werden.** Stattdessen:

- Klammernbilanz aller fünf geänderten/neuen Backend-Dateien -
  ausgeglichen.
- Automatisierter Record-Konstruktor-Argumentzahl-Abgleich über alle
  betroffenen Dateien - keine Abweichungen.
- Manuelle Prüfung auf zirkuläre Bean-Abhängigkeiten (keine gefunden) und
  auf manuelle Instanziierungen der geänderten Konstruktoren (keine
  gefunden, die brechen würden).
- Isolierter TypeScript-Transpile-Lauf über `client.ts` und die
  vollständige `main.tsx` - 0 Diagnosen.
- Manuelle Durchsicht: `PlatformInventorySection` wird in der
  `InventoryPage`-Detailansicht mit demselben `identityKey`-Prop
  aufgerufen wie im Geräteidentitäts-Dialog - keine zweite Implementierung.

```text
fachlich umgesetzt (Verknüpfungsarchitektur, Migration für künftige Geräte,
  Best-Effort-Nachverknüpfung für Altgeräte, UI-Integration)
statisch geprüft
NICHT durch echten Build oder echten Test mit den neun tatsächlichen
  Android-Geräten verifiziert
```

## Ehrliche Abschlussdokumentation (explizit beantwortet)

1. **War die fehlende Verbindung tatsächlich die Ursache?** Ja, bestätigt -
   und zwar konkreter als vermutet: `moveRegisteredToInventory()` löschte
   die Discovery-Identität bei jedem Verschieben aktiv.
2. **Wo wurde die Verbindung hergestellt?** In einer neuen, rein additiven
   Verknüpfungstabelle (`gam_inventory_identity_links`), gesetzt entweder
   sofort beim Verschieben (zuverlässig) oder per Best-Effort-
   Nachverknüpfung für Altgeräte (Seriennummer, dann IP+Name).
3. **Welche Datenbank-/Modelländerungen waren nötig?** Eine neue Tabelle;
   `InventoryDeviceDetail` additiv um vier Felder erweitert. Keine
   bestehende Tabelle/Spalte verändert.
4. **Wie wurden vorhandene Geräte nachverknüpft?** Siehe oben - nur bei
   eindeutigem, aktuell noch vorhandenem Treffer; mehrdeutige oder fehlende
   Treffer bleiben ehrlich ungeklärt, nicht stillschweigend verknüpft.
5. **Ist der Button jetzt nachweislich in der tatsächlich verwendeten
   InventoryPage enthalten?** Ja, strukturell bestätigt (per Codeanalyse,
   die tatsächlich genutzte Zeilen-Detailansicht wurde erweitert) -
   **aber nicht durch einen echten Browsertest verifiziert.**
6. **Werden Android-Inventardaten jetzt im Gerätebestand ausgeliefert?**
   Strukturell ja (dieselbe `PlatformInventorySection`-Komponente wie im
   Geräteidentitäts-Dialog wird eingebunden, sobald eine Verknüpfung
   besteht) - **nicht** durch einen echten Test mit den neun tatsächlichen
   Android-Geräten bestätigt.
7. **Welche Tests konnten wirklich durchgeführt werden?** Ausschließlich
   statische Codeprüfungen (Klammernbilanz, Record-Konstruktor-Abgleich,
   TypeScript-Syntax, manuelle Nachverfolgung der Datenflüsse).
8. **Welche Tests waren mangels echter Geräte/Build-Umgebung nicht
   möglich?** Jeglicher echter Build, jeglicher Test mit den neun
   tatsächlichen Android-Geräten, jeglicher echter Klick-/Browsertest der
   neuen UI-Abschnitte, jeglicher Test der Nachverknüpfungslogik gegen
   eine echte Datenbank mit echten Alt-Datensätzen.
9. **Welche Punkte bleiben offen?** Reale Verifikation mit den neun
   Android-Geräten; reale Prüfung, ob die Best-Effort-Nachverknüpfung für
   die konkret gemeldeten Altgeräte (Sebastian-Doogee-S96-Pro, Fossibot
   F107 Pro) tatsächlich einen Treffer liefert (abhängig davon, ob deren
   Discovery-Identität noch existiert oder durch einen erneuten Suchlauf
   wiederhergestellt wird); Verhalten bei sehr großen Bestandsgrößen.

Der reale Test erfolgt anschließend lokal bei Sebastian - dabei sollte
zuerst „Mit Discovery verknüpfen" für den bestehenden Bestand ausgelöst
und die zurückgemeldete Zusammenfassung (verknüpft/bereits verknüpft/
mehrdeutig/ungeklärt) geprüft werden, bevor die neuen UI-Abschnitte an
einem der neun Android-Geräte betrachtet werden.
