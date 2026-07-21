# GAM 2.1.0 – Preview 2

Mit **Preview 2** wird der Erststart von GAM 2.0 grundlegend vereinfacht und professionalisiert.

## Neuer grafischer Erststart

Nach der technischen Vorbereitung von Java und MariaDB startet GAM mit einem vollständig grafischen Einrichtungsassistenten im Frontend.

- Sprache als erster Einrichtungsschritt
- vollständiger Assistent in allen unterstützten Oberflächensprachen
- Anlage des ersten Superadministrators
- Erfassung grundlegender Praxisdaten
- Auswahl zwischen leerer Praxis und anonymisierter Beispieldatenbank
- sichere vollständige Initialisierung erst nach Bestätigung im Frontend
- bestehende Datenbanken werden nicht überschrieben

## Vereinfachter technischer Start

Die Backend-Installation beschränkt sich auf die notwendigen technischen Voraussetzungen:

- Prüfung und lokale Bereitstellung von JDK 21
- Prüfung und unter Windows automatische lokale Bereitstellung von MariaDB
- Anlage einer leeren Ziel-Datenbank ohne vorzeitigen Schema- oder Demoimport
- vollständige fachliche Datenbankeinrichtung ausschließlich über den Frontend-Assistenten

## Erste Schritte

1. `start-backend.bat` unter Windows oder `start-backend.sh` unter Linux/macOS starten.
2. Danach `start-frontend.bat` beziehungsweise `start-frontend.sh` starten.
3. Im Browser dem mehrsprachigen Einrichtungsassistenten folgen.

> **Hinweis:** Für notwendige Downloads während der erstmaligen technischen Einrichtung ist Internetzugriff erforderlich. Der anschließende Normalbetrieb von GAM 2.0 ist vollständig offline möglich.

## Datenbankvarianten

Im Verzeichnis `database` sind weiterhin beide Vorlagen enthalten:

- `gam_v2_1_0_preview2_empty.sql`
- `gam_demo_v2_1_0_preview2_anonymisiert.sql`

Der Nutzer muss diese Dateien nicht manuell importieren. Der Einrichtungsassistent übernimmt die Auswahl und Installation.

## Aktualisierung: verbesserte JDK-Erkennung

- Bereits installierte JDK-21-Versionen werden jetzt zuverlässig über `JAVA_HOME`, `java`, `javac`, Registry und typische Installationsordner erkannt.
- Ein globaler `javac`-Eintrag im `PATH` ist nicht mehr zwingend erforderlich.
- Verschiedene JDK-Distributionen werden unterstützt; entscheidend ist Java/Javac ab Version 21.
- Ein lokales Temurin-JDK wird nur noch heruntergeladen, wenn kein geeignetes vorhandenes JDK gefunden wurde.
