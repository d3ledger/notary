#!/usr/bin/env bash


docker build -t nexus.iroha.tech:19002/d3-deploy/eth-relay -f eth-relay.dockerfile .
docker build -t nexus.iroha.tech:19002/d3-deploy/registration -f registration.dockerfile .
docker build -t nexus.iroha.tech:19002/d3-deploy/notary -f notary.dockerfile .
docker build -t nexus.iroha.tech:19002/d3-deploy/withdrawal -f withdrawal.dockerfile .


docker push nexus.iroha.tech:19002/d3-deploy/eth-relay
docker push nexus.iroha.tech:19002/d3-deploy/registration &
docker push nexus.iroha.tech:19002/d3-deploy/notary &
docker push nexus.iroha.tech:19002/d3-deploy/withdrawal
