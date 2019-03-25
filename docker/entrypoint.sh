#!/usr/bin/env bash
##
## Copyright D3 Ledger, Inc. All Rights Reserved.
## SPDX-License-Identifier: Apache-2.0
##


if  [ -z "${IROHA_HOST}" ] || [ -z "${IROHA_PORT}" ] || [ -z "${CLASS}" ]; then
    echo "Please set IROHA_HOST, IROHA_PORT and CLASS variables"
    exit
fi

STATE=1

while [ $STATE -ne 0 ]; do
    ./grpc_healthcheck ${IROHA_HOST} ${IROHA_PORT}
    STATE=$?
    echo "Waiting for iroha on ${IROHA_HOST}:${IROHA_PORT}"
    sleep 0.1
done

java -cp /opt/notary/${JAR_NAME} ${CLASS}
