#!/usr/bin/env bash

CURDIR="$(cd "$(dirname "$0")"; pwd)"

${CURDIR}/db/deploy.sh
${CURDIR}/ethereum/deploy.sh
${CURDIR}/iroha/deploy.sh
