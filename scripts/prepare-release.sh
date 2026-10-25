#!/usr/bin/env bash
set -euo pipefail

mkdir -p dist
test "$(find agent/build/libs -maxdepth 1 -type f -name 'datafox-javaagent-*-all.jar' | wc -l)" -eq 1
test -f agent/build/publications/maven/pom-default.xml
test -n "$(find licenses -type f -print -quit)"

cp agent/build/libs/datafox-javaagent-*-all.jar dist/
cp agent/build/libs/datafox-javaagent-*-sources.jar dist/
cp agent/build/libs/datafox-javaagent-*-javadoc.jar dist/
cp agent/build/publications/maven/pom-default.xml dist/datafox-javaagent.pom

zip -rq dist/licenses.zip licenses

(
  cd dist
  sha256sum *.jar *.pom *.zip > SHA256SUMS
  sha256sum -c SHA256SUMS
)
