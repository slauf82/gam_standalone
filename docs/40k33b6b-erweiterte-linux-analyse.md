# Schritt 40k33b6b – Erweiterte Linux-Analyse

## Ziel

40k33b6a hat Linux-Systeme vollständig inventarisiert (Bereiche werden
automatisch beim Suchlauf gefüllt). 40k33b6b ergänzt umfangreiche, potenziell
große Zusatzinformationen (installierte Software, Docker, Podman, Snap,
Flatpak, Virtualisierung, Container, Entwicklungsumgebung, Monitoring) -
ausdrücklich NICHT automatisch während des Suchlaufs, sondern nur auf
Anforderung (Lazy Loading), um die Discovery nicht zu verlangsamen.

## Geänderte/neue Dateien

Backend:
- `backend/.../inventory/LinuxNetworkDiscoveryService.java` – die SSH-
  Ausführung wurde in eine gemeinsame Methode `runRemoteCommand()`
  extrahiert (von der bestehenden `sshInventory()` unverändert weiterverwendet)
  und um `fetchSection()` ergänzt: ruft auf Anforderung genau einen der neun
  neuen Bereiche per SSH ab, mit 10-minütigem In-Memory-Zwischenspeicher je
  Gerät und Bereich.
- `backend/.../inventory/LinuxOnDemandController.java` (neu) – dünner
  REST-Wrapper: löst den Identitätsschlüssel über die bereits vorhandene
  `DiscoveryRegistrationRepository` in eine IP-Adresse auf und delegiert den
  eigentlichen Abruf vollständig an `LinuxNetworkDiscoveryService`.

Frontend:
- `frontend/src/api/client.ts` – Typ `LinuxOnDemandSection` und
  `loadLinuxOnDemandSection()`.
- `frontend/src/main.tsx` – neue Komponente `LazyLinuxSection` (ein
  einklappbarer Bereich, der erst beim Öffnen abruft und für die Dauer des
  geöffneten Dialogs zwischenspeichert), eingebunden im bestehenden Dialog
  „Geräteidentität“ unter einer neuen Überschrift „Erweiterte Linux-Analyse“
  - nur sichtbar, wenn `identity.platform === 'Linux'`.

Dokumentation:
- `docs/40k33b6b-erweiterte-linux-analyse.md` (diese Datei).

## Architekturentscheidungen

- **Keine zweite Softwareverwaltung, keine zweite Linux-Erkennung:** Der
  gesamte neue Abruf läuft über dieselbe SSH-Verbindungslogik, dieselben
  Zugangsdaten-Einstellungen (`gam.discovery.linux.ssh-user/-key/-port`) und
  denselben Discovery-Baustein wie die reguläre Linux-Inventarisierung
  (`LinuxNetworkDiscoveryService`). Es gibt keine zweite Konfiguration, keine
  zweite Verbindungsmethode.
- **Keine zweite Identitätsverwaltung:** Der Identitätsschlüssel wird über die
  bereits vorhandene `DiscoveryRegistrationRepository.find()` aufgelöst - es
  entsteht keine neue Geräte- oder Identitätstabelle. Die neuen Bereiche
  werden im bestehenden Dialog „Geräteidentität“ (40k33b5) angezeigt, nicht in
  einer zusätzlichen Ansicht.
- **Lazy Loading:** Jeder der neun Bereiche wird ausschließlich beim Öffnen
  des jeweiligen `<details>`-Elements abgerufen (`onToggle`), nicht beim
  Laden des Dialogs und nicht während des regulären Suchlaufs.
- **Zwischenspeicherung:** Serverseitig 10 Minuten je Gerät und Bereich
  (In-Memory, kein neues Datenbankschema), zusätzlich hält der Dialog das
  Ergebnis für die Dauer seiner Öffnung im Frontend vor - erneutes
  Auf-/Zuklappen desselben Bereichs verursacht keinen weiteren Netzwerk-
  zugriff.
- **Lokale Suche:** Größere Listen (z.B. installierte Pakete) können direkt im
  Browser gefiltert werden, ohne erneuten Serverzugriff.
- **Herkunft/Zeitpunkt:** Jeder Bereich zeigt „Quelle: Linux-Discovery
  (On-Demand) · aus Zwischenspeicher/gerade abgerufen · <Zeitpunkt>“.

## Neue Bereiche

