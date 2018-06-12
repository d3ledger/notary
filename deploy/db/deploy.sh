#!/usr/bin/env bash

CURDIR="$(cd "$(dirname "$0")"; pwd)"

psql -U postgres -h localhost -p 5432 -f ${CURDIR}/sql/drop.sql

psql -U postgres -h localhost -p 5432 -f ${CURDIR}/sql/create_tables.sql
psql -U postgres -h localhost -p 5432 -f ${CURDIR}/sql/populate.sql
