#!/usr/bin/env bash
set -e

PROFILE=$1

checkProfile () {
  local e
  for e in "${@:2}"; do [[ "$e" == "$1" ]] && return 0; done
  echo "Invalid profile"
  echo "Usage: ./build_and_push_nexus.sh <local | testnet | mainnet>"
  exit 1
}

profiles=("local" "testnet" "mainnet")

checkProfile $PROFILE "${profiles[@]}"


rm build/libs/notary-1.0-SNAPSHOT-all.jar || true
gradle shadowJar -Pprofile=$PROFILE

docker build -t nexus.iroha.tech:19002/d3-deploy/eth-relay:$PROFILE -f eth-relay.dockerfile .
docker build -t nexus.iroha.tech:19002/d3-deploy/registration:$PROFILE  -f registration.dockerfile .
docker build -t nexus.iroha.tech:19002/d3-deploy/notary:$PROFILE  -f notary.dockerfile .
docker build -t nexus.iroha.tech:19002/d3-deploy/withdrawal:$PROFILE  -f withdrawal.dockerfile .


docker push nexus.iroha.tech:19002/d3-deploy/eth-relay:$PROFILE
docker push nexus.iroha.tech:19002/d3-deploy/registration:$PROFILE
docker push nexus.iroha.tech:19002/d3-deploy/notary:$PROFILE
docker push nexus.iroha.tech:19002/d3-deploy/withdrawal:$PROFILE
