# Schritt 40k33b9 – Linux-Inventarisierung ohne SSH optimieren

## Ziel

SSH bleibt ausdrücklich die vollständigste Inventarisierungsquelle - dieser
Schritt ersetzt SSH nicht, sondern nutzt bereits vorhandene Discoveryquellen
(Netzwerkscanner/Port-Probing, HTTP, SNMP, mDNS, Home Assistant) intelligenter,
damit Linux-Systeme auch ohne eingerichteten SSH-Zugang möglichst umfangreich
beschrieben werden. Es werden ausschließlich Informationen verwendet, die
tatsächlich von einer bereits vorhandenen Quelle stammen - keine Schätzungen.

## Wichtiger Fund: SNMP-Systembeschreibung wurde bisher verworfen

`SnmpDiscoveryService.query()` liest bereits die vollständige `sysDescr`
(OID `1.3.6.1.2.1.1.1.0`) - bei den meisten Linux-Systemen mit net-snmp im
Standardformat `Linux <Hostname> <Kernel-Version> #... <Datum> <Architektur>`.
Diese Zeichenkette wurde jedoch **nirgends gespeichert**: `DeviceDiscoveryService`
hat beim Anlegen des SNMP-Treffers nur die feste Zeichenkette `"SNMP v1"` als
Protokoll verwendet, die tatsächlich gemeldete Systembeschreibung ging
vollständig verloren. Das war die größte, konkret behebbare Lücke für eine
Linux-Erkennung ohne SSH und wurde in diesem Schritt behoben.

## Geänderte Dateien

Backend:
- `backend/.../inventory/DeviceDiscoveryService.java` – SNMP-sysDescr wird
  jetzt als Detailfeld gespeichert (nicht mehr verworfen); SNMP läuft jetzt
  bewusst **vor** der Linux-Netzwerkerkennung (reine Reihenfolgeänderung
  bereits vorhandener, unveränderter Schritte - keine neue Discovery).
- `backend/.../inventory/LinuxNetworkDiscoveryService.java` – neue,
  credential-freie Port-Sonden (IPP/CUPS, Docker-API, Webmin), einfache
  HTTP-Titelerkennung bekannter Linux-Weboberflächen, echte (nicht geschätzte)
  SNMP-Kernel-/Hostname-Übernahme, erweiterte Rollenerkennung mit
  Mehrquellen-Kombinationen, Inventarisierungsgrad- und
  Quellenübersichts-Berechnung, zusätzliches Debug-Logging.

Frontend:
- `frontend/src/main.tsx` – `LinuxInventorySection` (aus 40k33b8) zeigt jetzt
  Inventarisierungsgrad, verwendete Quellen (✔) und was zusätzlich über SSH
  verfügbar wäre; **Fehlerbehebung:** „SSH nicht konfiguriert/nicht
  erreichbar“ wurde bisher fälschlich als „Fehlermeldung“ dargestellt - das
  ist jetzt ein neutraler, informativer Hinweis, wie im Auftrag explizit
  gefordert.

Dokumentation:
- `docs/40k33b9-linux-ohne-ssh.md` (diese Datei).

## Architekturentscheidungen

- **Keine neue Discovery, keine zweite SNMP-/HTTP-Implementierung:** Die
  HTTP-Titelerkennung nutzt `java.net.http.HttpClient` (bereits im JDK
  enthalten) und wird ausschließlich für Ports ausgeführt, die die bereits
  bestehende `probe()`-Methode schon als offen erkannt hat - kein
  zusätzlicher, unabhängiger Scan. Die SNMP-Auswertung nutzt ausschließlich
  den bereits von `SnmpDiscoveryService` gelieferten Text.
- **Reihenfolgeänderung statt neuer Architektur:** SNMP lief bisher NACH der
  Linux-Netzwerkerkennung und konnte deshalb innerhalb desselben Suchlaufs nie
  zur Linux-Einstufung beitragen. Die Reihenfolge wurde geändert (SNMP vor
  Windows-/Linux-Inventarisierung), ohne die SNMP- oder Linux-Logik selbst zu
  verändern.
