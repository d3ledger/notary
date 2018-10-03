#!/usr/bin/env bash
set -e

TAG=$1

checkTag () {
  local e
  for e in "${@:2}"; do [[ "$e" == "$1" ]] && return 0; done
  echo "Invalid tag"
  echo "Usage: ./build_and_push_nexus.sh <master | develop | debug | reserved> "
  exit 1
}

tags=("master" "develop" "debug", "reserved")

checkTag $TAG "${tags[@]}"


rm build/libs/notary-1.0-SNAPSHOT-all.jar || true
./gradlew shadowJar
