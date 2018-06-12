#!/usr/bin/env bash

CURDIR="$(cd "$(dirname "$0")"; pwd)"

docker rm -f d3ledger-db-postgres

docker run --name d3ledger-db-postgres \
    -e POSTGRES_USER=postgres \
    -e POSTGRES_PASSWORD=mysecretpassword \
    -p 54321:5432 \
    -d postgres:9.5

echo "Wait 5 seconds to start Postgres"
sleep 5

export PGPASSWORD=mysecretpassword

psql -U postgres -h localhost -p 54321 -f ${CURDIR}/sql/create_tables.sql
psql -U postgres -h localhost -p 54321 -f ${CURDIR}/sql/populate.sql
