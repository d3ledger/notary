#!/usr/bin/env bash

CURDIR="$(cd "$(dirname "$0")"; pwd)"

docker rm -f node0 || true
docker rm -f node1 || true
docker rm -f manager || true

docker network create d3ledger-eth-network || true

# Run node0
docker run -d -v \
	${CURDIR}:/eth \
	--name node0 \
	-p 30303:30303 \
	-p 8545:8545 \
    -p 8546:8546 \
	-p 8180:8180 \
	--network d3ledger-eth-network \
	--entrypoint ""  parity/parity  bash -c "chmod +x /eth/entrypoint.sh && /eth/entrypoint.sh 0"

# Run node1
docker run -d -v \
    ${CURDIR}:/eth \
    --name node1 \
    --network d3ledger-eth-network \
	--entrypoint "" \
	parity/parity  bash -c "chmod +x /eth/entrypoint.sh && /eth/entrypoint.sh 1"

echo "Wait 5 seconds"
sleep 5

docker run -it \
    -v ${CURDIR}:/eth \
    --name manager \
    --network d3ledger-eth-network \
    --entrypoint "" \
    python bash -c "pip install requests && python /eth/main.py"
