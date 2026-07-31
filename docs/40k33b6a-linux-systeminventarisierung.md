# Schritt 40k33b6a – Linux-Systeminventarisierung

## Ziel

Linux wird nicht mehr nur als Betriebssystem erkannt, sondern möglichst
vollständig inventarisiert - ausschließlich als Erweiterung der bereits
bestehenden Geräteidentität (keine neuen Geräte, keine Änderung der
Merge-Logik).

## Geänderte Dateien

Backend:
- `backend/.../inventory/LinuxNetworkDiscoveryService.java` – zusätzliche
  credential-freie Rollen-Indizien (Ports 80/443/3306/5432), erweitertes
  SSH-Inventar-Skript (Distribution, Laufzeit, Standardgateway, DNS-Server,
  Paketmanager, Namen aktualisierbarer Pakete), neue `detectRoles()`-
  Ableitung, neue Protokoll-Detailfelder.
- `backend/.../inventory/DeviceIdentityConfidenceEngine.java` – neue,
  rein additive Konflikterkennung: abweichende, gültige IP-Adressen bei
  gleichzeitig als „ONLINE“ gemeldeten Geräten werden jetzt als Konflikt
  vermerkt. Keine Änderung an Gewichtung/Schwellenwerten/Entscheidungslogik.
- `backend/.../inventory/DeviceMergeService.java` – `Candidate` um
  IP/MAC beider Seiten sowie eine reine Anzeige-„Warnstufe“
  (🟢/🟡/🔴, abgeleitet aus der bereits bestehenden Entscheidung/
  Konfliktliste) erweitert.

Frontend:
- `frontend/src/api/client.ts` – `DeviceMergeCandidate` um die neuen
  Anzeigefelder erweitert.
- `frontend/src/main.tsx` – Kandidatentabelle im Zusammenführungsdialog um
  Warnstufen-Spalte und tabellarische Unterschieds-Ansicht (IP/MAC/Kategorie
  nebeneinander, abweichende Werte hervorgehoben) ergänzt; die bestehenden,
  generischen Geräte-Detailabschnitte wurden auf die geforderten,
  einklappbaren Linux-Bereichsnamen umgruppiert (Betriebssystem, Hardware,
  Netzwerk, Dienste, Paketmanager, Rollen, Dateisystem, Sicherheit & Wartung,
  Linux-Analyse) - dieselbe bereits vorhandene Pfeil-/`<details>`-Struktur,
  keine neue Ansicht.
- `frontend/src/style.css` – rein additive Regeln für die neue
  Warnstufen-/Konflikt-Hervorhebung.

Dokumentation:
- `docs/40k33b6a-linux-systeminventarisierung.md` (diese Datei).

## Architekturregeln eingehalten

- **Keine neuen Geräte / keine neue Linuxverwaltung:** Alle neuen Felder
  fließen ausschließlich in den bereits bestehenden `discovery_protocol`-Text
  derselben Geräteidentität (`DiscoveredDevice`/`gam_discovery_registered_devices`).
  Keine neue Tabelle, keine neue Datenstruktur.
- **Keine doppelte Ansicht:** Die neuen Abschnittsüberschriften nutzen exakt
  dieselbe, bereits vorhandene `registeredDetailSections()`-Kategorisierung
  und dieselbe `<details>`/Pfeil-Komponente wie bisher - nur mit
  granularerer, den Vorgaben entsprechender Titelvergabe. Es wurde keine
  zweite Detailansicht gebaut.
- **Keine Änderung der Merge-Logik:** Weder Schwellenwerte noch die
  Entscheidung (`AUTO_MERGE`/`POSSIBLE_DUPLICATE`/`DISTINCT`) wurden
  verändert. Die neue IP-Konflikterkennung ergänzt lediglich die bereits
  bestehende Konfliktliste (die schon vorher über `hardConflictsBlockMerge`
  automatische Zusammenführungen bei Konflikten verhindert hat) und die
  Warnstufe ist eine reine, nachgelagerte Anzeige-Klassifikation.

## Geräteidentität / Nachvollziehbarkeit

