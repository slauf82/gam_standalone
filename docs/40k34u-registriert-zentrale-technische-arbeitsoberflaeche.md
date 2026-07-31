# Schritt 40k34u – Registriert wird die zentrale technische Arbeitsoberfläche

## Neue Rollenverteilung

| Bereich | Rolle | Inhalt |
|---|---|---|
| **Registriert** (`DeviceManagerPanel`/`DeviceIdentityDialog`) | **Technische Arbeitsoberfläche** | Discovery, Geräteidentität, Plattform, Plattforminventarisierung (interaktiv), Linux-/Android-Inventarisierung, Identitätsverlauf, Merge/Zusammenführung, Aliasverwaltung |
| **Gerätebestand** (`InventoryPage`) | **Organisatorisches Asset-Management** | Inventarnummer, Gesellschaft, Filiale, Material/Verbrauch, medizinisch/elektrisch, aktiv/außer Betrieb - **zeigt** die technischen Ergebnisse zusätzlich an, **startet aber keine eigenen Analysen mehr** |

## Warum „Registriert" die technische Arbeitsoberfläche ist

„Registriert" ist bereits seit 40k33b–40k34r der Ort, an dem **jede**
technische Analyse tatsächlich implementiert ist: Discovery-Ergebnisse,
Geräteidentität (`DeviceIdentityDialog`), die Plattforminventarisierung
(`PlatformInventorySection`, 40k34p–s), die Linux-/Android-spezifischen
Detailbereiche, der Identitätsverlauf und die Merge-/Zusammenführungs-
Werkzeuge. Eine Bestandsaufnahme der tatsächlich vorhandenen Abschnitte in
`DeviceIdentityDialog` bestätigt dies:

```text
Identität · Quellenübersicht · Vertrauensbewertung ·
Plattforminventarisierung · Linux-Inventarisierung ·
Android-Inventarisierung · Erweiterte Linux-Analyse ·
Aliasverwaltung · Identitätsverlauf ·
Vorschau der Zusammenführung · Automatisch erkannte Kandidaten
```

Diese Bereiche mussten für 40k34u **nicht neu gebaut** werden - sie
existierten bereits vollständig. Die eigentliche, in diesem Schritt nötige
Änderung betraf ausschließlich die **Rolle des Gerätebestands**.

## Warum der Gerätebestand organisatorisch bleibt

`InventoryDevice` (siehe 40k34t) ist strukturell ein reines Asset-
Verwaltungssystem (Filiale, Gesellschaft, Material, Inventarnummer,
medizinisch/elektrisch) - es war nie als technische Analyseplattform
gedacht. Mit der in 40k34t hergestellten Verknüpfung zur Discovery-
Identität konnte der Gerätebestand die technischen Ergebnisse zwar
**anzeigen** - dabei wurde jedoch (in 40k34t) versehentlich die volle,
**interaktive** `PlatformInventorySection` eingebettet, inklusive der
Aktions-Buttons „Inventarisieren"/„Alle bekannten Plattformen erneut
inventarisieren". Das widersprach dem in 40k34u ausdrücklich geforderten
Prinzip „Registriert bleibt die einzige Stelle, an der technische Analysen
gestartet werden. Der Gerätebestand zeigt diese Ergebnisse lediglich an."

## Durchgeführte Korrektur

`PlatformInventorySection` wurde um eine **optionale** `readOnly`-
Eigenschaft ergänzt (**dieselbe Komponente, keine zweite UI**):