- **Keine unzuverlässigen Schätzungen:** Ein Kernel-Fallback wird
  ausschließlich verwendet, wenn die SNMP-sysDescr exakt dem bekannten
  net-snmp-Format `Linux <Host> <Kernel>` entspricht (Regex-Prüfung) - sonst
  bleibt das Feld leer, es wird nichts geraten.
- **SSH bleibt optional, "nicht eingerichtet" ist kein Fehler:** Sowohl im
  Backend-Statustext als auch im Frontend wird dieser Zustand jetzt
  ausdrücklich als normaler, informativer Hinweis dargestellt (siehe
  Fehlerbehebung oben).

## Neue/erweiterte Signale ohne SSH

- **Port-Sonden (bereits vorhanden, jetzt erweitert):** IPP/CUPS (631,
  Druckserver), Docker-API (2375, Containerhost), Webmin (10000,
  Verwaltungsoberfläche).
- **HTTP-Titelerkennung** (nur für bereits offene Web-Ports 80/443/9090/
  8006/8123/3000/9000/10000): Cockpit, CasaOS, OpenMediaVault, Portainer,
  Webmin, Nextcloud, Grafana, Prometheus, Proxmox, Home Assistant.
- **SNMP-Systembeschreibung:** vollständig gespeichert statt verworfen;
  liefert bei vielen Linux-Systemen bereits Hostname und Kernel-Version ohne
  SSH.
- **Erweiterte Rollenerkennung** (Kombination mehrerer Quellen, wie im
  Auftrag gefordert):
  - SSH + HTTP + Container-Hinweis → „Docker Host“
  - HTTP + Samba → „Dateiserver“
  - HTTP + Home Assistant → „Home Assistant Server“
  - IPP/CUPS-Port offen → „Druckserver“ (SNMP liefert bereits seit Vorschritt
    eine "Drucker / Scanner"-Kategorisierung für dasselbe Gerät, sofern per
    SNMP erkennbar - wird über die gemeinsame Geräteidentität sichtbar, siehe
    Einschränkungen unten)
  - CasaOS/OpenMediaVault-Weboberfläche erkannt → „NAS-Weboberfläche erkannt“

## Inventarisierungsgrad und Quellenübersicht

Neue Detailfelder (über dieselbe bestehende „· Label: Value“-Konvention wie
alle anderen Linux-Felder, keine neue Datenstruktur):

- `Inventarisierungsgrad` – Prozentsatz befüllter Informationskategorien
  (Hostname, Betriebssystem, Kernel, Architektur, CPU, RAM, Datenträger,
  Netzwerk, Rollen, Erkennungsmerkmale, Virtualisierung, SSH-Hostkey),
  unabhängig davon ob mit oder ohne SSH ermittelt.
- `Verwendete Quellen` – welche Quellen tatsächlich beigetragen haben
  (Netzwerkscanner, HTTP/HTTPS, SNMP, mDNS/Avahi, Home Assistant, SSH).
- `Zusätzlich über SSH verfügbar` – nur gesetzt, wenn SSH (noch) keine Daten
  geliefert hat; listet auf, was eine SSH-Einrichtung zusätzlich ermöglichen
  würde (exakte Distribution/Kernel, Pakete, vollständige Dienste,
  Dateisysteme, SSH-Hostkey).

## Logging

`LinuxNetworkDiscoveryService` protokolliert jetzt zusätzlich auf Debug-Ebene
den erreichten Inventarisierungsgrad, die tatsächlich verwendeten Quellen und
die daraus abgeleiteten Rollen für jedes geprüfte Ziel.

## Tests / durchgeführte Prüfungen in dieser Umgebung

Kein JDK, kein funktionierendes `npm install` in dieser Sandbox - kein echter
Build möglich. Stattdessen geprüft:

