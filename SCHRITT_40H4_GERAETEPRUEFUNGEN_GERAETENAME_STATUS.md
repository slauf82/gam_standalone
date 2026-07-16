# Schritt 40h4 – Gerätename in Geräteprüfungen

- Die Beziehung `kontrolle.GERÄTE -> geräte.GERÄTE_ID` bleibt unverändert.
- In der Tabelle der Geräteprüfungen wird statt des Fremdschlüssels der Wert aus `geräte.GeräteName` angezeigt.
- Im Bearbeitungsdialog steht eine Geräteauswahl mit verständlichen Gerätenamen zur Verfügung.
- Intern wird weiterhin ausschließlich die Geräte-ID gespeichert.