Jede neue Information erscheint weiterhin - wie schon in 40k33b3/b4 - als
`Label: Wert`-Eintrag im `discovery_protocol`-Text derselben Identität und
damit automatisch:

- in der bestehenden Quellenübersicht (`discoverySourceBadges`, seit
  40k33b4/b5 unverändert wiederverwendet),
- im bestehenden Identitätsverlauf (40k33b5), sofern sich dadurch die
  Kategorie ändert (bereits vorhandene `gam_discovery_device_type_history`),
- in den neuen, einklappbaren Detailabschnitten dieses Schritts.

Ob eine Information automatisch oder manuell ergänzt wurde, ist weiterhin
über die bereits vorhandenen `manual_name`/`manual_device_type`-Kennzeichnungen
(40k33b4/b5) ersichtlich - hierfür war keine Änderung nötig.

## Neue Linux-Inventarfelder (SSH-Tiefeninventarisierung, nur mit Zugangsdaten)

| Feld | Bereich | Bedeutung |
|---|---|---|
| Distributions-ID / -Version | Betriebssystem | `/etc/os-release` ID/VERSION_ID |
| Laufzeit seit Start | Betriebssystem | `uptime -p` bzw. `/proc/uptime` |
| Standardgateway | Netzwerk | `ip route show default` |
| DNS-Server | Netzwerk | `/etc/resolv.conf` |
| Paketmanager | Paketmanager | erkannter Paketmanager (apt/dnf/yum/pacman/zypper) |
| Aktualisierbare Pakete | Paketmanager | bis zu 6 Paketnamen (zusätzlich zur bereits bestehenden Update-Anzahl) |
| Erkannte Rollen | Rollen | aus bereits erfassten Diensten/Ports abgeleitet (Webserver, Datenbankserver, Containerhost, Fileserver, SSH-Fernzugriff, Cockpit, Virtualisierungshost, Smart-Home-Zentrale, Monitoring) |

Zusätzlich ohne Zugangsdaten (rein netzwerkbasiert, credential-frei): Ports
80/443 (Webserver-Indiz), 3306/5432 (Datenbank-Indiz) werden jetzt zusätzlich
zu den bereits vorhandenen Diensten geprüft und fließen in „Erkannte Rollen“
ein.

## Merge-Konflikte deutlicher hervorheben

- Neue Warnstufe je Zusammenführungskandidat: 🟢 Sehr sicher (automatische
  Zusammenführung würde greifen, keine Konflikte), 🟡 Bitte prüfen (möglicher
  Kandidat ohne harten Konflikt), 🔴 Hoher Konflikt (mindestens ein harter
  Konflikt, z.B. abweichende MAC/Seriennummer, oder - neu - abweichende
  gültige IP-Adressen bei gleichzeitig erreichbaren Geräten).
- Bei jedem Konflikt zeigt der Zusammenführungsdialog jetzt zusätzlich eine
  kleine Vergleichstabelle (IP-Adresse, MAC-Adresse, Kategorie
  nebeneinander), abweichende Werte rot hervorgehoben.
- Automatische Zusammenführung bleibt bei 🔴-Konflikten weiterhin
  ausgeschlossen (unverändertes Verhalten aus 40k33b4: harte Konflikte
  begrenzen den erreichbaren Punktwert unter die Auto-Merge-Schwelle).

## Datenbankmigrationen

Keine. Alle neuen Informationen sind zusätzliche `Label: Wert`-Einträge im
bereits vorhandenen `discovery_protocol`-Textfeld bzw. zusätzliche,
abgeleitete Felder in bestehenden API-Antworten (`Candidate`). Keine neue
Tabelle, keine neue Spalte.

## Tests / durchgeführte Prüfungen in dieser Umgebung

Wie in den vorherigen Schritten hat diese Sandbox **kein JDK** (nur JRE, kein
Internetzugriff zum Nachinstallieren) und **kein funktionierendes
`npm install`** (Registry antwortet mit 403). Ein echter
`mvn package`/`npm run build`-Lauf sowie automatisierte Tests waren daher
technisch nicht möglich. Stattdessen wurde geprüft:

