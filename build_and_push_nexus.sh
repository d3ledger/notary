#!/usr/bin/env bash

rm build/libs/notary-1.0-SNAPSHOT-all.jar || true
gradle shadowJar -Pprofile=testnet

docker build -t nexus.iroha.tech:19002/d3-deploy/eth-relay:testnet -f eth-relay.dockerfile .
docker build -t nexus.iroha.tech:19002/d3-deploy/registration:testnet  -f registration.dockerfile .
docker build -t nexus.iroha.tech:19002/d3-deploy/notary:testnet  -f notary.dockerfile .
docker build -t nexus.iroha.tech:19002/d3-deploy/withdrawal:testnet  -f withdrawal.dockerfile .


docker push nexus.iroha.tech:19002/d3-deploy/eth-relay:testnet
docker push nexus.iroha.tech:19002/d3-deploy/registration:testnet
docker push nexus.iroha.tech:19002/d3-deploy/notary:testnet
docker push nexus.iroha.tech:19002/d3-deploy/withdrawal:testnet