- **„Registriert"** (`DeviceIdentityDialog`): ruft die Komponente weiterhin
  **ohne** `readOnly` auf - volle Interaktivität unverändert erhalten
  („Neu inventarisieren", „Alle bekannten Plattformen erneut
  inventarisieren", je Plattform ein eigener Button).
- **„Gerätebestand"** (`InventoryPage`): ruft dieselbe Komponente jetzt mit
  `readOnly` auf - zeigt Plattform, Status, letzten Lauf, Dauer und
  Meldung **ausschließlich an**, ohne jeden Aktions-Button. Ein klarer
  Hinweistext verweist auf „Registriert" für tatsächliche Aktionen.

Diese Änderung war der **einzige** notwendige Code-Eingriff für 40k34u -
alle übrigen, im Auftrag beschriebenen Anforderungen (Plattform-Anzeige,
Status, Historie, Plattformdetails, Discovery bleibt erhalten, keine
doppelte Inventarisierung) waren durch die bereits bestehende Architektur
(40k34b–t) bereits erfüllt.

## Wiederverwendete Komponenten

`PlatformInventorySection` (40k34p, jetzt mit `readOnly`-Modus),
`LinuxInventorySection`/`LazyLinuxSection` (40k33b6a/b),
`AndroidInventorySection`/`AndroidAppsSection` (40k34b/c/d) -
**keine** dieser Komponenten wurde dupliziert oder durch eine
Parallelimplementierung ersetzt.

## Wiederverwendete APIs

**Keine neue API, kein neuer Dispatcher.** `runPlatformInventory()`,
`runAllKnownPlatforms()`, `loadPlatformInventoryStatus()` (alle seit
40k34p unverändert) bleiben die einzigen Stellen, an denen eine
Plattforminventarisierung ausgelöst oder ihr Status abgefragt wird - jetzt
für beide Bereiche einheitlich über dieselbe Komponente.

## Zukünftige Erweiterbarkeit

Der Auftrag nennt als künftige, technische Erweiterungen u.a. Netzwerk-
analyse, Dienste, Ports, SNMP, ONVIF, SSH, WinRM, ADB als eigene
Unterbereiche sowie Reports. Diese sind **nicht** Bestandteil von 40k34u
(„keine neuen Daten erfinden") - die hier etablierte Rollenverteilung
(„Registriert" = technische Arbeitsoberfläche) sorgt aber dafür, dass
jede künftige technische Erweiterung ausschließlich dort ansetzen muss,
nicht im Gerätebestand - die Architektur ist damit bereits vorbereitet,
ohne dass diese Funktionen jetzt vorgegriffen würden.

## Nicht Bestandteil

Keine neuen Inventardaten, keine neue Discovery, keine neue Historie,
keine neue Statusverwaltung, keine neue Geräteidentität. Discovery bleibt
unverändert („Neue Suchläufe ergänzen/aktualisieren/verbessern bestehende
Geräteidentitäten" - unverändert seit 40k33b/40k34h).

## Durchgeführte statische Prüfungen

**Es konnte in dieser Sandbox weiterhin kein echter Build durchgeführt
werden.** Stattdessen:

- Isolierter TypeScript-Transpile-Lauf über die vollständige `main.tsx` -
  0 Diagnosen.
- Manuelle Bestandsaufnahme der tatsächlich vorhandenen `<h3>`-Abschnitte
  in `DeviceIdentityDialog` (siehe oben) - bestätigt, dass „Registriert"
  bereits eine umfassende technische Arbeitsoberfläche ist.
- Manuelle Prüfung: beide Aufrufstellen von `PlatformInventorySection`
  identifiziert und bestätigt - „Registriert" ohne `readOnly` (interaktiv),
  „Gerätebestand" mit `readOnly` (nur Anzeige).

```text
fachlich umgesetzt (Rollentrennung hergestellt, Gerätebestand ist jetzt
  nachweislich nur-lesend für Plattforminventarisierung)
statisch geprüft
nicht durch echten Build oder echten Praxistest verifiziert
```

## Ehrliche Abschlussdokumentation (explizit beantwortet)

1. **Ist der Registriert-Bereich jetzt die zentrale technische
   Arbeitsoberfläche?** Ja - er war es bereits seit mehreren vorherigen
   Schritten (40k34b–s); 40k34u hat dies formalisiert und die einzige
   noch bestehende Abweichung (interaktive Buttons im Gerätebestand)
   korrigiert.
2. **Können alle technischen Inventarisierungen direkt dort gestartet
   werden?** Ja, für Android, Linux, Windows (lokal und remote) - über
   die bereits bestehende `PlatformInventorySection` sowie die
   plattformspezifischen Detailbereiche.
3. **Verwendet der Gerätebestand ausschließlich die Ergebnisse dieser
   Inventarisierungen?** Ja, seit dieser Korrektur nachweislich - die
   Aktions-Buttons wurden entfernt, nur die reine Anzeige bleibt.
4. **Existiert weiterhin nur eine Geräteidentität?** Ja - unverändert
   `identityKey`/`gam_discovery_registered_devices`, keine zweite
   eingeführt.
5. **Existiert weiterhin nur eine Plattforminventarisierung?** Ja -
   `DeviceIdentityService.runPlatformInventory()`, unverändert seit
   40k34p.
6. **Existiert weiterhin nur eine Hardwareanalyse?** Ja - dieselben,
   bereits vorhandenen Inventarisierer (Android/Linux/Windows), keine
   zweite ergänzt.
7. **Existiert weiterhin nur eine Historie?** Ja -
   `PlatformInventoryRepository`/`gam_discovery_platform_inventory_runs`,
   unverändert seit 40k34p.
8. **Welche Punkte bleiben für zukünftige Schritte offen?** Die im
   Auftrag als künftig genannten technischen Unterbereiche (Netzwerk-
   analyse, Dienste, Ports, SNMP, ONVIF, SSH/WinRM/ADB als eigene
   Detailkarten, Reports) wurden **nicht** vorgezogen - sie sind
   ausdrücklich als spätere Erweiterungen benannt, nicht Teil von 40k34u.
   Ebenfalls weiterhin offen: der reale Test mit den neun tatsächlichen
   Android-Geräten (seit 40k34s/t unverändert ausstehend).
