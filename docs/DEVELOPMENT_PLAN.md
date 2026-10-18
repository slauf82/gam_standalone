# GAM – Entwicklungsplan / Development Roadmap

**Projekt:** GAM Standalone / GAM 2.0  
**Ausgangsversion:** 2.1.0 (Release-Basis: 40k37e56c-fix48)  
**Stand:** 18. Oktober 2026  
**Status:** Lebender Entwicklungsleitfaden; geplante Versionen ab 2.1.5 sind **Zielvorschläge, keine verbindlichen Zusagen**.

## 1. Leitbild

GAM soll sich von einer eigenständig nutzbaren, plattformübergreifenden Anwendung zu einer **modularen Verwaltungs- und Inventarisierungsplattform** entwickeln. Der **öffentlich nutzbare GAM Core** funktioniert dauerhaft ohne kommerzielle Add-ons. Zusätzliche Module können unabhängig entwickelt, versioniert, installiert und – falls vorgesehen – lizenziert werden.

Die Entwicklung priorisiert **Datenintegrität, nachvollziehbare Zustände, Stabilität, Protokollierung, Rückwärtskompatibilität und sichere Berechtigungen** vor Funktionsfülle. Bereits nachweislich funktionierende Plattformpfade werden nicht ohne konkreten Anlass umgebaut.

## 2. Ausgangspunkt: GAM 2.1.0

Die Version 2.1.0 bildet die technische Ausgangsbasis für die weitere Entwicklung:

- Mehrquellen-Geräteerkennung und -inventarisierung: Windows (lokal, WinRM), Linux (lokal, SSH), macOS (lokal, SSH), Android (ADB) sowie iOS/iPadOS (Apple-Werkzeugkette, USB und entwickelter WLAN-Pfad).
- Geräteidentität, überprüfbare Zusammenführung und Trennung, Quellenzuordnung und umfangreiche Inventarisierungsdetails.
- Erfolgreich getestete Windows-WinRM-Inventarisierung mit Windows-nativem SSPI-Fallback; Linux lokal und über SSH erfolgreich nachgeprüft.
- Bereinigte Darstellung historischer Quellen, korrigierte Gerätetabellenbreiten und Beseitigung des störenden `whoami.exe`-Prozessfehlers.
- Release-Paket mit dokumentierter Versionsnummer 2.1.0 als Ausgangspunkt.

**Wichtig:** Testergebnisse einzelner Wege sind keine Garantie, dass jede Plattform-/Versions-/Hardwarekombination bereits umfassend abgedeckt ist. Neue Releases erhalten jeweils einen eigenen Regressionstest.

## 3. Versionsübersicht

| Version | Schwerpunkt | Wesentliche Ziele | Status |
| --- | --- | --- | --- |
| **2.1.0** | Stabile Plattformbasis | Plattformübergreifende Inventarisierung, Geräte-/Quellenverwaltung, Stabilisierung | **Funktional abgenommen; Release-Basis fix48** |
| **2.1.5** | Core- und Modulgrundlagen | Öffentlicher Core, Modulgrenzen, Modulmanager, APIs, `.gam`-Paketformat | Geplant |
| **2.2.0** | Infrastrukturinventarisierung | Zusätzliche Netzwerkgeräte und Protokolle, Virtualisierungs-/Containerquellen | Geplant; Umfang zu priorisieren |
| **2.2.5** | Hardwareanalyse | SMART/NVMe, Sensorik, Firmware, TPM/Secure Boot und Hardwarediagnose | Geplant |
| **2.3.0** | Fachmodule | Aufgaben, Kommunikation, Personal, Rechnungswesen, Material/Bestellungen als getrennte Module | Geplant; konkreter Zuschnitt offen |
| **2.4.0** | Administration/Skalierung | Rechte, Audits, zentrale Konfiguration, mögliche Collector-/Node-Architektur | Geplant |
| **2.5.0** | Dokumentation/Produktreife | Anwender-, Administrator-, API- und Entwicklerdokumentation, Modulhandbücher | Geplant; laufende Dokumentation ab sofort |
| **2.6.0** | Interne Patientenakte | Stammdaten, Dokumente, Verlauf, Rollen und Datenschutz | **Vorgeschlagene Neuordnung** |
| **2.7.0** | Medizinische Schnittstellen | Prüfung und ggf. Umsetzung passender Standards / Integrationen | **Vorgeschlagene Neuordnung** |
| **3.0.0** | KI-Unterstützung | Assistierte Suche, Diagnose, Erklärung und Auswertung mit nachvollziehbaren Quellen | Geplant |
| **3.5.0** | KI-gestützte Abläufe | Überwachungs-/Wartungsvorschläge, Automatisierung unter Freigabe- und Auditkontrolle | Geplant |

