#!/usr/bin/env bash

if  [ -z "${IROHA_HOST}" ] || [ -z "${IROHA_PORT}" ] || [ -z "${CLASS}" ]; then
    echo "Please set IROHA_NETWORK, IROHA_HOST, IROHA_PORT and CLASS variables"
    exit
fi

STATE=0

while [ $STATE -ne 1 ]; do
    let STATE="$(./grpc_healthcheck ${IROHA_HOST} ${IROHA_PORT})"
    echo "Waiting for iroha..."
    sleep 0.1
done

java -cp /opt/notary/notary.jar ${CLASS}