| Bereich | Inhalt | Voraussetzung |
|---|---|---|
| Installierte Software | vollständige Paketliste (dpkg/rpm/pacman/apk) | SSH-Zugang |
| Docker | laufende/gestoppte Container + Images | `docker` installiert |
| Podman | laufende/gestoppte Container + Images | `podman` installiert |
| Snap | installierte Snap-Pakete | `snap` installiert |
| Flatpak | installierte Flatpak-Anwendungen | `flatpak` installiert |
| Virtualisierung | erkannte Virtualisierungsplattform, `virsh`/`kvm-ok` falls vorhanden | - |
| Container | zusammengefasste Docker-/Podman-Container-Übersicht | Docker oder Podman |
| Entwicklungsumgebung | Versionen von git/docker/node/python3/java/gcc/make/go/rustc | jeweiliges Werkzeug installiert |
| Monitoring | aktive Monitoring-Agenten (node_exporter, Zabbix, Netdata, Glances, Telegraf, collectd) | jeweiliger Dienst installiert |

Fehlt ein Werkzeug oder ist kein SSH-Zugang konfiguriert, wird das dem
Benutzer verständlich angezeigt (kein Fehlerabbruch, keine leere Seite).

## Neue REST-Endpunkte

- `GET /api/inventory/discovery/linux/section?identityKey=...&section=...`

## Neue Datenbankmigrationen

Keine. Der Zwischenspeicher ist bewusst rein In-Memory (kurzlebig, pro
Backend-Prozess) - es entsteht keine neue Tabelle.

## Tests / durchgeführte Prüfungen in dieser Umgebung

Wie in den vorangegangenen Schritten: kein JDK, kein funktionierendes
`npm install` in dieser Sandbox - kein echter Build möglich. Stattdessen
geprüft:

- Klammern-/Parameterbilanz von `LinuxNetworkDiscoveryService.java` und
  `LinuxOnDemandController.java` (ausgeglichen).
- Jedes der neun neu erzeugten Shell-Kommandos wurde einzeln extrahiert und
  mit `bash -n` auf Syntaxfehler geprüft (alle neun fehlerfrei).
- Isolierter TypeScript-Transpile-Lauf über `client.ts` und die vollständige
  `main.tsx` (0 Diagnosen). Dabei wurde beim Einfügen der neuen Komponente
  eine vertauschte Doc-Kommentar-Zuordnung bemerkt und korrigiert (rein
  kosmetisch, keine Funktionsauswirkung, aber zur Klarheit behoben).

**Nicht durchgeführt:** echter Maven-/Vite-Build, automatisierte Tests, Test
gegen ein echtes Linux-System mit Docker/Podman/Snap/Flatpak. Bitte vor dem
Produktiveinsatz nachholen.

## Bekannte Einschränkungen

- Der In-Memory-Zwischenspeicher ist prozesslokal - bei einem Neustart des
  Backends oder in einer Umgebung mit mehreren Backend-Instanzen wird der
  erste Abruf nach einem Neustart erneut per SSH ausgeführt.
- Die Rollenerkennung/Werkzeugerkennung bleibt eine Heuristik über bekannte
  Programmnamen, keine vollständige Inventarisierung jeder denkbaren
  Software.
- Container-/Image-Listen werden als einfacher Text (eine Zeile je Eintrag)
  dargestellt, nicht als strukturierte Tabelle mit Sortierung o.Ä. - das war
  mit dem vorgegebenen Rahmen ("keine doppelten Ansichten", Wiederverwendung
  der bestehenden Listendarstellung) am konsistentesten umsetzbar.

## Manuelle Testanleitung

1. Dialog „Geräteidentität“ für ein als Linux erkanntes Gerät öffnen: unter
   „Erweiterte Linux-Analyse“ müssen alle neun Bereiche eingeklappt
   erscheinen, ohne dass automatisch ein Netzwerkzugriff stattfindet.
2. Bereich „Installierte Software“ öffnen: es muss kurz „Wird direkt von
   diesem Gerät abgerufen …“ erscheinen, danach die Paketliste mit lokalem
   Filterfeld.
3. Denselben Bereich schließen und erneut öffnen: es darf kein erneuter
   Ladezustand mehr erscheinen (aus dem Dialog-Zwischenspeicher).
4. Bereich „Docker“ auf einem Host ohne Docker öffnen: verständlicher
   Hinweis statt Fehler oder leerer Seite.
5. Ein Windows-Gerät öffnen: der Abschnitt „Erweiterte Linux-Analyse“ darf
   dort nicht erscheinen.
6. Regressionstest: die bereits bestehenden Bereiche aus 40k33b6a
   (Betriebssystem, Hardware, Netzwerk, Dienste, Paketmanager, Rollen,
   Dateisystem, Linux-Analyse) müssen unverändert weiter funktionieren.