### Hinweis zu früheren Planständen

In einer älteren Roadmap waren **Hardwareanalyse um 2.1.5/2.2.5**, die **Patientenakte um 2.5.0** und **KI-Hilfe ab 3.0.0** vorgesehen. Die hier dargestellte Einordnung von Modularisierung in 2.1.5 und Patientenakte in 2.6.0/2.7.0 ist eine **aktualisierte Empfehlung**. Vor dem Start der betreffenden Entwicklungsphase muss die endgültige Versionszuordnung bewusst entschieden werden; die frühere Planung wird nicht als stillschweigend aufgehoben betrachtet.

## 4. Architekturprinzipien

### 4.1 Öffentlicher Core und Module

- **Plattformkern:** Start-/Laufzeitinfrastruktur, gemeinsame Dienste, Konfiguration, Identität, Benutzer-/Rechtemodell, Modulverwaltung, Schnittstellen, Migrationen und Ereignis-/Auditmechanismen.
- **Öffentliche Coremodule:** eigenständig nutzbare Bestandteile, beispielsweise Mehrquellen-Gerätemanager, Rechnungsprogramm und Aufgabensystem. Interne Modularisierung bedeutet **nicht**, dass diese Funktionen zu kostenpflichtigen Voraussetzungen werden.
- **Optionale Erweiterungsmodule:** separat entwickelte und versionierte Funktionen, beispielsweise spezialisierte Datei-/Infrastrukturinventarisierung, erweiterte Skalierung oder Virtualisierungsfunktionen. Ob einzelne Funktionen öffentlich oder optional sind, wird vor ihrer Umsetzung festgelegt.
- **Modulverwaltung und Lizenzverwaltung bleiben getrennt.** Ein Modul kann ausdrücklich ohne Lizenzpflicht funktionieren; kommerzielle Erweiterungen dürfen den öffentlichen Core nicht blockieren.
- **Separate Repositories** für eigenständige Module sind das Ziel; gemeinsame APIs und Versionierungsregeln definieren die Kopplung.

### 4.2 Einheitliches `.gam`-Paketformat

Das vorgesehene `.gam`-Format basiert konzeptionell auf einem ZIP-Paket mit Metadaten wie `module.json`, Version, kompatiblen Core-Versionen, Abhängigkeiten, enthaltenen Ressourcen und optionalen Lizenzmerkmalen. Installation/Update/Deinstallation müssen nachvollziehbar und möglichst reversibel sein.

**Sicherheitsanforderungen:** Paketvalidierung, digitale Signaturen bzw. Vertrauensprüfung, sichere Pfadbehandlung, kontrollierte Berechtigungen, Integritätschecks, nachvollziehbare Migrationen und definierte Fehler-/Rollback-Verfahren. Das exakte Manifest-Schema, die Signaturtechnik und API-Verträge werden vor der Implementierung spezifiziert.

### 4.3 Migration statt Big Bang

Keine komplette Neuschreibung des Monolithen. Stattdessen **schrittweise, testbare Extraktion** mit stabilem Verhalten, klaren Schnittstellen und Rückfallmöglichkeiten. Microservices sind kein Selbstzweck; zunächst ist ein **modularer Monolith** zweckmäßig. Verteilte Laufzeitarchitektur kommt erst hinzu, wenn messbare Anforderungen sie rechtfertigen.

## 5. Inhalte der Entwicklungsphasen

### 2.1.5 – Modulgrundlagen