- Klammernbilanz (`{}`) aller geänderten Backend-Dateien (ausgeglichen). Die
  rohe Klammernzählung für `()` zeigte eine scheinbare Abweichung von 2, die
  sich als Fehlalarm herausstellte: zwei String-Literale enthalten absichtlich
  ein einzelnes `(` ohne Gegenstück (`"(TCP "`, `"(HTTP"`) - eine reine
  Text-Zählung ohne Java-Syntaxverständnis zählt das fälschlich mit. Keine
  echte Unausgeglichenheit im Code.
- Das (unveränderte) Remote-Inventarisierungsskript wurde erneut mit
  `bash -n` geprüft (weiterhin fehlerfrei).
- Isolierter TypeScript-Transpile-Lauf über die vollständige `main.tsx` - 0
  Diagnosen.
- Kreuzabgleich aller neuen Methodennamen zwischen Definition und Aufruf.

**Nicht durchgeführt:** echter Maven-/Vite-Build, automatisierte Tests, Test
gegen echte Systeme mit Cockpit/CasaOS/OpenMediaVault/Portainer/Webmin/
Nextcloud/Grafana/Prometheus zur Bestätigung der HTTP-Titelerkennung, sowie
gegen ein echtes SNMP-fähiges Linux-System zur Bestätigung des
Kernel-Fallbacks. Bitte vor dem Produktiveinsatz an echten Geräten
verifizieren.

## Bekannte Einschränkungen

- Die HTTP-Titelerkennung ist eine einfache Textsuche im Antworttext, keine
  vollständige Fingerprinting-Bibliothek - bei stark angepassten oder
  passwortgeschützten Oberflächen (Login-Seite ohne Produktname im HTML)
  kann die Erkennung fehlschlagen; das wird nicht als Fehler behandelt,
  sondern das Feld bleibt einfach leer.
- Der Kernel-Fallback aus SNMP funktioniert ausschließlich mit dem
  net-snmp-Standardformat; abweichende SNMP-Implementierungen oder
  benutzerdefinierte sysDescr-Texte werden nicht geraten und liefern keinen
  Kernel-Wert.
- "SNMP + Druckdienste → Druckserver" wird über den IPP/CUPS-Port-Probe
  (631) abgebildet, nicht über eine dedizierte SNMP-Drucker-MIB-Auswertung -
  eine solche würde zusätzliche SNMP-OID-Abfragen erfordern, was über die
  bereits vorhandene, bewusst minimale SNMP-Implementierung hinausginge.
- Es gibt weiterhin keine eigenständige ONVIF-Auswertung in der
  Linux-Inventarisierung, da im Projekt keine dedizierte ONVIF-Discovery-
  Quelle existiert, die wiederverwendet werden könnte.

## Manuelle Testanleitung

1. Ein per SNMP erreichbares Linux-System OHNE konfigurierten SSH-Zugang
   scannen: im Bereich „Linux-Inventarisierung“ müssen Hostname und Kernel
   (aus der SNMP-Systembeschreibung) erscheinen, ohne dass ein SSH-Fehler
   angezeigt wird.
2. Bei diesem Gerät muss unter „Quellen“ mindestens „✔ SNMP“ und „⚠ SSH nicht
   eingerichtet (optional)“ erscheinen - letzteres NICHT als „Fehlermeldung“.
3. Ein Linux-System mit laufendem Cockpit/Portainer/Grafana ohne SSH scannen:
   die entsprechende Weboberfläche muss unter „Erkennungsmerkmale“/„Erkannte
   Rollen“ auftauchen.
4. Inventarisierungsgrad prüfen: ein Gerät mit SSH sollte einen höheren
   Prozentsatz zeigen als dasselbe Gerät ohne SSH, aber auch ohne SSH sollte
   der Wert deutlich über 0 % liegen, wenn mehrere Quellen etwas beigetragen
   haben.
5. Regressionstest: bestehende SSH-basierte Inventarisierung (40k33b3/b6a/b8)
   muss unverändert weiter vollständige Daten liefern; bestehende
   Zusammenführung, Aliasverwaltung, Integritätsprüfung müssen unverändert
   funktionieren.
