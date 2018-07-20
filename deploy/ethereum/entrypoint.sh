#!/bin/sh
NODE=${NODE:-0}

/parity/parity account import --chain ropsten /eth/keys

/parity/parity account --chain ropsten list

/parity/parity --chain ropsten --light --cache-size 4096 --base-path /eth \
    --db-path /eth/data --keys-path /eth/keys \
    --jsonrpc-interface all --jsonrpc-port 8545 --jsonrpc-apis all --jsonrpc-cors all