1. Bestehende Funktionen in Plattformdienste, Coremodule und mögliche Erweiterungen inventarisieren; Abhängigkeiten dokumentieren.
2. Modul-API, Lifecycle, Versions- und Kompatibilitätsregeln festlegen.
3. Modulmanager und `.gam`-Manifest/Paketvalidierung als minimalen, sicheren Durchstich implementieren.
4. Ein risikoarmes Beispielmodul als Referenz auslagern, **ohne** funktionierende Inventarisierung zu beschädigen.
5. Modulupdates, fehlende Abhängigkeiten, ungültige Signaturen und Rollback testen.

**Abnahmekriterium:** Ein Beispielmodul lässt sich über definierte Schnittstellen installieren, laden und entfernen, ohne den öffentlichen Core unbrauchbar zu machen.

### 2.2.0 – Infrastruktur und weitere Quellen

Mögliche Erweiterungen: SNMP, Redfish, LLDP sowie gezielte Quellen für NAS, Netzwerkkomponenten, virtuelle Maschinen und Containerplattformen (z. B. VMware, VirtualBox, Hyper-V, Proxmox/KVM, Docker und später Kubernetes). Das sind **Kandidaten, keine Zusage, sämtliche Integrationen in einem Release abzuschließen**. Zuerst priorisieren und quellenspezifische Testsysteme bereitstellen.

### 2.2.5 – Hardwareanalyse

Tiefere Hardwaredaten und Zustandsdiagnose, wo Plattform und Berechtigungen dies zulassen: SMART/NVMe, Laufwerkszustände, Sensorschnittstellen, Firmware/BIOS, TPM/Secure Boot sowie verständliche Interpretation von nicht verfügbaren Messwerten. Keine unbelegten Aussagen über Hardwaregesundheit.

### 2.3.0 – Verwaltungs- und Fachmodule

Bestehende Fachbereiche (unter anderem Rechnungen, Aufgaben, Kommunikation, Personal, Bestellungen/Material) auf sinnvolle Modulgrenzen prüfen. Datenmodelle, Berechtigungen und gemeinsame Dienste klären, bevor Funktionen migriert oder erweitert werden. Historische oder ungenutzte GAM-1.0-Funktionen werden gesondert nach tatsächlichem Nutzen bewertet.

### 2.4.0 – Administration und Skalierung

Rollen-/Rechtesystem, Revisionsprotokolle und zentrale Betriebsdiagnose weiterentwickeln. Bei Bedarf Konzept für mehrere Collector-Knoten, kontrollierte Synchronisation und zentrale Geräteidentität ausarbeiten. Mehrinstanzbetrieb nur mit geklärter Konflikt-/Fehlerbehandlung.

### 2.5.0 – Dokumentation und Produktreife

Systemübersicht, Installations- und Updatehandbücher, Fehlersuche, API-/Moduldokumentation, Entwickler-Onboarding, Sicherheits-/Backupkonzept und Releaseverfahren standardisieren. **Dokumentation beginnt nicht erst mit 2.5.0**, sondern begleitet jede Entwicklungsphase.

### 2.6.0 / 2.7.0 – Medizinische Funktionen

Die frühere Patientenakten-Idee bleibt im Plan, wird aber erst nach belastbarer Anforderungsanalyse eingeplant. Erforderlich sind eindeutige Zuständigkeiten, Datenschutz-/Sicherheitskonzept, Datenintegrität, Rollen, Protokollierung, Backup und fachliche Prüfung. Externe medizinische Standards und Systeme werden nur nach konkreter Kompatibilitäts-, Berechtigungs- und Rechtsprüfung angebunden. **Keine erfundenen medizinischen Abläufe.**

### 3.0.0 – KI-Assistent

Optionaler Assistent für Inventarsuche, Diagnosehinweise, Dokumentationshilfe und verständliche Erklärungen. Antworten müssen auf überprüfbaren GAM-Daten beruhen und Unsicherheit ausweisen. Lokale Modelle als Datenschutzoption prüfen; ohne KI müssen Kernfunktionen weiter benutzbar bleiben.

