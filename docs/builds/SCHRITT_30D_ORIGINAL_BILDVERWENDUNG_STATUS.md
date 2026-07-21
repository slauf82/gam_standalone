# Schritt 30d – Original-Bildverwendung aus GAM 1.0

Dieser Schritt rekonstruiert die Bildverwendung nicht nur pauschal, sondern anhand der Alt-GAM-Quellen aus `src.zip`.

## Gefundene Referenzen im Altcode

### Rechnungslogos

Datei:

```text
src/java/jpa/controllers/RechnungJpaController.java
```

Dort wurde abhängig von `RGESELLSCHAFTS_ID` das Logo gesetzt:

```text
RGESELLSCHAFTS_ID 1 -> /images/logo_AMAE_blau.png
RGESELLSCHAFTS_ID 2 -> /images/logo_ACQUA_blau.png
RGESELLSCHAFTS_ID 3 -> /images/logo_ACQUA_blau.png
RGESELLSCHAFTS_ID 4 -> /images/KOPFZENTRUM_LOGO.png
RGESELLSCHAFTS_ID 5 -> /images/KOPFZENTRUM_LOGO.png
RGESELLSCHAFTS_ID 6 -> /images/Healthcode_logo_blau.png
RGESELLSCHAFTS_ID 7 -> /images/KOPFZENTRUM_LOGO.png
```

Diese Zuordnung wird jetzt für Rechnungsvorschau und PDF-Rechnung genutzt.

### Signatur-/Abbruchbilder

Datei:

```text
src/java/jsf/SignaturController.java
```

Gefundene Bilder:

```text
images/welcome.png
images/kopf_logo2.jpg
res/btn_cancel.png
res/btn_ok.png
res/btn_retry.png
res/btn_scroll.png
res/device_omega.png
res/device_gamma.png
res/device_delta.png
res/device_alpha.png
```

Im gelieferten `images.zip` sind unter anderem `welcome.png`, `kopf_logo2.jpg` und `cancel.png` vorhanden. Die `res/*`-Signaturpad-Bilder sind nicht vollständig als gleichnamige Dateien enthalten; vorhandene Varianten wurden aber übernommen.

## In Schritt 30d umgesetzt

- alle Bilder aus `images.zip` nach `frontend/public/images/`
- alle Bilder aus `images.zip` nach `backend/src/main/resources/static/images/`
- `favicon.ico` nach `frontend/public/favicon.ico`
- Browser-Titel auf `GAM 2.0`
- GAM-Logo im Login
- gesellschaftsabhängiges Logo in der Rechnungsvorschau
- gesellschaftsabhängiges Logo in der PDF-Rechnung

## Noch optional

- Signaturpad-Dialoge genauer rekonstruieren, falls das Signaturmodul wieder vollständig aktiviert wird
- Start-/Welcome-Bild an der ursprünglichen Stelle verwenden, falls der alte Startdialog nachgebaut wird
