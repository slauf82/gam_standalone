# Schritt 29e – Demo-LBD-Autoload & First-Run Experience

## Ziel

Die Demo-Installation soll direkt nach dem Start mit einem synthetischen Demo-Empfänger funktionieren, ohne dass zuerst manuell eine `.lbd`-Datei ausgewählt werden muss.

## Enthalten

- Demo-LBD-Ordner `config/demo-lbd`
- Standarddatei `max.mustermann.lbd`
- weitere Demo-Dateien für EN/FR/UK/Firma
- LBD-Suchpfad um `./config/demo-lbd` erweitert
- `GAM_LBD_FILE`-Default auf `max.mustermann.lbd` gesetzt, sofern in der YAML vorhanden
- Fallback im `LbdService`: Wenn keine Datei gewählt ist, wird `max.mustermann.lbd` in den Suchordnern bevorzugt
- README-Hinweis ergänzt

## Erwartetes Ergebnis

Bei einer Demo-Installation sollte die Meldung

```text
WARN: lbd – .lbd-Empfängerdatei wurde nicht gefunden
```

nicht mehr erscheinen, solange `config/demo-lbd/max.mustermann.lbd` vorhanden ist.

## Test

1. Backend neu starten
2. Rechnung öffnen
3. Vorschau prüfen
4. PDF/ZUGFeRD prüfen
5. QR-Portal öffnen
