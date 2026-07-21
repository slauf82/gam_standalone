# Schritt 33g – MaryTTS 5.2.1 Maven-Central-Fix

## Ursache

Der vorherige Build hat MaryTTS-Artefakte teilweise mit Version `5.2` gesucht.
In Maven Central sind die zentralen MaryTTS-Artefakte aber als `5.2.1` verfügbar.

## Fix

Der POM wurde auf Maven-Central-fähige Artefakte umgestellt:

* `marytts-runtime:5.2.1`
* `marytts-lang-de:5.2.1`
* `marytts-lang-en:5.2.1`
* `marytts-lang-fr:5.2.1`
* `voice-cmu-slt-hsmm:5.2.1`

## Wichtig

Deutsch und Französisch sind als Sprachmodule eingebunden.
Die einzige in diesem Build hart eingebundene Voice ist `cmu-slt-hsmm`, weil diese sicher in Maven Central als `5.2.1` vorhanden ist.

Weitere deutsche/französische Stimmen können später ergänzt werden, sobald deren Artefaktquelle zuverlässig geklärt ist.

## Build

Bitte einmal mit Maven-Update bauen:

```bat
mvn -U clean package
```

oder:

```bat
scripts\build-backend-marytts-521.bat
```

## Ergebnis

Dieser Schritt soll den Maven-Build wieder stabil machen und MaryTTS Embedded mit den sicher auflösbaren 5.2.1-Artefakten testen.
