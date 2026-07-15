# Schritt 40g – Labormodul und Laborworkflow V1

## Umsetzung
- Neues Modul **Labor** in Login-Auswahl, Modulnavigation, Workflow-Gruppe und Moduleinstellungen integriert.
- Neue persistente Tabelle `laboratory_order`, automatisch beim Backendstart angelegt.
- Laboraufträge mit Patient, Patientennummer, anforderndem Arzt, Labor, Untersuchungen, Material, Proben-ID/Barcode, Priorität und Fälligkeit.
- Organisatorischer Workflow:
  - ANGELEGT
  - ANGEFORDERT
  - PATIENT_VORBEREITET
  - PROBE_ENTNOMMEN
  - VERSANDT
  - BEFUND_EINGEGANGEN
  - AERZTLICH_GEPRUEFT
  - PATIENT_INFORMIERT
  - ABGESCHLOSSEN
  - NACHENTNAHME_ERFORDERLICH / ABGEBROCHEN
- Dokumentation von Entnahme, Versand, Befundeingang, ärztlicher Prüfung und Patienteninformation.
- Suche, Statusfilter und Überfälligkeitskennzeichnung.
- Serverseitige Pflichtprüfungen für Untersuchungen, Probenmaterial, ärztliche Prüfung und Patienteninformation.

## Bewusste Abgrenzung V1
Keine medizinische Diagnostik, Referenzwertbewertung, LDT-/HL7-Schnittstelle oder Laborgeräteanbindung. V1 bildet zunächst den organisatorischen und nachvollziehbaren Praxisworkflow ab.

## Buildhinweis
Die Quell- und Paketstruktur wurde geprüft. Ein vollständiger Frontendbuild war in der isolierten Umgebung nicht möglich, weil `frontend/node_modules` nicht im Ausgangspaket enthalten ist. Maven steht in der Umgebung ebenfalls nicht zur Verfügung.
