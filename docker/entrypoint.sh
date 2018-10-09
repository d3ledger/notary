#!/usr/bin/env bash

if [ -z "${IROHA_NETWORK}" ] || [ -z "${IROHA_HOST}" ] || [ -z "${IROHA_PORT}" ] || [ -z "${CLASS}" ]; then
    echo "Please set IROHA_NETWORK, IROHA_HOST, IROHA_PORT and CLASS variables"
    exit
fi

STATE=0

while [ $STATE -ne 1 ]; do
    let STATE="$(docker run --network="${IROHA_NETWORK}" x3medima17/grpc_healthcheck ${IROHA_HOST} ${IROHA_PORT})"
    echo "Waiting for iroha..."
    sleep 0.1
done

java -cp /opt/notary/notary.jar ${CLASS}
