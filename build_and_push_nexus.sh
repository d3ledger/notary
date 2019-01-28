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

# <gradle build>
# ETH
./gradlew eth:shadowJar
./gradlew eth-withdrawal:shadowJar
./gradlew eth-registration:shadowJar
./gradlew eth-vacuum:shadowJar

# BTC
./gradlew btc-address-generation:shadowJar
./gradlew btc-registration:shadowJar
./gradlew btc-dw-bridge:shadowJar
# </gradle build>

# <docker build>
# ETH
docker build -t nexus.iroha.tech:19002/d3-deploy/eth-relay:$TAG -f docker/eth-relay.dockerfile .
docker build -t nexus.iroha.tech:19002/d3-deploy/eth-registration:$TAG  -f docker/eth-registration.dockerfile .
docker build -t nexus.iroha.tech:19002/d3-deploy/notary:$TAG  -f docker/notary.dockerfile .
docker build -t nexus.iroha.tech:19002/d3-deploy/eth-withdrawal:$TAG  -f docker/eth-withdrawal.dockerfile .

# BTC
docker build -t nexus.iroha.tech:19002/d3-deploy/btc-address-generation:$TAG -f docker/btc-address-generation.dockerfile .
docker build -t nexus.iroha.tech:19002/d3-deploy/btc-registration:$TAG  -f docker/btc-registration.dockerfile .
docker build -t nexus.iroha.tech:19002/d3-deploy/btc-dw-bridge:$TAG  -f docker/btc-dw-bridge.dockerfile .
# </docker build>

# <docker push>
# ETH
docker push nexus.iroha.tech:19002/d3-deploy/eth-relay:$TAG
docker push nexus.iroha.tech:19002/d3-deploy/eth-registration:$TAG
docker push nexus.iroha.tech:19002/d3-deploy/notary:$TAG
docker push nexus.iroha.tech:19002/d3-deploy/eth-withdrawal:$TAG

# BTC
docker push nexus.iroha.tech:19002/d3-deploy/btc-address-generation:$TAG
docker push nexus.iroha.tech:19002/d3-deploy/btc-registration:$TAG
docker push nexus.iroha.tech:19002/d3-deploy/btc-dw-bridge:$TAG
# </docker push>
