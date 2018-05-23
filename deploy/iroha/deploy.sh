#!/usr/bin/env bash

CURDIR="$(cd "$(dirname "$0")"; pwd)"

docker rm -f d3ledger-iroha
docker rm -f d3ledger-postgres
docker network create d3ledger-iroha-network || true

docker run --name d3ledger-postgres \
    -e POSTGRES_USER=postgres \
    -e POSTGRES_PASSWORD=mysecretpassword \
    --network=d3ledger-iroha-network \
    -d postgres:9.5

echo "Wait 5 seconds to start Postgres"
sleep 5

docker volume create d3ledger-blockstore

docker run -it --name d3ledger-iroha \
-p 50051:50051 \
-v ${CURDIR}:/opt/iroha_data \
-v d3ledger-blockstore:/tmp/block_store \
--network=d3ledger-iroha-network \
--entrypoint="" \
hyperledger/iroha:develop bash -c "irohad --config config.docker --genesis_block genesis.block --keypair_name keys/admin\@test"
