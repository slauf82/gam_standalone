#!/usr/bin/env bash
# Muss mit "source scripts/activate-java.sh" eingebunden werden.
GAM_ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
if ! "$GAM_ROOT_DIR/scripts/ensure-java.sh"; then
  echo >&2
  echo "[FEHLER] Java 21 konnte nicht eingerichtet werden." >&2
  echo "Fuer einen automatischen Erstdownload ist Internetzugriff erforderlich." >&2
  return 1 2>/dev/null || exit 1
fi

GAM_JAVA_HOME=""
if [ -f "$GAM_ROOT_DIR/runtime/java-home.txt" ]; then
  GAM_JAVA_HOME="$(head -n 1 "$GAM_ROOT_DIR/runtime/java-home.txt" | tr -d '\r\n')"
elif [ -x "$GAM_ROOT_DIR/runtime/java/bin/java" ]; then
  GAM_JAVA_HOME="$GAM_ROOT_DIR/runtime/java"
fi

if [ -z "$GAM_JAVA_HOME" ] || [ ! -x "$GAM_JAVA_HOME/bin/java" ] || [ ! -x "$GAM_JAVA_HOME/bin/javac" ]; then
  echo "[FEHLER] Java 21 wurde gefunden/eingerichtet, aber JAVA_HOME konnte nicht aktiviert werden." >&2
  echo "[DIAGNOSE] java-home.txt: ${GAM_JAVA_HOME:-<leer>}" >&2
  return 1 2>/dev/null || exit 1
fi

export JAVA_HOME="$GAM_JAVA_HOME"
export PATH="$JAVA_HOME/bin:$PATH"
echo "[GAM] JAVA_HOME=$JAVA_HOME"
"$JAVA_HOME/bin/java" -version 2>&1 | head -n 1 | sed 's/^/[GAM] /'
"$JAVA_HOME/bin/javac" -version 2>&1 | head -n 1 | sed 's/^/[GAM] /'
unset GAM_JAVA_HOME GAM_ROOT_DIR
