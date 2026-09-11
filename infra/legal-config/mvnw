#!/bin/sh
set -eu
if command -v mvn >/dev/null 2>&1; then
  exec mvn "$@"
fi

VERSION=3.9.16
BASE="${HOME}/.m2/wrapper/dists"
DIR="${BASE}/apache-maven-${VERSION}"
MVN="${DIR}/bin/mvn"
if [ ! -x "$MVN" ]; then
  mkdir -p "$BASE"
  ZIP="${TMPDIR:-/tmp}/apache-maven-${VERSION}.zip"
  URL="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/${VERSION}/apache-maven-${VERSION}-bin.zip"
  if command -v curl >/dev/null 2>&1; then curl -fsSL "$URL" -o "$ZIP"
  elif command -v wget >/dev/null 2>&1; then wget -q "$URL" -O "$ZIP"
  else echo "Se requiere Maven o curl/wget para descargar Maven." >&2; exit 1; fi
  rm -rf "$DIR"
  unzip -q "$ZIP" -d "$BASE"
  rm -f "$ZIP"
fi
exec "$MVN" "$@"
