#!/usr/bin/env bash

CURDIR="$(cd "$(dirname "$0")"; pwd)"

docker rm -f iroha
docker rm -f some-postgres
docker network create iroha-network || true

docker run --name some-postgres \
    -e POSTGRES_USER=postgres \
    -e POSTGRES_PASSWORD=mysecretpassword \
    --network=iroha-network \
    -d postgres:9.5

echo "Wait 5 seconds to start Postgres"
sleep 5

docker volume create blockstore
docker run -it --name iroha \
-p 50051:50051 \
-v ${CURDIR}:/opt/iroha_data \
-v blockstore:/tmp/block_store \
--network=iroha-network \
--entrypoint="" \
hyperledger/iroha:develop bash -c "irohad --config config.docker --genesis_block genesis.block --keypair_name keys/admin\@test"