### 3.5.0 – Kontrollierte KI-Automatisierung

Vorausschauende Wartung und Workflow-Vorschläge; Ausführung nur mit expliziten Berechtigungen, überprüfbaren Regeln, vollständigem Audit und menschlicher Freigabe für risikoreiche Aktionen. Kein autonomer Eingriff in Geräte allein aufgrund eines unbestätigten KI-Hinweises.

## 6. Übergreifende Qualitätsregeln

- **Bestehende Wege schützen:** Windows lokal/WinRM inklusive Authentifizierungs-Fallbacks, Linux/macOS lokal und SSH, Android ADB sowie Apple-Toolchain.
- **Quellen und Geräteidentität:** Erkennungsindizien von verifizierten Betriebssystemnachweisen trennen; keine automatischen Zusammenführungen ohne ausreichend starke technische Identität. Manuelle Zusammenführungen müssen reversibel bleiben.
- **Inventardaten vollständig erhalten:** UI-Filter und Ausblendungen dürfen vorhandene Daten nicht stillschweigend löschen.
- **Regressionstests:** Backend-Kompilierung, Frontend-Typecheck/Build, Start auf Zielplattformen, Inventarisierungs-/Verbindungstests und UI-Tests für lange Tabellen/umfangreiche Werte.
- **Sicherheit:** minimale Rechte, verschlüsselte Geheimnisse, sichere Defaults, prüfbare Zugriffe, sichere Updatepfade.
- **Betrieb:** reproduzierbare Versionierung, strukturierte Logs, nachvollziehbare Fehlerdiagnose, migrationsfähige Datenbankschemata und Backups.
- **Kompatibilität:** deklarierte Core-/Modulversionen und dokumentierte Breaking Changes.

## 7. Vorgehen für jede neue Version

1. **Scope:** Ziel, Nicht-Ziele und betroffene Module festhalten.
2. **Architektur:** Schnittstellen/Abhängigkeiten und mögliche Datenmigrationen dokumentieren.
3. **Implementierung:** Kleine, nachvollziehbare Änderungen; funktionierende Wege nicht unnötig anfassen.
4. **Tests:** Automatisierte Prüfungen plus reale Verbindungs-/Inventarisierungstests je relevantem Plattformpfad.
5. **Dokumentation:** `docs/`, Changelog, Migrationshinweise und Beispiele aktualisieren.
6. **Release:** Vollständige Builds, Paketintegrität, Versionskonsistenz und GitHub-Releasetext prüfen.
7. **Nachkontrolle:** Regressionsbefunde aufnehmen; Änderungen für die Folgerelease priorisieren.

## 8. Offene Entscheidungen vor 2.1.5

- Welcher minimal funktionsfähige **Plattformkern** ist erforderlich, und welche vorhandenen Bereiche werden als erstes echtes Coremodul getrennt?
- Welche Fassung erhalten `module.json`, Kompatibilitätsregeln, Signaturen und Abhängigkeitsmodell?
- Welche Bestandteile sind langfristig öffentlicher Core, welche optionale Erweiterungen?
- Wie werden Datenbankmigrationen, Modulupdates und Deinstallationen abgesichert?
- Bleibt die frühere Platzierung der Patientenakte bei 2.5.0, oder wird die hier vorgeschlagene Entkopplung in 2.6.0/2.7.0 übernommen?
- Welche Integrationen und Testumgebungen sind für 2.2.0 tatsächlich verfügbar?

## 9. Pflege dieses Dokuments

Diese Datei ist der **gemeinsame Entwicklungsleitfaden**, keine unveränderliche Spezifikation. Entscheidungen werden mit Datum und Begründung dokumentiert; vorgeschlagene Versionszuordnungen dürfen nach Aufwand, Stabilitätsbedarf und Testverfügbarkeit angepasst werden. Konkrete Implementierungen erhalten zusätzlich eigene technische Spezifikationen und Issues.

**Nächster Schritt nach 2.1.0:** Architektur- und Modulgrenzen für **2.1.5** definieren; zunächst keine riskante Umstrukturierung ohne Migration und Regressionstests.
