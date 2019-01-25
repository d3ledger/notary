#!/usr/bin/env bash
set -e

TAG=$1

checkTag () {
  local e
  for e in "${@:2}"; do [[ "$e" == "$1" ]] && return 0; done
  echo "Invalid tag"
  echo "Usage: ./build_and_push_nexus.sh <master | develop | debug>"
  exit 1
}

tags=("master" "develop" "debug")

checkTag $TAG "${tags[@]}"


./gradlew eth:shadowJar
./gradlew eth-withdrawal:shadowJar
./gradlew eth-registration:shadowJar
./gradlew eth-vacuum:shadowJar

# Build common services docker images
docker build -t nexus.iroha.tech:19002/d3-deploy/registration:TAG -f docker/registration.dockerfile .

# Build Ethereum related docker images
docker build -t nexus.iroha.tech:19002/d3-deploy/eth-relay:$TAG -f docker/eth-relay.dockerfile .
docker build -t nexus.iroha.tech:19002/d3-deploy/eth-registration:$TAG  -f docker/eth-registration.dockerfile .
docker build -t nexus.iroha.tech:19002/d3-deploy/notary:$TAG  -f docker/notary.dockerfile .
docker build -t nexus.iroha.tech:19002/d3-deploy/withdrawal:$TAG  -f docker/withdrawal.dockerfile .

#Push common services docker images
docker push nexus.iroha.tech:19002/d3-deploy/registration:$TAG

# Push Ethereum related docker images
docker push nexus.iroha.tech:19002/d3-deploy/eth-relay:$TAG
docker push nexus.iroha.tech:19002/d3-deploy/eth-registration:$TAG
docker push nexus.iroha.tech:19002/d3-deploy/notary:$TAG
docker push nexus.iroha.tech:19002/d3-deploy/withdrawal:$TAG
