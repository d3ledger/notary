#!/usr/bin/env bash
##
## Copyright D3 Ledger, Inc. All Rights Reserved.
## SPDX-License-Identifier: Apache-2.0
##


CURDIR="$(cd "$(dirname "$0")"; pwd)"

docker rm -f d3ledger-waves || true
docker network create d3ledger-waves-network || true


docker run -it --name d3ledger-waves \
    -p 6869:6869 \
    -v ${CURDIR}:/opt/waves \
    --network=d3ledger-waves-network \
    --entrypoint="" \
    x3medima17/waves bash -c "java -jar /root/waves.jar settings.conf"