- Klammern-/Parameterbilanz aller geänderten Java-Dateien (ausgeglichen).
- Das per Java-String zusammengesetzte SSH-Remote-Inventarskript wurde
  extrahiert und mit `bash -n` auf Shell-Syntaxfehler geprüft (fehlerfrei,
  inkl. neuer Felder).
- Isolierter TypeScript-Transpile-Lauf (`ts.transpileModule`) über
  `client.ts` und die vollständige `main.tsx` - jeweils 0 Diagnosen.
- Manueller Abgleich der CSS-Spezifität der neuen `.warn`-Regel gegen
  bestehende, spezifischere Selektoren (`.note.warn`, `.report-kpi-grid
  .warn`), um sicherzustellen, dass keine bestehende Darstellung überschrieben
  wird.

**Nicht durchgeführt** (nicht ausführbar in dieser Umgebung): echter
Maven-/Vite-Build mit vollständiger Typprüfung, Ausführung automatisierter
Tests, Test gegen einen echten Linux-Host mit SSH-Zugangsdaten. Bitte vor dem
Produktiveinsatz nachholen.

## Bekannte Einschränkungen

- Die neuen Detailfelder (Distribution, Laufzeit, Gateway, DNS, Paketmanager,
  aktualisierbare Pakete, Rollen) stehen nur zur Verfügung, wenn die
  optionale SSH-Tiefeninventarisierung (gemeinsamer SSH-Nutzer/-Schlüssel)
  konfiguriert ist - ohne Zugangsdaten liefert die Discovery weiterhin nur
  Dienst-Fingerprinting (jetzt zusätzlich inkl. Web-/Datenbank-Port-Indizien
  für „Erkannte Rollen“).
- „Rollen“ werden heuristisch aus bereits erfassten Diensten/Ports
  abgeleitet, nicht durch eine vollständige Konfigurationsanalyse (z.B. wird
  nicht geprüft, ob ein erkannter Webserver tatsächlich produktiv genutzt
  wird).
- Die Warnstufen-Ampel ist eine reine Anzeige-Klassifikation der bereits
  bestehenden Entscheidung/Konfliktliste, keine neue, unabhängige
  Risikobewertung.

## Manuelle Testanleitung

1. **Ohne SSH-Zugangsdaten:** Ein Linux-Gerät mit offenem Port 80 oder 3306
   im Netzwerk simulieren/prüfen - im Abschnitt „Rollen“ sollte „Webserver“
   bzw. „Datenbankserver“ erscheinen, auch ohne SSH-Zugangsdaten.
2. **Mit SSH-Zugangsdaten:** Nach einem Suchlauf gegen ein konfiguriertes
   Linux-System die neuen, einklappbaren Bereiche prüfen: „Betriebssystem“
   (Distribution/Version/Laufzeit), „Netzwerk“ (Gateway/DNS), „Paketmanager“
   (erkannter Paketmanager + einzelne aktualisierbare Paketnamen), „Rollen“.
   Zunächst dürfen nur die Überschriften sichtbar sein.
3. **Merge-Konflikt-Ampel:** Im Dialog „Gerätezusammenführung prüfen“ einen
   Kandidaten mit bekanntem Konflikt (z.B. zwei unterschiedliche MAC-Adressen
   oder unterschiedliche IPs bei zwei aktuell „ONLINE“ gemeldeten Geräten)
   suchen - es muss 🔴 „Hoher Konflikt“ mit aufklappbarer Vergleichstabelle
   erscheinen, und „Zusammenführung bestätigen“ darf ohne ausdrückliche
   Bestätigung nicht möglich sein (unverändertes Verhalten aus 40k33b4).
4. **Keine Regression:** Ein bereits bekanntes, unauffälliges Windows- oder
   generisches Netzwerkgerät öffnen - die bestehenden Abschnitte
   (Betriebssystem/Hardware/Sicherheit & Wartung) müssen weiterhin wie zuvor
   befüllt und einklappbar sein.
